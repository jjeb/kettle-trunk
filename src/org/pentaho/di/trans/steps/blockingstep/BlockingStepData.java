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
 
package org.pentaho.di.trans.steps.blockingstep;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;


public class BlockingStepData extends BaseStepData implements StepDataInterface
{
	public List<FileObject>        files;
	public List<Object[]>          buffer;
	public List<InputStream>       fis;
	public List<GZIPInputStream>   gzis;
	public List<DataInputStream>   dis;
	public List<Object[]>          rowbuffer;
	
    public RowMetaInterface outputRowMeta;

	public int        fieldnrs[];    // the corresponding field numbers;
    public FileObject fil;

    public BlockingStepData()
    {
        super();
        		
		buffer    = new ArrayList<Object[]>(BlockingStepMeta.CACHE_SIZE);
		files     = new ArrayList<FileObject>();
		fis       = new ArrayList<InputStream>();
		dis       = new ArrayList<DataInputStream>();
		gzis      = new ArrayList<GZIPInputStream>();
		rowbuffer = new ArrayList<Object[]>();
    }
}