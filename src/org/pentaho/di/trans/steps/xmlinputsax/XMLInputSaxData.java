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

package org.pentaho.di.trans.steps.xmlinputsax;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.zip.ZipInputStream;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.w3c.dom.Node;


/**
 * @author Matt
 * @since 22-jan-2005
 */
public class XMLInputSaxData extends BaseStepData implements StepDataInterface
{
	public String thisline, nextline, lastline;

	public Object[] previousRow;

	public int nr_repeats;

	public NumberFormat nf;

	public DecimalFormat df;

	public DecimalFormatSymbols dfs;

	public SimpleDateFormat daf;

	public DateFormatSymbols dafs;

	public RowMetaInterface outputRowMeta;

	public String files[];

	public boolean last_file;

	public String filename;

	public int filenr;

	public FileInputStream fr;

	public ZipInputStream zi;

	public BufferedInputStream is;

	public XMLInputSaxDataRetriever document;

	public Node section;

	public String itemElement;

	public int itemCount;

	public int itemPosition;

	public long rownr;

	public RowMetaInterface convertRowMeta;

	/**
	 * 
	 */
	public XMLInputSaxData()
	{
		super();

		thisline = null;
		nextline = null;
		nf = NumberFormat.getInstance();
		df = (DecimalFormat) nf;
		dfs = new DecimalFormatSymbols();
		daf = new SimpleDateFormat();
		dafs = new DateFormatSymbols();

		nr_repeats = 0;
		previousRow = null;
		filenr = 0;

		fr = null;
		zi = null;
		is = null;
	}

}
