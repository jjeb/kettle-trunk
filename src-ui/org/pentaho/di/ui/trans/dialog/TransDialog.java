 /* Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/

 
/*
 * Created on 2-jul-2003
 *
 */

package org.pentaho.di.ui.trans.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.parameters.DuplicateParamException;
import org.pentaho.di.core.parameters.UnknownParamException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.trans.TransDependency;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.databaselookup.DatabaseLookupMeta;
import org.pentaho.di.trans.steps.tableinput.TableInputMeta;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.database.dialog.DatabaseDialog;
import org.pentaho.di.ui.core.database.dialog.SQLEditor;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.repository.dialog.SelectDirectoryDialog;
import org.pentaho.di.ui.trans.step.BaseStepDialog;


public class TransDialog extends Dialog
{
    private static Class<?> PKG = TransDialog.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  public static enum Tabs {TRANS_TAB, PARAM_TAB, LOG_TAB, DATE_TAB, DEP_TAB, MISC_TAB, MONITOR_TAB};
  
	private CTabFolder   wTabFolder;
	private FormData     fdTabFolder;
	
	private CTabItem     wTransTab, wParamTab, wLogTab, wDateTab, wDepTab, wMiscTab, wMonitorTab;

	private Text         wTransname;

	private Text         wTransFilename;

	// Trans description
	private Text         wTransdescription;
	
	private Label        wlExtendeddescription;
	private Text         wExtendeddescription;

	// Trans Status
	private Label    wlTransstatus;
	private CCombo   wTransstatus;
	private FormData fdlTransstatus, fdTransstatus;

	//Trans version
	private Text         wTransversion;

	private FormData fdlExtendeddescription, fdExtendeddescription;
    
    private Text         wDirectory;
	private Button       wbDirectory;
    	    
	private Text         wCreateUser;
	private Text         wCreateDate;

	private Text         wModUser;
	private Text         wModDate;
	private CCombo       wReadStep;
	private CCombo       wInputStep;
	private CCombo       wWriteStep;
	private CCombo       wOutputStep;
	private CCombo       wUpdateStep;

	private Button       wbLogconnection;
	private CCombo       wLogconnection;
	private Text         wLogtable;
	private Text         wStepLogtable;
	private Button       wBatch;
	private Button       wLogfield;
	
	private Label        wlLogSizeLimit;
	private TextVar      wLogSizeLimit;

	private CCombo       wMaxdateconnection;
	private Text         wMaxdatetable;
	private Text         wMaxdatefield;
	private Text         wMaxdateoffset;
	private Text         wMaxdatediff;
	
	private TableView    wFields;
	
	private TableView    wParamFields;

	private Text         wSizeRowset;
    private Button       wUniqueConnections;

	private Button wOK, wGet, wSQL, wCancel;
	private FormData fdGet;
	private Listener lsOK, lsGet, lsSQL, lsCancel;

	private TransMeta transMeta;
	private Shell  shell;
	
	private SelectionAdapter lsDef;
	
	private ModifyListener lsMod;
	private Repository rep;
	private PropsUI props;
	private RepositoryDirectory newDirectory;

    private int middle;

    private int margin;

    private String[] connectionNames;

    private Button wShowFeedback;

    private Text wFeedbackSize;

    private TextVar wSharedObjectsFile;
    
    private boolean sharedObjectsFileChanged;

    private Button wManageThreads;

    private CCombo wRejectedStep;

	private boolean directoryChangeAllowed;

	private Label wlDirectory;

	private Button wEnableStepPerfMonitor;

	private Text wEnableStepPerfInterval;

	private Label wlStepLogtable;
	
	private Tabs currentTab = null;

	protected boolean	changed;
	
  public TransDialog(Shell parent, int style, TransMeta transMeta, Repository rep, Tabs currentTab)
  {
      this(parent, style, transMeta, rep);
      this.currentTab = currentTab;
  }

  public TransDialog(Shell parent, int style, TransMeta transMeta, Repository rep)
    {
        super(parent, style);
        this.props    = PropsUI.getInstance();
        this.transMeta    = transMeta;
        this.rep      = rep;
        
        this.newDirectory = null;
        
        directoryChangeAllowed=true;
        changed=false;
    }


