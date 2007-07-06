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
 
package org.pentaho.di.trans.steps.fixedinput;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.provider.local.LocalFile;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Read a simple fixed width file
 * Just output fields found in the file...
 * 
 * @author Matt
 * @since 2007-07-06
 */

public class FixedInput extends BaseStep implements StepInterface
{
	private FixedInputMeta meta;
	private FixedInputData data;
	
	public FixedInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(FixedInputMeta)smi;
		data=(FixedInputData)sdi;

		if (first) {
			first=false;
			
			data.outputRowMeta = new RowMeta();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
			
			if (meta.isHeaderPresent()) {
				readOneRow(false); // skip this row.
			}
		}
		
		Object[] outputRowData=readOneRow(true);    // get row, set busy!
		if (outputRowData==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		putRow(data.outputRowMeta, outputRowData);     // copy row to possible alternate rowset(s).

        if (checkFeedback(linesInput)) logBasic(Messages.getString("FixedInput.Log.LineNumber", Long.toString(linesInput))); //$NON-NLS-1$
			
		return true;
	}

	
	/** Read a single row of data from the file... 
	 * 
	 * @param doConversions if you want to do conversions, set to false for the header row.
	 * @return a row of data...
	 * @throws KettleException
	 */
	private Object[] readOneRow(boolean doConversions) throws KettleException {

		try {

			Object[] outputRowData = RowDataUtil.allocateRowData(data.outputRowMeta.size());
			int outputIndex=0;
			
			// The strategy is as follows...
			// We read a block of byte[] from the file.
			// 
			// Then we scan that block of data.
			// We keep a byte[] that we extend if needed..
			// At the end of the block we read another, etc.
			//
			// Let's start by looking where we left off reading.
			//

			if (data.stopReading) {
				return null;
			}
			
			for (int i=0;i<meta.getFieldNames().length;i++) {
				
				int fieldWidth = meta.getFieldWidth()[i];
				if (data.endBuffer+meta.getFieldWidth()[i]>=data.bufferSize) {
					// Oops, we need to read more data...
					// Better resize this before we read other things in it...
					//
					data.resizeByteBuffer();
					
					// Also read another chunk of data, now that we have the space for it...
					// Ignore EOF, there might be other stuff in the buffer.
					//
					data.readBufferFromFile();
				}

				// The field is just start-end...
				if (data.endBuffer+fieldWidth>=data.bufferSize) {
					// still a problem?
					// We hit an EOF and are trying to read beyond the EOF...
					// Just take what's left for the current field.
					//
					fieldWidth=data.bufferSize-data.endBuffer;
					if (fieldWidth<0) fieldWidth=0;
				}
				byte[] field = new byte[fieldWidth];
				System.arraycopy(data.byteBuffer, data.startBuffer, field, 0, fieldWidth);
				
				if (doConversions) {
					if (meta.isLazyConversionActive()) {
						outputRowData[outputIndex++] = field;
					}
					else {
						// We're not lazy so we convert the data right here and now.
						//
						ValueMetaInterface targetValueMeta = data.outputRowMeta.getValueMeta(outputIndex);
						ValueMetaInterface sourceValueMeta = targetValueMeta.getStorageMetadata();
						
						outputRowData[outputIndex++] = targetValueMeta.convertData(sourceValueMeta, field);
					}
				}
				else {
					outputRowData[outputIndex++] = null; // nothing for the header, no conversions here.
				}
				
				// OK, onto the next field...
				// 
				data.endBuffer+=meta.getFieldWidth()[i];
				data.startBuffer=data.endBuffer;
			}
			
			// Now that we have all the data, see if there are any linefeed characters to remove from the buffer...
			//
			if (meta.isLineFeedPresent()) {
				
				while (data.byteBuffer[data.endBuffer]=='\n' || data.byteBuffer[data.endBuffer]=='\r') {

					data.endBuffer++;
					if (data.endBuffer>=data.bufferSize) {
						// Oops, we need to read more data...
						// Better resize this before we read other things in it...
						//
						data.resizeByteBuffer();
						
						// Also read another chunk of data, now that we have the space for it...
						data.readBufferFromFile();
					}
				}
			}
		
			linesInput++;
			return outputRowData;
		}
		catch (Exception e)
		{
			throw new KettleFileException("Exception reading line using NIO: " + e.toString());
		}

	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(FixedInputMeta)smi;
		data=(FixedInputData)sdi;
		
		if (super.init(smi, sdi)) {
			try {
				data.preferredBufferSize = Integer.parseInt(environmentSubstitute(meta.getBufferSize()));
				data.lineWidth = Integer.parseInt(environmentSubstitute(meta.getLineWidth()));
				data.filename = environmentSubstitute(meta.getFilename());
				
				FileObject fileObject = KettleVFS.getFileObject(data.filename);
				if (!(fileObject instanceof LocalFile)) {
					// We can only use NIO on local files at the moment, so that's what we limit ourselves to.
					//
					logError(Messages.getString("FixedInput.Log.OnlyLocalFilesAreSupported"));
					return false;
				}
				
				FileInputStream fis = (FileInputStream)((LocalFile)fileObject).getInputStream();
				data.fc = fis.getChannel();
				data.bb = ByteBuffer.allocateDirect( data.preferredBufferSize );
				
				data.stopReading = false;
								
				return true;
			} catch (IOException e) {
				logError("Error opening file '"+meta.getFilename()+"' : "+e.toString());
				logError(Const.getStackTracker(e));
			}
		}
		return false;
	}
	
	@Override
	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		
		try {
			if (data.fc!=null) {
				data.fc.close();
			}
		} catch (IOException e) {
			logError("Unable to close file channel for file '"+meta.getFilename()+"' : "+e.toString());
			logError(Const.getStackTracker(e));
		}
		
		super.dispose(smi, sdi);
	}
	
	//
	// Run is were the action happens!
	public void run()
	{
		try
		{
			logBasic(Messages.getString("FixedInput.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("FixedInput.Log.UnexpectedError")+" : "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            logError(Const.getStackTracker(e));
            setErrors(1);
			stopAll();
		}
		finally
		{
			dispose(meta, data);
			logSummary();
			markStop();
		}
	}
}
