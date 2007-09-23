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

 

package org.pentaho.di.ui.core.dialog;
import org.eclipse.swt.SWT;
import org.pentaho.di.ui.core.gui.GUIResource;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaAndData;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.trans.step.BaseStepDialog;


/**
 * Dialog to enter a Kettle Value
 * 
 * @author Matt
 * @since 01-11-2004
 * 
 */
public class EnterValueDialog extends Dialog
{
	private Display      display; 

	/*
	 * Type of Value: String, Number, Date, Boolean, Integer
	 */
	private Label        wlValueType;
	private CCombo       wValueType;
    private FormData     fdlValueType, fdValueType;

    private Label        wlInputString;
    private Text         wInputString;
    private FormData     fdlInputString, fdInputString;

	private Label        wlFormat;
	private CCombo       wFormat;
    private FormData     fdlFormat, fdFormat;
    
    private Label        wlLength;
    private Text         wLength;
    private FormData     fdlLength, fdLength;

    private Label        wlPrecision;
    private Text         wPrecision;
    private FormData     fdlPrecision, fdPrecision;

	private Button wOK, wCancel, wTest;
	private Listener lsOK, lsCancel, lsTest;

	private Shell  shell;
	private SelectionAdapter lsDef;
	private PropsUI props;
	
    private ValueMetaAndData valueMetaAndData;
    private ValueMetaInterface valueMeta;
    private Object valueData;

	public EnterValueDialog(Shell parent, int style, ValueMetaInterface value, Object data)
	{
		super(parent, style);
		this.props = PropsUI.getInstance();
		this.valueMeta = value;
        this.valueData = data;
	}

