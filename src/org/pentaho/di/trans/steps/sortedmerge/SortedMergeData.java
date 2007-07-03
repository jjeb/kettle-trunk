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
 

package org.pentaho.di.trans.steps.sortedmerge;

import java.util.Comparator;
import java.util.List;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Matt
 * @since 24-jan-2005
 *
 */
public class SortedMergeData extends BaseStepData implements StepDataInterface
{
	public int[] fieldIndices;
    //public RowComparator rowComparator;
	public RowMetaInterface rowMeta;
	public List<RowSetRow> sortedBuffer;
	public Comparator<RowSetRow> comparator;

    /**
	 * 
	 */
	public SortedMergeData()
	{
		super();
	}
}
