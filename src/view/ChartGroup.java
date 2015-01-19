/******************************************************************************
* Title: ChartGroup.java
* Author: Mike Schoonover
* Date: 01/45/15
*
* Purpose:
*
* This class subclasses a JPanel to contain several Charts.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package view;

import javax.swing.*;
import model.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ChartGroup
//

class ChartGroup extends JPanel{

    private IniFile configFile;
    
    private String title, shortTitle;
    private int index;
    private int graphWidth, graphHeight;        
    private int numCharts;
    private Chart charts[];
    
//-----------------------------------------------------------------------------
// ChartGroup::ChartGroup (constructor)
//
//

public ChartGroup()
{

}//end of ChartGroup::ChartGroup (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Chart::init
//
// Initializes the object.  Must be called immediately after instantiation.
//
// pTitle is the text title for the graph.
//
// pIndex is a unique identifier for the object -- usually it's index position
// in an array of the creating object.
//

public void init(int pIndex, IniFile pConfigFile)
{

    index = pIndex; configFile = pConfigFile;
    
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
    loadConfigSettings();
        
    createCharts();
    
/*    
    //create chart for Longitudinal
    charts[LONG_CHART] = new Chart();
    charts[LONG_CHART].init(
           "Longitudinal", LONG_CHART, 2, true, 50, CHART_WIDTH, CHART_HEIGHT);
    panel.add(charts[LONG_CHART]);

    //create chart for Transverse
    charts[TRANS_CHART] = new Chart();
    charts[TRANS_CHART].init(
             "Transverse", TRANS_CHART, 2, true, 50, CHART_WIDTH, CHART_HEIGHT);
    panel.add(charts[TRANS_CHART]);
  
    //sample trace draws the sample points without connecting them
    charts[TRANS_CHART].setTraceConnectPoints(0, SAMPLE_TRACE, false);

    //create chart for Transverse
    charts[WALL_CHART] = new Chart();
    charts[WALL_CHART].init(
                    "Wall", WALL_CHART, 1, false, 0, CHART_WIDTH, CHART_HEIGHT);
    panel.add(charts[WALL_CHART]);
    
  */  
    
    
}// end of Chart::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::createCharts
//
// Creates and configures the charts.
//

private void createCharts()
{

    charts = new Chart[numCharts];
    
    for (int i = 0; i<charts.length; i++){
        charts[i] = new Chart();
        charts[i].init(index, i, graphWidth, graphHeight, configFile);
        add(charts[i]);
    }
    
}// end of ChartGroup::createCharts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::loadConfigSettings
//
// Loads settings for the object from configFile.
//

private void loadConfigSettings()
{

    String section = "Chart Group " + index;
    
    title = configFile.readString(
                                section, "title", "Chart Group " + (index + 1));

    shortTitle = configFile.readString(
                                section, "short title", "chgrp" + (index + 1));    
        
    numCharts = configFile.readInt(section, "number of charts", 0);
    
    graphWidth = configFile.readInt(section, "default width for all graphs", 0);

    graphHeight = configFile.readInt(
                                  section, "default height for all graphs", 0);

}// end of Chart::loadConfigSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::repaintGroup
//
// Forces all charts to be repainted.
//

public void repaintGroup()
{

    invalidate();
    
}// end of Chart::repaintGroup
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::resetAllTraceData
//
// For all traces of all charts - resets all data to zero and all flags to
// DEFAULT_FLAGS. Resets dataInsertPos to zero.
//

public void resetAllTraceData()
{

    for (Chart chart: charts){ chart.resetAllTraceData(); }

}// end of ChartGroup::resetAllTraceData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::insertDataPointInTrace
//
// Adds data point pData to pTrace of pGraph of pChart.
//

public void insertDataPointInTrace(int pChart, int pGraph,
                                                        int pTrace, int pData)
{        

    charts[pChart].insertDataPointInTrace(pGraph, pTrace, pData);
    
}// end of ChartGroup::insertDataPointInTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::setTraceFlags
//
// Sets flag(s) pFlags at index pIndex of pTrace of pGraph of pChart.
//
// Each bit of pFlags represents a different flag.
//

public void setTraceFlags(int pChart, int pGraph, int pTrace,
                                                        int pIndex, int pFlags)
{        

    charts[pChart].setTraceFlags(pGraph, pTrace, pIndex, pFlags);

}// end of ChartGroup::setTraceFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::setTraceFlagsAtCurrentInsertionPoint
//
// Sets flag(s) pFlags at the current insertion point of pTrace of pGraph of
// pChart.
//
// Each bit of pFlags represents a different flag.
//

public void setTraceFlagsAtCurrentInsertionPoint(
                                int pChart, int pGraph, int pTrace, int pFlags)
{        

    charts[pChart].setTraceFlagsAtCurrentInsertionPoint(pGraph, pTrace, pFlags);

}// end of ChartGroup::setTraceFlagsAtCurrentInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::setAllChartAllTraceXScale
//
// Sets the display horizontal scale for all traces of all charts to pScale.
//

public void setAllChartAllTraceXScale(double pScale)
{
    
    for (Chart chart : charts) { chart.setAllTraceXScale(pScale);}

}// end of ChartGroup::setAllChartAllTraceXScale
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::getChart
//
// Returns Chart pChart.
//

public Chart getChart(int pChart)
{

    if (pChart < 0 || pChart >= charts.length){ return(null); }

    return(charts[pChart]);

}// end of ChartGroup::getChart
//-----------------------------------------------------------------------------

}//end of class ChartGroup
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------