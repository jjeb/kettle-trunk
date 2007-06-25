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

package be.ibridge.kettle.trans.step.flattener;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Node;

import be.ibridge.kettle.core.CheckResult;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleStepException;
import be.ibridge.kettle.core.exception.KettleXMLException;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.repository.Repository;
import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransMeta;
import be.ibridge.kettle.trans.step.BaseStepMeta;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.StepDialogInterface;
import be.ibridge.kettle.trans.step.StepInterface;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;

/**
 * The pivot transformation step meta-data
 * 
 * @since 17-jan-2006
 * @author Matt
 */

public class FlattenerMeta extends BaseStepMeta implements StepMetaInterface
{
    /** The field to flatten */
    private String fieldName;
    
    /** Fields to flatten, same data type as input */
    private String                    targetField[];

    public FlattenerMeta()
    {
        super(); // allocate BaseStepMeta
    }

    public String getFieldName()
    {
        return fieldName;
    }
    
    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }
    
    public String[] getTargetField()
    {
        return targetField;
    }
    
    public void setTargetField(String[] targetField)
    {
        this.targetField = targetField;
    }
    
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Hashtable counters) throws KettleXMLException
    {
        readData(stepnode);
    }

    public void allocate(int nrfields)
    {
        targetField = new String[nrfields];
    }

    public Object clone()
    {
        Object retval = super.clone();
        return retval;
    }

    public void setDefault()
    {
        int nrfields = 0;

        allocate(nrfields);
    }

    public Row getFields(Row r, String name, Row info) throws KettleStepException
    {
        Row row = r;

        // Remove the key value (there will be different entries for each output row)
        //
        if (fieldName != null && fieldName.length() > 0)
        {
            int idx = row.searchValueIndex(fieldName);
            if (idx < 0) { throw new KettleStepException(Messages.getString("FlattenerMeta.Exception.UnableToLocateFieldInInputFields", fieldName )); } //$NON-NLS-1$ //$NON-NLS-2$
            
            Value v = row.getValue(idx);
            row.removeValue(idx);
            
            for (int i=0;i<targetField.length;i++)
            {
                Value value = v.Clone();
                value.setName(targetField[i]);
                value.setOrigin(name);
                
                row.addValue(value);
            }
        }
        else
        {
            throw new KettleStepException(Messages.getString("FlattenerMeta.Exception.FlattenFieldRequired")); //$NON-NLS-1$
        }

        return row;
    }

    private void readData(Node stepnode) throws KettleXMLException
    {
        try
        {
            fieldName = XMLHandler.getTagValue(stepnode, "field_name"); //$NON-NLS-1$

            Node fields = XMLHandler.getSubNode(stepnode, "fields"); //$NON-NLS-1$
            int nrfields = XMLHandler.countNodes(fields, "field"); //$NON-NLS-1$

            allocate(nrfields);

            for (int i = 0; i < nrfields; i++)
            {
                Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i); //$NON-NLS-1$
                targetField[i] = XMLHandler.getTagValue(fnode, "name"); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            throw new KettleXMLException(Messages.getString("FlattenerMeta.Exception.UnableToLoadStepInfoFromXML"), e); //$NON-NLS-1$
        }
    }

    public String getXML()
    {
        StringBuffer retval = new StringBuffer();

        retval.append("      " + XMLHandler.addTagValue("field_name", fieldName)); //$NON-NLS-1$ //$NON-NLS-2$

        retval.append("      <fields>" + Const.CR); //$NON-NLS-1$
        for (int i = 0; i < targetField.length; i++)
        {
            retval.append("        <field>" + Const.CR); //$NON-NLS-1$
            retval.append("          " + XMLHandler.addTagValue("name", targetField[i])); //$NON-NLS-1$ //$NON-NLS-2$
            retval.append("          </field>" + Const.CR); //$NON-NLS-1$
        }
        retval.append("        </fields>" + Const.CR); //$NON-NLS-1$

        return retval.toString();
    }

    public void readRep(Repository rep, long id_step, ArrayList databases, Hashtable counters) throws KettleException
    {
        try
        {
            fieldName = rep.getStepAttributeString(id_step, "field_name"); //$NON-NLS-1$

            int nrvalues = rep.countNrStepAttributes(id_step, "target_field"); //$NON-NLS-1$

            allocate(nrvalues);

            for (int i = 0; i < nrvalues; i++)
            {
                targetField[i] = rep.getStepAttributeString(id_step, i, "target_field"); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            throw new KettleException(Messages.getString("FlattenerMeta.Exception.UnexpectedErrorInReadingStepInfoFromRepository"), e); //$NON-NLS-1$
        }
    }

    public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
    {
        try
        {
            rep.saveStepAttribute(id_transformation, id_step, "field_name", fieldName); //$NON-NLS-1$

            for (int i = 0; i < targetField.length; i++)
            {
                rep.saveStepAttribute(id_transformation, id_step, i, "target_field", targetField[i]); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            throw new KettleException(Messages.getString("FlattenerMeta.Exception.UnableToSaveStepInfoToRepository") + id_step, e); //$NON-NLS-1$
        }
    }

    public void check(ArrayList remarks, StepMeta stepMeta, Row prev, String input[], String output[], Row info)
    {
        CheckResult cr;

        if (input.length > 0)
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FlattenerMeta.CheckResult.StepReceivingInfoFromOtherSteps"), stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        }
        else
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("FlattenerMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        }
    }

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
    {
        return new FlattenerDialog(shell, info, transMeta, name);
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
    {
        return new Flattener(stepMeta, stepDataInterface, cnr, transMeta, trans);
    }

    public StepDataInterface getStepData()
    {
        return new FlattenerData();
    }

}
