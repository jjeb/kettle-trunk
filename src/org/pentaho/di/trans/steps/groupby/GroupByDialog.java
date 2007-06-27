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
 * Created on 2-jul-2003
 *
 */

//import java.text.DateFormat;
//import java.util.Date;

package org.pentaho.di.trans.steps.groupby;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.dialog.ErrorDialog;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.widget.ColumnInfo;
import org.pentaho.di.core.widget.TableView;
import org.pentaho.di.core.widget.TextVar;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepDialog;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;


public class GroupByDialog extends BaseStepDialog implements StepDialogInterface
{
    public static final String STRING_SORT_WARNING_PARAMETER = "GroupSortWarning"; //$NON-NLS-1$
	private Label        wlGroup;
	private TableView    wGroup;
	private FormData     fdlGroup, fdGroup;

	private Label        wlAgg;
	private TableView    wAgg;
	private FormData     fdlAgg, fdAgg;

	private Label        wlAllRows;
	private Button       wAllRows;
	private FormData     fdlAllRows, fdAllRows;

    private Label        wlSortDir;
    private Button       wbSortDir;
    private TextVar      wSortDir;
    private FormData     fdlSortDir, fdbSortDir, fdSortDir;

    private Label        wlPrefix;
    private Text         wPrefix;
    private FormData     fdlPrefix, fdPrefix;

    private Label        wlAddLineNr;
    private Button       wAddLineNr;
    private FormData     fdlAddLineNr, fdAddLineNr;
    
    private Label        wlLineNrField;
    private Text         wLineNrField;
    private FormData     fdlLineNrField, fdLineNrField;

	private Button wGet, wGetAgg;
	private FormData fdGet, fdGetAgg;
	private Listener lsGet, lsGetAgg;

	private GroupByMeta input;
	private boolean backupAllRows;

	public GroupByDialog(Shell parent, Object in, TransMeta transMeta, String sname)
	{
		super(parent, (BaseStepMeta)in, transMeta, sname);
		input=(GroupByMeta)in;
	}

	public String open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
 		props.setLook(shell);
        setShellImage(shell, input);

