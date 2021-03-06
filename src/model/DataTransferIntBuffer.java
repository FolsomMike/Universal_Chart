/******************************************************************************
* Title: DataTransferIntBuffer.java
* Author: Mike Schoonover
* Date: 03/16/15
*
* Purpose:
*
* This class handles storing of integer data by one thread in a circular buffer
* for retrieval by another thread.
*
* For any location which has not yet had data stored, the first data value will
* be stored without testing. Subsequent data stored at the same location will
* be tested against the data already in the location and the new data will only
* replace the existing data if the new data is a greater or lesser value,
* depending on the type of peak being stored (high or low).
*
* If the put pointer is moved and the ready flag set but no valid data is yet
* in place for that position, data from the previous position will be copied
* if that data is valid (ready flag set). If that data is not valid, then the
* "default" data set will be copied. That ensures that reasonably safe data
* is always retrieved. The default data value can be changed at any time.
*
* NOTE: The class is not Generic as Generic classes do not allow primitives
* for use as generic types.
*
* Because data can be erased, at which time the put and get pointers might
* end up pointing at the same place simultaneously, all data storage and
* retrieval as well as pointer manipulation must be synchronized.
*
* The data buffer dataBuf is circular. When the end is reached, storage starts
* back over at the beginning.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package model;

//-----------------------------------------------------------------------------
// class DataTransferIntBuffer
//

public class DataTransferIntBuffer{


public int chartGroupNum = -1;
public int chartNum = -1;
public int graphNum = -1;
public int traceNum = -1;

int putPointer;
int getPointer;

private boolean positionAdvanced = false;
public boolean getPositionAdvanced() { return positionAdvanced; }
public void setPositionAdvanced(boolean pSet) { positionAdvanced = pSet; }

private boolean leadBuffer = false;
public boolean isLeadBuffer() { return leadBuffer; }
public void setLeadBuffer(boolean pState) { leadBuffer = pState; }

private boolean trailBuffer = false;
public boolean isTrailBuffer() { return trailBuffer; }
public void setTrailBuffer(boolean pState) { trailBuffer = pState; }

int bufLength;
int dataBuf[];

int flags[];                 //stores various flags for plotting
                             //0000 0000 0000 0000 | 0000 000 | 0 0000 0000
                             //           ||| |||| | threshold| clock position
                             //           ||| |||> min or max was flagged
                             //           ||| ||> segment start separator
                             //           ||| |> segment end separator
                             //           ||| > end mask marks
                             //           ||> data is valid for use
                             //           |> data has been erased
                             //           > data in process

int peakType;

int defaultData = 0;
synchronized public void setDefaultData(int pValue){ defaultData = pValue; }

private int segmentLength;
private int lastSegmentStartIndex;
private int lastSegmentEndIndex;

private static int DATA_RESET_VALUE = 0;

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::DataTransferIntBuffer (constructor)
//
// Parameter pBufLength specifies the size of the data buffer array.
// If parameter pPeakIsHigher is true, then a peak is determined by one value
// being higher than another. If false, a peak reflects the lowest value.
//

public DataTransferIntBuffer(int pBufLength, int pPeakType)
{

    bufLength = pBufLength; peakType = pPeakType;

}//end of DataTransferIntBuffer::DataTransferIntBuffer (constr)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::init
//
// Creates the data buffer of size pBufLength and prepares for use.
//

public void init(int pDefaultDataValue)
{

    defaultData = pDefaultDataValue;

    if (peakType == DataFlags.CATCH_HIGHEST){
        DATA_RESET_VALUE = Integer.MIN_VALUE;
    }
    else{
        DATA_RESET_VALUE = Integer.MAX_VALUE;
    }

    dataBuf = new int[bufLength];
    flags = new int[bufLength];

}// end of DataTransferIntBuffer::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::reset
//
// Resets all data in the buffer and the put/get pointers to default values.
//

synchronized public void reset()
{

    for(int i=0; i<dataBuf.length; i++){
        dataBuf[i] = DATA_RESET_VALUE;
    }

    for(int k=0; k<dataBuf.length; k++){
        flags[k] = DataFlags.FLAG_RESET_VALUE;
    }

    putPointer = 0;
    getPointer = 0;

}// end of DataTransferIntBuffer::reset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::putData
//
// Stores pData at location pointed by putPointer. If no data has been
// previously stored at that location, the location is simply set to pData.
// If data has been stored at that row, it is only updated with pData if the
// value in pData is greater or lesser than the old data, depending on the
// state of peakIsHigher (true means higher data is a peak, false means lower
// data is a peak).
//

synchronized public boolean putData(int pData)
{

    boolean stored = false;

    if ((flags[putPointer] & DataFlags.DATA_VALID) == 0){

        //no data previously stored, so store new data
        dataBuf[putPointer] = pData;
        flags[putPointer] |= DataFlags.DATA_VALID;
        stored = true;

    }else{
        //only store if new data is a new peak
        if(peakType == DataFlags.CATCH_HIGHEST){
            if (pData > dataBuf[putPointer]){
                dataBuf[putPointer] = pData;
                stored = true;
            }
        }
        else{
            if (pData < dataBuf[putPointer]){
                dataBuf[putPointer] = pData;
                stored = true;
            }
        }
    }

    return stored;

}// end of DataTransferIntBuffer::putData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::storeThresholdAtInsertionPoint
//
// Stores the number of the threshold violated by the datapoint at the current
// insertion point.
//
// The threshold number is increased by a value of 2 for storage -- a zero
// indicates no threshold was violated, a value of 1 indicates a manual user
// flag, any other value represents the threshold number plus 2.
//
// After adding 2, only the lower 7 bits are stored.
//

synchronized public void storeThresholdAtInsertionPoint(int pThreshold)
{

    flags[putPointer] &= DataFlags.CLEAR_THRESHOLD_MASK; //erase old value
    //shift up by value of 2 (see notes above)
    pThreshold += 2;
    //mask top bits to protect against invalid value
    pThreshold &= DataFlags.TRIM_THRESHOLD_MASK;
    flags[putPointer] += pThreshold << 9; //store new flag

}//end of DataTransferIntBuffer::storeThresholdAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::markSegmentStart
//
// Resets the segmentLength variable and records the current buffer location.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//

synchronized public void markSegmentStart()
{

    segmentLength = 0;

    //set flag to display a separator bar at the start of the segment
    flags[putPointer] |= DataFlags.SEGMENT_START_SEPARATOR;

    //record the buffer start position of the last segment
    lastSegmentStartIndex = putPointer;

}//end of DataTransferIntBuffer::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::markSegmentEnd
//
// Records the current buffer position as the point where the current segment
// ends.  If the segment is to be saved, the save should occur after this
// function is called and before markSegmentStart is called for the next
// segment so the endpoints of the segment to be saved will still be valid.
//
// A separator bar is drawn for cases where the data might be free running
// between segments, thus leaving a gap.  In that case, a bar at the start and
// end points is necessary to delineate between segment data and useless data
// in the gap.
//
// This function should be called whenever a new segment is to end - each
// segment could represent a piece being monitored, a time period, etc.
//

synchronized public void markSegmentEnd()
{

    //set flag to display a separator bar at the end of the segment
    flags[putPointer] |= DataFlags.SEGMENT_END_SEPARATOR;

    //record the buffer end position of the last segment
    lastSegmentEndIndex = putPointer;

}//end of DataTransferIntBuffer::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::setFlags
//
// OR's pFlags with flags[pIndex] to set one or more flag bits in the flags
// array at the specified position pIndex.
//

synchronized public void setFlags(int pIndex, int pFlags)
{

    flags[pIndex] |= pFlags;

}// end of DataTransferIntBuffer::setFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::setFlagsAtCurrentInsertionPoint
//
// OR's pFlags with flags[getPointer] to set one or more flag bits in the flags
// array at the current data insertion (put) point.
//

synchronized public void setFlagsAtCurrentInsertionPoint(int pFlags)
{

    flags[getPointer] |= pFlags;

}// end of DataTransferIntBuffer::setFlagsAtCurrentInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::getData
//
// Retrieves the row of data and meta data and flags at location pointed by
// getPointer and returns it via the pDataSet object. If the data is still at
// the reset value, method returns false, if the data is valid, returns true.
//
// Regardless of whether the data is at reset value or valid, the data at
// the location is returned in pDataSet.
//

synchronized public boolean getData(DataSetInt pDataSet)
{

    pDataSet.d = dataBuf[getPointer];
    pDataSet.flags = flags[getPointer];

    return( (flags[getPointer] & DataFlags.DATA_VALID) != 0 );

}// end of DataTransferIntBuffer::getData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::getDataChange
//
// If new data is ready in the buffer or data has been marked for erasure, the
// data and flags at getPointer are returned via pDataSet and getPointer is
// incremented or decremented as appropriate.
//
// Note that the same data location can be marked ERASED and READY which means
// that the old data was erased and new data already added at the same spot.
// This happens when the producer thread erases and adds data before the
// consumer thread can respond. In this case, with repeated calls, the
// pointer will be decremented until the erased section has been passed and then
// subsequent calls will return the new data. That is why the ERASED flag is
// checked first and takes precedent over the READY flag.
//
// Returns:
//
// +1 if data has been added and getPointer incremented
// 0 if no data has been added or removed
// -1 if data has been removed and getPointer decremented
//

synchronized public int getDataChange(DataSetInt pDataSet)
{

    //if data at current location has been marked erased, return that data and
    //move pointer to previous location

    if ((flags[getPointer] & DataFlags.DATA_ERASED) != 0){
        flags[getPointer] &= ~DataFlags.DATA_ERASED; //remove ERASED flag
        pDataSet.d = dataBuf[getPointer];
        pDataSet.flags = flags[getPointer];
        getPointer--;
        if(getPointer < 0) getPointer = bufLength-1;
        return(-1);
    }

    //if data at current location has been marked ready, return that data and
    //move pointer to next location

    if ((flags[getPointer] & DataFlags.DATA_READY) != 0){
        pDataSet.d = dataBuf[getPointer];
        pDataSet.flags = flags[getPointer];
        getPointer++;
        if(getPointer >= bufLength) getPointer = 0;
        return(1);
    }

    return(0); //no data newly ready or removed

}// end of DataTransferIntBuffer::getDataChange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::incPutPtrAndSetReadyAfterDataFill
//
// Increments the putPointer. If the new value is past the end of the buffer,
// it is restarted at zero. The data ready flag is set to signal that it is
// ready fro retrieval.
//
// Since the buffer is circular and data slots will be reused, the slot
// pointed to by putPointer is reset to be ready for new data.
//
// If no valid data is yet in place for the current put position, data from the
// previous position will be copied if that data is valid (ready flag set). If
// that data is not valid, then the "default" data set will be copied. That
// ensures that reasonably safe data is always retrieved.
//

synchronized public void incPutPtrAndSetReadyAfterDataFill()
{

    //if valid data present in current slot, mark ready and inc pointer

    if ((flags[putPointer] & DataFlags.DATA_VALID) != 0){ //flag set if result != 0
        incrementPutPointerAndSetReadyFlag();
        return;
    }

    //if previous buffer position has valid data, copy it to current position

    //get pointer to previous slot
    int prevSlotPtr = putPointer-1;
    if(prevSlotPtr < 0) prevSlotPtr = bufLength-1;

    if ((flags[prevSlotPtr] & DataFlags.DATA_VALID) != 0){ //flag set if result != 0
        dataBuf[putPointer] = dataBuf[prevSlotPtr];
        incrementPutPointerAndSetReadyFlag();
        return;
    }

    //since previous data was also invalid, use the default values instead

    dataBuf[putPointer] = defaultData;

    incrementPutPointerAndSetReadyFlag();
    return;

}// end of DataTransferIntBuffer::incPutPtrAndSetReadyAfterDataFill
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::incrementPutPointerAndSetReadyFlag
//
// Increments the putPointer. If the new value is past the end of the buffer,
// it is restarted at zero.
//
// The DATA_READY flag is set before the pointer is incremented to signal that
// the data in the current position is ready for extraction.
//
// Since the buffer is circular and data slots will be reused, the slot
// pointed to by putPointer is reset to be ready for new data.
//
// WARNING: The data in the current position may not yet have been filled with
// valid data. Normally, the incPutPtrAndSetReadyAfterDataFill() method is
// called instead. See notes at the top of that method for more info.
//
// ALSO NOTE that this method is not synchronized and is private as it is
// expected to be called from synchronized method in this object.
//

private void incrementPutPointerAndSetReadyFlag()
{

    flags[putPointer] |= DataFlags.DATA_READY;

    putPointer++;
    if(putPointer >= bufLength) putPointer = 0;

    dataBuf[putPointer] = DATA_RESET_VALUE;

    flags[putPointer] = DataFlags.FLAG_RESET_VALUE;

}// end of DataTransferIntBuffer::incrementPutPointerAndSetReadyFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::incrementPutPointer
//
// Increments the putPointer. If the new value is past the end of the buffer,
// it is restarted at zero.
//
// Since the buffer is circular and data slots will be reused, the slot
// pointed to by putPointer is reset to be ready for new data.
//
// WARNING: The data in the current position may not yet have been filled with
// valid data. Normally, the incPutPtrAndSetReadyAfterDataFill() method is
// called instead. See notes at the top of that method for more info.
//
// ALSO NOTE that this method is not synchronized and is private as it is
// expected to be called from synchronized method in this object.
//

private void incrementPutPointer()
{

    putPointer++;
    if(putPointer >= bufLength) putPointer = 0;

    for (int i=0; i<bufLength; i++){
        dataBuf[putPointer] = DATA_RESET_VALUE;
    }

    flags[putPointer] = DataFlags.FLAG_RESET_VALUE;

}// end of DataTransferIntBuffer::incrementPutPointer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::decrementPutPointer
//
// Decrements the putPointer. If the new value is less than zero, it is
// restarted at the end of the buffer.
//

synchronized public void decrementPutPointer()
{

    putPointer--;
    if(putPointer < 0) putPointer = bufLength-1;

}// end of DataTransferIntBuffer::decrementPutPointer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::decrementPutPointerAndSetErasedFlag
//
// Decrements the putPointer. If the new value is less than zero, it is
// restarted at the end of the buffer.
//
// The DATA_ERASED flag is set before the pointer is deccremented.
//

synchronized public void decrementPutPointerAndSetErasedFlag()
{

    flags[putPointer] |= DataFlags.DATA_ERASED;
    putPointer--;
    if(putPointer < 0) putPointer = bufLength-1;

}// end of DataTransferIntBuffer::decrementPutPointerAndSetErasedFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::incrementGetPointer
//
// Increments the getPointer. If the new value is past the end of the buffer,
// it is restarted at zero.
//

synchronized public void incrementGetPointer()
{

    getPointer++;
    if(getPointer >= bufLength) getPointer = 0;

}// end of DataTransferIntBuffer::incrementGetPointer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntBuffer::decrementGetPointer
//
// Decrements the getPointer. If the new value is less than zero, it is
// restarted at the end of the buffer.
//

synchronized public void decrementGetPointer()
{

    getPointer--;
    if(getPointer < 0) getPointer = bufLength-1;

}// end of DataTransferIntBuffer::decrementGetPointer
//-----------------------------------------------------------------------------

}//end of class DataTransferIntBuffer
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

