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
 

package be.ibridge.kettle.trans.step.sql;

import java.util.ArrayList;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Result;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.database.Database;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleStepException;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransMeta;
import be.ibridge.kettle.trans.step.BaseStep;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.StepInterface;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;


/**
 * Execute one or more SQL statements in a script, one time or parameterised (for every row)
 * 
 * @author Matt
 * @since 10-sep-2005
 */
public class ExecSQL extends BaseStep implements StepInterface
{
	private ExecSQLMeta meta;
	private ExecSQLData data;
	
	public ExecSQL(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
    
    public static final Row getResultRow(Result result, String upd, String ins, String del, String read)
    {
        Row row = new Row();
        
        if (upd!=null && upd.length()>0)
        {
            Value rowsUpdated = new Value(upd, result.getNrLinesUpdated());
            rowsUpdated.setLength(9);
            row.addValue(rowsUpdated);
        }

        if (ins!=null && ins.length()>0)
        {
            Value rowsInserted = new Value(ins, result.getNrLinesOutput());
            rowsInserted.setLength(9);
            row.addValue(rowsInserted);
        }

        if (del!=null && del.length()>0)
        {
            Value rowsDeleted = new Value(del, result.getNrLinesDeleted());
            rowsDeleted.setLength(9);
            row.addValue(rowsDeleted);
        }

        if (read!=null && read.length()>0)
        {
            Value rowsRead = new Value(read, result.getNrLinesRead());
            rowsRead.setLength(9);
            row.addValue(rowsRead);
        }
        
        return row;
    }
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
        meta=(ExecSQLMeta)smi;
        data=(ExecSQLData)sdi;

        Row row = null;
        
        if (!meta.isExecutedEachInputRow())
        {
            row = getResultRow(data.result, meta.getUpdateField(), meta.getInsertField(), meta.getDeleteField(), meta.getReadField()); 
            putRow(row);
            setOutputDone(); // Stop processing, this is all we do!
            return false;
        }
        
        row = getRow();
        if (row==null) // no more input to be expected...
        {
            setOutputDone();
            return false;
        }
        
        
        StringBuffer sql = new StringBuffer( meta.getSql() ); 

		if (first) // we just got started
		{
            first=false;
            
            // Find the indexes of the arguments
            data.argumentIndexes = new int[meta.getArguments().length];
            for (int i=0;i<meta.getArguments().length;i++)
            {
                data.argumentIndexes[i] = row.searchValueIndex(meta.getArguments()[i]);
                if (data.argumentIndexes[i]<0)
                {
                    logError(Messages.getString("ExecSQL.Log.ErrorFindingField")+meta.getArguments()[i]+"]"); //$NON-NLS-1$ //$NON-NLS-2$
                    throw new KettleStepException(Messages.getString("ExecSQL.Exception.CouldNotFindField",meta.getArguments()[i])); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            
            // Find the locations of the question marks in the String...
            // We replace the question marks with the values...
            // We ignore quotes etc. to make inserts easier...
            data.markerPositions = new ArrayList();
            int len = sql.length();
            int pos = len-1;
            while (pos>=0)
            {
                if (sql.charAt(pos)=='?') data.markerPositions.add(new Integer(pos)); // save the marker position
                pos--;
            }
		}

        // Replace the values in the SQL string...
		for (int i=0;i<data.markerPositions.size();i++)
        {
            Value value = row.getValue( data.argumentIndexes[data.markerPositions.size()-i-1]);
		    int pos = ((Integer)data.markerPositions.get(i)).intValue();
            sql.replace(pos, pos+1, value.getString()); // replace the '?' with the String in the row.
        }

        if (log.isRowLevel()) logRowlevel(Messages.getString("ExecSQL.Log.ExecutingSQLScript")+Const.CR+sql); //$NON-NLS-1$
        data.result = data.db.execStatements(sql.toString());

        Row add = getResultRow(data.result, meta.getUpdateField(), meta.getInsertField(), meta.getDeleteField(), meta.getReadField());
        row.addRow(add);
        
        if (!data.db.isAutoCommit()) data.db.commit();
        
		putRow(row);  // send it out!    

        if (checkFeedback(linesWritten)) logBasic(Messages.getString("ExecSQL.Log.LineNumber")+linesWritten); //$NON-NLS-1$

		return true;
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
        meta=(ExecSQLMeta)smi;
        data=(ExecSQLData)sdi;

        logBasic(Messages.getString("ExecSQL.Log.FinishingReadingQuery")); //$NON-NLS-1$

        data.db.disconnect();

		super.dispose(smi, sdi);
	}
	
	/** Stop the running query */
	public void stopRunning(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
        meta=(ExecSQLMeta)smi;
        data=(ExecSQLData)sdi;

        if (data.db!=null) data.db.cancelQuery();
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(ExecSQLMeta)smi;
		data=(ExecSQLData)sdi;

		if (super.init(smi, sdi))
		{
			data.db=new Database(meta.getDatabaseMeta());
            
            // Connect to the database
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

                if (log.isDetailed()) logDetailed(Messages.getString("ExecSQL.Log.ConnectedToDB")); //$NON-NLS-1$

                // If the SQL needs to be executed once, this is a starting step somewhere.
                if (!meta.isExecutedEachInputRow())
                {
                    data.result = data.db.execStatements(meta.getSql());
                }
                return true;
            }
            catch(KettleException e)
            {
                logError(Messages.getString("ExecSQL.Log.ErrorOccurred")+e.getMessage()); //$NON-NLS-1$
                setErrors(1);
                stopAll();
            }
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
			logBasic(Messages.getString("ExecSQL.Log.StartingToRun")); //$NON-NLS-1$
			while (!isStopped() && processRow(meta, data) );
		}
		catch(Exception e)
		{
			logError(Messages.getString("ExecSQL.Log.UnexpectedError")+" : "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
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