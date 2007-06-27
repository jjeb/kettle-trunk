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
 
package org.pentaho.di.trans.steps.groupby;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueDataUtil;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Groups informations based on aggregation rules. (sum, count, ...)
 * 
 * @author Matt
 * @since 2-jun-2003
 */
public class GroupBy extends BaseStep implements StepInterface
{
	private GroupByMeta meta;
	private GroupByData data;
	
	public GroupBy(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
		
		meta=(GroupByMeta)getStepMeta().getStepMetaInterface();
		data=(GroupByData)stepDataInterface;
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
        meta=(GroupByMeta)smi;
        data=(GroupByData)sdi;
        
        Object[] r=getRow();    // get row!
        
		if (first)
		{
			// get the RowMeta
			data.previousMeta = (RowMetaInterface) getInputRowMeta().clone();
			//do not set first=false, below is another part that uses first
		}
		
		if (r==null)  // no more input to be expected...
		{
            if (meta.passAllRows())  // ALL ROWS
            {
                if (data.previous!=null)
                {
                    calcAggregate(data.previous);
                    addToBuffer(data.previous);
                }
                data.groupResult = getAggregateResult();

                Object[] row = getRowFromBuffer();
                RowMetaInterface rowMeta=(RowMetaInterface)data.previousMeta.clone();
                long lineNr=0;
                while (row!=null)
                {
                	row=RowDataUtil.addRowData(row, data.groupResult);
                	rowMeta.addRowMeta(data.aggMeta); // aggRowMeta is Meta for groupResult
                    lineNr++;
                    
                    if (meta.isAddingLineNrInGroup() && !Const.isEmpty(meta.getLineNrInGroupField()))
                    {
                    	Object lineNrValue= new Long(lineNr);
                    	ValueMetaInterface lineNrValueMeta = new ValueMeta(meta.getLineNrInGroupField(), ValueMetaInterface.TYPE_INTEGER);
                    	lineNrValueMeta.setLength(9);
                    	row=RowDataUtil.addValueData(row, lineNrValue);
                    	rowMeta.addValueMeta(lineNrValueMeta);
                    }
                    
                    putRow(rowMeta, row);
                    row = getRowFromBuffer();
                }
                closeInput();
            }
            else   // JUST THE GROUP + AGGREGATE
            {
    			// Don't forget the last set of rows...
    			if (data.previous!=null) 
    			{
    				calcAggregate(data.previous);
    				
    				Object[] result = buildResult(data.previous);
    				putRow(data.groupAggMeta, result);
    			} 
    		}
			setOutputDone();
			return false;
		}
		
		//System.out.println("r = "+r);
		
		if (first)
		{
			data.counts     = new long[meta.getSubjectField().length];
			data.subjectnrs = new int[meta.getSubjectField().length];
			for (int i=0;i<meta.getSubjectField().length;i++)
			{
				data.subjectnrs[i] = data.previousMeta.indexOfValue(meta.getSubjectField()[i]);
				if (data.subjectnrs[i]<0)
				{
					logError(Messages.getString("GroupBy.Log.AggregateSubjectFieldCouldNotFound",meta.getSubjectField()[i])); //$NON-NLS-1$ //$NON-NLS-2$
					setErrors(1);
					stopAll();
					return false;
				}
			}
			
			data.groupnrs = new int[meta.getGroupField().length];
			for (int i=0;i<meta.getGroupField().length;i++)
			{
				data.groupnrs[i] = data.previousMeta.indexOfValue(meta.getGroupField()[i]);
				if (data.groupnrs[i]<0)
				{
					logError(Messages.getString("GroupBy.Log.GroupFieldCouldNotFound",meta.getGroupField()[i])); //$NON-NLS-1$ //$NON-NLS-2$
					setErrors(1);
					stopAll();
					return false;
				}				
			}
			initGroupMeta(data.previousMeta);

			data.previous=data.previousMeta.cloneRow(r); // copy the row to previous
			newAggregate(r);         // Create a new group aggregate (init)

			// for speed: groupMeta+aggMeta
			data.groupAggMeta=new RowMeta();
			data.groupAggMeta.addRowMeta(data.groupMeta);
			data.groupAggMeta.addRowMeta(data.aggMeta);

			first=false;
		}
		else
		{
			calcAggregate(data.previous);
			//System.out.println("After calc, agg="+agg);

			if (meta.passAllRows())
			{
                addToBuffer(data.previous);
			}
		}
				
		// System.out.println("Check for same group...");
        
		if (!sameGroup(data.previous, r))
		{
            // System.out.println("Different group!");
            
            if (meta.passAllRows())
            {
                // System.out.println("Close output...");
                
                // Not the same group: close output (if any)
                closeOutput();

                // System.out.println("getAggregateResult()");

                // Get all rows from the buffer!
                data.groupResult = getAggregateResult();

                // System.out.println("dump rows from the buffer");

                Object[] row = getRowFromBuffer();
                RowMetaInterface rowMeta=(RowMetaInterface)data.previousMeta.clone();
                long lineNr=0;
                while (row!=null)
                {
                	row=RowDataUtil.addRowData(row, data.groupResult);
                	rowMeta.addRowMeta(data.aggMeta); // aggRowMeta is Meta for groupResult
                    lineNr++;
                    
                    if (meta.isAddingLineNrInGroup() && !Const.isEmpty(meta.getLineNrInGroupField()))
                    {
                    	Object lineNrValue= new Long(lineNr);
                    	ValueMetaInterface lineNrValueMeta = new ValueMeta(meta.getLineNrInGroupField(), ValueMetaInterface.TYPE_INTEGER);
                    	lineNrValueMeta.setLength(9);
                    	row=RowDataUtil.addValueData(row, lineNrValue);
                    	rowMeta.addValueMeta(lineNrValueMeta);

                    }
                    putRow(rowMeta, row);
                    row = getRowFromBuffer();
                }
                closeInput();
            }
            else
            {
    			Object[] result = buildResult(data.previous);
    			putRow(data.groupAggMeta, result);        // copy row to possible alternate rowset(s).
            }
            newAggregate(r);       // Create a new group aggregate (init)
		}

		data.previous=data.previousMeta.cloneRow(r);

		if ((linesRead>0) && (linesRead%Const.ROWS_UPDATE)==0) logBasic(Messages.getString("GroupBy.LineNumber")+linesRead); //$NON-NLS-1$
			
		return true;
	}
	
