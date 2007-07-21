/**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/

package org.pentaho.di.trans.steps.scriptvalues_mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.trans.RowProducer;
import org.pentaho.di.trans.RowStepCollector;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.dummytrans.DummyTransMeta;
import org.pentaho.di.trans.steps.injector.InjectorMeta;


/**
 * Test class for the Modified Javascript step. Things tested:
 * ltrim(), rtrim(), trim().
 *
 * @author Sven Boden
 */
public class JavaScriptStringTest extends TestCase
{
	public RowMetaInterface createRowMetaInterface1()
	{
		RowMetaInterface rm = new RowMeta();
		
		ValueMetaInterface valuesMeta[] = {
			    new ValueMeta("string", ValueMeta.TYPE_STRING),
	    };

		for (int i=0; i < valuesMeta.length; i++ )
		{
			rm.addValueMeta(valuesMeta[i]);
		}
		
		return rm;
	}
	
	public List<RowMetaAndData> createData()
	{
		List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();	
		
		RowMetaInterface rm = createRowMetaInterface1();
		
		Object[] r1 = new Object[] { null }; 
		Object[] r2 = new Object[] { "" };
		Object[] r3 = new Object[] { "    " };
		Object[] r4 = new Object[] { "small" };
		Object[] r5 = new Object[] { "longer string" };
		Object[] r6 = new Object[] { "spaces right    " };
		Object[] r7 = new Object[] { "   spaces left" };
		Object[] r8 = new Object[] { "   spaces   " };
		
		list.add(new RowMetaAndData(rm, r1));
		list.add(new RowMetaAndData(rm, r2));
		list.add(new RowMetaAndData(rm, r3));
		list.add(new RowMetaAndData(rm, r4));
		list.add(new RowMetaAndData(rm, r5));
		list.add(new RowMetaAndData(rm, r6));
		list.add(new RowMetaAndData(rm, r7));
		list.add(new RowMetaAndData(rm, r8));
		
		return list;
	}
	
	
	/**
	 * Create the meta data for the results (ltrim/rtrim/trim).
	 */
	public RowMetaInterface createRowMetaInterfaceResult1()
	{
		RowMetaInterface rm = new RowMeta();
		
		ValueMetaInterface valuesMeta[] = {
			    new ValueMeta("string",   ValueMeta.TYPE_STRING),
			    new ValueMeta("original", ValueMeta.TYPE_STRING),
			    new ValueMeta("ltrimStr", ValueMeta.TYPE_STRING),
			    new ValueMeta("rtrimStr", ValueMeta.TYPE_STRING),
			    new ValueMeta("trimStr",  ValueMeta.TYPE_STRING),
	    };

		for (int i=0; i < valuesMeta.length; i++ )
		{
			rm.addValueMeta(valuesMeta[i]);
		}
		
		return rm;
	}	
	
