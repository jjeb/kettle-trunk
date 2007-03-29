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
 

package be.ibridge.kettle.trans.step;
import java.util.ArrayList;

import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.SQLStatement;
import be.ibridge.kettle.core.database.Database;
import be.ibridge.kettle.core.database.DatabaseMeta;
import be.ibridge.kettle.core.exception.KettleDatabaseException;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleStepException;
import be.ibridge.kettle.trans.TransMeta;


/*
 * Created on 19-jun-2003
 *
 */

public class BaseStepMeta implements Cloneable
{
	private boolean changed;
	private long    id;
    
    /** database connection object to use for searching fields & checking steps */
    protected Database databases[];

	public BaseStepMeta()
	{
		changed    = false;
	}
	
	public long getID()
	{
		return id;
	}
	
	public void setID(long id)
	{
		this.id = id;
	}

	public Object clone()
	{
		try
		{
			Object retval = super.clone();
			return retval;
		}
		catch(CloneNotSupportedException e)
		{
			return null;
		}
	}
	
	public void setChanged(boolean ch)
	{
		changed=ch;
	}

	public void setChanged()
	{
		changed=true;
	}
	
	public boolean hasChanged()
	{
		return changed;
	}
	
	public Row getTableFields()
	{
		return null;
	}
	
	public void searchInfoAndTargetSteps(ArrayList steps)
	{
	}

	public boolean chosesTargetSteps()
	{
	    return false;
	}

	public String[] getTargetSteps()
	{
	    return null;
	}

	/**
	 * @return the informational source steps, if any. Null is the default: none.
	 */
	public String[] getInfoSteps()
	{
	    return null;
	}
    
    /**
     * @param infoSteps The infostep(s) to set
     */
    public void setInfoSteps(StepMeta[] infoSteps)
    {
    }
    
    /**
     * @param targetSteps The target step(s) to set
     */
    public void setTargetSteps(StepMeta[] targetSteps)
    {
    }

	/**
	 * Produces the XML string that describes this step's information.
	 * 
	 * @return String containing the XML describing this step.
	 */
	public String getXML()
	{
		String retval="";

		return retval;
	}

	
	/**
	    getFields determines which fields are
	      - added to the stream
	      - removed from the stream
	      - renamed
	      - changed
	        
	 * @param r Row containing fields that are used as input for the step.
	 * @param name Name of the step
	 * @param info Fields used as extra lookup information
	 * 
	 * @return The fields that are being put out by this step.
	 */
	public Row getFields(Row r, String name, Row info) throws KettleStepException
	{
		Row row;
		if (r==null) row=new Row(); // give back values
		else         row=r;         // add to the existing row of values...
		
		// Default: no values are added to the row in the step
	
		return row;
	}

    /**
     * Each step must be able to report on the impact it has on a database, table field, etc.
     * @param impact The list of impacts @see be.ibridge.kettle.transMeta.DatabaseImpact
     * @param transMeta The transformation information
     * @param stepMeta The step information
     * @param prev The fields entering this step
     * @param input The previous step names
     * @param output The output step names
     * @param info The fields used as information by this step
     */
    public void analyseImpact(ArrayList impact, TransMeta transMeta, StepMeta stepMeta, Row prev, String input[], String output[], Row info) throws KettleStepException
    {
        
    }
	
    
	/**
	 * Standard method to return one or more SQLStatement objects that the step needs in order to work correctly.
	 * This can mean "create table", "create index" statements but also "alter table ... add/drop/modify" statements.
	 *
	 * @return The SQL Statements for this step or null if an error occurred.  If nothing has to be done, the SQLStatement.getSQL() == null. 
	 * @param transMeta TransInfo object containing the complete transformation
	 * @param stepMeta StepMeta object containing the complete step
	 * @param prev Row containing meta-data for the input fields (no data)
	 */
	public SQLStatement getSQLStatements(TransMeta transMeta, StepMeta stepMeta, Row prev)  throws KettleStepException
	{
		// default: this doesn't require any SQL statements to be executed!
		return new SQLStatement(stepMeta.getName(), null, null);
	}
    
    /**
     *  Call this to cancel trailing database queries (too long running, etc)
     */
    public void cancelQueries() throws KettleDatabaseException
    {
        //
        // Cancel all defined queries...
        //
        if (databases!=null)
        {
            for (int i=0;i<databases.length;i++)
            {
                if (databases[i]!=null) databases[i].cancelQuery();
            }
        }
    }
    
    /**
     * Default a step doesn't use any arguments.
     * Implement this to notify the GUI that a window has to be displayed BEFORE launching a transformation.
     * 
     * @return A row of argument values. (name and optionally a default value)
     */
    public Row getUsedArguments()
    {
        return new Row();
    }

    /**
     * The natural way of data flow in a transformation is source-to-target.
     * However, this makes mapping to target tables difficult to do.
     * To help out here, we supply information to the transformation meta-data model about which fields are required for a step.
     * This allows us to automate certain tasks like the mapping to pre-defined tables.
     * The Table Output step in this case will output the fields in the target table using this method. 
     * 
     * This default implementation returns an empty row meaning that no fields are required for this step to operate.
     * @return the required fields for this steps metadata.
     * @throws KettleException in case the required fields can't be determined
     */
    public Row getRequiredFields() throws KettleException
    {
        return new Row();
    }

    /**
     * This method returns all the database connections that are used by the step.
     * @return an array of database connections meta-data.
     *         Return an empty array if no connections are used.
     */
    public DatabaseMeta[] getUsedDatabaseConnections()
    {
        return new DatabaseMeta[] { };
    }
    
    /**
     * @return the libraries that this step or plugin uses.
     */
    public String[] getUsedLibraries()
    {
        return new String[] {};
    }
        
    /**
     * @return true if this step supports error "reporting" on rows: the ability to send rows to a certain target step.
     */
    public boolean supportsErrorHandling()
    {
        return false;
    }
}
