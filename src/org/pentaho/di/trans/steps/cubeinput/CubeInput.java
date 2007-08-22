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

/**
 * Reads a micro-cube type of data-file from disk.
 * It's just a binary (compressed) representation of a buch of rows.
 * 
 * @author Matt
 * @since 8-apr-2003
 */

package org.pentaho.di.trans.steps.cubeinput;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.zip.GZIPInputStream;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


public class CubeInput extends BaseStep implements StepInterface
{
	private CubeInputMeta meta;
	private CubeInputData data;
	
	public CubeInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(CubeInputMeta)smi;
		data=(CubeInputData)sdi;

		try
		{
            Object[] r = data.meta.readData(data.dis);
            putRow(data.meta, r);  // fill the rowset(s). (sleeps if full)
            linesInput++;
			
			if (meta.getRowLimit()>0 && linesInput>=meta.getRowLimit()) // finished!
			{
				setOutputDone();
				return false;
			}
		}
		catch(KettleEOFException eof)
		{
			setOutputDone();
			return false;
		} 
		catch (SocketTimeoutException e) 
		{
			throw new KettleException(e); // shouldn't happen on files
		}

        if (checkFeedback(linesInput)) logBasic(Messages.getString("CubeInput.Log.LineNumber")+linesInput); //$NON-NLS-1$

		return true;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(CubeInputMeta)smi;
		data=(CubeInputData)sdi;

		if (super.init(smi, sdi))
		{
			try
			{
				data.fis=KettleVFS.getInputStream(environmentSubstitute(meta.getFilename()));
				data.zip = new GZIPInputStream(data.fis);
				data.dis = new DataInputStream(data.zip);
				
				try
				{
					data.meta = new RowMeta(data.dis);
					return true;
				}
				catch(KettleFileException kfe)
				{
					logError(Messages.getString("CubeInput.Log.UnableToReadMetadata")+kfe.getMessage()); //$NON-NLS-1$
					return false;
				}
			}
			catch(IOException e)
			{
				logError(Messages.getString("CubeInput.Log.ErrorReadingFromDataCube")+e.toString()); //$NON-NLS-1$
			}
		}
		return false;
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (CubeInputMeta)smi;
	    data = (CubeInputData)sdi;
	    
		try
		{
			if(data.dis!=null)
			{
				data.dis.close();
				data.dis=null;
			}
			if(data.zip!=null)
			{
				data.zip.close();
				data.zip=null;
			}
			if(data.fis!=null)
			{
				data.fis.close();
				data.fis=null;
			}
		}
		catch(IOException e)
		{
			logError(Messages.getString("CubeInput.Log.ErrorClosingCube")+e.toString()); //$NON-NLS-1$
			setErrors(1);
			stopAll();
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