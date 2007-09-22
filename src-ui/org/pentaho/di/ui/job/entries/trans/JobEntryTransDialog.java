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

package org.pentaho.di.ui.job.entries.trans;

import java.io.IOException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entries.trans.Messages;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.job.dialog.JobDialog;
import org.pentaho.di.ui.job.entry.JobEntryDialog;
import org.pentaho.di.ui.repository.dialog.SelectObjectDialog;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.vfs.ui.VfsFileChooserDialog;

/**
 * This dialog allows you to edit the transformation job entry (JobEntryTrans)
 * 
 * @author Matt
 * @since 19-06-2003
 */
public class JobEntryTransDialog extends JobEntryDialog implements JobEntryDialogInterface
{
	private static final String[] FILE_FILTERNAMES = new String[] {
			Messages.getString("JobTrans.Fileformat.Kettle"), Messages.getString("JobTrans.Fileformat.XML"),
			Messages.getString("JobTrans.Fileformat.All") };

	private Label wlName;

	private Text wName;

	private FormData fdlName, fdName;

	private Label wlTransname;

	private Button wbTransname;

	private TextVar wTransname;

	private FormData fdlTransname, fdbTransname, fdTransname;

	private Label wlDirectory;

	private Text wDirectory;

	private FormData fdlDirectory, fdDirectory;

	private Label wlFilename;

	private Button wbFilename;

	private TextVar wFilename;

	private FormData fdlFilename, fdbFilename, fdFilename;

	private Group wLogging;

	private FormData fdLogging;

	private Label wlSetLogfile;

	private Button wSetLogfile;

	private FormData fdlSetLogfile, fdSetLogfile;

	private Label wlLogfile;

	private TextVar wLogfile;

	private FormData fdlLogfile, fdLogfile;

	private Label wlLogext;

	private TextVar wLogext;

	private FormData fdlLogext, fdLogext;

	private Label wlAddDate;

	private Button wAddDate;

	private FormData fdlAddDate, fdAddDate;

	private Label wlAddTime;

	private Button wAddTime;

	private FormData fdlAddTime, fdAddTime;

	private Label wlLoglevel;

	private CCombo wLoglevel;

	private FormData fdlLoglevel, fdLoglevel;

	private Label wlPrevious;

	private Button wPrevious;

	private FormData fdlPrevious, fdPrevious;

	private Label wlEveryRow;

	private Button wEveryRow;

	private FormData fdlEveryRow, fdEveryRow;

	private Label wlClearRows;

	private Button wClearRows;

	private FormData fdlClearRows, fdClearRows;

	private Label wlClearFiles;

	private Button wClearFiles;

	private FormData fdlClearFiles, fdClearFiles;

	private Label wlCluster;

	private Button wCluster;

	private FormData fdlCluster, fdCluster;

	private Label wlFields;

	private TableView wFields;

	private FormData fdlFields, fdFields;

	private Button wOK, wCancel;

	private Listener lsOK, lsCancel;

	private Shell shell;

	private SelectionAdapter lsDef;

	private JobEntryTrans jobEntry;

	private boolean backupChanged;

	private Display display;

	public JobEntryTransDialog(Shell parent, JobEntryInterface jobEntryInt, Repository rep, JobMeta jobMeta)
	{
		super(parent, jobEntryInt, rep, jobMeta);
		jobEntry = (JobEntryTrans) jobEntryInt;
	}

	public JobEntryInterface open()
	{
		Shell parent = getParent();
		display = parent.getDisplay();

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
		backupChanged = jobEntry.hasChanged();

		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("JobTrans.Header"));

		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Name line
		wlName = new Label(shell, SWT.RIGHT);
		wlName.setText(Messages.getString("JobTrans.JobStep.Label"));
		props.setLook(wlName);
		fdlName = new FormData();
		fdlName.left = new FormAttachment(0, 0);
		fdlName.top = new FormAttachment(0, 0);
		fdlName.right = new FormAttachment(middle, 0);
		wlName.setLayoutData(fdlName);

