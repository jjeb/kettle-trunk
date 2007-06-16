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

package org.pentaho.di.spoon.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Props;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.dialog.SelectDirectoryDialog;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.LogWriter;



/**
 * 
 * On page one we select the name of the target transformation and the directory.
 * 
 * @author Matt
 * @since  29-mar-05
 */
public class CopyTableWizardPage3 extends WizardPage
{
	private Label        wlTransname;
	private Text         wTransname;
    private FormData     fdlTransname, fdTransname;

    private Label        wlDirectory;
	private Text         wDirectory;
	private Button       wbDirectory;
    private FormData     fdlDirectory, fdbDirectory, fdDirectory;    

	private Props props;
	private Repository rep;
	private RepositoryDirectory directory;
	private Shell shell;

    /** @deprecated */
    public CopyTableWizardPage3(String arg, LogWriter log, Props props, Repository rep)
	{
		super(arg);
		this.props=props;
		this.rep = rep;
		
		setTitle(Messages.getString("CopyTableWizardPage3.Dialog.Title")); //$NON-NLS-1$
		setDescription(Messages.getString("CopyTableWizardPage3.Dialog.Description")); //$NON-NLS-1$
		
		setPageComplete(false);
	}
    
    public CopyTableWizardPage3(String arg, Repository rep)
    {
        super(arg);
        this.props=Props.getInstance();
        this.rep = rep;
        
        
        setTitle(Messages.getString("CopyTableWizardPage3.Dialog.Title")); //$NON-NLS-1$
        setDescription(Messages.getString("CopyTableWizardPage3.Dialog.Description")); //$NON-NLS-1$
        
        setPageComplete(false);
    }
	
	public void createControl(Composite parent)
	{
		shell = parent.getShell();
				
		int margin = Const.MARGIN;
		int middle = props.getMiddlePct();
		
		ModifyListener lsMod = new ModifyListener()
		{
			public void modifyText(ModifyEvent arg0)
			{
				setPageComplete(canFlipToNextPage());
			}
		};
		
		// create the composite to hold the widgets
		Composite composite = new Composite(parent, SWT.NONE);
 		props.setLook(composite);
	    
	    FormLayout compLayout = new FormLayout();
	    compLayout.marginHeight = Const.FORM_MARGIN;
	    compLayout.marginWidth  = Const.FORM_MARGIN;
		composite.setLayout(compLayout);

		// Job name:
		wlTransname=new Label(composite, SWT.RIGHT);
		wlTransname.setText(Messages.getString("CopyTableWizardPage3.Dialog.JobName.Label")); //$NON-NLS-1$
 		props.setLook(wlTransname);
		fdlTransname=new FormData();
		fdlTransname.left = new FormAttachment(0, 0);
		fdlTransname.right= new FormAttachment(middle, -margin);
		fdlTransname.top  = new FormAttachment(0, margin);
		wlTransname.setLayoutData(fdlTransname);
		wTransname=new Text(composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wTransname);
		wTransname.addModifyListener(lsMod);
		fdTransname=new FormData();
		fdTransname.left = new FormAttachment(middle, 0);
		fdTransname.top  = new FormAttachment(0, margin);
		fdTransname.right= new FormAttachment(100, 0);
		wTransname.setLayoutData(fdTransname);
		
		// Directory:
		wlDirectory=new Label(composite, SWT.RIGHT);
		wlDirectory.setText(Messages.getString("CopyTableWizardPage3.Dialog.Directory.Label")); //$NON-NLS-1$
 		props.setLook(wlDirectory);
		fdlDirectory=new FormData();
		fdlDirectory.left = new FormAttachment(0, 0);
		fdlDirectory.right= new FormAttachment(middle, -margin);
		fdlDirectory.top  = new FormAttachment(wTransname, margin);
		wlDirectory.setLayoutData(fdlDirectory);

		wbDirectory=new Button(composite, SWT.PUSH);
		wbDirectory.setText(Messages.getString("CopyTableWizardPage3.Dialog.DirectoryButton.Label")); //$NON-NLS-1$
 		props.setLook(wbDirectory);
		fdbDirectory=new FormData();
		fdbDirectory.right= new FormAttachment(100, 0);
		fdbDirectory.top  = new FormAttachment(wTransname, margin);
		wbDirectory.setLayoutData(fdbDirectory);
		wbDirectory.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent arg0)
			{
				SelectDirectoryDialog sdd = new SelectDirectoryDialog(shell, SWT.NONE, rep);
				directory = sdd.open();
				if (directory!=null)
				{
					wDirectory.setText(directory.getPath());
					setPageComplete(canFlipToNextPage());
				}
			}
		});

		wDirectory=new Text(composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wDirectory);
		wDirectory.setEditable(false);
		fdDirectory=new FormData();
		fdDirectory.left = new FormAttachment(middle, 0);
		fdDirectory.top  = new FormAttachment(wTransname, margin);
		fdDirectory.right= new FormAttachment(wbDirectory, 0);
		wDirectory.setLayoutData(fdDirectory);
		
		// set the composite as the control for this page
		setControl(composite);
	}
		
	public boolean canFlipToNextPage()
	{
		return false;  
	}	
	
	public String getTransformationName()
	{
		String transname = wTransname.getText();
		if (transname!=null && transname.length()==0) transname=null;
		
		return transname;
	}

	/**
	 * @return Returns the directory.
	 */
	public RepositoryDirectory getDirectory()
	{
		return directory;
	}
	
	public boolean canFinish()
	{
		return getTransformationName()!=null && getDirectory()!=null;
	}
}
