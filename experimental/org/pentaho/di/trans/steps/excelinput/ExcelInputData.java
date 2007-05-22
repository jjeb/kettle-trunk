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
 
package org.pentaho.di.trans.steps.excelinput;

import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.playlist.FilePlayList;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandler;



/**
 * @author Matt
 * @since 24-jan-2005
 */
public class ExcelInputData extends BaseStepData implements StepDataInterface
{
    /**
     * The previous row in case we want to repeat values...
     */
    public Object[] previousRow;

	/**
	 * The maximum length of all filenames...
	 */
	public int maxfilelength;

	/**
	 * The maximum length of all sheets...
	 */
	public int maxsheetlength;

	/**
	 * The Excel files to read
	 */
	public FileInputList files;

	/**
	 * The file number that's being handled...
	 */
	public int filenr;

	public String filename;

	public FileObject file;

	/**
	 * The workbook that's being processed...
	 */
	public Workbook workbook;

	/**
	 * The sheet number that's being processed...
	 */
	public int sheetnr;

	/**
	 * The sheet that's being processed...
	 */
	public Sheet sheet;

	/**
	 * The row where we left off the previous time...
	 */
	public int rownr;

	/**
	 * The column where we left off previous time...
	 */
	public int colnr;

	/**
	 * The error handler when processing of a row fails.
	 */
	public FileErrorHandler errorHandler;

	public FilePlayList filePlayList;

    public RowMetaInterface outputRowMeta;
    
    ValueMetaInterface valueMetaString;
    ValueMetaInterface valueMetaNumber;
    ValueMetaInterface valueMetaDate;
    ValueMetaInterface valueMetaBoolean;

    public RowMetaInterface conversionRowMeta;

	/**
	 * 
	 */
	public ExcelInputData()
	{
		super();
		workbook=null;
		filenr=0;
		sheetnr=0;
		rownr=-1;
		colnr=-1;
        
        valueMetaString  = new ValueMeta("v", ValueMetaInterface.TYPE_STRING); 
        valueMetaNumber  = new ValueMeta("v", ValueMetaInterface.TYPE_NUMBER); 
        valueMetaDate    = new ValueMeta("v", ValueMetaInterface.TYPE_DATE); 
        valueMetaBoolean = new ValueMeta("v", ValueMetaInterface.TYPE_BOOLEAN); 
	}
}
