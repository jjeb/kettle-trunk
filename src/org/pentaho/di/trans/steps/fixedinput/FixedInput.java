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
		
		Object[] outputRowData=readOneRow(true);
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
			
	        // See if we need to call it a day...
	        //
	        if (meta.isRunningInParallel()) {
	        	if (linesInput>=data.rowsToRead) {
	        		return null; // We're done.  The rest is for the other steps in the cluster
	        	}
	        }

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
			
			for (int i=0;i<meta.getFieldDefinition().length;i++) {
				
				FixedFileInputField fieldDefinition = meta.getFieldDefinition()[i];
				
				int fieldWidth = fieldDefinition.getWidth();
				data.endBuffer = data.startBuffer+fieldWidth; 
				if (data.endBuffer>data.bufferSize) {
					// Oops, we need to read more data...
					// Better resize this before we read other things in it...
					//
					data.resizeByteBuffer();
					
					// Also read another chunk of data, now that we have the space for it...
					// Ignore EOF, there might be other stuff in the buffer.
					//
					data.readBufferFromFile();
				}

				// re-verify the buffer after we tried to read extra data from file...
				//
				if (data.endBuffer>data.bufferSize) {
					// still a problem?
					// We hit an EOF and are trying to read beyond the EOF...
					// Just take what's left for the current field.
					//
					fieldWidth=data.endBuffer-data.bufferSize;
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
				data.startBuffer=data.endBuffer;
			}
			
			// Now that we have all the data, see if there are any linefeed characters to remove from the buffer...
			//
			if (meta.isLineFeedPresent()) {

				data.endBuffer+=2;
				
				if (data.endBuffer>=data.bufferSize) {
					// Oops, we need to read more data...
					// Better resize this before we read other things in it...
					//
					data.resizeByteBuffer();
					
					// Also read another chunk of data, now that we have the space for it...
					data.readBufferFromFile();
				}

				// CR + Line feed in the worst case.
				//
				if (data.byteBuffer[data.startBuffer]=='\n' || data.byteBuffer[data.startBuffer]=='\r') {

					data.startBuffer++;

					if (data.byteBuffer[data.startBuffer]=='\n' || data.byteBuffer[data.startBuffer]=='\r') {

						data.startBuffer++;
					}
				}
				data.endBuffer = data.startBuffer;
			}
		
			linesInput++;
			return outputRowData;
		}
		catch (Exception e)
		{
			throw new KettleFileException("Exception reading line using NIO: " + e.toString(), e);
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
				try
				{
					FileInputStream fileInputStream = KettleVFS.getFileInputStream(fileObject);
					data.fc = fileInputStream.getChannel();
					data.bb = ByteBuffer.allocateDirect( data.preferredBufferSize );
				}
				catch(IOException e) {
					logError(e.toString());
					return false;
				}
				
				data.stopReading = false;
				
				if (meta.isRunningInParallel()) {
					data.stepNumber = getUniqueStepNrAcrossSlaves();
					data.totalNumberOfSteps = getUniqueStepCountAcrossSlaves();
		            data.fileSize = fileObject.getContent().getSize();
				}
				
				// OK, now we need to skip a number of bytes in case we're doing a parallel read.
				//
				if (meta.isRunningInParallel()) {
					
	                long nrRows = data.fileSize / data.lineWidth; // 100.000 / 100 = 1000 rows
	                long rowsToSkip = Math.round( data.stepNumber * nrRows / (double)data.totalNumberOfSteps );  // 0, 333, 667
	                long nextRowsToSkip = Math.round( (data.stepNumber+1) * nrRows / (double)data.totalNumberOfSteps );  // 333, 667, 1000
	                data.rowsToRead = nextRowsToSkip - rowsToSkip;
	                long bytesToSkip = rowsToSkip*data.lineWidth;
	             
	                logBasic("Step #"+data.stepNumber+" is skipping "+bytesToSkip+" to position in file, then it's reading "+data.rowsToRead+" rows.");

                    data.fc.position(bytesToSkip);
				}
								
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
			logBasic(Messages.getString("System.Log.StartingToRun")); //$NON-NLS-1$
			
			while (processRow(meta, data) && !isStopped());
		}
		catch(Throwable t)
		{
			logError(Messages.getString("System.Log.UnexpectedError")+" : "+t.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            logError(Const.getStackTracker(t));
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