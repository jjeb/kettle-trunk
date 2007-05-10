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
 * 
 * Created on 4-apr-2003
 * 
 */

package org.pentaho.di.trans.steps.textfileinput;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import be.ibridge.kettle.core.Base64;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleXMLException;
import be.ibridge.kettle.repository.Repository;

public class TextFileInputMeta extends BaseStepMeta implements StepMetaInterface
{
	private static final String NO = "N";

	private static final String YES = "Y";

	public final static int TYPE_TRIM_NONE = 0;

	public final static int TYPE_TRIM_LEFT = 1;

	public final static int TYPE_TRIM_RIGHT = 2;

	public final static int TYPE_TRIM_BOTH = 3;

	public final static String trimTypeCode[] = { "none", "left", "right", "both" };

	public final static String trimTypeDesc[] = { Messages.getString("TextFileInputMeta.TrimType.None"), Messages.getString("TextFileInputMeta.TrimType.Left"),
			Messages.getString("TextFileInputMeta.TrimType.Right"), Messages.getString("TextFileInputMeta.TrimType.Both") };

	private static final String STRING_BASE64_PREFIX = "Base64: ";

	/** Array of filenames */
	private String fileName[];

	/** Wildcard or filemask (regular expression) */
	private String fileMask[];

	/** Array of boolean values as string, indicating if a file is required. */
	private String fileRequired[];

	/** Type of file: CSV or fixed */
	private String fileType;

	/** String used to separated field (;) */
	private String separator;

	/** String used to enclose separated fields (") */
	private String enclosure;

	/** Escape character used to escape the enclosure String (\) */
	private String escapeCharacter;

	/** Switch to allow breaks (CR/LF) in Enclosures */
	private boolean breakInEnclosureAllowed;

	/** Flag indicating that the file contains one header line that should be skipped. */
	private boolean header;

	/** The number of header lines, defaults to 1 */
	private int nrHeaderLines;

	/** Flag indicating that the file contains one footer line that should be skipped. */
	private boolean footer;

	/** The number of footer lines, defaults to 1 */
	private int nrFooterLines;

	/** Flag indicating that a single line is wrapped onto one or more lines in the text file. */
	private boolean lineWrapped;

	/** The number of times the line wrapped */
	private int nrWraps;

	/** Flag indicating that the text-file has a paged layout. */
	private boolean layoutPaged;

	/** The number of lines in the document header */
	private int nrLinesDocHeader;

	/** The number of lines to read per page */
	private int nrLinesPerPage;

	/** Type of compression being used */
	private String fileCompression;

	/** Flag indicating that we should skip all empty lines */
	private boolean noEmptyLines;

	/** Flag indicating that we should include the filename in the output */
	private boolean includeFilename;

	/** The name of the field in the output containing the filename */
	private String filenameField;

	/** Flag indicating that a row number field should be included in the output */
	private boolean includeRowNumber;
	
	/** Flag indicating row number is per file */
	private boolean rowNumberByFile;

	/** The name of the field in the output containing the row number */
	private String rowNumberField;

	/** The file format: DOS or UNIX or mixed*/
	private String fileFormat;

	/** The maximum number or lines to read */
	private long rowLimit;

	/** The fields to import... */
	private TextFileInputField inputFields[];

	/** The filters to use... */
	private TextFileFilter filter[];

	/** The encoding to use for reading: null or empty string means system default encoding */
	private String encoding;

	/** Ignore error : turn into warnings */
	private boolean errorIgnored;

	/** The name of the field that will contain the number of errors in the row*/
	private String errorCountField;

	/** The name of the field that will contain the names of the fields that generated errors, separated by , */
	private String errorFieldsField;

	/** The name of the field that will contain the error texts, separated by CR */
	private String errorTextField;

	/** The directory that will contain warning files */
	private String warningFilesDestinationDirectory;

	/** The extension of warning files */
	private String warningFilesExtension;

	/** The directory that will contain error files */
	private String errorFilesDestinationDirectory;

	/** The extension of error files */
	private String errorFilesExtension;

	/** The directory that will contain line number files */
	private String lineNumberFilesDestinationDirectory;

	/** The extension of line number files */
	private String lineNumberFilesExtension;

	/** Indicate whether or not we want to date fields strictly according to the format or lenient */
	private boolean dateFormatLenient;

	/** Specifies the Locale of the Date format, null means the default */
	private Locale dateFormatLocale;

	/** If error line are skipped, you can replay without introducing doubles.*/
	private boolean errorLineSkipped;

	/** Are we accepting filenames in input rows?  */
	private boolean acceptingFilenames;
	
	/** The field in which the filename is placed */
	private String  acceptingField;

	/** The stepname to accept filenames from */
	private String  acceptingStepName;

	/** The step to accept filenames from */
	private StepMeta acceptingStep;

	
	/**
	 * @return Returns the encoding.
	 */
	public String getEncoding()
	{
		return encoding;
	}

	/**
	 * @param encoding The encoding to set.
	 */
	public void setEncoding(String encoding)
	{
		this.encoding = encoding;
	}

	public TextFileInputMeta()
	{
		super(); // allocate BaseStepMeta
	}

	/**
	 * @return Returns the input fields.
	 */
	public TextFileInputField[] getInputFields()
	{
		return inputFields;
	}

	/**
	 * @param inputFields The input fields to set.
	 */
	public void setInputFields(TextFileInputField[] inputFields)
	{
		this.inputFields = inputFields;
	}

	/**
	 * @return Returns the enclosure.
	 */
	public String getEnclosure()
	{
		return enclosure;
	}

	/**
	 * @param enclosure The enclosure to set.
	 */
	public void setEnclosure(String enclosure)
	{
		this.enclosure = enclosure;
	}

	/**
	 * @return Returns the breakInEnclosureAllowed.
	 */
	public boolean isBreakInEnclosureAllowed()
	{
		return breakInEnclosureAllowed;
	}

	/**
	 * @param breakInEnclosureAllowed The breakInEnclosureAllowed to set.
	 */
	public void setBreakInEnclosureAllowed(boolean breakInEnclosureAllowed)
	{
		this.breakInEnclosureAllowed = breakInEnclosureAllowed;
	}

	/**
	 * @return Returns the fileFormat.
	 */
	public String getFileFormat()
	{
		return fileFormat;
	}

	/**
	 * @param fileFormat The fileFormat to set.
	 */
	public void setFileFormat(String fileFormat)
	{
		this.fileFormat = fileFormat;
	}

	/**
	 * @return Returns the fileMask.
	 */
	public String[] getFileMask()
	{
		return fileMask;
	}

