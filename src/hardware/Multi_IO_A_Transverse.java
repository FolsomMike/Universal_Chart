/******************************************************************************
* Title: Multi_IO_A_Transverse.java
* Author: Mike Schoonover
* Date: 01/16/15
*
* Purpose:
*
* This class handles communication with a Multi-IO board configuration A
* used for Transverse data acquisition.
*
*/

//-----------------------------------------------------------------------------

package hardware;

//-----------------------------------------------------------------------------

import model.IniFile;
import view.LogPanel;

//-----------------------------------------------------------------------------
// class Multi_IO_A_Transverse
//

public class Multi_IO_A_Transverse extends MultiIODevice
{

//-----------------------------------------------------------------------------
// Multi_IO_A_Transverse::Multi_IO_A_Transverse (constructor)
//

public Multi_IO_A_Transverse(int pIndex, LogPanel pLogPanel, 
                                         IniFile pConfigFile, boolean pSimMode)
{

    super(pIndex, pLogPanel, pConfigFile, pSimMode);
    
    PACKET_SIZE = 88;

    if(simMode){ simulator = new SimulatorTransverse(0); simulator.init(); }
    
}//end of Multi_IO_A_Transverse::Multi_IO_A_Transverse (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Multi_IO_A_Transverse::init
//
// Initializes the object.  Must be called immediately after instantiation.
//

@Override
public void init()
{
    
    super.init();

    loadConfigSettings();

    initAfterLoadingConfig();

}// end of Multi_IO_A_Transverse::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Multi_IO_A_Transverse::collectData
//
// Collects data from source(s) -- remote hardware devices, databases,
// simulations, etc.
//
// Should be called periodically to allow collection of data buffered in the
// source.
//

@Override
public void collectData()
{
    
    super.collectData();
    
}// end of Multi_IO_A_Transverse::collectData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Multi_IO_A_Transverse::loadConfigSettings
//
// Loads settings for the object from configFile.
//

@Override
void loadConfigSettings()
{
    
    super.loadConfigSettings();
    
    String section = "Device " + deviceNum + " Settings";

}// end of Multi_IO_A_Transverse::loadConfigSettings
//-----------------------------------------------------------------------------


}//end of class Multi_IO_A_Transverse
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
