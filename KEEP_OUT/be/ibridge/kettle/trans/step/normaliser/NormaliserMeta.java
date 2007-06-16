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

package be.ibridge.kettle.trans.step.normaliser;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Node;

import be.ibridge.kettle.core.CheckResult;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.exception.KettleException;
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


/*
 * Created on 30-okt-2003
 *
 */

/*

DATE      PRODUCT1_NR  PRODUCT1_SL  PRODUCT2_NR PRODUCT2_SL PRODUCT3_NR PRODUCT3_SL 
20030101            5          100           10         250           4         150
          
DATE      PRODUCT    Sales   Number  
20030101  PRODUCT1     100        5
20030101  PRODUCT2     250       10
20030101  PRODUCT3     150        4

--> we need a mapping of fields with occurances.  (PRODUCT1_NR --> "PRODUCT1", PRODUCT1_SL --> "PRODUCT1", ...)
--> List of Fields with the type and the new fieldname to fill
--> PRODUCT1_NR, "PRODUCT1", Number
--> PRODUCT1_SL, "PRODUCT1", Sales
--> PRODUCT2_NR, "PRODUCT2", Number
--> PRODUCT2_SL, "PRODUCT2", Sales
--> PRODUCT3_NR, "PRODUCT3", Number
--> PRODUCT3_SL, "PRODUCT3", Sales

--> To parse this, we loop over the occurances of type: "PRODUCT1", "PRODUCT2" and "PRODUCT3"
--> For each of the occurance, we insert a record.

**/
 
public class NormaliserMeta extends BaseStepMeta implements StepMetaInterface
{
	private String typeField;    // Name of the new type-field.
	private String fieldName[];      // Names of the selected fields. ex. "PRODUCT1_NR"
	private String fieldValue[]; // Value of the type: ex.            "PRODUCT1"
	private String fieldNorm[];  // new normalised field              "Number"
	
	public NormaliserMeta()
	{
		super(); // allocate BaseStepMeta
	}
	
	/**
     * @return Returns the typeField.
     */
    public String getTypeField()
    {
        return typeField;
    }
    
