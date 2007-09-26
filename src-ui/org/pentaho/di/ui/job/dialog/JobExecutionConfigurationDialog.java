package org.pentaho.di.ui.job.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.job.JobExecutionConfiguration;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;



public class JobExecutionConfigurationDialog extends Dialog
{
    private Display display;
    private Shell parent;
    private Shell shell;
    private PropsUI props;
    private boolean retval;
    
    private Button wOK, wCancel;
    
    private Group gLocal;
    
    private JobExecutionConfiguration configuration;
    private JobMeta jobMeta;

    private Button wExecLocal;
    private Button wExecRemote;
    private CCombo wRemoteHost;
    private Label wlRemoteHost;
    private TableView wArguments;
    private Label wlArguments;
    private Label wlVariables;
    private TableView wVariables;
    private Label wlLogLevel;
    private CCombo wLogLevel;
    private Button wSafeMode;

    public JobExecutionConfigurationDialog(Shell parent, JobExecutionConfiguration configuration, JobMeta jobMeta)
    {
        super(parent);
        this.parent = parent;
        this.configuration = configuration;
        this.jobMeta  = jobMeta;
                
        props = PropsUI.getInstance();
    }
    
    public boolean open()
    {
        display = parent.getDisplay();
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
        props.setLook(shell);
		shell.setImage(GUIResource.getInstance().getImageTransGraph());
        
        FormLayout formLayout = new FormLayout ();
        formLayout.marginWidth  = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(Messages.getString("TransExecutionConfigurationDialog.Shell.Title")); //$NON-NLS-1$

        int margin = Const.MARGIN;
        int tabsize = 5*margin;
        int rightMiddle = 2*props.getMiddlePct()/3;
        
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(Messages.getString("TransExecutionConfigurationDialog.Button.Launch"));
        wOK.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { ok(); }});
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(Messages.getString("System.Button.Cancel"));
        wCancel.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { cancel(); }});
        
        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, null);
        
        
        gLocal = new Group(shell, SWT.SHADOW_ETCHED_IN);
        gLocal.setText(Messages.getString("TransExecutionConfigurationDialog.LocalGroup.Label")); //$NON-NLS-1$;
        // The layout
        FormLayout localLayout = new FormLayout();
        localLayout.marginWidth  = Const.FORM_MARGIN;
        localLayout.marginHeight = Const.FORM_MARGIN;
        gLocal.setLayout(localLayout);
        // 
        FormData fdLocal=new FormData();
        fdLocal.left   = new FormAttachment(0, 0);
        fdLocal.right  = new FormAttachment(100, 0);
        gLocal.setBackground(shell.getBackground()); // the default looks ugly
        gLocal.setLayoutData(fdLocal);

        /////////////////////////////////////////////////////////////////////////////////////////////////
        // Local execution
        //
        wExecLocal=new Button(gLocal, SWT.RADIO);
        wExecLocal.setText(Messages.getString("TransExecutionConfigurationDialog.ExecLocal.Label")); //$NON-NLS-1$
        wExecLocal.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.ExecLocal.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setLook(wExecLocal);
        FormData fdExecLocal = new FormData();
        fdExecLocal.left  = new FormAttachment(0, 0);
        fdExecLocal.right = new FormAttachment(33, 0);
        wExecLocal.setLayoutData(fdExecLocal);
        wExecLocal.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { enableFields(); }});
        
        /////////////////////////////////////////////////////////////////////////////////////////////////
        // remote execution
        //
        wExecRemote=new Button(gLocal, SWT.RADIO);
        wExecRemote.setText(Messages.getString("TransExecutionConfigurationDialog.ExecRemote.Label")); //$NON-NLS-1$
        wExecRemote.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.ExecRemote.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setLook(wExecRemote);
        FormData fdExecRemote = new FormData();
        fdExecRemote.left  = new FormAttachment(33, margin);
        fdExecRemote.right = new FormAttachment(66, 0);
        wExecRemote.setLayoutData(fdExecRemote);
        wExecRemote.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { enableFields(); }});

        wlRemoteHost = new Label(gLocal, SWT.LEFT);
        props.setLook(wlRemoteHost);
        wlRemoteHost.setText(Messages.getString("TransExecutionConfigurationDialog.RemoteHost.Label")); //$NON-NLS-1$
        wlRemoteHost.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.RemoteHost.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        FormData fdlRemoteHost = new FormData();
        fdlRemoteHost.left  = new FormAttachment(33, tabsize);
        fdlRemoteHost.top   = new FormAttachment(wExecRemote, margin*2);
        wlRemoteHost.setLayoutData(fdlRemoteHost);

        wRemoteHost = new CCombo(gLocal, SWT.READ_ONLY | SWT.BORDER);
        wRemoteHost.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.RemoteHost.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setLook(wRemoteHost);
        FormData fdRemoteHost = new FormData();
        fdRemoteHost.left  = new FormAttachment(wlRemoteHost, margin);
        fdRemoteHost.right = new FormAttachment(66, 0);
        fdRemoteHost.top   = new FormAttachment(wExecRemote, margin*2);
        wRemoteHost.setLayoutData(fdRemoteHost);
        for (int i=0;i<jobMeta.getSlaveServers().size();i++)
        {
            SlaveServer slaveServer = (SlaveServer)jobMeta.getSlaveServers().get(i);
            wRemoteHost.add(slaveServer.toString());
        }
        
        /////////////////////////////////////////////////////////////////////////////////////////////////
        // Replay date, arguments & variables
        //

        wSafeMode = new Button(shell, SWT.CHECK);
        wSafeMode.setText(Messages.getString("TransExecutionConfigurationDialog.SafeMode.Label")); //$NON-NLS-1$
        wSafeMode.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.SafeMode.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setLook(wSafeMode);
        FormData fdSafeMode = new FormData();
        fdSafeMode.left  = new FormAttachment( 50, margin);
        fdSafeMode.right = new FormAttachment(100, 0);
        fdSafeMode.top   = new FormAttachment(gLocal, margin*2);
        wSafeMode.setLayoutData(fdSafeMode);
        wSafeMode.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { enableFields(); }});

        wlLogLevel = new Label(shell, SWT.LEFT);
        props.setLook(wlLogLevel);
        wlLogLevel.setText(Messages.getString("TransExecutionConfigurationDialog.LogLevel.Label")); //$NON-NLS-1$
        wlLogLevel.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.LogLevel.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        FormData fdlLogLevel = new FormData();
        fdlLogLevel.left  = new FormAttachment(50, margin);
        fdlLogLevel.right = new FormAttachment(50+rightMiddle, 0);
        fdlLogLevel.top   = new FormAttachment(wSafeMode, margin*2);
        wlLogLevel.setLayoutData(fdlLogLevel);

        wLogLevel = new CCombo(shell, SWT.READ_ONLY | SWT.BORDER);
        wLogLevel.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.LogLevel.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setLook(wLogLevel);
        FormData fdLogLevel = new FormData();
        fdLogLevel.left  = new FormAttachment(50+rightMiddle, margin);
        fdLogLevel.right = new FormAttachment(100, 0);
        fdLogLevel.top   = new FormAttachment(wSafeMode, margin*2);
        wLogLevel.setLayoutData(fdLogLevel);
        wLogLevel.setItems( LogWriter.log_level_desc_long );

        // Arguments
        wlArguments = new Label(shell, SWT.LEFT);
        props.setLook(wlArguments);
        wlArguments.setText(Messages.getString("TransExecutionConfigurationDialog.Arguments.Label")); //$NON-NLS-1$
        wlArguments.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.Arguments.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        FormData fdlArguments = new FormData();
        fdlArguments.left   = new FormAttachment(0, 0);
        fdlArguments.right  = new FormAttachment(50, -margin);
        fdlArguments.top    = new FormAttachment(wLogLevel, margin*2);
        wlArguments.setLayoutData(fdlArguments);

        ColumnInfo[] cArguments = {
            new ColumnInfo( Messages.getString("TransExecutionConfigurationDialog.ArgumentsColumn.Argument"), ColumnInfo.COLUMN_TYPE_TEXT, false, true ), //Stepname
            new ColumnInfo( Messages.getString("TransExecutionConfigurationDialog.ArgumentsColumn.Value"), ColumnInfo.COLUMN_TYPE_TEXT, false, false), //Preview size
          };
              
        int nrArguments = configuration.getArguments() !=null ? configuration.getArguments().size() : 0; 
        wArguments = new TableView(jobMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, cArguments, nrArguments, true, null, props);
        FormData fdArguments = new FormData();
        fdArguments.left   = new FormAttachment(0, 0);
        fdArguments.right  = new FormAttachment(50, -margin);
        fdArguments.top    = new FormAttachment(wlArguments, margin);
        fdArguments.bottom = new FormAttachment(wOK, -margin*2);
        wArguments.setLayoutData(fdArguments);

        // Variables
        wlVariables = new Label(shell, SWT.LEFT);
        props.setLook(wlVariables);
        wlVariables.setText(Messages.getString("TransExecutionConfigurationDialog.Variables.Label")); //$NON-NLS-1$
        wlVariables.setToolTipText(Messages.getString("TransExecutionConfigurationDialog.Variables.Tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
        FormData fdlVariables = new FormData();
        fdlVariables.left   = new FormAttachment(50, margin);
        fdlVariables.right  = new FormAttachment(100, 0);
        fdlVariables.top    = new FormAttachment(wLogLevel, margin*2);
        wlVariables.setLayoutData(fdlVariables);

        ColumnInfo[] cVariables = {
            new ColumnInfo( Messages.getString("TransExecutionConfigurationDialog.VariablesColumn.Argument"), ColumnInfo.COLUMN_TYPE_TEXT, false, false), //Stepname
            new ColumnInfo( Messages.getString("TransExecutionConfigurationDialog.VariablesColumn.Value"), ColumnInfo.COLUMN_TYPE_TEXT, false, false), //Preview size
          };
              
        int nrVariables = configuration.getVariables() !=null ? configuration.getVariables().size() : 0; 
        wVariables = new TableView(jobMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, cVariables, nrVariables, true, null, props);
        FormData fdVariables = new FormData();
        fdVariables.left   = new FormAttachment(50, margin);
        fdVariables.right  = new FormAttachment(100, 0);
        fdVariables.top    = new FormAttachment(wlVariables, margin);
        fdVariables.bottom = new FormAttachment(wOK, -margin*2);
        wVariables.setLayoutData(fdVariables);
        
        getData();
        
        BaseStepDialog.setSize(shell);
        
        // Set the focus on the OK button
        wOK.setFocus();
        
        shell.open();
        while (!shell.isDisposed())
        {
            if (!display.readAndDispatch()) display.sleep();
        }
        
        return retval;
    }

    private void getVariablesData()
    {
        wVariables.clearAll(false);
        List<String> variableNames = new ArrayList<String>( configuration.getVariables().keySet() );
        Collections.sort(variableNames);
        
        for (int i=0;i<variableNames.size();i++)
        {
        	String variableName = variableNames.get(i);
        	String variableValue = configuration.getVariables().get(variableName);
        	
            TableItem tableItem = new TableItem(wVariables.table, SWT.NONE);
            tableItem.setText(1, variableName);
            tableItem.setText(2, Const.NVL(variableValue, ""));;
        }
        wVariables.removeEmptyRows();
        wVariables.setRowNums();
        wVariables.optWidth(true);
    }

    private void getArgumentsData()
    {
        wArguments.clearAll(false);
        
        List<String> argumentNames = new ArrayList<String>( configuration.getArguments().keySet() );
        Collections.sort(argumentNames);
        
        for (int i=0;i<argumentNames.size();i++)
        {
        	String argumentName = argumentNames.get(i);
        	String argumentValue = configuration.getArguments().get(argumentName);
        	
            TableItem tableItem = new TableItem(wArguments.table, SWT.NONE);
            tableItem.setText(1, Const.NVL(argumentName, ""));
            tableItem.setText(2, Const.NVL(argumentValue, ""));
        }
        wArguments.removeEmptyRows();
        wArguments.setRowNums();
        wArguments.optWidth(true);
    }

    private void cancel()
    {
        dispose();
    }
    
    private void dispose()
    {
        props.setScreen(new WindowProperty(shell));
        shell.dispose();
    }

    private void ok()
    {
        getInfo();
        retval=true;
        dispose();
    }
    
    public void getData()
    {
        wExecLocal.setSelection(configuration.isExecutingLocally());
        wExecRemote.setSelection(configuration.isExecutingRemotely());
        wSafeMode.setSelection(configuration.isSafeModeEnabled());
        wRemoteHost.setText( configuration.getRemoteServer()==null ? "" : configuration.getRemoteServer().toString() );
        wLogLevel.setText( LogWriter.getInstance().getLogLevelDesc() );
        getArgumentsData();
        getVariablesData();
        
        enableFields();
    }
    
    public void getInfo()
    {
        try
        {
            configuration.setExecutingLocally(wExecLocal.getSelection());
            configuration.setExecutingRemotely(wExecRemote.getSelection());
            
            // Remote data
            //
            if (wExecRemote.getSelection())
            {
                String serverName = wRemoteHost.getText();
                configuration.setRemoteServer(jobMeta.findSlaveServer(serverName));
            }
            
            // various settings
            //
            configuration.setSafeModeEnabled(wSafeMode.getSelection() );
            configuration.setLogLevel( LogWriter.getLogLevel(wLogLevel.getText()) );
            
            // The lower part of the dialog...
            getInfoVariables();
            getInfoArguments();
        }
        catch(Exception e)
        {
            new ErrorDialog(shell, "Error in settings", "There is an error in the dialog settings", e);
        }
    }
    
    private void getInfoVariables()
    {
        Map<String,String> map = new HashMap<String, String>();
    	int nrNonEmptyVariables = wVariables.nrNonEmpty(); 
        for (int i=0;i<nrNonEmptyVariables;i++)
        {
            TableItem tableItem = wVariables.getNonEmpty(i);
            String varName = tableItem.getText(1);
            String varValue = tableItem.getText(2);
            
            if (!Const.isEmpty(varName))
            {
                map.put(varName, varValue);
            }
        }
        configuration.setVariables(map);
    }
    
    private void getInfoArguments()
    {
    	Map<String,String> map = new HashMap<String, String>();
    	int nrNonEmptyArguments = wArguments.nrNonEmpty(); 
    	for (int i=0;i<nrNonEmptyArguments;i++)
        {
            TableItem tableItem = wArguments.getNonEmpty(i);
            String varName = tableItem.getText(1);
            String varValue = tableItem.getText(2);
            
            if (!Const.isEmpty(varName))
            {
                map.put(varName, varValue);
            }
        }
        configuration.setArguments(map);
    }
    
    private void enableFields()
    {
        boolean enableRemote = wExecRemote.getSelection();
                
        wRemoteHost.setEnabled(enableRemote);
        wlRemoteHost.setEnabled(enableRemote);
    }

    
    
    /**
     * @return the configuration
     */
    public JobExecutionConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(JobExecutionConfiguration configuration)
    {
        this.configuration = configuration;
    }
}
