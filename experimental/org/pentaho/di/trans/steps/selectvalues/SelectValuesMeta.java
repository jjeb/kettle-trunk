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

package org.pentaho.di.trans.steps.selectvalues;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleStepException;
import be.ibridge.kettle.core.exception.KettleValueException;
import be.ibridge.kettle.core.exception.KettleXMLException;
import be.ibridge.kettle.repository.Repository;


/**
 * Meta Data class for the Select Values Step.
 * 
 * Created on 02-jun-2003
 */
public class SelectValuesMeta extends BaseStepMeta implements StepMetaInterface
{
	// SELECT mode
    /** Select: Name of the selected field */
	private String selectName[];
	/** Select: Rename to ...  */
	private String selectRename[]; 
	/** Select: length of field  */
	private int    selectLength[]; 
	/** Select: Precision of field (for numbers)  */
	private int    selectPrecision[]; 
	
	// DE-SELECT mode
	/** Names of the fields to be removed!  */
	private String deleteName[]; 
	
	// META-DATA mode
	/** Fields of which we want to change the meta data  */
	private String metaName[];
	/** Meta: new name of field  */
	private String metaRename[];
	/** Meta: new type for this field or VALUE_TYPE_NONE if no change needed!  */
	private int    metaType[];
	/** Meta: new length of field  */
	private int    metaLength[];
	/** Meta: new precision of field (for numbers)  */
	private int    metaPrecision[];
	
	public SelectValuesMeta()
	{
		super(); // allocate BaseStepMeta
	}

    /**
     * @return Returns the deleteName.
     */
    public String[] getDeleteName()
    {
        return deleteName;
    }
    
    /**
     * @param deleteName The deleteName to set.
     */
    public void setDeleteName(String[] deleteName)
    {
        this.deleteName = deleteName;
    }
    
    /**
     * @return Returns the metaLength.
     */
    public int[] getMetaLength()
    {
        return metaLength;
    }
    
    /**
     * @param metaLength The metaLength to set.
     */
    public void setMetaLength(int[] metaLength)
    {
        this.metaLength = metaLength;
    }
    
    /**
     * @return Returns the metaName.
     */
    public String[] getMetaName()
    {
        return metaName;
    }
    
    /**
     * @param metaName The metaName to set.
     */
    public void setMetaName(String[] metaName)
    {
        this.metaName = metaName;
    }
    
    /**
     * @return Returns the metaPrecision.
     */
    public int[] getMetaPrecision()
    {
        return metaPrecision;
    }
    
    /**
     * @param metaPrecision The metaPrecision to set.
     */
    public void setMetaPrecision(int[] metaPrecision)
    {
        this.metaPrecision = metaPrecision;
    }
    
    /**
     * @return Returns the metaRename.
     */
    public String[] getMetaRename()
    {
        return metaRename;
    }
    
    /**
     * @param metaRename The metaRename to set.
     */
    public void setMetaRename(String[] metaRename)
    {
        this.metaRename = metaRename;
    }
    
    /**
     * @return Returns the metaType.
     */
    public int[] getMetaType()
    {
        return metaType;
    }
    
    /**
     * @param metaType The metaType to set.
     */
    public void setMetaType(int[] metaType)
    {
        this.metaType = metaType;
    }
    
    /**
     * @return Returns the selectLength.
     */
    public int[] getSelectLength()
    {
        return selectLength;
    }
    
    /**
     * @param selectLength The selectLength to set.
     */
    public void setSelectLength(int[] selectLength)
    {
        this.selectLength = selectLength;
    }
    
    /**
     * @return Returns the selectName.
     */
    public String[] getSelectName()
    {
        return selectName;
    }
    
    /**
     * @param selectName The selectName to set.
     */
    public void setSelectName(String[] selectName)
    {
        this.selectName = selectName;
    }
    
    /**
     * @return Returns the selectPrecision.
     */
    public int[] getSelectPrecision()
    {
        return selectPrecision;
    }
    
    /**
     * @param selectPrecision The selectPrecision to set.
     */
    public void setSelectPrecision(int[] selectPrecision)
    {
        this.selectPrecision = selectPrecision;
    }
    
    /**
     * @return Returns the selectRename.
     */
    public String[] getSelectRename()
    {
        return selectRename;
    }
    
    /**
     * @param selectRename The selectRename to set.
     */
    public void setSelectRename(String[] selectRename)
    {
        this.selectRename = selectRename;
    }
    
    
    
