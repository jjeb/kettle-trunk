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

package org.pentaho.di.ui.trans.steps.csvinput;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.provider.local.LocalFile;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.csvinput.CsvInputMeta;
import org.pentaho.di.trans.steps.csvinput.Messages;
import org.pentaho.di.trans.steps.textfileinput.TextFileInput;
import org.pentaho.di.trans.steps.textfileinput.TextFileInputField;
import org.pentaho.di.trans.steps.textfileinput.TextFileInputMeta;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.steps.textfileinput.TextFileCSVImportProgressDialog;

public class CsvInputDialog extends BaseStepDialog implements StepDialogInterface
{
	private CsvInputMeta inputMeta;
	
	private TextVar      wFilename;
	private Button       wbbFilename; // Browse for a file
	private TextVar      wDelimiter;
	private TextVar      wEnclosure;
	private TextVar      wBufferSize;
	private Button       wLazyConversion;
	private Button       wHeaderPresent;
	private TableView    wFields;
	
	public CsvInputDialog(Shell parent, Object in, TransMeta tr, String sname)
	{
		super(parent, (BaseStepMeta)in, tr, sname);
		inputMeta=(CsvInputMeta)in;
	}

	public String open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
 		props.setLook(shell);
 		setShellImage(shell, inputMeta);
        
