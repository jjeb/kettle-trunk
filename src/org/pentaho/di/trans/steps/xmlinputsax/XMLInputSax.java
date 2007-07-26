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

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Read all sorts of text files, convert them to rows and writes these to one or
 * more output streams.
 * 
 * @author Matt
 * @since 4-apr-2003
 */
public class XMLInputSax extends BaseStep implements StepInterface
{
	private XMLInputSaxMeta meta;

	private XMLInputSaxData data;

	public XMLInputSax(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
			TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		if (first)
		{
			first=false;
			
			data.outputRowMeta = new RowMeta();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
			
			// For String to <type> conversions, we allocate a conversion meta data row as well...
			//
			data.convertRowMeta = (RowMetaInterface)data.outputRowMeta.clone();
			for (int i=0;i<data.convertRowMeta.size();i++) {
				data.convertRowMeta.getValueMeta(i).setType(ValueMetaInterface.TYPE_STRING);
			}
		}

		Object[] outputRowData = getRowFromXML();
		if (outputRowData==null)
		{
			setOutputDone(); // signal end to receiver(s)
			return false; // This is the end of this step.
		}

		if (log.isRowLevel()) logRowlevel("Read row: " + data.outputRowMeta.getString(outputRowData));
		
		putRow(data.outputRowMeta, outputRowData);

		// limit has been reached: stop now.
		//
		if (meta.getRowLimit() > 0 && data.rownr >= meta.getRowLimit()) {
			setOutputDone();
			return false;
		}

		return true;
	}

	private Object[] getRowFromXML() throws KettleValueException
	{
		// finished reading the file, read the next file!
		//
		if (data.document == null) {
			data.filename = null;
		}  else if (!data.document.hasNext())
		{
			data.filename = null;
		}

		// First, see if we need to open a new file
		if (data.filename == null)
		{
			if (!openNextFile())
			{
				return null;
			}
		}

		Object[] outputRowData = data.document.getNext();
		int outputIndex = meta.getInputFields().length;

		// Node itemNode = XMLHandler.getSubNodeByNr(data.section,
		// data.itemElement, data.itemPosition);
		// data.itemPosition++;

		// See if we need to add the filename to the row...
		//
		if (meta.includeFilename() && !Const.isEmpty(meta.getFilenameField()))
		{
			outputRowData[outputIndex++] = data.filename;
		}

		// See if we need to add the row number to the row...
		if (meta.includeRowNumber() && !Const.isEmpty(meta.getRowNumberField()))
		{
			outputRowData[outputIndex] = new Long(data.rownr);
		}

		data.rownr++;

		return outputRowData;
	}

	private boolean openNextFile()
	{
		try
		{
			if (data.filenr >= data.files.length) // finished processing!
			{
				logDetailed("Finished processing files.");
				return false;
			}

			// Is this the last file?
			data.last_file = (data.filenr == data.files.length - 1);
			data.filename = environmentSubstitute(data.files[data.filenr]);

			logBasic("Opening file: " + data.filename);

			// Move file pointer ahead!
			data.filenr++;

			// Open the XML document
			data.document = new XMLInputSaxDataRetriever(data.filename, meta, data);
			data.document.runExample();

		} catch (Exception e)
		{
			logError("Couldn't open file #" + data.filenr + " : " + data.filename + " --> " + e.toString()
					+ e.getMessage() + e.getLocalizedMessage());
			stopAll();
			setErrors(1);
			return false;
		}
		return true;
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta = (XMLInputSaxMeta) smi;
		data = (XMLInputSaxData) sdi;

		if (super.init(smi, sdi))
		{
			data.files = meta.getFiles(this);
			if (data.files == null || data.files.length == 0)
			{
				logError("No file(s) specified! Stop processing.");
				return false;
			}

			data.rownr = 1L;

			return true;
		}
		return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta = (XMLInputSaxMeta) smi;
		data = (XMLInputSaxData) sdi;

		super.dispose(smi, sdi);
	}

	//
	// Run is were the action happens!
	//
	//
	public void run()
	{
		try
		{
			logBasic("Starting to run...");
			while (processRow(meta, data) && !isStopped())
				;
		} catch (Throwable t)
		{
			logError("Unexpected error : " + t.toString());
			setErrors(1);
			stopAll();
		} finally
		{
			dispose(meta, data);
			logSummary();
			markStop();
		}
	}

}