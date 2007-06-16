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
 
package org.pentaho.di.job.entries.shell;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.Log4jFileAppender;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.StreamLogger;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;


/**
 * Shell type of Job Entry.  You can define shell scripts to be executed in a Job.
 * 
 * @author Matt
 * @since 01-10-2003, rewritten on 18-06-2004
 * 
 */
public class JobEntryShell extends JobEntryBase implements Cloneable, JobEntryInterface
{	
	private String  filename;
	public  String  arguments[];
	public  boolean argFromPrevious;

    public  boolean setLogfile;
	public  String  logfile, logext;
	public  boolean addDate, addTime;
	public  int     loglevel;
	
	public  boolean execPerRow;
	
	public JobEntryShell(String name)
	{
		super(name, "");
		setType(JobEntryInterface.TYPE_JOBENTRY_SHELL);
	}

	public JobEntryShell()
	{
		this("");
		clear();
	}
	
	public JobEntryShell(JobEntryBase jeb)
	{
		super(jeb);
		setType(JobEntryInterface.TYPE_JOBENTRY_SHELL);
	}
    
    public Object clone()
    {
        JobEntryShell je = (JobEntryShell) super.clone();
        return je;
    }

	public String getXML()
	{
        StringBuffer retval = new StringBuffer(300);
		
		retval.append(super.getXML());
		
		retval.append("      ").append(XMLHandler.addTagValue("filename",          filename));
		retval.append("      ").append(XMLHandler.addTagValue("arg_from_previous", argFromPrevious));
        retval.append("      ").append(XMLHandler.addTagValue("exec_per_row",      execPerRow));
		retval.append("      ").append(XMLHandler.addTagValue("set_logfile",       setLogfile));
		retval.append("      ").append(XMLHandler.addTagValue("logfile",           logfile));
		retval.append("      ").append(XMLHandler.addTagValue("logext",            logext));
		retval.append("      ").append(XMLHandler.addTagValue("add_date",          addDate));
		retval.append("      ").append(XMLHandler.addTagValue("add_time",          addTime));
		retval.append("      ").append(XMLHandler.addTagValue("loglevel",          LogWriter.getLogLevelDesc(loglevel)));

		if (arguments!=null)
		for (int i=0;i<arguments.length;i++)
		{
			retval.append("      ").append(XMLHandler.addTagValue("argument"+i, arguments[i]));
		}

		return retval.toString();
	}
				
	public void loadXML(Node entrynode, ArrayList databases, Repository rep) throws KettleXMLException
	{
		try
		{
			super.loadXML(entrynode, databases);
			setFileName( XMLHandler.getTagValue(entrynode, "filename") );
			argFromPrevious = "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "arg_from_previous") );
            execPerRow = "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "exec_per_row") );
            setLogfile = "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "set_logfile") );
			addDate = "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "add_date") );
			addTime = "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "add_time") );
			logfile = XMLHandler.getTagValue(entrynode, "logfile");
			logext = XMLHandler.getTagValue(entrynode, "logext");
			loglevel = LogWriter.getLogLevel( XMLHandler.getTagValue(entrynode, "loglevel"));
	
			// How many arguments?
			int argnr = 0;
			while ( XMLHandler.getTagValue(entrynode, "argument"+argnr)!=null) argnr++;
			arguments = new String[argnr];
			
			// Read them all...
			for (int a=0;a<argnr;a++) arguments[a]=XMLHandler.getTagValue(entrynode, "argument"+a);
		}
		catch(KettleException e)
		{
			throw new KettleXMLException("Unable to load job entry of type 'shell' from XML node", e);			
		}
	}
	
