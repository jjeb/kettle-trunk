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
 

/**
 *   Kettle was (re-)started in March 2003
 */

package org.pentaho.di.kitchen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleJobException;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.variables.LocalVariables;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.pan.CommandLineOption;
import org.pentaho.di.repository.RepositoriesMeta;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.version.BuildVersion;

public class Kitchen
{
	public static final String STRING_KITCHEN = "Kitchen";
	
	public static void main(String[] a) throws KettleException
	{
		EnvUtil.environmentInit();
        Thread parentThread = Thread.currentThread();
        LocalVariables.getInstance().createKettleVariables(parentThread.getName(), null, false);
		
	    List<String> args = new ArrayList<String>();
	    for (int i=0;i<a.length;i++)
	    {
	        if (a[i].length()>0) args.add(a[i]);
	    }

		RepositoryMeta repinfo  = null;
		UserInfo       userinfo = null;
		Job            job      = null;
		
		StringBuffer optionRepname, optionUsername, optionPassword, optionJobname, optionDirname, optionFilename, optionLoglevel;
        StringBuffer optionLogfile, optionLogfileOld, optionListdir, optionListjobs, optionListrep, optionNorep, optionVersion;

		CommandLineOption options[] = new CommandLineOption[] 
            {
			    new CommandLineOption("rep", "Repository name", optionRepname=new StringBuffer()),
			    new CommandLineOption("user", "Repository username", optionUsername=new StringBuffer()),
			    new CommandLineOption("pass", "Repository password", optionPassword=new StringBuffer()),
			    new CommandLineOption("job", "The name of the transformation to launch", optionJobname=new StringBuffer()),
			    new CommandLineOption("dir", "The directory (don't forget the leading /)", optionDirname=new StringBuffer()),
			    new CommandLineOption("file", "The filename (Job XML) to launch", optionFilename=new StringBuffer()),
			    new CommandLineOption("level", "The logging level (Basic, Detailed, Debug, Rowlevel, Error, Nothing)", optionLoglevel=new StringBuffer()),
			    new CommandLineOption("logfile", "The logging file to write to", optionLogfile=new StringBuffer()),
			    new CommandLineOption("log", "The logging file to write to (deprecated)", optionLogfileOld=new StringBuffer(), false, true),
			    new CommandLineOption("listdir", "List the directories in the repository", optionListdir=new StringBuffer(), true, false),
			    new CommandLineOption("listjobs", "List the jobs in the specified directory", optionListjobs=new StringBuffer(), true, false),
			    new CommandLineOption("listrep", "List the available repositories", optionListrep=new StringBuffer(), true, false),
		        new CommandLineOption("norep", "Do not log into the repository", optionNorep=new StringBuffer(), true, false),
                new CommandLineOption("version", "show the version, revision and build date", optionVersion=new StringBuffer(), true, false),
            };

		if (args.size()==0 ) 
		{
		    CommandLineOption.printUsage(options);
		    System.exit(9);
		}
        
        CommandLineOption.parseArguments(args, options);

        String kettleRepname  = Const.getEnvironmentVariable("KETTLE_REPOSITORY", null);
        String kettleUsername = Const.getEnvironmentVariable("KETTLE_USER", null);
        String kettlePassword = Const.getEnvironmentVariable("KETTLE_PASSWORD", null);
        
        if (!Const.isEmpty(kettleRepname )) optionRepname  = new StringBuffer(kettleRepname );
        if (!Const.isEmpty(kettleUsername)) optionUsername = new StringBuffer(kettleUsername);
        if (!Const.isEmpty(kettlePassword)) optionPassword = new StringBuffer(kettlePassword);
        
		// System.out.println("Level="+loglevel);
        LogWriter log;
        LogWriter.setConsoleAppenderDebug();
        
        if (Const.isEmpty(optionLogfile) && !Const.isEmpty(optionLogfileOld))
        {
           // if the old style of logging name is filled in, and the new one is not
           // overwrite the new by the old
           optionLogfile = optionLogfileOld;
        }
        
        if (Const.isEmpty(optionLogfile))
        {
            log=LogWriter.getInstance( LogWriter.LOG_LEVEL_BASIC );
        }
        else
        {
            log=LogWriter.getInstance( optionLogfile.toString(), true, LogWriter.LOG_LEVEL_BASIC );
        }
        
        if (!Const.isEmpty(optionLoglevel)) 
        {
            log.setLogLevel(optionLoglevel.toString());
            log.logMinimal(STRING_KITCHEN, "Logging is at level : "+log.getLogLevelDesc());
        } 
		
        if (!Const.isEmpty(optionVersion))
        {
            BuildVersion buildVersion = BuildVersion.getInstance();
            log.logBasic("Pan", "Kettle version "+Const.VERSION+", build "+buildVersion.getVersion()+", build date : "+buildVersion.getBuildDate());
            if (a.length==1) System.exit(6);
        }
        
        // Start the action...
        //
        if (!Const.isEmpty(optionRepname) && !Const.isEmpty(optionUsername)) log.logDetailed(STRING_KITCHEN, "Repository and username supplied");

		log.logMinimal(STRING_KITCHEN, "Start of run.");
		
		/* Load the plugins etc.*/
		StepLoader steploader = StepLoader.getInstance();
		if (!steploader.read())
		{
			log.logError(STRING_KITCHEN, "Error loading steps... halting Kitchen!");
			System.exit(8);
		}
        
        /* Load the plugins etc.*/
        JobEntryLoader jeloader = JobEntryLoader.getInstance();
        if (!jeloader.read())
        {
            log.logError(STRING_KITCHEN, "Error loading job entries & plugins... halting Kitchen!");
            return;
        }

		Date start, stop;
		Calendar cal;
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		cal=Calendar.getInstance();
		start=cal.getTime();
				
		log.logDebug(STRING_KITCHEN, "Allocate new job.");
		JobMeta jobMeta = new JobMeta(log);
        
        // In case we use a repository...
        Repository repository = null;

		try
		{
			// Read kettle job specified on command-line?
			if (!Const.isEmpty(optionRepname) || !Const.isEmpty(optionFilename))
			{
				log.logDebug(STRING_KITCHEN, "Parsing command line options.");
				if (optionRepname!=null && !"Y".equalsIgnoreCase(optionNorep.toString()))
				{
					log.logDebug(STRING_KITCHEN, "Loading available repositories.");
					RepositoriesMeta repsinfo = new RepositoriesMeta(log);
					if (repsinfo.readData())
					{
						log.logDebug(STRING_KITCHEN, "Finding repository ["+optionRepname+"]");
						repinfo = repsinfo.findRepository(optionRepname.toString());
						if (repinfo!=null)
						{
							// Define and connect to the repository...
							log.logDebug(STRING_KITCHEN, "Allocate & connect to repository.");
							repository = new Repository(log, repinfo, userinfo);
							if (repository.connect("Kitchen commandline"))
							{
								RepositoryDirectory directory = repository.getDirectoryTree(); // Default = root
								
								// Find the directory name if one is specified...
								if (!Const.isEmpty(optionDirname))
								{
									directory = repository.getDirectoryTree().findDirectory(optionDirname.toString());
								}
								
								if (directory!=null)
								{
									// Check username, password
									log.logDebug(STRING_KITCHEN, "Check supplied username and password.");
									userinfo = new UserInfo(repository, optionUsername.toString(), optionPassword.toString());
									if (userinfo.getID()>0)
									{
									    // Load a job
										if (!Const.isEmpty(optionJobname))
										{
											log.logDebug(STRING_KITCHEN, "Load the job info...");
											jobMeta =  new JobMeta(log, repository, optionJobname.toString(), directory);
											log.logDebug(STRING_KITCHEN, "Allocate job...");
											job = new Job(log, steploader, repository, jobMeta);
										}
										else
										// List the jobs in the repository
										if ("Y".equalsIgnoreCase(optionListjobs.toString()))
										{
										    log.logDebug(STRING_KITCHEN, "Getting list of jobs in directory: "+directory);
											String jobnames[] = repository.getJobNames(directory.getID());
											for (int i=0;i<jobnames.length;i++)
											{
												System.out.println(jobnames[i]);
											}
										}
										else
										// List the directories in the repository
										if ("Y".equalsIgnoreCase(optionListdir.toString()))
										{
											String dirnames[] = repository.getDirectoryNames(directory.getID());
											for (int i=0;i<dirnames.length;i++)
											{
												System.out.println(dirnames[i]);
											}
										}
									}
									else
									{
										System.out.println("ERROR: Can't verify username and password.");
										userinfo=null;
										repinfo=null;
									}
								}
								else
								{
									System.out.println("ERROR: Can't find the supplied directory ["+optionDirname+"]");
									userinfo=null;
									repinfo=null;
								}
							}
							else
							{
								System.out.println("ERROR: Can't connect to the repository.");
							}
						}
						else
						{
							System.out.println("ERROR: No repository provided, can't load job.");
						}
					}
					else
					{
						System.out.println("ERROR: No repositories defined on this system.");
					}
				}
				
                // Try to load if from file anyway.
				if (!Const.isEmpty(optionFilename) && job==null)
				{
					jobMeta = new JobMeta(log, optionFilename.toString(), null);
					job = new Job(log, steploader, null, jobMeta);
				}
			}
			else
			if ("Y".equalsIgnoreCase(optionListrep.toString()))
			{
				RepositoriesMeta ri = new RepositoriesMeta(log);
				if (ri.readData())
				{
					System.out.println("List of repositories:");
					for (int i=0;i<ri.nrRepositories();i++)
					{
						RepositoryMeta rinfo = ri.getRepository(i);
						System.out.println("#"+(i+1)+" : "+rinfo.getName()+" ["+rinfo.getDescription()+"] ");
					}
				}
				else
				{
					System.out.println("ERROR: Unable to read/parse the repositories XML file.");
				}
			}
		}
		catch(KettleException e)
		{
			job=null;
			jobMeta=null;
			System.out.println("Processing stopped because of an error: "+e.getMessage());
		}

		if (job==null)
		{
			if (!"Y".equalsIgnoreCase(optionListjobs.toString()) &&  
				!"Y".equalsIgnoreCase(optionListdir.toString()) && 
				!"Y".equalsIgnoreCase(optionListrep.toString()) 
			    )
			{
				System.out.println("ERROR: Kitchen can't continue because the job couldn't be loaded.");			    
			}

            System.exit(7);
		}
		
		Result result = null;

        int returnCode=0;
        
		try
		{
            // Add Kettle variables for the job thread...
            LocalVariables.getInstance().createKettleVariables(job.getName(), parentThread.getName(), true);
            
            // Set the arguments on the job metadata as well...
            if ( args.size() == 0 )
            {
                job.getJobMeta().setArguments(null);
            }
            else
            {
                job.getJobMeta().setArguments((String[]) args.toArray(new String[args.size()]));
            }
            
			result = job.execute(); // Execute the selected job.
			job.endProcessing("end", result);  // The bookkeeping...
		}
		catch(KettleJobException je)
		{
            if (result==null)
            {
                result = new Result();
            }
            result.setNrErrors(1L);
            
			try
			{
				job.endProcessing("error", result);
			}
			catch(KettleJobException je2)
			{
				log.logError(job.getName(), "A serious error occured : "+je2.getMessage());
                returnCode = 2;
			}
		}
        finally
        {
            if (repository!=null) repository.disconnect();
        }
        
		log.logMinimal(STRING_KITCHEN, "Finished!");
		
		if (result!=null && result.getNrErrors()!=0)
		{
			log.logError(STRING_KITCHEN, "Finished with errors");
            returnCode = 1;
		}
		cal=Calendar.getInstance();
		stop=cal.getTime();
		String begin=df.format(start).toString();
		String end  =df.format(stop).toString();

		log.logMinimal(STRING_KITCHEN, "Start="+begin+", Stop="+end);
		long millis=stop.getTime()-start.getTime();
		log.logMinimal(STRING_KITCHEN, "Processing ended after "+(millis/1000)+" seconds.");
        
        System.exit(returnCode);

	}
}
