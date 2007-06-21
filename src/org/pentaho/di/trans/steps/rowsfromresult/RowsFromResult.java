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

package org.pentaho.di.trans.steps.rowsfromresult;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Reads results from a previous transformation in a Job
 * 
 * @author Matt
 * @since 2-jun-2003
 */

public class RowsFromResult extends BaseStep implements StepInterface
{
	private RowsFromResultMeta meta;

	private RowsFromResultData data;

	public RowsFromResult(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
			TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);

		meta = (RowsFromResultMeta) getStepMeta().getStepMetaInterface();
		data = (RowsFromResultData) stepDataInterface;
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		Result previousResult = getTransMeta().getPreviousResult();
		if (previousResult == null || linesRead >= previousResult.getRows().size())
		{
			setOutputDone();
			return false;
		}

		Object[] r = (Object[]) previousResult.getRows().get((int) linesRead);
		linesRead++;
		data = (RowsFromResultData) sdi;
		data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
		meta.getFields(data.outputRowMeta, getStepname(), null);
		putRow(data.outputRowMeta, r); // copy row to possible alternate
										// rowset(s).

		if (checkFeedback(linesRead))
			logBasic(Messages.getString("RowsFromResult.Log.LineNumber") + linesRead); //$NON-NLS-1$

		return true;
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta = (RowsFromResultMeta) smi;
		data = (RowsFromResultData) sdi;

		if (super.init(smi, sdi))
		{
			// Add init code here.
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
			logBasic(Messages.getString("RowsFromResult.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped())
				;
		} catch (Exception e)
		{
			logError(Messages.getString("RowsFromResult.Log.UnexpectedError") + " : " + e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			logError(Const.getStackTracker(e));
			setErrors(1);
			stopAll();
		} finally
		{
			dispose(meta, data);
			logSummary();
			markStop();
		}
	}
}
