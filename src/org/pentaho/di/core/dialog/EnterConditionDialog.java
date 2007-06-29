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

package org.pentaho.di.core.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Condition;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.gui.GUIResource;
import org.pentaho.di.core.gui.WindowProperty;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.widget.ConditionEditor;
import org.pentaho.di.trans.step.BaseStepDialog;



/**
 * This dialog allows you to enter a condition in a graphical way.
 * 
 * @author Matt
 * @since 29-07-2004
 */
public class EnterConditionDialog extends Dialog 
{
	private Props props;
	
	private Shell     shell;
	private ConditionEditor wCond;
	 
	private Button    wOK;
	private Button    wCancel;
	
	private Condition condition;
	private RowMetaInterface fields;				

	public EnterConditionDialog(Shell parent, int style, RowMetaInterface fields, Condition condition)
	{
		super(parent, style);
		this.props     = Props.getInstance();
		this.fields    = fields;
		this.condition = condition;
	}

	public Condition open() 
	{
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
 		props.setLook(shell);
		shell.setText(Messages.getString("EnterConditionDialog.Title"));
		shell.setImage(GUIResource.getInstance().getImageLogoSmall());
		
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;
		
		shell.setLayout (formLayout);
 		
 		// Condition widget
		wCond = new ConditionEditor(shell, SWT.NONE, condition, props, fields);
 		props.setLook(wCond, Props.WIDGET_STYLE_FIXED);
 		
 		if (!getData()) return null;
 		
 		// Buttons
		wOK = new Button(shell, SWT.PUSH); 
		wOK.setText(Messages.getString("System.Button.OK"));
		
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel"));
	
		FormData fdCond      = new FormData(); 
		
		int margin =  Const.MARGIN*2;

		fdCond.left   = new FormAttachment(0, 0); // To the right of the label
		fdCond.top    = new FormAttachment(0, 0);
		fdCond.right  = new FormAttachment(100, 0);
		fdCond.bottom = new FormAttachment(100, -50);
		wCond.setLayoutData(fdCond);
        
        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, null);
	
		// Add listeners
		wCancel.addListener(SWT.Selection, new Listener ()
			{
				public void handleEvent (Event e) 
				{
					condition=null;
					dispose();
				}
			}
		);

		wOK.addListener(SWT.Selection, new Listener ()
			{
				public void handleEvent (Event e) 
				{
					handleOK();
				}
			}
		);
				

		BaseStepDialog.setSize(shell);

		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) 
		{
			if (!display.readAndDispatch()) display.sleep();
		}
		return condition;
	}
	
	private boolean getData()
	{
		return true;
	}

	public void dispose()
	{
		props.setScreen(new WindowProperty(shell));
		shell.dispose();
	}
	
	public void handleOK()
	{
		if (wCond.getLevel()>0) wCond.goUp();
		else dispose();
	}
	
	public String toString()
	{
		return this.getClass().getName();
	}

}