    /**
     * @param typeField The typeField to set.
     */
    public void setTypeField(String typeField)
    {
        this.typeField = typeField;
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
     * @return Returns the fieldValue.
     */
    public String[] getFieldValue()
    {
        return fieldValue;
    }
    
    /**
     * @param fieldValue The fieldValue to set.
     */
    public void setFieldValue(String[] fieldValue)
    {
        this.fieldValue = fieldValue;
    }
    
    /**
     * @return Returns the fieldNorm.
     */
    public String[] getFieldNorm()
    {
        return fieldNorm;
    }
    
    /**
     * @param fieldNorm The fieldNorm to set.
     */
    public void setFieldNorm(String[] fieldNorm)
    {
        this.fieldNorm = fieldNorm;
    }
	
	public void loadXML(Node stepnode, ArrayList databases, Hashtable counters)
		throws KettleXMLException
	{
		readData(stepnode);
	}

	public void allocate(int nrfields)
	{
		fieldName      = new String[nrfields];
		fieldValue = new String[nrfields];
		fieldNorm  = new String[nrfields];
	}

	public Object clone()
	{
		NormaliserMeta retval = (NormaliserMeta)super.clone();

		int nrfields   = fieldName.length;
		
		retval.allocate(nrfields);
		
		for (int i=0;i<nrfields;i++)
		{
			retval.fieldName     [i] = fieldName[i]; 
			retval.fieldValue[i] = fieldValue[i];
			retval.fieldNorm [i] = fieldNorm[i];
		}
		
		return retval;
	}
	
	private void readData(Node stepnode)
		throws KettleXMLException
	{
		try
		{
			typeField  = XMLHandler.getTagValue(stepnode, "typefield"); //$NON-NLS-1$
			
			Node fields = XMLHandler.getSubNode(stepnode, "fields"); //$NON-NLS-1$
			int nrfields   = XMLHandler.countNodes(fields, "field"); //$NON-NLS-1$
			
			allocate(nrfields);
			
			for (int i=0;i<nrfields;i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i); //$NON-NLS-1$
				
				fieldName     [i] = XMLHandler.getTagValue(fnode, "name"); //$NON-NLS-1$
				fieldValue[i] = XMLHandler.getTagValue(fnode, "value"); //$NON-NLS-1$
				fieldNorm [i] = XMLHandler.getTagValue(fnode, "norm"); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleXMLException(Messages.getString("NormaliserMeta.Exception.UnableToLoadStepInfoFromXML"), e); //$NON-NLS-1$
		}
	}

	public void setDefault()
	{
		typeField = "typefield"; //$NON-NLS-1$
		
		int nrfields = 0;
	
		allocate(nrfields);		
		
		for (int i=0;i<nrfields;i++)
		{
			fieldName     [i] = "field"+i; //$NON-NLS-1$
			fieldValue[i] = "value"+i; //$NON-NLS-1$
			fieldNorm [i] = "value"+i; //$NON-NLS-1$
		}
	}

	public Row getFields(Row r, String name, Row info)
	{
		Row row;
		int i,j;	
	
		if (r==null)  // give back rename values 
		{
			row = new Row();
		}
		else  // Remove fields to normalise, Add the typefield and norm fields, leave the rest!         
		{
			row=r;

			// Get a unique list of the occurances of the type
			ArrayList norm_occ = new ArrayList();
			ArrayList field_occ  = new ArrayList();
			int maxlen=0;
			for (i=0;i<fieldNorm.length;i++)
			{
				boolean found=false;
				for (j=0;j<norm_occ.size();j++)
				{
					if (((String)norm_occ.get(j)).equalsIgnoreCase(fieldNorm[i])) found=true;
				}
				if (!found) 
				{
					norm_occ.add(fieldNorm[i]);
					field_occ.add(fieldName[i]);
				} 
				
				if (fieldValue[i].length()>maxlen) maxlen=fieldValue[i].length();
			}

			// Add the copy fields!
			int rowsize = r.size();
			for (i=0;i<rowsize;i++)
			{
				Value v=r.getValue(i);
				boolean found=false;
				for (j=0;j<fieldName.length;j++)
				{
					if (v.getName().equalsIgnoreCase(fieldName[j]))
					{
						found=true;
					}
				}
				if (!found) 
				{
					row.addValue(v);
				} 
			}
			
			// Then add the typefield!
			Value typefield_value = new Value(typeField, Value.VALUE_TYPE_STRING);
			typefield_value.setOrigin(name);
			typefield_value.setLength(maxlen);
			row.addValue(typefield_value);

			// Loop over the distinct list of fieldnorm[i]
			for (i=0;i<norm_occ.size();i++)
			{
				String normname = (String)norm_occ.get(i);
				String fieldname =(String)field_occ.get(i);
				Value v = r.searchValue(fieldname);
				v.setName(normname);
				v.setOrigin(name);
				row.addValue(v);
			}
			
			// Delete the first entries in the row...
			for (i=0;i<rowsize;i++)
			{
				row.removeValue(0);
			}
		}
		
		return row;
	}

	public String getXML()
	{
        StringBuffer retval = new StringBuffer();
		
		retval.append("   "+XMLHandler.addTagValue("typefield", typeField)); //$NON-NLS-1$ //$NON-NLS-2$
		
		retval.append("    <fields>"); //$NON-NLS-1$
		for (int i=0;i<fieldName.length;i++)
		{
			retval.append("      <field>"); //$NON-NLS-1$
			retval.append("        "+XMLHandler.addTagValue("name",  fieldName[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        "+XMLHandler.addTagValue("value", fieldValue[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        "+XMLHandler.addTagValue("norm",  fieldNorm[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        </field>"); //$NON-NLS-1$
		}
		retval.append("      </fields>"); //$NON-NLS-1$

		return retval.toString();
	}

	public void readRep(Repository rep, long id_step, ArrayList databases, Hashtable counters)
		throws KettleException
	{
		try
		{
			typeField = rep.getStepAttributeString(id_step, "typefield"); //$NON-NLS-1$
			
			int nrfields = rep.countNrStepAttributes(id_step, "field_name"); //$NON-NLS-1$
			
			allocate(nrfields);
	
			for (int i=0;i<nrfields;i++)
			{
				fieldName[i]       =  rep.getStepAttributeString (id_step, i, "field_name"); //$NON-NLS-1$
				fieldValue[i]  =  rep.getStepAttributeString (id_step, i, "field_value"); //$NON-NLS-1$
				fieldNorm[i]   =  rep.getStepAttributeString (id_step, i, "field_norm"); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("NormaliserMeta.Exception.UnexpectedErrorReadingStepInfoFromRepository"), e); //$NON-NLS-1$
		}
	}

	
	public void saveRep(Repository rep, long id_transformation, long id_step)
		throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "typefield", typeField); //$NON-NLS-1$
	
			for (int i=0;i<fieldName.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "field_name",      fieldName[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "field_value",     fieldValue[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "field_norm",      fieldNorm[i]); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("NormaliserMeta.Exception.UnableToSaveStepInfoToRepository")+id_step, e); //$NON-NLS-1$
		}
	}


	public void check(ArrayList remarks, StepMeta stepinfo, Row prev, String input[], String output[], Row info)
	{
		String error_message=""; //$NON-NLS-1$
		CheckResult cr;
		
		// Look up fields in the input stream <prev>
		if (prev!=null && prev.size()>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("NormaliserMeta.CheckResult.StepReceivingFieldsOK",prev.size()+""), stepinfo); //$NON-NLS-1$ //$NON-NLS-2$
			remarks.add(cr);
			
			boolean first=true;
			error_message = ""; //$NON-NLS-1$
			boolean error_found = false;
			
			for (int i=0;i<fieldName.length;i++)
			{
				String lufield = fieldName[i];

				Value v = prev.searchValue(lufield);
				if (v==null)
				{
					if (first)
					{
						first=false;
						error_message+=Messages.getString("NormaliserMeta.CheckResult.FieldsNotFound")+Const.CR; //$NON-NLS-1$
					}
					error_found=true;
					error_message+="\t\t"+lufield+Const.CR;  //$NON-NLS-1$
				}
			}
			if (error_found)
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepinfo);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("NormaliserMeta.CheckResult.AllFieldsFound"), stepinfo); //$NON-NLS-1$
			}
			remarks.add(cr);
		}
		else
		{
			error_message=Messages.getString("NormaliserMeta.CheckResult.CouldNotReadFieldsFromPreviousStep")+Const.CR; //$NON-NLS-1$
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepinfo);
			remarks.add(cr);
		}

		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("NormaliserMeta.CheckResult.StepReceivingInfoOK"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("NormaliserMeta.CheckResult.NoInputReceivedError"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
	}

	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new NormaliserDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new Normaliser(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new NormaliserData();
	}

}