	/**
	 * @return Returns the fileRequired.
	 */
	public String[] getFileRequired()
	{
		return fileRequired;
	}

	/**
	 * @param fileMask The fileMask to set.
	 */
	public void setFileMask(String[] fileMask)
	{
		this.fileMask = fileMask;
	}

	/**
	 * @param fileRequired The fileRequired to set.
	 */
	public void setFileRequired(String[] fileRequired)
	{
		this.fileRequired = fileRequired;
	}

	/**
	 * @return Returns the fileName.
	 */
	public String[] getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName The fileName to set.
	 */
	public void setFileName(String[] fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * @return Returns the filenameField.
	 */
	public String getFilenameField()
	{
		return filenameField;
	}

	/**
	 * @param filenameField The filenameField to set.
	 */
	public void setFilenameField(String filenameField)
	{
		this.filenameField = filenameField;
	}

	/**
	 * @return Returns the fileType.
	 */
	public String getFileType()
	{
		return fileType;
	}

	/**
	 * @param fileType The fileType to set.
	 */
	public void setFileType(String fileType)
	{
		this.fileType = fileType;
	}

	/**
	 * @return The array of filters for the metadata of this text file input step. 
	 */
	public TextFileFilter[] getFilter()
	{
		return filter;
	}

	/**
	 * @param filter The array of filters to use
	 */
	public void setFilter(TextFileFilter[] filter)
	{
		this.filter = filter;
	}

	/**
	 * @return Returns the footer.
	 */
	public boolean hasFooter()
	{
		return footer;
	}

	/**
	 * @param footer The footer to set.
	 */
	public void setFooter(boolean footer)
	{
		this.footer = footer;
	}

	/**
	 * @return Returns the header.
	 */
	public boolean hasHeader()
	{
		return header;
	}

	/**
	 * @param header The header to set.
	 */
	public void setHeader(boolean header)
	{
		this.header = header;
	}

	/**
	 * @return Returns the includeFilename.
	 */
	public boolean includeFilename()
	{
		return includeFilename;
	}

	/**
	 * @param includeFilename The includeFilename to set.
	 */
	public void setIncludeFilename(boolean includeFilename)
	{
		this.includeFilename = includeFilename;
	}

	/**
	 * @return Returns the includeRowNumber.
	 */
	public boolean includeRowNumber()
	{
		return includeRowNumber;
	}

	/**
	 * @param includeRowNumber The includeRowNumber to set.
	 */
	public void setIncludeRowNumber(boolean includeRowNumber)
	{
		this.includeRowNumber = includeRowNumber;
	}
	
	/**
	 * true if row number reset for each file
	 * @return rowNumberByFile
	 */
	public boolean isRowNumberByFile()
	{
		return rowNumberByFile;
	}
	/** 
	 * @param rowNumberByFile. True if row number field is reset for each file
	 */
	public void setRowNumberByFile(boolean rowNumberByFile)
	{
		this.rowNumberByFile = rowNumberByFile;
	}

	/**
	 * @return Returns the noEmptyLines.
	 */
	public boolean noEmptyLines()
	{
		return noEmptyLines;
	}

	/**
	 * @param noEmptyLines The noEmptyLines to set.
	 */
	public void setNoEmptyLines(boolean noEmptyLines)
	{
		this.noEmptyLines = noEmptyLines;
	}

	/**
	 * @return Returns the rowLimit.
	 */
	public long getRowLimit()
	{
		return rowLimit;
	}

	/**
	 * @param rowLimit The rowLimit to set.
	 */
	public void setRowLimit(long rowLimit)
	{
		this.rowLimit = rowLimit;
	}

	/**
	 * @return Returns the rowNumberField.
	 */
	public String getRowNumberField()
	{
		return rowNumberField;
	}

	/**
	 * @param rowNumberField The rowNumberField to set.
	 */
	public void setRowNumberField(String rowNumberField)
	{
		this.rowNumberField = rowNumberField;
	}

	/**
	 * @return Returns the separator.
	 */
	public String getSeparator()
	{
		return separator;
	}

	/**
	 * @param separator The separator to set.
	 */
	public void setSeparator(String separator)
	{
		this.separator = separator;
	}

	/**
	 * @return Returns the type of compression used
	 */
	public String getFileCompression()
	{
		return fileCompression;
	}

	/**
	 * @param Set the compression type
	 */
	public void setFileCompression(String fileCompression)
	{
		this.fileCompression = fileCompression;
	}

	public void loadXML(Node stepnode, ArrayList databases, Hashtable counters) throws KettleXMLException
	{
		readData(stepnode);
	}

	public Object clone()
	{
		TextFileInputMeta retval = (TextFileInputMeta) super.clone();

		int nrfiles = fileName.length;
		int nrfields = inputFields.length;
		int nrfilters = filter.length;

		retval.allocate(nrfiles, nrfields, nrfilters);

        for (int i = 0; i < nrfiles; i++)
        {
            retval.fileName[i] = fileName[i];
            retval.fileMask[i] = fileMask[i];
        }

		for (int i = 0; i < nrfields; i++)
		{
			retval.inputFields[i] = (TextFileInputField) inputFields[i].clone();
		}

		for (int i = 0; i < nrfilters; i++)
		{
			retval.filter[i] = (TextFileFilter) filter[i].clone();
		}

		retval.dateFormatLocale = (Locale) dateFormatLocale.clone();

		return retval;
	}

	public void allocate(int nrfiles, int nrfields, int nrfilters)
	{
		fileName = new String[nrfiles];
		fileMask = new String[nrfiles];
		fileRequired = new String[nrfiles];

		inputFields = new TextFileInputField[nrfields];

		filter = new TextFileFilter[nrfilters];
	}

	public void setDefault()
	{
		separator = ";";
		enclosure = "\"";
		breakInEnclosureAllowed = false;
		header = true;
		nrHeaderLines = 1;
		footer = false;
		nrFooterLines = 1;
		lineWrapped = false;
		nrWraps = 1;
		layoutPaged = false;
		nrLinesPerPage = 80;
		nrLinesDocHeader = 0;
		fileCompression = "None";
		noEmptyLines = true;
		fileFormat = "DOS";
		fileType = "CSV";
		includeFilename = false;
		filenameField = "";
		includeRowNumber = false;
		rowNumberField = "";
		errorIgnored = false;
		errorLineSkipped = false;
		warningFilesDestinationDirectory = null;
		warningFilesExtension = "warning";
		errorFilesDestinationDirectory = null;
		errorFilesExtension = "error";
		lineNumberFilesDestinationDirectory = null;
		lineNumberFilesExtension = "line";
		dateFormatLenient = true;
		rowNumberByFile = false;

		int nrfiles = 0;
		int nrfields = 0;
		int nrfilters = 0;

		allocate(nrfiles, nrfields, nrfilters);

		for (int i = 0; i < nrfiles; i++)
		{
			fileName[i] = "filename" + (i + 1);
			fileMask[i] = "";
			fileRequired[i] = NO;
		}

		for (int i = 0; i < nrfields; i++)
		{
			inputFields[i] = new TextFileInputField("field" + (i + 1), 1, -1);
		}

		dateFormatLocale = Locale.getDefault();

		rowLimit = 0L;
	}

	public void getFields(RowMetaInterface r, String name, RowMetaInterface info)
	{
        RowMetaInterface row;
		if (r == null)
			row = new RowMeta(); // give back values
		else
			row = r; // add to the existing row of values...

		for (int i = 0; i < inputFields.length; i++)
		{
			TextFileInputField field = inputFields[i];

			int type = field.getType();
			if (type == ValueMetaInterface.TYPE_NONE) type = ValueMetaInterface.TYPE_STRING;
            ValueMetaInterface v = new ValueMeta(field.getName(), type);
			v.setLength(field.getLength());
            v.setPrecision(field.getPrecision());
			v.setOrigin(name);
			row.addValueMeta(v);
		}
		if (errorIgnored)
		{
			if (errorCountField != null && errorCountField.length() > 0)
			{
				ValueMetaInterface v = new ValueMeta(errorCountField, ValueMetaInterface.TYPE_INTEGER);
				v.setOrigin(name);
				row.addValueMeta(v);
			}
			if (errorFieldsField != null && errorFieldsField.length() > 0)
			{
                ValueMetaInterface v = new ValueMeta(errorFieldsField, ValueMetaInterface.TYPE_STRING);
				v.setOrigin(name);
				row.addValueMeta(v);
			}
			if (errorTextField != null && errorTextField.length() > 0)
			{
                ValueMetaInterface v = new ValueMeta(errorTextField, ValueMetaInterface.TYPE_STRING);
				v.setOrigin(name);
				row.addValueMeta(v);
			}
		}
		if (includeFilename)
		{
            ValueMetaInterface v = new ValueMeta(filenameField, ValueMetaInterface.TYPE_STRING);
			v.setLength(100);
			v.setOrigin(name);
			row.addValueMeta(v);
		}
		if (includeRowNumber)
		{
            ValueMetaInterface v = new ValueMeta(rowNumberField, ValueMetaInterface.TYPE_NUMBER);
			v.setLength(7);
            v.setPrecision(0);
			v.setOrigin(name);
			row.addValueMeta(v);
		}
	}

	public String getXML()
	{
		StringBuffer retval = new StringBuffer(1500);

		retval.append("    ").append(XMLHandler.addTagValue("accept_filenames", acceptingFilenames));
		retval.append("    ").append(XMLHandler.addTagValue("accept_field", acceptingField));
		retval.append("    ").append(XMLHandler.addTagValue("accept_stepname", (acceptingStep!=null?acceptingStep.getName():"") ));
		
		retval.append("    ").append(XMLHandler.addTagValue("separator", separator));
		retval.append("    ").append(XMLHandler.addTagValue("enclosure", enclosure));
		retval.append("    ").append(XMLHandler.addTagValue("enclosure_breaks", breakInEnclosureAllowed));
		retval.append("    ").append(XMLHandler.addTagValue("escapechar", escapeCharacter));
		retval.append("    ").append(XMLHandler.addTagValue("header", header));
		retval.append("    ").append(XMLHandler.addTagValue("nr_headerlines", nrHeaderLines));
		retval.append("    ").append(XMLHandler.addTagValue("footer", footer));
		retval.append("    ").append(XMLHandler.addTagValue("nr_footerlines", nrFooterLines));
		retval.append("    ").append(XMLHandler.addTagValue("line_wrapped", lineWrapped));
		retval.append("    ").append(XMLHandler.addTagValue("nr_wraps", nrWraps));
		retval.append("    ").append(XMLHandler.addTagValue("layout_paged", layoutPaged));
		retval.append("    ").append(XMLHandler.addTagValue("nr_lines_per_page", nrLinesPerPage));
		retval.append("    ").append(XMLHandler.addTagValue("nr_lines_doc_header", nrLinesDocHeader));
		retval.append("    ").append(XMLHandler.addTagValue("noempty", noEmptyLines));
		retval.append("    ").append(XMLHandler.addTagValue("include", includeFilename));
		retval.append("    ").append(XMLHandler.addTagValue("include_field", filenameField));
		retval.append("    ").append(XMLHandler.addTagValue("rownum", includeRowNumber));
		retval.append("    ").append(XMLHandler.addTagValue("rownumByFile", rowNumberByFile));
		retval.append("    ").append(XMLHandler.addTagValue("rownum_field", rowNumberField));
		retval.append("    ").append(XMLHandler.addTagValue("format", fileFormat));
		retval.append("    ").append(XMLHandler.addTagValue("encoding", encoding));

		retval.append("    <file>").append(Const.CR);
		for (int i = 0; i < fileName.length; i++)
		{
			retval.append("      ").append(XMLHandler.addTagValue("name", fileName[i]));
			retval.append("      ").append(XMLHandler.addTagValue("filemask", fileMask[i]));
			retval.append("      ").append(XMLHandler.addTagValue("file_required", fileRequired[i]));
		}
		retval.append("      ").append(XMLHandler.addTagValue("type", fileType));
		retval.append("      ").append(XMLHandler.addTagValue("compression", fileCompression));
		retval.append("    </file>").append(Const.CR);

		retval.append("    <filters>").append(Const.CR);
		for (int i = 0; i < filter.length; i++)
		{
			String filterString = filter[i].getFilterString();
			byte[] filterBytes = new byte[] {};
			String filterPrefix = "";
			if (filterString != null)
			{
				filterBytes = filterString.getBytes();
				filterPrefix = STRING_BASE64_PREFIX;
			}
			String filterEncoded = filterPrefix + Base64.encodeBytes(filterBytes);

			retval.append("      <filter>").append(Const.CR);
			retval.append("        ").append(XMLHandler.addTagValue("filter_string", filterEncoded, false));
			retval.append("        ").append(XMLHandler.addTagValue("filter_position", filter[i].getFilterPosition(), false));
			retval.append("        ").append(XMLHandler.addTagValue("filter_is_last_line", filter[i].isFilterLastLine(), false));
			retval.append("      </filter>").append(Const.CR);
		}
		retval.append("    </filters>").append(Const.CR);

		retval.append("    <fields>").append(Const.CR);
		for (int i = 0; i < inputFields.length; i++)
		{
			TextFileInputField field = inputFields[i];

			retval.append("      <field>").append(Const.CR);
			retval.append("        ").append(XMLHandler.addTagValue("name", field.getName()));
			retval.append("        ").append(XMLHandler.addTagValue("type", field.getTypeDesc()));
			retval.append("        ").append(XMLHandler.addTagValue("format", field.getFormat()));
			retval.append("        ").append(XMLHandler.addTagValue("currency", field.getCurrencySymbol()));
			retval.append("        ").append(XMLHandler.addTagValue("decimal", field.getDecimalSymbol()));
			retval.append("        ").append(XMLHandler.addTagValue("group", field.getGroupSymbol()));
			retval.append("        ").append(XMLHandler.addTagValue("nullif", field.getNullString()));
			retval.append("        ").append(XMLHandler.addTagValue("ifnull", field.getIfNullValue()));
			retval.append("        ").append(XMLHandler.addTagValue("position", field.getPosition()));
			retval.append("        ").append(XMLHandler.addTagValue("length", field.getLength()));
			retval.append("        ").append(XMLHandler.addTagValue("precision", field.getPrecision()));
			retval.append("        ").append(XMLHandler.addTagValue("trim_type", field.getTrimTypeCode()));
			retval.append("        ").append(XMLHandler.addTagValue("repeat", field.isRepeated()));
			retval.append("      </field>").append(Const.CR);
		}
		retval.append("    </fields>").append(Const.CR);
		retval.append("    ").append(XMLHandler.addTagValue("limit", rowLimit));

		// ERROR HANDLING
		retval.append("    ").append(XMLHandler.addTagValue("error_ignored", errorIgnored));
		retval.append("    ").append(XMLHandler.addTagValue("error_line_skipped", errorLineSkipped));
		retval.append("    ").append(XMLHandler.addTagValue("error_count_field", errorCountField));
		retval.append("    ").append(XMLHandler.addTagValue("error_fields_field", errorFieldsField));
		retval.append("    ").append(XMLHandler.addTagValue("error_text_field", errorTextField));

		retval.append("    ").append(XMLHandler.addTagValue("bad_line_files_destination_directory", warningFilesDestinationDirectory));
		retval.append("    ").append(XMLHandler.addTagValue("bad_line_files_extension", warningFilesExtension));
		retval.append("    ").append(XMLHandler.addTagValue("error_line_files_destination_directory", errorFilesDestinationDirectory));
		retval.append("    ").append(XMLHandler.addTagValue("error_line_files_extension", errorFilesExtension));
		retval.append("    ").append(XMLHandler.addTagValue("line_number_files_destination_directory", lineNumberFilesDestinationDirectory));
		retval.append("    ").append(XMLHandler.addTagValue("line_number_files_extension", lineNumberFilesExtension));

		retval.append("    ").append(XMLHandler.addTagValue("date_format_lenient", dateFormatLenient));
		retval.append("    ").append(XMLHandler.addTagValue("date_format_locale", dateFormatLocale.toString()));

		return retval.toString();
	}

	private void readData(Node stepnode) throws KettleXMLException
	{
		try
		{
			acceptingFilenames = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "accept_filenames"));
			acceptingField = XMLHandler.getTagValue(stepnode, "accept_field");
			acceptingStepName = XMLHandler.getTagValue(stepnode, "accept_stepname");
			
			separator = XMLHandler.getTagValue(stepnode, "separator");
			enclosure = XMLHandler.getTagValue(stepnode, "enclosure");
			breakInEnclosureAllowed = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "enclosure_breaks"));
			escapeCharacter = XMLHandler.getTagValue(stepnode, "escapechar");
			header = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "header"));
			nrHeaderLines = Const.toInt(XMLHandler.getTagValue(stepnode, "nr_headerlines"), 1);
			footer = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "footer"));
			nrFooterLines = Const.toInt(XMLHandler.getTagValue(stepnode, "nr_footerlines"), 1);
			lineWrapped = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "line_wrapped"));
			nrWraps = Const.toInt(XMLHandler.getTagValue(stepnode, "nr_wraps"), 1);
			layoutPaged = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "layout_paged"));
			nrLinesPerPage = Const.toInt(XMLHandler.getTagValue(stepnode, "nr_lines_per_page"), 1);
			nrLinesDocHeader = Const.toInt(XMLHandler.getTagValue(stepnode, "nr_lines_doc_header"), 1);

			String nempty = XMLHandler.getTagValue(stepnode, "noempty");
			noEmptyLines = YES.equalsIgnoreCase(nempty) || nempty == null;
			includeFilename = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "include"));
			filenameField = XMLHandler.getTagValue(stepnode, "include_field");
			includeRowNumber = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "rownum"));
			rowNumberByFile = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "rownumByFile"));
			rowNumberField = XMLHandler.getTagValue(stepnode, "rownum_field");
			fileFormat = XMLHandler.getTagValue(stepnode, "format");
			encoding = XMLHandler.getTagValue(stepnode, "encoding");

			Node filenode = XMLHandler.getSubNode(stepnode, "file");
			Node fields = XMLHandler.getSubNode(stepnode, "fields");
			Node filtersNode = XMLHandler.getSubNode(stepnode, "filters");
			int nrfiles = XMLHandler.countNodes(filenode, "name");
			int nrfields = XMLHandler.countNodes(fields, "field");
			int nrfilters = XMLHandler.countNodes(filtersNode, "filter");

			allocate(nrfiles, nrfields, nrfilters);

			for (int i = 0; i < nrfiles; i++)
			{
				Node filenamenode = XMLHandler.getSubNodeByNr(filenode, "name", i);
				Node filemasknode = XMLHandler.getSubNodeByNr(filenode, "filemask", i);
				Node fileRequirednode = XMLHandler.getSubNodeByNr(filenode, "file_required", i);
				fileName[i] = XMLHandler.getNodeValue(filenamenode);
				fileMask[i] = XMLHandler.getNodeValue(filemasknode);
				fileRequired[i] = XMLHandler.getNodeValue(fileRequirednode);
			}

			fileType = XMLHandler.getTagValue(stepnode, "file", "type");
			fileCompression = XMLHandler.getTagValue(stepnode, "file", "compression");
			if (fileCompression == null)
			{
				if (YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "file", "zipped")))
				{
					fileCompression = "Zip";
				}
			}

			// Backward compatibility : just one filter
			if (XMLHandler.getTagValue(stepnode, "filter") != null)
			{
				filter = new TextFileFilter[1];
				filter[0] = new TextFileFilter();

				filter[0].setFilterPosition(Const.toInt(XMLHandler.getTagValue(stepnode, "filter_position"), -1));
				filter[0].setFilterString(XMLHandler.getTagValue(stepnode, "filter_string"));
				filter[0].setFilterLastLine(YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "filter_is_last_line")));
			}
			else
			{
				for (int i = 0; i < nrfilters; i++)
				{
					Node fnode = XMLHandler.getSubNodeByNr(filtersNode, "filter", i);
					filter[i] = new TextFileFilter();

					filter[i].setFilterPosition(Const.toInt(XMLHandler.getTagValue(fnode, "filter_position"), -1));

					String filterString = XMLHandler.getTagValue(fnode, "filter_string");
					if (filterString != null && filterString.startsWith(STRING_BASE64_PREFIX))
					{
						filter[i].setFilterString(new String(Base64.decode(filterString.substring(STRING_BASE64_PREFIX.length()))));
					}
					else
					{
						filter[i].setFilterString(filterString);
					}

					filter[i].setFilterLastLine(YES.equalsIgnoreCase(XMLHandler.getTagValue(fnode, "filter_is_last_line")));
				}
			}

			for (int i = 0; i < nrfields; i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);
				TextFileInputField field = new TextFileInputField();

				field.setName(XMLHandler.getTagValue(fnode, "name"));
				field.setType(ValueMeta.getType(XMLHandler.getTagValue(fnode, "type")));
				field.setFormat(XMLHandler.getTagValue(fnode, "format"));
				field.setCurrencySymbol(XMLHandler.getTagValue(fnode, "currency"));
				field.setDecimalSymbol(XMLHandler.getTagValue(fnode, "decimal"));
				field.setGroupSymbol(XMLHandler.getTagValue(fnode, "group"));
				field.setNullString(XMLHandler.getTagValue(fnode, "nullif"));
				field.setIfNullValue(XMLHandler.getTagValue(fnode, "ifnull"));
				field.setPosition(Const.toInt(XMLHandler.getTagValue(fnode, "position"), -1));
				field.setLength(Const.toInt(XMLHandler.getTagValue(fnode, "length"), -1));
				field.setPrecision(Const.toInt(XMLHandler.getTagValue(fnode, "precision"), -1));
				field.setTrimType(getTrimTypeByCode(XMLHandler.getTagValue(fnode, "trim_type")));
				field.setRepeated(YES.equalsIgnoreCase(XMLHandler.getTagValue(fnode, "repeat")));

				inputFields[i] = field;
			}

			// Is there a limit on the number of rows we process?
			rowLimit = Const.toLong(XMLHandler.getTagValue(stepnode, "limit"), 0L);

			errorIgnored = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "error_ignored"));
			errorLineSkipped = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "error_line_skipped"));
			errorCountField = XMLHandler.getTagValue(stepnode, "error_count_field");
			errorFieldsField = XMLHandler.getTagValue(stepnode, "error_fields_field");
			errorTextField = XMLHandler.getTagValue(stepnode, "error_text_field");
			warningFilesDestinationDirectory = XMLHandler.getTagValue(stepnode, "bad_line_files_destination_directory");
			warningFilesExtension = XMLHandler.getTagValue(stepnode, "bad_line_files_extension");
			errorFilesDestinationDirectory = XMLHandler.getTagValue(stepnode, "error_line_files_destination_directory");
			errorFilesExtension = XMLHandler.getTagValue(stepnode, "error_line_files_extension");
			lineNumberFilesDestinationDirectory = XMLHandler.getTagValue(stepnode, "line_number_files_destination_directory");
			lineNumberFilesExtension = XMLHandler.getTagValue(stepnode, "line_number_files_extension");
			// Backward compatible

			dateFormatLenient = !NO.equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "date_format_lenient"));
			String dateLocale = XMLHandler.getTagValue(stepnode, "date_format_locale");
			if (dateLocale != null)
			{
				dateFormatLocale = new Locale(dateLocale);
			}
			else
			{
				dateFormatLocale = Locale.getDefault();
			}
		}
		catch (Exception e)
		{
			throw new KettleXMLException("Unable to load step info from XML", e);
		}
	}
	
	public String getLookupStepname()
	{
		if (acceptingFilenames &&
		    acceptingStep!=null && 
		    !Const.isEmpty( acceptingStep.getName() )
		   ) 
			return acceptingStep.getName();
		return null;
	}

	public void searchInfoAndTargetSteps(ArrayList steps)
	{
		acceptingStep = StepMeta.findStep(steps, acceptingStepName);
	}

	public String[] getInfoSteps()
	{
		if (acceptingFilenames && acceptingStep!=null)
		{
			return new String[] { acceptingStep.getName() };
		}
		return super.getInfoSteps();
	}
	
	public void readRep(Repository rep, long id_step, ArrayList databases, Hashtable counters) throws KettleException
	{
		try
		{
			acceptingFilenames = rep.getStepAttributeBoolean(id_step, "accept_filenames");
			acceptingField     = rep.getStepAttributeString (id_step, "accept_field");
			acceptingStepName  = rep.getStepAttributeString (id_step, "accept_stepname");

			separator = rep.getStepAttributeString(id_step, "separator");
			enclosure = rep.getStepAttributeString(id_step, "enclosure");
			breakInEnclosureAllowed = rep.getStepAttributeBoolean(id_step, "enclosure_breaks");
			escapeCharacter = rep.getStepAttributeString(id_step, "escapechar");
			header = rep.getStepAttributeBoolean(id_step, "header");
			nrHeaderLines = (int) rep.getStepAttributeInteger(id_step, "nr_headerlines");
			footer = rep.getStepAttributeBoolean(id_step, "footer");
			nrFooterLines = (int) rep.getStepAttributeInteger(id_step, "nr_footerlines");
			lineWrapped = rep.getStepAttributeBoolean(id_step, "line_wrapped");
			nrWraps = (int) rep.getStepAttributeInteger(id_step, "nr_wraps");
			layoutPaged = rep.getStepAttributeBoolean(id_step, "layout_paged");
			nrLinesPerPage = (int) rep.getStepAttributeInteger(id_step, "nr_lines_per_page");
			nrLinesDocHeader = (int) rep.getStepAttributeInteger(id_step, "nr_lines_doc_header");
			noEmptyLines = rep.getStepAttributeBoolean(id_step, "noempty");
			
            includeFilename = rep.getStepAttributeBoolean(id_step, "include");
			filenameField = rep.getStepAttributeString(id_step, "include_field");
			includeRowNumber = rep.getStepAttributeBoolean(id_step, "rownum");
			rowNumberByFile = rep.getStepAttributeBoolean(id_step, "rownumByFile");
			rowNumberField = rep.getStepAttributeString(id_step, "rownum_field");
            
			fileFormat = rep.getStepAttributeString(id_step, "format");
			encoding = rep.getStepAttributeString(id_step, "encoding");

			rowLimit = (int) rep.getStepAttributeInteger(id_step, "limit");

			int nrfiles = rep.countNrStepAttributes(id_step, "file_name");
			int nrfields = rep.countNrStepAttributes(id_step, "field_name");
			int nrfilters = rep.countNrStepAttributes(id_step, "filter_string");

			allocate(nrfiles, nrfields, nrfilters);

			for (int i = 0; i < nrfiles; i++)
			{
				fileName[i] = rep.getStepAttributeString(id_step, i, "file_name");
				fileMask[i] = rep.getStepAttributeString(id_step, i, "file_mask");
				fileRequired[i] = rep.getStepAttributeString(id_step, i, "file_required");
				if (!YES.equalsIgnoreCase(fileRequired[i])) fileRequired[i] = NO;
			}
			fileType = rep.getStepAttributeString(id_step, "file_type");
			fileCompression = rep.getStepAttributeString(id_step, "compression");
			if (fileCompression == null)
			{
				if (rep.getStepAttributeBoolean(id_step, "file_zipped"))
					fileCompression = "Zip";
			}

			for (int i = 0; i < nrfilters; i++)
			{
				filter[i] = new TextFileFilter();
				filter[i].setFilterPosition((int) rep.getStepAttributeInteger(id_step, i, "filter_position"));
				filter[i].setFilterString(rep.getStepAttributeString(id_step, i, "filter_string"));
				filter[i].setFilterLastLine(rep.getStepAttributeBoolean(id_step, i, "filter_is_last_line"));
			}

			for (int i = 0; i < nrfields; i++)
			{
				TextFileInputField field = new TextFileInputField();

				field.setName(rep.getStepAttributeString(id_step, i, "field_name"));
				field.setType(ValueMeta.getType(rep.getStepAttributeString(id_step, i, "field_type")));
				field.setFormat(rep.getStepAttributeString(id_step, i, "field_format"));
				field.setCurrencySymbol(rep.getStepAttributeString(id_step, i, "field_currency"));
				field.setDecimalSymbol(rep.getStepAttributeString(id_step, i, "field_decimal"));
				field.setGroupSymbol(rep.getStepAttributeString(id_step, i, "field_group"));
				field.setNullString(rep.getStepAttributeString(id_step, i, "field_nullif"));
				field.setIfNullValue(rep.getStepAttributeString(id_step, i, "field_ifnull"));
				field.setPosition((int) rep.getStepAttributeInteger(id_step, i, "field_position"));
				field.setLength((int) rep.getStepAttributeInteger(id_step, i, "field_length"));
				field.setPrecision((int) rep.getStepAttributeInteger(id_step, i, "field_precision"));
				field.setTrimType(getTrimTypeByCode(rep.getStepAttributeString(id_step, i, "field_trim_type")));
				field.setRepeated(rep.getStepAttributeBoolean(id_step, i, "field_repeat"));

				inputFields[i] = field;
			}

			errorIgnored = rep.getStepAttributeBoolean(id_step, "error_ignored");
			errorLineSkipped = rep.getStepAttributeBoolean(id_step, "error_line_skipped");
			errorCountField = rep.getStepAttributeString(id_step, "error_count_field");
			errorFieldsField = rep.getStepAttributeString(id_step, "error_fields_field");
			errorTextField = rep.getStepAttributeString(id_step, "error_text_field");

			warningFilesDestinationDirectory = rep.getStepAttributeString(id_step, "bad_line_files_dest_dir");
			warningFilesExtension = rep.getStepAttributeString(id_step, "bad_line_files_ext");
			errorFilesDestinationDirectory = rep.getStepAttributeString(id_step, "error_line_files_dest_dir");
			errorFilesExtension = rep.getStepAttributeString(id_step, "error_line_files_ext");
			lineNumberFilesDestinationDirectory = rep.getStepAttributeString(id_step, "line_number_files_dest_dir");
			lineNumberFilesExtension = rep.getStepAttributeString(id_step, "line_number_files_ext");

			dateFormatLenient = rep.getStepAttributeBoolean(id_step, 0, "date_format_lenient", true);

			String dateLocale = rep.getStepAttributeString(id_step, 0, "date_format_locale");
			if (dateLocale != null)
			{
				dateFormatLocale = new Locale(dateLocale);
			}
			else
			{
				dateFormatLocale = Locale.getDefault();
			}
		}
		catch (Exception e)
		{
			throw new KettleException("Unexpected error reading step information from the repository", e);
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "accept_filenames", acceptingFilenames);
			rep.saveStepAttribute(id_transformation, id_step, "accept_field", acceptingField);
			rep.saveStepAttribute(id_transformation, id_step, "accept_stepname", (acceptingStep!=null?acceptingStep.getName():"") );
			
			rep.saveStepAttribute(id_transformation, id_step, "separator", separator);
			rep.saveStepAttribute(id_transformation, id_step, "enclosure", enclosure);
			rep.saveStepAttribute(id_transformation, id_step, "enclosure_breaks", breakInEnclosureAllowed);
			rep.saveStepAttribute(id_transformation, id_step, "escapechar", escapeCharacter);
			rep.saveStepAttribute(id_transformation, id_step, "header", header);
			rep.saveStepAttribute(id_transformation, id_step, "nr_headerlines", nrHeaderLines);
			rep.saveStepAttribute(id_transformation, id_step, "footer", footer);
			rep.saveStepAttribute(id_transformation, id_step, "nr_footerlines", nrFooterLines);
			rep.saveStepAttribute(id_transformation, id_step, "line_wrapped", lineWrapped);
			rep.saveStepAttribute(id_transformation, id_step, "nr_wraps", nrWraps);
			rep.saveStepAttribute(id_transformation, id_step, "layout_paged", layoutPaged);
			rep.saveStepAttribute(id_transformation, id_step, "nr_lines_per_page", nrLinesPerPage);
			rep.saveStepAttribute(id_transformation, id_step, "nr_lines_doc_header", nrLinesDocHeader);

			rep.saveStepAttribute(id_transformation, id_step, "noempty", noEmptyLines);
            
			rep.saveStepAttribute(id_transformation, id_step, "include", includeFilename);
			rep.saveStepAttribute(id_transformation, id_step, "include_field", filenameField);
			rep.saveStepAttribute(id_transformation, id_step, "rownum", includeRowNumber);
			rep.saveStepAttribute(id_transformation, id_step, "rownumByFile", rowNumberByFile);
			rep.saveStepAttribute(id_transformation, id_step, "rownum_field", rowNumberField);
            
			rep.saveStepAttribute(id_transformation, id_step, "format", fileFormat);
			rep.saveStepAttribute(id_transformation, id_step, "encoding", encoding);

			rep.saveStepAttribute(id_transformation, id_step, "limit", rowLimit);

			for (int i = 0; i < fileName.length; i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "file_name", fileName[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_mask", fileMask[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_required", fileRequired[i]);
			}
			rep.saveStepAttribute(id_transformation, id_step, "file_type", fileType);
			rep.saveStepAttribute(id_transformation, id_step, "compression", fileCompression);

			for (int i = 0; i < filter.length; i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "filter_position", filter[i].getFilterPosition());
				rep.saveStepAttribute(id_transformation, id_step, i, "filter_string", filter[i].getFilterString());
				rep.saveStepAttribute(id_transformation, id_step, i, "filter_is_last_line", filter[i].isFilterLastLine());
			}

			for (int i = 0; i < inputFields.length; i++)
			{
				TextFileInputField field = inputFields[i];

				rep.saveStepAttribute(id_transformation, id_step, i, "field_name", field.getName());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_type", field.getTypeDesc());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_format", field.getFormat());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_currency", field.getCurrencySymbol());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_decimal", field.getDecimalSymbol());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_group", field.getGroupSymbol());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_nullif", field.getNullString());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_ifnull", field.getIfNullValue());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_position", field.getPosition());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_length", field.getLength());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_precision", field.getPrecision());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_trim_type", field.getTrimTypeCode());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_repeat", field.isRepeated());
			}

			rep.saveStepAttribute(id_transformation, id_step, "error_ignored", errorIgnored);
			rep.saveStepAttribute(id_transformation, id_step, "error_line_skipped", errorLineSkipped);
			rep.saveStepAttribute(id_transformation, id_step, "error_count_field", errorCountField);
			rep.saveStepAttribute(id_transformation, id_step, "error_fields_field", errorFieldsField);
			rep.saveStepAttribute(id_transformation, id_step, "error_text_field", errorTextField);

			rep.saveStepAttribute(id_transformation, id_step, "bad_line_files_dest_dir", warningFilesDestinationDirectory);
			rep.saveStepAttribute(id_transformation, id_step, "bad_line_files_ext", warningFilesExtension);
			rep.saveStepAttribute(id_transformation, id_step, "error_line_files_dest_dir", errorFilesDestinationDirectory);
			rep.saveStepAttribute(id_transformation, id_step, "error_line_files_ext", errorFilesExtension);
			rep.saveStepAttribute(id_transformation, id_step, "line_number_files_dest_dir", lineNumberFilesDestinationDirectory);
			rep.saveStepAttribute(id_transformation, id_step, "line_number_files_ext", lineNumberFilesExtension);

			rep.saveStepAttribute(id_transformation, id_step, "date_format_lenient", dateFormatLenient);
			rep.saveStepAttribute(id_transformation, id_step, "date_format_locale", dateFormatLocale.toString());
		}
		catch (Exception e)
		{
			throw new KettleException("Unable to save step information to the repository for id_step=" + id_step, e);
		}
	}

	public final static int getTrimTypeByCode(String tt)
	{
		if (tt == null) return 0;

		for (int i = 0; i < trimTypeCode.length; i++)
		{
			if (trimTypeCode[i].equalsIgnoreCase(tt)) return i;
		}
		return 0;
	}

	public final static int getTrimTypeByDesc(String tt)
	{
		if (tt == null) return 0;

		for (int i = 0; i < trimTypeDesc.length; i++)
		{
			if (trimTypeDesc[i].equalsIgnoreCase(tt)) return i;
		}

        // If this fails, try to match using the code.
        return getTrimTypeByCode(tt);
	}

	public final static String getTrimTypeCode(int i)
	{
		if (i < 0 || i >= trimTypeCode.length) return trimTypeCode[0];
		return trimTypeCode[i];
	}

	public final static String getTrimTypeDesc(int i)
	{
		if (i < 0 || i >= trimTypeDesc.length) return trimTypeDesc[0];
		return trimTypeDesc[i];
	}

	public String[] getFilePaths()
	{
		return FileInputList.createFilePathList(fileName, fileMask, fileRequired);
	}

	public FileInputList getTextFileList()
	{
		return FileInputList.createFileList(fileName, fileMask, fileRequired);
	}
    

	public void check(ArrayList remarks, StepMeta stepinfo, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;

		// See if we get input...
		if (input.length > 0)
		{
			if ( !isAcceptingFilenames() )
			{					
			    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TextFileInputMeta.CheckResult.NoInputError"), stepinfo);
			    remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("TextFileInputMeta.CheckResult.AcceptFilenamesOk"), stepinfo);
			    remarks.add(cr);
			}
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("TextFileInputMeta.CheckResult.NoInputOk"), stepinfo);
			remarks.add(cr);
		}

		FileInputList textFileList = getTextFileList();
		if (textFileList.nrOfFiles() == 0)
		{
			if ( ! isAcceptingFilenames() )
			{
			    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TextFileInputMeta.CheckResult.ExpectedFilesError"), stepinfo);
			    remarks.add(cr);
			}
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("TextFileInputMeta.CheckResult.ExpectedFilesOk", "" + textFileList.nrOfFiles()), stepinfo);
			remarks.add(cr);
		}
	}

	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new TextFileInputDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new TextFileInput(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new TextFileInputData();
	}

	/**
	 * @return Returns the escapeCharacter.
	 */
	public String getEscapeCharacter()
	{
		return escapeCharacter;
	}

	/**
	 * @param escapeCharacter The escapeCharacter to set.
	 */
	public void setEscapeCharacter(String escapeCharacter)
	{
		this.escapeCharacter = escapeCharacter;
	}

	public String getErrorCountField()
	{
		return errorCountField;
	}

	public void setErrorCountField(String errorCountField)
	{
		this.errorCountField = errorCountField;
	}

	public String getErrorFieldsField()
	{
		return errorFieldsField;
	}

	public void setErrorFieldsField(String errorFieldsField)
	{
		this.errorFieldsField = errorFieldsField;
	}

	public boolean isErrorIgnored()
	{
		return errorIgnored;
	}

	public void setErrorIgnored(boolean errorIgnored)
	{
		this.errorIgnored = errorIgnored;
	}

	public String getErrorTextField()
	{
		return errorTextField;
	}

	public void setErrorTextField(String errorTextField)
	{
		this.errorTextField = errorTextField;
	}

	/**
	 * @return Returns the lineWrapped.
	 */
	public boolean isLineWrapped()
	{
		return lineWrapped;
	}

	/**
	 * @param lineWrapped The lineWrapped to set.
	 */
	public void setLineWrapped(boolean lineWrapped)
	{
		this.lineWrapped = lineWrapped;
	}

	/**
	 * @return Returns the nrFooterLines.
	 */
	public int getNrFooterLines()
	{
		return nrFooterLines;
	}

	/**
	 * @param nrFooterLines The nrFooterLines to set.
	 */
	public void setNrFooterLines(int nrFooterLines)
	{
		this.nrFooterLines = nrFooterLines;
	}

	/**
	 * @return Returns the nrHeaderLines.
	 */
	public int getNrHeaderLines()
	{
		return nrHeaderLines;
	}

	/**
	 * @param nrHeaderLines The nrHeaderLines to set.
	 */
	public void setNrHeaderLines(int nrHeaderLines)
	{
		this.nrHeaderLines = nrHeaderLines;
	}

	/**
	 * @return Returns the nrWraps.
	 */
	public int getNrWraps()
	{
		return nrWraps;
	}

	/**
	 * @param nrWraps The nrWraps to set.
	 */
	public void setNrWraps(int nrWraps)
	{
		this.nrWraps = nrWraps;
	}

	/**
	 * @return Returns the layoutPaged.
	 */
	public boolean isLayoutPaged()
	{
		return layoutPaged;
	}

	/**
	 * @param layoutPaged The layoutPaged to set.
	 */
	public void setLayoutPaged(boolean layoutPaged)
	{
		this.layoutPaged = layoutPaged;
	}

	/**
	 * @return Returns the nrLinesPerPage.
	 */
	public int getNrLinesPerPage()
	{
		return nrLinesPerPage;
	}

	/**
	 * @param nrLinesPerPage The nrLinesPerPage to set.
	 */
	public void setNrLinesPerPage(int nrLinesPerPage)
	{
		this.nrLinesPerPage = nrLinesPerPage;
	}

	/**
	 * @return Returns the nrLinesDocHeader.
	 */
	public int getNrLinesDocHeader()
	{
		return nrLinesDocHeader;
	}

	/**
	 * @param nrLinesDocHeader The nrLinesDocHeader to set.
	 */
	public void setNrLinesDocHeader(int nrLinesDocHeader)
	{
		this.nrLinesDocHeader = nrLinesDocHeader;
	}

	public String getWarningFilesDestinationDirectory()
	{
		return warningFilesDestinationDirectory;
	}

	public void setWarningFilesDestinationDirectory(String warningFilesDestinationDirectory)
	{
		this.warningFilesDestinationDirectory = warningFilesDestinationDirectory;
	}

	public String getWarningFilesExtension()
	{
		return warningFilesExtension;
	}

	public void setWarningFilesExtension(String warningFilesExtension)
	{
		this.warningFilesExtension = warningFilesExtension;
	}

	public String getLineNumberFilesDestinationDirectory()
	{
		return lineNumberFilesDestinationDirectory;
	}

	public void setLineNumberFilesDestinationDirectory(String lineNumberFilesDestinationDirectory)
	{
		this.lineNumberFilesDestinationDirectory = lineNumberFilesDestinationDirectory;
	}

	public String getLineNumberFilesExtension()
	{
		return lineNumberFilesExtension;
	}

	public void setLineNumberFilesExtension(String lineNumberFilesExtension)
	{
		this.lineNumberFilesExtension = lineNumberFilesExtension;
	}

	public String getErrorFilesDestinationDirectory()
	{
		return errorFilesDestinationDirectory;
	}

	public void setErrorFilesDestinationDirectory(String errorFilesDestinationDirectory)
	{
		this.errorFilesDestinationDirectory = errorFilesDestinationDirectory;
	}

	public String getErrorLineFilesExtension()
	{
		return errorFilesExtension;
	}

	public void setErrorLineFilesExtension(String errorLineFilesExtension)
	{
		this.errorFilesExtension = errorLineFilesExtension;
	}

	public boolean isDateFormatLenient()
	{
		return dateFormatLenient;
	}

	public void setDateFormatLenient(boolean dateFormatLenient)
	{
		this.dateFormatLenient = dateFormatLenient;
	}

	public boolean isErrorLineSkipped()
	{
		return errorLineSkipped;
	}

	public void setErrorLineSkipped(boolean errorLineSkipped)
	{
		this.errorLineSkipped = errorLineSkipped;
	}

	/**
	 * @return Returns the dateFormatLocale.
	 */
	public Locale getDateFormatLocale()
	{
		return dateFormatLocale;
	}

	/**
	 * @param dateFormatLocale The dateFormatLocale to set.
	 */
	public void setDateFormatLocale(Locale dateFormatLocale)
	{
		this.dateFormatLocale = dateFormatLocale;
	}

	public boolean isAcceptingFilenames()
	{
		return acceptingFilenames;
	}

	public void setAcceptingFilenames(boolean getFileFromChef)
	{
		this.acceptingFilenames = getFileFromChef;
	}

	/**
	 * @return Returns the fileNameField.
	 */
	public String getAcceptingField()
	{
		return acceptingField;
	}

	/**
	 * @param fileNameField The fileNameField to set.
	 */
	public void setAcceptingField(String fileNameField)
	{
		this.acceptingField = fileNameField;
	}

	/**
	 * @return Returns the acceptingStep.
	 */
	public String getAcceptingStepName()
	{
		return acceptingStepName;
	}

	/**
	 * @param acceptingStep The acceptingStep to set.
	 */
	public void setAcceptingStepName(String acceptingStep)
	{
		this.acceptingStepName = acceptingStep;
	}

	/**
	 * @return Returns the acceptingStep.
	 */
	public StepMeta getAcceptingStep()
	{
		return acceptingStep;
	}

	/**
	 * @param acceptingStep The acceptingStep to set.
	 */
	public void setAcceptingStep(StepMeta acceptingStep)
	{
		this.acceptingStep = acceptingStep;
	}
}