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
 

package org.pentaho.di.trans.steps.textfileoutput;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.StreamLogger;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Converts input rows to text and then writes this text to one or more files.
 * 
 * @author Matt
 * @since 4-apr-2003
 */
public class TextFileOutput extends BaseStep implements StepInterface
{
    private static final String FILE_COMPRESSION_TYPE_NONE = TextFileOutputMeta.fileCompressionTypeCodes[TextFileOutputMeta.FILE_COMPRESSION_TYPE_NONE];
    private static final String FILE_COMPRESSION_TYPE_ZIP  = TextFileOutputMeta.fileCompressionTypeCodes[TextFileOutputMeta.FILE_COMPRESSION_TYPE_ZIP];
    private static final String FILE_COMPRESSION_TYPE_GZIP = TextFileOutputMeta.fileCompressionTypeCodes[TextFileOutputMeta.FILE_COMPRESSION_TYPE_GZIP];
    
	private TextFileOutputMeta meta;
	private TextFileOutputData data;
	 
	public TextFileOutput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
    
	public synchronized boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(TextFileOutputMeta)smi;
		data=(TextFileOutputData)sdi;

		boolean result=true;
		boolean bEndedLineWrote=false;
		Object[] r=getRow();       // This also waits for a row to be finished.

        if (r!=null && first)
        {
            first=false;
            data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
            
            if (!meta.isFileAppended() && ( meta.isHeaderEnabled() || meta.isFooterEnabled())) // See if we have to write a header-line)
            {
                if (meta.isHeaderEnabled() && data.outputRowMeta!=null)
                {
                    writeHeader();
                }
            }
            
            data.fieldnrs=new int[meta.getOutputFields().length];
            for (int i=0;i<meta.getOutputFields().length;i++)
            {
                data.fieldnrs[i]=data.outputRowMeta.indexOfValue(meta.getOutputFields()[i].getName());
                if (data.fieldnrs[i]<0)
                {
                    throw new KettleStepException("Field ["+meta.getOutputFields()[i].getName()+"] couldn't be found in the input stream!");
                }
            }
        }

		if ( ( r==null && data.outputRowMeta!=null && meta.isFooterEnabled() ) ||
		     ( r!=null && linesOutput>0 && meta.getSplitEvery()>0 && ((linesOutput+1)%meta.getSplitEvery())==0)
		   )
		{
			if (data.outputRowMeta!=null) 
			{
			   if ( meta.isFooterEnabled() )
			   {
			      writeHeader();
			   }
			}
			
			if (r==null)
			{
				//add tag to last line if needed
				writeEndedLine();
				bEndedLineWrote=true;
			}
			// Done with this part or with everything.
			closeFile();
			
			// Not finished: open another file...
			if (r!=null)
			{
				if (!openNewFile())
				{
					logError("Unable to open new file (split #"+data.splitnr+"...");
					setErrors(1);
					return false;
				}

				if (meta.isHeaderEnabled() && data.outputRowMeta!=null) if (writeHeader()) linesOutput++;
			}
		}
		
		if (r==null)  // no more input to be expected...
		{
			if (false==bEndedLineWrote)
			{
				//add tag to last line if needed
				writeEndedLine();
				bEndedLineWrote=true;
			}
			
			setOutputDone();
			return false;
		}
		
		writeRowToFile(data.outputRowMeta, r);
		putRow(data.outputRowMeta, r);       // in case we want it to go further...
		
        if (checkFeedback(linesOutput)) logBasic("linenr "+linesOutput);
		
