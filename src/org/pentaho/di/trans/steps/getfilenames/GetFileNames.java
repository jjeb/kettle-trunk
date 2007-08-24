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

package org.pentaho.di.trans.steps.getfilenames;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Read all sorts of text files, convert them to rows and writes these to one or more output streams.
 * 
 * @author Matt
 * @since 4-apr-2003
 */
public class GetFileNames extends BaseStep implements StepInterface
{
    private GetFileNamesMeta meta;

    private GetFileNamesData data;

    public GetFileNames(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
    {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
    {
        if (data.filenr >= data.files.nrOfFiles())
        {
            setOutputDone();
            return false;
        }
        
        if (first)
        {
            first = false;
            data.outputRowMeta = new RowMeta();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
        }

        try
        {
            Object[] outputRow = new Object[data.outputRowMeta.size()];
            int outputIndex = 0;

            FileObject file = data.files.getFile(data.filenr);

            if (meta.getFilterFileType().equals("all_files") || (meta.getFilterFileType().equals("only_files") && file.getType() == FileType.FILE)
                    || meta.getFilterFileType().equals("only_folders") && file.getType() == FileType.FOLDER)
            {

                // filename
                outputRow[outputIndex++] = KettleVFS.getFilename(file);

                // short_filename
                outputRow[outputIndex++] = file.getName().getBaseName();

                try
                {
                    // path
                    outputRow[outputIndex++] = KettleVFS.getFilename(file.getParent());

                    // type
                    outputRow[outputIndex++] = file.getType().toString();

                    // exists
                    outputRow[outputIndex++] = new Boolean(file.exists());

                    // ishidden
                    outputRow[outputIndex++] = new Boolean(file.isHidden());

                    // isreadable
                    outputRow[outputIndex++] = new Boolean(file.isReadable());

                    // iswriteable
                    outputRow[outputIndex++] = new Boolean(file.isWriteable());

                    // lastmodifiedtime
                    outputRow[outputIndex++] = new Date( file.getContent().getLastModifiedTime() );

                    // size
                    Long size = null;
                    if (file.getType().equals(FileType.FILE))
                    {
                        size = new Long( file.getContent().getSize() );
                    }
                    outputRow[outputIndex++] = size;
                }
                catch (IOException e)
                {
                    throw new KettleException(e);
                }

                // extension
                outputRow[outputIndex++] = file.getName().getExtension();

                // uri
                outputRow[outputIndex++] = file.getName().getURI();

                // rooturi
                outputRow[outputIndex++] = file.getName().getRootURI();
                
                putRow(data.outputRowMeta, outputRow);
            }
        }
        catch (Exception e)
        {
            log.logError(toString(), "Error exception: " + e.getMessage());
        }

        data.filenr++;

        if ((linesInput > 0) && (linesInput % Const.ROWS_UPDATE) == 0) logBasic("linenr " + linesInput);

        return true;
    }

    private void handleMissingFiles() throws KettleException
    {
        List<FileObject> nonExistantFiles = data.files.getNonExistantFiles();

        if (nonExistantFiles.size() != 0)
        {
            String message = FileInputList.getRequiredFilesDescription(nonExistantFiles);
            logBasic("ERROR: Missing " + message);
            throw new KettleException("Following required files are missing: " + message);
        }

        List<FileObject> nonAccessibleFiles = data.files.getNonAccessibleFiles();
        if (nonAccessibleFiles.size() != 0)
        {
            String message = FileInputList.getRequiredFilesDescription(nonAccessibleFiles);
            logBasic("WARNING: Not accessible " + message);
            throw new KettleException("Following required files are not accessible: " + message);
        }
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi)
    {
        meta = (GetFileNamesMeta) smi;
        data = (GetFileNamesData) sdi;

        if (super.init(smi, sdi))
        {
            try
            {
                data.files = meta.getTextFileList(this);
                handleMissingFiles();

                return true;
            }
            catch (Exception e)
            {
                logError("Error initializing step: " + e.toString());
                logError(Const.getStackTracker(e));
                return false;
            }
        }
        return false;
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi)
    {
        meta = (GetFileNamesMeta) smi;
        data = (GetFileNamesData) sdi;

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
        	logError(Messages.getString("System.Log.UnexpectedError")+" : "); //$NON-NLS-1$ //$NON-NLS-2$
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