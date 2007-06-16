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

 
package be.ibridge.kettle.core.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import be.ibridge.kettle.core.ColumnInfo;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.GUIResource;
import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Props;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.WindowProperty;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.core.widget.TableView;
import be.ibridge.kettle.trans.step.BaseStepDialog;


/**
 * Displays the meta-data on the Values in a row as well as the Step origin of the Value.
 * 
 * @author Matt
 * @since 19-06-2003
 */
public class StepFieldsDialog extends Dialog
{
	private Label        wlStepname;
	private Text         wStepname;
	private FormData     fdlStepname, fdStepname;
		
	private Label        wlFields;
	private TableView    wFields;
	private FormData     fdlFields, fdFields;

	private Button wEdit, wCancel;
	private Listener lsEdit, lsCancel;

	private Row           input;
	private Shell         shell;
	private Props         props;
	private String        stepname;
	
	private SelectionAdapter lsDef;
	
    /**
     * @deprecated Use CT without <i>log</i> and <i>props</i> parameter
     */
    public StepFieldsDialog(Shell parent, int style, LogWriter log, String stepname, Row input, Props props)
    {
        this(parent, style, stepname, input);
        this.props = props;
    }
    
	public StepFieldsDialog(Shell parent, int style, String stepname, Row input)
	{
			super(parent, style);
			this.stepname=stepname;
            this.input=input;
			props=Props.getInstance();
	}

	public Object open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		props.setLook(shell);

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("StepFieldsDialog.Title"));
		shell.setImage(GUIResource.getInstance().getImageLogoSmall());
		int margin = Const.MARGIN;

		// Filename line
		wlStepname=new Label(shell, SWT.NONE);
		wlStepname.setText(Messages.getString("StepFieldsDialog.Name.Label"));
		props.setLook(wlStepname);
		fdlStepname=new FormData();
		fdlStepname.left = new FormAttachment(0, 0);
		fdlStepname.top  = new FormAttachment(0, margin);
		wlStepname.setLayoutData(fdlStepname);
		wStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.READ_ONLY);
		wStepname.setText(stepname);
		props.setLook(wStepname);
		fdStepname=new FormData();
		fdStepname.left = new FormAttachment(wlStepname, margin);
		fdStepname.top  = new FormAttachment(0, margin);
		fdStepname.right= new FormAttachment(100, 0);
		wStepname.setLayoutData(fdStepname);

		wlFields=new Label(shell, SWT.NONE);
		wlFields.setText(Messages.getString("StepFieldsDialog.Fields.Label"));
		props.setLook(wlFields);
		fdlFields=new FormData();
		fdlFields.left = new FormAttachment(0, 0);
		fdlFields.top  = new FormAttachment(wlStepname, margin);
		wlFields.setLayoutData(fdlFields);
		
		final int FieldsCols=5;
		final int FieldsRows=input.size();
		
		ColumnInfo[] colinf=new ColumnInfo[FieldsCols];
		colinf[0]=new ColumnInfo(Messages.getString("StepFieldsDialog.TableCol.Fieldname"),   ColumnInfo.COLUMN_TYPE_TEXT, false, true );
		colinf[1]=new ColumnInfo(Messages.getString("StepFieldsDialog.TableCol.Type"),        ColumnInfo.COLUMN_TYPE_TEXT, false, true );
		colinf[2]=new ColumnInfo(Messages.getString("StepFieldsDialog.TableCol.Length"),      ColumnInfo.COLUMN_TYPE_TEXT, false, true );
		colinf[3]=new ColumnInfo(Messages.getString("StepFieldsDialog.TableCol.Precision"),   ColumnInfo.COLUMN_TYPE_TEXT, false, true );
		colinf[4]=new ColumnInfo(Messages.getString("StepFieldsDialog.TableCol.Origin"), ColumnInfo.COLUMN_TYPE_TEXT, false, true );
		
		wFields=new TableView(shell, 
						      SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
						      colinf, 
						      FieldsRows,  
						      true, // read-only
						      null,
							  props
						      );
		wFields.optWidth(true);
		
		fdFields=new FormData();
		fdFields.left   = new FormAttachment(0, 0);
		fdFields.top    = new FormAttachment(wlFields, margin);
		fdFields.right  = new FormAttachment(100, 0);
		fdFields.bottom = new FormAttachment(100, -50);
		wFields.setLayoutData(fdFields);

		wEdit=new Button(shell, SWT.PUSH);
		wEdit.setText(Messages.getString("StepFieldsDialog.Buttons.EditOrigin"));
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel"));

		BaseStepDialog.positionBottomButtons(shell, new Button[] { wEdit, wCancel }, margin, wFields);
		
		// Add listeners
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		lsEdit       = new Listener() { public void handleEvent(Event e) { edit();     } };
		
		wCancel.addListener(SWT.Selection, lsCancel);
		wEdit.addListener    (SWT.Selection, lsEdit    );
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { edit(); } };
		
		wStepname.addSelectionListener( lsDef );
				
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		
		wFields.table.addMouseListener(new MouseListener()
        {
            public void mouseDoubleClick(MouseEvent arg0)
            {
                edit();
            }

            public void mouseDown(MouseEvent arg0)
            {
            }

            public void mouseUp(MouseEvent arg0)
            {
            }
        });
		
		getData();
		
		BaseStepDialog.setSize(shell);

		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return stepname;
	}

	public void dispose()
	{
		props.setScreen(new WindowProperty(shell));
		shell.dispose();
	}
	
	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{	
		int i;
		
		for (i=0;i<input.size();i++)
		{
			TableItem item = wFields.table.getItem(i);
			Value v=input.getValue(i);
			if (v.getName()!=null) item.setText(1, v.getName());
			item.setText(2, v.getTypeDesc());
			item.setText(3, v.getLength()<0?"-":""+v.getLength());
			item.setText(4, v.getPrecision()<0?"-":""+v.getPrecision());
			if (v.getOrigin()!=null) item.setText(5, v.getOrigin());
		}
		wFields.optWidth(true);
	}
	
	private void cancel()
	{
		stepname=null;
		dispose();
	}
	
	private void edit()
	{
		int idx=wFields.table.getSelectionIndex();
		if (idx>=0)
		{
			stepname = wFields.table.getItem(idx).getText(5);
		}
		else
		{
			stepname = null;
		}
		
		dispose();
	}
}