	public void loadXML(Node stepnode, ArrayList databases, Hashtable counters)
		throws KettleXMLException
	{
		readData(stepnode);
	}

	public void allocate(int nrfields, int nrremove, int nrmeta)
	{
		selectName      = new String[nrfields];
		selectRename    = new String[nrfields];
		selectLength    = new int   [nrfields];
		selectPrecision = new int   [nrfields];
		
		deleteName      = new String[nrremove];
		
		metaName      = new String[nrmeta];
		metaRename    = new String[nrmeta];
		metaType      = new int   [nrmeta];
		metaLength    = new int   [nrmeta];
		metaPrecision = new int   [nrmeta];
	}

	public Object clone()
	{
		SelectValuesMeta retval = (SelectValuesMeta)super.clone();

		int nrfields = selectName.length;
		int nrremove = deleteName.length;
		int nrmeta   = metaName.length;
		
		retval.allocate(nrfields, nrremove, nrmeta);
		
		for (int i=0;i<nrfields;i++)
		{
			retval.selectName     [i] = selectName[i];
			retval.selectRename   [i] = selectRename[i];
			retval.selectLength   [i] = selectLength[i];
			retval.selectPrecision[i] = selectPrecision[i];
		}

		for (int i=0;i<nrremove;i++)
		{
			retval.deleteName     [i] = deleteName[i];
		}
		
		for (int i=0;i<nrmeta;i++)
		{
			retval.metaName     [i] = metaName[i];
			retval.metaRename   [i] = metaRename[i];
			retval.metaType     [i] = metaType[i];
			retval.metaLength   [i] = metaLength[i];
			retval.metaPrecision[i] = metaPrecision[i];
		}

		return retval;
	}
	
