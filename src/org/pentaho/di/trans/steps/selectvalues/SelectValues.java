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
 
package org.pentaho.di.trans.steps.selectvalues;

import java.util.Arrays;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Select, re-order, remove or change the meta-data of the fields in the inputstreams.
 * 
 * @author Matt
 * @since 5-apr-2003
 *
 */
public class SelectValues extends BaseStep implements StepInterface
{
	private SelectValuesMeta meta;
	private SelectValuesData data;
	
	public SelectValues(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	/**
	   Only select the values that are still needed...<p>
	   Put the values in the right order...<p>
	   Change the meta-data information if needed...<p>
	   
	   @param row The row to manipulate
	   @return true if everything went well, false if we need to stop because of an error!	   
	*/
	private synchronized Object[] selectValues(RowMetaInterface rowMeta, Object[] rowData) throws KettleValueException
	{
		if (data.firstselect)
		{
			data.firstselect=false;

            // We need to create a new Metadata row to drive the output
            // We also want to know the indexes of the selected fields in the source row.
            //
			data.fieldnrs=new int[meta.getSelectName().length];
			data.outputMeta=new RowMeta();

			for (int i=0;i<data.fieldnrs.length;i++) 
			{
				data.fieldnrs[i]=rowMeta.indexOfValue( meta.getSelectName()[i] );
				if (data.fieldnrs[i]<0)
				{
					logError(Messages.getString("SelectValues.Log.CouldNotFindField",meta.getSelectName()[i])); //$NON-NLS-1$ //$NON-NLS-2$
					setErrors(1);
					stopAll();
					return null;
				}
                
                // Create the metadata values too...
                ValueMetaInterface valueMeta = (ValueMetaInterface) rowMeta.getValueMeta( data.fieldnrs[i] ).clone();
                
                // Optionally change the name
                if (!Const.isEmpty(meta.getSelectRename()[i]))
                {
                    valueMeta.setName( meta.getSelectRename()[i] );
                }
                
                // Optionally set the length and precision type
                if (meta.getSelectLength()[i]!=-2)    valueMeta.setLength(meta.getSelectLength()[i]);
                if (meta.getSelectPrecision()[i]!=-2) valueMeta.setPrecision(meta.getSelectPrecision()[i]);
                
                // Save this info
                data.outputMeta.addValueMeta(valueMeta);
			}
			
			// Check for doubles in the selected fields... AFTER renaming!!
			int cnt[] = new int[meta.getSelectName().length];
			for (int i=0;i<meta.getSelectName().length;i++)
			{
				cnt[i]=0;
				for (int j=0;j<meta.getSelectName().length;j++)
				{
                    String one = Const.NVL( meta.getSelectRename()[i], meta.getSelectName()[i]);
                    String two = Const.NVL( meta.getSelectRename()[j], meta.getSelectName()[j]);
					if (one.equals(two)) cnt[i]++;
					
					if (cnt[i]>1)
					{
						logError(Messages.getString("SelectValues.Log.FieldCouldNotSpecifiedMoreThanTwice",one)); //$NON-NLS-1$ //$NON-NLS-2$
						setErrors(1);
						stopAll();
						return null;
					}
				}
			}
		}

        // Create a new output row
        Object[] outputData = new Object[data.fieldnrs.length];
        
		// Get the field values
		for (int i=0;i<data.fieldnrs.length;i++)
		{
            // Normally this can't happen, except when streams are mixed with different
			// number of fields.
			// 
			if (data.fieldnrs[i]<rowMeta.size())
			{
                ValueMetaInterface valueMeta = rowMeta.getValueMeta( data.fieldnrs[i] );
                
			    // TODO: Clone might be a 'bit' expensive as it is only needed in case you want to copy a single field to 2 or more target fields.
                // And even then it is only required for the last n-1 target fields.
                // Perhaps we can consider the requirements for cloning at init(), store it in a boolean[] and just consider this at runtime
                //
                outputData[i] = valueMeta.cloneValueData(rowData[data.fieldnrs[i]]);
			}
			else
			{
				if (log.isDetailed()) logDetailed(Messages.getString("SelectValues.Log.MixingStreamWithDifferentFields")); //$NON-NLS-1$
			}			
		}

		return outputData;
	}
	
	/**
	   
	   Remove the values that are no longer needed.<p>
	   This, we can do VERY fast.<p>
	   
	   @param row The row to manipulate
	   @return true if everything went well, false if we need to stop because of an error!
	   
	*/
	private synchronized Object[] removeValues(RowMetaInterface rowMeta, Object[] rowData)
	{		
		if (data.firstdeselect)
		{
			data.firstdeselect=false;

			// System.out.println("Fields to remove: "+info.dname.length);
			data.removenrs=new int[meta.getDeleteName().length];
			data.outputMeta = (RowMetaInterface) rowMeta.clone();
            
			for (int i=0;i<data.removenrs.length;i++) 
			{
				data.removenrs[i]=rowMeta.indexOfValue(meta.getDeleteName()[i]);
				if (data.removenrs[i]<0)
				{
					logError(Messages.getString("SelectValues.Log.CouldNotFindField",meta.getDeleteName()[i])); //$NON-NLS-1$ //$NON-NLS-2$
					setErrors(1);
					stopAll();
					return null;
				}
			}
			
			// Check for doubles in the selected fields...
			int cnt[] = new int[meta.getDeleteName().length];
			for (int i=0;i<meta.getDeleteName().length;i++)
			{
				cnt[i]=0;
				for (int j=0;j<meta.getDeleteName().length;j++)
				{
					if (meta.getDeleteName()[i].equals(meta.getDeleteName()[j])) cnt[i]++;
					
					if (cnt[i]>1)
					{
						logError(Messages.getString("SelectValues.Log.FieldCouldNotSpecifiedMoreThanTwice2",meta.getDeleteName()[i])); //$NON-NLS-1$ //$NON-NLS-2$
						setErrors(1);
						stopAll();
						return null;
					}
				}
			}
			
			// Sort removenrs descending.  So that we can delete in ascending order...
            Arrays.sort(data.removenrs);
            
            // Patch the output metadata as well...
            //
            for (int i=data.removenrs.length-1;i>=0;i--)
            {
                data.outputMeta.removeValueMeta(data.removenrs[i]);
            }
		}

		/*
		 *  Remove the field values
		 *  Take into account that field indexes change once you remove them!!!
		 *  Therefor removenrs is sorted in reverse on index...
		 */
        return RowDataUtil.removeItems(rowData, data.removenrs);
	}

	/**
	   
	   Change the meta-data of certain fields.<p>
	   This, we can do VERY fast.<p>
	   
	   @param row The row to manipulate
	   @return true if everything went well, false if we need to stop because of an error!
	 * @throws KettleValueException 
	   
	*/
	private synchronized Object[] metadataValues(RowMetaInterface rowMeta, Object[] rowData) throws KettleValueException
	{
		if (data.firstmetadata)
		{
			data.firstmetadata=false;

			data.metanrs=new int[meta.getMetaName().length];
            data.outputMeta = (RowMetaInterface) rowMeta.clone();
			
			for (int i=0;i<data.metanrs.length;i++) 
			{
				data.metanrs[i]=rowMeta.indexOfValue(meta.getMetaName()[i]);
				if (data.metanrs[i]<0)
				{
					logError(Messages.getString("SelectValues.Log.CouldNotFindField",meta.getMetaName()[i])); //$NON-NLS-1$ //$NON-NLS-2$
					setErrors(1);
					stopAll();
					return null;
				}
			}
			
			// Check for doubles in the selected fields...
			int cnt[] = new int[meta.getMetaName().length];
			for (int i=0;i<meta.getMetaName().length;i++)
			{
				cnt[i]=0;
				for (int j=0;j<meta.getMetaName().length;j++)
				{
					if (meta.getMetaName()[i].equals(meta.getMetaName()[j])) cnt[i]++;
					
					if (cnt[i]>1)
					{
						logError(Messages.getString("SelectValues.Log.FieldCouldNotSpecifiedMoreThanTwice2",meta.getMetaName()[i])); //$NON-NLS-1$ //$NON-NLS-2$
						setErrors(1);
						stopAll();
						return null;
					}
				}
			}
            
            /*
             * Change the meta-data! 
             */
            for (int i=0;i<data.metanrs.length;i++)
            {
                ValueMetaInterface v = data.outputMeta.getValueMeta(data.metanrs[i]);
                
                if (!Const.isEmpty(meta.getMetaRename()[i]))             v.setName(meta.getMetaRename()[i]);
                if (meta.getMetaType()[i]!=ValueMetaInterface.TYPE_NONE) 
                {
                    v.setType(meta.getMetaType()[i]);
                    v.setStorageType(ValueMetaInterface.STORAGE_TYPE_NORMAL); // this performs a reset, there is no other options
                }
                if (meta.getMetaLength()[i]!=-2)                         v.setLength(meta.getMetaLength()[i]);
                if (meta.getMetaPrecision()[i]!=-2)                      v.setPrecision(meta.getMetaPrecision()[i]);
            }
		}

		//
		// Change the data too 
		//
		for (int i=0;i<data.metanrs.length;i++)
		{
			if (meta.getMetaType()[i]!=ValueMetaInterface.TYPE_NONE)
            {
                ValueMetaInterface fromMeta = rowMeta.getValueMeta(data.metanrs[i]);
                ValueMetaInterface toMeta   = data.outputMeta.getValueMeta(data.metanrs[i]);
                
                switch(toMeta.getType())
                {
                case ValueMetaInterface.TYPE_STRING    : rowData[data.metanrs[i]] = fromMeta.getString(rowData[data.metanrs[i]]); break;
                case ValueMetaInterface.TYPE_NUMBER    : rowData[data.metanrs[i]] = fromMeta.getNumber(rowData[data.metanrs[i]]); break;
                case ValueMetaInterface.TYPE_INTEGER   : rowData[data.metanrs[i]] = fromMeta.getInteger(rowData[data.metanrs[i]]); break;
                case ValueMetaInterface.TYPE_DATE      : rowData[data.metanrs[i]] = fromMeta.getDate(rowData[data.metanrs[i]]); break;
                case ValueMetaInterface.TYPE_BIGNUMBER : rowData[data.metanrs[i]] = fromMeta.getBigNumber(rowData[data.metanrs[i]]); break;
                case ValueMetaInterface.TYPE_BOOLEAN   : rowData[data.metanrs[i]] = fromMeta.getBoolean(rowData[data.metanrs[i]]); break;
                case ValueMetaInterface.TYPE_BINARY    : rowData[data.metanrs[i]] = fromMeta.getBinary(rowData[data.metanrs[i]]); break;
                default: throw new KettleValueException("Unable to convert data type of value '"+fromMeta+"' to data type "+toMeta.getType());
                }
            }
		}

		return rowData;
	}

	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(SelectValuesMeta)smi;
		data=(SelectValuesData)sdi;

		Object[] rowData=getRow();   // get row from rowset, wait for our turn, indicate busy!
		if (rowData==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		if (log.isRowLevel()) logRowlevel(Messages.getString("SelectValues.Log.GotRowFromPreviousStep")+rowData); //$NON-NLS-1$

		Object[] outputData = null;

        if (data.select)   outputData = selectValues(getInputRowMeta(), rowData);
		if (data.deselect) outputData = removeValues(getInputRowMeta(), rowData);
		if (data.metadata) outputData = metadataValues(getInputRowMeta(), rowData);
		
		if (outputData==null) 
		{
			setOutputDone();  // signal end to receiver(s)
			return false;
		} 

        // Send the row on its way
		putRow(data.outputMeta, outputData);
        
		if (log.isRowLevel()) logRowlevel(Messages.getString("SelectValues.Log.WroteRowToNextStep")+rowData); //$NON-NLS-1$

        if (checkFeedback(linesRead)) logBasic(Messages.getString("SelectValues.Log.LineNumber")+linesRead); //$NON-NLS-1$
			
		return true;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SelectValuesMeta)smi;
		data=(SelectValuesData)sdi;

		if (super.init(smi, sdi))
		{
			data.firstselect   = true;
			data.firstdeselect = true;
			data.firstmetadata = true;

			data.select=false;
			data.deselect=false;
			data.metadata=false;
			
			if (meta.getSelectName()!=null && meta.getSelectName().length>0) data.select   = true;
			if (meta.getDeleteName()!=null && meta.getDeleteName().length>0) data.deselect = true;
			if (meta.getMetaName()!=null && meta.getMetaName().length>0) data.metadata = true;
			
			boolean atLeastOne = data.select || data.deselect || data.metadata;
			if (!atLeastOne)
			{
				setErrors(1);
				logError(Messages.getString("SelectValues.Log.InputShouldContainData")); //$NON-NLS-1$
			}
			
			return atLeastOne; // One of those three has to work!
		}
		else
		{
			return false;
		}
	}
			
	//
	// Run is were the action happens!
	//
	public void run()
	{
		try
		{
			logBasic(Messages.getString("SelectValues.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Throwable t)
		{
			logError(Messages.getString("SelectValues.Log.UnexpectedError")+" : "+t.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            logError(Const.getStackTracker(t));
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