	public TransMeta open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
 		props.setLook(shell);
		shell.setImage((Image) GUIResource.getInstance().getImageTransGraph());
	
		
		lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				changed=true;
			}
		};
		
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(BaseMessages.getString(PKG, "TransDialog.Shell.Title")); //$NON-NLS-1$
		
		middle = props.getMiddlePct();
		margin = Const.MARGIN;
        
		wTabFolder = new CTabFolder(shell, SWT.BORDER);
 		props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);
 		wTabFolder.setSimple(false);

 		addTransTab();
 		addParamTab();
		addLogTab();
		addDateTab();
		addDepTab();
		addMiscTab();
        addMonitoringTab();
		
		fdTabFolder = new FormData();
		fdTabFolder.left  = new FormAttachment(0, 0);
		fdTabFolder.top   = new FormAttachment(0, 0);
		fdTabFolder.right = new FormAttachment(100, 0);
		fdTabFolder.bottom= new FormAttachment(100, -50);
		wTabFolder.setLayoutData(fdTabFolder);
		

		// THE BUTTONS
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(BaseMessages.getString(PKG, "System.Button.OK")); //$NON-NLS-1$
		wSQL=new Button(shell, SWT.PUSH);
		wSQL.setText(BaseMessages.getString(PKG, "System.Button.SQL")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel")); //$NON-NLS-1$
		
		BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wSQL, wCancel }, Const.MARGIN, null);
		
		// Add listeners
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		lsGet      = new Listener() { public void handleEvent(Event e) { get();    } };
		lsSQL      = new Listener() { public void handleEvent(Event e) { sql();    } };
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		
		wOK.addListener    (SWT.Selection, lsOK    );
		wGet.addListener   (SWT.Selection, lsGet   );
		wSQL.addListener   (SWT.Selection, lsSQL   );
		wCancel.addListener(SWT.Selection, lsCancel);
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wTransname.addSelectionListener( lsDef );
        wTransdescription.addSelectionListener( lsDef );
        wTransversion.addSelectionListener( lsDef );
		wMaxdatetable.addSelectionListener( lsDef );
		wMaxdatefield.addSelectionListener( lsDef );
		wMaxdateoffset.addSelectionListener( lsDef );
		wMaxdatediff.addSelectionListener( lsDef );
		wLogtable.addSelectionListener( lsDef );
		wStepLogtable.addSelectionListener( lsDef );
		wLogSizeLimit.addSelectionListener( lsDef );
		wSizeRowset.addSelectionListener( lsDef );
        wUniqueConnections.addSelectionListener( lsDef );
        wFeedbackSize.addSelectionListener( lsDef );
        wEnableStepPerfInterval.addSelectionListener( lsDef );

		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		if (currentTab != null){
		  setCurrentTab(currentTab);
		}else{
	    wTabFolder.setSelection(0);
		}

		getData();

		BaseStepDialog.setSize(shell);

		changed = false;
		sharedObjectsFileChanged = false;
		
		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return transMeta;
	}

	private void addTransTab()
    {
        //////////////////////////
        // START OF TRANS TAB///
        ///
        wTransTab=new CTabItem(wTabFolder, SWT.NONE);
        wTransTab.setText(BaseMessages.getString(PKG, "TransDialog.TransTab.Label")); //$NON-NLS-1$
        
        Composite wTransComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wTransComp);

        FormLayout transLayout = new FormLayout();
        transLayout.marginWidth  = Const.FORM_MARGIN;
        transLayout.marginHeight = Const.FORM_MARGIN;
        wTransComp.setLayout(transLayout);


        // Transformation name:
        Label wlTransname = new Label(wTransComp, SWT.RIGHT);
        wlTransname.setText(BaseMessages.getString(PKG, "TransDialog.Transname.Label")); //$NON-NLS-1$
        props.setLook(wlTransname);
        FormData fdlTransname = new FormData();
        fdlTransname.left = new FormAttachment(0, 0);
        fdlTransname.right= new FormAttachment(middle, -margin);
        fdlTransname.top  = new FormAttachment(0, margin);
        wlTransname.setLayoutData(fdlTransname);
        wTransname=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wTransname);
        wTransname.addModifyListener(lsMod);
        FormData fdTransname = new FormData();
        fdTransname.left = new FormAttachment(middle, 0);
        fdTransname.top  = new FormAttachment(0, margin);
        fdTransname.right= new FormAttachment(100, 0);
        wTransname.setLayoutData(fdTransname);

        // Transformation name:
        Label wlTransFilename = new Label(wTransComp, SWT.RIGHT);
        wlTransFilename.setText(BaseMessages.getString(PKG, "TransDialog.TransFilename.Label")); //$NON-NLS-1$
        props.setLook(wlTransFilename);
        FormData fdlTransFilename = new FormData();
        fdlTransFilename.left = new FormAttachment(0, 0);
        fdlTransFilename.right= new FormAttachment(middle, -margin);
        fdlTransFilename.top  = new FormAttachment(wTransname, margin);
        wlTransFilename.setLayoutData(fdlTransFilename);
        wTransFilename=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wTransFilename);
        wTransFilename.addModifyListener(lsMod);
        FormData fdTransFilename = new FormData();
        fdTransFilename.left = new FormAttachment(middle, 0);
        fdTransFilename.top  = new FormAttachment(wTransname, margin);
        fdTransFilename.right= new FormAttachment(100, 0);
        wTransFilename.setLayoutData(fdTransFilename);
        wTransFilename.setEditable(false);

		// Transformation description:
		Label wlTransdescription = new Label(wTransComp, SWT.RIGHT);
		wlTransdescription.setText(BaseMessages.getString(PKG, "TransDialog.Transdescription.Label")); //$NON-NLS-1$
		props.setLook(wlTransdescription);
		FormData fdlTransdescription = new FormData();
		fdlTransdescription.left = new FormAttachment(0, 0);
		fdlTransdescription.right= new FormAttachment(middle, -margin);
		fdlTransdescription.top  = new FormAttachment(wTransFilename, margin);
		wlTransdescription.setLayoutData(fdlTransdescription);
		wTransdescription=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wTransdescription);
		wTransdescription.addModifyListener(lsMod);
		FormData fdTransdescription = new FormData();
		fdTransdescription.left = new FormAttachment(middle, 0);
		fdTransdescription.top  = new FormAttachment(wTransFilename, margin);
		fdTransdescription.right= new FormAttachment(100, 0);
		wTransdescription.setLayoutData(fdTransdescription);
        

		// Transformation Extended description
		wlExtendeddescription = new Label(wTransComp, SWT.RIGHT);
		wlExtendeddescription.setText(BaseMessages.getString(PKG, "TransDialog.Extendeddescription.Label"));
		props.setLook(wlExtendeddescription);
		fdlExtendeddescription = new FormData();
		fdlExtendeddescription.left = new FormAttachment(0, 0);
		fdlExtendeddescription.top = new FormAttachment(wTransdescription, margin);
		fdlExtendeddescription.right = new FormAttachment(middle, -margin);
		wlExtendeddescription.setLayoutData(fdlExtendeddescription);

		wExtendeddescription = new Text(wTransComp, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		props.setLook(wExtendeddescription,Props.WIDGET_STYLE_FIXED);
		wExtendeddescription.addModifyListener(lsMod);
		fdExtendeddescription = new FormData();
		fdExtendeddescription.left = new FormAttachment(middle, 0);
		fdExtendeddescription.top = new FormAttachment(wTransdescription, margin);
		fdExtendeddescription.right = new FormAttachment(100, 0);
		fdExtendeddescription.bottom =new FormAttachment(50, -margin);
		wExtendeddescription.setLayoutData(fdExtendeddescription);

		//Trans Status
		wlTransstatus = new Label(wTransComp, SWT.RIGHT);
		wlTransstatus.setText(BaseMessages.getString(PKG, "TransDialog.Transstatus.Label"));
		props.setLook(wlTransstatus);
		fdlTransstatus = new FormData();
		fdlTransstatus.left = new FormAttachment(0, 0);
		fdlTransstatus.right = new FormAttachment(middle, 0);
		fdlTransstatus.top = new FormAttachment(wExtendeddescription, margin*2);
		wlTransstatus.setLayoutData(fdlTransstatus);
		wTransstatus = new CCombo(wTransComp, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
		wTransstatus.add(BaseMessages.getString(PKG, "TransDialog.Draft_Transstatus.Label"));
		wTransstatus.add(BaseMessages.getString(PKG, "TransDialog.Production_Transstatus.Label"));
		wTransstatus.add("");
		wTransstatus.select(-1); // +1: starts at -1

		props.setLook(wTransstatus);
		fdTransstatus= new FormData();
		fdTransstatus.left = new FormAttachment(middle, 0);
		fdTransstatus.top = new FormAttachment(wExtendeddescription, margin*2);
		fdTransstatus.right = new FormAttachment(100, 0);
		wTransstatus.setLayoutData(fdTransstatus);


		// Transformation Transversion:
		Label wlTransversion = new Label(wTransComp, SWT.RIGHT);
		wlTransversion.setText(BaseMessages.getString(PKG, "TransDialog.Transversion.Label")); //$NON-NLS-1$
		props.setLook(wlTransversion);
		FormData fdlTransversion = new FormData();
		fdlTransversion.left = new FormAttachment(0, 0);
		fdlTransversion.right= new FormAttachment(middle, -margin);
		fdlTransversion.top  = new FormAttachment(wTransstatus, margin);
		wlTransversion.setLayoutData(fdlTransversion);
		wTransversion=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wTransversion);
		wTransversion.addModifyListener(lsMod);
		FormData fdTransversion = new FormData();
		fdTransversion.left = new FormAttachment(middle, 0);
		fdTransversion.top  = new FormAttachment(wTransstatus, margin);
		fdTransversion.right= new FormAttachment(100, 0);
		wTransversion.setLayoutData(fdTransversion);

		// Directory:
		wlDirectory = new Label(wTransComp, SWT.RIGHT);
		wlDirectory.setText(BaseMessages.getString(PKG, "TransDialog.Directory.Label")); //$NON-NLS-1$
		props.setLook(wlDirectory);
		FormData fdlDirectory = new FormData();
		fdlDirectory.left = new FormAttachment(0, 0);
		fdlDirectory.right= new FormAttachment(middle, -margin);
		fdlDirectory.top  = new FormAttachment(wTransversion, margin);
		wlDirectory.setLayoutData(fdlDirectory);

		wbDirectory=new Button(wTransComp, SWT.PUSH);
		wbDirectory.setToolTipText(BaseMessages.getString(PKG, "TransDialog.selectTransFolder.Tooltip")); //$NON-NLS-1$
		wbDirectory.setImage(GUIResource.getInstance().getImageArrow());
		props.setLook(wbDirectory);
		FormData fdbDirectory = new FormData();
		fdbDirectory.right= new FormAttachment(100, 0);
		fdbDirectory.top  = new FormAttachment(wTransversion, 0);
		wbDirectory.setLayoutData(fdbDirectory);
		wbDirectory.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent arg0)
			{
				if (rep!=null)
				{
					RepositoryDirectory directoryFrom = transMeta.getRepositoryDirectory();
					if (directoryFrom==null) directoryFrom = new RepositoryDirectory();
					ObjectId idDirectoryFrom  = directoryFrom.getObjectId();
                    
                    SelectDirectoryDialog sdd = new SelectDirectoryDialog(shell, SWT.NONE, rep);
                    RepositoryDirectory rd = sdd.open();
                    if (rd!=null)
                    {
                        if (idDirectoryFrom!=rd.getObjectId())
                        {
                            // We need to change this in the repository as well!!
                            // We do this when the user pressed OK
                            newDirectory = rd;
                            wDirectory.setText(rd.getPath());
                        }
                        else
                        {
                            // Same directory!
                        }
                    }
                }
                else
                {
                    
                }
            }
        });

        wDirectory=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wDirectory);
        wDirectory.setEditable(false);
        wDirectory.setEnabled(false);
        FormData fdDirectory = new FormData();
        fdDirectory.left = new FormAttachment(middle, 0);
		fdDirectory.top  = new FormAttachment(wTransversion, margin);
        fdDirectory.right= new FormAttachment(wbDirectory, 0);
        wDirectory.setLayoutData(fdDirectory);

		// Create User:
		Label wlCreateUser = new Label(wTransComp, SWT.RIGHT);
		wlCreateUser.setText(BaseMessages.getString(PKG, "TransDialog.CreateUser.Label")); //$NON-NLS-1$
		props.setLook(wlCreateUser);
		FormData fdlCreateUser = new FormData();
		fdlCreateUser.left = new FormAttachment(0, 0);
		fdlCreateUser.right= new FormAttachment(middle, -margin);
		fdlCreateUser.top  = new FormAttachment(wDirectory, margin);
		wlCreateUser.setLayoutData(fdlCreateUser);
		wCreateUser=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wCreateUser);
		wCreateUser.setEditable(false);
		wCreateUser.addModifyListener(lsMod);
		FormData fdCreateUser = new FormData();
		fdCreateUser.left = new FormAttachment(middle, 0);
		fdCreateUser.top  = new FormAttachment(wDirectory, margin);
		fdCreateUser.right= new FormAttachment(100, 0);
		wCreateUser.setLayoutData(fdCreateUser);

		// Created Date:
		Label wlCreateDate = new Label(wTransComp, SWT.RIGHT);
		wlCreateDate.setText(BaseMessages.getString(PKG, "TransDialog.CreateDate.Label")); //$NON-NLS-1$
		props.setLook(wlCreateDate);
		FormData fdlCreateDate = new FormData();
		fdlCreateDate.left = new FormAttachment(0, 0);
		fdlCreateDate.right= new FormAttachment(middle, -margin);
		fdlCreateDate.top  = new FormAttachment(wCreateUser, margin);
		wlCreateDate.setLayoutData(fdlCreateDate);
		wCreateDate=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wCreateDate);
		wCreateDate.setEditable(false);
		wCreateDate.addModifyListener(lsMod);
		FormData fdCreateDate = new FormData();
		fdCreateDate.left = new FormAttachment(middle, 0);
		fdCreateDate.top  = new FormAttachment(wCreateUser, margin);
		fdCreateDate.right= new FormAttachment(100, 0);
		wCreateDate.setLayoutData(fdCreateDate);


        // Modified User:
        Label wlModUser = new Label(wTransComp, SWT.RIGHT);
        wlModUser.setText(BaseMessages.getString(PKG, "TransDialog.LastModifiedUser.Label")); //$NON-NLS-1$
        props.setLook(wlModUser);
        FormData fdlModUser = new FormData();
        fdlModUser.left = new FormAttachment(0, 0);
        fdlModUser.right= new FormAttachment(middle, -margin);
		fdlModUser.top  = new FormAttachment(wCreateDate, margin);
        wlModUser.setLayoutData(fdlModUser);
        wModUser=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wModUser);
        wModUser.setEditable(false);
        wModUser.addModifyListener(lsMod);
        FormData fdModUser = new FormData();
        fdModUser.left = new FormAttachment(middle, 0);
		fdModUser.top  = new FormAttachment(wCreateDate, margin);
        fdModUser.right= new FormAttachment(100, 0);
        wModUser.setLayoutData(fdModUser);

        // Modified Date:
        Label wlModDate = new Label(wTransComp, SWT.RIGHT);
        wlModDate.setText(BaseMessages.getString(PKG, "TransDialog.LastModifiedDate.Label")); //$NON-NLS-1$
        props.setLook(wlModDate);
        FormData fdlModDate = new FormData();
        fdlModDate.left = new FormAttachment(0, 0);
        fdlModDate.right= new FormAttachment(middle, -margin);
        fdlModDate.top  = new FormAttachment(wModUser, margin);
        wlModDate.setLayoutData(fdlModDate);
        wModDate=new Text(wTransComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wModDate);
        wModDate.setEditable(false);
        wModDate.addModifyListener(lsMod);
        FormData fdModDate = new FormData();
        fdModDate.left = new FormAttachment(middle, 0);
        fdModDate.top  = new FormAttachment(wModUser, margin);
        fdModDate.right= new FormAttachment(100, 0);
        wModDate.setLayoutData(fdModDate);

        FormData fdTransComp = new FormData();
        fdTransComp.left  = new FormAttachment(0, 0);
        fdTransComp.top   = new FormAttachment(0, 0);
        fdTransComp.right = new FormAttachment(100, 0);
        fdTransComp.bottom= new FormAttachment(100, 0);
        wTransComp.setLayoutData(fdTransComp);
    
        wTransComp.layout();
        wTransTab.setControl(wTransComp);
        
        /////////////////////////////////////////////////////////////
        /// END OF TRANS TAB
        /////////////////////////////////////////////////////////////
    }

    private void addParamTab()
    {
        //////////////////////////
        // START OF PARAM TAB
        ///
        wParamTab=new CTabItem(wTabFolder, SWT.NONE);
        wParamTab.setText(BaseMessages.getString(PKG, "TransDialog.ParamTab.Label")); //$NON-NLS-1$

        FormLayout paramLayout = new FormLayout ();
        paramLayout.marginWidth  = Const.MARGIN;
        paramLayout.marginHeight = Const.MARGIN;
        
        Composite wParamComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wParamComp);
        wParamComp.setLayout(paramLayout);

        Label wlFields = new Label(wParamComp, SWT.RIGHT);
        wlFields.setText(BaseMessages.getString(PKG, "TransDialog.Parameters.Label")); //$NON-NLS-1$
        props.setLook(wlFields);
        FormData fdlFields = new FormData();
        fdlFields.left = new FormAttachment(0, 0);
        fdlFields.top  = new FormAttachment(0, 0);
        wlFields.setLayoutData(fdlFields);
        
        final int FieldsCols=3;
        final int FieldsRows=100;  // TODO get the real number of parameters?
        
        ColumnInfo[] colinf=new ColumnInfo[FieldsCols];
        colinf[0]=new ColumnInfo(BaseMessages.getString(PKG, "TransDialog.ColumnInfo.Parameter.Label"),   ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
        colinf[1]=new ColumnInfo(BaseMessages.getString(PKG, "TransDialog.ColumnInfo.Default.Label"),     ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
        colinf[2]=new ColumnInfo(BaseMessages.getString(PKG, "TransDialog.ColumnInfo.Description.Label"), ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
    
        
        wParamFields=new TableView(transMeta, wParamComp, 
                              SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
                              colinf, 
                              FieldsRows,  
                              lsMod,
                              props
                              );
       
        FormData fdFields = new FormData();
        fdFields.left  = new FormAttachment(0, 0);
        fdFields.top   = new FormAttachment(wlFields, margin);
        fdFields.right = new FormAttachment(100, 0);
        fdFields.bottom= new FormAttachment(100, 0);
        wParamFields.setLayoutData(fdFields);

        FormData fdDepComp = new FormData();
        fdDepComp.left  = new FormAttachment(0, 0);
        fdDepComp.top   = new FormAttachment(0, 0);
        fdDepComp.right = new FormAttachment(100, 0);
        fdDepComp.bottom= new FormAttachment(100, 0);
        wParamComp.setLayoutData(fdDepComp);
        
        wParamComp.layout();
        wParamTab.setControl(wParamComp);

        /////////////////////////////////////////////////////////////
        /// END OF PARAM TAB
        /////////////////////////////////////////////////////////////
   }	
	
    private void addLogTab()
    {
        //////////////////////////
        // START OF LOG TAB///
        ///
        wLogTab=new CTabItem(wTabFolder, SWT.NONE);
        wLogTab.setText(BaseMessages.getString(PKG, "TransDialog.LogTab.Label")); //$NON-NLS-1$

        FormLayout LogLayout = new FormLayout ();
        LogLayout.marginWidth  = Const.MARGIN;
        LogLayout.marginHeight = Const.MARGIN;
        
        Composite wLogComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wLogComp);
        wLogComp.setLayout(LogLayout);


        // Log step: lines read...
        Label wlReadStep = new Label(wLogComp, SWT.RIGHT);
        wlReadStep.setText(BaseMessages.getString(PKG, "TransDialog.ReadStep.Label")); //$NON-NLS-1$
        props.setLook(wlReadStep);
        FormData fdlReadStep = new FormData();
        fdlReadStep.left = new FormAttachment(0, 0);
        fdlReadStep.right= new FormAttachment(middle, -margin);
        fdlReadStep.top  = new FormAttachment(0, 0);
        wlReadStep.setLayoutData(fdlReadStep);
        wReadStep=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wReadStep);
        wReadStep.addModifyListener(lsMod);
        FormData fdReadStep = new FormData();
        fdReadStep.left = new FormAttachment(middle, 0);
        fdReadStep.top  = new FormAttachment(0, 0);
        fdReadStep.right= new FormAttachment(100, 0);
        wReadStep.setLayoutData(fdReadStep);

        // Log step: lines input...
        Label wlInputStep = new Label(wLogComp, SWT.RIGHT);
        wlInputStep.setText(BaseMessages.getString(PKG, "TransDialog.InputStep.Label")); //$NON-NLS-1$
        props.setLook(wlInputStep);
        FormData fdlInputStep = new FormData();
        fdlInputStep.left = new FormAttachment(0, 0);
        fdlInputStep.right= new FormAttachment(middle, -margin);
        fdlInputStep.top  = new FormAttachment(wReadStep, margin*2);
        wlInputStep.setLayoutData(fdlInputStep);
        wInputStep=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wInputStep);
        wInputStep.addModifyListener(lsMod);
        FormData fdInputStep = new FormData();
        fdInputStep.left = new FormAttachment(middle, 0);
        fdInputStep.top  = new FormAttachment(wReadStep, margin*2);
        fdInputStep.right= new FormAttachment(100, 0);
        wInputStep.setLayoutData(fdInputStep);

        // Log step: lines written...
        Label wlWriteStep = new Label(wLogComp, SWT.RIGHT);
        wlWriteStep.setText(BaseMessages.getString(PKG, "TransDialog.WriteStep.Label")); //$NON-NLS-1$
        props.setLook(wlWriteStep);
        FormData fdlWriteStep = new FormData();
        fdlWriteStep.left = new FormAttachment(0, 0);
        fdlWriteStep.right= new FormAttachment(middle, -margin);
        fdlWriteStep.top  = new FormAttachment(wInputStep, margin*2);
        wlWriteStep.setLayoutData(fdlWriteStep);
        wWriteStep=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wWriteStep);
        wWriteStep.addModifyListener(lsMod);
        FormData fdWriteStep = new FormData();
        fdWriteStep.left = new FormAttachment(middle, 0);
        fdWriteStep.top  = new FormAttachment(wInputStep, margin*2);
        fdWriteStep.right= new FormAttachment(100, 0);
        wWriteStep.setLayoutData(fdWriteStep);

        // Log step: lines to output...
        Label wlOutputStep = new Label(wLogComp, SWT.RIGHT);
        wlOutputStep.setText(BaseMessages.getString(PKG, "TransDialog.OutputStep.Label")); //$NON-NLS-1$
        props.setLook(wlOutputStep);
        FormData fdlOutputStep = new FormData();
        fdlOutputStep.left = new FormAttachment(0, 0);
        fdlOutputStep.right= new FormAttachment(middle, -margin);
        fdlOutputStep.top  = new FormAttachment(wWriteStep, margin*2);
        wlOutputStep.setLayoutData(fdlOutputStep);
        wOutputStep=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wOutputStep);
        wOutputStep.addModifyListener(lsMod);
        FormData fdOutputStep = new FormData();
        fdOutputStep.left = new FormAttachment(middle, 0);
        fdOutputStep.top  = new FormAttachment(wWriteStep, margin*2);
        fdOutputStep.right= new FormAttachment(100, 0);
        wOutputStep.setLayoutData(fdOutputStep);

        // Log step: update...
        Label wlUpdateStep = new Label(wLogComp, SWT.RIGHT);
        wlUpdateStep.setText(BaseMessages.getString(PKG, "TransDialog.UpdateStep.Label")); //$NON-NLS-1$
        props.setLook(wlUpdateStep);
        FormData fdlUpdateStep = new FormData();
        fdlUpdateStep.left = new FormAttachment(0, 0);
        fdlUpdateStep.right= new FormAttachment(middle, -margin);
        fdlUpdateStep.top  = new FormAttachment(wOutputStep, margin*2);
        wlUpdateStep.setLayoutData(fdlUpdateStep);
        wUpdateStep=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wUpdateStep);
        wUpdateStep.addModifyListener(lsMod);
        FormData fdUpdateStep = new FormData();
        fdUpdateStep.left = new FormAttachment(middle, 0);
        fdUpdateStep.top  = new FormAttachment(wOutputStep, margin*2);
        fdUpdateStep.right= new FormAttachment(100, 0);
        wUpdateStep.setLayoutData(fdUpdateStep);

        // Log step: update...
        Label wlRejectedStep = new Label(wLogComp, SWT.RIGHT);
        wlRejectedStep.setText(BaseMessages.getString(PKG, "TransDialog.RejectedStep.Label")); //$NON-NLS-1$
        props.setLook(wlRejectedStep);
        FormData fdlRejectedStep = new FormData();
        fdlRejectedStep.left = new FormAttachment(0, 0);
        fdlRejectedStep.right= new FormAttachment(middle, -margin);
        fdlRejectedStep.top  = new FormAttachment(wUpdateStep, margin*2);
        wlRejectedStep.setLayoutData(fdlRejectedStep);
        wRejectedStep=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wRejectedStep);
        wRejectedStep.addModifyListener(lsMod);
        FormData fdRejectedStep = new FormData();
        fdRejectedStep.left = new FormAttachment(middle, 0);
        fdRejectedStep.top  = new FormAttachment(wUpdateStep, margin*2);
        fdRejectedStep.right= new FormAttachment(100, 0);
        wRejectedStep.setLayoutData(fdRejectedStep);

        for (int i=0;i<transMeta.nrSteps();i++)
        {
            StepMeta stepMeta = transMeta.getStep(i);
            wReadStep.add(stepMeta.getName());
            wWriteStep.add(stepMeta.getName());
            wInputStep.add(stepMeta.getName());
            wOutputStep.add(stepMeta.getName());
            wUpdateStep.add(stepMeta.getName());
            wRejectedStep.add(stepMeta.getName());
        }

        // Log table connection...
        Label wlLogconnection = new Label(wLogComp, SWT.RIGHT);
        wlLogconnection.setText(BaseMessages.getString(PKG, "TransDialog.LogConnection.Label")); //$NON-NLS-1$
        props.setLook(wlLogconnection);
        FormData fdlLogconnection = new FormData();
        fdlLogconnection.left = new FormAttachment(0, 0);
        fdlLogconnection.right= new FormAttachment(middle, -margin);
        fdlLogconnection.top  = new FormAttachment(wRejectedStep, margin*4);
        wlLogconnection.setLayoutData(fdlLogconnection);

        wbLogconnection=new Button(wLogComp, SWT.PUSH);
        wbLogconnection.setText(BaseMessages.getString(PKG, "TransDialog.LogconnectionButton.Label")); //$NON-NLS-1$
        wbLogconnection.addSelectionListener(new SelectionAdapter() 
        {
            public void widgetSelected(SelectionEvent e) 
            {
                DatabaseMeta databaseMeta = new DatabaseMeta();
                databaseMeta.shareVariablesWith(transMeta);
                DatabaseDialog cid = new DatabaseDialog(shell, databaseMeta);
                if (cid.open()!=null)
                {
                    transMeta.addDatabase(databaseMeta);
                    wLogconnection.add(databaseMeta.getName());
                    wLogconnection.select(wLogconnection.getItemCount()-1);
                }
            }
        });
        FormData fdbLogconnection = new FormData();
        fdbLogconnection.right= new FormAttachment(100, 0);
        fdbLogconnection.top  = new FormAttachment(wRejectedStep, margin*4);
        wbLogconnection.setLayoutData(fdbLogconnection);

        wLogconnection=new CCombo(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wLogconnection);
        wLogconnection.addModifyListener(lsMod);
        FormData fdLogconnection = new FormData();
        fdLogconnection.left = new FormAttachment(middle, 0);
        fdLogconnection.top  = new FormAttachment(wRejectedStep, margin*4);
        fdLogconnection.right= new FormAttachment(wbLogconnection, -margin);
        wLogconnection.setLayoutData(fdLogconnection);


        // Log table...:
        Label wlLogtable = new Label(wLogComp, SWT.RIGHT);
        wlLogtable.setText(BaseMessages.getString(PKG, "TransDialog.Logtable.Label")); //$NON-NLS-1$
        props.setLook(wlLogtable);
        FormData fdlLogtable = new FormData();
        fdlLogtable.left = new FormAttachment(0, 0);
        fdlLogtable.right= new FormAttachment(middle, -margin);
        fdlLogtable.top  = new FormAttachment(wLogconnection, margin);
        wlLogtable.setLayoutData(fdlLogtable);
        wLogtable=new Text(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wLogtable);
        wLogtable.addModifyListener(lsMod);
        FormData fdLogtable = new FormData();
        fdLogtable.left = new FormAttachment(middle, 0);
        fdLogtable.top  = new FormAttachment(wLogconnection, margin);
        fdLogtable.right= new FormAttachment(100, 0);
        wLogtable.setLayoutData(fdLogtable);
        
        // step Log table...:
        //
        wlStepLogtable = new Label(wLogComp, SWT.RIGHT);
        wlStepLogtable.setText(BaseMessages.getString(PKG, "TransDialog.StepLogtable.Label")); //$NON-NLS-1$
        props.setLook(wlStepLogtable);
        FormData fdlStepLogtable = new FormData();
        fdlStepLogtable.left = new FormAttachment(0, 0);
        fdlStepLogtable.right= new FormAttachment(middle, -margin);
        fdlStepLogtable.top  = new FormAttachment(wLogtable, margin);
        wlStepLogtable.setLayoutData(fdlStepLogtable);
        wStepLogtable=new Text(wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wStepLogtable);
        wStepLogtable.addModifyListener(lsMod);
        FormData fdStepLogtable = new FormData();
        fdStepLogtable.left = new FormAttachment(middle, 0);
        fdStepLogtable.top  = new FormAttachment(wLogtable, margin);
        fdStepLogtable.right= new FormAttachment(100, 0);
        wStepLogtable.setLayoutData(fdStepLogtable);


        Label wlBatch = new Label(wLogComp, SWT.RIGHT);
        wlBatch.setText(BaseMessages.getString(PKG, "TransDialog.LogBatch.Label")); //$NON-NLS-1$
        props.setLook(wlBatch);
        FormData fdlBatch = new FormData();
        fdlBatch.left = new FormAttachment(0, 0);
        fdlBatch.top  = new FormAttachment(wStepLogtable, margin);
        fdlBatch.right= new FormAttachment(middle, -margin);
        wlBatch.setLayoutData(fdlBatch);
        wBatch=new Button(wLogComp, SWT.CHECK);
        props.setLook(wBatch);
        FormData fdBatch = new FormData();
        fdBatch.left = new FormAttachment(middle, 0);
        fdBatch.top  = new FormAttachment(wStepLogtable, margin);
        fdBatch.right= new FormAttachment(100, 0);
        wBatch.setLayoutData(fdBatch);

        Label wlLogfield = new Label(wLogComp, SWT.RIGHT);
        wlLogfield.setText(BaseMessages.getString(PKG, "TransDialog.Logfield.Label")); //$NON-NLS-1$
        props.setLook(wlLogfield);
        FormData fdlLogfield = new FormData();
        fdlLogfield.left = new FormAttachment(0, 0);
        fdlLogfield.top  = new FormAttachment(wBatch, margin);
        fdlLogfield.right= new FormAttachment(middle, -margin);
        wlLogfield.setLayoutData(fdlLogfield);
        wLogfield=new Button(wLogComp, SWT.CHECK);
        props.setLook(wLogfield);
        FormData fdLogfield = new FormData();
        fdLogfield.left = new FormAttachment(middle, 0);
        fdLogfield.top  = new FormAttachment(wBatch, margin);
        fdLogfield.right= new FormAttachment(100, 0);
        wLogfield.setLayoutData(fdLogfield);
        wLogfield.addSelectionListener(new SelectionAdapter() { public void widgetSelected(SelectionEvent e) { setFlags(); } });

        // The log size limit
        //
        wlLogSizeLimit = new Label(wLogComp, SWT.RIGHT);
        wlLogSizeLimit.setText(BaseMessages.getString(PKG, "TransDialog.LogSizeLimit.Label")); //$NON-NLS-1$
        wlLogSizeLimit.setToolTipText(BaseMessages.getString(PKG, "TransDialog.LogSizeLimit.Tooltip")); //$NON-NLS-1$
        props.setLook(wlLogSizeLimit);
        FormData fdlLogSizeLimit = new FormData();
        fdlLogSizeLimit.left = new FormAttachment(0, 0);
        fdlLogSizeLimit.right= new FormAttachment(middle, -margin);
        fdlLogSizeLimit.top  = new FormAttachment(wLogfield, margin);
        wlLogSizeLimit.setLayoutData(fdlLogSizeLimit);
        wLogSizeLimit=new TextVar(transMeta, wLogComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wLogSizeLimit.setToolTipText(BaseMessages.getString(PKG, "TransDialog.LogSizeLimit.Tooltip")); //$NON-NLS-1$
        props.setLook(wLogSizeLimit);
        wLogSizeLimit.addModifyListener(lsMod);
        FormData fdLogSizeLimit = new FormData();
        fdLogSizeLimit.left = new FormAttachment(middle, 0);
        fdLogSizeLimit.top  = new FormAttachment(wLogfield, margin);
        fdLogSizeLimit.right= new FormAttachment(100, 0);
        wLogSizeLimit.setLayoutData(fdLogSizeLimit);

        FormData fdLogComp = new FormData();
        fdLogComp.left  = new FormAttachment(0, 0);
        fdLogComp.top   = new FormAttachment(0, 0);
        fdLogComp.right = new FormAttachment(100, 0);
        fdLogComp.bottom= new FormAttachment(100, 0);
        wLogComp.setLayoutData(fdLogComp);
    
        wLogComp.layout();
        wLogTab.setControl(wLogComp);
        
        /////////////////////////////////////////////////////////////
        /// END OF LOG TAB
        /////////////////////////////////////////////////////////////
    }


    private void addDateTab()
    {
        //////////////////////////
        // START OF DATE TAB///
        ///
        wDateTab=new CTabItem(wTabFolder, SWT.NONE);
        wDateTab.setText(BaseMessages.getString(PKG, "TransDialog.DateTab.Label")); //$NON-NLS-1$

        FormLayout DateLayout = new FormLayout ();
        DateLayout.marginWidth  = Const.MARGIN;
        DateLayout.marginHeight = Const.MARGIN;
        
        Composite wDateComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wDateComp);
        wDateComp.setLayout(DateLayout);

        // Max date table connection...
        Label wlMaxdateconnection = new Label(wDateComp, SWT.RIGHT);
        wlMaxdateconnection.setText(BaseMessages.getString(PKG, "TransDialog.MaxdateConnection.Label")); //$NON-NLS-1$
        props.setLook(wlMaxdateconnection);
        FormData fdlMaxdateconnection = new FormData();
        fdlMaxdateconnection.left = new FormAttachment(0, 0);
        fdlMaxdateconnection.right= new FormAttachment(middle, -margin);
        fdlMaxdateconnection.top  = new FormAttachment(0, 0);
        wlMaxdateconnection.setLayoutData(fdlMaxdateconnection);
        wMaxdateconnection=new CCombo(wDateComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wMaxdateconnection);
        wMaxdateconnection.addModifyListener(lsMod);
        FormData fdMaxdateconnection = new FormData();
        fdMaxdateconnection.left = new FormAttachment(middle, 0);
        fdMaxdateconnection.top  = new FormAttachment(0, 0);
        fdMaxdateconnection.right= new FormAttachment(100, 0);
        wMaxdateconnection.setLayoutData(fdMaxdateconnection);

        // Maxdate table...:
        Label wlMaxdatetable = new Label(wDateComp, SWT.RIGHT);
        wlMaxdatetable.setText(BaseMessages.getString(PKG, "TransDialog.MaxdateTable.Label")); //$NON-NLS-1$
        props.setLook(wlMaxdatetable);
        FormData fdlMaxdatetable = new FormData();
        fdlMaxdatetable.left = new FormAttachment(0, 0);
        fdlMaxdatetable.right= new FormAttachment(middle, -margin);
        fdlMaxdatetable.top  = new FormAttachment(wMaxdateconnection, margin);
        wlMaxdatetable.setLayoutData(fdlMaxdatetable);
        wMaxdatetable=new Text(wDateComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wMaxdatetable);
        wMaxdatetable.addModifyListener(lsMod);
        FormData fdMaxdatetable = new FormData();
        fdMaxdatetable.left = new FormAttachment(middle, 0);
        fdMaxdatetable.top  = new FormAttachment(wMaxdateconnection, margin);
        fdMaxdatetable.right= new FormAttachment(100, 0);
        wMaxdatetable.setLayoutData(fdMaxdatetable);

        // Maxdate field...:
        Label wlMaxdatefield = new Label(wDateComp, SWT.RIGHT);
        wlMaxdatefield.setText(BaseMessages.getString(PKG, "TransDialog.MaxdateField.Label")); //$NON-NLS-1$
        props.setLook(wlMaxdatefield);
        FormData fdlMaxdatefield = new FormData();
        fdlMaxdatefield.left = new FormAttachment(0, 0);
        fdlMaxdatefield.right= new FormAttachment(middle, -margin);
        fdlMaxdatefield.top  = new FormAttachment(wMaxdatetable, margin);
        wlMaxdatefield.setLayoutData(fdlMaxdatefield);
        wMaxdatefield=new Text(wDateComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wMaxdatefield);
        wMaxdatefield.addModifyListener(lsMod);
        FormData fdMaxdatefield = new FormData();
        fdMaxdatefield.left = new FormAttachment(middle, 0);
        fdMaxdatefield.top  = new FormAttachment(wMaxdatetable, margin);
        fdMaxdatefield.right= new FormAttachment(100, 0);
        wMaxdatefield.setLayoutData(fdMaxdatefield);

        // Maxdate offset...:
        Label wlMaxdateoffset = new Label(wDateComp, SWT.RIGHT);
        wlMaxdateoffset.setText(BaseMessages.getString(PKG, "TransDialog.MaxdateOffset.Label")); //$NON-NLS-1$
        props.setLook(wlMaxdateoffset);
        FormData fdlMaxdateoffset = new FormData();
        fdlMaxdateoffset.left = new FormAttachment(0, 0);
        fdlMaxdateoffset.right= new FormAttachment(middle, -margin);
        fdlMaxdateoffset.top  = new FormAttachment(wMaxdatefield, margin);
        wlMaxdateoffset.setLayoutData(fdlMaxdateoffset);
        wMaxdateoffset=new Text(wDateComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wMaxdateoffset);
        wMaxdateoffset.addModifyListener(lsMod);
        FormData fdMaxdateoffset = new FormData();
        fdMaxdateoffset.left = new FormAttachment(middle, 0);
        fdMaxdateoffset.top  = new FormAttachment(wMaxdatefield, margin);
        fdMaxdateoffset.right= new FormAttachment(100, 0);
        wMaxdateoffset.setLayoutData(fdMaxdateoffset);

        // Maxdate diff...:
        Label wlMaxdatediff = new Label(wDateComp, SWT.RIGHT);
        wlMaxdatediff.setText(BaseMessages.getString(PKG, "TransDialog.Maxdatediff.Label")); //$NON-NLS-1$
        props.setLook(wlMaxdatediff);
        FormData fdlMaxdatediff = new FormData();
        fdlMaxdatediff.left = new FormAttachment(0, 0);
        fdlMaxdatediff.right= new FormAttachment(middle, -margin);
        fdlMaxdatediff.top  = new FormAttachment(wMaxdateoffset, margin);
        wlMaxdatediff.setLayoutData(fdlMaxdatediff);
        wMaxdatediff=new Text(wDateComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wMaxdatediff);
        wMaxdatediff.addModifyListener(lsMod);
        FormData fdMaxdatediff = new FormData();
        fdMaxdatediff.left = new FormAttachment(middle, 0);
        fdMaxdatediff.top  = new FormAttachment(wMaxdateoffset, margin);
        fdMaxdatediff.right= new FormAttachment(100, 0);
        wMaxdatediff.setLayoutData(fdMaxdatediff);


        connectionNames = new String[transMeta.nrDatabases()]; 
        for (int i=0;i<transMeta.nrDatabases();i++)
        {
            DatabaseMeta ci = transMeta.getDatabase(i);
            wLogconnection.add(ci.getName());
            wMaxdateconnection.add(ci.getName());
            connectionNames[i] = ci.getName();
        }
        
        FormData fdDateComp = new FormData();
        fdDateComp.left  = new FormAttachment(0, 0);
        fdDateComp.top   = new FormAttachment(0, 0);
        fdDateComp.right = new FormAttachment(100, 0);
        fdDateComp.bottom= new FormAttachment(100, 0);
        wDateComp.setLayoutData(fdDateComp);
    
        wDateComp.layout();
        wDateTab.setControl(wDateComp);
        
        /////////////////////////////////////////////////////////////
        /// END OF DATE TAB
        /////////////////////////////////////////////////////////////
    }


    private void addDepTab()
    {
        //////////////////////////
        // START OF Dep TAB///
        ///
        wDepTab=new CTabItem(wTabFolder, SWT.NONE);
        wDepTab.setText(BaseMessages.getString(PKG, "TransDialog.DepTab.Label")); //$NON-NLS-1$

        FormLayout DepLayout = new FormLayout ();
        DepLayout.marginWidth  = Const.MARGIN;
        DepLayout.marginHeight = Const.MARGIN;
        
        Composite wDepComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wDepComp);
        wDepComp.setLayout(DepLayout);

        Label wlFields = new Label(wDepComp, SWT.RIGHT);
        wlFields.setText(BaseMessages.getString(PKG, "TransDialog.Fields.Label")); //$NON-NLS-1$
        props.setLook(wlFields);
        FormData fdlFields = new FormData();
        fdlFields.left = new FormAttachment(0, 0);
        fdlFields.top  = new FormAttachment(0, 0);
        wlFields.setLayoutData(fdlFields);
        
        final int FieldsCols=3;
        final int FieldsRows=transMeta.nrDependencies();
        
        ColumnInfo[] colinf=new ColumnInfo[FieldsCols];
        colinf[0]=new ColumnInfo(BaseMessages.getString(PKG, "TransDialog.ColumnInfo.Connection.Label"), ColumnInfo.COLUMN_TYPE_CCOMBO, connectionNames); //$NON-NLS-1$
        colinf[1]=new ColumnInfo(BaseMessages.getString(PKG, "TransDialog.ColumnInfo.Table.Label"),      ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
        colinf[2]=new ColumnInfo(BaseMessages.getString(PKG, "TransDialog.ColumnInfo.Field.Label"),      ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
        
        wFields=new TableView(transMeta, wDepComp, 
                              SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
                              colinf, 
                              FieldsRows,  
                              lsMod,
                              props
                              );

        wGet=new Button(wDepComp, SWT.PUSH);
        wGet.setText(BaseMessages.getString(PKG, "TransDialog.GetDependenciesButton.Label")); //$NON-NLS-1$

        fdGet = new FormData();
        fdGet.bottom = new FormAttachment(100, 0);
        fdGet.left   = new FormAttachment(50, 0);
        wGet.setLayoutData(fdGet);
        
        FormData fdFields = new FormData();
        fdFields.left  = new FormAttachment(0, 0);
        fdFields.top   = new FormAttachment(wlFields, margin);
        fdFields.right = new FormAttachment(100, 0);
        fdFields.bottom= new FormAttachment(wGet, 0);
        wFields.setLayoutData(fdFields);

        FormData fdDepComp = new FormData();
        fdDepComp.left  = new FormAttachment(0, 0);
        fdDepComp.top   = new FormAttachment(0, 0);
        fdDepComp.right = new FormAttachment(100, 0);
        fdDepComp.bottom= new FormAttachment(100, 0);
        wDepComp.setLayoutData(fdDepComp);
        
        wDepComp.layout();
        wDepTab.setControl(wDepComp);

        /////////////////////////////////////////////////////////////
        /// END OF DEP TAB
        /////////////////////////////////////////////////////////////
   }


    private void addMiscTab()
    {
        //////////////////////////
        // START OF PERFORMANCE TAB///
        ///
        wMiscTab=new CTabItem(wTabFolder, SWT.NONE);
        wMiscTab.setText(BaseMessages.getString(PKG, "TransDialog.MiscTab.Label")); //$NON-NLS-1$
        
        Composite wMiscComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wMiscComp);

        FormLayout perfLayout = new FormLayout();
        perfLayout.marginWidth  = Const.FORM_MARGIN;
        perfLayout.marginHeight = Const.FORM_MARGIN;
        wMiscComp.setLayout(perfLayout);


        // Rows in Rowset:
        Label wlSizeRowset = new Label(wMiscComp, SWT.RIGHT);
        wlSizeRowset.setText(BaseMessages.getString(PKG, "TransDialog.SizeRowset.Label")); //$NON-NLS-1$
        props.setLook(wlSizeRowset);
        FormData fdlSizeRowset = new FormData();
        fdlSizeRowset.left = new FormAttachment(0, 0);
        fdlSizeRowset.right= new FormAttachment(middle, -margin);
        fdlSizeRowset.top  = new FormAttachment(0, margin);
        wlSizeRowset.setLayoutData(fdlSizeRowset);
        wSizeRowset=new Text(wMiscComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wSizeRowset);
        wSizeRowset.addModifyListener(lsMod);
        FormData fdSizeRowset = new FormData();
        fdSizeRowset.left = new FormAttachment(middle, 0);
        fdSizeRowset.top  = new FormAttachment(0, margin);
        fdSizeRowset.right= new FormAttachment(100, 0);
        wSizeRowset.setLayoutData(fdSizeRowset);

        // Show feedback in transformations steps?
        Label wlShowFeedback = new Label(wMiscComp, SWT.RIGHT);
        wlShowFeedback.setText(BaseMessages.getString(PKG, "TransDialog.ShowFeedbackRow.Label"));
        props.setLook(wlShowFeedback);
        FormData fdlShowFeedback = new FormData();
        fdlShowFeedback.left = new FormAttachment(0, 0);
        fdlShowFeedback.top  = new FormAttachment(wSizeRowset, margin);
        fdlShowFeedback.right= new FormAttachment(middle, -margin);
        wlShowFeedback.setLayoutData(fdlShowFeedback);
        wShowFeedback=new Button(wMiscComp, SWT.CHECK);
        props.setLook(wShowFeedback);
        FormData fdShowFeedback = new FormData();
        fdShowFeedback.left = new FormAttachment(middle, 0);
        fdShowFeedback.top  = new FormAttachment(wSizeRowset, margin);
        fdShowFeedback.right= new FormAttachment(100, 0);
        wShowFeedback.setLayoutData(fdShowFeedback);

        // Feedback size
        Label wlFeedbackSize = new Label(wMiscComp, SWT.RIGHT);
        wlFeedbackSize.setText(BaseMessages.getString(PKG, "TransDialog.FeedbackSize.Label"));
        props.setLook(wlFeedbackSize);
        FormData fdlFeedbackSize = new FormData();
        fdlFeedbackSize.left = new FormAttachment(0, 0);
        fdlFeedbackSize.right= new FormAttachment(middle, -margin);
        fdlFeedbackSize.top  = new FormAttachment(wShowFeedback, margin);
        wlFeedbackSize.setLayoutData(fdlFeedbackSize);
        wFeedbackSize=new Text(wMiscComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wFeedbackSize);
        FormData fdFeedbackSize = new FormData();
        fdFeedbackSize.left = new FormAttachment(middle, 0);
        fdFeedbackSize.right= new FormAttachment(100, -margin);
        fdFeedbackSize.top  = new FormAttachment(wShowFeedback, margin);
        wFeedbackSize.setLayoutData(fdFeedbackSize);
        
        // Unique connections
        Label wlUniqueConnections = new Label(wMiscComp, SWT.RIGHT);
        wlUniqueConnections.setText(BaseMessages.getString(PKG, "TransDialog.UniqueConnections.Label")); //$NON-NLS-1$
        props.setLook(wlUniqueConnections);
        FormData fdlUniqueConnections = new FormData();
        fdlUniqueConnections.left = new FormAttachment(0, 0);
        fdlUniqueConnections.right= new FormAttachment(middle, -margin);
        fdlUniqueConnections.top  = new FormAttachment(wFeedbackSize, margin);
        wlUniqueConnections.setLayoutData(fdlUniqueConnections);
        wUniqueConnections=new Button(wMiscComp, SWT.CHECK);
        props.setLook(wUniqueConnections);
        FormData fdUniqueConnections = new FormData();
        fdUniqueConnections.left = new FormAttachment(middle, 0);
        fdUniqueConnections.top  = new FormAttachment(wFeedbackSize, margin);
        fdUniqueConnections.right= new FormAttachment(100, 0);
        wUniqueConnections.setLayoutData(fdUniqueConnections);

        // Shared objects file
        Label wlSharedObjectsFile = new Label(wMiscComp, SWT.RIGHT);
        wlSharedObjectsFile.setText(BaseMessages.getString(PKG, "TransDialog.SharedObjectsFile.Label")); //$NON-NLS-1$
        props.setLook(wlSharedObjectsFile);
        FormData fdlSharedObjectsFile = new FormData();
        fdlSharedObjectsFile.left = new FormAttachment(0, 0);
        fdlSharedObjectsFile.right= new FormAttachment(middle, -margin);
        fdlSharedObjectsFile.top  = new FormAttachment(wUniqueConnections, margin);
        wlSharedObjectsFile.setLayoutData(fdlSharedObjectsFile);
        wSharedObjectsFile=new TextVar(transMeta, wMiscComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wlSharedObjectsFile.setToolTipText(BaseMessages.getString(PKG, "TransDialog.SharedObjectsFile.Tooltip")); //$NON-NLS-1$
        wSharedObjectsFile.setToolTipText(BaseMessages.getString(PKG, "TransDialog.SharedObjectsFile.Tooltip")); //$NON-NLS-1$
        props.setLook(wSharedObjectsFile);
        FormData fdSharedObjectsFile = new FormData();
        fdSharedObjectsFile.left = new FormAttachment(middle, 0);
        fdSharedObjectsFile.top  = new FormAttachment(wUniqueConnections, margin);
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

        // Show feedback in transformations steps?
        Label wlManageThreads = new Label(wMiscComp, SWT.RIGHT);
        wlManageThreads.setText(BaseMessages.getString(PKG, "TransDialog.ManageThreadPriorities.Label"));
        props.setLook(wlManageThreads);
        FormData fdlManageThreads = new FormData();
        fdlManageThreads.left = new FormAttachment(0, 0);
        fdlManageThreads.top  = new FormAttachment(wSharedObjectsFile, margin);
        fdlManageThreads.right= new FormAttachment(middle, -margin);
        wlManageThreads.setLayoutData(fdlManageThreads);
        wManageThreads=new Button(wMiscComp, SWT.CHECK);
        props.setLook(wManageThreads);
        FormData fdManageThreads = new FormData();
        fdManageThreads.left = new FormAttachment(middle, 0);
        fdManageThreads.top  = new FormAttachment(wSharedObjectsFile, margin);
        fdManageThreads.right= new FormAttachment(100, 0);
        wManageThreads.setLayoutData(fdManageThreads);

        

        FormData fdMiscComp = new FormData();
        fdMiscComp.left  = new FormAttachment(0, 0);
        fdMiscComp.top   = new FormAttachment(0, 0);
        fdMiscComp.right = new FormAttachment(100, 0);
        fdMiscComp.bottom= new FormAttachment(100, 0);
        wMiscComp.setLayoutData(fdMiscComp);
    
        wMiscComp.layout();
        wMiscTab.setControl(wMiscComp);
        
        /////////////////////////////////////////////////////////////
        /// END OF PERF TAB
        /////////////////////////////////////////////////////////////

    }


    private void addMonitoringTab()
    {
        //////////////////////////
        // START OF MONITORING TAB///
        ///
        wMonitorTab=new CTabItem(wTabFolder, SWT.NONE);
        wMonitorTab.setText(BaseMessages.getString(PKG, "TransDialog.MonitorTab.Label")); //$NON-NLS-1$
        
        Composite wMonitorComp = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wMonitorComp);

        FormLayout monitorLayout = new FormLayout();
        monitorLayout.marginWidth  = Const.FORM_MARGIN;
        monitorLayout.marginHeight = Const.FORM_MARGIN;
        wMonitorComp.setLayout(monitorLayout);

        // 
        // Enable step performance monitoring?
        //
        Label wlEnableStepPerfMonitor = new Label(wMonitorComp, SWT.LEFT);
        wlEnableStepPerfMonitor.setText(BaseMessages.getString(PKG, "TransDialog.StepPerformanceMonitoring.Label")); //$NON-NLS-1$
        props.setLook(wlEnableStepPerfMonitor);
        FormData fdlSchemaName=new FormData();
        fdlSchemaName.left = new FormAttachment(0, 0);
        fdlSchemaName.right = new FormAttachment(middle, -margin);
        fdlSchemaName.top  = new FormAttachment(0, 0);
        wlEnableStepPerfMonitor.setLayoutData(fdlSchemaName);
        wEnableStepPerfMonitor=new Button(wMonitorComp, SWT.CHECK);
        props.setLook(wEnableStepPerfMonitor);
        FormData fdEnableStepPerfMonitor=new FormData();
        fdEnableStepPerfMonitor.left   = new FormAttachment(middle, 0);
        fdEnableStepPerfMonitor.right  = new FormAttachment(100, 0);
        fdEnableStepPerfMonitor.top    = new FormAttachment(0, 0);
        wEnableStepPerfMonitor.setLayoutData(fdEnableStepPerfMonitor);
        wEnableStepPerfMonitor.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				setFlags();
			}
		});

        // 
        // Step performance interval
        //
        Label wlEnableStepPerfInterval = new Label(wMonitorComp, SWT.LEFT);
        wlEnableStepPerfInterval.setText(BaseMessages.getString(PKG, "TransDialog.StepPerformanceInterval.Label")); //$NON-NLS-1$
        props.setLook(wlEnableStepPerfInterval);
        FormData fdlEnableStepPerfInterval=new FormData();
        fdlEnableStepPerfInterval.left  = new FormAttachment(0, 0);
        fdlEnableStepPerfInterval.right = new FormAttachment(middle, -margin);
        fdlEnableStepPerfInterval.top = new FormAttachment(wEnableStepPerfMonitor, margin);
        wlEnableStepPerfInterval.setLayoutData(fdlEnableStepPerfInterval);
        wEnableStepPerfInterval=new Text(wMonitorComp, SWT.LEFT | SWT.BORDER | SWT.SINGLE);
        props.setLook(wEnableStepPerfInterval);
        FormData fdEnableStepPerfInterval=new FormData();
        fdEnableStepPerfInterval.left   = new FormAttachment(middle, 0);
        fdEnableStepPerfInterval.right  = new FormAttachment(100, 0);
        fdEnableStepPerfInterval.top    = new FormAttachment(wEnableStepPerfMonitor, margin);
        wEnableStepPerfInterval.setLayoutData(fdEnableStepPerfInterval);


        // TransDialog.SchemaMonitoritions.Label

        FormData fdMonitorComp = new FormData();
        fdMonitorComp.left  = new FormAttachment(0, 0);
        fdMonitorComp.top   = new FormAttachment(0, 0);
        fdMonitorComp.right = new FormAttachment(100, 0);
        fdMonitorComp.bottom= new FormAttachment(100, 0);
        wMonitorComp.setLayoutData(fdMonitorComp);
    
        wMonitorComp.layout();
        wMonitorTab.setControl(wMonitorComp);
        
        /////////////////////////////////////////////////////////////
        /// END OF MONITORING TAB
        /////////////////////////////////////////////////////////////

    }


    public void dispose()
	{
		shell.dispose();
	}
	
	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{
		wTransname.setText( Const.NVL(transMeta.getName(), "") );
		wTransFilename.setText(Const.NVL(transMeta.getFilename(), "") );
		wTransdescription.setText( Const.NVL(transMeta.getDescription(), "") );
		wExtendeddescription.setText( Const.NVL(transMeta.getExtendedDescription(), "") );
		wTransversion.setText( Const.NVL(transMeta.getTransversion(), "") );
		wTransstatus.select( transMeta.getTransstatus()-1 );
				

		if (transMeta.getCreatedUser()!=null)     wCreateUser.setText          ( transMeta.getCreatedUser() );
		if (transMeta.getCreatedDate()!=null )    wCreateDate.setText          ( transMeta.getCreatedDate().toString() );
		
		if (transMeta.getModifiedUser()!=null) wModUser.setText          ( transMeta.getModifiedUser() );
		if (transMeta.getModifiedDate()!=null) wModDate.setText          ( transMeta.getModifiedDate().toString() );
	
		if (transMeta.getReadStep()!=null)      wReadStep.setText         ( transMeta.getReadStep().getName() );
		if (transMeta.getWriteStep()!=null)     wWriteStep.setText        ( transMeta.getWriteStep().getName() );
		if (transMeta.getInputStep()!=null)     wInputStep.setText        ( transMeta.getInputStep().getName() );
		if (transMeta.getOutputStep()!=null)    wOutputStep.setText       ( transMeta.getOutputStep().getName() );
		if (transMeta.getUpdateStep()!=null)    wUpdateStep.setText       ( transMeta.getUpdateStep().getName() );
        if (transMeta.getRejectedStep()!=null)  wRejectedStep.setText     ( transMeta.getRejectedStep().getName() );

        if (transMeta.getLogConnection()!=null) wLogconnection.setText    ( transMeta.getLogConnection().getName());
		wLogtable.setText( Const.NVL(transMeta.getLogTable(),"") );
		wStepLogtable.setText( Const.NVL(transMeta.getStepPerformanceLogTable(),"") );
		wLogSizeLimit.setText( Const.NVL(transMeta.getLogSizeLimit(), ""));

		wBatch.setSelection(transMeta.isBatchIdUsed());
		wLogfield.setSelection(transMeta.isLogfieldUsed());
		
		if (transMeta.getMaxDateConnection()!=null) wMaxdateconnection.setText( transMeta.getMaxDateConnection().getName());
		if (transMeta.getMaxDateTable()!=null)      wMaxdatetable.setText     ( transMeta.getMaxDateTable());
		if (transMeta.getMaxDateField()!=null)      wMaxdatefield.setText     ( transMeta.getMaxDateField());
		wMaxdateoffset.setText(Double.toString(transMeta.getMaxDateOffset()));
		wMaxdatediff.setText(Double.toString(transMeta.getMaxDateDifference()));

		// The dependencies
		for (int i=0;i<transMeta.nrDependencies();i++)
		{
			TableItem item = wFields.table.getItem(i);
			TransDependency td = transMeta.getDependency(i);
			
			DatabaseMeta conn = td.getDatabase();
			String table   = td.getTablename();
			String field   = td.getFieldname();
			if (conn !=null) item.setText(1, conn.getName() );
			if (table!=null) item.setText(2, table);
			if (field!=null) item.setText(3, field);
		}

		// The named parameters
		String[] parameters = transMeta.listParameters();
		for (int idx=0;idx<parameters.length;idx++)
		{
			TableItem item = wParamFields.table.getItem(idx);
			
			String defValue;
			try {
				defValue = transMeta.getParameterDefault(parameters[idx]);
			} catch (UnknownParamException e) {
				defValue = "";
			}
			String description;
			try {
				description = transMeta.getParameterDescription(parameters[idx]);
			} catch (UnknownParamException e) {
				description = "";
			}
						
			item.setText(1, parameters[idx]);
			item.setText(2, Const.NVL(defValue, ""));
			item.setText(3, Const.NVL(description, ""));
		}
				
		wSizeRowset.setText(Integer.toString(transMeta.getSizeRowset()));
		wUniqueConnections.setSelection(transMeta.isUsingUniqueConnections());
		wShowFeedback.setSelection(transMeta.isFeedbackShown());
        wFeedbackSize.setText(Integer.toString(transMeta.getFeedbackSize()));
        wSharedObjectsFile.setText(Const.NVL(transMeta.getSharedObjectsFile(), ""));
        wManageThreads.setSelection(transMeta.isUsingThreadPriorityManagment());
        
		wFields.setRowNums();
		wFields.optWidth(true);
		
		wParamFields.setRowNums();
		wParamFields.optWidth(true);
        
		// Directory:
		if (transMeta.getRepositoryDirectory()!=null && transMeta.getRepositoryDirectory().getPath()!=null) 
			wDirectory.setText(transMeta.getRepositoryDirectory().getPath());
		
		// Performance monitoring tab:
		//
		wEnableStepPerfMonitor.setSelection(transMeta.isCapturingStepPerformanceSnapShots());
		wEnableStepPerfInterval.setText(Long.toString(transMeta.getStepPerformanceCapturingDelay()));
		
		wTransname.selectAll();
		wTransname.setFocus();
		
		setFlags();
	}
	
	public void setFlags()
    {
        wbDirectory.setEnabled(rep!=null);
        // wDirectory.setEnabled(rep!=null);
        wlDirectory.setEnabled(rep!=null);
        
        wlStepLogtable.setEnabled(wEnableStepPerfMonitor.getSelection());
        wStepLogtable.setEnabled(wEnableStepPerfMonitor.getSelection());
        
        wlLogSizeLimit.setEnabled(wLogfield.getSelection());
        wLogSizeLimit.setEnabled(wLogfield.getSelection());
     }

    private void cancel()
	{
		props.setScreen(new WindowProperty(shell));
		transMeta=null;
		dispose();
	}
	
	private void ok() {
		boolean OK = true;

		transMeta.setReadStep(transMeta.findStep(wReadStep.getText()));
		transMeta.setWriteStep(transMeta.findStep(wWriteStep.getText()));
		transMeta.setInputStep(transMeta.findStep(wInputStep.getText()));
		transMeta.setOutputStep(transMeta.findStep(wOutputStep.getText()));
		transMeta.setUpdateStep(transMeta.findStep(wUpdateStep.getText()));
		transMeta.setRejectedStep(transMeta.findStep(wRejectedStep.getText()));

		transMeta.setLogConnection(transMeta.findDatabase(wLogconnection.getText()));
		transMeta.setLogTable(wLogtable.getText());
		transMeta.setStepPerformanceLogTable(wStepLogtable.getText());
		transMeta.setLogSizeLimit(wLogSizeLimit.getText());
		transMeta.setMaxDateConnection(transMeta.findDatabase(wMaxdateconnection.getText()));
		transMeta.setMaxDateTable(wMaxdatetable.getText());
		transMeta.setMaxDateField(wMaxdatefield.getText());
		transMeta.setBatchIdUsed(wBatch.getSelection());
		transMeta.setLogfieldUsed(wLogfield.getSelection());
		transMeta.setName(wTransname.getText());

		transMeta.setDescription(wTransdescription.getText());
		transMeta.setExtendedDescription(wExtendeddescription.getText());
		transMeta.setTransversion(wTransversion.getText());

		if (wTransstatus.getSelectionIndex() != 2) {
			transMeta.setTransstatus(wTransstatus.getSelectionIndex() + 1);
		} else {
			transMeta.setTransstatus(-1);
		}

		try {
			transMeta.setMaxDateOffset(Double.parseDouble(wMaxdateoffset.getText()));
		} catch (Exception e) {
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setText(BaseMessages.getString(PKG, "TransDialog.InvalidOffsetNumber.DialogTitle")); //$NON-NLS-1$
			mb.setMessage(BaseMessages.getString(PKG, "TransDialog.InvalidOffsetNumber.DialogMessage")); //$NON-NLS-1$
			mb.open();
			wMaxdateoffset.setFocus();
			wMaxdateoffset.selectAll();
			OK = false;
		}

		try {
			transMeta.setMaxDateDifference(Double.parseDouble(wMaxdatediff.getText()));
		} catch (Exception e) {
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setText(BaseMessages.getString(PKG, "TransDialog.InvalidDateDifferenceNumber.DialogTitle")); //$NON-NLS-1$
			mb.setMessage(BaseMessages.getString(PKG, "TransDialog.InvalidDateDifferenceNumber.DialogMessage")); //$NON-NLS-1$
			mb.open();
			wMaxdatediff.setFocus();
			wMaxdatediff.selectAll();
			OK = false;
		}

		// Clear and add current dependencies
		transMeta.removeAllDependencies();
		int nrNonEmptyFields = wFields.nrNonEmpty();
		for (int i = 0; i < nrNonEmptyFields; i++) {
			TableItem item = wFields.getNonEmpty(i);

			DatabaseMeta db = transMeta.findDatabase(item.getText(1));
			String tablename = item.getText(2);
			String fieldname = item.getText(3);
			TransDependency td = new TransDependency(db, tablename, fieldname);
			transMeta.addDependency(td);
		}

		// Clear and add parameters
		transMeta.eraseParameters();
		nrNonEmptyFields = wParamFields.nrNonEmpty();
		for (int i = 0; i < nrNonEmptyFields; i++) {
			TableItem item = wParamFields.getNonEmpty(i);

			try {
				transMeta.addParameterDefinition(item.getText(1), item.getText(2), item.getText(3));
			} catch (DuplicateParamException e) {
				// Ignore the duplicate parameter.
			}
		}

		transMeta.setSizeRowset(Const.toInt(wSizeRowset.getText(), Const.ROWS_IN_ROWSET));
		transMeta.setUsingUniqueConnections(wUniqueConnections.getSelection());

		transMeta.setFeedbackShown(wShowFeedback.getSelection());
		transMeta.setFeedbackSize(Const.toInt(wFeedbackSize.getText(), Const.ROWS_UPDATE));
		transMeta.setSharedObjectsFile(wSharedObjectsFile.getText());
		transMeta.setUsingThreadPriorityManagment(wManageThreads.getSelection());

		if (directoryChangeAllowed && transMeta.getObjectId()!=null) {
			if (newDirectory != null) {
				RepositoryDirectory dirFrom = transMeta.getRepositoryDirectory();

				try {
					ObjectId newId = rep.renameTransformation(transMeta.getObjectId(), newDirectory, transMeta.getName());
					transMeta.setObjectId(newId);
					transMeta.setRepositoryDirectory(newDirectory);
				} catch (KettleException ke) {
					transMeta.setRepositoryDirectory(dirFrom);
					OK = false;
					new ErrorDialog(shell, BaseMessages.getString(PKG, "TransDialog.ErrorMovingTransformation.DialogTitle"), BaseMessages.getString(PKG, "TransDialog.ErrorMovingTransformation.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} else {
			// Just update to the new selected directory...
			//
			if (newDirectory != null)
				transMeta.setRepositoryDirectory(newDirectory);
		}

		// Performance monitoring tab:
		//
		transMeta.setCapturingStepPerformanceSnapShots(wEnableStepPerfMonitor.getSelection());

		try {
			transMeta.setStepPerformanceCapturingDelay(Long.parseLong(wEnableStepPerfInterval.getText()));
		} catch (Exception e) {
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setText(BaseMessages.getString(PKG, "TransDialog.InvalidStepPerfIntervalNumber.DialogTitle")); //$NON-NLS-1$
			mb.setMessage(BaseMessages.getString(PKG, "TransDialog.InvalidStepPerfIntervalNumber.DialogMessage")); //$NON-NLS-1$
			mb.open();
			wEnableStepPerfInterval.setFocus();
			wEnableStepPerfInterval.selectAll();
			OK = false;
		}

		if (OK) {
			transMeta.setChanged(changed || transMeta.hasChanged());
			dispose();
		}
	}
	
	// Get the dependencies
	private void get()
	{
		Table table = wFields.table;
		for (int i=0;i<transMeta.nrSteps();i++)
		{
			StepMeta stepMeta = transMeta.getStep(i);
			String con=null;
			String tab=null;
			TableItem item=null;
			StepMetaInterface sii = stepMeta.getStepMetaInterface();
			if (sii instanceof TableInputMeta)
			{
				TableInputMeta tii = (TableInputMeta)stepMeta.getStepMetaInterface();
				if (tii.getDatabaseMeta()==null)
				{
					 MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                     mb.setMessage(BaseMessages.getString(PKG, "TransDialog.DatabaseMetaNotSet.Text"));
                     mb.open();
                     
                     return;
				}
				con  = tii.getDatabaseMeta().getName();
				tab  = getTableFromSQL(tii.getSQL());
				if (tab==null) tab=stepMeta.getName();
			}
			if (sii instanceof DatabaseLookupMeta)
			{
				DatabaseLookupMeta dvli = (DatabaseLookupMeta)stepMeta.getStepMetaInterface();
				con  = dvli.getDatabaseMeta().getName();
				tab  = dvli.getTablename();
				if (tab==null) tab=stepMeta.getName();
				break;	
			}

			if (tab!=null || con!=null)
			{
				item = new TableItem(table, SWT.NONE);
				if (con!=null) item.setText(1, con);
				if (tab!=null) item.setText(2, tab);
			}
		}
		wFields.setRowNums();
	}
	
	private String getTableFromSQL(String sql)
	{
		if (sql==null) return null;
		
		int idxfrom = sql.toUpperCase().indexOf("FROM"); //$NON-NLS-1$
		int idxto   = sql.toUpperCase().indexOf("WHERE"); //$NON-NLS-1$
		if (idxfrom==-1) return null;
		if (idxto==-1) idxto=sql.length();
		return sql.substring(idxfrom+5, idxto);
	}

	// Generate code for create table...
	// Conversions done by Database
	private void sql()
	{
		DatabaseMeta ci = transMeta.findDatabase(wLogconnection.getText());
		if (ci!=null)
		{
			String tablename = wLogtable.getText();
			String stepTablename = wStepLogtable.getText();
			
			if (!Const.isEmpty(tablename) || !Const.isEmpty(stepTablename) )
			{
				Database db = new Database(transMeta, ci);
				db.shareVariablesWith(transMeta);
				try
				{
					db.connect();
					
					RowMetaInterface r;
					String createTable = "";
					
					if (!Const.isEmpty(tablename)) {
						r = Database.getTransLogrecordFields(false, wBatch.getSelection(), wLogfield.getSelection());
						createTable += db.getDDL(tablename, r);
					}
					
					if (!Const.isEmpty(stepTablename)) {
						r = Database.getStepPerformanceLogrecordFields();
						createTable += db.getDDL(stepTablename, r);
					}
					
					if (!Const.isEmpty(createTable))
					{
						SQLEditor sqledit = new SQLEditor(shell, SWT.NONE, ci, transMeta.getDbCache(), createTable);
						sqledit.open();
					}
					else
					{
						MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION );
						mb.setText(BaseMessages.getString(PKG, "TransDialog.NoSqlNedds.DialogTitle")); //$NON-NLS-1$
						mb.setMessage(BaseMessages.getString(PKG, "TransDialog.NoSqlNedds.DialogMessage")); //$NON-NLS-1$
						mb.open(); 
					}
				}
				catch(KettleException e)
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
					mb.setText(BaseMessages.getString(PKG, "TransDialog.ErrorOccurred.DialogTitle")); //$NON-NLS-1$
					mb.setMessage(BaseMessages.getString(PKG, "TransDialog.ErrorOccurred.DialogMessage")+Const.CR+e.getMessage()); //$NON-NLS-1$
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
				mb.setText(BaseMessages.getString(PKG, "TransDialog.NeedLogtableName.DialogTitle")); //$NON-NLS-1$
				mb.setMessage(BaseMessages.getString(PKG, "TransDialog.NeedLogtableName.DialogMessage")); //$NON-NLS-1$
				mb.open(); 
			}
		}
		else
		{
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR );
			mb.setText(BaseMessages.getString(PKG, "TransDialog.NeedValidLogtableConnection.DialogTitle")); //$NON-NLS-1$
			mb.setMessage(BaseMessages.getString(PKG, "TransDialog.NeedValidLogtableConnection.DialogMessage")); //$NON-NLS-1$
			mb.open(); 
		}
	}



	public String toString()
	{
		return this.getClass().getName();
	}


    public boolean isSharedObjectsFileChanged()
    {
        return sharedObjectsFileChanged;
    }


	public void setDirectoryChangeAllowed(boolean directoryChangeAllowed) {
		this.directoryChangeAllowed = directoryChangeAllowed;
	}
	
	private void setCurrentTab(Tabs currentTab){
	  
	  switch (currentTab) {
		case TRANS_TAB:
			wTabFolder.setSelection(wTransTab);
			break;
		case PARAM_TAB:
			wTabFolder.setSelection(wParamTab);
			break;
		case MISC_TAB:
			wTabFolder.setSelection(wMiscTab);
			break;
		case DATE_TAB:
			wTabFolder.setSelection(wDateTab);
			break;
		case LOG_TAB:
			wTabFolder.setSelection(wLogTab);
			break;
		case DEP_TAB:
			wTabFolder.setSelection(wDepTab);
			break;
		case MONITOR_TAB:
			wTabFolder.setSelection(wMonitorTab);
			break;
		}
	}
}