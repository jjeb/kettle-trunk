 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 **                                                                   **
 **                                                                   **
 **********************************************************************/


package be.ibridge.kettle.job.entry.createfile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Props;
import be.ibridge.kettle.core.WindowProperty;
import be.ibridge.kettle.core.util.StringUtil;
import be.ibridge.kettle.core.widget.TextVar;
import be.ibridge.kettle.job.JobMeta;
import be.ibridge.kettle.job.dialog.JobDialog;
import be.ibridge.kettle.job.entry.JobEntryDialogInterface;
import be.ibridge.kettle.job.entry.JobEntryInterface;
import be.ibridge.kettle.trans.step.BaseStepDialog;


/**
 * This dialog allows you to edit the Create File job entry settings.
 *
 * @author Sven Boden
 * @since  28-01-2007
 */
public class JobEntryCreateFileDialog extends Dialog implements JobEntryDialogInterface
{
   private static final String[] FILETYPES = new String[] {
           Messages.getString("JobCreateFile.Filetype.All") };
	
	private Label        wlName;
	private Text         wName;
    private FormData     fdlName, fdName;

	private Label        wlFilename;
	private Button       wbFilename;
	private TextVar      wFilename;
	private FormData     fdlFilename, fdbFilename, fdFilename;
	
    private Label        wlAbortExists;
    private Button       wAbortExists;
    private FormData     fdlAbortExists, fdAbortExists;

	private Button wOK, wCancel;
	private Listener lsOK, lsCancel;

	private JobEntryCreateFile jobEntry;
	private Shell       	shell;
	private Props       	props;

	private SelectionAdapter lsDef;

	private boolean changed;

	public JobEntryCreateFileDialog(Shell parent, JobEntryCreateFile jobEntry, JobMeta jobMeta)
	{
		super(parent, SWT.NONE);
		props=Props.getInstance();
		this.jobEntry=jobEntry;

		if (this.jobEntry.getName() == null) 
			this.jobEntry.setName(Messages.getString("JobCreateFile.Name.Default"));
	}

	public JobEntryInterface open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

        shell = new Shell(parent, props.getJobsDialogStyle());
        props.setLook(shell);
        JobDialog.setShellImage(shell, jobEntry);

		ModifyListener lsMod = new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				jobEntry.setChanged();
			}
		};
		changed = jobEntry.hasChanged();

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("JobCreateFile.Title"));

		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Filename line
		wlName=new Label(shell, SWT.RIGHT);
		wlName.setText(Messages.getString("JobCreateFile.Name.Label"));
 		props.setLook(wlName);
		fdlName=new FormData();
		fdlName.left = new FormAttachment(0, 0);
		fdlName.right= new FormAttachment(middle, -margin);
		fdlName.top  = new FormAttachment(0, margin);
		wlName.setLayoutData(fdlName);
		wName=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wName);
		wName.addModifyListener(lsMod);
		fdName=new FormData();
		fdName.left = new FormAttachment(middle, 0);
		fdName.top  = new FormAttachment(0, margin);
		fdName.right= new FormAttachment(100, 0);
		wName.setLayoutData(fdName);

		// Filename line
		wlFilename=new Label(shell, SWT.RIGHT);
		wlFilename.setText(Messages.getString("JobCreateFile.Filename.Label"));
 		props.setLook(wlFilename);
		fdlFilename=new FormData();
		fdlFilename.left = new FormAttachment(0, 0);
		fdlFilename.top  = new FormAttachment(wName, margin);
		fdlFilename.right= new FormAttachment(middle, -margin);
		wlFilename.setLayoutData(fdlFilename);

		wbFilename=new Button(shell, SWT.PUSH| SWT.CENTER);
 		props.setLook(wbFilename);
		wbFilename.setText(Messages.getString("System.Button.Browse"));
		fdbFilename=new FormData();
		fdbFilename.right= new FormAttachment(100, 0);
		fdbFilename.top  = new FormAttachment(wName, 0);
		wbFilename.setLayoutData(fdbFilename);

		wFilename=new TextVar(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wFilename);
		wFilename.addModifyListener(lsMod);
		fdFilename=new FormData();
		fdFilename.left = new FormAttachment(middle, 0);
		fdFilename.top  = new FormAttachment(wName, margin);
		fdFilename.right= new FormAttachment(wbFilename, -margin);
		wFilename.setLayoutData(fdFilename);

		// Whenever something changes, set the tooltip to the expanded version:
		wFilename.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					wFilename.setToolTipText(StringUtil.environmentSubstitute( wFilename.getText() ) );
				}
			}
		);

		wbFilename.addSelectionListener
		(
			new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e)
				{
					FileDialog dialog = new FileDialog(shell, SWT.OPEN);
					dialog.setFilterExtensions(new String[] {"*"});
					if (wFilename.getText()!=null)
					{
						dialog.setFileName(StringUtil.environmentSubstitute(wFilename.getText()) );
					}
					dialog.setFilterNames(FILETYPES);
					if (dialog.open()!=null)
					{
						wFilename.setText(dialog.getFilterPath()+Const.FILE_SEPARATOR+dialog.getFileName());
					}
				}
			}
		);

        wlAbortExists = new Label(shell, SWT.RIGHT);
        wlAbortExists.setText(Messages.getString("JobCreateFile.FailIfExists.Label"));
        props.setLook(wlAbortExists);
        fdlAbortExists = new FormData();
        fdlAbortExists.left = new FormAttachment(0, 0);
        fdlAbortExists.top = new FormAttachment(wFilename, margin);
        fdlAbortExists.right = new FormAttachment(middle, -margin);
        wlAbortExists.setLayoutData(fdlAbortExists);
        wAbortExists = new Button(shell, SWT.CHECK);
        props.setLook(wAbortExists);
        wAbortExists.setToolTipText(Messages.getString("JobCreateFile.FailIfExists.Tooltip"));
        fdAbortExists = new FormData();
        fdAbortExists.left = new FormAttachment(middle, 0);
        fdAbortExists.top = new FormAttachment(wFilename, margin);
        fdAbortExists.right = new FormAttachment(100, 0);
        wAbortExists.setLayoutData(fdAbortExists);
        wAbortExists.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                jobEntry.setChanged();
            }
        });
		
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(Messages.getString("System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(Messages.getString("System.Button.Cancel"));
        
		BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, wAbortExists);

		// Add listeners
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };

		wCancel.addListener(SWT.Selection, lsCancel);
		wOK.addListener    (SWT.Selection, lsOK    );

		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };

		wName.addSelectionListener( lsDef );
		wFilename.addSelectionListener( lsDef );

		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		getData();

		BaseStepDialog.setSize(shell);

		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return jobEntry;
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
		if (jobEntry.getName()    != null) wName.setText( jobEntry.getName() );
		wName.selectAll();
		if (jobEntry.getFilename()!= null) wFilename.setText( jobEntry.getFilename() );
		wAbortExists.setSelection(jobEntry.isFailIfFileExists());
	}

	private void cancel()
	{
		jobEntry.setChanged(changed);
		jobEntry=null;
		dispose();
	}

	private void ok()
	{
		jobEntry.setName(wName.getText());
		jobEntry.setFilename(wFilename.getText());
		jobEntry.setFailIfFileExists(wAbortExists.getSelection());
		dispose();
	}

	public String toString()
	{
		return this.getClass().getName();
	}

	public boolean evaluates()
	{
		return true;
	}

	public boolean isUnconditional()
	{
		return false;
	}
}