	// Is the row r of the same group as previous?
	private boolean sameGroup(Object[] previous, Object[] r) throws KettleValueException
	{
		for (int i=0;i<data.groupnrs.length;i++)
		{
			Object prev = previous[data.groupnrs[i]];
			Object curr = r[data.groupnrs[i]];
			
			if (data.previousMeta.getValueMeta(data.groupnrs[i]).compare(prev, curr)!=0) return false;  //TODO JB can we use this "equals"?
		}
		
		return true;
	}
	
	// Calculate the aggregates in the row...
	private void calcAggregate(Object[] r) throws KettleValueException
	{
		for (int i=0;i<data.subjectnrs.length;i++)
		{
			Object subj = r[data.subjectnrs[i]];
			ValueMetaInterface subjMeta=data.previousMeta.getValueMeta(data.subjectnrs[i]);
			Object value = data.agg[i];
			ValueMetaInterface valueMeta=data.aggMeta.getValueMeta(i);
			
			//System.out.println("  calcAggregate value, i="+i+", agg.size()="+agg.size()+", subj="+subj+", value="+value);
			
			switch(meta.getAggregateType()[i])
			{
				case GroupByMeta.TYPE_GROUP_SUM            :
					data.agg[i]=ValueDataUtil.plus(valueMeta, value, subjMeta, subj);
					break; 
				case GroupByMeta.TYPE_GROUP_AVERAGE        :
					data.agg[i]=ValueDataUtil.plus(valueMeta, value, subjMeta, subj);
				data.counts[i]++;
					break; 
				case GroupByMeta.TYPE_GROUP_COUNT_ALL      :
					data.counts[i]++;
					break;
				case GroupByMeta.TYPE_GROUP_MIN            :
					if (subjMeta.compare(subj,valueMeta,value)<0) data.agg[i]=subj; 
					break; 
				case GroupByMeta.TYPE_GROUP_MAX            : 
					if (subjMeta.compare(subj,valueMeta,value)>0) data.agg[i]=subj; 
					break; 
                case GroupByMeta.TYPE_GROUP_FIRST          :
                    if (!(subj==null) && value==null) data.agg[i]=subj;
                    break; 
                case GroupByMeta.TYPE_GROUP_LAST           : 
                    if (!(subj==null)) data.agg[i]=subj; 
                    break; 
                case GroupByMeta.TYPE_GROUP_FIRST_INCL_NULL:
                	// This is on purpose. The calculation of the 
                	// first field is done when setting up a new group
                	// This is just the field of the first row
                    // if (linesWritten==0) value.setValue(subj);
                    break; 
                case GroupByMeta.TYPE_GROUP_LAST_INCL_NULL : 
                	data.agg[i]=subj; 
                    break; 
                case GroupByMeta.TYPE_GROUP_CONCAT_COMMA   :
                    if (!(subj==null)) 
                    {
                    	String vString=valueMeta.getString(value);
                        if (vString.length()>0) vString=vString+", "; //$NON-NLS-1$
                        data.agg[i]=vString+subjMeta.getString(subj);
                    }
                    break; 
				default: break;
			}
		}
	}

