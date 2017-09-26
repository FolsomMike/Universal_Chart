/******************************************************************************
* Title: DataTransferIntMultiDimBuffer.java
* Author: Mike Schoonover
* Date: 03/16/15
*
* Purpose:
*
* This class handles storing of integer data by one thread in a circular buffer
* for retrieval by another thread.
*
* The buffer is a multi-dimensional array. Data is stored and retrieved one
* slice at a time.
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
* A separate array named metaBuf is used to store information about each
* point in buffer array, such as the system which generated the point, the
* point's color, or any other information.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package model;

//-----------------------------------------------------------------------------
// class DataTransferIntMultiDimBuffer
//

public class DataTransferIntMultiDimBuffer{


public int chartGroupNum = -1;
public int chartNum = -1;
public int graphNum = -1;
public int traceNum = -1;

int putPointer;
int getPointer;

int bufLength;
int bufWidth;
int dataBuf[][];
int metaBuf[][];
int flags[];

int peakType;

int defaultData = 0;
synchronized public void setDefaultData(int pValue){ defaultData = pValue; }
int defaultMeta = 0;
synchronized public void setDefaultMeta(int pValue){ defaultMeta = pValue; }

private int segmentLength;
private int lastSegmentStartIndex;
private int lastSegmentEndIndex;

//constants

public static final int CATCH_HIGHEST = 0;
public static final int CATCH_LOWEST = 1;

private static int DATA_RESET_VALUE = 0;
private static final int META_RESET_VALUE = 0;

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::DataTransferIntMultiDimBuffer (constructor)
//
// Parameters pBufLength, pBufWidth specify the size of the data buffer array.
// If parameter pPeakIsHigher is true, then a peak is determined by one value
// being higher than another. If false, a peak reflects the lowest value.
//

public DataTransferIntMultiDimBuffer(int pBufLength, int pBufWidth,
                                                                 int pPeakType)
{

    bufLength = pBufLength; bufWidth = pBufWidth;
    peakType = pPeakType;

}//end of DataTransferIntMultiDimBuffer::DataTransferIntMultiDimBuffer (constr)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::init
//
// Creates the data buffer of size pBufSize and prepares for use.
//

public void init(int pDefaultDataValue, int pDefaultMetaValue)
{

    defaultData = pDefaultDataValue; defaultMeta = pDefaultMetaValue;

    if (peakType == CATCH_HIGHEST){
        DATA_RESET_VALUE = Integer.MIN_VALUE;
    }
    else{
        DATA_RESET_VALUE = Integer.MAX_VALUE;
    }

    dataBuf = new int[bufLength][bufWidth];
    metaBuf = new int[bufLength][bufWidth];
    flags = new int[bufLength];

}// end of DataTransferIntMultiDimBuffer::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::reset
//
// Resets all data in the buffer and the put/get pointers to default values.
//

synchronized public void reset()
{

    for(int i=0; i<dataBuf.length; i++){
        for(int j=0; j<dataBuf[i].length; j++){
        dataBuf[i][j] = DATA_RESET_VALUE;
        metaBuf[i][j] = META_RESET_VALUE;
        }
    }

    for(int k=0; k<dataBuf.length; k++){
        flags[k] = DataFlags.FLAG_RESET_VALUE;
    }

    putPointer = 0;
    getPointer = 0;

}// end of DataTransferIntMultiDimBuffer::reset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::putData
//
// Stores a slice of pData at row location pointed by putPointer. If no data
// has been previously stored at that row, the locations are simple set equal
// to pData. If data has been stored at that row, it is only updated
// with the values in pData if the values in pData are greater or lesser than
// the old data in each corresponding position in the row, depending on the
// state of peakIsHigher (true means higher data is a peak, false means lower
// data is a peak).
//
// For every new data point stored in buffer, the corresponding meta data in
// pMetaData will be stored in metaBuf.
//

synchronized public void putData(int[] pData, int[] pMetaData)
{

    if ((flags[putPointer] & DataFlags.DATA_VALID) == 0){

        //no data previously stored, so store new data
        System.arraycopy(pData,0, dataBuf[putPointer], 0, pData.length);
        System.arraycopy(pMetaData,0,metaBuf[putPointer], 0, pMetaData.length);
        flags[putPointer] |= DataFlags.DATA_VALID;

    }else{
        //only store if new data is a new peak
        if(peakType == CATCH_HIGHEST){
            for(int i=0; i<pData.length; i++){
                if (pData[i] > dataBuf[putPointer][i]){
                    dataBuf[putPointer][i] = pData[i];
                    metaBuf[putPointer][i] = pMetaData[i];
                }
            }
        }
        else{
            for(int i=0; i<pData.length; i++){
                if (pData[i] < dataBuf[putPointer][i]){
                    dataBuf[putPointer][i] = pData[i];
                    metaBuf[putPointer][i] = pMetaData[i];
                }
            }
        }
    }

}// end of DataTransferIntMultiDimBuffer::putData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::forcePutData
//
// Stores a slice of pData at row location pointed by putPointer.
//
// Has force in function name because there is no check for greater values.
//

synchronized public void forcePutData(int[] pData, int[] pMetaData)
{
    if ((flags[putPointer] & DataFlags.DATA_VALID) == 0){
        flags[putPointer] |= DataFlags.DATA_VALID;
    }

    //store new data
    System.arraycopy(pData,0, dataBuf[putPointer], 0, pData.length);
    System.arraycopy(pMetaData,0,metaBuf[putPointer], 0, pMetaData.length);

}// end of DataTransferIntMultiDimBuffer::forcePutData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::markSegmentStart
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

}//end of DataTransferIntMultiDimBuffer::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::markSegmentEnd
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

}//end of DataTransferIntMultiDimBuffer::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::setFlags
//
// OR's pFlags with flags[pIndex] to set one or more flag bits in the flags
// array at the specified position pIndex.
//

synchronized public void setFlags(int pIndex, int pFlags)
{

    flags[pIndex] |= pFlags;

}// end of DataTransferIntMultiDimBuffer::setFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::setFlagsAtCurrentInsertionPoint
//
// OR's pFlags with flags[getPointer] to set one or more flag bits in the flags
// array at the current data insertion (put) point.
//

synchronized public void setFlagsAtCurrentInsertionPoint(int pFlags)
{

    flags[getPointer] |= pFlags;

}// end of DataTransferIntMultiDimBuffer::setFlagsAtCurrentInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::getData
//
// Retrieves the row of data and meta data and flags at location pointed by
// getPointer and returns it via the pDataSet object. If the data is still at
// the reset value, method returns false, if the data is valid, returns true.
//
// Regardless of whether the data is at reset value or valid, the data at
// the location is returned in pDataSet.
//

synchronized public boolean getData(DataSetIntMultiDim pDataSet)
{

    System.arraycopy(dataBuf[putPointer], 0, pDataSet.d, 0, pDataSet.length);
    System.arraycopy(metaBuf[putPointer], 0, pDataSet.m, 0, pDataSet.length);

    pDataSet.flags = flags[getPointer];

    return( (flags[getPointer] & DataFlags.DATA_VALID) != 0 );

}// end of DataTransferIntMultiDimBuffer::getData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::getDataChange
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

synchronized public int getDataChange(DataSetIntMultiDim pDataSet)
{

    //if data at current location has been marked erased, return that data and
    //move pointer to previous location

    if ((flags[getPointer] & DataFlags.DATA_ERASED) != 0){
        flags[getPointer] &= ~DataFlags.DATA_ERASED; //remove ERASED flag
        System.arraycopy(dataBuf[getPointer],0, pDataSet.d,0, pDataSet.length);
        System.arraycopy(metaBuf[getPointer],0, pDataSet.m,0, pDataSet.length);
        pDataSet.flags = flags[getPointer];
        getPointer--;
        if(getPointer < 0) getPointer = bufLength-1;
        return(-1);
    }

    //if data at current location has been marked ready, return that data and
    //move pointer to next location

    if ((flags[getPointer] & DataFlags.DATA_READY) != 0){
        System.arraycopy(dataBuf[getPointer],0, pDataSet.d,0, pDataSet.length);
        System.arraycopy(metaBuf[getPointer],0, pDataSet.m,0, pDataSet.length);
        pDataSet.flags = flags[getPointer];
        getPointer++;
        if(getPointer >= bufLength) getPointer = 0;
        return(1);
    }

    return(0); //no data newly ready or removed

}// end of DataTransferIntMultiDimBuffer::getDataChange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::incPutPtrAndSetReadyAfterDataFill
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
        System.arraycopy(dataBuf[prevSlotPtr],0,dataBuf[putPointer],0,bufWidth);
        System.arraycopy(metaBuf[prevSlotPtr],0,metaBuf[putPointer],0,bufWidth);
        incrementPutPointerAndSetReadyFlag();
        return;
    }

    //since previous data was also invalid, use the default values instead

    for(int i=0; i<bufWidth; i++){
        dataBuf[putPointer][i] = defaultData;
        metaBuf[putPointer][i] = defaultMeta;
    }

    incrementPutPointerAndSetReadyFlag();
    return;

}// end of DataTransferIntMultiDimBuffer::incPutPtrAndSetReadyAfterDataFill
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::incrementPutPointerAndSetReadyFlag
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

    for (int i=0; i<bufWidth; i++){
        dataBuf[putPointer][i] = DATA_RESET_VALUE;
        metaBuf[putPointer][i] = META_RESET_VALUE;
    }

    flags[putPointer] = DataFlags.FLAG_RESET_VALUE;

}// end of DataTransferIntMultiDimBuffer::incrementPutPointerAndSetReadyFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::incrementPutPointer
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

    for (int i=0; i<bufWidth; i++){
        dataBuf[putPointer][i] = DATA_RESET_VALUE;
        metaBuf[putPointer][i] = META_RESET_VALUE;
    }

    flags[putPointer] = DataFlags.FLAG_RESET_VALUE;

}// end of DataTransferIntMultiDimBuffer::incrementPutPointer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::decrementPutPointer
//
// Decrements the putPointer. If the new value is less than zero, it is
// restarted at the end of the buffer.
//

synchronized public void decrementPutPointer()
{

    putPointer--;
    if(putPointer < 0) putPointer = bufLength-1;

}// end of DataTransferIntMultiDimBuffer::decrementPutPointer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::decrementPutPointerAndSetErasedFlag
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

}// end of DataTransferIntMultiDimBuffer::decrementPutPointerAndSetErasedFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::incrementGetPointer
//
// Increments the getPointer. If the new value is past the end of the buffer,
// it is restarted at zero.
//

synchronized public void incrementGetPointer()
{

    getPointer++;
    if(getPointer >= bufLength) getPointer = 0;

}// end of DataTransferIntMultiDimBuffer::incrementGetPointer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DataTransferIntMultiDimBuffer::decrementGetPointer
//
// Decrements the getPointer. If the new value is less than zero, it is
// restarted at the end of the buffer.
//

synchronized public void decrementGetPointer()
{

    getPointer--;
    if(getPointer < 0) getPointer = bufLength-1;

}// end of DataTransferIntMultiDimBuffer::decrementGetPointer
//-----------------------------------------------------------------------------

}//end of class DataTransferIntMultiDimBuffer
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