	/**
	 * Create result data for test case 1.
	 */
	public List<RowMetaAndData> createResultData1()
	{
		List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();	
		
		RowMetaInterface rm = createRowMetaInterfaceResult1();
		
		Object[] r1 = new Object[] { null, "bnulle", "bnulle", "bnulle", "bnulle" };
		Object[] r2 = new Object[] { null, "bnulle", "bnulle", "bnulle", "bnulle" };
		Object[] r3 = new Object[] { "    ",  "b    e", "be", "be", "be"  };
		Object[] r4 = new Object[] { "small", "bsmalle", "bsmalle", "bsmalle", "bsmalle" };	
		Object[] r5 = new Object[] { "longer string", "blonger stringe", "blonger stringe", "blonger stringe", "blonger stringe" };
		Object[] r6 = new Object[] { "spaces right    ", "bspaces right    e", "bspaces right    e", "bspaces righte", "bspaces righte" };
		Object[] r7 = new Object[] { "   spaces left", "b   spaces lefte", "bspaces lefte", "b   spaces lefte", "bspaces lefte" };
		Object[] r8 = new Object[] { "   spaces   ", "b   spaces   e", "bspaces   e", "b   spacese", "bspacese" };
		
		list.add(new RowMetaAndData(rm, r1));
		list.add(new RowMetaAndData(rm, r2));
		list.add(new RowMetaAndData(rm, r3));
		list.add(new RowMetaAndData(rm, r4));
		list.add(new RowMetaAndData(rm, r5));
		list.add(new RowMetaAndData(rm, r6));
		list.add(new RowMetaAndData(rm, r7));
		list.add(new RowMetaAndData(rm, r8));	
		
		return list;
	}	
	
	
	/**
	 *  Check the 2 lists comparing the rows in order.
	 *  If they are not the same fail the test. 
	 */
    public void checkRows(List<RowMetaAndData> rows1, List<RowMetaAndData> rows2)
    {
    	int idx = 1;
        if ( rows1.size() != rows2.size() )
        {
        	fail("Number of rows is not the same: " + 
          		 rows1.size() + " and " + rows2.size());
        }
        Iterator<RowMetaAndData> it1 = rows1.iterator();
        Iterator<RowMetaAndData> it2 = rows2.iterator();
        
        while ( it1.hasNext() && it2.hasNext() )
        {
        	RowMetaAndData rm1 = it1.next();
        	RowMetaAndData rm2 = it2.next();
        	
        	Object[] r1 = rm1.getData();
        	Object[] r2 = rm2.getData();
        	
        	if ( rm1.size() != rm2.size() )
        	{
        		fail("row nr " + idx + " is not equal");
        	}
        	int fields[] = new int[rm1.size()];
        	for ( int ydx = 0; ydx < rm1.size(); ydx++ )
        	{
        		fields[ydx] = ydx;
        	}
            try {
				if ( rm1.getRowMeta().compare(r1, r2, fields) != 0 )
				{
					fail("row nr " + idx + " is not equal");
				}
			} catch (KettleValueException e) {
				fail("row nr " + idx + " is not equal");
			}
            	
            idx++;
        }
    }

    
	/**
	 * Test case for javascript functionality: ltrim(), rtrim(), trim().
	 */
    public void testStringsTrim() throws Exception
    {
        EnvUtil.environmentInit();

        //
        // Create a new transformation...
        //
        TransMeta transMeta = new TransMeta();
        transMeta.setName("javascripttest1");
    	
        StepLoader steploader = StepLoader.getInstance();            

        // 
        // create an injector step...
        //
        String injectorStepname = "injector step";
        InjectorMeta im = new InjectorMeta();
        
        // Set the information of the injector.                
        String injectorPid = steploader.getStepPluginID(im);
        StepMeta injectorStep = new StepMeta(injectorPid, injectorStepname, (StepMetaInterface)im);
        transMeta.addStep(injectorStep);                       

        // 
        // Create a javascript step
        //
        String javaScriptStepname = "blocking step";            
        ScriptValuesMetaMod svm = new ScriptValuesMetaMod();
        
        ScriptValuesScript[] js = new ScriptValuesScript[] {new ScriptValuesScript(ScriptValuesScript.TRANSFORM_SCRIPT,
        		                                              "script",
                                                              "var original = 'b' + string.getString() + 'e';\n" +
                                                              "var ltrimStr = 'b' + ltrim(string.getString()) + 'e';\n" +
                                                              "var rtrimStr = 'b' + rtrim(string.getString()) + 'e';\n" +
                                                              "var trimStr  = 'b' + trim(string.getString()) + 'e';\n") };
        svm.setJSScripts(js);
        svm.setName(new String[] { "original", "ltrimStr", "rtrimStr", "trimStr" });
        svm.setRename(new String[] { "", "", "", "" });
        svm.setType(new int[] { ValueMetaInterface.TYPE_STRING,
        		                ValueMetaInterface.TYPE_STRING,
        		                ValueMetaInterface.TYPE_STRING,
        		                ValueMetaInterface.TYPE_STRING});        
        svm.setLength(new int[] { -1, -1, -1, -1 });
        svm.setPrecision(new int[] { -1, -1, -1, -1 });
        svm.setCompatible(true);

        String javaScriptStepPid = steploader.getStepPluginID(svm);
        StepMeta javaScriptStep = new StepMeta(javaScriptStepPid, javaScriptStepname, (StepMetaInterface)svm);
        transMeta.addStep(javaScriptStep);            

        TransHopMeta hi1 = new TransHopMeta(injectorStep, javaScriptStep);
        transMeta.addTransHop(hi1);        
        
        // 
        // Create a dummy step 
        //
        String dummyStepname = "dummy step";            
        DummyTransMeta dm = new DummyTransMeta();

        String dummyPid = steploader.getStepPluginID(dm);
        StepMeta dummyStep = new StepMeta(dummyPid, dummyStepname, (StepMetaInterface)dm);
        transMeta.addStep(dummyStep);                              

        TransHopMeta hi2 = new TransHopMeta(javaScriptStep, dummyStep);
        transMeta.addTransHop(hi2);        
        
        // Now execute the transformation...
        Trans trans = new Trans(transMeta);

        trans.prepareExecution(null);
                
        StepInterface si;

        si = trans.getStepInterface(javaScriptStepname, 0);
        RowStepCollector javaScriptRc = new RowStepCollector();
        si.addRowListener(javaScriptRc);
               
        si = trans.getStepInterface(dummyStepname, 0);
        RowStepCollector dummyRc = new RowStepCollector();
        si.addRowListener(dummyRc);
        
        RowProducer rp = trans.addRowProducer(injectorStepname, 0);
        trans.startThreads();
        
        // add rows
        List<RowMetaAndData> inputList = createData();
        Iterator<RowMetaAndData> it = inputList.iterator();
        while ( it.hasNext() )
        {
        	RowMetaAndData rm = (RowMetaAndData)it.next();
        	rp.putRow(rm.getRowMeta(), rm.getData());
        }   
        rp.finished();

        trans.waitUntilFinished();   
        
        List<RowMetaAndData> goldenImageRows = createResultData1();
        List<RowMetaAndData> resultRows1 = javaScriptRc.getRowsWritten();
        checkRows(resultRows1, goldenImageRows);
                
        List<RowMetaAndData> resultRows2 = dummyRc.getRowsRead();
        checkRows(resultRows2, goldenImageRows);
    }    
}