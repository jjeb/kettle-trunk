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

package org.pentaho.di.trans.steps.sort;

import java.io.File;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
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


/*
 * Created on 02-jun-2003
 * 
 */

public class SortRowsMeta extends BaseStepMeta implements StepMetaInterface
{
    /** order by which fields? */
    private String  fieldName[];

    /** false : descending, true=ascending */
    private boolean ascending[];

    /** Directory to store the temp files */
    private String  directory;

    /** Temp files prefix... */
    private String  prefix;

    /** The sort size: number of rows sorted and kept in memory */
    private int     sortSize;

    /** The sort size: number of rows sorted and kept in memory */
    private boolean onlyPassingUniqueRows;

    /**
     * Compress files: if set to true, temporary files are compressed, thus reducing I/O at the cost of slightly higher
     * CPU usage
     */
    private boolean compressFiles;

    public SortRowsMeta()
    {
        super(); // allocate BaseStepMeta
    }

    /**
     * @return Returns the ascending.
     */
    public boolean[] getAscending()
    {
        return ascending;
    }

    /**
     * @param ascending The ascending to set.
     */
    public void setAscending(boolean[] ascending)
    {
        this.ascending = ascending;
    }

    /**
     * @return Returns the directory.
     */
    public String getDirectory()
    {
        return directory;
    }

    /**
     * @param directory The directory to set.
     */
    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    /**
     * @return Returns the fieldName.
     */
    public String[] getFieldName()
    {
        return fieldName;
    }

