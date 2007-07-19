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


package org.pentaho.di.ui.core.database.wizard;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.ui.core.database.wizard.Messages;
import org.pentaho.di.ui.core.PropsUI;


/**
 * 
 * On page one we set the Informix servername 
 * 
 * @author Matt
 * @since  04-apr-2005
 */
public class CreateDatabaseWizardPageInformix extends WizardPage
{
	private Label    wlServername;
	private Text     wServername;
	private FormData fdlServername, fdServername;
	
	private PropsUI props;
	private DatabaseMeta info;
	
	public CreateDatabaseWizardPageInformix(String arg, PropsUI props, DatabaseMeta info)
	{
		super(arg);
		this.props=props;
		this.info = info;
		
		setTitle(Messages.getString("CreateDatabaseWizardPageInformix.DialogTitle")); //$NON-NLS-1$
		setDescription(Messages.getString("CreateDatabaseWizardPageInformix.DialogMessage")); //$NON-NLS-1$
		
		setPageComplete(false);
	}
	
	public void createControl(Composite parent)
	{
		int margin = Const.MARGIN;
		int middle = props.getMiddlePct();
		
		// create the composite to hold the widgets
		Composite composite = new Composite(parent, SWT.NONE);
 		props.setLook(composite);
	    
	    FormLayout compLayout = new FormLayout();
	    compLayout.marginHeight = Const.FORM_MARGIN;
	    compLayout.marginWidth  = Const.FORM_MARGIN;
		composite.setLayout(compLayout);

		wlServername = new Label(composite, SWT.RIGHT);
		wlServername.setText(Messages.getString("CreateDatabaseWizardPageInformix.Servername.Label")); //$NON-NLS-1$
 		props.setLook(wlServername);
		fdlServername = new FormData();
		fdlServername.top    = new FormAttachment(0, 0);
		fdlServername.left   = new FormAttachment(0, 0);
		fdlServername.right  = new FormAttachment(middle,0);
		wlServername.setLayoutData(fdlServername);
		
		wServername = new Text(composite, SWT.SINGLE | SWT.BORDER);
 		props.setLook(wServername);
		fdServername = new FormData();
		fdServername.top     = new FormAttachment(0, 0);
		fdServername.left    = new FormAttachment(middle, margin);
		fdServername.right   = new FormAttachment(100, 0);
		wServername.setLayoutData(fdServername);
		wServername.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent arg0)
			{
				setPageComplete(false);
			}
		});
		
		// set the composite as the control for this page
		setControl(composite);
	}
	
	public boolean canFlipToNextPage()
	{
		String name = wServername.getText()!=null?wServername.getText().length()>0?wServername.getText():null:null;
		if (name==null)
		{
			setErrorMessage(Messages.getString("CreateDatabaseWizardPageInformix.ErrorMessage.ServernameRequired")); //$NON-NLS-1$
			return false;
		}
		else
		{
			getDatabaseInfo();
			setErrorMessage(null);
			setMessage(Messages.getString("CreateDatabaseWizardPageInformix.Message.Next")); //$NON-NLS-1$
			return true;
		}
	}	
	
	public DatabaseMeta getDatabaseInfo()
	{
		if (wServername.getText()!=null && wServername.getText().length()>0) 
		{
			info.setServername(wServername.getText());
		}
		
		return info;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	public IWizardPage getNextPage()
	{
		IWizard wiz = getWizard();
		return wiz.getPage("2"); //$NON-NLS-1$
	}
	
}
