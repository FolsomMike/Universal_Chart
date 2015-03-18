/******************************************************************************
* Title: SimulatorWall.java
* Author: Mike Schoonover
* Date: 03/16/15
*
* Purpose:
*
* This class provides simulation data for the EMI Wall system.
*
*/

//-----------------------------------------------------------------------------

package hardware;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SimulatorWall
//

public class SimulatorWall extends Simulator
{

    int avgWallSpikeLength = 0;
    int pulseWallSpikeLength = 0;
    int intCoilSpikeLength = 0;
    
//-----------------------------------------------------------------------------
// SimulatorWall::SimulatorWall (constructor)
//
    
public SimulatorWall(int pSimulatorNum)
{

    super(pSimulatorNum);
    
}//end of SimulatorWall::SimulatorWall (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SimulatorWall::init
//
// Initializes the object.  Must be called immediately after instantiation.
//

@Override
public void init()
{

    super.init();

    spikeOdds = 100;
    
}// end of SimulatorWall::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SimulatorWall::getRunPacket
//
// Returns a run-time packet of simulated data.
//
// The data range is 0 ~ 1,023 with zero volts at approximately 511.
//

@Override
public void getRunPacket(byte[] pPacket)
{

    //getRunPacket2(pPacket); //debug mks remove this
    
    int index = 0;
    
    addUnsignedShortToPacket(pPacket, index, simulateAverageWall());
    index += 2;
    addUnsignedShortToPacket(pPacket, index, simulatePulseWall());
    index += 2;
    addUnsignedShortToPacket(pPacket, index, simulateIntelligentCoil());
      
}// end of SimulatorWall::getRunPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::simulateAverageWall
//
// Simulates an average wall trace.
//
// More than one of the simulate methods shares lastSpikeValue so each can
// mess with other if one happens to spike when another is still in a spike.
// Won't happen very often and not really a problem.
//

int simulateAverageWall()
{

    int value = AD_ZERO_OFFSET;
    
    value += 45;
    
    value += (int)(WALL_SIM_NOISE * Math.random());
    
    if ((int)(WALL_SPIKE_ODDS_RANGE*Math.random()) < spikeOdds){
        lastSpikeValue = (int)(100 * Math.random());
        avgWallSpikeLength = 4 + (int)(10 * Math.random());
        value -= lastSpikeValue;
    }else{
        if (avgWallSpikeLength > 0){
            value -= lastSpikeValue; avgWallSpikeLength--;
        }
    }

    if (value > AD_MAX_VALUE) { value = AD_MAX_VALUE; }
    
    return(value);

}//end of Simulator::simulateAverageWall
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::simulatePulseWall
//
// Simulates a Pulse Wall trace.
//
// More than one of the simulate methods shares lastSpikeValue so each can
// mess with other if one happens to spike when another is still in a spike.
// Won't happen very often and not really a problem.
//

int simulatePulseWall()
{

    int value = AD_ZERO_OFFSET;
    
    value += 50;
    
    value += (int)(WALL_SIM_NOISE * Math.random());
    
    if ((int)(WALL_SPIKE_ODDS_RANGE*Math.random()) < spikeOdds){
        lastSpikeValue = (int)(100 * Math.random());
        pulseWallSpikeLength = 4 + (int)(10 * Math.random());
        value -= lastSpikeValue;
    }else{
        if (pulseWallSpikeLength > 0){
            value -= lastSpikeValue; pulseWallSpikeLength--;
        }
    }

    if (value > AD_MAX_VALUE) { value = AD_MAX_VALUE; }
    
    return(value);

}//end of Simulator::simulatePulseWall
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::simulateIntelligentCoil
//
// Simulates an Intelligent Coil wall trace.
//
// More than one of the simulate methods shares lastSpikeValue so each can
// mess with other if one happens to spike when another is still in a spike.
// Won't happen very often and not really a problem.
//

int simulateIntelligentCoil()
{

    int value = AD_ZERO_OFFSET;
    
    value += 55;
    
    value += (int)(WALL_SIM_NOISE * Math.random());
    
    if ((int)(WALL_SPIKE_ODDS_RANGE*Math.random()) < spikeOdds){
        lastSpikeValue = (int)(100 * Math.random());
        intCoilSpikeLength = 4 + (int)(10 * Math.random());
        value -= lastSpikeValue;
    }else{
        if (intCoilSpikeLength > 0){
            value -= lastSpikeValue; intCoilSpikeLength--;
        }
    }

    if (value > AD_MAX_VALUE) { value = AD_MAX_VALUE; }
    
    return(value);

}//end of Simulator::simulateIntelligentCoil
//-----------------------------------------------------------------------------


}//end of class SimulatorWall
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------