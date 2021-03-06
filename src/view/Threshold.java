/******************************************************************************
* Title: Threshold.java
* Author: Hunter Schoonover
* Date: 09/19/2017
*
* Purpose:
*
* This class handles a single threshold.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package view;

import java.awt.*;
import java.io.*;
import model.IniFile;
import model.SharedSettings;
import model.ThresholdInfo;
import toolkit.Tools;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Threshold
//
// This class creates and controls a trace.
//

public class Threshold extends Object{

    private final SharedSettings sharedSettings;
    public int getLevel() { return thresholdInfo.getLevel(); }
    public void setLevel(int pLvl) { thresholdInfo.setLevel(pLvl); }

    private final ThresholdInfo thresholdInfo;
    public ThresholdInfo getThresholdInfo() { return thresholdInfo; }

    private final IniFile configFile;
    private GraphInfo graphInfo;
    public GraphInfo getGraphInfo() { return graphInfo; }
    private final String section;
    public String getSection() { return section; }

    private int width, height;
    private int flagWidth, flagHeight;

    private int xMax, yMax;
    private int prevX = -1, prevY = Integer.MAX_VALUE;

    private double xScale = 1.0, yScale = 1.0;
    private int offset = 0;
    private int baseLine = 0;

    private Color backgroundColor;

    private boolean okToMark = true;

    // references to point at the controls used to adjust the values - these
    // references are set up by the object which handles the adjusters and are
    // only used temporarily

    private Object levelAdjuster;


//-----------------------------------------------------------------------------
// Threshold::Threshold (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Threshold(SharedSettings pSettings, IniFile pConfigFile,
                    GraphInfo pGraphInfo, int pChartGroupNum,
                    int pChartNum, int pGraphNum, int pThresholdNum, int pWidth,
                    int pHeight, Color pBackgroundColor)
{

    sharedSettings = pSettings;
    configFile = pConfigFile;
    graphInfo = pGraphInfo;

    thresholdInfo = new ThresholdInfo();
    thresholdInfo.setChartGroupNum(pChartGroupNum);
    thresholdInfo.setChartNum(pChartNum);
    thresholdInfo.setGraphNum(pGraphNum);
    thresholdInfo.setThresholdNum(pThresholdNum);

    width = pWidth; height = pHeight;
    xMax = width - 1; yMax = height - 1;
    backgroundColor = pBackgroundColor;

    section = "Chart Group " + thresholdInfo.getChartGroupNum()
                + " Chart " + thresholdInfo.getChartNum()
                + " Graph " + thresholdInfo.getGraphNum()
                + " Threshold " + thresholdInfo.getThresholdNum();

}//end of Threshold::Threshold (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::init
//
// Initializes the object.  Must be called immediately after instantiation.
//
// pIndex is a unique identifier for the object -- usually it's index position
// in an array of the creating object.
//

public void init()
{

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

}// end of Threshold::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::configure
//
// Loads configuration settings from the configuration.ini file.
//

private void configure(IniFile pConfigFile)
{

    thresholdInfo.setTitle(pConfigFile.readString(section, "title", "*"));

    thresholdInfo.setShortTitle(pConfigFile.readString(section, "short title",
                                                                        "*"));

    thresholdInfo.setDoNotFlag(pConfigFile.readBoolean(section,
                                    "do not flag - for reference only", false));

    thresholdInfo.setThresholdColor(pConfigFile.readColor(section, "color",
                                                                    Color.RED));

    thresholdInfo.setInvert(pConfigFile.readBoolean(section, "invert threshold",
                                                                        true));

    int lvl = pConfigFile.readInt(section, "default level", 50);
    thresholdInfo.setLevel(lvl);

    thresholdInfo.setAlarmChannel(pConfigFile.readInt(section, "alarm channel",
                                                        0));

    thresholdInfo.setFlagOnOver(pConfigFile.readBoolean(section, "flag on over",
                                                        true));

    //stuff that is only used for gui
    flagWidth = pConfigFile.readInt(section, "flag width", 5);
    flagHeight = pConfigFile.readInt(section, "flag height", 7);
    offset = configFile.readInt(section, "offset", 0);
    xScale = configFile.readDouble(section, "x scale", 1.0);
    yScale = configFile.readDouble(section, "y scale", 1.0);
    baseLine = configFile.readInt(section, "baseline", 0);

}//end of Threshold::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

    setLevel(pCalFile.readInt(section, "threshold level", getLevel()));

}//end of Threshold::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

    pCalFile.writeInt(section, "threshold level", getLevel());

}//end of Threshold::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::calculateY
//
// Calculates and returns the scaled and offset y derived from pY.
//

private int calculateY(int pY)
{

    return (int)Math.round(((pY - baseLine) * yScale) + offset);

}// end of Threshold::calculateY
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::calculateFlagY
//
// Calculates and returns what the y position of the flag should be.
//

private int calculateFlagY(int pY)
{

    int y = pY;

    //move so that the flag extends towards graph center
    if (!thresholdInfo.getInvert()){ y -= flagHeight-1; }

    //if flag would be drawn above or below the screen, force on screen
    if (y < 0) {y = 0;} if (y+flagHeight > yMax) {y = yMax-flagHeight;}

    return y;

}//end of Threshold::calculateFlagY
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::drawFlag
//
// Draws a flag with the threshold color at location xPos,pSigHeight.
//
// Note that pX and pY should already be scaled, the scroll offset for pX
// should have already been calculated, and the inversion for pY should have
// already been done.
//

public void drawFlag(Graphics2D pPG2, int pX, int pY)
{

    //if flag would be drawn above or below the screen, force on screen
    int y = calculateFlagY(pY);

    pPG2.setColor(thresholdInfo.getThresholdColor());

    //add 1 to xPos so flag is drawn to the right of the peak
    pPG2.fillRect(pX-1-flagWidth, y, flagWidth, flagHeight);

}//end of Threshold::drawFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::drawNextSlice
//
// Draws the threshold line up to pX. Note that pX should already be scaled
// before it is passed to this function. However, scroll offset should not have
// been calculated yet.
//

public void drawNextSlice(Graphics2D pG2, int pX)
{

    //adjust for any scrolling that has occurred before plotting
    int xAdj = pX - graphInfo.scrollOffset;
    int prevXAdj = prevX - graphInfo.scrollOffset;

    //draw threshold line
    int lvl = getPlotThresholdLevel();
    pG2.setColor(thresholdInfo.getThresholdColor());
    pG2.drawLine(prevXAdj, lvl, xAdj, lvl);

}// end of Threshold::drawNextSlice
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::getPlotThresholdLevel
//
// Calculates and returns the y point at which to plot the threshold level.
//
// This is not done once and stored in a class variable because sharedSettings
// is shared with multiple threads and objects; the threshold level can change
// at any time.
//

private int getPlotThresholdLevel()
{

    int plotThresholdLevel = calculateY(getLevel());
    if(plotThresholdLevel < 0) {plotThresholdLevel = 0;}
    if(plotThresholdLevel > yMax) {plotThresholdLevel = yMax;}

    //invert the y position if specified
    if (thresholdInfo.getInvert()){
        plotThresholdLevel = yMax - plotThresholdLevel;
    }

    return plotThresholdLevel;

}//end of Threshold::getPlotThresholdLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::paintThresholdLine
//
// Draws the threshold line all the way across the graph.
//

public void paintThresholdLine(Graphics2D pG2)

{

    int lvl = getPlotThresholdLevel();
    pG2.setColor(thresholdInfo.getThresholdColor());
    pG2.drawLine(0, lvl, xMax, lvl);

}//end of Threshold::paintThresholdLine
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::saveSegment
//
// Saves the thresholds settings to the open file pOut.
//

public void saveSegment(BufferedWriter pOut) throws IOException
{

    pOut.write("[Threshold]"); pOut.newLine();
    pOut.write("Threshold Index=" + thresholdInfo.getThresholdNum());
    pOut.newLine();
    pOut.write("Threshold Title=" + thresholdInfo.getTitle());
    pOut.newLine();
    pOut.write("Threshold Short Title=" + thresholdInfo.getShortTitle());
    pOut.newLine(); pOut.newLine();

    //save the threshold level
    pOut.write("Threshold Level=" + thresholdInfo.getLevel());
    pOut.newLine(); pOut.newLine();

}//end of Threshold::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::loadSegment
//
// Loads the data for a segment from pIn.  It is expected that the Threshold
// section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Threshold section, the [Threshold] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                             throws IOException
{

    //handle entries for the threshold itself
    String line = processThresholdEntries(pIn, pLastLine);

    return(line);

}//end of Threshold::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::processThresholdEntries
//
// Processes the entries for the threshold itself via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Threshold section, the [Threshold] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processThresholdEntries(BufferedReader pIn, String pLastLine)
                                                             throws IOException

{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //if pLastLine contains the [Threshold] tag, then skip ahead else read until
    // end of file reached or "[Threshold]" section tag reached

    if (Tools.matchAndParseString(pLastLine, "[Threshold]", "",  matchSet)) {
        success = true; //tag already found
    }
    else {
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Tools.matchAndParseString(line, "[Threshold]", "",  matchSet)){
                success = true; break;
            }
        }//while
    }

    if (!success) {
        throw new IOException(
        "The file could not be read - section not found for " + section);
    }

    //set defaults
    int thresholdIndexRead = -1;
    String titleRead = "", shortTitleRead = "";
    int levelRead = 100;

    //scan the first part of the section and parse its entries
    //these entries apply to the chart group itself

    success = false;
    while ((line = pIn.readLine()) != null){

        //stop when next section tag reached (will start with [)
        if (Tools.matchAndParseString(line, "[", "",  matchSet)){
            success = true; break;
        }

        //read the "Threshold Index" entry - if not found, default to -1
        if (Tools.matchAndParseInt(line, "Threshold Index", -1, matchSet)) {
            thresholdIndexRead = matchSet.rInt1;
        }

        //NOTE: this match is due to a bug in segments saved under
        // Segment Data Version 1.0 - the tag was misspelled - can be removed
        // eventually - only one job run with that version
        //read the "Theshold Index" entry - if not found, default to -1
        if (Tools.matchAndParseInt(line, "Theshold Index", -1, matchSet)) {
            thresholdIndexRead = matchSet.rInt1;
        }

        //read the "Threshold Title" entry - if not found, default to ""
        if (Tools.matchAndParseString(line, "Threshold Title", "", matchSet)){
            titleRead = matchSet.rString1;
        }

        //read the "Threshold Short Title" entry - if not found, default to ""
        if (Tools.matchAndParseString(
                                line, "Threshold Short Title", "", matchSet)) {
            shortTitleRead = matchSet.rString1;
        }

        //read the "Threshold Level" entry - if not found, default to 100
        if (Tools.matchAndParseInt(line, "Threshold Level", 100, matchSet)) {
            levelRead = matchSet.rInt1;
        }

    }//while ((line = pIn.readLine()) != null)

    //apply settings
    thresholdInfo.setTitle(titleRead);
    thresholdInfo.setShortTitle(shortTitleRead);
    thresholdInfo.setLevel(levelRead);

    if (!success) {
        throw new IOException(
        "The file could not be read - missing end of section for " + section);
    }

    //if the index number in the file does not match the index number for this
    //threshold, abort the file read

    if (thresholdIndexRead != thresholdInfo.getThresholdNum()) {
        throw new IOException(
        "The file could not be read - section not found for  " + section);
    }

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Threshold::processThresholdEntries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::updateDimensions
//
// Adjusts all width and height variables for the panel along with all such
// values in relevant child objects.
//
// Should be called any time the panel is resized.
//

public void updateDimensions(int pNewWidth, int pNewHeight)
{

    width = pNewWidth; height = pNewHeight;

    xMax = width - 1; yMax = height - 1;

}// end of Threshold::updateDimensions
//-----------------------------------------------------------------------------\

}//end of class Threshold
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------