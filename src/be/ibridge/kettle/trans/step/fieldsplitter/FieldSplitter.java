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
 
package be.ibridge.kettle.trans.step.fieldsplitter;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransMeta;
import be.ibridge.kettle.trans.step.BaseStep;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.StepInterface;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;
import be.ibridge.kettle.trans.step.textfileinput.TextFileInput;
import be.ibridge.kettle.trans.step.textfileinput.TextFileInputMeta;


/**
 * Split a single String fields into multiple parts based on certain conditions.
 * 
 * @author Matt
 * @since 31-Okt-2003
 */
 public class FieldSplitter extends BaseStep implements StepInterface
{
	private FieldSplitterMeta meta;
	private FieldSplitterData data;
	
	public FieldSplitter(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	private boolean splitField(Row r)
	{
		if (first)
		{
			data.fieldnr=r.searchValueIndex(meta.getSplitField());
			if (data.fieldnr<0)
			{
				logError(Messages.getString("FieldSplitter.Log.CouldNotFindFieldToSplit",meta.getSplitField())); //$NON-NLS-1$ //$NON-NLS-2$
				setErrors(1);
				stopAll();
				return false;
			}
		}
		Value v=r.getValue(data.fieldnr);
		r.removeValue(data.fieldnr); 
		boolean insert = data.fieldnr<r.size();
		
		if (!v.isString())
		{
			logError(Messages.getString("FieldSplitter.Log.SplitFieldNotValid",meta.getSplitField())); //$NON-NLS-1$ //$NON-NLS-2$
			setErrors(1);
			stopAll();
			return false;
		}
		
		// Named values info.id[0] not filled in!
		boolean use_ids = meta.getFieldID().length>0 && meta.getFieldID()[0]!=null && meta.getFieldID()[0].length()>0;
		
		Value value=null;
		if (use_ids)
		{
			if (log.isDebug()) logDebug(Messages.getString("FieldSplitter.Log.UsingIds")); //$NON-NLS-1$
			// pol all split fields
			// Loop over the specified field list
			// If we spot the corresponding id[] entry in pol, add the value
			//
			String pol[] = new String[meta.getField().length];
			int prev=0;
			int i=0;
			while(v.getString()!=null && prev<v.getString().length() && i<pol.length)
			{
				pol[i]=polNext(v.getString(), meta.getDelimiter(), prev);
				if (log.isDebug()) logDebug(Messages.getString("FieldSplitter.Log.SplitFieldsInfo",pol[i],String.valueOf(prev))); //$NON-NLS-1$ //$NON-NLS-2$
				prev+=pol[i].length()+meta.getDelimiter().length();
				i++;
			}

			// We have to add info.field.length variables!
			for (i=0;i<meta.getField().length;i++)
			{
				// We have a field, search the corresponding pol[] entry.
				String split=null;

				for (int p=0; p<pol.length && split==null; p++) 
				{
					// With which line does pol[p] correspond?
					if (pol[p]!=null && pol[p].indexOf(meta.getFieldID()[i])>=0) split=pol[p];
				}
				
				// Optionally remove the indicator				
				if (split!=null && meta.removeID()[i])
				{
					StringBuffer sb = new StringBuffer(split);
					int idx = sb.indexOf(meta.getFieldID()[i]);
					sb.delete(idx, idx+meta.getFieldID()[i].length());
					split=sb.toString();
				}

				if (split==null) split=""; //$NON-NLS-1$
				if (log.isDebug()) logDebug(Messages.getString("FieldSplitter.Log.SplitInfo")+split); //$NON-NLS-1$

				try
				{
					value = TextFileInput.convertValue
					(
						split,
						meta.getField()[i],
						meta.getFieldType()[i],
						meta.getFieldFormat()[i],
						meta.getFieldLength()[i],
						meta.getFieldPrecision()[i],
						meta.getFieldGroup()[i],
						meta.getFieldDecimal()[i],
						meta.getFieldCurrency()[i],
						meta.getFieldDefault()[i],
						"", // --> The default String value in case a field is empty. //$NON-NLS-1$
						TextFileInputMeta.TYPE_TRIM_BOTH,
						data.df, data.dfs,
						data.daf, data.dafs
					);
				}
				catch(Exception e)
				{
					logError(Messages.getString("FieldSplitter.Log.ErrorConvertingSplitValue",split,meta.getSplitField()+"]!")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					setErrors(1);
					stopAll();
					return false;
				}
				if (insert) r.addValue(data.fieldnr+i, value); else r.addValue(value);
			}
		}
		else
		{
			if (log.isDebug()) logDebug(Messages.getString("FieldSplitter.Log.UsingPositionOfValue")); //$NON-NLS-1$
			int prev=0;
			for (int i=0;i<meta.getField().length;i++)
			{
				String pol = polNext(v.getString(), meta.getDelimiter(), prev);
				if (log.isDebug()) logDebug(Messages.getString("FieldSplitter.Log.SplitFieldsInfo",pol,String.valueOf(prev))); //$NON-NLS-1$ //$NON-NLS-2$
				prev+=(pol==null?0:pol.length()) + meta.getDelimiter().length();
				
				try
				{
					value = TextFileInput.convertValue
					(
						pol,
						meta.getField()[i],
						meta.getFieldType()[i],
						meta.getFieldFormat()[i],
						meta.getFieldLength()[i],
						meta.getFieldPrecision()[i],
						meta.getFieldGroup()[i],
						meta.getFieldDecimal()[i],
						meta.getFieldCurrency()[i],
						meta.getFieldDefault()[i],
						"", // --> The default String value in case a field is empty. //$NON-NLS-1$
						TextFileInputMeta.TYPE_TRIM_BOTH,
						data.df, data.dfs,
						data.daf, data.dafs
					);
				}
				catch(Exception e)
				{
					logError(Messages.getString("FieldSplitter.Log.ErrorConvertingSplitValue",pol,meta.getSplitField()+"]!")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					setErrors(1);
					stopAll();
					return false;
				}
				if (insert) r.addValue(data.fieldnr+i, value); else r.addValue(value);
			}
		}
		
		return true;
	}
	
	private static final String polNext(String str, String del, int start)
	{
		String retval;
		
		if (str==null || start>=str.length()) return ""; //$NON-NLS-1$
		
		int next = str.indexOf(del, start);
		if (next == start) // ;; or ,, : two consecutive delimiters
		{
			retval=""; //$NON-NLS-1$
		}
		else 
		if (next > start) // part of string
		{
			retval=str.substring(start, next);
		}
		else // Last field in string
		{
			retval=str.substring(start);
		}
		return retval;
	}
	
	public synchronized boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(FieldSplitterMeta)smi;
		data=(FieldSplitterData)sdi;

		Row r=getRow();   // get row from rowset, wait for our turn, indicate busy!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		boolean noerr = splitField(r);
		if (!noerr)
		{
			setOutputDone();
			return false;
		}
				
		putRow(r);

        if (checkFeedback(linesRead)) logBasic(Messages.getString("FieldSplitter.Log.LineNumber")+linesRead); //$NON-NLS-1$
			
		return true;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(FieldSplitterMeta)smi;
		data=(FieldSplitterData)sdi;
		
		if (super.init(smi, sdi))
		{
		    // Add init code here.
		    return true;
		}
		return false;
	}

			
	//
	// Run is were the action happens!
	//
	public void run()
	{
		try
		{
			logBasic(Messages.getString("FieldSplitter.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("FieldSplitter.Log.UnexpectedError")+" : "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
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
