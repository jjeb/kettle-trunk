 /* Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/
 
package org.pentaho.di.job.entries.movefiles;
import static org.pentaho.di.job.entry.validator.AbstractFileValidator.putVariableSpace;
import static org.pentaho.di.job.entry.validator.AndValidator.putValidators;
import static org.pentaho.di.job.entry.validator.JobEntryValidatorUtils.andValidator;
import static org.pentaho.di.job.entry.validator.JobEntryValidatorUtils.fileExistsValidator;
import static org.pentaho.di.job.entry.validator.JobEntryValidatorUtils.notNullValidator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;

import org.apache.commons.vfs.FileType;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryType;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.movefiles.Messages;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.job.entry.validator.ValidatorContext;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;



/**
 * This defines a 'move files' job entry.
 * 
 * @author Samatar Hassan
 * @since 25-02-2008
 */
public class JobEntryMoveFiles extends JobEntryBase implements Cloneable, JobEntryInterface
{
	public boolean move_empty_folders;
	public  boolean arg_from_previous;
	public  boolean overwrite_files;
	public  boolean include_subfolders;
	public boolean add_result_filesname;
	public boolean destination_is_a_file;
	public boolean create_destination_folder;
	public  String  source_filefolder[];
	public  String  destination_filefolder[];
	public  String  wildcard[];
	public boolean IgnoreRestOfFiles;
	private String nr_errors_less_than;
	private String success_condition;
	private boolean add_date;
	private boolean add_time;
	private boolean SpecifyFormat;
	private String date_time_format;
	private boolean AddDateBeforeExtension;
	boolean DoNotKeepFolderStructure;
	
	boolean DoNotProcessRest=false;
	int NrErrors=0;
	
	public JobEntryMoveFiles(String n)
	{
		super(n, "");
		DoNotKeepFolderStructure=false;
		move_empty_folders=true;
		arg_from_previous=false;
		source_filefolder=null;
		destination_filefolder=null;
		wildcard=null;
		overwrite_files=false;
		include_subfolders=false;
		add_result_filesname=false;
		destination_is_a_file=false;
		create_destination_folder=false;
		IgnoreRestOfFiles=false;
		nr_errors_less_than="10";
		success_condition="success_when_all_works_fine";
		add_date=false;
		add_time=false;
		SpecifyFormat=false;
		date_time_format=null;
		AddDateBeforeExtension=false;
		setID(-1L);
		setJobEntryType(JobEntryType.MOVE_FILES);
	}

	public JobEntryMoveFiles()
	{
		this("");
	}

	public JobEntryMoveFiles(JobEntryBase jeb)
	{
		super(jeb);
	}

	public Object clone()
	{
		JobEntryMoveFiles je = (JobEntryMoveFiles) super.clone();
		return je;
	}
    
