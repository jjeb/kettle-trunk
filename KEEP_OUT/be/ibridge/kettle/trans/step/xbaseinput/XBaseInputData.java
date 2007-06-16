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
 
package be.ibridge.kettle.trans.step.xbaseinput;

import org.apache.commons.vfs.FileObject;

import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.XBase;
import be.ibridge.kettle.trans.step.BaseStepData;
import be.ibridge.kettle.trans.step.StepDataInterface;
import be.ibridge.kettle.trans.step.fileinput.FileInputList;


/**
 * Provides data for the XBaseInput step.
 * 
 * @author Matt
 * @since 20-jan-2005
 */
public class XBaseInputData extends BaseStepData implements StepDataInterface
{
	public XBase xbi;
	public Row fields;
    public int fileNr;
    public FileObject file_dbf;
    public FileInputList files;

	public XBaseInputData()
	{
		super();

		xbi=null;
		fields=null;
	}

}
