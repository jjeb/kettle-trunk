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

package org.pentaho.di.trans.steps.orabulkloader;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.util.StringUtil;


/**
 * Does the opening of the output "stream". It's either a file or inter process
 * communication which is transparant to users of this class.
 *
 * @author Sven Boden
 * @since  20-feb-2007
 */
public class OraBulkDataOutput 
{
	private OraBulkLoaderMeta meta;
	private PrintWriter       output = null;
	private boolean           first = true;
	private int               fieldNumbers[] = null;
	private String            enclosure = null;
	private SimpleDateFormat  sdfDate = null;
	private SimpleDateFormat  sdfDateTime = null;

	public OraBulkDataOutput(OraBulkLoaderMeta meta)
	{
		this.meta = meta;
	}
	
	public void open(Process sqlldrProcess) throws KettleException
	{
		// String loadMethod = meta.getLoadMethod();
		try 
		{
			OutputStream os = null;

		//	if ( OraBulkLoaderMeta.METHOD_AUTO_CONCURRENT.equals(loadMethod))
		//	{
		//		String dataFile = meta.getControlFile();
		//		dataFile = StringUtil.environmentSubstitute(dataFile);
				
        //      os = new FileOutputStream(dataFile, true);
		//	}
		//	else
		//	{
				// Else open the data file filled in.
				String dataFile = meta.getDataFile();
				dataFile = StringUtil.environmentSubstitute(dataFile);
				
                os = new FileOutputStream(dataFile, false);
		//	}	
			
            String encoding = meta.getEncoding();
            if ( Const.isEmpty(encoding) )
            {
            	// Use the default encoding.
			    output = new PrintWriter(
  	                      	     new BufferedWriter(
							      new OutputStreamWriter(os)));
            }
            else
            {
            	// Use the specified encoding
			    output = new PrintWriter(
  	                      	     new BufferedWriter(
							      new OutputStreamWriter(os, encoding)));
            }
		}
		catch ( IOException e )
		{
			throw new KettleException("IO exception occured: "  + e.getMessage(), e);
		}
	}
	
	public void close() throws IOException
	{
		if ( output != null )
		{
			output.close();
		}
	}
	
	PrintWriter getOutput()
	{
	    return output;
	}
	
	private String createEscapedString(String orig, String enclosure)
	{
		StringBuffer buf = new StringBuffer(orig);
		
		Const.repl(buf, enclosure, enclosure + enclosure);
		return buf.toString();
	}
	
	public void writeLine(RowMetaInterface mi, Object row[]) throws KettleException
	{
        if ( first )
        {
            first = false;
     
            enclosure = meta.getEnclosure();
            
            // Setup up the fields we need to take for each of the rows
            // as this speeds up processing.
            fieldNumbers=new int[meta.getFieldStream().length];
			for (int i=0;i<fieldNumbers.length;i++) 
			{
				fieldNumbers[i]=mi.indexOfValue(meta.getFieldStream()[i]);
				if (fieldNumbers[i]<0)
				{
					throw new KettleException("Could not find field " + 
							                  meta.getFieldStream()[i] + " in stream");
				}
			}
			
			sdfDate = new SimpleDateFormat("yyyy-MM-dd");
			sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        }
        
        // Write the data to the output
        ValueMetaInterface v = null;
        int number = 0;
		for (int i=0;i<fieldNumbers.length;i++) 
		{
			if ( i!=0 ) output.print(",");
			v = mi.getValueMeta(i);
			number = fieldNumbers[i];
			if ( row[number] == null)
			{
				// TODO (SB): special check for null in case of Strings.
				output.print(enclosure);
				output.print(enclosure);
			}
			else
			{
				switch ( v.getType() )
				{
				case ValueMetaInterface.TYPE_STRING:
					String s = mi.getString(row, number);
					if ( s.indexOf(enclosure) >= 0 )
						s = createEscapedString(s, enclosure);
					output.print(enclosure);
					output.print(s);
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_INTEGER:
					Long l = mi.getInteger(row, number);
					output.print(enclosure);
					output.print(l);
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_NUMBER:
					Double d = mi.getNumber(row, number);
					output.print(enclosure);
					output.print(d);
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_BIGNUMBER:
					BigDecimal bd = mi.getBigNumber(row, number);
					output.print(enclosure);
					output.print(bd);
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_DATE:
					Date dt = mi.getDate(row, number);
					output.print(enclosure);
					String mask = meta.getDateMask()[i];
					if ( OraBulkLoaderMeta.DATE_MASK_DATETIME.equals(mask))
					{
						output.print(sdfDateTime.format(dt));	
					}
					else
					{
						// Default is date format
						output.print(sdfDate.format(dt));
					}					   
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_BOOLEAN:
					Boolean b = mi.getBoolean(row, number);
					output.print(enclosure);
					if ( b.booleanValue() )
						output.print("Y");
					else
						output.print("N");
					output.print(enclosure);
					break;			    	
				case ValueMetaInterface.TYPE_BINARY:
					byte byt[] = mi.getBinary(row, number);
					output.print("<startlob>");
					output.print(byt);
					output.print("<endlob>");
					break;			    
				default:
					throw new KettleException("Unsupported type");
				}
			}
		}
		output.print(Const.CR);
	}
	
	public String toString()
	{
		return this.getClass().getName();
	}
}