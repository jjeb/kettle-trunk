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
 
package org.pentaho.di.spoon.job;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.dialog.ErrorDialog;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.widget.ColumnInfo;
import org.pentaho.di.core.widget.TableView;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.spoon.Spoon;
import org.pentaho.di.spoon.TabItemInterface;


/**
 * ChefHistory handles the display of the historical information regarding earlier runs of this job.
 * The idea is that this Composite is only populated when after loading of a job, we find a connection and logging table.
 * We then read from this table and populate the grid and log.
 *  
 * @see Spoon
 * @author Matt
 * @since  16-mar-2006
 */
public class JobHistory extends Composite implements TabItemInterface
{
	private ColumnInfo[] colinf;	
	
	private Text   wText;
	private Button wRefresh, wReplay;
    private TableView wFields;
    
	private FormData fdText, fdSash, fdRefresh, fdReplay; 
	
	private Spoon spoon;

    private List<RowMetaAndData> rowList;

	private final Shell shell;

	private boolean refreshNeeded = true;
	
	private Object refreshNeededLock = new Object();

    private JobMeta jobMeta;
	
	public JobHistory(Composite parent, Spoon spoon, JobMeta jobMeta)
	{
		super(parent, SWT.NONE);
		this.spoon = spoon;
		this.shell = parent.getShell();
        this.jobMeta = jobMeta;
		
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;
		
		setLayout(formLayout);
		
		setVisible(true);
        spoon.props.setLook(this);
		
		SashForm sash = new SashForm(this, SWT.VERTICAL);
		spoon.props.setLook(sash);
		
		sash.setLayout(new FillLayout());

		final int FieldsRows=1;
		
		colinf=new ColumnInfo[] {
            new ColumnInfo(Messages.getString("ChefHistory.Column.BatchID"),        ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
    		new ColumnInfo(Messages.getString("ChefHistory.Column.Status"),         ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
            new ColumnInfo(Messages.getString("ChefHistory.Column.Read"),           ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
    		new ColumnInfo(Messages.getString("ChefHistory.Column.Written"),        ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
            new ColumnInfo(Messages.getString("ChefHistory.Column.Updated"),        ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
    		new ColumnInfo(Messages.getString("ChefHistory.Column.Input"),          ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
    		new ColumnInfo(Messages.getString("ChefHistory.Column.Output"),         ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
    		new ColumnInfo(Messages.getString("ChefHistory.Column.Errors"),         ColumnInfo.COLUMN_TYPE_TEXT, true , true), //$NON-NLS-1$
            new ColumnInfo(Messages.getString("ChefHistory.Column.StartDate"),      ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
            new ColumnInfo(Messages.getString("ChefHistory.Column.EndDate"),        ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
    		new ColumnInfo(Messages.getString("ChefHistory.Column.LogDate"),        ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
            new ColumnInfo(Messages.getString("ChefHistory.Column.DependencyDate"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
            new ColumnInfo(Messages.getString("ChefHistory.Column.ReplayDate"),     ColumnInfo.COLUMN_TYPE_TEXT, false, true) //$NON-NLS-1$
        };
        
        for (int i=0;i<colinf.length;i++) colinf[i].setAllignement(SWT.RIGHT);
        
        wFields=new TableView(sash, 
							  SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE, 
							  colinf, 
							  FieldsRows,  
							  true, // readonly!
							  null,
							  spoon.props
							  );
		
		wText = new Text(sash, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY );
		spoon.props.setLook(wText);
		wText.setVisible(true);
		
		wRefresh = new Button(this, SWT.PUSH);
		wRefresh.setText(Messages.getString("ChefHistory.Button.Refresh")); //$NON-NLS-1$

		fdRefresh    = new FormData(); 
		fdRefresh.left   = new FormAttachment(15, 0);  
		fdRefresh.bottom = new FormAttachment(100, 0);
		wRefresh.setLayoutData(fdRefresh);
		
		wReplay = new Button(this, SWT.PUSH);
		wReplay.setText(Messages.getString("ChefHistory.Button.Replay")); //$NON-NLS-1$

		fdReplay    = new FormData(); 
		fdReplay.left   = new FormAttachment(wRefresh, Const.MARGIN);  
		fdReplay.bottom = new FormAttachment(100, 0);
		wReplay.setLayoutData(fdReplay);

		// Put text in the middle
		fdText=new FormData();
		fdText.left   = new FormAttachment(0, 0);
		fdText.top    = new FormAttachment(0, 0);
		fdText.right  = new FormAttachment(100, 0);
		fdText.bottom = new FormAttachment(100, 0);
		wText.setLayoutData(fdText);

		
		fdSash     = new FormData(); 
		fdSash.left   = new FormAttachment(0, 0);  // First one in the left top corner
		fdSash.top    = new FormAttachment(0, 0);
		fdSash.right  = new FormAttachment(100, 0);
		fdSash.bottom = new FormAttachment(wRefresh, -5);
		sash.setLayoutData(fdSash);
		
		// sash.setWeights(new int[] { 60, 40} );

		pack();
		
		setupReplayListener();
        
        SelectionAdapter lsRefresh = new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    refreshHistory();
                }
            };
		
		wRefresh.addSelectionListener(lsRefresh);
        
        wFields.table.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    showLogEntry();
                }
            }
        );
        wFields.table.addKeyListener(new KeyListener()
            {
                public void keyReleased(KeyEvent e)
                {
                    showLogEntry();
                }
            
                public void keyPressed(KeyEvent e)
                {
                }
            
            }
        );
	}

	private void setupReplayListener() 
    {
		SelectionAdapter lsReplay = new SelectionAdapter()
        {
			final SimpleDateFormat df = new SimpleDateFormat(ValueMeta.DEFAULT_DATE_FORMAT_MASK);

			public void widgetSelected(SelectionEvent e)
            {
                int idx = wFields.getSelectionIndex();
                if (idx >= 0)
                {
                    String fields[] = wFields.getItem(idx);
                    String dateString = fields[12];
                    if (dateString == null || dateString.equals(Const.NULL_STRING))
                    {
                        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                        mb.setMessage(Messages.getString("ChefHistory.Error.ReplayingJob") //$NON-NLS-1$
                                + Const.CR + Messages.getString("ChefHistory.Error.ReplayDateCannotBeNull")); //$NON-NLS-1$
                        mb.setText(Messages.getString("ChefHistory.ERROR")); //$NON-NLS-1$
                        mb.open();
                        return;
                    }
                    try
                    {
                        Date date = df.parse(dateString);
                        spoon.executeJob(jobMeta, true, false, false, false, date);
                    }
                    catch (ParseException e1)
                    {
                        new ErrorDialog(shell, Messages.getString("ChefHistory.Error.ReplayingJob2"), //$NON-NLS-1$
                                Messages.getString("ChefHistory.Error.InvalidReplayDate") + dateString, e1); //$NON-NLS-1$
                    }
                }
            }
		};
	
        wReplay.addSelectionListener(lsReplay);
	}
	
    /**
     * Refreshes the history window in Spoon: reads entries from the specified log table in the Job Settings dialog.
     */
	public void refreshHistory()
	{
        // See if there is a job loaded that has a connection table specified.
        if (jobMeta!=null && !Const.isEmpty(jobMeta.getName()))
        {
            if (jobMeta.getLogConnection()!=null)
            {
                if (!Const.isEmpty(jobMeta.getLogTable()))
                {
                    Database database = null;
                    try
                    {
                        // open a connection
                        database = new Database(jobMeta.getLogConnection());
                        database.connect();
                        
                        RowMetaAndData params = new RowMetaAndData();
                        params.addValue(new ValueMeta("transname", ValueMetaInterface.TYPE_STRING), jobMeta.getName()); //$NON-NLS-1$
                        ResultSet resultSet = database.openQuery("SELECT * FROM "+jobMeta.getLogTable()+" WHERE JOBNAME = ? ORDER BY ID_JOB desc", params.getRowMeta(), params.getData()); //$NON-NLS-1$ //$NON-NLS-2$
                        
                        rowList = new ArrayList<RowMetaAndData>();
                        Object[] rowData = database.getRow(resultSet);
                        while (rowData!=null)
                        {
                            RowMetaInterface rowMeta = database.getReturnRowMeta();
                            rowList.add(new RowMetaAndData(rowMeta, rowData));
                            rowData = database.getRow(resultSet);
                        }
                        database.closeQuery(resultSet);

                        if (rowList.size()>0)
                        {
                            wFields.table.clearAll();
                            
                            // OK, now that we have a series of rows, we can add them to the table view...
                            for (int i=0;i<rowList.size();i++)
                            {
                                RowMetaAndData row = (RowMetaAndData) rowList.get(i);
                                TableItem item = new TableItem(wFields.table, SWT.NONE);
                                String batchID = row.getString("ID_JOB", "");
                                if(batchID != null)
                                item.setText( 1, batchID);           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 2, Const.NVL( row.getString("STATUS", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 3, Const.NVL( row.getString("LINES_READ", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 4, Const.NVL( row.getString("LINES_WRITTEN", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 5, Const.NVL( row.getString("LINES_UPDATED", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 6, Const.NVL( row.getString("LINES_INPUT", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 7, Const.NVL( row.getString("LINES_OUTPUT", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 8, Const.NVL( row.getString("ERRORS", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText( 9, Const.NVL( row.getString("STARTDATE", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText(10, Const.NVL( row.getString("ENDDATE", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText(11, Const.NVL( row.getString("LOGDATE", ""), ""));           //$NON-NLS-1$ //$NON-NLS-2$
                                item.setText(12, Const.NVL( row.getString("DEPDATE", ""), ""));   //$NON-NLS-1$ //$NON-NLS-2$
                                String replayDate = row.getString("REPLAYDATE", ""); //$NON-NLS-1$ //$NON-NLS-2$
                                if(replayDate == null)
                                	replayDate = Const.NULL_STRING;
								item.setText(13, replayDate);
                            }
                            
                            wFields.removeEmptyRows();
                            wFields.setRowNums();
                            wFields.optWidth(true);
                            wFields.table.setSelection(0);
                            
                            showLogEntry();
                        }
                    }
                    catch(KettleException e)
                    {
                        new ErrorDialog(this.getShell(), Messages.getString("ChefHistory.Error.GettingLoggingInfo"), Messages.getString("ChefHistory.Error.GettingInfoFromLoggingTable"), e); //$NON-NLS-1$ //$NON-NLS-2$
                        wFields.clearAll(false);
                    }
                    finally
                    {
                        if (database!=null) database.disconnect();
                    }
                    
                }
                else
                {
                    wFields.clearAll(false);
                }
            }
            else
            {
                wFields.clearAll(false);
            }
        }
        else
        {
            wFields.clearAll(false);
        }
	}
    	
	public void showLogEntry()
    {
        if (rowList==null) 
        {
            wText.setText(""); //$NON-NLS-1$
            return;
        }
        
        // grab the selected line in the table:
        int nr = wFields.table.getSelectionIndex();
        if (nr>=0 && rowList!=null && nr<rowList.size())
        {
            // OK, grab this one from the buffer...
            RowMetaAndData row = (RowMetaAndData) rowList.get(nr);
            String logging;
            try
            {
                logging = row.getString("LOG_FIELD", ""); //$NON-NLS-1$ //$NON-NLS-2$
            }
            catch (KettleValueException e)
            {
                logging = Const.getStackTracker(e);
            } 
            if (logging!=null) 
            {
                wText.setText(logging);
            }
            else
            {
                wText.setText(""); //$NON-NLS-1$
            }
        }
    }

    public String toString()
	{
		return Spoon.APP_NAME;
	}

	public void refreshHistoryIfNeeded() {
		boolean reallyRefresh = false;
		synchronized (refreshNeededLock) {
			reallyRefresh = refreshNeeded;
			refreshNeeded = false;
		}
		
		if (reallyRefresh) {
			refreshHistory();
		}
	}

	public void markRefreshNeeded() {
		synchronized (refreshNeededLock) {
			refreshNeeded = true;
		}
	}

    /**
     * @return the jobMeta
     */
    public JobMeta getJobMeta()
    {
        return jobMeta;
    }

    /**
     * @param jobMeta the jobMeta to set
     */
    public void setJobMeta(JobMeta jobMeta)
    {
        this.jobMeta = jobMeta;
    }

    public boolean applyChanges()
    {
        return false;
    }

    public boolean canBeClosed()
    {
        return true;
    }

    public Object getManagedObject()
    {
        return null;
    }

    public boolean hasContentChanged()
    {
        return false;
    }

    public int showChangedWarning()
    {
        return 0;
    }

    /**
     * @return the spoon
     */
    public Spoon getSpoon()
    {
        return spoon;
    }

    /**
     * @param spoon the spoon to set
     */
    public void setSpoon(Spoon spoon)
    {
        this.spoon = spoon;
    }

}
