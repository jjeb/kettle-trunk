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


 
package be.ibridge.kettle.job.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.GUIResource;
import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Props;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.WindowProperty;
import be.ibridge.kettle.core.database.Database;
import be.ibridge.kettle.core.database.DatabaseMeta;
import be.ibridge.kettle.core.dialog.DatabaseDialog;
import be.ibridge.kettle.core.dialog.SQLEditor;
import be.ibridge.kettle.core.exception.KettleDatabaseException;
import be.ibridge.kettle.core.widget.TextVar;
import be.ibridge.kettle.i18n.GlobalMessages;
import be.ibridge.kettle.job.JobEntryLoader;
import be.ibridge.kettle.job.JobMeta;
import be.ibridge.kettle.job.entry.JobEntryInterface;
import be.ibridge.kettle.repository.Repository;
import be.ibridge.kettle.repository.RepositoryDirectory;
import be.ibridge.kettle.repository.dialog.SelectDirectoryDialog;
import be.ibridge.kettle.trans.step.BaseStepDialog;


/**
 * Allows you to edit the Job settings.  Just pass a JobInfo object.
 * 
 * @author Matt
 * @since  02-jul-2003
 */
public class JobDialog extends Dialog
{
	private LogWriter    log;
	private Props        props;
		
	private Label        wlJobname;
	private Text         wJobname;
    private FormData     fdlJobname, fdJobname;

    private Label        wlDirectory;
	private Text         wDirectory;
	private Button       wbDirectory;
    private FormData     fdlDirectory, fdbDirectory, fdDirectory;    

	private Label        wlLogconnection;
	private Button       wbLogconnection;
	private CCombo       wLogconnection;
	private FormData     fdlLogconnection, fdbLogconnection, fdLogconnection;

	private Label        wlLogtable;
	private Text         wLogtable;
	private FormData     fdlLogtable, fdLogtable;

    private Label        wlBatch;
    private Button       wBatch;
    private FormData     fdlBatch, fdBatch;

    private Label        wlBatchTrans;
    private Button       wBatchTrans;
    private FormData     fdlBatchTrans, fdBatchTrans;

    private Label        wlLogfield;
    private Button       wLogfield;
    private FormData     fdlLogfield, fdLogfield;

	private Button wOK, wSQL, wCancel;
	private Listener lsOK, lsSQL, lsCancel;

	private JobMeta jobMeta;
	private Shell  shell;
	private Repository rep;
	
	private SelectionAdapter lsDef;
	
	private ModifyListener lsMod;
	private boolean changed;
    
    private TextVar wSharedObjectsFile;
    private boolean sharedObjectsFileChanged;

    /**
     * @deprecated Use version without <i>log</i> and <i>props</i> parameter
     */
    public JobDialog(Shell parent, int style, LogWriter log, Props props, JobMeta jobMeta, Repository rep)
    {
        this(parent, style, jobMeta, rep);
        this.log = log;
        this.props = props;
    }

	public JobDialog(Shell parent, int style, JobMeta jobMeta, Repository rep)
	{
		super(parent, style);
		this.log=LogWriter.getInstance();
		this.jobMeta=jobMeta;
		this.props=Props.getInstance();
		this.rep=rep;
	}
	

	public JobMeta open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
 		props.setLook(		shell);
		
		lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				changed = true;
			}
		};
		
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("JobDialog.JobProperties.ShellText"));
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Transformation name:
		wlJobname=new Label(shell, SWT.RIGHT);
		wlJobname.setText(Messages.getString("JobDialog.JobName.Label"));
 		props.setLook(		wlJobname);
		fdlJobname=new FormData();
		fdlJobname.left = new FormAttachment(0, 0);
        fdlJobname.right= new FormAttachment(middle, 0);
		fdlJobname.top  = new FormAttachment(0, margin);
		wlJobname.setLayoutData(fdlJobname);
		wJobname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(		wJobname);
		wJobname.addModifyListener(lsMod);
		fdJobname=new FormData();
		fdJobname.left = new FormAttachment(middle, 0);
		fdJobname.top  = new FormAttachment(0, margin);
		fdJobname.right= new FormAttachment(100, 0);
		wJobname.setLayoutData(fdJobname);

		// Directory:
		wlDirectory=new Label(shell, SWT.RIGHT);
		wlDirectory.setText(Messages.getString("JobDialog.Directory.Label"));
 		props.setLook(wlDirectory);
		fdlDirectory=new FormData();
		fdlDirectory.left = new FormAttachment(0, 0);
		fdlDirectory.right= new FormAttachment(middle, -margin);
		fdlDirectory.top  = new FormAttachment(wJobname, margin);
		wlDirectory.setLayoutData(fdlDirectory);

		wbDirectory=new Button(shell, SWT.PUSH);
		wbDirectory.setText("...");
 		props.setLook(wbDirectory);
		fdbDirectory=new FormData();
		fdbDirectory.top  = new FormAttachment(wJobname, margin);
		fdbDirectory.right= new FormAttachment(100, 0);
		wbDirectory.setLayoutData(fdbDirectory);
		wbDirectory.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent arg0)
			{
				RepositoryDirectory directoryFrom = jobMeta.getDirectory();
				long idDirectoryFrom  = directoryFrom.getID();
				
				SelectDirectoryDialog sdd = new SelectDirectoryDialog(shell, SWT.NONE, rep);
				RepositoryDirectory rd = sdd.open();
				if (rd!=null)
				{
					if (idDirectoryFrom!=rd.getID())
					{
					 	try
						{
					 		rep.moveJob(jobMeta.getName(), idDirectoryFrom, rd.getID() );
					 		log.logDetailed(getClass().getName(), "Moved directory to ["+rd.getPath()+"]");
							jobMeta.setDirectory( rd );
							wDirectory.setText(jobMeta.getDirectory().getPath());
						}
					 	catch(KettleDatabaseException dbe)
					 	{
					 		jobMeta.setDirectory( directoryFrom );
					 		
							MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
							mb.setText(Messages.getString("JobDialog.Dialog.ErrorChangingDirectory.Title"));
							mb.setMessage(Messages.getString("JobDialog.Dialog.ErrorChangingDirectory.Message"));
							mb.open();
					 	}
					}
					else
					{
						// Same directory!
					}
				}
			}
		});

		wDirectory=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wDirectory);
        wDirectory.setToolTipText(Messages.getString("JobDialog.Directory.Tooltip"));
		wDirectory.setEditable(false);
		fdDirectory=new FormData();
		fdDirectory.top  = new FormAttachment(wJobname, margin);
		fdDirectory.left = new FormAttachment(middle, 0);
		fdDirectory.right= new FormAttachment(wbDirectory, 0);
		wDirectory.setLayoutData(fdDirectory);

		// Log table connection...
		wlLogconnection=new Label(shell, SWT.RIGHT);
		wlLogconnection.setText(Messages.getString("JobDialog.LogConnection.Label"));
 		props.setLook(wlLogconnection);
		fdlLogconnection=new FormData();
		fdlLogconnection.top  = new FormAttachment(wDirectory, margin*4);
		fdlLogconnection.left = new FormAttachment(0, 0);
        fdlLogconnection.right= new FormAttachment(middle, 0);
		wlLogconnection.setLayoutData(fdlLogconnection);

		wbLogconnection=new Button(shell, SWT.PUSH);
		wbLogconnection.setText(GlobalMessages.getSystemString("System.Button.Edit"));
		fdbLogconnection=new FormData();
		fdbLogconnection.top   = new FormAttachment(wDirectory, margin*4);
		fdbLogconnection.right = new FormAttachment(100, 0);
		wbLogconnection.setLayoutData(fdbLogconnection);
        wbLogconnection.addSelectionListener(new SelectionAdapter() 
            {
                public void widgetSelected(SelectionEvent e) 
                {
                    DatabaseMeta databaseMeta = jobMeta.findDatabase(wLogconnection.getText());
                    if (databaseMeta==null) databaseMeta=new DatabaseMeta();
                    DatabaseDialog cid = new DatabaseDialog(shell, databaseMeta);
                    if (cid.open()!=null)
                    {
                        wLogconnection.setText(databaseMeta.getName());
                    }
                }
            }
        );

		wLogconnection=new CCombo(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wLogconnection);
        wLogconnection.setToolTipText(Messages.getString("JobDialog.LogConnection.Tooltip"));
		wLogconnection.addModifyListener(lsMod);
		fdLogconnection=new FormData();
		fdLogconnection.top  = new FormAttachment(wDirectory, margin*4);
		fdLogconnection.left = new FormAttachment(middle, 0);
		fdLogconnection.right= new FormAttachment(wbLogconnection, -margin);
		wLogconnection.setLayoutData(fdLogconnection);

        // populate the combo box...
        for (int i=0;i<jobMeta.nrDatabases();i++)
        {
            DatabaseMeta meta = jobMeta.getDatabase(i);
            wLogconnection.add(meta.getName());
        }
        
        // add a listener
        wLogconnection.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent e) { setFlags(); } } );

		// Log table...:
		wlLogtable=new Label(shell, SWT.RIGHT);
		wlLogtable.setText(Messages.getString("JobDialog.LogTable.Label"));
 		props.setLook(wlLogtable);
		fdlLogtable=new FormData();
        fdlLogtable.left = new FormAttachment(0, 0);
		fdlLogtable.right= new FormAttachment(middle, 0);
		fdlLogtable.top  = new FormAttachment(wLogconnection, margin);
		wlLogtable.setLayoutData(fdlLogtable);
		wLogtable=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wLogtable);
        wLogtable.setToolTipText(Messages.getString("JobDialog.LogTable.Tooltip"));
		wLogtable.addModifyListener(lsMod);
		fdLogtable=new FormData();
		fdLogtable.left = new FormAttachment(middle, 0);
		fdLogtable.top  = new FormAttachment(wLogconnection, margin);
		fdLogtable.right= new FormAttachment(100, 0);
		wLogtable.setLayoutData(fdLogtable);

        wlBatch=new Label(shell, SWT.RIGHT);
        wlBatch.setText(Messages.getString("JobDialog.UseBatchID.Label"));
        props.setLook(wlBatch);
        fdlBatch=new FormData();
        fdlBatch.left = new FormAttachment(0, 0);
        fdlBatch.top  = new FormAttachment(wLogtable, margin*3);
        fdlBatch.right= new FormAttachment(middle, -margin);
        wlBatch.setLayoutData(fdlBatch);
        wBatch=new Button(shell, SWT.CHECK);
        props.setLook(wBatch);
        wBatch.setToolTipText(Messages.getString("JobDialog.UseBatchID.Tooltip"));
        fdBatch=new FormData();
        fdBatch.left = new FormAttachment(middle, 0);
        fdBatch.top  = new FormAttachment(wLogtable, margin*3);
        fdBatch.right= new FormAttachment(100, 0);
        wBatch.setLayoutData(fdBatch);

        wlBatchTrans=new Label(shell, SWT.RIGHT);
        wlBatchTrans.setText(Messages.getString("JobDialog.PassBatchID.Label"));
        props.setLook(wlBatchTrans);
        fdlBatchTrans=new FormData();
        fdlBatchTrans.left = new FormAttachment(0, 0);
        fdlBatchTrans.top  = new FormAttachment(wBatch, margin);
        fdlBatchTrans.right= new FormAttachment(middle, -margin);
        wlBatchTrans.setLayoutData(fdlBatchTrans);
        wBatchTrans=new Button(shell, SWT.CHECK);
        props.setLook(wBatchTrans);
        wBatchTrans.setToolTipText(Messages.getString("JobDialog.PassBatchID.Tooltip"));
        fdBatchTrans=new FormData();
        fdBatchTrans.left = new FormAttachment(middle, 0);
        fdBatchTrans.top  = new FormAttachment(wBatch, margin);
        fdBatchTrans.right= new FormAttachment(100, 0);
        wBatchTrans.setLayoutData(fdBatchTrans);

        wlLogfield=new Label(shell, SWT.RIGHT);
        wlLogfield.setText(Messages.getString("JobDialog.UseLogField.Label"));
        props.setLook(wlLogfield);
        fdlLogfield=new FormData();
        fdlLogfield.left = new FormAttachment(0, 0);
        fdlLogfield.top  = new FormAttachment(wBatchTrans, margin);
        fdlLogfield.right= new FormAttachment(middle, -margin);
        wlLogfield.setLayoutData(fdlLogfield);
        wLogfield=new Button(shell, SWT.CHECK);
        props.setLook(wLogfield);
        wLogfield.setToolTipText(Messages.getString("JobDialog.UseLogField.Tooltip"));
        fdLogfield=new FormData();
        fdLogfield.left = new FormAttachment(middle, 0);
        fdLogfield.top  = new FormAttachment(wBatchTrans, margin);
        fdLogfield.right= new FormAttachment(100, 0);
        wLogfield.setLayoutData(fdLogfield);

        // Shared objects file
        Label wlSharedObjectsFile = new Label(shell, SWT.RIGHT);
        wlSharedObjectsFile.setText(Messages.getString("JobDialog.SharedObjectsFile.Label"));
        props.setLook(wlSharedObjectsFile);
        FormData fdlSharedObjectsFile = new FormData();
        fdlSharedObjectsFile.left = new FormAttachment(0, 0);
        fdlSharedObjectsFile.right= new FormAttachment(middle, -margin);
        fdlSharedObjectsFile.top  = new FormAttachment(wLogfield, 3*margin);
        wlSharedObjectsFile.setLayoutData(fdlSharedObjectsFile);
        wSharedObjectsFile=new TextVar(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wlSharedObjectsFile.setToolTipText(Messages.getString("JobDialog.SharedObjectsFile.Tooltip"));
        wSharedObjectsFile.setToolTipText(Messages.getString("JobDialog.SharedObjectsFile.Tooltip"));
        props.setLook(wSharedObjectsFile);
        FormData fdSharedObjectsFile = new FormData();
        fdSharedObjectsFile.left = new FormAttachment(middle, 0);
        fdSharedObjectsFile.top  = new FormAttachment(wLogfield, 3*margin);
        fdSharedObjectsFile.right= new FormAttachment(100, 0);
        wSharedObjectsFile.setLayoutData(fdSharedObjectsFile);
        wSharedObjectsFile.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent arg0)
                {
                    sharedObjectsFileChanged = true;
                }
            }
        );

		// THE BUTTONS
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(GlobalMessages.getSystemString("System.Button.OK"));
		wSQL=new Button(shell, SWT.PUSH);
		wSQL.setText(GlobalMessages.getSystemString("System.Button.SQL"));
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(GlobalMessages.getSystemString("System.Button.Cancel"));

        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wSQL, wCancel }, margin, wSharedObjectsFile);
        
		// Add listeners
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		lsSQL      = new Listener() { public void handleEvent(Event e) { sql();    } };
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		
		wOK.addListener    (SWT.Selection, lsOK    );
		wSQL.addListener   (SWT.Selection, lsSQL   );
		wCancel.addListener(SWT.Selection, lsCancel);
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wJobname.addSelectionListener( lsDef );
		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		BaseStepDialog.setSize(shell);

		getData();
		
		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return jobMeta;
	}

	public void dispose()
	{
		WindowProperty winprop = new WindowProperty(shell);
		props.setScreen(winprop);
		shell.dispose();
	}
	
	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{
		log.logDebug(toString(), "getting transformation info...");
	
		if (jobMeta.getName()!=null)           wJobname.setText      ( jobMeta.getName());
		if (jobMeta.getDirectory()!=null)      wDirectory.setText    ( jobMeta.getDirectory().getPath() );
		if (jobMeta.getLogConnection()!=null)  wLogconnection.setText( jobMeta.getLogConnection().getName());
		if (jobMeta.getLogTable()!=null)       wLogtable.setText     ( jobMeta.getLogTable());
        
        wBatch.setSelection(jobMeta.isBatchIdUsed());
        wBatchTrans.setSelection(jobMeta.isBatchIdPassed());
        wLogfield.setSelection(jobMeta.isLogfieldUsed());
        
        wSharedObjectsFile.setText(Const.NVL(jobMeta.getSharedObjectsFile(), ""));
        sharedObjectsFileChanged=false;
        
        changed = jobMeta.hasChanged();

        setFlags();
	}
    
    public void setFlags()
    {
        wbDirectory.setEnabled(rep!=null);
        wDirectory.setEnabled(rep!=null);
        wlDirectory.setEnabled(rep!=null);
        
        DatabaseMeta dbMeta = jobMeta.findDatabase(wLogconnection.getText());
        wbLogconnection.setEnabled(dbMeta!=null);
    }
	
	private void cancel()
	{
		jobMeta.setChanged(changed);
		jobMeta=null;
		dispose();
	}
	
	private void ok()
	{
		jobMeta.setName( wJobname.getText() );
		jobMeta.setLogConnection( jobMeta.findDatabase(wLogconnection.getText()) );
        jobMeta.setLogTable( wLogtable.getText() );
        
        jobMeta.setUseBatchId( wBatch.getSelection());
        jobMeta.setBatchIdPassed( wBatchTrans.getSelection());
        jobMeta.setLogfieldUsed( wLogfield.getSelection());
        jobMeta.setSharedObjectsFile( wSharedObjectsFile.getText() );

		dispose();
	}
	
	// Generate code for create table...
	// Conversions done by Database
	private void sql()
	{
		DatabaseMeta ci = jobMeta.findDatabase(wLogconnection.getText());
		if (ci!=null)
		{
			Row r = Database.getJobLogrecordFields(wBatch.getSelection(), wLogfield.getSelection());
			if (r!=null && r.size()>0)
			{
				String tablename = wLogtable.getText();
				if (tablename!=null && tablename.length()>0)
				{
					Database db = new Database(ci);
					try
					{
                        db.connect();
                        
                        // Get the DDL for the specified tablename and fields...
						String createTable = db.getDDL(tablename, r);
                        
                        if (!Const.isEmpty(createTable))
                        {
    						log.logBasic(toString(), createTable);
    	
    						SQLEditor sqledit = new SQLEditor(shell, SWT.NONE, ci, null, createTable);
    						sqledit.open();
                        }
                        else
                        {
                            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION );
                            mb.setText(Messages.getString("TransDialog.NoSqlNedds.DialogTitle"));
                            mb.setMessage(Messages.getString("TransDialog.NoSqlNedds.DialogMessage"));
                            mb.open(); 
                        }
					}
					catch(KettleDatabaseException dbe)
					{
						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
						mb.setMessage(Messages.getString("JobDialog.Dialog.ErrorCreatingSQL.Message")+Const.CR+dbe.getMessage());
						mb.setText(Messages.getString("JobDialog.Dialog.ErrorCreatingSQL.Title"));
						mb.open();
					}
                    finally
                    {
                        db.disconnect();
                    }
				}
				else
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
					mb.setMessage(Messages.getString("JobDialog.Dialog.PleaseEnterALogTable.Message"));
					mb.setText(Messages.getString("JobDialog.Dialog.PleaseEnterALogTable.Title"));
					mb.open(); 
				}
			}
			else
			{
				MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
				mb.setMessage(Messages.getString("JobDialog.Dialog.CouldNotFindFieldsToCreateLogTable.Message"));
				mb.setText(Messages.getString("JobDialog.Dialog.CouldNotFindFieldsToCreateLogTable.Title"));
				mb.open(); 
			}
		}
		else
		{
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
			mb.setMessage(Messages.getString("JobDialog.Dialog.SelectCreateValidLogConnection.Message"));
			mb.setText(Messages.getString("JobDialog.Dialog.SelectCreateValidLogConnection.Title"));
			mb.open(); 
		}
	}

    public boolean isSharedObjectsFileChanged()
    {
        return sharedObjectsFileChanged;
    }

	public String toString()
	{
		return this.getClass().getName();
	}
    
    public static final void setShellImage(Shell shell, JobEntryInterface jobEntryInterface)
    {
        try
        {
            String id = JobEntryLoader.getInstance().getJobEntryID(jobEntryInterface);
            if (id!=null)
            {
                shell.setImage((Image) GUIResource.getInstance().getImagesJobentries().get(id));
            }
        }
        catch(Throwable e)
        {
        }
    }
}