	// Load the jobentry from repository
	public void loadRep(Repository rep, long id_jobentry, ArrayList databases)
		throws KettleException
	{
		try
		{
			super.loadRep(rep, id_jobentry, databases);
			
			setFileName( rep.getJobEntryAttributeString(id_jobentry, "file_name")  );
			argFromPrevious = rep.getJobEntryAttributeBoolean(id_jobentry, "arg_from_previous");
            execPerRow       = rep.getJobEntryAttributeBoolean(id_jobentry, "exec_per_row");
	
			setLogfile = rep.getJobEntryAttributeBoolean(id_jobentry, "set_logfile");
			addDate = rep.getJobEntryAttributeBoolean(id_jobentry, "add_date");
			addTime = rep.getJobEntryAttributeBoolean(id_jobentry, "add_time");
			logfile = rep.getJobEntryAttributeString(id_jobentry, "logfile");
			logext = rep.getJobEntryAttributeString(id_jobentry, "logext");
			loglevel = LogWriter.getLogLevel( rep.getJobEntryAttributeString(id_jobentry, "loglevel") );
	
			// How many arguments?
			int argnr = rep.countNrJobEntryAttributes(id_jobentry, "argument");
			arguments = new String[argnr];
			
			// Read them all...
			for (int a=0;a<argnr;a++) 
			{
				arguments[a]= rep.getJobEntryAttributeString(id_jobentry, a, "argument");
			}
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to load job entry of type 'shell' from the repository with id_jobentry="+id_jobentry, dbe);
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
			
			rep.saveJobEntryAttribute(id_job, getID(), "file_name", filename);
			rep.saveJobEntryAttribute(id_job, getID(), "arg_from_previous", argFromPrevious);
            rep.saveJobEntryAttribute(id_job, getID(), "exec_per_row", execPerRow);
			rep.saveJobEntryAttribute(id_job, getID(), "set_logfile", setLogfile);
			rep.saveJobEntryAttribute(id_job, getID(), "add_date", addDate);
			rep.saveJobEntryAttribute(id_job, getID(), "add_time", addTime);
			rep.saveJobEntryAttribute(id_job, getID(), "logfile", logfile);
			rep.saveJobEntryAttribute(id_job, getID(), "logext", logext);
			rep.saveJobEntryAttribute(id_job, getID(), "loglevel", LogWriter.getLogLevelDesc(loglevel));
				
			// save the arguments...
			if (arguments!=null)
			{
				for (int i=0;i<arguments.length;i++) 
				{
					rep.saveJobEntryAttribute(id_job, getID(), i, "argument", arguments[i]);
				}
			}
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to save job entry of type 'shell' to the repository", dbe);
		}
	}
	
	public void clear()
	{
		super.clear();
		
		filename=null;
		arguments=null;
		argFromPrevious=false;
		addDate=false;
		addTime=false;
		logfile=null;
		logext=null;
		setLogfile=false;
        execPerRow=false;
	}

	public void setFileName(String n)
	{
		filename=n;
	}
	
    /**
     * @deprecated use getFilename() instead
     * @return the filename
     */
    public String getFileName()
	{
		return filename;
	}

    public String getFilename()
    {
        return filename;
    }

    public String getRealFilename()
    {
        return StringUtil.environmentSubstitute(getFilename());
    }
    