		ModifyListener lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				input.setChanged();
			}
		};
		backupChanged = input.hasChanged();
		backupAllRows = input.passAllRows();

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("GroupByDialog.Shell.Title")); //$NON-NLS-1$
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;
		
		// Stepname line
		wlStepname=new Label(shell, SWT.RIGHT);
		wlStepname.setText(Messages.getString("GroupByDialog.Stepname.Label")); //$NON-NLS-1$
 		props.setLook(wlStepname);
		fdlStepname=new FormData();
		fdlStepname.left = new FormAttachment(0, 0);
		fdlStepname.right= new FormAttachment(middle, -margin);
		fdlStepname.top  = new FormAttachment(0, margin);
		wlStepname.setLayoutData(fdlStepname);
		wStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wStepname.setText(stepname);
 		props.setLook(wStepname);
		wStepname.addModifyListener(lsMod);
		fdStepname=new FormData();
		fdStepname.left = new FormAttachment(middle, 0);
		fdStepname.top  = new FormAttachment(0, margin);
		fdStepname.right= new FormAttachment(100, 0);
		wStepname.setLayoutData(fdStepname);

		// Include all rows?
		wlAllRows=new Label(shell, SWT.RIGHT);
		wlAllRows.setText(Messages.getString("GroupByDialog.AllRows.Label")); //$NON-NLS-1$
 		props.setLook(wlAllRows);
		fdlAllRows=new FormData();
		fdlAllRows.left = new FormAttachment(0, 0);
		fdlAllRows.top  = new FormAttachment(wStepname, margin);
		fdlAllRows.right= new FormAttachment(middle, -margin);
		wlAllRows.setLayoutData(fdlAllRows);
		wAllRows=new Button(shell, SWT.CHECK );
 		props.setLook(wAllRows);
		fdAllRows=new FormData();
		fdAllRows.left = new FormAttachment(middle, 0);
		fdAllRows.top  = new FormAttachment(wStepname, margin);
		fdAllRows.right= new FormAttachment(100, 0);
		wAllRows.setLayoutData(fdAllRows);
		wAllRows.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent e) 
				{
					input.setPassAllRows( !input.passAllRows() );
					input.setChanged();
                    setFlags();
				}
			}
		);
        
        wlSortDir=new Label(shell, SWT.RIGHT);
        wlSortDir.setText(Messages.getString("GroupByDialog.TempDir.Label")); //$NON-NLS-1$
        props.setLook(wlSortDir);
        fdlSortDir=new FormData();
        fdlSortDir.left = new FormAttachment(0, 0);
        fdlSortDir.right= new FormAttachment(middle, -margin);
        fdlSortDir.top  = new FormAttachment(wAllRows, margin);
        wlSortDir.setLayoutData(fdlSortDir);

        wbSortDir=new Button(shell, SWT.PUSH| SWT.CENTER);
        props.setLook(wbSortDir);
        wbSortDir.setText(Messages.getString("GroupByDialog.Browse.Button")); //$NON-NLS-1$
        fdbSortDir=new FormData();
        fdbSortDir.right= new FormAttachment(100, 0);
        fdbSortDir.top  = new FormAttachment(wAllRows, margin);
        wbSortDir.setLayoutData(fdbSortDir);

        wSortDir=new TextVar(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wSortDir);
        wSortDir.addModifyListener(lsMod);
        fdSortDir=new FormData();
        fdSortDir.left = new FormAttachment(middle, 0);
        fdSortDir.top  = new FormAttachment(wAllRows, margin);
        fdSortDir.right= new FormAttachment(wbSortDir, -margin);
        wSortDir.setLayoutData(fdSortDir);
        
        wbSortDir.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent arg0)
            {
                DirectoryDialog dd = new DirectoryDialog(shell, SWT.NONE);
                dd.setFilterPath(wSortDir.getText());
                String dir = dd.open();
                if (dir!=null)
                {
                    wSortDir.setText(dir);
                }
            }
        });

        // Whenever something changes, set the tooltip to the expanded version:
        wSortDir.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    wSortDir.setToolTipText(transMeta.environmentSubstitute( wSortDir.getText() ) );
                }
            }
        );

        // Prefix line...
        wlPrefix=new Label(shell, SWT.RIGHT);
        wlPrefix.setText(Messages.getString("GroupByDialog.FilePrefix.Label")); //$NON-NLS-1$
        props.setLook(wlPrefix);
        fdlPrefix=new FormData();
        fdlPrefix.left = new FormAttachment(0, 0);
        fdlPrefix.right= new FormAttachment(middle, -margin);
        fdlPrefix.top  = new FormAttachment(wbSortDir, margin*2);
        wlPrefix.setLayoutData(fdlPrefix);
        wPrefix=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook(wPrefix);
        wPrefix.addModifyListener(lsMod);
        fdPrefix=new FormData();
        fdPrefix.left  = new FormAttachment(middle, 0);
        fdPrefix.top   = new FormAttachment(wbSortDir, margin*2);
        fdPrefix.right = new FormAttachment(100, 0);
        wPrefix.setLayoutData(fdPrefix);

        // Include all rows?
        wlAddLineNr=new Label(shell, SWT.RIGHT);
        wlAddLineNr.setText(Messages.getString("GroupByDialog.AddLineNr.Label")); //$NON-NLS-1$
        props.setLook(wlAddLineNr);
        fdlAddLineNr=new FormData();
        fdlAddLineNr.left = new FormAttachment(0, 0);
        fdlAddLineNr.top  = new FormAttachment(wPrefix, margin);
        fdlAddLineNr.right= new FormAttachment(middle, -margin);
        wlAddLineNr.setLayoutData(fdlAddLineNr);
        wAddLineNr=new Button(shell, SWT.CHECK );
        props.setLook(wAddLineNr);
        fdAddLineNr=new FormData();
        fdAddLineNr.left = new FormAttachment(middle, 0);
        fdAddLineNr.top  = new FormAttachment(wPrefix, margin);
        fdAddLineNr.right= new FormAttachment(100, 0);
        wAddLineNr.setLayoutData(fdAddLineNr);
        wAddLineNr.addSelectionListener(new SelectionAdapter() 
            {
                public void widgetSelected(SelectionEvent e) 
                {
                    input.setAddingLineNrInGroup( !input.isAddingLineNrInGroup() );
                    input.setChanged();
                    setFlags();
                }
            }
        );

        // LineNrField line...
        wlLineNrField=new Label(shell, SWT.RIGHT);
        wlLineNrField.setText(Messages.getString("GroupByDialog.LineNrField.Label")); //$NON-NLS-1$
        props.setLook(wlLineNrField);
        fdlLineNrField=new FormData();
        fdlLineNrField.left = new FormAttachment(0, 0);
        fdlLineNrField.right= new FormAttachment(middle, -margin);
        fdlLineNrField.top  = new FormAttachment(wAddLineNr, margin);
        wlLineNrField.setLayoutData(fdlLineNrField);
        wLineNrField=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook(wLineNrField);
        wLineNrField.addModifyListener(lsMod);
        fdLineNrField=new FormData();
        fdLineNrField.left  = new FormAttachment(middle, 0);
        fdLineNrField.top   = new FormAttachment(wAddLineNr, margin);
        fdLineNrField.right = new FormAttachment(100, 0);
        wLineNrField.setLayoutData(fdLineNrField);

        
		wlGroup=new Label(shell, SWT.NONE);
		wlGroup.setText(Messages.getString("GroupByDialog.Group.Label")); //$NON-NLS-1$
 		props.setLook(wlGroup);
		fdlGroup=new FormData();
		fdlGroup.left  = new FormAttachment(0, 0);
		fdlGroup.top   = new FormAttachment(wLineNrField, margin);
		wlGroup.setLayoutData(fdlGroup);

		int nrKeyCols=1;
		int nrKeyRows=(input.getGroupField()!=null?input.getGroupField().length:1);
		
		ColumnInfo[] ciKey=new ColumnInfo[nrKeyCols];
		ciKey[0]=new ColumnInfo(Messages.getString("GroupByDialog.ColumnInfo.GroupField"),  ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
		
		wGroup=new TableView(shell, 
						      SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, 
						      ciKey, 
						      nrKeyRows,  
						      lsMod,
							  props
						      );

		wGet=new Button(shell, SWT.PUSH);
		wGet.setText(Messages.getString("GroupByDialog.GetFields.Button")); //$NON-NLS-1$
		fdGet = new FormData();
		fdGet.top   = new FormAttachment(wlGroup, margin);
		fdGet.right = new FormAttachment(100, 0);
		wGet.setLayoutData(fdGet);
		
		fdGroup=new FormData();
		fdGroup.left  = new FormAttachment(0, 0);
		fdGroup.top   = new FormAttachment(wlGroup, margin);
		fdGroup.right = new FormAttachment(wGet, -margin);
		fdGroup.bottom= new FormAttachment(45, 0);
		wGroup.setLayoutData(fdGroup);

		// THE Aggregate fields
		wlAgg=new Label(shell, SWT.NONE);
		wlAgg.setText(Messages.getString("GroupByDialog.Aggregates.Label")); //$NON-NLS-1$
 		props.setLook(wlAgg);
		fdlAgg=new FormData();
		fdlAgg.left  = new FormAttachment(0, 0);
		fdlAgg.top   = new FormAttachment(wGroup, margin);
		wlAgg.setLayoutData(fdlAgg);
		
		int UpInsCols=3;
		int UpInsRows= (input.getAggregateField()!=null?input.getAggregateField().length:1);
		
		ColumnInfo[] ciReturn=new ColumnInfo[UpInsCols];
		ciReturn[0]=new ColumnInfo(Messages.getString("GroupByDialog.ColumnInfo.Name"),     ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
		ciReturn[1]=new ColumnInfo(Messages.getString("GroupByDialog.ColumnInfo.Subject"),  ColumnInfo.COLUMN_TYPE_TEXT,   false); //$NON-NLS-1$
		ciReturn[2]=new ColumnInfo(Messages.getString("GroupByDialog.ColumnInfo.Type"),     ColumnInfo.COLUMN_TYPE_CCOMBO, GroupByMeta.typeGroupLongDesc); //$NON-NLS-1$
		
		wAgg=new TableView(shell, 
							  SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, 
							  ciReturn, 
							  UpInsRows,  
							  lsMod,
							  props
							  );

		wGetAgg=new Button(shell, SWT.PUSH);
		wGetAgg.setText(Messages.getString("GroupByDialog.GetLookupFields.Button")); //$NON-NLS-1$
		fdGetAgg = new FormData();
		fdGetAgg.top   = new FormAttachment(wlAgg, margin);
		fdGetAgg.right = new FormAttachment(100, 0);
		wGetAgg.setLayoutData(fdGetAgg);

		// THE BUTTONS
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel")); //$NON-NLS-1$

		setButtonPositions(new Button[] { wOK, wCancel }, margin, null);

		fdAgg=new FormData();
		fdAgg.left  = new FormAttachment(0, 0);
		fdAgg.top   = new FormAttachment(wlAgg, margin);
		fdAgg.right = new FormAttachment(wGetAgg, -margin);
		fdAgg.bottom= new FormAttachment(wOK, -margin);
		wAgg.setLayoutData(fdAgg);

		// Add listeners
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();        } };
		lsGet      = new Listener() { public void handleEvent(Event e) { get();       } };
		lsGetAgg   = new Listener() { public void handleEvent(Event e) { getAgg(); } };
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel();    } };
		
		wOK.addListener    (SWT.Selection, lsOK    );
		wGet.addListener   (SWT.Selection, lsGet   );
		wGetAgg.addListener (SWT.Selection, lsGetAgg );
		wCancel.addListener(SWT.Selection, lsCancel);
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wStepname.addSelectionListener( lsDef );
		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );



		// Set the shell size, based upon previous time...
		setSize();
				
		getData();
		input.setChanged(backupChanged);

		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return stepname;
	}
    
    public void setFlags()
    {
        wlSortDir.setEnabled( wAllRows.getSelection() );
        wbSortDir.setEnabled( wAllRows.getSelection() );
        wSortDir.setEnabled( wAllRows.getSelection() );
        wlPrefix.setEnabled( wAllRows.getSelection() );
        wPrefix.setEnabled( wAllRows.getSelection() );
        wlAddLineNr.setEnabled( wAllRows.getSelection() );
        wAddLineNr.setEnabled( wAllRows.getSelection() );
        
        wlLineNrField.setEnabled( wAllRows.getSelection() && wAddLineNr.getSelection() );
        wLineNrField.setEnabled( wAllRows.getSelection() && wAddLineNr.getSelection() );
    }

	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{
		int i;
		log.logDebug(toString(), Messages.getString("GroupByDialog.Log.GettingKeyInfo")); //$NON-NLS-1$
		
		wAllRows.setSelection(input.passAllRows());
		
        if (input.getPrefix() != null) wPrefix.setText(input.getPrefix());
        if (input.getDirectory() != null) wSortDir.setText(input.getDirectory());
        wAddLineNr.setSelection( input.isAddingLineNrInGroup() );
        if (input.getLineNrInGroupField()!=null) wLineNrField.setText( input.getLineNrInGroupField() );
        
		if (input.getGroupField()!=null)
		for (i=0;i<input.getGroupField().length;i++)
		{
			TableItem item = wGroup.table.getItem(i);
			if (input.getGroupField()[i]   !=null) item.setText(1, input.getGroupField()[i]);
		}
		
		if (input.getAggregateField()!=null)
		for (i=0;i<input.getAggregateField().length;i++)
		{
			TableItem item = wAgg.table.getItem(i);
			if (input.getAggregateField()[i]!=null     ) item.setText(1, input.getAggregateField()[i]);
			if (input.getSubjectField()[i]!=null       ) item.setText(2, input.getSubjectField()[i]);
			item.setText(3, GroupByMeta.getTypeDescLong(input.getAggregateType()[i]));
		}
        
		wStepname.selectAll();
		wGroup.setRowNums();
		wGroup.optWidth(true);
		wAgg.setRowNums();
		wAgg.optWidth(true);
        
        setFlags();
	}
	
	private void cancel()
	{
		stepname=null;
		input.setChanged(backupChanged);
		input.setPassAllRows(  backupAllRows );
		dispose();
	}
	
	private void ok()
	{
		int sizegroup = wGroup.nrNonEmpty();
		int nrfields = wAgg.nrNonEmpty();
        input.setPrefix( wPrefix.getText() );
        input.setDirectory( wSortDir.getText() );

        input.setLineNrInGroupField( wLineNrField.getText() );
        
		input.allocate(sizegroup, nrfields);
				
		for (int i=0;i<sizegroup;i++)
		{
			TableItem item = wGroup.getNonEmpty(i);
			input.getGroupField()[i]    = item.getText(1);
		}
		
		for (int i=0;i<nrfields;i++)
		{
			TableItem item      = wAgg.getNonEmpty(i);
			input.getAggregateField()[i]  = item.getText(1);		
			input.getSubjectField()[i]    = item.getText(2);		
			input.getAggregateType()[i]       = GroupByMeta.getType(item.getText(3));		
		}
		
		stepname = wStepname.getText();
        
        if ( "Y".equalsIgnoreCase( props.getCustomParameter(STRING_SORT_WARNING_PARAMETER, "Y") )) //$NON-NLS-1$ //$NON-NLS-2$
        {
            MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
                 Messages.getString("GroupByDialog.GroupByWarningDialog.DialogTitle"),  //$NON-NLS-1$
                 null,
                 Messages.getString("GroupByDialog.GroupByWarningDialog.DialogMessage", Const.CR )+Const.CR, //$NON-NLS-1$ //$NON-NLS-2$
                 MessageDialog.WARNING,
                 new String[] { Messages.getString("GroupByDialog.GroupByWarningDialog.Option1") }, //$NON-NLS-1$
                 0,
                 Messages.getString("GroupByDialog.GroupByWarningDialog.Option2"), //$NON-NLS-1$
                 "N".equalsIgnoreCase( props.getCustomParameter(STRING_SORT_WARNING_PARAMETER, "Y") ) //$NON-NLS-1$ //$NON-NLS-2$
            );
            md.open();
            props.setCustomParameter(STRING_SORT_WARNING_PARAMETER, md.getToggleState()?"N":"Y"); //$NON-NLS-1$ //$NON-NLS-2$
            props.saveProps();
        }
					
		dispose();
	}

	private void get()
	{
		try
		{
			RowMetaInterface r = transMeta.getPrevStepFields(stepname);
			if (r!=null)
			{
                BaseStepDialog.getFieldsFromPrevious(r, wGroup, 1, new int[] { 1 }, new int[] {}, -1, -1, null);
			}
		}
		catch(KettleException ke)
		{
			new ErrorDialog(shell, Messages.getString("GroupByDialog.FailedToGetFields.DialogTitle"), Messages.getString("GroupByDialog.FailedToGetFields.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void getAgg()
	{
		try
		{
			RowMetaInterface r = transMeta.getPrevStepFields(stepname);
			if (r!=null)
			{
                BaseStepDialog.getFieldsFromPrevious(r, wAgg, 1, new int[] { 1, 2 }, new int[] {}, -1, -1, null);
			}
		}
		catch(KettleException ke)
		{
			new ErrorDialog(shell, Messages.getString("GroupByDialog.FailedToGetFields.DialogTitle"), Messages.getString("GroupByDialog.FailedToGetFields.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public String toString()
	{
		return this.getClass().getName();
	}
}
