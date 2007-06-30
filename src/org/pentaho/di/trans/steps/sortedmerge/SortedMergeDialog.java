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
 * Created on 18-mei-2003
 *
 */

package org.pentaho.di.trans.steps.sortedmerge;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.dialog.ErrorDialog;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.widget.ColumnInfo;
import org.pentaho.di.core.widget.TableView;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepDialog;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.TableItemInsertListener;

public class SortedMergeDialog extends BaseStepDialog implements StepDialogInterface
{
    private Label        wlFields;
    private TableView    wFields;
    private FormData     fdlFields, fdFields;

	private SortedMergeMeta input;

	public SortedMergeDialog(Shell parent, Object in, TransMeta tr, String sname)
	{
		super(parent, (BaseStepMeta)in, tr, sname);
		input=(SortedMergeMeta)in;
	}

	public String open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
 		props.setLook(shell);
        setShellImage(shell, input);

		ModifyListener lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				input.setChanged();
			}
		};
		changed = input.hasChanged();

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("SortedMergeDialog.Shell.Title")); //$NON-NLS-1$
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Stepname line
		wlStepname=new Label(shell, SWT.RIGHT);
		wlStepname.setText(Messages.getString("SortedMergeDialog.Stepname.Label")); //$NON-NLS-1$
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
		
		// Some buttons
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK")); //$NON-NLS-1$
        wGet=new Button(shell, SWT.PUSH);
        wGet.setText(Messages.getString("System.Button.GetFields")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel")); //$NON-NLS-1$

		setButtonPositions(new Button[] { wOK, wGet, wCancel }, margin, null);

        
        wlFields=new Label(shell, SWT.NONE);
        wlFields.setText(Messages.getString("SortedMergeDialog.Fields.Label"));
        props.setLook(wlFields);
        fdlFields=new FormData();
        fdlFields.left = new FormAttachment(0, 0);
        fdlFields.top  = new FormAttachment(wStepname, margin);
        wlFields.setLayoutData(fdlFields);
        
        final int FieldsCols=2;
        final int FieldsRows=input.getFieldName().length;
        
        ColumnInfo[] colinf=new ColumnInfo[FieldsCols];
        colinf[0]=new ColumnInfo(Messages.getString("SortedMergeDialog.Fieldname.Column"),  ColumnInfo.COLUMN_TYPE_TEXT,   false);
        colinf[1]=new ColumnInfo(Messages.getString("SortedMergeDialog.Ascending.Column"),  ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { Messages.getString("System.Combo.Yes"), Messages.getString("System.Combo.No") } );
        
        wFields=new TableView(transMeta, shell, 
                              SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
                              colinf, 
                              FieldsRows,  
                              lsMod,
                              props
                              );

        fdFields=new FormData();
        fdFields.left  = new FormAttachment(0, 0);
        fdFields.top   = new FormAttachment(wlFields, margin);
        fdFields.right = new FormAttachment(100, 0);
        fdFields.bottom= new FormAttachment(wOK, -2*margin);
        wFields.setLayoutData(fdFields);
        
		// Add listeners
		wCancel.addListener(SWT.Selection, new Listener() { public void handleEvent(Event e) { cancel(); } });
		wGet.addListener   (SWT.Selection, new Listener() { public void handleEvent(Event e) { get();    } });
        wOK.addListener    (SWT.Selection, new Listener() { public void handleEvent(Event e) { ok();     } });
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wStepname.addSelectionListener( lsDef );
		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );


		// Set the shell size, based upon previous time...
		setSize();
		
		getData();
		input.setChanged(changed);
	
		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return stepname;
	}
	
	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{
        Table table = wFields.table;
        if (input.getFieldName().length>0) table.removeAll();
        for (int i=0;i<input.getFieldName().length;i++)
        {
            TableItem ti = new TableItem(table, SWT.NONE);
            ti.setText(0, ""+(i+1));
            ti.setText(1, input.getFieldName()[i]);
            ti.setText(2, input.getAscending()[i]?Messages.getString("System.Combo.Yes"):Messages.getString("System.Combo.No"));
        }

        wFields.setRowNums();
        wFields.optWidth(true);

		wStepname.selectAll();
	}
	
	private void cancel()
	{
		stepname=null;
		input.setChanged(changed);
		dispose();
	}
	
	private void ok()
	{
		stepname = wStepname.getText(); // return value
		
        int nrfields = wFields.nrNonEmpty();

        input.allocate(nrfields);
        
        for (int i=0;i<nrfields;i++)
        {
            TableItem ti = wFields.getNonEmpty(i);
            input.getFieldName()[i] = ti.getText(1);
            input.getAscending()[i] = Messages.getString("System.Combo.Yes").equalsIgnoreCase(ti.getText(2));
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
                BaseStepDialog.getFieldsFromPrevious(r, wFields, 1, new int[] { 1 }, new int[] {}, -1, -1, new TableItemInsertListener()
                    {
                        public boolean tableItemInserted(TableItem tableItem, ValueMetaInterface v)
                        {
                            tableItem.setText(2, "Y"); //$NON-NLS-1$
                            return true;
                        }
                    }
                );
            }
        }
        catch(KettleException ke)
        {
            new ErrorDialog(shell, Messages.getString("CombinationLookupDialog.UnableToGetFieldsError.DialogTitle"), Messages.getString("CombinationLookupDialog.UnableToGetFieldsError.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