	// Initialize a group..
	private void newAggregate(Object[] r)
	{
		// Put all the counters at 0
		for (int i=0;i<data.counts.length;i++) data.counts[i]=0;
		
		data.agg = new Object[data.subjectnrs.length];
		data.aggMeta=new RowMeta();
		
		for (int i=0;i<data.subjectnrs.length;i++)
		{
			Object subj = r[data.subjectnrs[i]];
			ValueMetaInterface subjMeta=data.previousMeta.getValueMeta(data.subjectnrs[i]);
			Object v=null;
			ValueMetaInterface vMeta=null;
			switch(meta.getAggregateType()[i])
			{
				case GroupByMeta.TYPE_GROUP_SUM            : 
				case GroupByMeta.TYPE_GROUP_AVERAGE        :
				case GroupByMeta.TYPE_GROUP_COUNT_ALL      :
                    vMeta = new ValueMeta(meta.getAggregateField()[i], subjMeta.isNumeric()?subjMeta.getType():ValueMetaInterface.TYPE_NUMBER);
                    switch(subjMeta.getType())
                    {
                    case ValueMetaInterface.TYPE_BIGNUMBER: v=new BigDecimal("0"); break;
                    case ValueMetaInterface.TYPE_INTEGER:   v=new Long(0L); break;
                    case ValueMetaInterface.TYPE_NUMBER:    
                    default:                         v=new Double(0.0); break;
                    }
					break; 
				case GroupByMeta.TYPE_GROUP_FIRST :
				case GroupByMeta.TYPE_GROUP_LAST  :
				case GroupByMeta.TYPE_GROUP_FIRST_INCL_NULL :
				case GroupByMeta.TYPE_GROUP_LAST_INCL_NULL  :					
				case GroupByMeta.TYPE_GROUP_MIN             : 
				case GroupByMeta.TYPE_GROUP_MAX             : 
					vMeta = new ValueMeta(meta.getAggregateField()[i], subjMeta.getType());
					v = subj; 
					break;
                case GroupByMeta.TYPE_GROUP_CONCAT_COMMA   :
                    vMeta = new ValueMeta(meta.getAggregateField()[i], ValueMetaInterface.TYPE_STRING);
                    v = new String(""); //$NON-NLS-1$
                    break; 
				default: break;
			}
            
            if (meta.getAggregateType()[i]!=GroupByMeta.TYPE_GROUP_COUNT_ALL)
            {
            	vMeta.setLength(subjMeta.getLength(), subjMeta.getPrecision());
            }
			if (v!=null) data.agg[i]=v;
			data.aggMeta.addValueMeta(vMeta);
		}
	}
	
	private Object[] buildResult(Object[] r) throws KettleValueException
	{
		Object[] result = new Object[data.groupnrs.length];
		for (int i=0;i<data.groupnrs.length;i++)
		{
			result[i]=r[data.groupnrs[i]]; 
		}
		
		result=RowDataUtil.addRowData(result, getAggregateResult());
        
		return result;
	}

	private void initGroupMeta(RowMetaInterface previousRowMeta) throws KettleValueException
	{
		data.groupMeta=new RowMeta();
		for (int i=0;i<data.groupnrs.length;i++)
		{
			data.groupMeta.addValueMeta(previousRowMeta.getValueMeta(data.groupnrs[i]));
		}
        
		return;
	}