		ModifyListener lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				inputMeta.setChanged();
			}
		};
		changed = inputMeta.hasChanged();

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("CsvInputDialog.Shell.Title")); //$NON-NLS-1$
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Step name line
		//
		wlStepname=new Label(shell, SWT.RIGHT);
		wlStepname.setText(Messages.getString("CsvInputDialog.Stepname.Label")); //$NON-NLS-1$
 		props.setLook(wlStepname);
		fdlStepname=new FormData();
		fdlStepname.left = new FormAttachment(0, 0);
		fdlStepname.right= new FormAttachment(middle, -margin);
		fdlStepname.top  = new FormAttachment(0, margin);
		wlStepname.setLayoutData(fdlStepname);
		wStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wStepname);
		wStepname.addModifyListener(lsMod);
		fdStepname=new FormData();
		fdStepname.left = new FormAttachment(middle, 0);
		fdStepname.top  = new FormAttachment(0, margin);
		fdStepname.right= new FormAttachment(100, 0);
		wStepname.setLayoutData(fdStepname);
		Control lastControl = wStepname;
		
		// Filename...
		//
		// The filename browse button
		//
        wbbFilename=new Button(shell, SWT.PUSH| SWT.CENTER);
        props.setLook(wbbFilename);
        wbbFilename.setText(Messages.getString("System.Button.Browse"));
        wbbFilename.setToolTipText(Messages.getString("System.Tooltip.BrowseForFileOrDirAndAdd"));
        FormData fdbFilename = new FormData();
        fdbFilename.top  = new FormAttachment(lastControl, margin);
        fdbFilename.right= new FormAttachment(100, 0);
        wbbFilename.setLayoutData(fdbFilename);

        // The field itself...
        //
		Label wlFilename = new Label(shell, SWT.RIGHT);
		wlFilename.setText(Messages.getString("CsvInputDialog.Filename.Label")); //$NON-NLS-1$
 		props.setLook(wlFilename);
		FormData fdlFilename = new FormData();
		fdlFilename.top  = new FormAttachment(lastControl, margin);
		fdlFilename.left = new FormAttachment(0, 0);
		fdlFilename.right= new FormAttachment(middle, -margin);
		wlFilename.setLayoutData(fdlFilename);
		wFilename=new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wFilename);
		wFilename.addModifyListener(lsMod);
		FormData fdFilename = new FormData();
		fdFilename.top  = new FormAttachment(lastControl, margin);
		fdFilename.left = new FormAttachment(middle, 0);
		fdFilename.right= new FormAttachment(wbbFilename, -margin);
		wFilename.setLayoutData(fdFilename);
		lastControl = wFilename;
		
		// delimiter
		Label wlDelimiter = new Label(shell, SWT.RIGHT);
		wlDelimiter.setText(Messages.getString("CsvInputDialog.Delimiter.Label")); //$NON-NLS-1$
 		props.setLook(wlDelimiter);
		FormData fdlDelimiter = new FormData();
		fdlDelimiter.top  = new FormAttachment(lastControl, margin);
		fdlDelimiter.left = new FormAttachment(0, 0);
		fdlDelimiter.right= new FormAttachment(middle, -margin);
		wlDelimiter.setLayoutData(fdlDelimiter);
		wDelimiter=new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wDelimiter);
		wDelimiter.addModifyListener(lsMod);
		FormData fdDelimiter = new FormData();
		fdDelimiter.top  = new FormAttachment(lastControl, margin);
		fdDelimiter.left = new FormAttachment(middle, 0);
		fdDelimiter.right= new FormAttachment(100, 0);
		wDelimiter.setLayoutData(fdDelimiter);
		lastControl = wDelimiter;

		// delimiter
		Label wlEnclosure = new Label(shell, SWT.RIGHT);
		wlEnclosure.setText(Messages.getString("CsvInputDialog.Enclosure.Label")); //$NON-NLS-1$
 		props.setLook(wlEnclosure);
		FormData fdlEnclosure = new FormData();
		fdlEnclosure.top  = new FormAttachment(lastControl, margin);
		fdlEnclosure.left = new FormAttachment(0, 0);
		fdlEnclosure.right= new FormAttachment(middle, -margin);
		wlEnclosure.setLayoutData(fdlEnclosure);
		wEnclosure=new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wEnclosure);
		wEnclosure.addModifyListener(lsMod);
		FormData fdEnclosure = new FormData();
		fdEnclosure.top  = new FormAttachment(lastControl, margin);
		fdEnclosure.left = new FormAttachment(middle, 0);
		fdEnclosure.right= new FormAttachment(100, 0);
		wEnclosure.setLayoutData(fdEnclosure);
		lastControl = wEnclosure;

		// bufferSize
		//
		Label wlBufferSize = new Label(shell, SWT.RIGHT);
		wlBufferSize.setText(Messages.getString("CsvInputDialog.BufferSize.Label")); //$NON-NLS-1$
 		props.setLook(wlBufferSize);
		FormData fdlBufferSize = new FormData();
		fdlBufferSize.top  = new FormAttachment(lastControl, margin);
		fdlBufferSize.left = new FormAttachment(0, 0);
		fdlBufferSize.right= new FormAttachment(middle, -margin);
		wlBufferSize.setLayoutData(fdlBufferSize);
		wBufferSize = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wBufferSize);
		wBufferSize.addModifyListener(lsMod);
		FormData fdBufferSize = new FormData();
		fdBufferSize.top  = new FormAttachment(lastControl, margin);
		fdBufferSize.left = new FormAttachment(middle, 0);
		fdBufferSize.right= new FormAttachment(100, 0);
		wBufferSize.setLayoutData(fdBufferSize);
		lastControl = wBufferSize;
		
		// performingLazyConversion?
		//
		Label wlLazyConversion = new Label(shell, SWT.RIGHT);
		wlLazyConversion.setText(Messages.getString("CsvInputDialog.LazyConversion.Label")); //$NON-NLS-1$
 		props.setLook(wlLazyConversion);
		FormData fdlLazyConversion = new FormData();
		fdlLazyConversion.top  = new FormAttachment(lastControl, margin);
		fdlLazyConversion.left = new FormAttachment(0, 0);
		fdlLazyConversion.right= new FormAttachment(middle, -margin);
		wlLazyConversion.setLayoutData(fdlLazyConversion);
		wLazyConversion = new Button(shell, SWT.CHECK);
 		props.setLook(wLazyConversion);
		FormData fdLazyConversion = new FormData();
		fdLazyConversion.top  = new FormAttachment(lastControl, margin);
		fdLazyConversion.left = new FormAttachment(middle, 0);
		fdLazyConversion.right= new FormAttachment(100, 0);
		wLazyConversion.setLayoutData(fdLazyConversion);
		lastControl = wLazyConversion;

		// header row?
		//
		Label wlHeaderPresent = new Label(shell, SWT.RIGHT);
		wlHeaderPresent.setText(Messages.getString("CsvInputDialog.HeaderPresent.Label")); //$NON-NLS-1$
 		props.setLook(wlHeaderPresent);
		FormData fdlHeaderPresent = new FormData();
		fdlHeaderPresent.top  = new FormAttachment(lastControl, margin);
		fdlHeaderPresent.left = new FormAttachment(0, 0);
		fdlHeaderPresent.right= new FormAttachment(middle, -margin);
		wlHeaderPresent.setLayoutData(fdlHeaderPresent);
		wHeaderPresent = new Button(shell, SWT.CHECK);
 		props.setLook(wHeaderPresent);
		FormData fdHeaderPresent = new FormData();
		fdHeaderPresent.top  = new FormAttachment(lastControl, margin);
		fdHeaderPresent.left = new FormAttachment(middle, 0);
		fdHeaderPresent.right= new FormAttachment(100, 0);
		wHeaderPresent.setLayoutData(fdHeaderPresent);
		lastControl = wHeaderPresent;

		// Some buttons first, so that the dialog scales nicely...
		//
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel")); //$NON-NLS-1$
		wGet=new Button(shell, SWT.PUSH);
		wGet.setText(Messages.getString("System.Button.GetFields")); //$NON-NLS-1$

		setButtonPositions(new Button[] { wOK, wCancel, wGet, }, margin, null);


		// Fields
        ColumnInfo[] colinf=new ColumnInfo[]
            {
             new ColumnInfo(Messages.getString("CsvInputDialog.NameColumn.Column"),       ColumnInfo.COLUMN_TYPE_TEXT,    false),
             new ColumnInfo(Messages.getString("CsvInputDialog.TypeColumn.Column"),       ColumnInfo.COLUMN_TYPE_CCOMBO,  ValueMeta.getTypes(), true ),
             new ColumnInfo(Messages.getString("CsvInputDialog.FormatColumn.Column"),     ColumnInfo.COLUMN_TYPE_CCOMBO,  Const.getConversionFormats()),
             new ColumnInfo(Messages.getString("CsvInputDialog.LengthColumn.Column"),     ColumnInfo.COLUMN_TYPE_TEXT,    false),
             new ColumnInfo(Messages.getString("CsvInputDialog.PrecisionColumn.Column"),  ColumnInfo.COLUMN_TYPE_TEXT,    false),
             new ColumnInfo(Messages.getString("CsvInputDialog.CurrencyColumn.Column"),   ColumnInfo.COLUMN_TYPE_TEXT,    false),
             new ColumnInfo(Messages.getString("CsvInputDialog.DecimalColumn.Column"),    ColumnInfo.COLUMN_TYPE_TEXT,    false),
             new ColumnInfo(Messages.getString("CsvInputDialog.GroupColumn.Column"),      ColumnInfo.COLUMN_TYPE_TEXT,    false),
            };
        
        wFields=new TableView(transMeta, shell, 
                              SWT.FULL_SELECTION | SWT.MULTI, 
                              colinf, 
                              1,  
                              lsMod,
                              props
                              );

        FormData fdFields = new FormData();
        fdFields.top   = new FormAttachment(lastControl, margin*2);
        fdFields.bottom= new FormAttachment(wOK, -margin*2);
        fdFields.left  = new FormAttachment(0, 0);
        fdFields.right = new FormAttachment(100, 0);
        wFields.setLayoutData(fdFields);
        
		// Add listeners
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		lsGet      = new Listener() { public void handleEvent(Event e) { getCSV(); } };

		wCancel.addListener(SWT.Selection, lsCancel);
		wOK.addListener    (SWT.Selection, lsOK    );
		wGet.addListener   (SWT.Selection, lsGet   );
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wStepname.addSelectionListener( lsDef );
		wFilename.addSelectionListener( lsDef );
		wDelimiter.addSelectionListener( lsDef );
		wEnclosure.addSelectionListener( lsDef );
		wBufferSize.addSelectionListener( lsDef );
		
		// Listen to the browse button next to the file name
		wbbFilename.addSelectionListener(
				new SelectionAdapter()
				{
					public void widgetSelected(SelectionEvent e) 
					{
						FileDialog dialog = new FileDialog(shell, SWT.OPEN);
						dialog.setFilterExtensions(new String[] {"*.txt;*.csv", "*.csv", "*.txt", "*"});
						if (wFilename.getText()!=null)
						{
							String fname = transMeta.environmentSubstitute(wFilename.getText());
							dialog.setFileName( fname );
						}
						
						dialog.setFilterNames(new String[] {Messages.getString("System.FileType.CSVFiles")+", "+Messages.getString("System.FileType.TextFiles"), Messages.getString("System.FileType.CSVFiles"), Messages.getString("System.FileType.TextFiles"), Messages.getString("System.FileType.AllFiles")});
						
						if (dialog.open()!=null)
						{
							String str = dialog.getFilterPath()+System.getProperty("file.separator")+dialog.getFileName();
							wFilename.setText(str);
						}
					}
				}
			);

		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );


		// Set the shell size, based upon previous time...
		setSize();
		
		getData();
		inputMeta.setChanged(changed);
	
		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return stepname;
	}
	
	public void getData()
	{
		getData(inputMeta);
	}
	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData(CsvInputMeta inputMeta)
	{
		wStepname.setText(stepname);
		wFilename.setText(Const.NVL(inputMeta.getFilename(), ""));
		wDelimiter.setText(Const.NVL(inputMeta.getDelimiter(), ""));
		wEnclosure.setText(Const.NVL(inputMeta.getEnclosure(), ""));
		wBufferSize.setText(Const.NVL(inputMeta.getBufferSize(), ""));
		wLazyConversion.setSelection(inputMeta.isLazyConversionActive());
		wHeaderPresent.setSelection(inputMeta.isHeaderPresent());

		for (int i=0;i<inputMeta.getInputFields().length;i++) {
			TextFileInputField field = inputMeta.getInputFields()[i];
			
			TableItem item = new TableItem(wFields.table, SWT.NONE);
			int colnr=1;
			item.setText(colnr++, Const.NVL(field.getName(), ""));
			item.setText(colnr++, ValueMeta.getTypeDesc(field.getType()));
			item.setText(colnr++, Const.NVL(field.getFormat(), ""));
			item.setText(colnr++, field.getLength()>=0?Integer.toString(field.getLength()):"") ;
			item.setText(colnr++, field.getPrecision()>=0?Integer.toString(field.getPrecision()):"") ;
			item.setText(colnr++, Const.NVL(field.getCurrencySymbol(), ""));
			item.setText(colnr++, Const.NVL(field.getDecimalSymbol(), ""));
			item.setText(colnr++, Const.NVL(field.getGroupSymbol(), ""));
		}
		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);
		
		wStepname.selectAll();
	}
	
	private void cancel()
	{
		stepname=null;
		inputMeta.setChanged(changed);
		dispose();
	}
	
	private void getInfo(CsvInputMeta inputMeta) {
		
		inputMeta.setFilename(wFilename.getText());
		inputMeta.setDelimiter(wDelimiter.getText());
		inputMeta.setEnclosure(wEnclosure.getText());
		inputMeta.setBufferSize(wBufferSize.getText());
		inputMeta.setLazyConversionActive(wLazyConversion.getSelection());
		inputMeta.setHeaderPresent(wHeaderPresent.getSelection());

    	int nrNonEmptyFields = wFields.nrNonEmpty(); 
    	inputMeta.allocate(nrNonEmptyFields);

		for (int i=0;i<nrNonEmptyFields;i++) {
			TableItem item = wFields.getNonEmpty(i);
			inputMeta.getInputFields()[i] = new TextFileInputField();
			
			int colnr=1;
			inputMeta.getInputFields()[i].setName( item.getText(colnr++) );
			inputMeta.getInputFields()[i].setType( ValueMeta.getType( item.getText(colnr++) ) );
			inputMeta.getInputFields()[i].setFormat( item.getText(colnr++) );
			inputMeta.getInputFields()[i].setLength( Const.toInt(item.getText(colnr++), -1) );
			inputMeta.getInputFields()[i].setPrecision( Const.toInt(item.getText(colnr++), -1) );
			inputMeta.getInputFields()[i].setCurrencySymbol( item.getText(colnr++) );
			inputMeta.getInputFields()[i].setDecimalSymbol( item.getText(colnr++) );
			inputMeta.getInputFields()[i].setGroupSymbol( item.getText(colnr++) );
		}
		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);
		
		inputMeta.setChanged();
	}
	
	private void ok()
	{
		getInfo(inputMeta);
		stepname = wStepname.getText();
		dispose();
	}
	
	// Get the data layout
	private void getCSV() 
	{
		InputStream inputStream = null;
		try
		{
			CsvInputMeta meta = new CsvInputMeta();
			getInfo(meta);
			
			String filename = transMeta.environmentSubstitute(meta.getFilename());
			
			FileObject fileObject = KettleVFS.getFileObject(filename);
			if (!(fileObject instanceof LocalFile)) {
				// We can only use NIO on local files at the moment, so that's what we limit ourselves to.
				//
				throw new KettleException(Messages.getString("CsvInput.Log.OnlyLocalFilesAreSupported"));
			}
			
			wFields.table.removeAll();
			
			inputStream = KettleVFS.getInputStream(fileObject);
	        
            InputStreamReader reader = new InputStreamReader(inputStream);
            
            // Read a line of data to determine the number of rows...
            //
            String line = TextFileInput.getLine(log, reader, TextFileInputMeta.FILE_FORMAT_MIXED, new StringBuffer(1000));
            
            // Split the string, header or data into parts...
            //
            String[] fieldNames = line.split(meta.getDelimiter()); 
            
            if (!meta.isHeaderPresent()) {
            	// Don't use field names from the header...
            	// Generate field names F1 ... F10
            	//
            	DecimalFormat df = new DecimalFormat("000"); // $NON-NLS-1$
            	for (int i=0;i<fieldNames.length;i++) {
            		fieldNames[i] = "Field_"+df.format(i); // $NON-NLS-1$
            	}
            }
            
            // Update the GUI
            //
            for (int i=0;i<fieldNames.length;i++) {
            	TableItem item = new TableItem(wFields.table, SWT.NONE);
            	item.setText(1, fieldNames[i]);
            	item.setText(2, ValueMeta.getTypeDesc(ValueMetaInterface.TYPE_STRING));
            }
            wFields.removeEmptyRows();
            wFields.setRowNums();
            wFields.optWidth(true);
            
            // Now we can continue reading the rows of data and we can guess the 
            // Sample a few lines to determine the correct type of the fields...
            // 
            String shellText = org.pentaho.di.trans.steps.textfileinput.Messages.getString("TextFileInputDialog.LinesToSample.DialogTitle");
            String lineText = org.pentaho.di.trans.steps.textfileinput.Messages.getString("TextFileInputDialog.LinesToSample.DialogMessage");
            EnterNumberDialog end = new EnterNumberDialog(shell, 100, shellText, lineText);
            int samples = end.open();
            if (samples >= 0)
            {
                getInfo(meta);

		        TextFileCSVImportProgressDialog pd = new TextFileCSVImportProgressDialog(shell, meta, transMeta, reader, samples, SWT.YES);
                String message = pd.open();
                if (message!=null)
                {
                    // OK, what's the result of our search?
                    getData(meta);
                    wFields.removeEmptyRows();
                    wFields.setRowNums();
                    wFields.optWidth(true);

					EnterTextDialog etd = new EnterTextDialog(shell, Messages.getString("TextFileInputDialog.ScanResults.DialogTitle"), Messages.getString("TextFileInputDialog.ScanResults.DialogMessage"), message, true);
					etd.setReadOnly();
					etd.open();
                }
            }
		}
		catch(IOException e)
		{
            new ErrorDialog(shell, Messages.getString("TextFileInputDialog.IOError.DialogTitle"), Messages.getString("TextFileInputDialog.IOError.DialogMessage"), e);
		}
        catch(KettleException e)
        {
            new ErrorDialog(shell, Messages.getString("System.Dialog.Error.Title"), Messages.getString("TextFileInputDialog.ErrorGettingFileDesc.DialogMessage"), e);
        }
		finally
		{
			try
			{
				inputStream.close();
			}
			catch(Exception e)
			{					
			}
		}
	}

	private String readLine(InputStreamReader reader) throws IOException {
		StringBuffer line = new StringBuffer(250);
		
		int c = reader.read();
		while (c=='\n' || c=='\r') { // left over from last line...
			c=reader.read();
		}
		
		while (c!='\n' && c!='\r') {
			line.append((char)c);
			c=reader.read();
		}
		
		return line.toString();
	}
}
