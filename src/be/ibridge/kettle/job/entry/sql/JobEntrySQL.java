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
 
package be.ibridge.kettle.job.entry.sql;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Node;

import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Result;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.database.Database;
import be.ibridge.kettle.core.database.DatabaseMeta;
import be.ibridge.kettle.core.exception.KettleDatabaseException;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleXMLException;
import be.ibridge.kettle.core.util.StringUtil;
import be.ibridge.kettle.job.Job;
import be.ibridge.kettle.job.JobMeta;
import be.ibridge.kettle.job.entry.JobEntryBase;
import be.ibridge.kettle.job.entry.JobEntryDialogInterface;
import be.ibridge.kettle.job.entry.JobEntryInterface;
import be.ibridge.kettle.repository.Repository;


/**
 * This defines an SQL job entry.
 * 
 * @author Matt
 * @since 05-11-2003
 *
 */
public class JobEntrySQL extends JobEntryBase implements Cloneable, JobEntryInterface
{
	private String sql;
	private DatabaseMeta connection;
	private boolean useVariableSubstitution = false;

	public JobEntrySQL(String n)
	{
		super(n, "");
		sql=null;
		connection=null;
		setID(-1L);
		setType(JobEntryInterface.TYPE_JOBENTRY_SQL);
	}

	public JobEntrySQL()
	{
		this("");
	}

	public JobEntrySQL(JobEntryBase jeb)
	{
		super(jeb);
	}
    
    public Object clone()
    {
        JobEntrySQL je = (JobEntrySQL) super.clone();
        return je;
    }

	public String getXML()
	{
        StringBuffer retval = new StringBuffer(200);
		
		retval.append(super.getXML());
		
		retval.append("      ").append(XMLHandler.addTagValue("sql",      sql));
		retval.append("      ").append(XMLHandler.addTagValue("useVariableSubstitution", useVariableSubstitution ? "T" : "F"));
		retval.append("      ").append(XMLHandler.addTagValue("connection", connection==null?null:connection.getName()));
		
		return retval.toString();
	}
	
	public void loadXML(Node entrynode, ArrayList databases, Repository rep) throws KettleXMLException
	{
		try
		{
			super.loadXML(entrynode, databases);
			sql           = XMLHandler.getTagValue(entrynode, "sql");
			String dbname = XMLHandler.getTagValue(entrynode, "connection");
			String sSubs  = XMLHandler.getTagValue(entrynode, "useVariableSubstitution");
			if (sSubs != null && sSubs.equalsIgnoreCase("T"))
				useVariableSubstitution = true;
			connection    = DatabaseMeta.findDatabase(databases, dbname);
		}
		catch(KettleException e)
		{
			throw new KettleXMLException("Unable to load job entry of type 'sql' from XML node", e);
		}
	}

	public void loadRep(Repository rep, long id_jobentry, ArrayList databases)
		throws KettleException
	{
		try
		{			
			super.loadRep(rep, id_jobentry, databases);

			sql = rep.getJobEntryAttributeString(id_jobentry, "sql");
			String sSubs = rep.getJobEntryAttributeString(id_jobentry, "useVariableSubstitution");
			if (sSubs != null && sSubs.equalsIgnoreCase("T"))
				useVariableSubstitution = true;
			long id_db = rep.getJobEntryAttributeInteger(id_jobentry, "id_database");
			if (id_db>0)
			{
				connection = DatabaseMeta.findDatabase(databases, id_db);
			}
			else
			{
				// This is were we end up in normally, the previous lines are for backward compatibility.
				connection = DatabaseMeta.findDatabase(databases, rep.getJobEntryAttributeString(id_jobentry, "connection"));
			}
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to load job entry of type 'sql' from the repository with id_jobentry="+id_jobentry, dbe);
		}
	}
	
	// Save the attributes of this job entry
	//
	public void saveRep(Repository rep, long id_job)
		throws KettleException
	{
		try
		{
			super.saveRep(rep, id_job);

			if (connection!=null) rep.saveJobEntryAttribute(id_job, getID(), "connection", connection.getName());
			rep.saveJobEntryAttribute(id_job, getID(), "sql", sql);
			rep.saveJobEntryAttribute(id_job, getID(), "useVariableSubstitution", useVariableSubstitution ? "T" : "F" );
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to save job entry of type 'sql' to the repository for id_job="+id_job, dbe);
		}
	}

	public void setSQL(String sql)
	{
		this.sql = sql;
	}
	
	public String getSQL()
	{
		return sql;
	}
	
	public boolean getUseVariableSubstitution()
	{
		return useVariableSubstitution;
	}
	
	public void setUseVariableSubstitution(boolean subs)
	{
		useVariableSubstitution = subs;
	}
	
	public void setDatabase(DatabaseMeta database)
	{
		this.connection = database;
	}
	
	public DatabaseMeta getDatabase()
	{
		return connection;
	}

	public Result execute(Result prev_result, int nr, Repository rep, Job parentJob)
	{
		LogWriter log = LogWriter.getInstance();

		Result result = new Result(nr);
		
		if (connection!=null)
		{
			Database db = new Database(connection);
			try
			{
				db.connect();
				String mySQL = null;
				if (useVariableSubstitution)
					mySQL = StringUtil.environmentSubstitute(sql);
				else
					mySQL = sql;
				db.execStatements(mySQL);
			}
			catch(KettleDatabaseException je)
			{
				result.setNrErrors(1);
				log.logError(toString(), "An error occurred executing this job entry : "+je.getMessage());
			}
			finally
			{
				db.disconnect();
			}	
		}
		else
		{
			result.setNrErrors(1);
			log.logError(toString(), "No database connection is defined.");
		}
		
		if (result.getNrErrors()==0)
		{
			result.setResult(true);
		}
		else
		{
			result.setResult(false);
		}

		return result;
	}

	public boolean evaluates()
	{
		return true;
	}

	public boolean isUnconditional()
	{
		return true;
	}

    public JobEntryDialogInterface getDialog(Shell shell,JobEntryInterface jei,JobMeta jobMeta,String jobName,Repository rep) {
        return new JobEntrySQLDialog(shell,this,jobMeta);
    }
    
    public DatabaseMeta[] getUsedDatabaseConnections()
    {
        return new DatabaseMeta[] { connection, };
    }

}