    private Object[] getAggregateResult() throws KettleValueException
    {
        Object[] result = new Object[data.subjectnrs.length];

        if (data.subjectnrs!=null)
        {
            for (int i=0;i<data.subjectnrs.length;i++)
            {
                Object ag = data.agg[i];
                switch(meta.getAggregateType()[i])
                {
                    case GroupByMeta.TYPE_GROUP_SUM            : break; 
                    case GroupByMeta.TYPE_GROUP_AVERAGE        :
                    		ag=ValueDataUtil.divide(data.aggMeta.getValueMeta(i), ag, 
                    				new ValueMeta("c",ValueMetaInterface.TYPE_INTEGER), new Long(data.counts[i])); 
                    		break;  //$NON-NLS-1$
                    case GroupByMeta.TYPE_GROUP_COUNT_ALL      : ag=new Long(data.counts[i]); break;
                    case GroupByMeta.TYPE_GROUP_MIN            : break; 
                    case GroupByMeta.TYPE_GROUP_MAX            : break; 
                    default: break;
                }
                result[i]=ag;
            }
        }

        return result;

    }
		
    private void addToBuffer(Object[] row) throws KettleFileException
    {
        //System.out.println("Add to buffer: "+row);
        
        data.bufferList.add(row);
        if (data.bufferList.size()>5000)
        {
            if (data.rowsOnFile==0)
            {
                try
                {
                    data.tempFile = File.createTempFile(meta.getPrefix(), ".tmp", new File(environmentSubstitute(meta.getDirectory()))); //$NON-NLS-1$
                    data.fos=new FileOutputStream(data.tempFile);
                    data.dos=new DataOutputStream(data.fos);
                    data.firstRead = true;
                }
                catch(IOException e)
                {
                    throw new KettleFileException(Messages.getString("GroupBy.Exception.UnableToCreateTemporaryFile"), e); //$NON-NLS-1$
                }
            }
            // OK, save the oldest rows to disk!
            Object[] oldest = (Object[]) data.bufferList.get(0);
            data.previousMeta.writeData(data.dos, oldest);
            data.bufferList.remove(0);
            data.rowsOnFile++;
        }
    }
    
    private Object[] getRowFromBuffer() throws KettleFileException
    {
        if (data.rowsOnFile>0)
        {
            if (data.firstRead)
            {
                // Open the inputstream first...
                try
                {
                    data.fis=new FileInputStream( data.tempFile );
                    data.dis=new DataInputStream( data.fis );
                    data.firstRead = false;
                }
                catch(IOException e)
                {
                    throw new KettleFileException(Messages.getString("GroupBy.Exception.UnableToReadBackRowFromTemporaryFile"), e); //$NON-NLS-1$
                }
            }
            
            // Read one row from the file!
            Object[] row=data.previousMeta.readData(data.dis);
            data.rowsOnFile--;
            
            return row;
        }
        else
        {
            if (data.bufferList.size()>0)
            {
            	Object[] row = (Object[])data.bufferList.get(0);
                data.bufferList.remove(0);
                return row;
            }
            else
            {
                return null; // Nothing left!
            }
        }
    }
    
    private void closeOutput() throws KettleFileException
    {
        try
        {
            if (data.dos!=null) { data.dos.close(); data.dos=null; }
            if (data.fos!=null) { data.fos.close(); data.fos=null; }
            data.firstRead = true;
        }
        catch(IOException e)
        {
            throw new KettleFileException(Messages.getString("GroupBy.Exception.UnableToCloseInputStream"), e); //$NON-NLS-1$
        }
    }
    
    private void closeInput() throws KettleFileException
    {
        try
        {
            if (data.fis!=null) { data.fis.close(); data.fis=null; }
            if (data.dis!=null) { data.dis.close(); data.dis=null; }
        }
        catch(IOException e)
        {
            throw new KettleFileException(Messages.getString("GroupBy.Exception.UnableToCloseInputStream"), e); //$NON-NLS-1$
        }
    }

    
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(GroupByMeta)smi;
		data=(GroupByData)sdi;
		
		if (super.init(smi, sdi))
		{
            data.bufferList = new ArrayList<Object[]>();
            
            data.rowsOnFile = 0;
            
            return true;
		}
		return false;
	}

	
	//
	// Run is were the action happens!
	public void run()
	{		
		try
		{
			logBasic(Messages.getString("GroupBy.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("GroupBy.Log.UnexpectedError")+" : "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            logError(Const.getStackTracker(e));
            setErrors(1);
			stopAll();
		}
		finally
		{
            if (data.tempFile!=null) data.tempFile.delete();
			dispose(meta, data);
			logSummary();
			markStop();
		}
	}
}
