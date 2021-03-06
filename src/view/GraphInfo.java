/******************************************************************************
* Title: GraphInfo.java
* Author: Mike Schoonover
* Date: 02/28/15
*
* Purpose:
*
* This class encapsulates variables related to Graph objects.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package view;
//-----------------------------------------------------------------------------
// class GraphInfo
//

public class GraphInfo{

    public String title;
    public String shortTitle;
    
    //y offset
    public int yOffset;

    //tracks number of pixels the chart (and all graphs in the chart) has been
    //scrolled since last reset
    public int scrollOffset;

    //specifies amount graph was scrolled for the last scroll operation
    public int lastScrollAmount;

    //specifies the last drawn x. useful for keeping graphs aligned
    public int lastDrawnX;

}//end of class GraphInfo
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
