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
 **********************************************************************/

package org.pentaho.di.trans.steps.scriptvalues_mod;

import java.util.List;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.trans.step.RowListener;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Interface to make the real ScriptValueMod and ScriptValueModDummy similar.
 * 
 * @author Sven Boden
 */
public interface ScriptValuesModInterface extends VariableSpace
{
	boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException;

	void addRowListener(RowListener rowListener);

	void dispose(StepMetaInterface sii, StepDataInterface sdi);

	long getErrors();

	List<RowSet> getInputRowSets();

	long getLinesInput();

	long getLinesOutput();

	long getLinesRead();

	long getLinesUpdated();

	long getLinesWritten();
	
	long getLinesRejected();

	List<RowSet> getOutputRowSets();

	String getPartitionID();

	Object[] getRow() throws KettleException;

	List<RowListener> getRowListeners();

	String getStepID();

	String getStepname();

	boolean init(StepMetaInterface stepMetaInterface, StepDataInterface stepDataInterface);

	boolean isAlive();

	boolean isPartitioned();

	boolean isStopped();

	void markStart();

	void markStop();

	void putRow(RowMetaInterface rowMeta, Object[] row) throws KettleException;

	void removeRowListener(RowListener rowListener);

	void run();

	void setErrors(long errors);

	void setOutputDone();

	void setPartitionID(String partitionID);

	void start();

	void stopAll();

	void stopRunning(StepMetaInterface stepMetaInterface, StepDataInterface stepDataInterface) throws KettleException;

	void cleanup();

	void pauseRunning();

	void resumeRunning();
};