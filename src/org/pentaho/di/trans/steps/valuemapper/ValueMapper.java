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
 
package org.pentaho.di.trans.steps.valuemapper;

import java.util.Hashtable;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Convert Values in a certain fields to other values
 * 
 * @author Matt 
 * @since 3-apr-2006
 */
public class ValueMapper extends BaseStep implements StepInterface
{
	private ValueMapperMeta meta;
	private ValueMapperData data;
	private boolean nonMatchActivated = false;
	
	public ValueMapper(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(ValueMapperMeta)smi;
		data=(ValueMapperData)sdi;
		
	    // Get one row from one of the rowsets...
        //
		Object[] r = getRow();
		if (r==null)  // means: no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		if (first)
		{
		    first=false;
		    
		    data.previousMeta = (RowMetaInterface) getInputRowMeta().clone();
		    data.outputMeta = (RowMetaInterface) data.previousMeta.clone();
		    meta.getFields(data.outputMeta, getStepname(), null, null, null);
		    data.keynr     = data.previousMeta.indexOfValue(meta.getFieldToUse());
            if (data.keynr<0)
            {
                String message = Messages.getString("ValueMapper.RuntimeError.FieldToUseNotFound.VALUEMAPPER0001", meta.getFieldToUse(), Const.CR, r.toString()); //$NON-NLS-1$ 
                log.logError(toString(), message);
                setErrors(1);
                stopAll();
                return false;
            }
            
            // If there is an empty entry: we map null or "" to the target at the index
            // 0 or 1 empty mapping is allowed, not 2 or more.
            // 
            for (int i=0;i<meta.getSourceValue().length;i++)
            {
                if (Const.isEmpty(meta.getSourceValue()[i]))
                {
                    if (data.emptyFieldIndex<0)
                    {
                        data.emptyFieldIndex=i;
                    }
                    else
                    {
                        throw new KettleException(Messages.getString("ValueMapper.RuntimeError.OnlyOneEmptyMappingAllowed.VALUEMAPPER0004"));
                    }
                }
            }
            
		}

        String source = data.previousMeta.getString(r, data.keynr);
        String target = null;
        
        // Null/Empty mapping to value...
        //
        if (data.emptyFieldIndex>=0 && (r[data.keynr]==null || Const.isEmpty(source)) )
        {
            target = meta.getTargetValue()[data.emptyFieldIndex]; // that's all there is to it.
        }
        else
        {
            if (!Const.isEmpty(source))
            {
                target=(String)data.hashtable.get(source);
                if ( nonMatchActivated && target == null )
                {
                	// If we do non matching and we don't have a match
                	target = meta.getNonMatchDefault();
                }
            }
        }

        if (!Const.isEmpty(meta.getTargetField()))
        {
        	// room for the target
        	r=RowDataUtil.resizeArray(r, data.outputMeta.size());
            // Did we find anything to map to?
            if (!Const.isEmpty(target)) 
            {
            	r[data.outputMeta.size()-1]=target;
            }
            else
            {
            	r[data.outputMeta.size()-1]=null;
            }
            putRow(data.outputMeta, r);
        }
        else
        {
            // Don't set the original value to null if we don't have a target.
            if (!Const.isEmpty(target)) r[data.keynr]=target;
            putRow(data.outputMeta, r);
        }
        
		return true;
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(ValueMapperMeta)smi;
		data=(ValueMapperData)sdi;

		super.dispose(smi, sdi);
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(ValueMapperMeta)smi;
		data=(ValueMapperData)sdi;
		
		if (super.init(smi, sdi))
		{
		    data.hashtable = new Hashtable<String,String>();
            data.emptyFieldIndex=-1;

            if ( !Const.isEmpty(meta.getNonMatchDefault()) )
            {
            	nonMatchActivated = true;
            }
            
            // Add all source to target mappings in here...
            for (int i=0;i<meta.getSourceValue().length;i++)
            {
                String src = meta.getSourceValue()[i];
                String tgt = meta.getTargetValue()[i];
            
                if (!Const.isEmpty(src) && !Const.isEmpty(tgt))
                {
                    data.hashtable.put(src, tgt);
                }
                else
                {
                    if (Const.isEmpty(tgt))
                    {
                        log.logError(toString(), Messages.getString("ValueMapper.RuntimeError.ValueNotSpecified.VALUEMAPPER0002", ""+i)); //$NON-NLS-1$ //$NON-NLS-2$
                        return false;
                    }
                }
            }
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
			logBasic(Messages.getString("ValueMapper.Log.StartingToRun")); //$NON-NLS-1$
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError(Messages.getString("ValueMapper.RuntimeError.UnexpectedError.VALUEMAPPER0003", e.toString())); //$NON-NLS-1$
            logError(Const.getStackTracker(e));
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