	public ValueMetaAndData open()
	{
		Shell parent = getParent();
		display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE );
 		props.setLook(shell);
 		shell.setImage(GUIResource.getInstance().getImageSpoon());

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("EnterValueDialog.Title"));
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;
		
		// Type of value
		wlValueType=new Label(shell, SWT.RIGHT);
		wlValueType.setText(Messages.getString("EnterValueDialog.Type.Label"));
 		props.setLook(wlValueType);
		fdlValueType=new FormData();
		fdlValueType.left = new FormAttachment(0, 0);
		fdlValueType.right= new FormAttachment(middle, -margin);
		fdlValueType.top  = new FormAttachment(0, margin);
		wlValueType.setLayoutData(fdlValueType);
		wValueType=new CCombo(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.READ_ONLY);
		wValueType.setItems(ValueMeta.getTypes());
 		props.setLook(wValueType);
		fdValueType=new FormData();
		fdValueType.left = new FormAttachment(middle, 0);
		fdValueType.top  = new FormAttachment(0, margin);
		fdValueType.right= new FormAttachment(100, -margin);
		wValueType.setLayoutData(fdValueType);
		wValueType.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent arg0)
			{
				setFormats();
			}
		});

		
		// Iconsize line
		wlInputString=new Label(shell, SWT.RIGHT);
		wlInputString.setText(Messages.getString("EnterValueDialog.Value.Label"));
 		props.setLook(wlInputString);
		fdlInputString=new FormData();
		fdlInputString.left = new FormAttachment(0, 0);
		fdlInputString.right= new FormAttachment(middle, -margin);
		fdlInputString.top  = new FormAttachment(wValueType, margin);
		wlInputString.setLayoutData(fdlInputString);
		wInputString=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wInputString);
		fdInputString=new FormData();
		fdInputString.left = new FormAttachment(middle, 0);
		fdInputString.top  = new FormAttachment(wValueType, margin);
		fdInputString.right= new FormAttachment(100, -margin);
		wInputString.setLayoutData(fdInputString);

		// Format mask
		wlFormat=new Label(shell, SWT.RIGHT);
		wlFormat.setText(Messages.getString("EnterValueDialog.ConversionFormat.Label"));
 		props.setLook(wlFormat);
		fdlFormat=new FormData();
		fdlFormat.left = new FormAttachment(0, 0);
		fdlFormat.right= new FormAttachment(middle, -margin);
		fdlFormat.top  = new FormAttachment(wInputString, margin);
		wlFormat.setLayoutData(fdlFormat);
		wFormat=new CCombo(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wFormat);
		fdFormat=new FormData();
		fdFormat.left = new FormAttachment(middle, 0);
		fdFormat.right= new FormAttachment(100, -margin);
		fdFormat.top  = new FormAttachment(wInputString, margin);
		wFormat.setLayoutData(fdFormat);

		// Length line
		wlLength=new Label(shell, SWT.RIGHT);
		wlLength.setText(Messages.getString("EnterValueDialog.Length.Label"));
 		props.setLook(wlLength);
		fdlLength=new FormData();
		fdlLength.left = new FormAttachment(0, 0);
		fdlLength.right= new FormAttachment(middle, -margin);
		fdlLength.top  = new FormAttachment(wFormat, margin);
		wlLength.setLayoutData(fdlLength);
		wLength=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wLength);
		fdLength=new FormData();
		fdLength.left = new FormAttachment(middle, 0);
		fdLength.right= new FormAttachment(100, -margin);
		fdLength.top  = new FormAttachment(wFormat, margin);
		wLength.setLayoutData(fdLength);
		
		// Precision line
		wlPrecision=new Label(shell, SWT.RIGHT);
		wlPrecision.setText(Messages.getString("EnterValueDialog.Precision.Label"));
 		props.setLook(wlPrecision);
		fdlPrecision=new FormData();
		fdlPrecision.left = new FormAttachment(0, 0);
		fdlPrecision.right= new FormAttachment(middle, -margin);
		fdlPrecision.top  = new FormAttachment(wLength, margin);
		wlPrecision.setLayoutData(fdlPrecision);
		wPrecision=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wPrecision);
		fdPrecision=new FormData();
		fdPrecision.left = new FormAttachment(middle, 0);
		fdPrecision.right= new FormAttachment(100, -margin);
		fdPrecision.top  = new FormAttachment(wLength, margin);
		wPrecision.setLayoutData(fdPrecision);



		// Some buttons
		wOK=new Button(shell, SWT.PUSH );
		wOK.setText(Messages.getString("System.Button.OK"));
		wTest=new Button(shell, SWT.PUSH );
		wTest.setText(Messages.getString("System.Button.Test"));
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel"));

		BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wTest, wCancel }, margin, wPrecision);

        // Add listeners
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		lsTest     = new Listener() { public void handleEvent(Event e) { test();   } };
		
		wCancel.addListener(SWT.Selection, lsCancel );
		wOK.addListener    (SWT.Selection, lsOK     );
		wTest.addListener  (SWT.Selection, lsTest   );
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		wInputString.addSelectionListener  (lsDef);
		wLength.addSelectionListener       (lsDef);
		wPrecision.addSelectionListener    (lsDef);
		
		// Detect [X] or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );


		getData();

		BaseStepDialog.setSize(shell);

		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return valueMetaAndData;
	}

	public void dispose()
	{
		shell.dispose();
	}
	
	public void getData()
	{
		wValueType.setText(valueMeta.getTypeDesc());
		try
        {
            if (valueMeta.getString(valueData)!=null) wInputString.setText(valueMeta.getString(valueData));
        }
        catch (KettleValueException e)
        {
            wInputString.setText(valueMeta.toString());
        }
		setFormats();
		
		if (valueMeta.isNumber())
		{
			wFormat.setText(Const.getNumberFormats()[0]);
		}
		if (valueMeta.isDate())
		{
			wFormat.setText(Const.getDateFormats()[0]);
		}
		
		wLength.setText(Integer.toString(valueMeta.getLength()));
		wPrecision.setText(Integer.toString(valueMeta.getPrecision()));
		
		wInputString.setFocus();
		wInputString.selectAll();
	}
	
	public void setFormats()
	{
		wFormat.removeAll();
		int valtype = ValueMeta.getType( wValueType.getText() );
		switch(valtype)
		{
		case ValueMetaInterface.TYPE_NUMBER:
			
			for (int i=0;i<Const.getNumberFormats().length;i++) 
				wFormat.add(Const.getNumberFormats()[i]);
			break;
        case ValueMetaInterface.TYPE_DATE:
			for (int i=0;i<Const.getDateFormats().length;i++) 
				wFormat.add(Const.getDateFormats()[i]);
			break;
        case ValueMetaInterface.TYPE_STRING  : 
        case ValueMetaInterface.TYPE_BOOLEAN : 
        case ValueMetaInterface.TYPE_INTEGER : 
		default                       : break;
		}
	}
	
	private void cancel()
	{
		props.setScreen(new WindowProperty(shell));
		valueMeta=null;
		dispose();
	}
	
	private ValueMetaAndData getValue(String valuename) throws KettleValueException
	{
		int valtype = ValueMeta.getType(wValueType.getText()); 
		ValueMetaAndData val = new ValueMetaAndData(valuename, wInputString.getText());

		ValueMetaInterface valueMeta = val.getValueMeta(); 
		Object valueData = val.getValueData();
		
		valueMeta.setType( valtype );
		valueMeta.setConversionMask( wFormat.getText() );
		valueMeta.setLength( Const.toInt( wLength.getText(), -1) );
		valueMeta.setPrecision( Const.toInt( wPrecision.getText(), -1) );
        
        ValueMetaInterface stringValueMeta = valueMeta.clone();
        stringValueMeta.setType(ValueMetaInterface.TYPE_STRING);
        
        Object targetData = valueMeta.convertData(stringValueMeta, valueData);
        val.setValueData(targetData);
		
		return val;
	}
	
	private void ok()
	{
        try
        {
            valueMetaAndData = getValue(valueMeta.getName()); // Keep the same name...
            dispose();
        }
        catch (KettleValueException e) 
        {
            new ErrorDialog(shell, "Error", "There was a conversion error: ", e);
        }
	}
	
	/**
	 * Test the entered value
	 *
	 */
	public void test()
	{
        try
        {
    		ValueMetaAndData v = getValue(valueMeta.getName());
    		MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
    
    		StringBuffer result = new StringBuffer();
    		result.append(Const.CR).append(Const.CR).append("    ").append(v.toString());
    		result.append(Const.CR).append("    ").append(v.toStringMeta());
    
    		mb.setMessage(Messages.getString("EnterValueDialog.TestResult.Message", result.toString()));
    		mb.setText(Messages.getString("EnterValueDialog.TestResult.Title"));
    		mb.open();
        }
        catch(KettleValueException e)
        {
            new ErrorDialog(shell, "Error", "There was an error during data type conversion: ", e);
        }
	}
}
