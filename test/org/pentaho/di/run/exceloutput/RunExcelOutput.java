package org.pentaho.di.run.exceloutput;

import junit.framework.TestCase;

import org.pentaho.di.core.Result;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.run.AllRunTests;
import org.pentaho.di.run.TimedTransRunner;

public class RunExcelOutput extends TestCase
{
    public void test_EXCEL_OUTPUT_00()
    {
        System.out.println();
        System.out.println("EXCEL OUTPUT");
        System.out.println("==================");
    }
    
    public void test_EXCEL_OUTPUT_01_Simple() throws Exception
    {
        TimedTransRunner timedTransRunner = new TimedTransRunner(
                "test/org/pentaho/di/run/exceloutput/ExcelOutputDataFromGenerator.ktr", 
                LogWriter.LOG_LEVEL_ERROR, 
                AllRunTests.getOldTargetDatabase(),
                AllRunTests.getNewTargetDatabase(),
                10000
            );
        timedTransRunner.runOldAndNew();
        
        be.ibridge.kettle.core.Result oldResult = timedTransRunner.getOldResult();
        assertTrue(oldResult.getNrErrors()==0);
        
        Result newResult = timedTransRunner.getNewResult();
        assertTrue(newResult.getNrErrors()==0);
    }
}
