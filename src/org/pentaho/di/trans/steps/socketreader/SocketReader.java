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
 
package org.pentaho.di.trans.steps.socketreader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Read data from a TCP/IP socket supplied by SocketWriter.
 * The data coming over the socket is one serialized Row object including metadata and then a series of serialized rows, data only. 
 * 
 * @author Matt
 * @since 01-dec-2006
 */

public class SocketReader extends BaseStep implements StepInterface
{
	public static final String STRING_FINISHED = "Finished";
    private static final int TIMEOUT_IN_SECONDS = 30;
    private SocketReaderMeta meta;
	private SocketReaderData data;
	
	public SocketReader(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(SocketReaderMeta)smi;
		data=(SocketReaderData)sdi;

        try
        {
            Object[] r;
            
            if (first)
            {
                // Connect to the server socket (started during init)
                // Because the accept() call on the server socket can be called after we reached this code
                // it is best to build in a retry loop with a time-out here.
                // 
                long startTime = System.currentTimeMillis();
                boolean connected=false;
                KettleException lastException=null;
                
                //// timeout with retry until connected
                while ( !connected && (TIMEOUT_IN_SECONDS > (System.currentTimeMillis()-startTime)/1000) && !isStopped())
                {
                    try
                    {
                        int port = Integer.parseInt( StringUtil.environmentSubstitute(meta.getPort()) );
                        int bufferSize = Integer.parseInt( StringUtil.environmentSubstitute(meta.getBufferSize()));
                        
                        data.socket = new Socket(StringUtil.environmentSubstitute(meta.getHostname()), port);
                        connected=true;

                        if (meta.isCompressed())
                        {
                            data.outputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(data.socket.getOutputStream()), bufferSize));
                            data.inputStream  = new DataInputStream(new BufferedInputStream(new GZIPInputStream(data.socket.getInputStream()), bufferSize));
                        }
                        else
                        {
                            data.outputStream = new DataOutputStream(new BufferedOutputStream(data.socket.getOutputStream(), bufferSize));
                            data.inputStream  = new DataInputStream(new BufferedInputStream(data.socket.getInputStream(), bufferSize));
                        }
                        lastException=null;
                    }
                    catch(Exception e)
                    {
                        lastException=new KettleException("Unable to open socket to server "+StringUtil.environmentSubstitute(meta.getHostname())+" port "+StringUtil.environmentSubstitute(meta.getPort()), e);
                    }
                    
                    if (lastException!=null) // Sleep for a second
                    {
                        Thread.sleep(1000);
                    }
                }
                
                if (lastException!=null)
                {
                    logError("Error initialising step: "+lastException.toString());
                    logError(Const.getStackTracker(lastException));
                    throw lastException;
                }
                else
                {
                    if (data.inputStream==null) throw new KettleException("Unable to connect to the SocketWriter in the "+TIMEOUT_IN_SECONDS+"s timeout period.");
                }
                
                
                data.rowMeta = new RowMeta(data.inputStream); // This is the metadata
                first=false;
            }
            r = data.rowMeta.readData(data.inputStream);
            
            linesInput++;
            
            if (checkFeedback(linesInput)) logBasic(Messages.getString("SocketReader.Log.LineNumber")+linesInput); //$NON-NLS-1$
            
            putRow(data.rowMeta, r);
        }
        catch(KettleEOFException e)
        {
            setOutputDone(); // finished reading.
            return false;
        }
        catch (Exception e)
        {
            throw new KettleException(e);
        }
        
		return true;
	}

    public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SocketReaderMeta)smi;
		data=(SocketReaderData)sdi;
		
		if (super.init(smi, sdi))
		{
            return true;
		}
		return false;
	}
    
    public void dispose(StepMetaInterface smi, StepDataInterface sdi)
    {
        // Ignore errors, we don't care
        // If we are here, it means all work is done
        // It's a lot of work to keep it all in sync for now we don't need to do that.
        // 
        try { data.inputStream.close(); } catch(Exception e) {}
        try { data.outputStream.close(); } catch(Exception e) {}
        try { data.socket.close(); } catch(Exception e) {}
        
        super.dispose(smi, sdi);
    }
	
	//
	// Run is were the action happens!
	public void run()
	{
		try
		{
			logBasic(Messages.getString("SocketReader.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("SocketReader.Log.UnexpectedError")+" : "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
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
