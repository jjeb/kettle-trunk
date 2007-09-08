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
 
package org.pentaho.di.trans.steps.streamlookup;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Collections;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.hash.ByteArrayHashIndex;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
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
 * Looks up information by first reading data into a hash table (in memory)
 * 
 * TODO: add warning with conflicting types OR modify the lookup values to the input row type. (this is harder to do as currently we don't know the types)
 * 
 * @author Matt
 * @since  26-apr-2003
 */
public class StreamLookup extends BaseStep implements StepInterface
{
	private StreamLookupMeta meta;
	private StreamLookupData data;


	public StreamLookup(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	private void handleNullIf()
	{
	    data.nullIf = new Object[meta.getValue().length];
	    
		for (int i=0;i<meta.getValue().length;i++)
		{
			if (meta.getValueDefaultType()[i]<0)
			{
				//logError("unknown default value type: "+dtype+" for value "+value[i]+", default to type: String!");
				meta.getValueDefaultType()[i]=ValueMetaInterface.TYPE_STRING;
			}
			data.nullIf[i]=null;
			switch(meta.getValueDefaultType()[i])
			{
			case ValueMetaInterface.TYPE_STRING: 
				if (Const.isEmpty(meta.getValueDefault()[i]))
                {
                    data.nullIf[i] = null;
                }
                else
                {
                    data.nullIf[i]=meta.getValueDefault()[i];
                }
				break;
			case ValueMetaInterface.TYPE_DATE  :
				try { data.nullIf[i] = DateFormat.getInstance().parse(meta.getValueDefault()[i]); }
				catch(Exception e) { }
				break;
			case ValueMetaInterface.TYPE_NUMBER: 
				try { data.nullIf[i] = new Double( Double.parseDouble(meta.getValueDefault()[i]) ); }
				catch(Exception e) { }
				break;
			case ValueMetaInterface.TYPE_INTEGER: 
				try { data.nullIf[i] = new Long( Long.parseLong(meta.getValueDefault()[i]) ); }
				catch(Exception e) { }
				break;
			case ValueMetaInterface.TYPE_BOOLEAN: 
				if ("TRUE".equalsIgnoreCase(meta.getValueDefault()[i]) || //$NON-NLS-1$
				    "Y".equalsIgnoreCase(meta.getValueDefault()[i]) )  //$NON-NLS-1$
				    data.nullIf[i] = new Boolean(true); 
				else
				    data.nullIf[i] = new Boolean(false); 
				;
				break;
			case ValueMetaInterface.TYPE_BIGNUMBER: 
				try { data.nullIf[i] = new BigDecimal(meta.getValueDefault()[i]); }
				catch(Exception e) { }
				break;
			default: 
				// if a default value is given and no conversion is implemented throw an error
				if (meta.getValueDefault()[i] != null && meta.getValueDefault()[i].trim().length()>0 ) {
					throw new RuntimeException(Messages.getString("StreamLookup.Exception.ConversionNotImplemented") +" " + ValueMeta.getTypeDesc(meta.getValueDefaultType()[i]));
				} else {
					// no default value given: just set it to null
					data.nullIf[i] = null;
					break;
				}				
			}
		}
	}

	private boolean readLookupValues() throws KettleException
	{
		// data.firstrow=null;
			
		if (meta.getLookupFromStep()==null)
		{
			logError(Messages.getString("StreamLookup.Log.NoLookupStepSpecified")); //$NON-NLS-1$
			return false;
		}
		if (log.isDetailed()) logDetailed(Messages.getString("StreamLookup.Log.ReadingFromStream")+meta.getLookupFromStep().getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$

        int[] keyNrs = new int[meta.getKeylookup().length];
        int[] valueNrs = new int[meta.getValue().length];
        boolean firstRun = true;
        
        // Which row set do we read from?
        //
        RowSet rowSet = findInputRowSet(meta.getLookupFromStep().getName());
        Object[] rowData=getRowFrom(rowSet); // rows are originating from "lookup_from"
		while (rowData!=null)
		{
            if (log.isRowLevel()) logRowlevel(Messages.getString("StreamLookup.Log.ReadLookupRow")+rowSet.getRowMeta().getString(rowData)); //$NON-NLS-1$

            if (firstRun)
            {
                firstRun=false;
                
                data.infoMeta = rowSet.getRowMeta().clone();
                data.keyMeta = new RowMeta();
                data.valueMeta = new RowMeta();
            
                // Look up the keys in the source rows
                for (int i=0;i<meta.getKeylookup().length;i++)
                {
                    keyNrs[i] = rowSet.getRowMeta().indexOfValue(meta.getKeylookup()[i]);
                    if (keyNrs[i]<0)
                    {
                        throw new KettleStepException(Messages.getString("StreamLookup.Exception.UnableToFindField",meta.getKeylookup()[i])); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    data.keyMeta.addValueMeta( rowSet.getRowMeta().getValueMeta( keyNrs[i] ));
                }
            
                // Save the data types of the keys to optionally convert input rows later on...
                if (data.keyTypes==null)
                {
                    data.keyTypes=data.keyMeta.clone();
                }
			
    			for (int v=0;v<meta.getValue().length;v++)
    			{
    			    valueNrs[v] = rowSet.getRowMeta().indexOfValue( meta.getValue()[v] );
                    if (valueNrs[v]<0)
    				{
                        throw new KettleStepException(Messages.getString("StreamLookup.Exception.UnableToFindField",meta.getValue()[v])); //$NON-NLS-1$ //$NON-NLS-2$
    				}
                    data.valueMeta.addValueMeta( rowSet.getRowMeta().getValueMeta(valueNrs[v]) );
    			}
            }
            
            Object[] keyData = new Object[keyNrs.length];
            for (int i=0;i<keyNrs.length;i++)
            {
                keyData[i] = rowData[ keyNrs[i] ];
            }

            Object[] valueData = new Object[valueNrs.length];
            for (int i=0;i<valueNrs.length;i++)
            {
                valueData[i] = rowData[ valueNrs[i] ];
            }

            addToCache(data.keyMeta, keyData, data.valueMeta, valueData);
			
			rowData=getRowFrom(rowSet);
		}
		
		return true;
	}


    private Object[] lookupValues(RowMetaInterface rowMeta, Object[] row) throws KettleStepException
	{
		// See if we need to stop.
		if (isStopped()) return null;

		if( data.lookupColumnIndex == null ) 
		{
			String names[] = data.lookupMeta.getFieldNames();
			data.lookupColumnIndex = new int[names.length];
			
			for( int i=0; i<names.length; i++ ) 
			{
				data.lookupColumnIndex[i] = rowMeta.indexOfValue(names[i]);
				if ( data.lookupColumnIndex[i] < 0 ) 
				{
					// we should not get here
					throw new KettleStepException( "The lookup column '"+names[i]+"' could not be found" );
				}
			}
		}
		
		// Copy value references to lookup table.
        Object[] lu = RowDataUtil.resizeArray(row, data.keynrs.length);

        // Handle conflicting types (Number-Integer-String conversion to lookup type in hashtable)
        if (data.keyTypes!=null)
        {
            for (int i=0;i<data.lookupMeta.size();i++)
            {
                ValueMetaInterface inputValue  = data.lookupMeta.getValueMeta(i);
                ValueMetaInterface lookupValue = data.keyTypes.getValueMeta(i);
                if (inputValue.getType()!=lookupValue.getType())
                {
                    try
                    {
                        lu[i] = inputValue.convertData(lookupValue, lu[i]);
                    }
                    catch (KettleValueException e)
                    {
                        throw new KettleStepException("Error converting data while looking up value", e);
                    }
                }
            }
        }
        
        Object[] add;
        
		try
		{
			if (meta.getKeystream().length>0)
			{
				Object lookupData[] = new Object[data.lookupColumnIndex.length];
				for (int i=0;i<lookupData.length;i++) lookupData[i] = row[data.lookupColumnIndex[i]];
				add=getFromCache(data.lookupMeta, lookupData);
			}
			else
			{
				// Just take the first element in the hashtable...
				throw new KettleStepException(Messages.getString("StreamLookup.Log.GotRowWithoutKeys")); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleStepException(e);
		}
		
		if (add==null) // nothing was found, unknown code: add null-values
		{
			add=new Object[meta.getValue().length];
		} 
		
        return RowDataUtil.addRowData(row, rowMeta.size(), add);
	}
	
    private void addToCache(RowMetaInterface keyMeta, Object[] keyData, RowMetaInterface valueMeta, Object[] valueData) throws KettleValueException
    {
        if (meta.isMemoryPreservationActive())
        {
            if (meta.isUsingSortedList())
            {
                KeyValue keyValue = new KeyValue(keyData, valueData);
                int idx = Collections.binarySearch(data.list, keyValue, data.comparator);
                if (idx<0)
                {
                    int index = -idx-1; // this is the insertion point
                    data.list.add(index, keyValue); // insert to keep sorted.
                }
                else
                {
                    data.list.set(idx, keyValue); // Overwrite to simulate Hashtable behaviour
                }
            }
            else
            {
                if (meta.isUsingIntegerPair())
                {
                    Long key = keyMeta.getInteger(keyData, 0);
                    Long value = valueMeta.getInteger(valueData, 0);
                    data.longIndex.put(key, value);
                }
                else
                {
                    if (data.hashIndex==null) 
                    { 
                        data.hashIndex = new ByteArrayHashIndex(keyMeta);
                    }
                    data.hashIndex.put(RowMeta.extractData(keyMeta, keyData), RowMeta.extractData(valueMeta, valueData));
                }
            }
        }
        else
        {
            // We can't just put Object[] in the map
            // The compare function is not in it.
            // We need to wrap in and use that.
            // Let's use RowMetaAndData for this one.
            //
            data.look.put(new RowMetaAndData(keyMeta, keyData), valueData);
        }
    }
    
	private Object[] getFromCache(RowMetaInterface keyMeta, Object[] keyData) throws KettleValueException
    {
        if (meta.isMemoryPreservationActive())
        {
            if (meta.isUsingSortedList())
            {
                KeyValue keyValue = new KeyValue(keyData, null);
                int idx = Collections.binarySearch(data.list, keyValue, data.comparator);
                if (idx<0) return null; // nothing found
                
                keyValue = (KeyValue)data.list.get(idx);
                return keyValue.getValue();
            }
            else
            {
                if (meta.isUsingIntegerPair())
                {
                    Long value = data.longIndex.get( keyMeta.getInteger(keyData, 0) );
                    if (value==null) return null;
                    return new Object[] { value, };
                }
                else
                {
                	try
                	{
	                    byte[] value = data.hashIndex.get(RowMeta.extractData(keyMeta, keyData));
	                    if (value==null) return null;
	                    return RowMeta.getRow(data.valueMeta, value);
                	}
                	catch(Exception e) {
                		logError("Oops", e);
                		throw new RuntimeException(e);
                	}
                }
            }
        }
        else
        {
            return (Object[])data.look.get(new RowMetaAndData(keyMeta, keyData));
        }
    }
    
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
	    meta = (StreamLookupMeta)smi;
	    data = (StreamLookupData)sdi;
	    
	    if (data.readLookupValues)
	    {
	        data.readLookupValues = false;
	        
			logBasic(Messages.getString("StreamLookup.Log.ReadingLookupValuesFromStep")+meta.getLookupFromStep()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			if (! readLookupValues()) // Read values in lookup table (look)
			{
				logError(Messages.getString("StreamLookup.Log.UnableToReadDataFromLookupStream")); //$NON-NLS-1$
				setErrors(1);
				stopAll();
				return false;
			}
	    }
	    
		Object[] r=getRow();      // Get row from input rowset & set row busy!
		if (r==null)         // no more input to be expected...
		{
			if (log.isDetailed()) logDetailed(Messages.getString("StreamLookup.Log.StoppedProcessingWithEmpty",linesRead+"")); //$NON-NLS-1$ //$NON-NLS-2$
			setOutputDone();
			return false;
		}
        
        if (first)
        {
            first=false;
            
            // read the lookup values!
            data.keynrs = new int[meta.getKeystream().length];
            data.lookupMeta = new RowMeta();
            
            for (int i=0;i<meta.getKeystream().length;i++)
            {
                // Find the keynr in the row (only once)
                data.keynrs[i]=getInputRowMeta().indexOfValue(meta.getKeystream()[i]);
                if (data.keynrs[i]<0)
                {
                    throw new KettleStepException(Messages.getString("StreamLookup.Log.FieldNotFound",meta.getKeystream()[i],""+getInputRowMeta().getString(r))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                else
                {
                    if (log.isDetailed()) logDetailed(Messages.getString("StreamLookup.Log.FieldInfo",meta.getKeystream()[i],""+data.keynrs[i])); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                
                data.lookupMeta.addValueMeta( getInputRowMeta().getValueMeta(data.keynrs[i]).clone() );
            }
            
            data.outputRowMeta = getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), new RowMetaInterface[] { data.infoMeta }, null, this);
            
            // Handle the NULL values (not found...)
            handleNullIf();
        }
		
		Object[] outputRow = lookupValues(getInputRowMeta(), r); // Do the actual lookup in the hastable.
		if (outputRow==null)
		{
			setOutputDone();  // signal end to receiver(s)
			return false;
		}
		
		putRow(data.outputRowMeta, outputRow);       // copy row to output rowset(s);
			
        if (checkFeedback(linesRead)) logBasic(Messages.getString("StreamLookup.Log.LineNumber")+linesRead); //$NON-NLS-1$
			
		return true;
	}
		
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (StreamLookupMeta)smi;
	    data = (StreamLookupData)sdi;
	    
	    if (super.init(smi, sdi))
	    {
	        data.readLookupValues = true;
	        
	        return true;
	    }
	    return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		super.dispose(smi, sdi);
	}
	
	//
	// Run is were the action happens!
	public void run()
	{
		try
		{
			logBasic(Messages.getString("System.Log.StartingToRun")); //$NON-NLS-1$
			
			while (processRow(meta, data) && !isStopped());
		}
		catch(Throwable t)
		{
			logError(Messages.getString("System.Log.UnexpectedError")+" : "); //$NON-NLS-1$ //$NON-NLS-2$
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