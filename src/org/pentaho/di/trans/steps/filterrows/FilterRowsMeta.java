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

package org.pentaho.di.trans.steps.filterrows;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.Condition;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaAndData;
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

public class FilterRowsMeta extends BaseStepMeta implements StepMetaInterface
{
	/**
	 * This is the main condition for the complete filter.
	 * @since version 2.1
	 */
	private Condition condition;

	private String sendTrueStepname;  // Which step is getting the 'true' records?
	private StepMeta sendTrueStep;    // The true step itself...

	private String sendFalseStepname;  // Which step is getting the 'false' records?
	private StepMeta sendFalseStep;    // The false step itself...

	public FilterRowsMeta()
	{
		super(); // allocate BaseStepMeta
        condition=new Condition();
	}
	
	public void loadXML(Node stepnode, List<? extends SharedObjectInterface> databases, Hashtable counters)
		throws KettleXMLException
	{
		readData(stepnode);
	}

	/**
	 * @return Returns the condition.
	 */
	public Condition getCondition()
	{
		return condition;
	}
	
	/**
	 * @param condition The condition to set.
	 */
	public void setCondition(Condition condition)
	{
		this.condition = condition;
	}
	
	/**
     * @return Returns the sendFalseStepname.
     */
    public String getSendFalseStepname()
    {
		if (sendFalseStep!=null && 
		        sendFalseStep.getName()!=null &&
		        sendFalseStep.getName().length()>0
			   ) 
				return sendFalseStep.getName();
			return null;
   }
 
	/**
     * @return Returns the sendTrueStepname.
     */
    public String getSendTrueStepname()
    {
		if (sendTrueStep!=null && 
		        sendTrueStep.getName()!=null &&
		        sendTrueStep.getName().length()>0
			   ) 
				return sendTrueStep.getName();
			return null;
   }
    

    /**
     * @param sendFalseStepname The sendFalseStepname to set.
     */
    public void setSendFalseStepname(String sendFalseStepname)
    {
        this.sendFalseStepname = sendFalseStepname;
    }
    
    /**
     * @param sendTrueStepname The sendTrueStepname to set.
     */
    public void setSendTrueStepname(String sendTrueStepname)
    {
        this.sendTrueStepname = sendTrueStepname;
    }
    
    /**
     * @return Returns the sendFalseStep.
     */
    public StepMeta getSendFalseStep()
    {
        return sendFalseStep;
    }
    
    /**
     * @return Returns the sendTrueStep.
     */
    public StepMeta getSendTrueStep()
    {
        return sendTrueStep;
    }
    
    /**
     * @param sendFalseStep The sendFalseStep to set.
     */
    public void setSendFalseStep(StepMeta sendFalseStep)
    {
        this.sendFalseStep = sendFalseStep;
    }
	
    /**
     * @param sendTrueStep The sendTrueStep to set.
     */
    public void setSendTrueStep(StepMeta sendTrueStep)
    {
        this.sendTrueStep = sendTrueStep;
    }
	
	public void allocate()
	{
		condition = new Condition();
	}

	public Object clone()
	{
		FilterRowsMeta retval = (FilterRowsMeta)super.clone();

		if (condition!=null)
        {
            retval.condition = (Condition)condition.clone();
        }
        else 
        {
            retval.condition=null;
        }

		return retval;
	}
	
	public String getXML()
	{
        StringBuffer retval = new StringBuffer();

		retval.append(XMLHandler.addTagValue("send_true_to", getSendTrueStepname()));		 //$NON-NLS-1$
		retval.append(XMLHandler.addTagValue("send_false_to", getSendFalseStepname()));		 //$NON-NLS-1$
		retval.append("    <compare>"+Const.CR); //$NON-NLS-1$
		
		if (condition!=null)
		{
			retval.append(condition.getXML());
		}
		
		retval.append("    </compare>"+Const.CR); //$NON-NLS-1$

		return retval.toString();
	}

