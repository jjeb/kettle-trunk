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
 
package org.pentaho.di.trans.steps.databaselookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.TimedRow;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
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
 * Looks up values in a database using keys from input streams.
 * 
 * @author Matt
 * @since 26-apr-2003
 */
public class DatabaseLookup extends BaseStep implements StepInterface
{
	private DatabaseLookupMeta meta;
	private DatabaseLookupData data;

	public DatabaseLookup(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	/**
	 * Performs the lookup based on the meta-data and the input row.
	 * @param row The row to use as lookup data and the row to add the returned lookup fields to
	 * @return the resulting row after the lookup values where added
	 * @throws KettleException In case something goes wrong.
	 */
	private synchronized Object[] lookupValues(RowMetaInterface inputRowMeta, Object[] row) throws KettleException
	{
		Object[] outputRow = new Object[data.outputRowMeta.size()];
        
        // copy the original objects...
        for (int i=0;i<inputRowMeta.size();i++)
        {
            outputRow[i] = row[i];
        }
        
        Object[] lookupRow = new Object[data.lookupMeta.size()];
        int lookupIndex=0;
        
        for (int i=0;i<meta.getStreamKeyField1().length;i++)
		{
			if (data.keynrs[i]>=0)
			{
                ValueMetaInterface input = inputRowMeta.getValueMeta(data.keynrs[i]);
				ValueMetaInterface value = data.lookupMeta.getValueMeta(lookupIndex);
                lookupRow[lookupIndex] = row[data.keynrs[i]];
                
				// Try to convert type if needed
				if (value.getType()!=data.keytypes[i])
                {
                    lookupRow[lookupIndex] = value.convertData(input, lookupRow[lookupIndex]);
                }
                lookupIndex++;
			}
			if (data.keynrs2[i]>=0)
			{
                ValueMetaInterface input = inputRowMeta.getValueMeta(data.keynrs2[i]);
                ValueMetaInterface value = data.lookupMeta.getValueMeta(lookupIndex);
                lookupRow[lookupIndex] = row[data.keynrs2[i]];
                
                // Try to convert type if needed
                if (value.getType()!=data.keytypes[i])
                {
                    lookupRow[lookupIndex] = value.convertData(input, lookupRow[lookupIndex]);
                }
                lookupIndex++;
			}
		}

        Object[] add = null;
        boolean cache_now=false;        

		// First, check if we looked up before
		if (meta.isCached())
        {
            TimedRow timedRow = (TimedRow) data.look.get(new RowMetaAndData(data.lookupMeta, lookupRow));
            if (timedRow!=null)
            {
                add=timedRow.getRow();
            }
        }
		else add=null; 

		if (add==null)
		{
			if (log.isRowLevel()) logRowlevel(Messages.getString("DatabaseLookup.Log.AddedValuesToLookupRow1")+meta.getStreamKeyField1().length+Messages.getString("DatabaseLookup.Log.AddedValuesToLookupRow2")+data.lookupMeta.getString(lookupRow)); //$NON-NLS-1$ //$NON-NLS-2$

			data.db.setValuesLookup(data.lookupMeta, lookupRow);
			add = data.db.getLookup(meta.isFailingOnMultipleResults());
			cache_now=true;
		}

		if (add==null) // nothing was found, unknown code: add default values
		{
			if (meta.isEatingRowOnLookupFailure())
			{
				return null;
			}
			if (getStepMeta().isDoingErrorHandling())
			{
                putError(getInputRowMeta(), row, 1L, "No lookup found", null, "DBL001");

                // return false else we would still be processed.
                return null;
			}
			
			if (log.isRowLevel()) logRowlevel(Messages.getString("DatabaseLookup.Log.NoResultsFoundAfterLookup")); //$NON-NLS-1$
            
			add=new Object[data.returnMeta.size()];
			for (int i=0;i<meta.getReturnValueField().length;i++)
			{
				if (data.nullif[i]!=null)
				{
					add[i] = data.nullif[i];
				}
				else
				{
					add[i] = null;			
				}
			}
		}
        else
        {
        	if (log.isRowLevel()) logRowlevel(Messages.getString("DatabaseLookup.Log.FoundResultsAfterLookup")+add); //$NON-NLS-1$

        	int types[] = meta.getReturnValueDefaultType();

        	// The assumption here is that the types are in the same order
        	// as the returned lookup row, but since we make the lookup row
        	// that should not be a problem.
            //
            for (int i=0;i<types.length;i++)
        	{  
        		ValueMetaInterface returned = data.db.getReturnRowMeta().getValueMeta(i);
                ValueMetaInterface expected = data.returnMeta.getValueMeta(i);
                
                if ( returned != null && types[i] > 0 && types[i] !=  returned.getType() )
        		{
        			// Set the type to the default return type
        		    add[i] = expected.convertData(returned, add[i]);
        		}
        	}        	
        } 

		// Store in cache if we need to!
		if (meta.isCached() && cache_now)
		{
			data.look.put(new RowMetaAndData(data.lookupMeta, lookupRow), new TimedRow(add));

            // See if we have to limit the cache_size.
            // Sample 10% of the rows in the cache.
            // Remove everything below the second lowest date.
            // That should on average remove more than 10% of the entries
            // It's not exact science, but it will be faster than the old algorithm
            // 
            if (meta.getCacheSize()>0 && data.look.size()>meta.getCacheSize())
            {
                List<RowMetaAndData> keys = new ArrayList<RowMetaAndData>(data.look.keySet());
                List<Date> samples = new ArrayList<Date>();
                int incr = keys.size()/10;
                if (incr==0) incr=1;
                for (int k=0;k<keys.size();k+=incr)
                {
                    RowMetaAndData key = (RowMetaAndData) keys.get(k);
                    TimedRow timedRow = (TimedRow) data.look.get(key);
                    samples.add(timedRow.getLogDate());
                }
                
                Collections.sort(samples);
                
                if (samples.size()>1)
                {
                    Date smallest = (Date) samples.get(1);
                    
                    // Everything below the smallest date goes away...
                    for (int k=0;k<keys.size();k++)
                    {
                        RowMetaAndData key = (RowMetaAndData) keys.get(k);
                        TimedRow timedRow = (TimedRow) data.look.get(key);
                        
                        if (timedRow.getLogDate().compareTo(smallest)<0)
                        {
                            data.look.remove(key);
                        }
                    }
                }
            }
		} 

		for (int i=0;i<add.length;i++)
		{
			outputRow[inputRowMeta.size()+i] = add[i];
		}

		return outputRow;
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(DatabaseLookupMeta)smi;
		data=(DatabaseLookupData)sdi;

		Object[] r=getRow();       // Get row from input rowset & set row busy!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
        
        if (first)
        {
            first=false;
            
            // create the output metadata
            data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null);

            if (meta.isCached())
            {
                if (meta.getCacheSize()>0)
                {
                    data.look=new Hashtable<RowMetaAndData, TimedRow>((int)(meta.getCacheSize()*1.5));
                }
                else
                {
                    data.look=new Hashtable<RowMetaAndData, TimedRow>();
                }
            }

            data.db.setLookup(meta.getSchemaName(), meta.getTablename(), meta.getTableKeyField(), meta.getKeyCondition(), meta.getReturnValueField(), meta.getReturnValueNewName(), meta.getOrderByClause(), meta.isFailingOnMultipleResults());

            // lookup the values!
            if (log.isRowLevel()) logDetailed(Messages.getString("DatabaseLookup.Log.CheckingRow")+getInputRowMeta().getString(r)); //$NON-NLS-1$
            
            data.keynrs = new int[meta.getStreamKeyField1().length];
            data.keynrs2= new int[meta.getStreamKeyField1().length];

            for (int i=0;i<meta.getStreamKeyField1().length;i++)
            {
                data.keynrs[i]=getInputRowMeta().indexOfValue(meta.getStreamKeyField1()[i]);
                if (data.keynrs[i]<0 &&  // couldn't find field!
                    !"IS NULL".equalsIgnoreCase(meta.getKeyCondition()[i]) &&   // No field needed! //$NON-NLS-1$
                    !"IS NOT NULL".equalsIgnoreCase(meta.getKeyCondition()[i])  // No field needed! //$NON-NLS-1$
                   )
                {
                    throw new KettleStepException(Messages.getString("DatabaseLookup.ERROR0001.FieldRequired1.Exception")+meta.getStreamKeyField1()[i]+Messages.getString("DatabaseLookup.ERROR0001.FieldRequired2.Exception")); //$NON-NLS-1$ //$NON-NLS-2$
                }
                data.keynrs2[i]=getInputRowMeta().indexOfValue(meta.getStreamKeyField2()[i]);
                if (data.keynrs2[i]<0 &&  // couldn't find field!
                    "BETWEEN".equalsIgnoreCase(meta.getKeyCondition()[i])   // 2 fields needed! //$NON-NLS-1$
                   )
                {
                    throw new KettleStepException(Messages.getString("DatabaseLookup.ERROR0001.FieldRequired3.Exception")+meta.getStreamKeyField2()[i]+Messages.getString("DatabaseLookup.ERROR0001.FieldRequired4.Exception")); //$NON-NLS-1$ //$NON-NLS-2$
                }
                if (log.isDebug()) logDebug(Messages.getString("DatabaseLookup.Log.FieldHasIndex1")+meta.getStreamKeyField1()[i]+Messages.getString("DatabaseLookup.Log.FieldHasIndex2")+data.keynrs[i]); //$NON-NLS-1$ //$NON-NLS-2$
            }

            data.nullif = new Object[meta.getReturnValueField().length];

            for (int i=0;i<meta.getReturnValueField().length;i++)
            {
                ValueMetaInterface stringMeta = new ValueMeta("string", ValueMetaInterface.TYPE_STRING);
                ValueMetaInterface returnMeta = data.outputRowMeta.getValueMeta(i+getInputRowMeta().size());
                
                if (!Const.isEmpty(meta.getReturnValueDefault()[i]))
                {
                    data.nullif[i] = returnMeta.convertData(stringMeta, meta.getReturnValueDefault()[i]);
                }
                else
                {
                    data.nullif[i] = null;;
                }
            }

            // Determine the types...
            data.keytypes = new int[meta.getTableKeyField().length];
            String schemaTable = meta.getDatabaseMeta().getQuotedSchemaTableCombination(meta.getSchemaName(), meta.getTablename());
            RowMetaInterface fields = data.db.getTableFields(schemaTable);
            if (fields!=null)
            {
                // Fill in the types...
                for (int i=0;i<meta.getTableKeyField().length;i++)
                {
                    ValueMetaInterface key = fields.searchValueMeta(meta.getTableKeyField()[i]);
                    if (key!=null)
                    {
                        data.keytypes[i] = key.getType();
                    }
                    else
                    {
                        throw new KettleStepException(Messages.getString("DatabaseLookup.ERROR0001.FieldRequired5.Exception")+meta.getTableKeyField()[i]+Messages.getString("DatabaseLookup.ERROR0001.FieldRequired6.Exception")); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
            else
            {
                throw new KettleStepException(Messages.getString("DatabaseLookup.ERROR0002.UnableToDetermineFieldsOfTable")+schemaTable+"]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            
            // Count the number of values in the lookup as well as the metadata to send along with it.
            //
            data.lookupMeta = new RowMeta();
            
            for (int i=0;i<meta.getStreamKeyField1().length;i++)
            {
                if (data.keynrs[i]>=0)
                {
                    ValueMetaInterface value = (ValueMetaInterface) getInputRowMeta().getValueMeta(data.keynrs[i]).clone();
                    
                    // Try to convert type if needed in a clone, we don't want to
                    // change the type in the original row

                    if (value.getType()!=data.keytypes[i]) value.setType(data.keytypes[i]);
                    data.lookupMeta.addValueMeta( value );
                }
                if (data.keynrs2[i]>=0)
                {
                    ValueMetaInterface value = getInputRowMeta().getValueMeta(data.keynrs2[i]);

                    // Try to convert type if needed in a clone, we don't want to
                    // change the type in the original row
                    
                    if (value.getType()!=data.keytypes[i]) value.setType(data.keytypes[i]);
                    data.lookupMeta.addValueMeta( value );
                }
            }
            
            // We also want to know the metadata of the return values beforehand (null handling)
            data.returnMeta = new RowMeta();
            
            for (int i=0;i<meta.getReturnValueField().length;i++)
            {
                ValueMetaInterface v = (ValueMetaInterface) data.outputRowMeta.getValueMeta(getInputRowMeta().size()+i).clone();
                data.returnMeta.addValueMeta(v);
            }
        }

		if (log.isRowLevel()) logRowlevel(Messages.getString("DatabaseLookup.Log.GotRowFromPreviousStep")+r); //$NON-NLS-1$

		try
		{
            // add new lookup values to the row
            Object[] outputRow = lookupValues(getInputRowMeta(), r); 

            // copy row to output rowset(s);
			putRow(data.outputRowMeta, outputRow);
            
			if (log.isRowLevel()) logRowlevel(Messages.getString("DatabaseLookup.Log.WroteRowToNextStep")+r); //$NON-NLS-1$
            if (checkFeedback(linesRead)) logBasic("linenr "+linesRead); //$NON-NLS-1$
		}
		catch(KettleException e)
		{
			logError(Messages.getString("DatabaseLookup.ERROR003.UnexpectedErrorDuringProcessing")+e.getMessage()); //$NON-NLS-1$
			setErrors(1);
			stopAll();
			setOutputDone();  // signal end to receiver(s)
			return false;
		}

		return true;
	}
    
    /** Stop the running query */
    public void stopRunning(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
    {
        meta=(DatabaseLookupMeta)smi;
        data=(DatabaseLookupData)sdi;

        if (data.db!=null) data.db.cancelQuery();
    }


	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(DatabaseLookupMeta)smi;
		data=(DatabaseLookupData)sdi;

		if (super.init(smi, sdi))
		{
			data.db=new Database(meta.getDatabaseMeta());
			data.db.shareVariablesWith(this);
			try
			{
                if (getTransMeta().isUsingUniqueConnections())
                {
                    synchronized (getTrans()) { data.db.connect(getTrans().getThreadName(), getPartitionID()); }
                }
                else
                {
                    data.db.connect(getPartitionID());
                }
                
                data.db.setCommit(100); // we never get a commit, but it just turns off auto-commit.
                                
                logBasic(Messages.getString("DatabaseLookup.Log.ConnectedToDatabase")); //$NON-NLS-1$

				return true;
			}
			catch(Exception e)
			{
				logError(Messages.getString("DatabaseLookup.ERROR0004.UnexpectedErrorDuringInit")+e.toString()); //$NON-NLS-1$
				data.db.disconnect();
			}
		}
		return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (DatabaseLookupMeta)smi;
	    data = (DatabaseLookupData)sdi;

	    data.db.disconnect();

	    super.dispose(smi, sdi);
	}

	//
	// Run is were the action happens!
	public void run()
	{
		logBasic(Messages.getString("DatabaseLookup.Log.StartingToRun")); //$NON-NLS-1$

		try
		{
			logBasic(Messages.getString("DatabaseLookup.Log.ConnectedToDatabase2"));	 //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("DatabaseLookup.ERROR003.UnexpectedErrorDuringProcessing2")+e.getMessage()); //$NON-NLS-1$
			logError(e.toString());
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

	public String toString()
	{
		return this.getClass().getName();
	}
}
