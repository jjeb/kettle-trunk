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

package org.pentaho.di.job.entries.eval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.pentaho.di.core.gui.WindowProperty;
import org.pentaho.di.job.dialog.JobDialog;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.trans.step.BaseStepDialog;


/**
 * This dialog allows you to edit a JobEntryEval object.
 * 
 * @author Matt
 * @since 19-06-2003
 */
public class JobEntryEvalDialog extends Dialog implements JobEntryDialogInterface
{
    private Label wlName;

    private Text wName;

    private FormData fdlName, fdName;

    private Label wlScript;

    private Text wScript;

    private FormData fdlScript, fdScript;

    private Label wlPosition;

    private FormData fdlPosition;

    private Button wOK, wCancel;

    private Listener lsOK, lsCancel;

    private JobEntryEval jobEntry;

    private Shell shell;

    private Props props;

    private SelectionAdapter lsDef;

    private boolean changed;

    public JobEntryEvalDialog(Shell parent, JobEntryEval jobEntry)
    {
        super(parent, SWT.NONE);
        props = Props.getInstance();
        this.jobEntry = jobEntry;

        if (this.jobEntry.getName() == null)
            this.jobEntry.setName(Messages.getString("JobEval.Name.Default"));
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
        shell.setText(Messages.getString("JobEval.Title"));

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(Messages.getString("System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(Messages.getString("System.Button.Cancel"));

        // at the bottom
        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, null);

        // Filename line
        wlName = new Label(shell, SWT.NONE);
        wlName.setText(Messages.getString("JobEval.Jobname.Label"));
        props.setLook(wlName);
        fdlName = new FormData();
        fdlName.left = new FormAttachment(0, 0);
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

        wlPosition = new Label(shell, SWT.NONE);
        wlPosition.setText(Messages.getString("JobEval.LineNr.Label", "0"));
        props.setLook(wlPosition);
        fdlPosition = new FormData();
        fdlPosition.left = new FormAttachment(0, 0);
        fdlPosition.bottom = new FormAttachment(wOK, -margin);
        wlPosition.setLayoutData(fdlPosition);

        // Script line
        wlScript = new Label(shell, SWT.NONE);
        wlScript.setText(Messages.getString("JobEval.Script.Label"));
        props.setLook(wlScript);
        fdlScript = new FormData();
        fdlScript.left = new FormAttachment(0, 0);
        fdlScript.top = new FormAttachment(wName, margin);
        wlScript.setLayoutData(fdlScript);
        wScript = new Text(shell, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        wScript.setText(Messages.getString("JobEval.Script.Default"));
        props.setLook(wScript, Props.WIDGET_STYLE_FIXED);
        wScript.addModifyListener(lsMod);
        fdScript = new FormData();
        fdScript.left = new FormAttachment(0, 0);
        fdScript.top = new FormAttachment(wlScript, margin);
        fdScript.right = new FormAttachment(100, -5);
        fdScript.bottom = new FormAttachment(wlPosition, -margin);
        wScript.setLayoutData(fdScript);

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

        // Detect X or ALT-F4 or something that kills this window...
        shell.addShellListener(new ShellAdapter()
        {
            public void shellClosed(ShellEvent e)
            {
                cancel();
            }
        });

        wScript.addKeyListener(new KeyAdapter()
        {
            public void keyReleased(KeyEvent e)
            {
                int linenr = wScript.getCaretLineNumber() + 1;
                wlPosition.setText(Messages.getString("JobEval.LineNr.Label", Integer
                    .toString(linenr)));
            }
        });

        getData();

        BaseStepDialog.setSize(shell, 250, 250, false);

        shell.open();
        props.setDialogSize(shell, "JobEvalDialogSize");
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
        if (jobEntry.getName() != null)
            wName.setText(jobEntry.getName());
        wName.selectAll();
        if (jobEntry.getScript() != null)
            wScript.setText(jobEntry.getScript());
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
        jobEntry.setScript(wScript.getText());
        dispose();
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}
