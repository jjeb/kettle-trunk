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

package org.pentaho.di.trans.steps.injector;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.w3c.dom.Node;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleStepException;
import be.ibridge.kettle.core.exception.KettleXMLException;
import be.ibridge.kettle.repository.Repository;


// TODO: check conversion of types from strings to numbers and back.
//       As compared in the old version.

/**
 * Metadata class to allow a java program to inject rows of data into a transformation.
 * This step can be used as a starting point in such a "headless" transformation.
 * 
 * @since 22-jun-2006
 */
public class InjectorMeta extends BaseStepMeta implements StepMetaInterface
{
    private String name[];
    private int    type[];
    private int    length[];
    private int    precision[];
    
	/**
     * @return Returns the length.
     */
    public int[] getLength()
    {
        return length;
    }

    /**
     * @param length The length to set.
     */
    public void setLength(int[] length)
    {
        this.length = length;
    }

    /**
     * @return Returns the name.
     */
    public String[] getName()
    {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String[] name)
    {
        this.name = name;
    }

    /**
     * @return Returns the precision.
     */
    public int[] getPrecision()
    {
        return precision;
    }

    /**
     * @param precision The precision to set.
     */
    public void setPrecision(int[] precision)
    {
        this.precision = precision;
    }

    /**
     * @return Returns the type.
     */
    public int[] getType()
    {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(int[] type)
    {
        this.type = type;
    }

    public InjectorMeta()
	{
		super(); // allocate BaseStepMeta
	}

	public void loadXML(Node stepnode, ArrayList databases, Hashtable counters)
		throws KettleXMLException
	{
		readData(stepnode);
	}

	public Object clone()
	{
		Object retval = super.clone();
		return retval;
	}
	
    public void allocate(int nrFields)
    {
        name = new String[nrFields];
        type = new int[nrFields];
        length = new int[nrFields];
        precision = new int[nrFields];
    }
    
    public String getXML()
    {
        StringBuffer retval=new StringBuffer(300);
        retval.append("    <fields>"); //$NON-NLS-1$
        for (int i=0;i<name.length;i++)
        {
            retval.append("      <field>"); //$NON-NLS-1$
            retval.append("        ").append(XMLHandler.addTagValue("name",      name[i])); //$NON-NLS-1$ //$NON-NLS-2$
            retval.append("        ").append(XMLHandler.addTagValue("type",      ValueMeta.getTypeDesc(type[i]))); //$NON-NLS-1$ //$NON-NLS-2$
            retval.append("        ").append(XMLHandler.addTagValue("length",    length[i])); //$NON-NLS-1$ //$NON-NLS-2$
            retval.append("        ").append(XMLHandler.addTagValue("precision", precision[i])); //$NON-NLS-1$ //$NON-NLS-2$
            retval.append("      </field>"); //$NON-NLS-1$
        }
        retval.append("    </fields>"); //$NON-NLS-1$

        return retval.toString();
    }
    
	private void readData(Node stepnode)
	{
        Node fields = XMLHandler.getSubNode(stepnode, "fields"); //$NON-NLS-1$
        int nrfields   = XMLHandler.countNodes(fields, "field"); //$NON-NLS-1$

        allocate(nrfields);
        
        for (int i=0;i<nrfields;i++)
        {
            Node line = XMLHandler.getSubNodeByNr(fields, "field", i); //$NON-NLS-1$
            name     [i] = XMLHandler.getTagValue(line, "name"); //$NON-NLS-1$
            type     [i] = ValueMeta.getType(XMLHandler.getTagValue(line, "type")); //$NON-NLS-1$
            length   [i] = Const.toInt(XMLHandler.getTagValue(line, "length"), -2); //$NON-NLS-1$
            precision[i] = Const.toInt(XMLHandler.getTagValue(line, "precision"), -2); //$NON-NLS-1$
        }

	}

	public void setDefault()
	{
        allocate(0);
	}

	public void readRep(Repository rep, long id_step, ArrayList databases, Hashtable counters)
		throws KettleException
	{
        try
        {
           int nrfields = rep.countNrStepAttributes(id_step, "field_name"); //$NON-NLS-1$
            allocate(nrfields);
    
            for (int i=0;i<nrfields;i++)
            {
                name[i]      = rep.getStepAttributeString (id_step, i, "field_name"); //$NON-NLS-1$
                type[i]      = ValueMeta.getType( rep.getStepAttributeString (id_step, i, "field_type")); //$NON-NLS-1$
                length[i]    = (int)rep.getStepAttributeInteger(id_step, i, "field_length"); //$NON-NLS-1$
                precision[i] = (int)rep.getStepAttributeInteger(id_step, i, "field_precision"); //$NON-NLS-1$
            }
        }
        catch(Exception e)
        {
            throw new KettleException(Messages.getString("RowsFromResultMeta.Exception.ErrorReadingStepInfoFromRepository"), e); //$NON-NLS-1$
        }

	}
	
	public void saveRep(Repository rep, long id_transformation, long id_step)
	throws KettleException
	{
        try
        {
            for (int i=0;i<name.length;i++)
            {
                rep.saveStepAttribute(id_transformation, id_step, i, "field_name",      name[i]); //$NON-NLS-1$
                rep.saveStepAttribute(id_transformation, id_step, i, "field_type",      ValueMeta.getTypeDesc(type[i])); //$NON-NLS-1$
                rep.saveStepAttribute(id_transformation, id_step, i, "field_length",    length[i]); //$NON-NLS-1$
                rep.saveStepAttribute(id_transformation, id_step, i, "field_precision", precision[i]); //$NON-NLS-1$
            }
        }
        catch(Exception e)
        {
            throw new KettleException(Messages.getString("RowsFromResultMeta.Exception.UnableToSaveStepInfoToRepository")+id_step, e); //$NON-NLS-1$
        }
	}
    
    public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface info) throws KettleStepException    
    {
        for (int i=0;i<this.name.length;i++)
        {
            ValueMetaInterface v = new ValueMeta(this.name[i], type[i], length[i], precision[i]);
            inputRowMeta.addValueMeta(v);
        }
    }

    public void check(ArrayList remarks, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("RowsFromResultMeta.CheckResult.StepExpectingNoReadingInfoFromOtherSteps"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("RowsFromResultMeta.CheckResult.NoInputReceivedError"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
	}

	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new InjectorDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new Injector(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new InjectorData();
	}
}