	public String getLogFilename()
	{
		String retval="";
		if (setLogfile)
		{
			retval+=logfile;
			Calendar cal = Calendar.getInstance();
			if (addDate)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				retval+="_"+sdf.format(cal.getTime());
			}
			if (addTime)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
				retval+="_"+sdf.format(cal.getTime());
			}
			if (logext!=null && logext.length()>0)
			{
				retval+="."+logext;
			}
		}
		return retval;
	}
	
	public Result execute(Result result, int nr, Repository rep, Job parentJob) throws KettleException
	{
		LogWriter log = LogWriter.getInstance();
        
        Log4jFileAppender appender = null;
        int backupLogLevel = log.getLogLevel();
        if (setLogfile)
        {
            try
            {
                appender = LogWriter.createFileAppender(StringUtil.environmentSubstitute(getLogFilename()), true);
            }
            catch(KettleException e)
            {
                log.logError(toString(), "Unable to open file appender for file ["+getLogFilename()+"] : "+e.toString());
                log.logError(toString(), Const.getStackTracker(e));
                result.setNrErrors(1);
                result.setResult(false);
                return result;
            }
            log.addAppender(appender);
            log.setLogLevel(loglevel);
        }

		result.setEntryNr( nr );
		
        int iteration = 0;
        String args[] = arguments;
        RowMetaAndData resultRow = null;
        boolean first = true;
        List rows = result.getRows();
        
        log.logDetailed(toString(), "Found "+(rows!=null?rows.size():0)+" previous result rows");
        
        while( ( first && !execPerRow ) || ( execPerRow && rows!=null && iteration<rows.size() && result.getNrErrors()==0 ) )
        {
            first=false;
            if (rows!=null && execPerRow)
            {
            	resultRow = (RowMetaAndData) rows.get(iteration);
            }
            else
            {
            	resultRow = null;
            }
            
            List cmdRows = null;
            
            if (execPerRow) // Execute for each input row
            {
                if (argFromPrevious) // Copy the input row to the (command line) arguments
                {
                    if (resultRow!=null)
                    {
                        args = new String[resultRow.size()];
                        for (int i=0;i<resultRow.size();i++)
                        {
                            args[i] = resultRow.getString(i, null);
                        }
                    }
                }
                else
                {
                    // Just pass a single row
                    ArrayList newList = new ArrayList();
                    newList.add(resultRow);
                    cmdRows = newList;
                }
            }
            else
            {
                if (argFromPrevious)
                {
                    // Only put the first Row on the arguments
                    args = null;
                    if (resultRow!=null)
                    {
                        args = new String[resultRow.size()];
                        for (int i=0;i<resultRow.size();i++)
                        {
                            args[i] = resultRow.getString(i, null);
                        }
                    }
                    else
                    {
                    	cmdRows = rows;
                    }
                }
                else
                {
                    // Keep it as it was...
                    cmdRows = rows;
                }
            }

            executeShell(result, cmdRows, args);
            
            iteration++;
        }
        
        if (setLogfile)
        {
            if (appender!=null) 
            {
                log.removeAppender(appender);
                appender.close();
            }
            log.setLogLevel(backupLogLevel);
            
        }
		
		return result;		
	}
        
    private void executeShell(Result result, List cmdRows, String[] args)
    {
        LogWriter log = LogWriter.getInstance();
        
        try
        {
            // What's the exact command?
            String cmd[] = null;
            String base[] = null;
            
            
            log.logBasic(toString(), "Running on platform : "+Const.getOS());
            
			FileObject fileObject = null;

			String realFilename = StringUtil.environmentSubstitute(getFilename());
			fileObject = KettleVFS.getFileObject(realFilename);			
			
            if( Const.getOS().equals( "Windows 95" ) )
            {
                base = new String[] { "command.com", "/C" };
            }
            else
            if( Const.getOS().startsWith( "Windows" ) )
            {
                base = new String[] { "cmd.exe", "/C" };
            }
            else 
            {
                base = new String[] { KettleVFS.getFilename(fileObject) };
            }
    
            // Construct the arguments...
            if (argFromPrevious && cmdRows!=null)
            {
                ArrayList cmds = new ArrayList();
                
                // Add the base command...
                for (int i=0;i<base.length;i++) cmds.add(base[i]);
    
                if( Const.getOS().equals( "Windows 95" ) ||
                	Const.getOS().startsWith( "Windows" ) ) 
                {
                	// for windows all arguments including the command itself need to be
                	// included in 1 argument to cmd/command.

                	StringBuffer cmdline = new StringBuffer(300);
                	
                	cmdline.append('"');
                	cmdline.append(optionallyQuoteField(KettleVFS.getFilename(fileObject), "\""));
                	// Add the arguments from previous results...
                	for (int i=0;i<cmdRows.size();i++) // Normally just one row, but once in a while to remain compatible we have multiple. 
                	{
                        RowMetaAndData r = (RowMetaAndData)cmdRows.get(i);
                		for (int j=0;j<r.size();j++)
                		{
                			cmdline.append(' ');
                			cmdline.append(optionallyQuoteField(r.getString(j, null), "\""));                			
                		}
                	}
                	cmdline.append('"');
                	cmds.add(cmdline.toString());
                }
                else
                {
                	// Add the arguments from previous results...
                	for (int i=0;i<cmdRows.size();i++) // Normally just one row, but once in a while to remain compatible we have multiple. 
                	{
                        RowMetaAndData r = (RowMetaAndData)cmdRows.get(i);
                		for (int j=0;j<r.size();j++)
                		{
                			cmds.add(optionallyQuoteField(r.getString(j, null), "\""));
                		}
                	}
                }
                cmd = (String[]) cmds.toArray(new String[cmds.size()]);                
            }
            else if (args!=null)
            {            	
                ArrayList cmds = new ArrayList();
    
                // Add the base command...
                for (int i=0;i<base.length;i++) cmds.add(base[i]);
                
                if( Const.getOS().equals( "Windows 95" ) ||
                    	Const.getOS().startsWith( "Windows" ) ) 
                {
                	// for windows all arguments including the command itself need to be
                	// included in 1 argument to cmd/command.

                	StringBuffer cmdline = new StringBuffer(300);

                	cmdline.append('"');
                	cmdline.append(optionallyQuoteField(KettleVFS.getFilename(fileObject), "\""));

                    for (int i=0;i<args.length;i++) 
                    {
                    	cmdline.append(' ');
                        cmdline.append(optionallyQuoteField(args[i], "\""));
                    }
                	cmdline.append('"');
                	cmds.add(cmdline.toString());
                }
                else
                {    
                    for (int i=0;i<args.length;i++) 
                    {
                        cmds.add(args[i]);
                    }
                }
                cmd = (String[]) cmds.toArray(new String[cmds.size()]);
            }
            
            if (log.isBasic())
            {
                StringBuffer command = new StringBuffer();
                for (int i=0;i<cmd.length;i++)
                {
                    if (i>0) command.append(' ');
                    command.append(cmd[i]);
                }
                log.logBasic(toString(), "Executing command : "+command.toString());
            }
             
            // Launch the script!
            log.logDetailed(toString(), "Passing "+(cmd.length-1)+" arguments to command : ["+cmd[0]+"]");

            // Build the environment variable list...
            Runtime runtime = java.lang.Runtime.getRuntime();
            Process proc = runtime.exec(cmd,  
                    EnvUtil.getEnvironmentVariablesForRuntimeExec());
            
            // any error message?
            StreamLogger errorLogger = new
                StreamLogger(proc.getErrorStream(), toString()+" (stderr)");            
            
            // any output?
            StreamLogger outputLogger = new
                StreamLogger(proc.getInputStream(), toString()+" (stdout)");
                
            // kick them off
            new Thread(errorLogger).start();
            new Thread(outputLogger).start();
                                    
            proc.waitFor();
            log.logDetailed(toString(), "command ["+cmd[0]+"] has finished");
            
            // What's the exit status?
            result.setExitStatus( proc.exitValue() );
            if (result.getExitStatus()!=0) 
            {
                log.logDetailed(toString(), "Exit status of shell ["+StringUtil.environmentSubstitute(getFileName())+"] was "+result.getExitStatus());
                result.setNrErrors(1);
            } 
        }
        catch(IOException ioe)
        {
            log.logError(toString(), "Error running shell ["+StringUtil.environmentSubstitute(getFileName())+"] : "+ioe.toString());
            result.setNrErrors(1);
        }
        catch(InterruptedException ie)
        {
            log.logError(toString(), "Shell ["+StringUtil.environmentSubstitute(getFileName())+"] was interupted : "+ie.toString());
            result.setNrErrors(1);
        }
        catch(Exception e)
        {
            log.logError(toString(), "Unexpected error running shell ["+StringUtil.environmentSubstitute(getFileName())+"] : "+e.toString());
            result.setNrErrors(1);
        }
    
        if (result.getNrErrors() > 0)
        {
            result.setResult( false );
        }
        else
        {
            result.setResult( true );
        }
    }
    
	private String optionallyQuoteField(String field, String quote)
	{
        if (Const.isEmpty(field) ) return "\"\"";
        
        // If the field already contains quotes, we don't touch it anymore, just return the same string...
        // also return it if no spaces are found
        if (field.indexOf(quote)>=0 || field.indexOf(' ')<0 )
        {
            return field;
        }
        else
        {
        	return quote + field + quote;
        }
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
        return new JobEntryShellDialog(shell,this);
    }
}