		return result;
	}

	private void writeRowToFile(RowMetaInterface rowMeta, Object[] r) throws KettleStepException
	{
		try
		{	
			if (meta.getOutputFields()==null || meta.getOutputFields().length==0)
			{
				/*
				 * Write all values in stream to text file.
				 */
				for (int i=0;i<rowMeta.size();i++)
				{
					if (i>0 && meta.getSeparator()!=null && meta.getSeparator().length()>0)
                    {
						data.writer.write(data.binarySeparator);
                    }
					ValueMetaInterface v=rowMeta.getValueMeta(i);
                    Object valueData = r[i];
                    
					writeField(v, valueData);
				}
                data.writer.write(data.binaryNewline);
			}
			else
			{
				/*
				 * Only write the fields specified!
				 */
				for (int i=0;i<meta.getOutputFields().length;i++)
				{
					if (i>0 && meta.getSeparator()!=null && meta.getSeparator().length()>0)
						data.writer.write(data.binarySeparator);
	
					ValueMetaInterface v = rowMeta.getValueMeta(data.fieldnrs[i]);
					Object valueData = r[data.fieldnrs[i]];
					writeField(v, valueData);
				}
                data.writer.write(data.binaryNewline);
			}

            linesOutput++;
            
            // flush every 4k lines
            // if (linesOutput>0 && (linesOutput&0xFFF)==0) data.writer.flush();
		}
		catch(Exception e)
		{
			throw new KettleStepException("Error writing line", e);
		}
	}

    private byte[] formatField(ValueMetaInterface v, Object valueData) throws KettleValueException
    {
    	if( v.isString() )
    	{
    		if( valueData instanceof String ) 
    		{
        		return convertStringToBinaryString( v, (String) valueData );
    		} else {
        		return convertStringToBinaryString( v, v.getString( valueData ) );
    		}
    	} else {
            return v.getBinaryString(valueData);
    	}
    }
    

    private byte[] convertStringToBinaryString(ValueMetaInterface v, String string) throws KettleValueException
    {
    	int length = v.getLength();
    	
    	if( length > -1 && length < string.length() ) {
    		// we need to truncate
    		String tmp = string.substring(0, length);
            if (Const.isEmpty(v.getStringEncoding()))
            {
            	return tmp.getBytes();
            }
            else
            {
                try
                {
                	return tmp.getBytes(v.getStringEncoding());
                }
                catch(UnsupportedEncodingException e)
                {
                    throw new KettleValueException("Unable to convert String to Binary with specified string encoding ["+v.getStringEncoding()+"]", e);
                }
            }
    	}
    	else {
    		byte text[];
            if (Const.isEmpty(v.getStringEncoding()))
            {
            	text = string.getBytes();
            }
            else
            {
                try
                {
                	text = string.getBytes(v.getStringEncoding());
                }
                catch(UnsupportedEncodingException e)
                {
                    throw new KettleValueException("Unable to convert String to Binary with specified string encoding ["+v.getStringEncoding()+"]", e);
                }
            }
        	if( length > string.length() ) 
        	{
        		// we need to pad this
        		byte filler[] = " ".getBytes();
        		int size = filler.length*length;
        		byte bytes[] = new byte[size];
        		if( filler.length == 1 ) {
            		java.util.Arrays.fill( bytes, filler[0] );
        		} 
        		else 
        		{
        			// need to copy the filler array in lots of times
        		}
        		System.arraycopy( text, 0, bytes, 0, text.length );
        		return bytes;
        	}
        	else
        	{
        		// do not need to pad or truncate
        		return text;
        	}
    	}
    }
    
    private byte[] getBinaryString(String string) throws KettleStepException {
    	try {
    		if (data.hasEncoding) {
        		return string.getBytes(meta.getEncoding());
    		}
    		else {
        		return string.getBytes();
    		}
    	}
    	catch(Exception e) {
    		throw new KettleStepException(e);
    	}
    }
    
    private void writeField(ValueMetaInterface v, Object valueData) throws KettleStepException
    {
        try
        {
        	byte[] str;
        	if (meta.isFastDump()) {
        		if( valueData instanceof byte[] )
        		{
            		str = (byte[]) valueData;
        		} else {
            		str = getBinaryString(valueData.toString());
        		}
        	}
        	else {
        		str = formatField(v, valueData);
        	}
    		if (str!=null) data.writer.write(str);
        }
        catch(Exception e)
        {
            throw new KettleStepException("Error writing field content to file", e);
        }
    }

	private boolean writeEndedLine()
	{
		boolean retval=false;
		try
		{
			String sLine = meta.getEndedLine();
			if (sLine!=null)
			{
				if (sLine.trim().length()>0)
				{
					data.writer.write(getBinaryString(sLine));
					linesOutput++;
				}
			}
		}
		catch(Exception e)
		{
			logError("Error writing ended tag line: "+e.toString());
            logError(Const.getStackTracker(e));
			retval=true;
		}
		
		return retval;
	}
	
	private boolean writeHeader()
	{
		boolean retval=false;
		RowMetaInterface r=data.outputRowMeta;
		
		try
		{
			// If we have fields specified: list them in this order!
			if (meta.getOutputFields()!=null && meta.getOutputFields().length>0)
			{
				String header = "";
				for (int i=0;i<meta.getOutputFields().length;i++)
				{
                    String fieldName = meta.getOutputFields()[i].getName();
                    ValueMetaInterface v = r.searchValueMeta(fieldName);
                    
					if (i>0 && meta.getSeparator()!=null && meta.getSeparator().length()>0)
					{
						header+=meta.getSeparator();
					}
                    if (meta.isEnclosureForced() && meta.getEnclosure()!=null && v!=null && v.isString())
                    {
                        header+=meta.getEnclosure();
                    }
					header+=fieldName;
                    if (meta.isEnclosureForced() && meta.getEnclosure()!=null && v!=null && v.isString())
                    {
                        header+=meta.getEnclosure();
                    }
				}
				header+=meta.getNewline();
                data.writer.write(getBinaryString(header));
			}
			else
			if (r!=null)  // Just put all field names in the header/footer
			{
				for (int i=0;i<r.size();i++)
				{
					if (i>0 && meta.getSeparator()!=null && meta.getSeparator().length()>0)
						data.writer.write(data.binarySeparator);
					ValueMetaInterface v = r.getValueMeta(i);
					
                    // Header-value contains the name of the value
					ValueMetaInterface header_value = new ValueMeta(v.getName(), ValueMetaInterface.TYPE_STRING);

                    if (meta.isEnclosureForced() && meta.getEnclosure()!=null && v.isString())
                    {
                        data.writer.write(data.binaryEnclosure);
                    }
                    data.writer.write(getBinaryString(header_value.getName()));
                    if (meta.isEnclosureForced() && meta.getEnclosure()!=null && v.isString())
                    {
                        data.writer.write(data.binaryEnclosure);
                    }
				}
                data.writer.write(data.binaryNewline);
			}
			else
			{
                data.writer.write(getBinaryString("no rows selected"+Const.CR));
			}
		}
		catch(Exception e)
		{
			logError("Error writing header line: "+e.toString());
            logError(Const.getStackTracker(e));
			retval=true;
		}
		linesOutput++;
		return retval;
	}

	public String buildFilename(boolean ziparchive)
	{
		return meta.buildFilename(this, getCopy(), getPartitionID(), data.splitnr, ziparchive);
	}
	
	public boolean openNewFile()
	{
		boolean retval=false;
		data.writer=null;
		
		try
		{
            if (meta.isFileAsCommand())
            {
            	logDebug("Spawning external process");
            	if (data.cmdProc != null)
            	{
            		logError("Previous command not correctly terminated");
            		setErrors(1);
            	}
            	String cmdstr = environmentSubstitute(meta.getFileName());
            	if (Const.getOS().equals("Windows 95"))
                {
            		cmdstr = "command.com /C " + cmdstr;
                }
            	else
                {
                    if (Const.getOS().startsWith("Windows"))
                    {
                        cmdstr = "cmd.exe /C " + cmdstr;
                    }
                }
            	logDetailed("Starting: " + cmdstr);
            	Runtime r = Runtime.getRuntime();
            	data.cmdProc = r.exec(cmdstr, EnvUtil.getEnvironmentVariablesForRuntimeExec());
            	data.writer = data.cmdProc.getOutputStream();
            	StreamLogger stdoutLogger = new StreamLogger( data.cmdProc.getInputStream(), "(stdout)" );
            	StreamLogger stderrLogger = new StreamLogger( data.cmdProc.getErrorStream(), "(stderr)" );
            	new Thread(stdoutLogger).start();
            	new Thread(stderrLogger).start();
            	retval = true;
            }
            else
            {
                String filename = buildFilename(true);
                
				// Add this to the result file names...
				ResultFile resultFile = new ResultFile(ResultFile.FILE_TYPE_GENERAL, KettleVFS.getFileObject(filename), getTransMeta().getName(), getStepname());
				resultFile.setComment("This file was created with a text file output step");
	            addResultFile(resultFile);
	
	            OutputStream outputStream;
                
                if (!Const.isEmpty(meta.getFileCompression()) && !meta.getFileCompression().equals(FILE_COMPRESSION_TYPE_NONE))
                {
    				if (meta.getFileCompression().equals(FILE_COMPRESSION_TYPE_ZIP))
    				{
    		            log.logDetailed(toString(), "Opening output stream in zipped mode");
                        
                        data.fos = KettleVFS.getOutputStream(filename, meta.isFileAppended());
                        data.zip = new ZipOutputStream(data.fos);
    					File entry = new File(buildFilename(false));
    					ZipEntry zipentry = new ZipEntry(entry.getName());
    					zipentry.setComment("Compressed by Kettle");
    					data.zip.putNextEntry(zipentry);
    					outputStream=data.zip;
    				}
    				else if (meta.getFileCompression().equals(FILE_COMPRESSION_TYPE_GZIP))
    				{
    		            log.logDetailed(toString(), "Opening output stream in gzipped mode");
                        data.fos = KettleVFS.getOutputStream(filename, meta.isFileAppended());
                        data.gzip = new GZIPOutputStream(data.fos);
    					outputStream=data.gzip;
    				}
                    else
                    {
                        throw new KettleFileException("No compression method specified!");
                    }
                }
				else
				{
		            log.logDetailed(toString(), "Opening output stream in nocompress mode");
                    data.fos = KettleVFS.getOutputStream(filename, meta.isFileAppended());
                    outputStream=data.fos;
				}
                
	            if (!Const.isEmpty(meta.getEncoding()))
	            {
	                log.logBasic(toString(), "Opening output stream in encoding: "+meta.getEncoding());
	                data.writer = new BufferedOutputStream(outputStream, 5000);
	            }
	            else
	            {
	                log.logBasic(toString(), "Opening output stream in default encoding");
	                data.writer = new BufferedOutputStream(outputStream, 5000);
	            }
	
	            logDetailed("Opened new file with name ["+filename+"]");
				
				retval=true;
            }
		}
		catch(Exception e)
		{
			logError("Error opening new file : "+e.toString());
		}
		// System.out.println("end of newFile(), splitnr="+splitnr);

		data.splitnr++;

		return retval;
	}
	
	private boolean closeFile()
	{
		boolean retval=false;
		
		try
		{			
			if ( data.writer != null )
			{
				logDebug("Closing output stream");
			    data.writer.close();
			    logDebug("Closed output stream");
			}			
			data.writer = null;
			if (data.cmdProc != null)
			{
				logDebug("Ending running external command");
				int procStatus = data.cmdProc.waitFor();
				data.cmdProc = null;
				logBasic("Command exit status: " + procStatus);
			}
			else
			{
				logDebug("Closing normal file ...");
				if (meta.getFileCompression() == "Zip")
				{
					data.zip.closeEntry();
					data.zip.finish();
					data.zip.close();
				}
				if (meta.getFileCompression() == "GZip")
				{
					data.gzip.finish();
				}
                if (data.fos!=null)
                {
                    data.fos.close();
                    data.fos=null;
                }
			}

			retval=true;
		}
		catch(Exception e)
		{
			logError("Exception trying to close file: " + e.toString());
			setErrors(1);
			retval = false;
		}

		return retval;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(TextFileOutputMeta)smi;
		data=(TextFileOutputData)sdi;

		if (super.init(smi, sdi))
		{
			data.splitnr=0;
			
			if (openNewFile())
			{
				try {
					data.hasEncoding = !Const.isEmpty(meta.getEncoding());
					data.binarySeparator = new byte[] {};
					data.binaryEnclosure = new byte[] {};
					data.binaryNewline   = new byte[] {};
					
					if (data.hasEncoding) {
						if (!Const.isEmpty(meta.getSeparator())) data.binarySeparator= meta.getSeparator().getBytes(meta.getEncoding());
						if (!Const.isEmpty(meta.getEnclosure())) data.binaryEnclosure = meta.getEnclosure().getBytes(meta.getEncoding());
						if (!Const.isEmpty(meta.getNewline()))   data.binaryNewline   = meta.getNewline().getBytes(meta.getEncoding());
					}
					else {
						if (!Const.isEmpty(meta.getSeparator())) data.binarySeparator= meta.getSeparator().getBytes();
						if (!Const.isEmpty(meta.getEnclosure())) data.binaryEnclosure = meta.getEnclosure().getBytes();
						if (!Const.isEmpty(meta.getNewline()))   data.binaryNewline   = meta.getNewline().getBytes();
					}
					
					
				} catch (UnsupportedEncodingException e) {
					logError("Encoding problem: "+e.toString());
					logError(Const.getStackTracker(e));
					return false;
				}
				
				return true;
			}
			else
			{
				logError("Couldn't open file "+meta.getFileName());
				setErrors(1L);
				stopAll();
			}
		}
		return false;
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(TextFileOutputMeta)smi;
		data=(TextFileOutputData)sdi;
		
		closeFile();

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