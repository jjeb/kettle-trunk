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

/*
 * Created on 19-jun-2003
 *
 */

package org.pentaho.di.job.entries.tableexists;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.dialog.DatabaseDialog;
import org.pentaho.di.core.gui.WindowProperty;
import org.pentaho.di.core.widget.TextVar;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.dialog.JobDialog;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.trans.step.BaseStepDialog;


/**
 * This dialog allows you to edit the Table Exists job entry settings. (select the connection and
 * the table to be checked) This entry type evaluates!
 * 
 * @author Matt
 * @since 19-06-2003
 */
public class JobEntryTableExistsDialog extends Dialog implements JobEntryDialogInterface
{
    private Label wlName;

    private Text wName;

    private FormData fdlName, fdName;

    private Label wlConnection;

    private CCombo wConnection;

    private Button wbConnection;

    private FormData fdlConnection, fdbConnection, fdConnection;

    private Label wlTablename;

    private TextVar wTablename;

    private FormData fdlTablename, fdTablename;

    private Button wOK, wCancel;

    private Listener lsOK, lsCancel;

    private JobEntryTableExists jobEntry;

    private JobMeta jobMeta;

    private Shell shell;

    private Props props;

    private SelectionAdapter lsDef;

    private boolean changed;

    public JobEntryTableExistsDialog(Shell parent, JobEntryTableExists jobEntry, JobMeta jobMeta)
    {
        super(parent, SWT.NONE);
        props = Props.getInstance();
        this.jobEntry = jobEntry;
        this.jobMeta = jobMeta;

        if (this.jobEntry.getName() == null)
            this.jobEntry.setName(Messages.getString("JobTableExists.Name.Default"));
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

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(Messages.getString("JobTableExists.Title"));

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        // Filename line
        wlName = new Label(shell, SWT.RIGHT);
        wlName.setText(Messages.getString("JobTableExists.Name.Label"));
        props.setLook(wlName);
        fdlName = new FormData();
        fdlName.left = new FormAttachment(0, 0);
        fdlName.right = new FormAttachment(middle, 0);
        fdlName.top = new FormAttachment(0, margin);
        wlName.setLayoutData(fdlName);
        wName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wName);
        wName.addModifyListener(lsMod);
        fdName = new FormData();
        fdName.left = new FormAttachment(middle, 0);
        fdName.top = new FormAttachment(0, margin);
        fdName.right = new FormAttachment(100, 0);
        wName.setLayoutData(fdName);

        // Connection line
        wlConnection = new Label(shell, SWT.RIGHT);
        wlConnection.setText(Messages.getString("JobTableExists.Connection.Label"));
        props.setLook(wlConnection);
        fdlConnection = new FormData();
        fdlConnection.left = new FormAttachment(0, 0);
        fdlConnection.top = new FormAttachment(wName, margin);
        fdlConnection.right = new FormAttachment(middle, -margin);
        wlConnection.setLayoutData(fdlConnection);

        wbConnection = new Button(shell, SWT.PUSH);
        wbConnection.setText(Messages.getString("System.Button.New") + "...");
        wbConnection.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                DatabaseMeta databaseMeta = new DatabaseMeta();
                DatabaseDialog cid = new DatabaseDialog(shell, databaseMeta);
                if (cid.open() != null)
                {
                    jobMeta.addDatabase(databaseMeta);

                    // SB: Maybe do the same her as in BaseStepDialog: remove
                    // all db connections and add them again.
                    wConnection.add(databaseMeta.getName());
                    wConnection.select(wConnection.getItemCount() - 1);
                }
            }
        });
        fdbConnection = new FormData();
        fdbConnection.right = new FormAttachment(100, 0);
        fdbConnection.top = new FormAttachment(wName, margin);
        fdbConnection.height = 20;
        wbConnection.setLayoutData(fdbConnection);

        wConnection = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        props.setLook(wConnection);
        for (int i = 0; i < jobMeta.nrDatabases(); i++)
        {
            DatabaseMeta ci = jobMeta.getDatabase(i);
            wConnection.add(ci.getName());
        }
        wConnection.select(0);
        wConnection.addModifyListener(lsMod);
        fdConnection = new FormData();
        fdConnection.left = new FormAttachment(middle, 0);
        fdConnection.top = new FormAttachment(wName, margin);
        fdConnection.right = new FormAttachment(wbConnection, -margin);
        wConnection.setLayoutData(fdConnection);

        // Table name line
        wlTablename = new Label(shell, SWT.RIGHT);
        wlTablename.setText(Messages.getString("JobTableExists.Tablename.Label"));
        props.setLook(wlTablename);
        fdlTablename = new FormData();
        fdlTablename.left = new FormAttachment(0, 0);
        fdlTablename.right = new FormAttachment(middle, 0);
        fdlTablename.top = new FormAttachment(wConnection, margin);
        wlTablename.setLayoutData(fdlTablename);

        wTablename = new TextVar(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wTablename);
        wTablename.addModifyListener(lsMod);
        fdTablename = new FormData();
        fdTablename.left = new FormAttachment(middle, 0);
        fdTablename.top = new FormAttachment(wConnection, margin);
        fdTablename.right = new FormAttachment(100, 0);
        wTablename.setLayoutData(fdTablename);

        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(Messages.getString("System.Button.OK"));
        FormData fd = new FormData();
        fd.right = new FormAttachment(50, -10);
        fd.bottom = new FormAttachment(100, 0);
        fd.width = 100;
        wOK.setLayoutData(fd);

        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(Messages.getString("System.Button.Cancel"));
        fd = new FormData();
        fd.left = new FormAttachment(50, 10);
        fd.bottom = new FormAttachment(100, 0);
        fd.width = 100;
        wCancel.setLayoutData(fd);

        // Add listeners
        lsCancel = new Listener()
        {
            public void handleEvent(Event e)
            {
                cancel();
            }
        };
        lsOK = new Listener()
        {
            public void handleEvent(Event e)
            {
                ok();
            }
        };

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);

        lsDef = new SelectionAdapter()
        {
            public void widgetDefaultSelected(SelectionEvent e)
            {
                ok();
            }
        };

        wName.addSelectionListener(lsDef);
        wTablename.addSelectionListener(lsDef);

        // Detect X or ALT-F4 or something that kills this window...
        shell.addShellListener(new ShellAdapter()
        {
            public void shellClosed(ShellEvent e)
            {
                cancel();
            }
        });

        getData();

        BaseStepDialog.setSize(shell);

        shell.open();
        props.setDialogSize(shell, "JobTableExistsDialogSize");
        while (!shell.isDisposed())
        {
            if (!display.readAndDispatch())
                display.sleep();
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
        // System.out.println("evaluates: "+jobentry.evaluates());

        if (jobEntry.getName() != null)
            wName.setText(jobEntry.getName());
        if (jobEntry.getTablename() != null)
            wTablename.setText(jobEntry.getTablename());
        if (jobEntry.getDatabase() != null)
        {
            wConnection.setText(jobEntry.getDatabase().getName());
        }
        wName.selectAll();
    }

    private void cancel()
    {
        jobEntry.setChanged(changed);
        jobEntry = null;
        dispose();
    }

    private void ok()
    {
        jobEntry.setName(wName.getText());
        jobEntry.setDatabase(jobMeta.findDatabase(wConnection.getText()));
        jobEntry.setTablename(wTablename.getText());
        dispose();
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}
