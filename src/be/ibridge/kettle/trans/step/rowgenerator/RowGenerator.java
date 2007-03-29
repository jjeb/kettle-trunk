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
 
package be.ibridge.kettle.trans.step.rowgenerator;

import java.math.BigDecimal;
import java.util.ArrayList;

import be.ibridge.kettle.core.CheckResult;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.util.StringUtil;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.core.value.ValueInterface;
import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransMeta;
import be.ibridge.kettle.trans.step.BaseStep;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.StepInterface;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;


/**
 * Generates a number of (empty or the same) rows
 * 
 * @author Matt
 * @since 4-apr-2003
 */
public class RowGenerator extends BaseStep implements StepInterface
{
	private RowGeneratorMeta meta;
	private RowGeneratorData data;
	
	public RowGenerator(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
		
		meta=(RowGeneratorMeta)getStepMeta().getStepMetaInterface();
		data=(RowGeneratorData)stepDataInterface;
	}
	
    public static final Row buildRow(RowGeneratorMeta meta, RowGeneratorData data, ArrayList remarks)
    {
        Row r=new Row();
        Value value;

        for (int i=0;i<meta.getFieldName().length;i++)
        {
            int valtype = Value.getType(meta.getFieldType()[i]); 
            if (meta.getFieldName()[i]!=null)
            {
                value=new Value(meta.getFieldName()[i], valtype); // build a value!
                value.setLength(meta.getFieldLength()[i], meta.getFieldPrecision()[i]);
                String stringValue = meta.getValue()[i];
                
                // If the value is empty: consider it to be NULL.
                if (stringValue==null || stringValue.length()==0)
                {
                    value.setNull();
                    
                    if ( value.getType() == Value.VALUE_TYPE_NONE )
                    {
                        String message = Messages.getString("RowGenerator.CheckResult.SpecifyTypeError", value.getName(), value.getString());
                        remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, null));                    
                    }
                }
                else
                {
                    switch(value.getType())
                    {
                    case Value.VALUE_TYPE_NUMBER:
                        try
                        {
                            if (meta.getFieldFormat()[i]!=null || meta.getDecimal()[i] !=null ||
                            meta.getGroup()[i]       !=null || meta.getCurrency()[i]!=null    
                            )
                            {
                                if (meta.getFieldFormat()[i]!=null && meta.getFieldFormat()[i].length()>=1) data.df.applyPattern(meta.getFieldFormat()[i]);
                                if (meta.getDecimal()[i] !=null && meta.getDecimal()[i].length()>=1) data.dfs.setDecimalSeparator( meta.getDecimal()[i].charAt(0) );
                                if (meta.getGroup()[i]   !=null && meta.getGroup()[i].length()>=1) data.dfs.setGroupingSeparator( meta.getGroup()[i].charAt(0) );
                                if (meta.getCurrency()[i]!=null && meta.getCurrency()[i].length()>=1) data.dfs.setCurrencySymbol( meta.getCurrency()[i] );
                                
                                data.df.setDecimalFormatSymbols(data.dfs);
                            }
                            
                            value.setValue( data.nf.parse(stringValue).doubleValue() );
                        }
                        catch(Exception e)
                        {
                            String message = Messages.getString("RowGenerator.BuildRow.Error.Parsing.Number", value.getName(), stringValue, e.toString() );
                            remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, null));
                        }
                        break;
                    case Value.VALUE_TYPE_STRING:
                        value.setValue(stringValue);
                        break;
                    case Value.VALUE_TYPE_DATE:
                        try
                        {
                            if (meta.getFieldFormat()[i]!=null)
                            {
                                data.daf.applyPattern(meta.getFieldFormat()[i]);
                                data.daf.setDateFormatSymbols(data.dafs);
                            }
                            
                            value.setValue( data.daf.parse(stringValue) );
                        }
                        catch(Exception e)
                        {
                            String message = Messages.getString("RowGenerator.BuildRow.Error.Parsing.Date", value.getName(), stringValue, e.toString() );
                            remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, null));
                        }
                        break;
                        
                    case Value.VALUE_TYPE_INTEGER:
                        try
                        {
                            value.setValue( Long.parseLong(stringValue) );
                        }
                        catch(Exception e)
                        {
                            String message = Messages.getString("RowGenerator.BuildRow.Error.Parsing.Integer", value.getName(), stringValue, e.toString() );
                            remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, null));
                        }
                        break;
    
                    case Value.VALUE_TYPE_BIGNUMBER:
                        try
                        {
                            value.setValue( new BigDecimal(stringValue) );
                        }
                        catch(Exception e)
                        {
                            String message = Messages.getString("RowGenerator.BuildRow.Error.Parsing.BigNumber", value.getName(), stringValue, e.toString() );
                            remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, null));
                        }
                        break;
                        
                    case Value.VALUE_TYPE_BOOLEAN:
                        value.setValue( "Y".equalsIgnoreCase(stringValue) || "TRUE".equalsIgnoreCase(stringValue));
                        break;
                        
                    default:
                        String message = Messages.getString("RowGenerator.CheckResult.SpecifyTypeError", value.getName(), value.getString());
                        remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, null));
                    }
                }
                // Now add value to the row!
                // This is in fact a copy from the fields row, but now with data.
                r.addValue(value); 
            }
        }
        
        return r;
    }
    	
	public synchronized boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
        meta=(RowGeneratorMeta)smi;
        data=(RowGeneratorData)sdi;

		Row r=null;
		boolean retval=true;
		
		if (linesWritten<data.rowLimit)
		{
			r=new Row(); // Copy the data, otherwise it gets manipulated aferwards.
            for (int i=0;i<data.constants.size();i++)
            {
                Value value = data.constants.getValue(i);

                Value copy = new Value(value.getName());
                copy.setLength(value.getLength(), value.getPrecision());
                copy.setNull(value.isNull());
                copy.setValueInterface( (ValueInterface) value.getValueInterface().clone() );
                
                r.addValue(copy);
            }
		}
		else
		{
			setOutputDone();  // signal end to receiver(s)
			return false;
		}
		
		putRow(r);

        if (log.isRowLevel())
        {
            log.logRowlevel(toString(), Messages.getString("RowGenerator.Log.Wrote.Row", Long.toString(linesWritten), r.toString()) );
        }
        
        if (checkFeedback(linesRead)) logBasic( Messages.getString("RowGenerator.Log.LineNr", Long.toString(linesWritten) ) );
		
		return retval;
	}

    public boolean init(StepMetaInterface smi, StepDataInterface sdi)
    {
        meta=(RowGeneratorMeta)smi;
        data=(RowGeneratorData)sdi;
        
        if (super.init(smi, sdi))
        {
            // Determine the number of rows to generate...
            data.rowLimit = Const.toLong(StringUtil.environmentSubstitute(meta.getRowLimit()), -1L);
            
            if (data.rowLimit<0L) // Unable to parse
            {
                logError(Messages.getString("RowGenerator.Wrong.RowLimit.Number"));
                return false; // fail
            }
            
            // Create a row (constants) with all the values in it...
            ArrayList remarks = new ArrayList(); // stores the errors...
            data.constants = buildRow(meta, data, remarks);
            if (remarks.size()==0) 
            { 
                return true;
            }
            else
            {
                for (int i=0;i<remarks.size();i++)
                {
                    CheckResult cr = (CheckResult) remarks.get(i);
                    logError(cr.getText());
                }
            }
        }
        return false;
    }


	//
	// Run is were the action happens!
	public void run()
	{
		try
		{
			logBasic(Messages.getString("RowGenerator.Log.StartToRun"));
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError("Unexpected error : "+e.toString());
            logError(Const.getStackTracker(e));
            setErrors(1);
			stopAll();
		}
		finally
		{
			dispose(meta, data);
			logSummary();
			markStop();
		}
	}
}