    /**
     * @param fieldName The fieldName to set.
     */
    public void setFieldName(String[] fieldName)
    {
        this.fieldName = fieldName;
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

    public void loadXML(Node stepnode, List<? extends SharedObjectInterface> databases, Hashtable counters) throws KettleXMLException
    {
        readData(stepnode);
    }

    public void allocate(int nrfields)
    {
        fieldName = new String[nrfields]; // order by
        ascending = new boolean[nrfields];
    }

    public Object clone()
    {
        SortRowsMeta retval = (SortRowsMeta) super.clone();

        int nrfields = fieldName.length;

        retval.allocate(nrfields);

        for (int i = 0; i < nrfields; i++)
        {
            retval.fieldName[i] = fieldName[i];
            retval.ascending[i] = ascending[i];
        }

        return retval;
    }

    private void readData(Node stepnode) throws KettleXMLException
    {
        try
        {
            directory = XMLHandler.getTagValue(stepnode, "directory");
            prefix = XMLHandler.getTagValue(stepnode, "prefix");
            sortSize = Const.toInt(XMLHandler.getTagValue(stepnode, "sort_size"), Const.SORT_SIZE);
            compressFiles = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "compress"));
            onlyPassingUniqueRows = "Y".equalsIgnoreCase( XMLHandler.getTagValue(stepnode, "unique_rows") );

            Node fields = XMLHandler.getSubNode(stepnode, "fields");
            int nrfields = XMLHandler.countNodes(fields, "field");

            allocate(nrfields);

            for (int i = 0; i < nrfields; i++)
            {
                Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);

                fieldName[i] = XMLHandler.getTagValue(fnode, "name");
                String asc = XMLHandler.getTagValue(fnode, "ascending");
                if (asc.equalsIgnoreCase("Y"))
                    ascending[i] = true;
                else
                    ascending[i] = false;
            }
        }
        catch (Exception e)
        {
            throw new KettleXMLException("Unable to load step info from XML", e);
        }
    }

    public void setDefault()
    {
        directory = "%%java.io.tmpdir%%";
        prefix = "out";
        sortSize = Const.SORT_SIZE;
        compressFiles = false;
        onlyPassingUniqueRows = false;

        int nrfields = 0;

        allocate(nrfields);

        for (int i = 0; i < nrfields; i++)
        {
            fieldName[i] = "field" + i;
        }
    }

    public String getXML()
    {
        StringBuffer retval = new StringBuffer();

        retval.append("      " + XMLHandler.addTagValue("directory", directory));
        retval.append("      " + XMLHandler.addTagValue("prefix", prefix));
        retval.append("      " + XMLHandler.addTagValue("sort_size", sortSize));
        retval.append("      " + XMLHandler.addTagValue("compress", compressFiles));
        retval.append("      " + XMLHandler.addTagValue("unique_rows", onlyPassingUniqueRows));

        retval.append("    <fields>" + Const.CR);
        for (int i = 0; i < fieldName.length; i++)
        {
            retval.append("      <field>" + Const.CR);
            retval.append("        " + XMLHandler.addTagValue("name", fieldName[i]));
            retval.append("        " + XMLHandler.addTagValue("ascending", ascending[i]));
            retval.append("        </field>" + Const.CR);
        }
        retval.append("      </fields>" + Const.CR);

        return retval.toString();
    }

    public void readRep(Repository rep, long id_step, List<? extends SharedObjectInterface> databases, Hashtable counters) throws KettleException
    {
        try
        {
            directory = rep.getStepAttributeString(id_step, "directory");
            prefix = rep.getStepAttributeString(id_step, "prefix");
            sortSize = (int) rep.getStepAttributeInteger(id_step, "sort_size");
            compressFiles = rep.getStepAttributeBoolean(id_step, "compress");
            if (sortSize == 0) sortSize = Const.SORT_SIZE;
            onlyPassingUniqueRows = rep.getStepAttributeBoolean(id_step, "unique_rows");

            int nrfields = rep.countNrStepAttributes(id_step, "field_name");

            allocate(nrfields);

            for (int i = 0; i < nrfields; i++)
            {
                fieldName[i] = rep.getStepAttributeString(id_step, i, "field_name");
                ascending[i] = rep.getStepAttributeBoolean(id_step, i, "field_ascending");
            }
        }
        catch (Exception e)
        {
            throw new KettleException("Unexpected error reading step information from the repository", e);
        }
    }

    public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
    {
        try
        {
            rep.saveStepAttribute(id_transformation, id_step, "directory", directory);
            rep.saveStepAttribute(id_transformation, id_step, "prefix", prefix);
            rep.saveStepAttribute(id_transformation, id_step, "sort_size", sortSize);
            rep.saveStepAttribute(id_transformation, id_step, "compress", compressFiles);
            rep.saveStepAttribute(id_transformation, id_step, "unique_rows", onlyPassingUniqueRows);

            for (int i = 0; i < fieldName.length; i++)
            {
                rep.saveStepAttribute(id_transformation, id_step, i, "field_name", fieldName[i]);
                rep.saveStepAttribute(id_transformation, id_step, i, "field_ascending", ascending[i]);
            }
        }
        catch (Exception e)
        {
            throw new KettleException("Unable to save step information to the repository for id_step=" + id_step, e);
        }
    }
    
    public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info) throws KettleStepException
    {
        // Set the sorted properties: ascending/descending
        for (int i=0;i<fieldName.length;i++)
        {
            int idx = inputRowMeta.indexOfValue(fieldName[i]);
            if (idx>=0)
            {
                ValueMetaInterface valueMeta = inputRowMeta.getValueMeta(idx);
                valueMeta.setSortedDescending(!ascending[i]);
                
                // TODO: add case insensivity
            }
        }
        
    }

    public void check(List<CheckResult> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
    {
        CheckResult cr;

        if (prev != null && prev.size() > 0)
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SortRowsMeta.CheckResult.FieldsReceived", "" + prev.size()),
                    stepMeta);
            remarks.add(cr);

            String error_message = "";
            boolean error_found = false;

            // Starting from selected fields in ...
            for (int i = 0; i < fieldName.length; i++)
            {
                int idx = prev.indexOfValue(fieldName[i]);
                if (idx < 0)
                {
                    error_message += "\t\t" + fieldName[i] + Const.CR;
                    error_found = true;
                }
            }
            if (error_found)
            {
                error_message = Messages.getString("SortRowsMeta.CheckResult.SortKeysNotFound", error_message);

                cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta);
                remarks.add(cr);
            }
            else
            {
                if (fieldName.length > 0)
                {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SortRowsMeta.CheckResult.AllSortKeysFound"), stepMeta);
                    remarks.add(cr);
                }
                else
                {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SortRowsMeta.CheckResult.NoSortKeysEntered"), stepMeta);
                    remarks.add(cr);
                }
            }

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
        if (input.length > 0)
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SortRowsMeta.CheckResult.ExpectedInputOk"), stepMeta);
            remarks.add(cr);
        }
        else
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SortRowsMeta.CheckResult.ExpectedInputError"), stepMeta);
            remarks.add(cr);
        }
    }

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
    {
        return new SortRowsDialog(shell, info, transMeta, name);
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
    {
        return new SortRows(stepMeta, stepDataInterface, cnr, transMeta, trans);
    }

    public StepDataInterface getStepData()
    {
        return new SortRowsData();
    }

    /**
     * @return Returns the sortSize.
     */
    public int getSortSize()
    {
        return sortSize;
    }

    /**
     * @param sortSize The sortSize to set.
     */
    public void setSortSize(int sortSize)
    {
        this.sortSize = sortSize;
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
     * @return the onlyPassingUniqueRows
     */
    public boolean isOnlyPassingUniqueRows()
    {
        return onlyPassingUniqueRows;
    }

    /**
     * @param onlyPassingUniqueRows the onlyPassingUniqueRows to set
     */
    public void setOnlyPassingUniqueRows(boolean onlyPassingUniqueRows)
    {
        this.onlyPassingUniqueRows = onlyPassingUniqueRows;
    }
}