	private void readData(Node step)
		throws KettleXMLException
	{
		try
		{
			Node fields = XMLHandler.getSubNode(step, "fields"); //$NON-NLS-1$
	
			int nrfields   = XMLHandler.countNodes(fields, "field"); //$NON-NLS-1$
			int nrremove   = XMLHandler.countNodes(fields, "remove"); //$NON-NLS-1$
			int nrmeta     = XMLHandler.countNodes(fields, "meta"); //$NON-NLS-1$
			allocate(nrfields, nrremove, nrmeta);
			
			for (int i=0;i<nrfields;i++)
			{
				Node line = XMLHandler.getSubNodeByNr(fields, "field", i); //$NON-NLS-1$
				selectName     [i] = XMLHandler.getTagValue(line, "name"); //$NON-NLS-1$
				selectRename   [i] = XMLHandler.getTagValue(line, "rename"); //$NON-NLS-1$
				selectLength   [i] = Const.toInt(XMLHandler.getTagValue(line, "length"), -2); //$NON-NLS-1$
				selectPrecision[i] = Const.toInt(XMLHandler.getTagValue(line, "precision"), -2); //$NON-NLS-1$
			}
	
			for (int i=0;i<nrremove;i++)
			{
				Node line = XMLHandler.getSubNodeByNr(fields, "remove", i); //$NON-NLS-1$
				deleteName     [i] = XMLHandler.getTagValue(line, "name"); //$NON-NLS-1$
			}
	
			for (int i=0;i<nrmeta;i++)
			{
				Node line = XMLHandler.getSubNodeByNr(fields, "meta", i); //$NON-NLS-1$
				metaName     [i] = XMLHandler.getTagValue(line, "name"); //$NON-NLS-1$
				metaRename   [i] = XMLHandler.getTagValue(line, "rename"); //$NON-NLS-1$
				metaType     [i] = ValueMeta.getType(XMLHandler.getTagValue(line, "type")); //$NON-NLS-1$
				metaLength   [i] = Const.toInt(XMLHandler.getTagValue(line, "length"), -2); //$NON-NLS-1$
				metaPrecision[i] = Const.toInt(XMLHandler.getTagValue(line, "precision"), -2); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleXMLException(Messages.getString("SelectValuesMeta.Exception.UnableToReadStepInfoFromXML"), e); //$NON-NLS-1$
		}
	}

	public void setDefault()
	{
		int nrfields = 0;
		int nrremove = 0;
		int nrmeta   = 0;

		allocate(nrfields, nrremove, nrmeta);
		
		for (int i=0;i<nrfields;i++)
		{
			selectName     [i] = "fieldname"+(i+1); //$NON-NLS-1$
			selectRename   [i] = ""; //$NON-NLS-1$
			selectLength   [i] = -2;
			selectPrecision[i] = -2;
		}

		for (int i=0;i<nrremove;i++)
		{
			deleteName     [i] = "fieldname"+(i+1); //$NON-NLS-1$
		}

		for (int i=0;i<nrmeta;i++)
		{
			metaName     [i] = "fieldname"+(i+1); //$NON-NLS-1$
			metaRename   [i] = ""; //$NON-NLS-1$
			metaType     [i] = ValueMetaInterface.TYPE_NONE;
			metaLength   [i] = -2;
			metaPrecision[i] = -2;
		}
	}

	public RowMetaInterface getFields(RowMetaInterface r, String name, RowMetaInterface info) throws KettleStepException
	{
        RowMetaInterface row=null;

		if (selectName!=null && selectName.length>0)  // SELECT values
		{
			if (r==null)  // give back rename values 
			{
				row=new RowMeta();
				for (int i=0;i<selectRename.length;i++)
				{
					ValueMetaInterface v=new ValueMeta(selectRename[i], ValueMetaInterface.TYPE_NONE);
					row.addValueMeta(v);
				}
			}
			else          // select / rename in existing values...         
			{
				// 0. Start with an empty row
				// 1. Keep only the selected values
				// 2. Rename the selected values
				// 3. Keep the order in which they are specified... (not the input order!)
				//
				
				row=new RowMeta();
				for (int i=0;i<selectName.length;i++)
				{
					ValueMetaInterface v = r.searchValueMeta(selectName[i]);
					
					if (v!=null)  // We found the value
					{
						v = (ValueMetaInterface) v.clone();
						// Do we need to rename ?
						if (!v.getName().equals(selectRename[i]) && selectRename[i]!=null && selectRename[i].length()>0)
						{
							v.setName(selectRename[i]);
							v.setOrigin(name);
						}
						if (selectLength[i]!=-2   ) { v.setLength(selectLength[i]);       v.setOrigin(name); } 
						if (selectPrecision[i]!=-2) { v.setPrecision(selectPrecision[i]); v.setOrigin(name); }
						
						// Add to the resulting row!
						row.addValueMeta(v);
					}
				}
				
				// OK, now remove all from r and re-add row:
				r.clear();
				r.addRowMeta(row);
			}
		}
		
		if (deleteName!=null && deleteName.length>0)  // DESELECT values from the stream...
		{
			if (r!=null)
			{
				for (int i=0;i<deleteName.length;i++)
				{
					try
                    {
                        r.removeValueMeta(deleteName[i]);
                    }
                    catch (KettleValueException e)
                    {
                        throw new KettleStepException(e);
                    }
				}
			}
			else
			{
				row = new RowMeta(); // Return empty row...
			}
		}

		if (metaName!=null && metaName.length>0) // METADATA mode: change the meta-data of the values mentioned...
		{
			if (r!=null)
			{
				for (int i=0;i<metaName.length;i++)
				{
					int idx = r.indexOfValue(metaName[i]);
					if (idx>=0)  // We found the value
					{
						// This is the value we need to change:
						ValueMetaInterface v = r.getValueMeta(idx);
						
						// Do we need to rename ?
						if (!v.getName().equals(metaRename[i]) && metaRename[i]!=null && metaRename[i].length()>0)
						{
							v.setName(metaRename[i]);
							v.setOrigin(name);
						}
						// Change the type?
						if (metaType[i]!=ValueMetaInterface.TYPE_NONE)
						{
							v.setType(metaType[i]);
						}
						if (metaLength[i]!=-2   ) { v.setLength(metaLength[i]);       v.setOrigin(name); } 
						if (metaPrecision[i]!=-2) { v.setPrecision(metaPrecision[i]); v.setOrigin(name); }
					}
				}
			}
			else
			{
				row = new RowMeta();
			}
		}
		
		if (row==null) row = (RowMetaInterface) r.clone();
		
		return row;
	}

	public String getXML()
	{
        StringBuffer retval = new StringBuffer(300);
		
		retval.append("    <fields>"); //$NON-NLS-1$
		for (int i=0;i<selectName.length;i++)
		{
			retval.append("      <field>"); //$NON-NLS-1$
			retval.append("        ").append(XMLHandler.addTagValue("name",      selectName[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("rename",    selectRename[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("length",    selectLength[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("precision", selectPrecision[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("      </field>"); //$NON-NLS-1$
		}
		for (int i=0;i<deleteName.length;i++)
		{
			retval.append("      <remove>"); //$NON-NLS-1$
			retval.append("        ").append(XMLHandler.addTagValue("name",      deleteName[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("      </remove>"); //$NON-NLS-1$
		}
		for (int i=0;i<metaName.length;i++)
		{
			retval.append("      <meta>"); //$NON-NLS-1$
			retval.append("        ").append(XMLHandler.addTagValue("name",      metaName[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("rename",    metaRename[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("type",      ValueMeta.getTypeDesc(metaType[i])) ); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("length",    metaLength[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("precision", metaPrecision[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("      </meta>"); //$NON-NLS-1$
		}
		retval.append("    </fields>"); //$NON-NLS-1$

		return retval.toString();
	}

	public void readRep(Repository rep, long id_step, ArrayList databases, Hashtable counters)
		throws KettleException
	{
		try
		{
			int nrfields = rep.countNrStepAttributes(id_step, "field_name"); //$NON-NLS-1$
			int nrremove = rep.countNrStepAttributes(id_step, "remove_name"); //$NON-NLS-1$
			int nrmeta   = rep.countNrStepAttributes(id_step, "meta_name"); //$NON-NLS-1$
			
			allocate(nrfields, nrremove, nrmeta);
	
			for (int i=0;i<nrfields;i++)
			{
				selectName[i]      =      rep.getStepAttributeString (id_step, i, "field_name"); //$NON-NLS-1$
				selectRename[i]    =      rep.getStepAttributeString (id_step, i, "field_rename"); //$NON-NLS-1$
				selectLength[i]    = (int)rep.getStepAttributeInteger(id_step, i, "field_length"); //$NON-NLS-1$
				selectPrecision[i] = (int)rep.getStepAttributeInteger(id_step, i, "field_precision"); //$NON-NLS-1$
			}

			for (int i=0;i<nrremove;i++)
			{
				deleteName[i]      =      rep.getStepAttributeString(id_step, i, "remove_name"); //$NON-NLS-1$
			}

			for (int i=0;i<nrmeta;i++)
			{
				metaName[i]      =      rep.getStepAttributeString (id_step, i, "meta_name"); //$NON-NLS-1$
				metaRename[i]    =      rep.getStepAttributeString (id_step, i, "meta_rename"); //$NON-NLS-1$
				metaType[i]      = (int)rep.getStepAttributeInteger(id_step, i, "meta_type"); //$NON-NLS-1$
				metaLength[i]    = (int)rep.getStepAttributeInteger(id_step, i, "meta_length"); //$NON-NLS-1$
				metaPrecision[i] = (int)rep.getStepAttributeInteger(id_step, i, "meta_precision"); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("SelectValuesMeta.Exception.UnexpectedErrorReadingStepInfoFromRepository"), e); //$NON-NLS-1$
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step)
		throws KettleException
	{
		try
		{
			for (int i=0;i<selectName.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "field_name",      selectName[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "field_rename",    selectRename[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "field_length",    selectLength[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "field_precision", selectPrecision[i]); //$NON-NLS-1$
			}
	
			for (int i=0;i<deleteName.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "remove_name",      deleteName[i]); //$NON-NLS-1$
			}
	
			for (int i=0;i<metaName.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "meta_name",      metaName[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "meta_rename",    metaRename[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "meta_type",      metaType[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "meta_length",    metaLength[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "meta_precision", metaPrecision[i]); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("SelectValuesMeta.Exception.UnableToSaveStepInfoToRepository")+id_step, e); //$NON-NLS-1$
		}

	}

	public void check(ArrayList remarks, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		
		if (prev!=null && prev.size()>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SelectValuesMeta.CheckResult.StepReceivingFields",prev.size()+""), stepMeta); //$NON-NLS-1$ //$NON-NLS-2$
			remarks.add(cr);

			/*
			 * Take care of the normal SELECT fields...
			 */
			String  error_message=""; //$NON-NLS-1$
			boolean error_found=false;
			
			// Starting from selected fields in ...
			for (int i=0;i< this.selectName.length;i++)
			{
				int idx = prev.indexOfValue(selectName[i]);
				if (idx<0)
				{
					error_message+="\t\t"+selectName[i]+Const.CR; //$NON-NLS-1$
					error_found=true;
				} 
			}
			if (error_found) 
			{
				error_message=Messages.getString("SelectValuesMeta.CheckResult.SelectedFieldsNotFound")+Const.CR+Const.CR+error_message; //$NON-NLS-1$

				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta);
				remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SelectValuesMeta.CheckResult.AllSelectedFieldsFound"), stepMeta); //$NON-NLS-1$
				remarks.add(cr);
			}
			
			if (this.selectName.length>0)
			{
				// Starting from prev...
				for (int i=0;i<prev.size();i++)
				{
					ValueMetaInterface pv = prev.getValueMeta(i);
					int idx = Const.indexOfString(pv.getName(), selectName);
					if (idx<0) 
					{
						error_message+="\t\t"+pv.getName()+" ("+pv.getTypeDesc()+")"+Const.CR; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						error_found=true;
					} 
				}
				if (error_found) 
				{
					error_message=Messages.getString("SelectValuesMeta.CheckResult.FieldsNotFound")+Const.CR+Const.CR+error_message; //$NON-NLS-1$
	
					cr = new CheckResult(CheckResult.TYPE_RESULT_COMMENT, error_message, stepMeta);
					remarks.add(cr);
				}
				else
				{
					cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SelectValuesMeta.CheckResult.AllSelectedFieldsFound2"), stepMeta); //$NON-NLS-1$
					remarks.add(cr);
				}
			}

			/*
			 * How about the DE-SELECT (remove) fields...
			 */
		
			error_message=""; //$NON-NLS-1$
			error_found=false;
			
			// Starting from selected fields in ...
			for (int i=0;i< this.deleteName.length;i++)
			{
				int idx = prev.indexOfValue(deleteName[i]);
				if (idx<0)
				{
					error_message+="\t\t"+deleteName[i]+Const.CR; //$NON-NLS-1$
					error_found=true;
				} 
			}
			if (error_found) 
			{
				error_message=Messages.getString("SelectValuesMeta.CheckResult.DeSelectedFieldsNotFound")+Const.CR+Const.CR+error_message; //$NON-NLS-1$

				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta);
				remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SelectValuesMeta.CheckResult.AllDeSelectedFieldsFound"), stepMeta); //$NON-NLS-1$
				remarks.add(cr);
			}

			/*
			 * How about the Meta-fields...?
			 */
			error_message=""; //$NON-NLS-1$
			error_found=false;
			
			// Starting from selected fields in ...
			for (int i=0;i< this.metaName.length;i++)
			{
				int idx = prev.indexOfValue(metaName[i]);
				if (idx<0)
				{
					error_message+="\t\t"+metaName[i]+Const.CR; //$NON-NLS-1$
					error_found=true;
				} 
			}
			if (error_found) 
			{
				error_message=Messages.getString("SelectValuesMeta.CheckResult.MetadataFieldsNotFound")+Const.CR+Const.CR+error_message; //$NON-NLS-1$

				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta);
				remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SelectValuesMeta.CheckResult.AllMetadataFieldsFound"), stepMeta); //$NON-NLS-1$
				remarks.add(cr);
			}
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SelectValuesMeta.CheckResult.FieldsNotFound2"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}

		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("SelectValuesMeta.CheckResult.StepReceivingInfoFromOtherSteps"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("SelectValuesMeta.CheckResult.NoInputReceivedError"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
		
		// Check for doubles in the selected fields...
		int cnt[] = new int[selectName.length];
		boolean error_found = false;
		String error_message=""; //$NON-NLS-1$
		
		for (int i=0;i<selectName.length;i++)
		{
			cnt[i]=0;
			for (int j=0;j<selectName.length;j++)
			{
				if (selectName[i].equals(selectName[j])) cnt[i]++;
			}
			
			if (cnt[i]>1)
			{
				if (!error_found) // first time...
				{
					error_message=Messages.getString("SelectValuesMeta.CheckResult.DuplicateFieldsSpecified")+Const.CR; //$NON-NLS-1$
				}
				else
				{
					error_found=true;
				}
				error_message+=Messages.getString("SelectValuesMeta.CheckResult.OccurentRow",i+" : "+selectName[i]+"  ("+cnt[i])+Const.CR; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				error_found=true;
			}
		}
		if (error_found)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta);
			remarks.add(cr);
		}
	}

	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new SelectValuesDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new SelectValues(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new SelectValuesData();
	}
}