	public String getXML()
	{
		StringBuffer retval = new StringBuffer(300);
		
		retval.append(super.getXML());		
		retval.append("      ").append(XMLHandler.addTagValue("move_empty_folders",      move_empty_folders));
		retval.append("      ").append(XMLHandler.addTagValue("arg_from_previous",  arg_from_previous));
		retval.append("      ").append(XMLHandler.addTagValue("overwrite_files",      overwrite_files));
		retval.append("      ").append(XMLHandler.addTagValue("include_subfolders", include_subfolders));
		retval.append("      ").append(XMLHandler.addTagValue("add_result_filesname", add_result_filesname));
		retval.append("      ").append(XMLHandler.addTagValue("destination_is_a_file", destination_is_a_file));
		retval.append("      ").append(XMLHandler.addTagValue("create_destination_folder", create_destination_folder));
		retval.append("      ").append(XMLHandler.addTagValue("IgnoreRestOfFiles", IgnoreRestOfFiles));
		retval.append("      ").append(XMLHandler.addTagValue("add_date", add_date));
		retval.append("      ").append(XMLHandler.addTagValue("add_time", add_time));
		retval.append("      ").append(XMLHandler.addTagValue("SpecifyFormat", SpecifyFormat));
		retval.append("      ").append(XMLHandler.addTagValue("date_time_format", date_time_format));
		retval.append("      ").append(XMLHandler.addTagValue("nr_errors_less_than", nr_errors_less_than));
		retval.append("      ").append(XMLHandler.addTagValue("success_condition", success_condition));
		retval.append("      ").append(XMLHandler.addTagValue("AddDateBeforeExtension", AddDateBeforeExtension));
		retval.append("      ").append(XMLHandler.addTagValue("DoNotKeepFolderStructure", DoNotKeepFolderStructure));
		
		
		retval.append("      <fields>").append(Const.CR);
		if (source_filefolder!=null)
		{
			for (int i=0;i<source_filefolder.length;i++)
			{
				retval.append("        <field>").append(Const.CR);
				retval.append("          ").append(XMLHandler.addTagValue("source_filefolder",     source_filefolder[i]));
				retval.append("          ").append(XMLHandler.addTagValue("destination_filefolder",     destination_filefolder[i]));
				retval.append("          ").append(XMLHandler.addTagValue("wildcard", wildcard[i]));
				retval.append("        </field>").append(Const.CR);
			}
		}
		retval.append("      </fields>").append(Const.CR);
		
		return retval.toString();
	}
	
	
	 public void loadXML(Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep) throws KettleXMLException
	  {
		try
		{
			super.loadXML(entrynode, databases, slaveServers);
			move_empty_folders      = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "move_empty_folders"));
			arg_from_previous   = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "arg_from_previous") );
			overwrite_files      = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "overwrite_files") );
			include_subfolders = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "include_subfolders") );
			add_result_filesname = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "add_result_filesname") );
			destination_is_a_file = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "destination_is_a_file") );
			create_destination_folder = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "create_destination_folder") );
			IgnoreRestOfFiles = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "IgnoreRestOfFiles") );
			add_date = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "add_date"));
			add_time = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "add_time"));
			SpecifyFormat = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "SpecifyFormat"));
			AddDateBeforeExtension = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "AddDateBeforeExtension"));
			DoNotKeepFolderStructure = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "DoNotKeepFolderStructure"));

			date_time_format          = XMLHandler.getTagValue(entrynode, "date_time_format");
			
			nr_errors_less_than          = XMLHandler.getTagValue(entrynode, "nr_errors_less_than");
			success_condition          = XMLHandler.getTagValue(entrynode, "success_condition");
			
			
			Node fields = XMLHandler.getSubNode(entrynode, "fields");
			
			// How many field arguments?
			int nrFields = XMLHandler.countNodes(fields, "field");	
			source_filefolder = new String[nrFields];
			destination_filefolder = new String[nrFields];
			wildcard = new String[nrFields];
			
			// Read them all...
			for (int i = 0; i < nrFields; i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);
				
				source_filefolder[i] = XMLHandler.getTagValue(fnode, "source_filefolder");
				destination_filefolder[i] = XMLHandler.getTagValue(fnode, "destination_filefolder");
				wildcard[i] = XMLHandler.getTagValue(fnode, "wildcard");
			}
		}
	
		catch(KettleXMLException xe)
		{
			
			throw new KettleXMLException(Messages.getString("JobMoveFiles.Error.Exception.UnableLoadXML"), xe);
		}
	}

	 public void loadRep(Repository rep, long id_jobentry, List<DatabaseMeta> databases, List<SlaveServer> slaveServers) throws KettleException
	  {
		try
		{
			super.loadRep(rep, id_jobentry, databases, slaveServers);
			move_empty_folders      = rep.getJobEntryAttributeBoolean(id_jobentry, "move_empty_folders");
			arg_from_previous   = rep.getJobEntryAttributeBoolean(id_jobentry, "arg_from_previous");
			overwrite_files      = rep.getJobEntryAttributeBoolean(id_jobentry, "overwrite_files");
			include_subfolders = rep.getJobEntryAttributeBoolean(id_jobentry, "include_subfolders");

			add_result_filesname = rep.getJobEntryAttributeBoolean(id_jobentry, "add_result_filesname");
			destination_is_a_file = rep.getJobEntryAttributeBoolean(id_jobentry, "destination_is_a_file");
			create_destination_folder = rep.getJobEntryAttributeBoolean(id_jobentry, "create_destination_folder");
			IgnoreRestOfFiles = rep.getJobEntryAttributeBoolean(id_jobentry, "IgnoreRestOfFiles");
			
			nr_errors_less_than  = rep.getJobEntryAttributeString(id_jobentry, "nr_errors_less_than");
			success_condition  = rep.getJobEntryAttributeString(id_jobentry, "success_condition");
			add_date = rep.getJobEntryAttributeBoolean(id_jobentry, "add_date"); 
			add_time = rep.getJobEntryAttributeBoolean(id_jobentry, "add_time"); 
			SpecifyFormat = rep.getJobEntryAttributeBoolean(id_jobentry, "SpecifyFormat"); 
			date_time_format  = rep.getJobEntryAttributeString(id_jobentry, "date_time_format");
			AddDateBeforeExtension = rep.getJobEntryAttributeBoolean(id_jobentry, "AddDateBeforeExtension");
			DoNotKeepFolderStructure = rep.getJobEntryAttributeBoolean(id_jobentry, "DoNotKeepFolderStructure");
			
			
			// How many arguments?
			int argnr = rep.countNrJobEntryAttributes(id_jobentry, "source_filefolder");
			source_filefolder = new String[argnr];
			destination_filefolder = new String[argnr];
			wildcard = new String[argnr];
			
			// Read them all...
			for (int a=0;a<argnr;a++) 
			{
				source_filefolder[a]= rep.getJobEntryAttributeString(id_jobentry, a, "source_filefolder");
				destination_filefolder[a]= rep.getJobEntryAttributeString(id_jobentry, a, "destination_filefolder");
				wildcard[a]= rep.getJobEntryAttributeString(id_jobentry, a, "wildcard");
			}
		}
		catch(KettleException dbe)
		{
			
			throw new KettleException(Messages.getString("JobMoveFiles.Error.Exception.UnableLoadRep")+id_jobentry, dbe);
		}
	}
	
	public void saveRep(Repository rep, long id_job)
		throws KettleException
	{
		try
		{
			super.saveRep(rep, id_job);
			
			rep.saveJobEntryAttribute(id_job, getID(), "move_empty_folders",      move_empty_folders);
			rep.saveJobEntryAttribute(id_job, getID(), "arg_from_previous",  arg_from_previous);
			rep.saveJobEntryAttribute(id_job, getID(), "overwrite_files",      overwrite_files);
			rep.saveJobEntryAttribute(id_job, getID(), "include_subfolders", include_subfolders);
			rep.saveJobEntryAttribute(id_job, getID(), "destination_is_a_file", destination_is_a_file);
			rep.saveJobEntryAttribute(id_job, getID(), "create_destination_folder", create_destination_folder);
			
			rep.saveJobEntryAttribute(id_job, getID(), "IgnoreRestOfFiles", IgnoreRestOfFiles);
			
			
			rep.saveJobEntryAttribute(id_job, getID(), "nr_errors_less_than",      nr_errors_less_than);
			rep.saveJobEntryAttribute(id_job, getID(), "success_condition",      success_condition);
			rep.saveJobEntryAttribute(id_job, getID(), "add_date", add_date);
			rep.saveJobEntryAttribute(id_job, getID(), "add_time", add_time);
			rep.saveJobEntryAttribute(id_job, getID(), "SpecifyFormat", SpecifyFormat);
			rep.saveJobEntryAttribute(id_job, getID(), "date_time_format",      date_time_format);
			rep.saveJobEntryAttribute(id_job, getID(), "AddDateBeforeExtension", AddDateBeforeExtension);
			rep.saveJobEntryAttribute(id_job, getID(), "DoNotKeepFolderStructure", DoNotKeepFolderStructure);
			
			
			
			// save the arguments...
			if (source_filefolder!=null)
			{
				for (int i=0;i<source_filefolder.length;i++) 
				{
					rep.saveJobEntryAttribute(id_job, getID(), i, "source_filefolder",     source_filefolder[i]);
					rep.saveJobEntryAttribute(id_job, getID(), i, "destination_filefolder",     destination_filefolder[i]);
					rep.saveJobEntryAttribute(id_job, getID(), i, "wildcard", wildcard[i]);
				}
			}
		}
		catch(KettleDatabaseException dbe)
		{
			
			throw new KettleException(Messages.getString("JobMoveFiles.Error.Exception.UnableSaveRep")+id_job, dbe);
		}
	}

	public Result execute(Result previousResult, int nr, Repository rep, Job parentJob) throws KettleException 
	{
		LogWriter log = LogWriter.getInstance();
		Result result = previousResult;

	    List<RowMetaAndData> rows = result.getRows();
	    RowMetaAndData resultRow = null;
		
	    NrErrors=0;
		DoNotProcessRest=false;
		
		// Get source and destination files, also wildcard
		String vsourcefilefolder[] = source_filefolder;
		String vdestinationfilefolder[] = destination_filefolder;
		String vwildcard[] = wildcard;
		
		result.setResult( true );
		
			
		if (arg_from_previous)
		{
			if (log.isDetailed())
				log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.ArgFromPrevious.Found",(rows!=null?rows.size():0)+ ""));
			
		}
		if (arg_from_previous && rows!=null) // Copy the input row to the (command line) arguments
		{
			for (int iteration=0;iteration<rows.size();iteration++) 
			{
			
				resultRow = rows.get(iteration);
			
				// Get source and destination file names, also wildcard
				String vsourcefilefolder_previous = resultRow.getString(0,null);
				String vdestinationfilefolder_previous = resultRow.getString(1,null);
				String vwildcard_previous = resultRow.getString(2,null);

				if(!Const.isEmpty(vsourcefilefolder_previous) &&  !Const.isEmpty(vdestinationfilefolder_previous))
				{
					if(!DoNotProcessRest)
	           		{
						if(log.isDetailed())
							log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.ProcessingRow",vsourcefilefolder_previous, vdestinationfilefolder_previous, vwildcard_previous));

						if(! ProcessFileFolder(vsourcefilefolder_previous,vdestinationfilefolder_previous,vwildcard_previous,parentJob,result))
						{
							// The move process fail
							// Update Errors
							updateErrors();
						}
	           		}else
	           		{
	           			if(log.isDetailed()) 
	           				log.logDetailed(toString(),Messages.getString("JobEntryMoveFiles.log.IgnoringFile",vsourcefilefolder_previous));
	           	
	           		}

				}
				else
				{
				 
					if(log.isDetailed())
						log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.IgnoringRow",vsourcefilefolder[iteration],vdestinationfilefolder[iteration],vwildcard[iteration]));
			
				}
			}
		}
		else if (vsourcefilefolder!=null && vdestinationfilefolder!=null)
		{
			for (int i=0;i<vsourcefilefolder.length;i++)
			{
				if(!Const.isEmpty(vsourcefilefolder[i]) && !Const.isEmpty(vdestinationfilefolder[i]))
				{

					// ok we can process this file/folder
					if(!DoNotProcessRest)
	           		{
						if(log.isDetailed())
							log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.ProcessingRow",vsourcefilefolder[i],vdestinationfilefolder[i],vwildcard[i]));
					
						if(!ProcessFileFolder(vsourcefilefolder[i],vdestinationfilefolder[i],vwildcard[i],parentJob,result))
						{
							// Update Errors
							updateErrors();
						}
	           		}else
	           		{
	           			if(log.isDetailed()) log.logDetailed(toString(),Messages.getString("JobEntryMoveFiles.log.IgnoringFile",vsourcefilefolder[i]));
	           	
	           		}
				}
				else
				{
							
					if(log.isDetailed())
						log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.IgnoringRow",vsourcefilefolder[i],vdestinationfilefolder[i],vwildcard[i]));
				
				}
			}
		}	
		
		// Success Condition
		if (getStatus())
		{
			result.setResult( false );
			result.setNrErrors(NrErrors);	
		}else
			result.setResult(true);
		

		
		return result;
	}
	private boolean getStatus()
	{
		boolean retval=false;
		int limitErrors=Const.toInt(environmentSubstitute(getNrErrorsLessThan()),10);
		if ((NrErrors>0 && getSuccessCondition().equals("success_when_all_works_fine"))
				|| (NrErrors>=limitErrors && !getSuccessCondition().equals("success_when_all_works_fine")))
				//|| (NrErrors>0 &&  limitErrors==0))
			{
				retval=true;	
			}
		
		return retval;
	}
	
	private boolean ProcessFileFolder(String sourcefilefoldername,String destinationfilefoldername,String wildcard,Job parentJob,Result result)
	{
		LogWriter log = LogWriter.getInstance();
		boolean entrystatus = false ;
		FileObject sourcefilefolder = null;
		FileObject destinationfilefolder = null;
		FileObject Currentfile =null;
		
		// Get real source, destination file and wilcard
		String realSourceFilefoldername = environmentSubstitute(sourcefilefoldername);
		String realDestinationFilefoldername = environmentSubstitute(destinationfilefoldername);
		String realWildcard=environmentSubstitute(wildcard);

		try
		{
			
		     // Here gc() is explicitly called if e.g. createfile is used in the same
		     // job for the same file. The problem is that after creating the file the
		     // file object is not properly garbaged collected and thus the file cannot
		     // be deleted anymore. This is a known problem in the JVM.

		     System.gc();
		      
			sourcefilefolder = KettleVFS.getFileObject(realSourceFilefoldername);
			destinationfilefolder = KettleVFS.getFileObject(realDestinationFilefoldername);
			
			if (sourcefilefolder.exists())
			{
			
				// Check if destination folder/parent folder exists !
				// If user wanted and if destination folder does not exist
				// PDI will create it
				if(CreateDestinationFolder(destinationfilefolder))
				{

					// Basic Tests
					if (sourcefilefolder.getType().equals(FileType.FOLDER) && destination_is_a_file)
					{
						// Source is a folder, destination is a file
						// WARNING !!! CAN NOT MOVE FOLDER TO FILE !!!
						
						log.logError(Messages.getString("JobMoveFiles.Log.Forbidden"), Messages.getString("JobMoveFiles.Log.CanNotMoveFolderToFile",realSourceFilefoldername,realDestinationFilefoldername));	
						
						// Update Errors
						updateErrors();
						
					}
					else
					{

						if (destinationfilefolder.getType().equals(FileType.FOLDER) && sourcefilefolder.getType().equals(FileType.FILE) )
						{				
							// Source is a file, destination is a folder
							// return destination short filename
							String shortfilename=sourcefilefolder.getName().getBaseName();
							try{
							 shortfilename=getDestinationFilename(sourcefilefolder.getName().getBaseName());
							}catch (Exception e)
							{
								log.logError(toString(), Messages.getString(Messages.getString("JobMoveFiles.Error.GettingFilename",sourcefilefolder.getName().getBaseName(),e.toString())));
								return entrystatus;
							}
							
							
							// Move the file to the destination folder				
							
							String destinationfilenamefull=destinationfilefolder.toString()+Const.FILE_SEPARATOR+shortfilename;
							FileObject destinationfile= KettleVFS.getFileObject(destinationfilenamefull);
							
							sourcefilefolder.moveTo(destinationfile);
							
							if(log.isDetailed()) log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileCopied",sourcefilefolder.getName().toString(),destinationfile.getName().toString()));
							// add filename to result filename
							if(add_result_filesname) addFileToResultFilenames(destinationfile.toString(),log,result,parentJob);
						}
						else if (sourcefilefolder.getType().equals(FileType.FILE) && destination_is_a_file)
						{
							// Source is a file, destination is a file
							
							
							FileObject destinationfile= KettleVFS.getFileObject(realDestinationFilefoldername);
							
							// return destination short filename
							String shortfilename=destinationfile.getName().getBaseName();
							try{
							 shortfilename=getDestinationFilename(destinationfile.getName().getBaseName());
							}catch (Exception e)
							{
								log.logError(toString(), Messages.getString(Messages.getString("JobMoveFiles.Error.GettingFilename",sourcefilefolder.getName().getBaseName(),e.toString())));
								return entrystatus;
							}

							String destinationfilenamefull=destinationfilefolder.getParent().toString()+Const.FILE_SEPARATOR+shortfilename;
							destinationfile= KettleVFS.getFileObject(destinationfilenamefull);
							
							sourcefilefolder.moveTo(destinationfile);
							if(log.isDetailed()) log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileCopied",sourcefilefolder.getName().toString(),destinationfile.getName().toString()));
							// add filename to result filename
							if(add_result_filesname) addFileToResultFilenames(destinationfile.toString(),log,result,parentJob);
						}
						else
						{
							// Both source and destination are folders
							if(log.isDetailed())
							{
								log.logDetailed(toString(),"  ");
								log.logDetailed(toString(),Messages.getString("JobMoveFiles.Log.FetchFolder",sourcefilefolder.toString()));
							}
							
							FileObject[] fileObjects = sourcefilefolder.findFiles(
	                                new AllFileSelector() 
	                                {	
	                                    public boolean traverseDescendents(FileSelectInfo info)
	                                    {
	                                        return true;
	                                    }
	                                    
	                                    public boolean includeFile(FileSelectInfo info)
	                                    {
	                                    
	                                    	FileObject fileObject = info.getFile();
	                                    	try {
	                                    	    if ( fileObject == null) return false;
	                                    	}
	                                    	catch (Exception ex)
	                                    	{
	                                    		// Upon error don't process the file.
	                                    		return false;
	                                    	}
	                                    	
	                                    	finally 
	                                		{
	                                			if ( fileObject != null )
	                                			{
	                                				try  {fileObject.close();} catch ( IOException ex ) {};
	                                			}
	           
	                                		}
	                                    	return true;
	                                    }
	                                }
	                            );
							
							if (fileObjects != null) 
	                        {
	                            for (int j = 0; j < fileObjects.length; j++)
	                            {
	                            	// Fetch files list one after one ...
	                                Currentfile=fileObjects[j];
	                                if(!DoNotProcessRest)
	    			           		{
		                                if(!MoveOneFile(Currentfile, sourcefilefolder,realDestinationFilefoldername, 
		                						realWildcard,log,parentJob,result))
		                                {
		                                	// Update Errors
		        							updateErrors();
		                                }
	    			           		}else
	    			           		{
	    			           			if(log.isDetailed()) log.logDetailed(toString(),Messages.getString("JobEntryMoveFiles.log.IgnoringFile",Currentfile.toString()));
	    			           	
	    			           		}
	                            }
	                        }
						}
						
					}
					entrystatus = true ;
				} // end if	
				else
				{
					// Destination Folder or Parent folder is missing
					log.logError(toString(), Messages.getString("JobMoveFiles.Error.DestinationFolderNotFound",realDestinationFilefoldername));					
					
					// Update Errors
					updateErrors();
				}	
			} // end if
			else
			{	
				log.logError(toString(), Messages.getString("JobMoveFiles.Error.SourceFileNotExists",realSourceFilefoldername));					
				
				// Update Errors
				updateErrors();
			}
		} // end try
	
		catch (IOException e) 
		{

			log.logError(toString(), Messages.getString("JobMoveFiles.Error.Exception.MoveProcess",realSourceFilefoldername.toString(),destinationfilefolder.toString(), e.getMessage()));					
			// Update Errors
			updateErrors();
		}
		finally 
		{
			if ( sourcefilefolder != null )
			{
				try  
				{
					sourcefilefolder.close();
					
				}
				catch ( IOException ex ) {};
			}
			if ( destinationfilefolder != null )
			{
				try  
				{
					destinationfilefolder.close();
					
				}
				catch ( IOException ex ) {};
			}
			if ( Currentfile != null )
			{
				try  
				{
					Currentfile.close();
					
				}
				catch ( IOException ex ) {};
			}
		}

		return entrystatus;
	}
	private boolean MoveOneFile(FileObject Currentfile, FileObject sourcefilefolder,String realDestinationFilefoldername, 
						String realWildcard,LogWriter log,Job parentJob,Result result)
	{
		boolean entrystatus=false;
		FileObject file_name=null;
	try {
     	if (!Currentfile.toString().equals(sourcefilefolder.toString()))
		{
			// Pass over the Base folder itself
			
    		// return destination short filename
    		String shortfilename=Currentfile.getName().getBaseName();
			try{
			  shortfilename=getDestinationFilename(Currentfile.getName().getBaseName());
			}catch (Exception e)
			{
				log.logError(toString(), Messages.getString(Messages.getString("JobMoveFiles.Error.GettingFilename",Currentfile.getName().getBaseName(),e.toString())));
				return entrystatus;
			}
			//log.logBasic("-----Current-------", Currentfile.getName().getBaseName());
			int lenCurrent=Currentfile.getName().getBaseName().length();
			//log.logBasic("-----short_filename-------", shortfilename);
			String short_filename_from_basefolder=shortfilename;
			if(!isDoNotKeepFolderStructure())	
				short_filename_from_basefolder=Currentfile.toString().substring(sourcefilefolder.toString().length(),Currentfile.toString().length());        					
			//log.logBasic("-----short_filename_from_basefolder-------", short_filename_from_basefolder);
			short_filename_from_basefolder=short_filename_from_basefolder.substring(0,short_filename_from_basefolder.length()-lenCurrent)+shortfilename;
			//log.logBasic("-----short_filename_from_basefolder-------", short_filename_from_basefolder);
			// Built destination filename
			file_name=KettleVFS.getFileObject(realDestinationFilefoldername + Const.FILE_SEPARATOR + short_filename_from_basefolder); 
			
			if (!Currentfile.getParent().toString().equals(sourcefilefolder.toString()))
			 {
				// Not in the Base Folder..Only if include sub folders  
				 if (include_subfolders)
				 {
					// Folders..only if include subfolders
					 if (Currentfile.getType() == FileType.FOLDER)
					 {
						 if (include_subfolders && move_empty_folders && Const.isEmpty(wildcard))
						 {
							 if (!file_name.exists())
							 {
								// Move Folder
								Currentfile.moveTo(file_name);
								 
								 if(log.isDetailed())
								 {	                											 
									 log.logDetailed(toString()," ------ ");
									 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FolderMoved",Currentfile.getName().toString(),file_name.getName().toString()));

								 }
								 
								 // add filename to result filename
								 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
							
								
							 }
							 else
							 {
								 if(log.isDetailed()) 
								 {
									 log.logDetailed(toString()," ------ ");
									 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FolderExists", file_name.toString()));
								 }
								
								 if (overwrite_files)
								 {
									 // Move File
									 Currentfile.moveTo(file_name);
									
									 if(log.isDetailed()) log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileOverwrite",file_name.getName().toString()));
										
									 // add filename to result filename
									 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
								 
									 
								 }
							 } 
						 }
						 
					 }
					 else
					 {
						if (GetFileWildcard(shortfilename,realWildcard))
						{	
							// Check if the file exists
							 if (!file_name.exists())
							 {
								 // Move File
								 Currentfile.moveTo(file_name);
								 
								if(log.isDetailed()) 
								{
									log.logDetailed(toString()," ------ ");
									log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileMoved",Currentfile.toString(),file_name.toString()));
									
								}
								// add filename to result filename
								if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
								
							
							 }
							 else
							 {
								
								 if(log.isDetailed()) 
								 {
									 log.logDetailed(toString()," ------ ");
									 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileExists",file_name.getName().toString()));
								 }
								
								 if (overwrite_files)
								 {
									 // Move File
									 Currentfile.moveTo(file_name);
									 
									 if(log.isDetailed()) log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileOverwrite",file_name.toString()));
										
									 
									 // add filename to result filename
									 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
								 

								 }
							 }
						}
					 }
				 }
			 }
			 else
			 {
				// In the Base Folder...
				// Folders..only if include subfolders
				 if (Currentfile.getType() == FileType.FOLDER)
				 {
					 if (include_subfolders && move_empty_folders  && Const.isEmpty(wildcard))
					 {
						 if (!file_name.exists())
						 {
							 // Move File
							 Currentfile.moveTo(file_name);
							 
							 if(log.isDetailed())  
							{
								 log.logDetailed(toString()," ------ ");
								 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileMoved",Currentfile.toString(),file_name.toString()));
								
							}
							 // add filename to result filename
							 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
						 
						 }
						 else
						 {
							 if(log.isDetailed())  
							 {
								 log.logDetailed(toString()," ------ ");
								 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileExists",file_name.getName().toString()));
							 }
							 
							 if (overwrite_files)
							 {
								 // Move File
								 Currentfile.moveTo(file_name);
								 
								 if(log.isDetailed())  log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileOverwrite",file_name.toString()));
									
								 
								 // add filename to result filename
								 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
							  

							 }
						 }

					 }
				 }
				 else
				 {
					 // file...Check if exists
					 if (GetFileWildcard(shortfilename,realWildcard))
					 {	
						 if (!file_name.exists())
						 {
							 // Move File
							 Currentfile.moveTo(file_name);
								 
							 if(log.isDetailed())  
							 {
								 log.logDetailed(toString()," ------ ");
								 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileMoved",Currentfile.toString(),file_name.toString()));
								 
								
							 }	
							 // add filename to result filename
							 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
						 
						 }
						 else
						 {
								
							 if(log.isDetailed())  
							 {
								 log.logDetailed(toString()," ------ ");
								 log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileExists",file_name.getName().toString()));
							 }
							 

						
							 if (overwrite_files)
							 {
								 // Move File
								 Currentfile.moveTo(file_name);
								 
								 if(log.isDetailed()) log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FileOverwrite",file_name.toString()));
								 
								 // add filename to result filename
								 if(add_result_filesname) addFileToResultFilenames(file_name.toString(),log,result,parentJob);
							 
							 } 
							 
						 }
					 }
				 }

			 }
			
		}
     	entrystatus=true;
    
	}catch (Exception e)
	{
		log.logError(toString(), Messages.getString("JobMoveFiles.Log.Error",e.toString()));
	}
	finally 
	{
		if ( file_name != null )
		{
			try  
			{
				file_name.close();
				
			}
			catch ( IOException ex ) {};
		}
	
	}
 return entrystatus;
 }

	private void updateErrors()
	{
		NrErrors++;
		if(IgnoreRestOfFiles) 
		{
			if(getStatus()) DoNotProcessRest=true;
		}
	}
	private void addFileToResultFilenames(String fileaddentry,LogWriter log,Result result,Job parentJob)
	{	
		try
		{
			ResultFile resultFile = new ResultFile(ResultFile.FILE_TYPE_GENERAL, KettleVFS.getFileObject(fileaddentry), parentJob.getName(), toString());
			result.getResultFiles().put(resultFile.getFile().toString(), resultFile);
	    
			if(log.isDetailed())
			{
				log.logDetailed(toString()," ------ ");
				log.logDetailed(toString(),Messages.getString("JobMoveFiles.Log.FileAddedToResultFilesName",fileaddentry));
			}
			
		}catch (Exception e)
		{
			log.logError(toString(),Messages.getString("JobMoveFiles.Error.AddingToFilenameResult"),fileaddentry,e.getMessage());
		}

}
	private boolean CreateDestinationFolder(FileObject filefolder)
	{
		LogWriter log = LogWriter.getInstance();
		FileObject folder=null;
		try
		{
			if(destination_is_a_file)
				folder=filefolder.getParent();
			else
				folder=filefolder;
			
    		if(!folder.exists())	
    		{
    			if(log.isDetailed())
    				log.logDetailed(toString(),Messages.getString("JobMoveFiles.Log.FolderNotExist",folder.getName().toString()));
    			if(create_destination_folder)
    				folder.createFolder();
    			else
    				return false;
    			if(log.isDetailed())
    				log.logDetailed(toString(), Messages.getString("JobMoveFiles.Log.FolderWasCreated", folder.getName().toString()));
    		}
    		return true;
		}
		catch (Exception e) {
			log.logError(toString(),Messages.getString("JobMoveFiles.Log.CanNotCreateParentFolder",folder.getName().toString()));
			
		}
		 finally {
         	if ( folder != null )
         	{
         		try  {
         			folder.close();
         		}
         		catch (Exception ex ) {};
         	}
         }
		 return false;
	}
	
	
	
	/**********************************************************
	 * 
	 * @param selectedfile
	 * @param wildcard
	 * @return True if the selectedfile matches the wildcard
	 **********************************************************/
	private boolean GetFileWildcard(String selectedfile, String wildcard)
	{
		Pattern pattern = null;
		boolean getIt=true;
	
        if (!Const.isEmpty(wildcard))
        {
        	 pattern = Pattern.compile(wildcard);
			// First see if the file matches the regular expression!
			if (pattern!=null)
			{
				Matcher matcher = pattern.matcher(selectedfile);
				getIt = matcher.matches();
			}
        }
		
		return getIt;
	}
	private String getDestinationFilename(String shortsourcefilename) throws Exception
	{
		String shortfilename=shortsourcefilename;
		int lenstring=shortsourcefilename.length();
		int lastindexOfDot=shortfilename.lastIndexOf('.');
		if(lastindexOfDot==-1) lastindexOfDot=lenstring;
		
		if(isAddDateBeforeExtension())
			shortfilename=shortfilename.substring(0, lastindexOfDot);
		
			
		SimpleDateFormat daf  = new SimpleDateFormat();
		Date now = new Date();
	
		if(isSpecifyFormat() && !Const.isEmpty(getDateTimeFormat()))
		{
			daf.applyPattern(getDateTimeFormat());
			String dt = daf.format(now);
			shortfilename+=dt;
		}else
		{
			if (isAddDate())
			{
				daf.applyPattern("yyyyMMdd");
				String d = daf.format(now);
				shortfilename+="_"+d;
			}
			if (isAddTime())
			{
				daf.applyPattern("HHmmssSSS");
				String t = daf.format(now);
				shortfilename+="_"+t;
			}
		}
		if(isAddDateBeforeExtension())
			shortfilename+=shortsourcefilename.substring(lastindexOfDot, lenstring);
		
		
		return shortfilename;
	}
   public boolean isIgnoreRestOfFiles()
    {
    	return IgnoreRestOfFiles;
    }
    
   public void setAddDate(boolean adddate)
   {
   	this.add_date=adddate;
   }
   
   public boolean  isAddDate()
   {
   	return add_date;
   }
   
   
   public void setAddTime(boolean addtime)
   {
   	this.add_time=addtime;
   }
   
   public boolean  isAddTime()
   {
   	return add_time;
   }
  
   public void setAddDateBeforeExtension(boolean AddDateBeforeExtension)
   {
   	this.AddDateBeforeExtension=AddDateBeforeExtension;
   }
   public boolean  isSpecifyFormat()
   {
   	return SpecifyFormat;
   }
   public void setSpecifyFormat(boolean SpecifyFormat)
   {
   	this.SpecifyFormat=SpecifyFormat;
   }
   public String getDateTimeFormat()
	{
		return date_time_format;
	}
	public void setDateTimeFormat(String date_time_format)
	{
		this.date_time_format=date_time_format;
	}
   
   public boolean  isAddDateBeforeExtension()
   {
   	return AddDateBeforeExtension;
   }
   public boolean  isDoNotKeepFolderStructure()
   {
   	return DoNotKeepFolderStructure;
   }
   public void setDoNotKeepFolderStructure(boolean DoNotKeepFolderStructure)
	{
		this.DoNotKeepFolderStructure=DoNotKeepFolderStructure;
	}
   
    
    public void setIgnoreRestOfFiles(boolean IgnoreRestOfFiles)
	{
		this.IgnoreRestOfFiles=IgnoreRestOfFiles;
	}
    
    
    
    
	public void setMoveEmptyFolders(boolean move_empty_foldersin) 
	{
		this.move_empty_folders = move_empty_foldersin;
	}
	
	public void setoverwrite_files(boolean overwrite_filesin) 
	{
		this.overwrite_files = overwrite_filesin;
	}

	public void setIncludeSubfolders(boolean include_subfoldersin) 
	{
		this.include_subfolders = include_subfoldersin;
	}
	
	public void setAddresultfilesname(boolean add_result_filesnamein) 
	{
		this.add_result_filesname = add_result_filesnamein;
	}
	
	
	public void setArgFromPrevious(boolean argfrompreviousin) 
	{
		this.arg_from_previous = argfrompreviousin;
	}
	
	
	public void setDestinationIsAFile(boolean destination_is_a_file)
	{
		this.destination_is_a_file=destination_is_a_file;
	}
	
	public void setCreateDestinationFolder(boolean create_destination_folder)
	{
		this.create_destination_folder=create_destination_folder;
	}
	
	public void setDoNotProcessRest(boolean IgnoreRestOfFiles)
	{
		this.IgnoreRestOfFiles=IgnoreRestOfFiles;
	}
	
	public void setNrErrorsLessThan(String nr_errors_less_than)
	{
		this.nr_errors_less_than=nr_errors_less_than;
	}
	
	public String getNrErrorsLessThan()
	{
		return nr_errors_less_than;
	}
	
	
	public void setSuccessCondition(String success_condition)
	{
		this.success_condition=success_condition;
	}
	public String getSuccessCondition()
	{
		return success_condition;
	}
	
	
   public void check(List<CheckResultInterface> remarks, JobMeta jobMeta) 
   {
	    boolean res = andValidator().validate(this, "arguments", remarks, putValidators(notNullValidator())); 

	    if (res == false) 
	    {
	      return;
	    }

	    ValidatorContext ctx = new ValidatorContext();
	    putVariableSpace(ctx, getVariables());
	    putValidators(ctx, notNullValidator(), fileExistsValidator());

	    for (int i = 0; i < source_filefolder.length; i++) 
	    {
	      andValidator().validate(this, "arguments[" + i + "]", remarks, ctx);
	    } 
	  }

   public boolean evaluates() {
		return true;
   }
}