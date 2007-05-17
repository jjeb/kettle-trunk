
package org.pentaho.di.core.database.dialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.row.RowMetaInterface;

import be.ibridge.kettle.core.LocalVariables;
import be.ibridge.kettle.core.dialog.ErrorDialog;
import be.ibridge.kettle.core.exception.KettleException;


/**
 * Takes care of displaying a dialog that will handle the wait while 
 * we're getting rows for a certain SQL query on a database.
 * 
 * @author Matt
 * @since  12-may-2005
 */
public class GetPreviewTableProgressDialog
{
	private Shell shell;
	private DatabaseMeta dbMeta;
	private String tableName;
	private int limit;
	private ArrayList rows;
    private RowMetaInterface rowMeta;
	
	private Database db;
    private Thread parentThread;

	/**
	 * Creates a new dialog that will handle the wait while we're doing the hard work.
	 */
	public GetPreviewTableProgressDialog(Shell shell, DatabaseMeta dbInfo, String tableName, int limit)
	{
		this.shell = shell;
		this.dbMeta = dbInfo;
		this.tableName = tableName;
		this.limit = limit;
        
        this.parentThread = Thread.currentThread();
	}
	
	public ArrayList open()
	{
		IRunnableWithProgress op = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
                // This is running in a new process: copy some KettleVariables info
                LocalVariables.getInstance().createKettleVariables(Thread.currentThread().getName(), parentThread.getName(), true);

				db = new Database(dbMeta);
				try 
				{
					db.connect();
					
					rows =  db.getFirstRows(tableName, limit, monitor);
                    rowMeta = db.getReturnRowMeta();
					
					if (monitor.isCanceled()) 
						throw new InvocationTargetException(new Exception("This operation was cancelled!"));

				}
				catch(KettleException e)
				{
					throw new InvocationTargetException(e, "Couldn't find any rows because of an error :"+e.toString());
				}
				finally
				{
					db.disconnect();
				}
			}
		};
		
		try
		{
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
			// Run something in the background to cancel active database queries, forecably if needed!
			Runnable run = new Runnable()
            {
                public void run()
                {
                    IProgressMonitor monitor = pmd.getProgressMonitor();
                    while (pmd.getShell()==null || ( !pmd.getShell().isDisposed() && !monitor.isCanceled() ))
                    {
                        try { Thread.sleep(100); } catch(InterruptedException e) { };
                    }
                    
                    if (monitor.isCanceled()) // Disconnect and see what happens!
                    {
                        try { db.cancelQuery(); } catch(Exception e) {};
                    }
                }
            };
            // Start the cancel tracker in the background!
            new Thread(run).start();
            
			pmd.run(true, true, op);
		}
		catch (InvocationTargetException e)
		{
		    showErrorDialog(e);
			return null;
		}
		catch (InterruptedException e)
		{
		    showErrorDialog(e);
			return null;
		}
		
		return rows;
	}

    /**
     * Showing an error dialog
     * 
     * @param e
    */
    private void showErrorDialog(Exception e)
    {
        new ErrorDialog(shell, Messages.getString("GetPreviewTableProgressDialog.Error.Title"),
            Messages.getString("GetPreviewTableProgressDialog.Error.Message"), e);
    }

    /**
     * @return the rowMeta
     */
    public RowMetaInterface getRowMeta()
    {
        return rowMeta;
    }
}
