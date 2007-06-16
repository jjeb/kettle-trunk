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

package be.ibridge.kettle.trans.step.cubeinput;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.exception.KettleEOFException;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleFileException;
import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransMeta;
import be.ibridge.kettle.trans.step.BaseStep;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.StepInterface;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;


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
            Row r;
            if (first)
            {
                first=false;
                r= data.meta;
            }
            else
            {
    			r = new Row(data.dis, data.meta.size(), data.meta);
    			
            }
            putRow(r);  // fill the rowset(s). (sleeps if full)
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
				File f = new File(meta.getFilename());
				data.fis = new GZIPInputStream(new FileInputStream(f));
				data.dis = new DataInputStream(data.fis);
				
				try
				{
					data.meta = CubeInputMeta.getMetaData(data.dis);
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
			data.dis.close();
			data.fis.close();
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
	//
	public void run()
	{
		try
		{
			logBasic(Messages.getString("CubeInput.Log.StartingToRun")); //$NON-NLS-1$
			while (!isStopped() && processRow(meta, data) );
		}
		catch(Exception e)
		{
			logError(Messages.getString("CubeInput.Log.UnexpectedError")+" : "+e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            logError(Const.getStackTracker(e));
            setErrors(1);
			stopAll();
		}
		finally
		{
		    dispose(meta, data);
			markStop();
		    logSummary();
		}
	}
}
