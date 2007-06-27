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
 
package org.pentaho.di.trans.steps.blockingstep;

import java.io.File;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;


public class BlockingStepMeta  extends BaseStepMeta implements StepMetaInterface {
	
    /** Directory to store the temp files */
    private String  directory;

    /** Temp files prefix... */
    private String  prefix;

    /** The cache size: number of rows to keep in memory */
    private int     cacheSize;

    /**
     * Compress files: if set to true, temporary files are compressed, thus reducing I/O at the cost of slightly higher
     * CPU usage
     */
    private boolean compressFiles;

    /**
     * Pass all rows, or only the last one. Only the last row was the original behaviour.
     */
	private boolean passAllRows;

	/**
	 * Cache size: how many rows do we keep in memory
	 */
	public static final int CACHE_SIZE = 5000;	
		   
	public void check(List<CheckResult> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
    {
        CheckResult cr;
        
        if (prev != null && prev.size() > 0)
        {
            // Check the sort directory
            String realDirectory = transMeta.environmentSubstitute(directory);

            File f = new File(realDirectory);
            if (f.exists())
            {
                if (f.isDirectory())
                {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SortRowsMeta.CheckResult.DirectoryExists", realDirectory),
                            stepMeta);
                    remarks.add(cr);
                }
                else
                {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SortRowsMeta.CheckResult.ExistsButNoDirectory",
                            realDirectory), stepMeta);
                    remarks.add(cr);
                }
            }
            else
            {
                cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SortRowsMeta.CheckResult.DirectoryNotExists", realDirectory),
                        stepMeta);
                remarks.add(cr);
            }
        }
        else
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SortRowsMeta.CheckResult.NoFields"), stepMeta);
            remarks.add(cr);
        }

        // See if we have input streams leading to this step!
        if (input.length>0) 
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("BlockingStepMeta.CheckResult.StepExpectingRowsFromOtherSteps"), stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        } 
        else 
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("BlockingStepMeta.CheckResult.NoInputReceivedError"), stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        }
    }
    
    public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String stepname) {
        return new BlockingStepDialog(shell, info, transMeta, stepname);
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        return new BlockingStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    public StepDataInterface getStepData() {
        return new BlockingStepData();
    }

    public void loadXML(Node stepnode, List<? extends SharedObjectInterface> databases, Hashtable counters) throws KettleXMLException {
        readData(stepnode);        
    }

    private void readData(Node stepnode)
    {
       	passAllRows   = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "pass_all_rows"));
        directory     = XMLHandler.getTagValue(stepnode, "directory");
        prefix        = XMLHandler.getTagValue(stepnode, "prefix");
        cacheSize     = Const.toInt(XMLHandler.getTagValue(stepnode, "cache_size"), CACHE_SIZE);
        compressFiles = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "compress"));
    }    
    
    public void setDefault() {
    	passAllRows   = false;    	
        directory     = "%%java.io.tmpdir%%";
        prefix        = "block";
        cacheSize     = CACHE_SIZE;
        compressFiles = true;    	
    }
    
    public String getXML()
    {
        StringBuffer retval = new StringBuffer(300);

        retval.append("      ").append(XMLHandler.addTagValue("pass_all_rows", passAllRows));
        retval.append("      ").append(XMLHandler.addTagValue("directory",     directory));
        retval.append("      ").append(XMLHandler.addTagValue("prefix",        prefix));
        retval.append("      ").append(XMLHandler.addTagValue("cache_size",    cacheSize));
        retval.append("      ").append(XMLHandler.addTagValue("compress",      compressFiles));

        return retval.toString();
    }    
    
    public void readRep(Repository rep, long id_step, List<? extends SharedObjectInterface> databases, Hashtable counters) throws KettleException {
        try
        {
        	passAllRows   =       rep.getStepAttributeBoolean(id_step, "pass_all_rows");
            directory     =       rep.getStepAttributeString(id_step,  "directory");
            prefix        =       rep.getStepAttributeString(id_step,  "prefix");
            cacheSize     = (int) rep.getStepAttributeInteger(id_step, "cache_size");
            compressFiles =       rep.getStepAttributeBoolean(id_step, "compress");
            if (cacheSize == 0) cacheSize = CACHE_SIZE;
        }
        catch (Exception e)
        {
            throw new KettleException("Unexpected error reading step information from the repository", e);
        }    	
    }

    public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException {
        try
        {
        	rep.saveStepAttribute(id_transformation, id_step, "pass_all_rows", passAllRows);
            rep.saveStepAttribute(id_transformation, id_step, "directory",     directory);
            rep.saveStepAttribute(id_transformation, id_step, "prefix",        prefix);
            rep.saveStepAttribute(id_transformation, id_step, "cache_size",    cacheSize);
            rep.saveStepAttribute(id_transformation, id_step, "compress",      compressFiles);
        }
        catch (Exception e)
        {
            throw new KettleException("Unable to save step information to the repository for id_step=" + id_step, e);
        }    	
    }

    /**
     * @return Returns the cacheSize.
     */
    public int getCacheSize()
    {
        return cacheSize;
    }

    /**
     * @param cacheSize The cacheSize to set.
     */
    public void setCacheSize(int tmpSize)
    {
        this.cacheSize = tmpSize;
    }
    
    /**
     * @return Returns the prefix.
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @param prefix The prefix to set.
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }
    
    /**
     * @return Returns whether temporary files should be compressed
     */
    public boolean getCompress()
    {
        return compressFiles;
    }

    /**
     * @param compressFiles Whether to compress temporary files created during sorting
     */
    public void setCompress(boolean compressFiles)
    {
        this.compressFiles = compressFiles;
    }    
 
    /**
     * @param Returns where all rows are passed or only the last one. 
     */
	public boolean isPassAllRows() 
	{
		return passAllRows;
	}

	/**
	 *  @param passAllRows Whether to pass all rows or only the last one.
	 */
	public void setPassAllRows(boolean passAllRows) 
	{
		this.passAllRows = passAllRows;
	}

	/** 
	 * @return The directory to store the temporary files in.
	 */
	public String getDirectory() 
	{
		return directory;
	}

	/**
	 * Set the directory to store the temp files in.
	 */
	public void setDirectory(String directory) 
	{
		this.directory = directory;
	}
}