		wName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wName);
		wName.addModifyListener(lsMod);
		fdName = new FormData();
		fdName.top = new FormAttachment(0, 0);
		fdName.left = new FormAttachment(middle, 0);
		fdName.right = new FormAttachment(100, 0);
		wName.setLayoutData(fdName);

		// Transname line
		wlTransname = new Label(shell, SWT.RIGHT);
		wlTransname.setText(Messages.getString("JobTrans.NameOfTransformation.Label"));
		props.setLook(wlTransname);
		fdlTransname = new FormData();
		fdlTransname.top = new FormAttachment(wName, margin * 2);
		fdlTransname.left = new FormAttachment(0, 0);
		fdlTransname.right = new FormAttachment(middle, 0);
		wlTransname.setLayoutData(fdlTransname);

		wbTransname = new Button(shell, SWT.PUSH | SWT.CENTER);
		props.setLook(wbTransname);
		wbTransname.setText(Messages.getString("JobTrans.Browse.Label"));
		fdbTransname = new FormData();
		fdbTransname.top = new FormAttachment(wName, margin * 2);
		fdbTransname.right = new FormAttachment(100, 0);
		wbTransname.setLayoutData(fdbTransname);
		wbTransname.setEnabled(rep != null);

		wTransname = new TextVar(jobMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wTransname);
		wTransname.addModifyListener(lsMod);
		fdTransname = new FormData();
		fdTransname.top = new FormAttachment(wName, margin * 2);
		fdTransname.left = new FormAttachment(middle, 0);
		fdTransname.right = new FormAttachment(wbTransname, -margin);
		wTransname.setLayoutData(fdTransname);

		// Directory line
		wlDirectory = new Label(shell, SWT.RIGHT);
		wlDirectory.setText(Messages.getString("JobTrans.RepositoryDir.Label"));
		props.setLook(wlDirectory);
		fdlDirectory = new FormData();
		fdlDirectory.top = new FormAttachment(wTransname, margin * 2);
		fdlDirectory.left = new FormAttachment(0, 0);
		fdlDirectory.right = new FormAttachment(middle, 0);
		wlDirectory.setLayoutData(fdlDirectory);

		wDirectory = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wDirectory);
		wDirectory.addModifyListener(lsMod);
		fdDirectory = new FormData();
		fdDirectory.top = new FormAttachment(wTransname, margin * 2);
		fdDirectory.left = new FormAttachment(middle, 0);
		fdDirectory.right = new FormAttachment(100, 0);
		wDirectory.setLayoutData(fdDirectory);
		wDirectory.setEditable(false);

		// Filename line
		wlFilename = new Label(shell, SWT.RIGHT);
		wlFilename.setText(Messages.getString("JobTrans.TransformationFile.Label"));
		props.setLook(wlFilename);
		fdlFilename = new FormData();
		fdlFilename.top = new FormAttachment(wDirectory, margin);
		fdlFilename.left = new FormAttachment(0, 0);
		fdlFilename.right = new FormAttachment(middle, 0);
		wlFilename.setLayoutData(fdlFilename);

		wbFilename = new Button(shell, SWT.PUSH | SWT.CENTER);
		props.setLook(wbFilename);
		wbFilename.setText(Messages.getString("JobTrans.Browse.Label"));
		fdbFilename = new FormData();
		fdbFilename.top = new FormAttachment(wDirectory, margin);
		fdbFilename.right = new FormAttachment(100, 0);
		wbFilename.setLayoutData(fdbFilename);

		wFilename = new TextVar(jobMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		props.setLook(wFilename);
		wFilename.addModifyListener(lsMod);
		fdFilename = new FormData();
		fdFilename.top = new FormAttachment(wDirectory, margin);
		fdFilename.left = new FormAttachment(middle, 0);
		fdFilename.right = new FormAttachment(wbFilename, -margin);
		wFilename.setLayoutData(fdFilename);

		// logging grouping?
		// ////////////////////////
		// START OF LOGGING GROUP///
		// /
		wLogging = new Group(shell, SWT.SHADOW_NONE);
		props.setLook(wLogging);
		wLogging.setText(Messages.getString("JobTrans.LogSettings.Group.Label"));

		FormLayout groupLayout = new FormLayout();
		groupLayout.marginWidth = 10;
		groupLayout.marginHeight = 10;

		wLogging.setLayout(groupLayout);

		// Set the logfile?
		wlSetLogfile = new Label(wLogging, SWT.RIGHT);
		wlSetLogfile.setText(Messages.getString("JobTrans.Specify.Logfile.Label"));
		props.setLook(wlSetLogfile);
		fdlSetLogfile = new FormData();
		fdlSetLogfile.left = new FormAttachment(0, 0);
		fdlSetLogfile.top = new FormAttachment(0, margin);
		fdlSetLogfile.right = new FormAttachment(middle, -margin);
		wlSetLogfile.setLayoutData(fdlSetLogfile);
		wSetLogfile = new Button(wLogging, SWT.CHECK);
		props.setLook(wSetLogfile);
		fdSetLogfile = new FormData();
		fdSetLogfile.left = new FormAttachment(middle, 0);
		fdSetLogfile.top = new FormAttachment(0, margin);
		fdSetLogfile.right = new FormAttachment(100, 0);
		wSetLogfile.setLayoutData(fdSetLogfile);
		wSetLogfile.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				setActive();
			}
		});

		// Set the logfile path + base-name
		wlLogfile = new Label(wLogging, SWT.RIGHT);
		wlLogfile.setText(Messages.getString("JobTrans.NameOfLogfile.Label"));
		props.setLook(wlLogfile);
		fdlLogfile = new FormData();
		fdlLogfile.left = new FormAttachment(0, 0);
		fdlLogfile.top = new FormAttachment(wlSetLogfile, margin);
		fdlLogfile.right = new FormAttachment(middle, 0);
		wlLogfile.setLayoutData(fdlLogfile);
		wLogfile = new TextVar(jobMeta, wLogging, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wLogfile.setText("");
		props.setLook(wLogfile);
		fdLogfile = new FormData();
		fdLogfile.left = new FormAttachment(middle, 0);
		fdLogfile.top = new FormAttachment(wlSetLogfile, margin);
		fdLogfile.right = new FormAttachment(100, 0);
		wLogfile.setLayoutData(fdLogfile);

		// Set the logfile filename extention
		wlLogext = new Label(wLogging, SWT.RIGHT);
		wlLogext.setText(Messages.getString("JobTrans.LogfileExtension.Label"));
		props.setLook(wlLogext);
		fdlLogext = new FormData();
		fdlLogext.left = new FormAttachment(0, 0);
		fdlLogext.top = new FormAttachment(wLogfile, margin);
		fdlLogext.right = new FormAttachment(middle, 0);
		wlLogext.setLayoutData(fdlLogext);
		wLogext = new TextVar(jobMeta, wLogging, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wLogext.setText("");
		props.setLook(wLogext);
		fdLogext = new FormData();
		fdLogext.left = new FormAttachment(middle, 0);
		fdLogext.top = new FormAttachment(wLogfile, margin);
		fdLogext.right = new FormAttachment(100, 0);
		wLogext.setLayoutData(fdLogext);

		// Add date to logfile name?
		wlAddDate = new Label(wLogging, SWT.RIGHT);
		wlAddDate.setText(Messages.getString("JobTrans.Logfile.IncludeDate.Label"));
		props.setLook(wlAddDate);
		fdlAddDate = new FormData();
		fdlAddDate.left = new FormAttachment(0, 0);
		fdlAddDate.top = new FormAttachment(wLogext, margin);
		fdlAddDate.right = new FormAttachment(middle, -margin);
		wlAddDate.setLayoutData(fdlAddDate);
		wAddDate = new Button(wLogging, SWT.CHECK);
		props.setLook(wAddDate);
		fdAddDate = new FormData();
		fdAddDate.left = new FormAttachment(middle, 0);
		fdAddDate.top = new FormAttachment(wLogext, margin);
		fdAddDate.right = new FormAttachment(100, 0);
		wAddDate.setLayoutData(fdAddDate);

		// Add time to logfile name?
		wlAddTime = new Label(wLogging, SWT.RIGHT);
		wlAddTime.setText(Messages.getString("JobTrans.Logfile.IncludeTime.Label"));
		props.setLook(wlAddTime);
		fdlAddTime = new FormData();
		fdlAddTime.left = new FormAttachment(0, 0);
		fdlAddTime.top = new FormAttachment(wlAddDate, margin);
		fdlAddTime.right = new FormAttachment(middle, -margin);
		wlAddTime.setLayoutData(fdlAddTime);
		wAddTime = new Button(wLogging, SWT.CHECK);
		props.setLook(wAddTime);
		fdAddTime = new FormData();
		fdAddTime.left = new FormAttachment(middle, 0);
		fdAddTime.top = new FormAttachment(wlAddDate, margin);
		fdAddTime.right = new FormAttachment(100, 0);
		wAddTime.setLayoutData(fdAddTime);

		wlLoglevel = new Label(wLogging, SWT.RIGHT);
		wlLoglevel.setText(Messages.getString("JobTrans.Loglevel.Label"));
		props.setLook(wlLoglevel);
		fdlLoglevel = new FormData();
		fdlLoglevel.left = new FormAttachment(0, 0);
		fdlLoglevel.right = new FormAttachment(middle, -margin);
		fdlLoglevel.top = new FormAttachment(wlAddTime, margin);
		wlLoglevel.setLayoutData(fdlLoglevel);
		wLoglevel = new CCombo(wLogging, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
		for (int i = 0; i < LogWriter.logLevelDescription.length; i++)
			wLoglevel.add(LogWriter.logLevelDescription[i]);
		wLoglevel.select(jobEntry.loglevel);

		props.setLook(wLoglevel);
		fdLoglevel = new FormData();
		fdLoglevel.left = new FormAttachment(middle, 0);
		fdLoglevel.top = new FormAttachment(wlAddTime, margin);
		fdLoglevel.right = new FormAttachment(100, 0);
		wLoglevel.setLayoutData(fdLoglevel);

		fdLogging = new FormData();
		fdLogging.left = new FormAttachment(0, margin);
		fdLogging.top = new FormAttachment(wbFilename, margin);
		fdLogging.right = new FormAttachment(100, -margin);
		wLogging.setLayoutData(fdLogging);
		// ///////////////////////////////////////////////////////////
		// / END OF LOGGING GROUP
		// ///////////////////////////////////////////////////////////

		wlPrevious = new Label(shell, SWT.RIGHT);
		wlPrevious.setText(Messages.getString("JobTrans.Previous.Label"));
		props.setLook(wlPrevious);
		fdlPrevious = new FormData();
		fdlPrevious.left = new FormAttachment(0, 0);
		fdlPrevious.top = new FormAttachment(wLogging, margin * 3);
		fdlPrevious.right = new FormAttachment(middle, -margin);
		wlPrevious.setLayoutData(fdlPrevious);
		wPrevious = new Button(shell, SWT.CHECK);
		props.setLook(wPrevious);
		wPrevious.setSelection(jobEntry.argFromPrevious);
		wPrevious.setToolTipText(Messages.getString("JobTrans.Previous.Tooltip"));
		fdPrevious = new FormData();
		fdPrevious.left = new FormAttachment(middle, 0);
		fdPrevious.top = new FormAttachment(wLogging, margin * 3);
		fdPrevious.right = new FormAttachment(100, 0);
		wPrevious.setLayoutData(fdPrevious);
		wPrevious.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				wlFields.setEnabled(!jobEntry.argFromPrevious);
				wFields.setEnabled(!jobEntry.argFromPrevious);
			}
		});

		wlEveryRow = new Label(shell, SWT.RIGHT);
		wlEveryRow.setText(Messages.getString("JobTrans.ExecForEveryInputRow.Label"));
		props.setLook(wlEveryRow);
		fdlEveryRow = new FormData();
		fdlEveryRow.left = new FormAttachment(0, 0);
		fdlEveryRow.top = new FormAttachment(wPrevious, margin);
		fdlEveryRow.right = new FormAttachment(middle, -margin);
		wlEveryRow.setLayoutData(fdlEveryRow);
		wEveryRow = new Button(shell, SWT.CHECK);
		props.setLook(wEveryRow);
		wEveryRow.setToolTipText(Messages.getString("JobTrans.ExecForEveryInputRow.Tooltip"));
		fdEveryRow = new FormData();
		fdEveryRow.left = new FormAttachment(middle, 0);
		fdEveryRow.top = new FormAttachment(wPrevious, margin);
		fdEveryRow.right = new FormAttachment(100, 0);
		wEveryRow.setLayoutData(fdEveryRow);

		// Clear the result rows before executing the transformation?
		//
		wlClearRows = new Label(shell, SWT.RIGHT);
		wlClearRows.setText(Messages.getString("JobTrans.ClearResultList.Label"));
		props.setLook(wlClearRows);
		fdlClearRows = new FormData();
		fdlClearRows.left = new FormAttachment(0, 0);
		fdlClearRows.top = new FormAttachment(wEveryRow, margin);
		fdlClearRows.right = new FormAttachment(middle, -margin);
		wlClearRows.setLayoutData(fdlClearRows);
		wClearRows = new Button(shell, SWT.CHECK);
		props.setLook(wClearRows);
		fdClearRows = new FormData();
		fdClearRows.left = new FormAttachment(middle, 0);
		fdClearRows.top = new FormAttachment(wEveryRow, margin);
		fdClearRows.right = new FormAttachment(100, 0);
		wClearRows.setLayoutData(fdClearRows);

		// Clear the result files before executing the transformation?
		//
		wlClearFiles = new Label(shell, SWT.RIGHT);
		wlClearFiles.setText(Messages.getString("JobTrans.ClearResultFiles.Label"));
		props.setLook(wlClearFiles);
		fdlClearFiles = new FormData();
		fdlClearFiles.left = new FormAttachment(0, 0);
		fdlClearFiles.top = new FormAttachment(wClearRows, margin);
		fdlClearFiles.right = new FormAttachment(middle, -margin);
		wlClearFiles.setLayoutData(fdlClearFiles);
		wClearFiles = new Button(shell, SWT.CHECK);
		props.setLook(wClearFiles);
		fdClearFiles = new FormData();
		fdClearFiles.left = new FormAttachment(middle, 0);
		fdClearFiles.top = new FormAttachment(wClearRows, margin);
		fdClearFiles.right = new FormAttachment(100, 0);
		wClearFiles.setLayoutData(fdClearFiles);

		// Clear the result rows before executing the transformation?
		//
		wlCluster = new Label(shell, SWT.RIGHT);
		wlCluster.setText(Messages.getString("JobTrans.RunTransInCluster.Label"));
		props.setLook(wlCluster);
		fdlCluster = new FormData();
		fdlCluster.left = new FormAttachment(0, 0);
		fdlCluster.top = new FormAttachment(wClearFiles, margin);
		fdlCluster.right = new FormAttachment(middle, -margin);
		wlCluster.setLayoutData(fdlCluster);
		wCluster = new Button(shell, SWT.CHECK);
		props.setLook(wCluster);
		fdCluster = new FormData();
		fdCluster.left = new FormAttachment(middle, 0);
		fdCluster.top = new FormAttachment(wClearFiles, margin);
		fdCluster.right = new FormAttachment(100, 0);
		wCluster.setLayoutData(fdCluster);

		wlFields = new Label(shell, SWT.NONE);
		wlFields.setText(Messages.getString("JobTrans.Fields.Label"));
		props.setLook(wlFields);
		fdlFields = new FormData();
		fdlFields.left = new FormAttachment(0, 0);
		fdlFields.top = new FormAttachment(wCluster, margin);
		wlFields.setLayoutData(fdlFields);

		final int FieldsCols = 1;
		int rows = jobEntry.arguments == null ? 1 : (jobEntry.arguments.length == 0 ? 0
				: jobEntry.arguments.length);
		final int FieldsRows = rows;

		ColumnInfo[] colinf = new ColumnInfo[FieldsCols];
		colinf[0] = new ColumnInfo(Messages.getString("JobTrans.Fields.Argument.Label"),
				ColumnInfo.COLUMN_TYPE_TEXT, false);
		colinf[0].setUsingVariables(true);

		wFields = new TableView(jobMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf,
				FieldsRows, lsMod, props);

		fdFields = new FormData();
		fdFields.left = new FormAttachment(0, 0);
		fdFields.top = new FormAttachment(wlFields, margin);
		fdFields.right = new FormAttachment(100, 0);
		fdFields.bottom = new FormAttachment(100, -50);
		wFields.setLayoutData(fdFields);

		wlFields.setEnabled(!jobEntry.argFromPrevious);
		wFields.setEnabled(!jobEntry.argFromPrevious);

		// Some buttons
		wOK = new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK"));
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel"));

		BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, wFields);

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

		wOK.addListener(SWT.Selection, lsOK);
		wCancel.addListener(SWT.Selection, lsCancel);

		lsDef = new SelectionAdapter()
		{
			public void widgetDefaultSelected(SelectionEvent e)
			{
				ok();
			}
		};
		wName.addSelectionListener(lsDef);
		wFilename.addSelectionListener(lsDef);

		wbTransname.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (rep != null)
				{
					SelectObjectDialog sod = new SelectObjectDialog(shell, rep, true, false);
					String transname = sod.open();
					if (transname != null)
					{
						wTransname.setText(transname);
						wDirectory.setText(sod.getDirectory().getPath());
						// Copy it to the job entry name too...
						wName.setText(wTransname.getText());
					}
				}
			}
		});

		wbFilename.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				FileObject fileName = null;

				try
				{
					String curFile = wFilename.getText();

					if (curFile.trim().length() > 0)
						fileName = VFS.getManager().resolveFile(
								jobMeta.environmentSubstitute(wFilename.getText()));
					else
						fileName = VFS.getManager().resolveFile(Const.USER_HOME_DIRECTORY);

				} catch (IOException ex)
				{
					try
					{
						fileName = VFS.getManager().resolveFile(Const.USER_HOME_DIRECTORY);
					} catch (IOException iex)
					{
						// this should not happen
						throw new RuntimeException(iex);
					}
				}

				try
				{
					VfsFileChooserDialog dialog = new VfsFileChooserDialog(fileName.getParent(), fileName);
					FileObject lroot = dialog.open(shell, null, new String[] { "*.ktr;*.xml", "*.xml", "*" }, //$NON-NLS-1$
							FILE_FILTERNAMES, VfsFileChooserDialog.VFS_DIALOG_OPEN_FILE); //$NON-NLS-1$

					if (lroot == null) {
					  return;
					}
					String selected = lroot.getURL().toString();

					wFilename.setText(lroot != null ? selected : Const.EMPTY_STRING);

					TransMeta transMeta = new TransMeta(wFilename.getText());
					if (transMeta.getName() != null)
						wName.setText(transMeta.getName());
					else
						wName.setText(selected);
					
				} catch (Exception ke)
				{
					MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
					mb.setText(Messages.getString("JobTrans.ErrorReadingTransformation.Text"));
					mb.setMessage(Messages.getString("JobTrans.ErrorReadingTransformation.Text", wFilename
							.getText(), ke.getMessage()));
					mb.open();
				}

			}
		});

		// Detect [X] or ALT-F4 or something that kills this window...
		shell.addShellListener(new ShellAdapter()
		{
			public void shellClosed(ShellEvent e)
			{
				cancel();
			}
		});

		getData();
		setActive();

		BaseStepDialog.setSize(shell);

		shell.open();
		props.setDialogSize(shell, "JobTransDialogSize");
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

	public void setActive()
	{
		wlLogfile.setEnabled(wSetLogfile.getSelection());
		wLogfile.setEnabled(wSetLogfile.getSelection());

		wlLogext.setEnabled(wSetLogfile.getSelection());
		wLogext.setEnabled(wSetLogfile.getSelection());

		wlAddDate.setEnabled(wSetLogfile.getSelection());
		wAddDate.setEnabled(wSetLogfile.getSelection());

		wlAddTime.setEnabled(wSetLogfile.getSelection());
		wAddTime.setEnabled(wSetLogfile.getSelection());

		wlLoglevel.setEnabled(wSetLogfile.getSelection());
		wLoglevel.setEnabled(wSetLogfile.getSelection());

		/*
		 * if (jobEntry.setLogfile) {
		 * wLoglevel.setForeground(display.getSystemColor(SWT.COLOR_BLACK)); }
		 * else {
		 * wLoglevel.setForeground(display.getSystemColor(SWT.COLOR_GRAY)); }
		 */
	}

	public void getData()
	{
		if (jobEntry.getDirectory() != null)
		{
			wDirectory.setText(jobEntry.getDirectory().getPath());
		} else
		{
			if (jobEntry.getDirectoryPath() != null)
				wDirectory.setText(jobEntry.getDirectoryPath());
		}
		if (jobEntry.getName() != null)
			wName.setText(jobEntry.getName());
		if (jobEntry.getTransname() != null)
			wTransname.setText(jobEntry.getTransname());
		if (jobEntry.getFilename() != null)
			wFilename.setText(jobEntry.getFilename());
		if (jobEntry.arguments != null)
		{
			for (int i = 0; i < jobEntry.arguments.length; i++)
			{
				TableItem ti = wFields.table.getItem(i);
				if (jobEntry.arguments[i] != null)
					ti.setText(1, jobEntry.arguments[i]);
			}
			wFields.setRowNums();
			wFields.optWidth(true);
		}
		if (jobEntry.logfile != null)
			wLogfile.setText(jobEntry.logfile);
		if (jobEntry.logext != null)
			wLogext.setText(jobEntry.logext);

		wPrevious.setSelection(jobEntry.argFromPrevious);
		wEveryRow.setSelection(jobEntry.execPerRow);
		wSetLogfile.setSelection(jobEntry.setLogfile);
		wAddDate.setSelection(jobEntry.addDate);
		wAddTime.setSelection(jobEntry.addTime);
		wClearRows.setSelection(jobEntry.clearResultRows);
		wClearFiles.setSelection(jobEntry.clearResultFiles);
		wCluster.setSelection(jobEntry.isClustering());

		wLoglevel.select(jobEntry.loglevel);
	}

	private void cancel()
	{
		jobEntry.setChanged(backupChanged);

		jobEntry = null;
		dispose();
	}

	private void ok()
	{
		jobEntry.setTransname(wTransname.getText());
		jobEntry.setFileName(wFilename.getText());
		jobEntry.setName(wName.getText());
		if (rep != null)
			jobEntry.setDirectory(rep.getDirectoryTree().findDirectory(wDirectory.getText()));

		int nritems = wFields.nrNonEmpty();
		int nr = 0;
		for (int i = 0; i < nritems; i++)
		{
			String arg = wFields.getNonEmpty(i).getText(1);
			if (arg != null && arg.length() != 0)
				nr++;
		}
		jobEntry.arguments = new String[nr];
		nr = 0;
		for (int i = 0; i < nritems; i++)
		{
			String arg = wFields.getNonEmpty(i).getText(1);
			if (arg != null && arg.length() != 0)
			{
				jobEntry.arguments[nr] = arg;
				nr++;
			}
		}
		jobEntry.logfile = wLogfile.getText();
		jobEntry.logext = wLogext.getText();
		jobEntry.loglevel = wLoglevel.getSelectionIndex();

		jobEntry.argFromPrevious = wPrevious.getSelection();
		jobEntry.execPerRow = wEveryRow.getSelection();
		jobEntry.setLogfile = wSetLogfile.getSelection();
		jobEntry.addDate = wAddDate.getSelection();
		jobEntry.addTime = wAddTime.getSelection();
		jobEntry.clearResultRows = wClearRows.getSelection();
		jobEntry.clearResultFiles = wClearFiles.getSelection();
		jobEntry.setClustering(wCluster.getSelection());

		jobEntry.setChanged();

		dispose();
	}
}
