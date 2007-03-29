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
 
package be.ibridge.kettle.trans.step.setvariable;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.KettleVariables;
import be.ibridge.kettle.core.LocalVariables;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleStepException;
import be.ibridge.kettle.job.Job;
import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransMeta;
import be.ibridge.kettle.trans.step.BaseStep;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.StepInterface;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;


/**
 * Convert Values in a certain fields to other values
 * 
 * @author Matt 
 * @since 27-apr-2006
 */
public class SetVariable extends BaseStep implements StepInterface
{
	private SetVariableMeta meta;
	private SetVariableData data;
	
	public SetVariable(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(SetVariableMeta)smi;
		data=(SetVariableData)sdi;
		
	    // Get one row from one of the rowsets...
        //
		Row r = getRow();
		if (r==null)  // means: no more input to be expected...
		{
            logBasic("Finished after "+linesWritten+" rows.");
			setOutputDone();
			return false;
		}
		
		if (first)
		{
		    first=false;
            
            logBasic("Setting environment variables...");

            for (int i=0;i<meta.getFieldName().length;i++)
            {
                // Set the appropriate environment variable
                String value = r.getString(meta.getFieldName()[i], "");
                
                String varname = meta.getVariableName()[i];
                
                if (Const.isEmpty(varname))
                {
                    if (Const.isEmpty(value))
                    {
                        throw new KettleException("Variable name nor value was specified on line #"+(i+1));
                    }
                    else
                    {
                        throw new KettleException("There was no variable name specified for value ["+value+"]");
                    }
                }
                
                // OK, where do we set this value...
                switch(meta.getVariableType()[i])
                {
                case SetVariableMeta.VARIABLE_TYPE_JVM: 
                    System.setProperty(varname, value); 
                    break;
                case SetVariableMeta.VARIABLE_TYPE_ROOT_JOB:
                    {
                        KettleVariables.getInstance().setVariable(varname, value);

                        Job parentJob = getTrans().getParentJob();
                        Job rootJob = parentJob;
                        while (parentJob!=null)
                        {
                            KettleVariables vars = LocalVariables.getKettleVariables(parentJob.getName());
                            vars.setVariable(varname, value);
                            rootJob = parentJob;
                            parentJob = parentJob.getParentJob();
                        }
                        // OK, we have the rootjob, set the variable on it...
                        if (rootJob==null)
                        {
                            throw new KettleStepException("Can't set variable ["+varname+"] on root job: the root job is not available (meaning: not even the parent job)");
                        }
                    }
                    break;
                case SetVariableMeta.VARIABLE_TYPE_GRAND_PARENT_JOB:
                    {
                        KettleVariables.getInstance().setVariable(varname, value);

                        Job parentJob = getTrans().getParentJob();
                        if (parentJob!=null)
                        {
                            parentJob.getKettleVariables().setVariable(varname, value);
                            Job gpJob = parentJob.getParentJob();
                            if (gpJob!=null)
                            {
                                gpJob.getKettleVariables().setVariable(varname, value);
                            }
                            else
                            {
                                throw new KettleStepException("Can't set variable ["+varname+"] on grand parent job: the grand parent job is not available");
                            }
                        }
                        else
                        {
                            throw new KettleStepException("Can't set variable ["+varname+"] on grand parent job: the parent job is not available");
                        }
                    }
                case SetVariableMeta.VARIABLE_TYPE_PARENT_JOB:
                    {
                        // This thread, the transformation and the parent job run in the same namespace.
                        //
                        KettleVariables.getInstance().setVariable(varname, value);
                    }
                }
                
                logBasic("Set variable "+meta.getVariableName()[i]+" to value ["+value+"]");
            }
            
            putRow(r);
            return true;
		}

        throw new KettleStepException(Messages.getString("SetVariable.RuntimeError.MoreThanOneRowReceived.SETVARIABLE0007"));
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SetVariableMeta)smi;
		data=(SetVariableData)sdi;

		super.dispose(smi, sdi);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SetVariableMeta)smi;
		data=(SetVariableData)sdi;
		
		if (super.init(smi, sdi))
		{
            return true;
		}
		return false;
	}

	//
	// Run is were the action happens!
	public void run()
	{
		try
		{
			logBasic(Messages.getString("SetVariable.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("SetVariable.RuntimeError.UnexpectedError.SETVARIABLE0003", e.toString())); //$NON-NLS-1$
            logError(Const.getStackTracker(e)); //$NON-NLS-1$
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