	private void readData(Node stepnode) throws KettleXMLException
	{
		try
		{
			sendFalseStepname = XMLHandler.getTagValue(stepnode, "send_false_to"); //$NON-NLS-1$
			sendTrueStepname = XMLHandler.getTagValue(stepnode, "send_true_to"); //$NON-NLS-1$

			Node compare = XMLHandler.getSubNode(stepnode, "compare"); //$NON-NLS-1$
			Node condnode = XMLHandler.getSubNode(compare, "condition"); //$NON-NLS-1$
	
			// The new situation...
			if (condnode!=null)
			{
				condition = new Condition(condnode);
			}
			else // Old style condition: Line1 OR Line2 OR Line3: @deprecated!
			{
				condition = new Condition();
				
				int nrkeys   = XMLHandler.countNodes(compare, "key"); //$NON-NLS-1$
				if (nrkeys==1)
				{
					Node knode = XMLHandler.getSubNodeByNr(compare, "key", 0); //$NON-NLS-1$
					
					String key         = XMLHandler.getTagValue(knode, "name"); //$NON-NLS-1$
					String value       = XMLHandler.getTagValue(knode, "value"); //$NON-NLS-1$
					String field       = XMLHandler.getTagValue(knode, "field"); //$NON-NLS-1$
					String comparator  = XMLHandler.getTagValue(knode, "condition"); //$NON-NLS-1$
	
					condition.setOperator( Condition.OPERATOR_NONE );
					condition.setLeftValuename(key);
					condition.setFunction( Condition.getFunction(comparator) );
					condition.setRightValuename(field);
					condition.setRightExact( new ValueMetaAndData("value", value ) ); //$NON-NLS-1$
				}
				else
				{
					for (int i=0;i<nrkeys;i++)
					{
						Node knode = XMLHandler.getSubNodeByNr(compare, "key", i); //$NON-NLS-1$
						
						String key         = XMLHandler.getTagValue(knode, "name"); //$NON-NLS-1$
						String value       = XMLHandler.getTagValue(knode, "value"); //$NON-NLS-1$
						String field       = XMLHandler.getTagValue(knode, "field"); //$NON-NLS-1$
						String comparator  = XMLHandler.getTagValue(knode, "condition"); //$NON-NLS-1$
						
						Condition subc = new Condition();
						if (i>0) subc.setOperator( Condition.OPERATOR_OR   );
						else     subc.setOperator( Condition.OPERATOR_NONE );
						subc.setLeftValuename(key);
						subc.setFunction( Condition.getFunction(comparator) );
						subc.setRightValuename(field);
						subc.setRightExact( new ValueMetaAndData("value", value ) ); //$NON-NLS-1$
						
						condition.addCondition(subc);
					}
				}
			}
		}
		catch(Exception e)
		{
			throw new KettleXMLException(Messages.getString("FilterRowsMeta.Exception..UnableToLoadStepInfoFromXML"), e); //$NON-NLS-1$
		}
	}
	
	public void setDefault()
	{
		allocate();
	}

	public void readRep(Repository rep, long id_step, List<? extends SharedObjectInterface> databases, Hashtable counters) throws KettleException
	{
		try
		{
			allocate();

			sendTrueStepname  =      rep.getStepAttributeString (id_step, "send_true_to");  //$NON-NLS-1$
			sendFalseStepname =      rep.getStepAttributeString (id_step, "send_false_to");  //$NON-NLS-1$

			long id_condition = rep.getStepAttributeInteger(id_step, 0, "id_condition"); //$NON-NLS-1$
			if (id_condition>0)
			{
				condition = new Condition(rep, id_condition);
			}
			else
			{
				int nrkeys = rep.countNrStepAttributes(id_step, "compare_name"); //$NON-NLS-1$
				if (nrkeys==1)
				{
					String key        = rep.getStepAttributeString(id_step, 0, "compare_name"); //$NON-NLS-1$
					String comparator = rep.getStepAttributeString(id_step, 0, "compare_condition"); //$NON-NLS-1$
					String value      = rep.getStepAttributeString(id_step, 0, "compare_value"); //$NON-NLS-1$
					String field      = rep.getStepAttributeString(id_step, 0, "compare_field"); //$NON-NLS-1$

					condition = new Condition();
					condition.setOperator( Condition.OPERATOR_NONE );
					condition.setLeftValuename(key);
					condition.setFunction( Condition.getFunction(comparator) );
					condition.setRightValuename(field);
					condition.setRightExact( new ValueMetaAndData("value", value ) ); //$NON-NLS-1$
				}
				else
				{
					condition = new Condition();
					
					for (int i=0;i<nrkeys;i++)
					{
						String key        = rep.getStepAttributeString(id_step, i, "compare_name"); //$NON-NLS-1$
						String comparator = rep.getStepAttributeString(id_step, i, "compare_condition"); //$NON-NLS-1$
						String value      = rep.getStepAttributeString(id_step, i, "compare_value"); //$NON-NLS-1$
						String field      = rep.getStepAttributeString(id_step, i, "compare_field"); //$NON-NLS-1$
		
						Condition subc = new Condition();
						if (i>0) subc.setOperator( Condition.OPERATOR_OR   );
						else     subc.setOperator( Condition.OPERATOR_NONE );
						subc.setLeftValuename(key);
						subc.setFunction( Condition.getFunction(comparator) );
						subc.setRightValuename(field);
						subc.setRightExact( new ValueMetaAndData("value", value ) ); //$NON-NLS-1$
						
						condition.addCondition(subc);
					}
				}
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("FilterRowsMeta.Exception.UnexpectedErrorInReadingStepInfoFromRepository"), e); //$NON-NLS-1$
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
	{
		try
		{
			if (condition!=null) 
			{
				condition.saveRep(rep);
				rep.saveStepAttribute(id_transformation, id_step, "id_condition", condition.getID()); //$NON-NLS-1$
				rep.insertTransStepCondition(id_transformation, id_step, condition.getID());
				rep.saveStepAttribute(id_transformation, id_step, "send_true_to", getSendTrueStepname()); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, "send_false_to", getSendFalseStepname()); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("FilterRowsMeta.Exception.UnableToSaveStepInfoToRepository")+id_step, e); //$NON-NLS-1$
		}
	}
	
	public void searchInfoAndTargetSteps(ArrayList steps)
	{
		sendTrueStep  = StepMeta.findStep(steps, sendTrueStepname);
		sendFalseStep = StepMeta.findStep(steps, sendFalseStepname);
	}

    /**
     * @return true if this step chooses both target steps
     */
	public boolean chosesTargetSteps()
	{
	    return sendTrueStep!=null && sendFalseStep!=null;
	}

	public String[] getTargetSteps()
	{
	    if (chosesTargetSteps())
	    {
	        return new String[] { getSendTrueStepname(), getSendFalseStepname() };
	    }
	    return null;
	}
    
    /**
     * @param targetSteps The target step(s) to set
     */
    public void setTargetSteps(StepMeta[] targetSteps)
    {
        if (targetSteps!=null && targetSteps.length>0)
        {
            sendTrueStep  = targetSteps[0];
            sendFalseStep = targetSteps[1];
        }
    }

	public void check(List<CheckResult> remarks, StepMeta stepinfo, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		String error_message = ""; //$NON-NLS-1$
		
		if (getSendTrueStepname()!=null && getSendFalseStepname()!=null)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FilterRowsMeta.CheckResult.BothTrueAndFalseStepSpecified"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		if (getSendTrueStepname()==null && getSendFalseStepname()==null)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FilterRowsMeta.CheckResult.NeitherTrueAndFalseStepSpecified"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FilterRowsMeta.CheckResult.PlsSpecifyBothTrueAndFalseStep"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		
		// Look up fields in the input stream <prev>
		if (prev!=null && prev.size()>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FilterRowsMeta.CheckResult.StepReceivingFields",prev.size()+""), stepinfo); //$NON-NLS-1$ //$NON-NLS-2$
			remarks.add(cr);
			
			boolean first=true;
			error_message = ""; //$NON-NLS-1$
			boolean error_found = false;
			
			// What fields are used in the condition?
			String key[] = condition.getUsedFields();
			for (int i=0;i<key.length;i++)
			{
				ValueMetaInterface v = prev.searchValueMeta(key[i]);
				if (v==null)
				{
					if (first)
					{
						first=false;
						error_message+=Messages.getString("FilterRowsMeta.CheckResult.FieldsNotFoundFromPreviousStep")+Const.CR; //$NON-NLS-1$
					}
					error_found=true;
					error_message+="\t\t"+key[i]+Const.CR;  //$NON-NLS-1$
				}
			}
			if (error_found)
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepinfo);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FilterRowsMeta.CheckResult.AllFieldsFoundInInputStream"), stepinfo); //$NON-NLS-1$
			}
			remarks.add(cr);
		}
		else
		{
			error_message=Messages.getString("FilterRowsMeta.CheckResult.CouldNotReadFieldsFromPreviousStep")+Const.CR; //$NON-NLS-1$
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepinfo);
			remarks.add(cr);
		}

		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FilterRowsMeta.CheckResult.StepReceivingInfoFromOtherSteps"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("FilterRowsMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new FilterRowsDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface,  int cnr, TransMeta tr, Trans trans)
	{
		return new FilterRows(stepMeta, stepDataInterface, cnr, tr, trans);
	}

	public StepDataInterface getStepData()
	{
		return new FilterRowsData();
	}
}
