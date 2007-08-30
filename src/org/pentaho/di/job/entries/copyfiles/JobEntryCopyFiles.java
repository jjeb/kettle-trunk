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
 
package org.pentaho.di.job.entries.copyfiles;
import static org.pentaho.di.job.entry.validator.AbstractFileValidator.putVariableSpace;
import static org.pentaho.di.job.entry.validator.AndValidator.putValidators;
import static org.pentaho.di.job.entry.validator.JobEntryValidatorUtils.andValidator;
import static org.pentaho.di.job.entry.validator.JobEntryValidatorUtils.fileExistsValidator;
import static org.pentaho.di.job.entry.validator.JobEntryValidatorUtils.notNullValidator;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeSet;

import org.w3c.dom.Node;

import org.pentaho.di.core.Result;
import org.pentaho.di.core.ResultFile;

import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryType;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.validator.ValidatorContext;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.core.vfs.KettleVFS;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSelectInfo;
import org.pentaho.di.core.logging.LogWriter;



/**
 * This defines a 'copy files' job entry.
 * 
 * @author Samatar Hassan
 * @since 06-05-2007
 */
public class JobEntryCopyFiles extends JobEntryBase implements Cloneable, JobEntryInterface
{
	public boolean copy_empty_folders;
	public  boolean arg_from_previous;
	public  boolean overwrite_files;
	public  boolean include_subfolders;
	public boolean add_result_filesname;
	public boolean remove_source_files;
	public  String  source_filefolder[];
	public  String  destination_filefolder[];
	public  String  wildcard[];
	TreeSet list_files_remove = new TreeSet();
	TreeSet list_add_result = new TreeSet();
	int NbrFail=0;
	
	public JobEntryCopyFiles(String n)
	{
		super(n, "");
		copy_empty_folders=true;
		arg_from_previous=false;
		source_filefolder=null;
		remove_source_files=false;
		destination_filefolder=null;
		wildcard=null;
		overwrite_files=false;
		include_subfolders=false;
		add_result_filesname=false;
		setID(-1L);
		setJobEntryType(JobEntryType.COPY_FILES);
	}

	public JobEntryCopyFiles()
	{
		this("");
	}

	public JobEntryCopyFiles(JobEntryBase jeb)
	{
		super(jeb);
	}

	public Object clone()
	{
		JobEntryCopyFiles je = (JobEntryCopyFiles) super.clone();
		return je;
	}
    
	public String getXML()
	{
		StringBuffer retval = new StringBuffer(300);
		
		retval.append(super.getXML());		
		retval.append("      ").append(XMLHandler.addTagValue("copy_empty_folders",      copy_empty_folders));
		retval.append("      ").append(XMLHandler.addTagValue("arg_from_previous",  arg_from_previous));
		retval.append("      ").append(XMLHandler.addTagValue("overwrite_files",      overwrite_files));
		retval.append("      ").append(XMLHandler.addTagValue("include_subfolders", include_subfolders));
		retval.append("      ").append(XMLHandler.addTagValue("remove_sourcefiles", remove_source_files));
		retval.append("      ").append(XMLHandler.addTagValue("add_resultfilesname", add_result_filesname));
		
		
		
		
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
	
	
 public void loadXML(Node entrynode, List<DatabaseMeta> databases, Repository rep) throws KettleXMLException {
		try
		{
			super.loadXML(entrynode, databases);
			copy_empty_folders      = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "copy_empty_folders"));
			arg_from_previous   = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "arg_from_previous") );
			overwrite_files      = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "overwrite_files") );
			include_subfolders = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "include_subfolders") );
			remove_source_files = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "remove_sourcefiles") );
			add_result_filesname = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "add_resultfilesname") );
			
		
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
			
			throw new KettleXMLException(Messages.getString("JobCopyFiles.Error.Exception.UnableLoadXML"), xe);
		}
	}

	public void loadRep(Repository rep, long id_jobentry, ArrayList databases)
		throws KettleException
	{
		try
		{
			super.loadRep(rep, id_jobentry, databases);
			copy_empty_folders      = rep.getJobEntryAttributeBoolean(id_jobentry, "copy_empty_folders");
			arg_from_previous   = rep.getJobEntryAttributeBoolean(id_jobentry, "arg_from_previous");
			overwrite_files      = rep.getJobEntryAttributeBoolean(id_jobentry, "overwrite_files");
			include_subfolders = rep.getJobEntryAttributeBoolean(id_jobentry, "include_subfolders");
			remove_source_files = rep.getJobEntryAttributeBoolean(id_jobentry, "remove_sourcefiles");
			
			add_result_filesname = rep.getJobEntryAttributeBoolean(id_jobentry, "add_resultfilesname");
			
				
			// How many arguments?
			int argnr = rep.countNrJobEntryAttributes(id_jobentry, "sourcefilefolder");
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
			
			throw new KettleException(Messages.getString("JobCopyFiles.Error.Exception.UnableLoadRep")+id_jobentry, dbe);
		}
	}
	
	public void saveRep(Repository rep, long id_job)
		throws KettleException
	{
		try
		{
			super.saveRep(rep, id_job);
			
			rep.saveJobEntryAttribute(id_job, getID(), "copyemptfolders",      copy_empty_folders);
			rep.saveJobEntryAttribute(id_job, getID(), "arg_from_previous",  arg_from_previous);
			rep.saveJobEntryAttribute(id_job, getID(), "overwrite_files",      overwrite_files);
			rep.saveJobEntryAttribute(id_job, getID(), "include_subfolders", include_subfolders);
			rep.saveJobEntryAttribute(id_job, getID(), "remove_sourcefiles", remove_source_files);
			
			
			// save the arguments...
			if (source_filefolder!=null)
			{
				for (int i=0;i<source_filefolder.length;i++) 
				{
					rep.saveJobEntryAttribute(id_job, getID(), i, "sourcefilefolder",     source_filefolder[i]);
					rep.saveJobEntryAttribute(id_job, getID(), i, "destinationfilefolder",     destination_filefolder[i]);
					rep.saveJobEntryAttribute(id_job, getID(), i, "wildcard", wildcard[i]);
				}
			}
		}
		catch(KettleDatabaseException dbe)
		{
			
			throw new KettleException(Messages.getString("JobCopyFiles.Error.Exception.UnableSaveRep")+id_job, dbe);
		}
	}

	public Result execute(Result previousResult, int nr, Repository rep, Job parentJob) throws KettleException 
	{
		    
	
		LogWriter log = LogWriter.getInstance();
		
		List<RowMetaAndData> rows = previousResult.getRows();
	    RowMetaAndData resultRow = null;
		
		Result result = previousResult;
		//List rows = previousResult.getRows();
		//Row resultRow = null;
		
		NbrFail=0;
	
		// Get source and destination files, also wildcard
		String vsourcefilefolder[] = source_filefolder;
		String vdestinationfilefolder[] = destination_filefolder;
		String vwildcard[] = wildcard;
		
		result.setResult( true );
		
			
		if (arg_from_previous)
		{
			log.logDetailed(toString(), Messages.getString("JobCopyFiles.Log.ArgFromPrevious.Found1") + " " +
					+(rows!=null?rows.size():0)+ " " + Messages.getString("JobCopyFiles.Log.ArgFromPrevious.Found2"));
			
		}

		if (arg_from_previous && rows!=null) // Copy the input row to the (command line) arguments
		{
			for (int iteration=0;iteration<rows.size();iteration++) 
			{
				resultRow = rows.get(iteration);
				vsourcefilefolder = new String[resultRow.size()];
				vdestinationfilefolder = new String[resultRow.size()];
				vwildcard = new String[resultRow.size()];
				// Get source and destination file names, also wildcard
				vsourcefilefolder[iteration] = resultRow.getString(0,null);
				vdestinationfilefolder[iteration] = resultRow.getString(1,null);
				vwildcard[iteration] = resultRow.getString(2,null);

				
				if(!Const.isEmpty(vsourcefilefolder[iteration]) &&  !Const.isEmpty(vdestinationfilefolder[iteration]))
				
				{
				
					log.logBasic(toString(), Messages.getString("JobCopyFiles.Log.ProcessingRow") + " ["  
							+ vsourcefilefolder[iteration] + "]..[" + vdestinationfilefolder[iteration] + "]..[" + vwildcard[iteration]+"]");

					if(! ProcessFileFolder(vsourcefilefolder[iteration],vdestinationfilefolder[iteration],vwildcard[iteration],parentJob,result))
					{
						// The copy process fail
						NbrFail=NbrFail++;
					}

				}
				else
				{
					 
					log.logDetailed(toString(), Messages.getString("JobCopyFiles.Log.IgnoringRow") + " [" +
							vsourcefilefolder[iteration] + "]..[" + vdestinationfilefolder[iteration] + " ]..[" +  vwildcard[iteration]+"]");
				
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
					
					log.logBasic(toString(), Messages.getString("JobCopyFiles.Log.ProcessingRow") + " ["  
							+ vsourcefilefolder[i] + "]..[" + vdestinationfilefolder[i] + "]..[" +  vwildcard[i]+"]");
					
					if(!ProcessFileFolder(vsourcefilefolder[i],vdestinationfilefolder[i],vwildcard[i],parentJob,result))
					{
						// The copy process fail
						NbrFail=NbrFail++;
					}
		
				}
				else
				{
								
					log.logDetailed(toString(), Messages.getString("JobCopyFiles.Log.IgnoringRow") + " [" +
							vsourcefilefolder[i] + "]..[" + vdestinationfilefolder[i] + " ]..[" +  vwildcard[i]+"]");
				
				}
			}
		}		
		
		// Check if all files was process with success
		if (NbrFail>0)
		{
			result.setResult( false );
			result.setNrErrors(NbrFail);	
		}

		
		return result;
	}

	private boolean ProcessFileFolder(String sourcefilefoldername,String destinationfilefoldername,String wildcard,Job parentJob,Result result)
	{

		
		LogWriter log = LogWriter.getInstance();
		boolean entrystatus = false ;
		FileObject sourcefilefolder = null;
		FileObject destinationfilefolder = null;
		
		// Clear list files to remove after copy process
		// This list is also added to result files name
		list_files_remove.clear();
		list_add_result.clear();
		
		
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
			
				// Basic Tests
				if (sourcefilefolder.getType().equals(FileType.FOLDER) && destinationfilefolder.getType().equals(FileType.FILE))
				{
					// Source is a folder, destination is a file
					// WARNING !!! CAN NOT COPY FOLDER TO FILE !!!
					
					log.logError(Messages.getString("JobCopyFiles.Log.Forbidden"), Messages.getString("JobCopyFiles.Log.CanNotCopyFolderToFile1") + " [" 
							+realSourceFilefoldername + "] " +  Messages.getString("JobCopyFiles.Log.CanNotCopyFolderToFile2") + " [" 
							+ realDestinationFilefoldername + "]");	
					
					NbrFail++;
					
				}
				else
				{
					
					if (destinationfilefolder.exists() && destinationfilefolder.getType().equals(FileType.FOLDER) && sourcefilefolder.getType().equals(FileType.FILE) )
					{				
						// Source is a file, destination is a folder
						// Copy the file to the destination folder				
						
						destinationfilefolder.copyFrom(sourcefilefolder.getParent(),new TextOneFileSelector(sourcefilefolder.getParent().toString(),sourcefilefolder.getName().getBaseName(),destinationfilefolder.toString() ) );
						
						log.logDetailed(Messages.getString("JobCopyFiles.Log.FileCopiedInfos"), 
								Messages.getString("JobCopyFiles.Log.FileCopied1") + " [" 
								 + sourcefilefolder.getName() + "] " +
								 Messages.getString("JobCopyFiles.Log.FileCopied2") + " ["
								 + destinationfilefolder.getName() + "]");
						
						
					
					}
					else
					{
						// Both source and destination are folders
						log.logBasic("","  ");
						
						log.logBasic("---> " + Messages.getString("JobCopyFiles.Log.FetchFolder"), 
								"[" + sourcefilefolder.toString() + "]");
						
						destinationfilefolder.copyFrom(sourcefilefolder,new TextFileSelector(sourcefilefolder.toString(),destinationfilefolder.toString(),realWildcard) );
						
						
					}
					
					// Remove Files if needed
					if (remove_source_files && !list_files_remove.isEmpty())
					{

						 for (Iterator iter = list_files_remove.iterator(); iter.hasNext();)
				        {
				            String fileremoventry = (String) iter.next();
				            // Remove ONLY Files
				            if (KettleVFS.getFileObject(fileremoventry).getType() == FileType.FILE)
				            {
					            boolean deletefile=KettleVFS.getFileObject(fileremoventry).delete();
					            log.logBasic(""," ------ ");
					            if (!deletefile)
								{
					            	
									log.logError("      " + Messages.getString("JobCopyFiles.Log.Error"), 
											Messages.getString("JobCopyFiles.Error.Exception.CanRemoveFileFolder") + 
											" [" + fileremoventry + "]");

								}
					            else
					            {
					            	
					            	log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileFolderRemovedInfos"), 
					            			Messages.getString("JobCopyFiles.Log.FileFolderRemoved1") + " [" +   fileremoventry + "] " 
					            			+ Messages.getString("JobCopyFiles.Log.FileFolderRemoved2"));
					            }
				            }
				        }
						
						
					}
					
					
					// Add files to result files name
					if (add_result_filesname && !list_add_result.isEmpty())
					{

						 for (Iterator iter = list_add_result.iterator(); iter.hasNext();)
				        {
				            String fileaddentry = (String) iter.next();
				            // Add ONLY Files
				            if (KettleVFS.getFileObject(fileaddentry).getType() == FileType.FILE)
				            {
			
			                    
			                	ResultFile resultFile = new ResultFile(ResultFile.FILE_TYPE_GENERAL, KettleVFS.getFileObject(fileaddentry), parentJob.getName(), toString());
			                    result.getResultFiles().put(resultFile.getFile().toString(), resultFile);
			                    
					            log.logBasic(""," ------ ");
					            log.logBasic("      " + Messages.getString("JobCopyFiles.Log.ResultFilesName"),
					            		Messages.getString("JobCopyFiles.Log.FileFolderAddedToResultFilesName1") + " [" +   fileaddentry + "] " 
					            		+ Messages.getString("JobCopyFiles.Log.FileFolderAddedToResultFilesName2"));
				            }
				        }
						
						
					}
					

				}
				
				entrystatus = true ;
			}
			else
			{
				
				log.logError(toString(), Messages.getString("JobCopyFiles.Error.SourceFileNotExists1") + " ["
						+realSourceFilefoldername+"] " + Messages.getString("JobCopyFiles.Error.SourceFileNotExists2"));					
				
			}
			
		
		}
	
		catch (IOException e) 
		{

			log.logError("Error", Messages.getString("JobCopyFiles.Error.Exception.CopyProcess1") 
					+ " ["+realSourceFilefoldername+ "] " + Messages.getString("JobCopyFiles.Error.Exception.CopyProcess1") + " ["  
					+ destinationfilefolder + "] " + Messages.getString("JobCopyFiles.Error.Exception.CopyProcess3") + e.getMessage());					
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
			
			
		}

		return entrystatus;
	}

	private class TextFileSelector implements FileSelector 
	{
		LogWriter log = LogWriter.getInstance();
		String file_wildcard=null,source_folder=null,destination_folder=null;
		
		public TextFileSelector(String sourcefolderin,String destinationfolderin,String filewildcard) 
		 {
			
			 if ( !Const.isEmpty(sourcefolderin))
			 {
				 source_folder=sourcefolderin;
			 }
			 if ( !Const.isEmpty(destinationfolderin))
			 {
				 destination_folder=destinationfolderin;
			 }
			 if ( !Const.isEmpty(filewildcard))
			 {
				 file_wildcard=filewildcard;
			 }
		 }
		 
		public boolean includeFile(FileSelectInfo info) 
		{
			boolean returncode=false;
			FileObject file_name=null;
			try
			{
				
				if (!info.getFile().toString().equals(source_folder))
				{
					// Pass over the Base folder itself
					
					String short_filename_from_basefolder=info.getFile().getName().toString().substring(source_folder.length(),info.getFile().getName().toString().length());
					String short_filename= info.getFile().getName().getBaseName();
					// Built destination filename
					file_name=KettleVFS.getFileObject(destination_folder + Const.FILE_SEPARATOR + short_filename_from_basefolder);//source_folder + Const.FILE_SEPARATOR + info.getFile().getName().getBaseName()); 
					
					if (!info.getFile().getParent().equals(info.getBaseFolder()))
					 {
						
						// Not in the Base Folder..Only if include sub folders  
						 if (include_subfolders)
						 {
							// Folders..only if include subfolders
							 if (info.getFile().getType() == FileType.FOLDER)
							 {
								 if (include_subfolders && copy_empty_folders && Const.isEmpty(file_wildcard))
								 {
									 if (!file_name.exists())
									 {
										log.logBasic(""," ------ ");

										log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FolderCopiedInfos"),  Messages.getString("JobCopyFiles.Log.FolderCopied1") + 
												" [" +info.getFile().toString() + "]" + " " + Messages.getString("JobCopyFiles.Log.FolderCopied2") + " [" +file_name.toString() + "]");
										returncode= true;
									 }
									 else
									 {
										 log.logBasic(""," ------ ");
										 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FolderExistsInfos"), Messages.getString("JobCopyFiles.Log.FolderExists1") + 
										 		" [" + file_name.getName() + "] " + Messages.getString("JobCopyFiles.Log.FolderExists2"));
										 if (overwrite_files)
										 {
											 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FolderOverwriteInfos"),Messages.getString("JobCopyFiles.Log.FolderOverwrite1") +
													 " [" +info.getFile().toString() + "] " + Messages.getString("JobCopyFiles.Log.FolderOverwrite2")  + " ]" + file_name.toString());
											 returncode= true; 
										 }
									 } 
								 }
								 
							 }
							 else
							 {
								if (GetFileWildcard(short_filename,file_wildcard))
								{	
									// Check if the file exists
									 if (!file_name.exists())
									 {
										log.logBasic(""," ------ ");
										
										log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileCopiedInfos"),Messages.getString("JobCopyFiles.Log.FileCopied1") +
												" [" +info.getFile().toString() + "] " + Messages.getString("JobCopyFiles.Log.FileCopied2")  + " [" +file_name.toString() + "]");
										
										returncode= true;
									 }
									 else
									 {
										 log.logBasic(""," ------ ");
										 
										 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileExistsInfos"), Messages.getString("JobCopyFiles.Log.FileExists1") +
												 " [" + file_name.getName() + Messages.getString("JobCopyFiles.Log.FileExists2"));
										 if (overwrite_files)
										 {
											 log.logBasic("       " + Messages.getString("JobCopyFiles.Log.FileExistsInfos"),Messages.getString("JobCopyFiles.Log.FileExists1") +
											 		" [" +info.getFile().toString() + "] "  + Messages.getString("JobCopyFiles.Log.FileExists2") + " [" + file_name.toString() + "]");
											 
											 returncode= true; 
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
						 if (info.getFile().getType() == FileType.FOLDER)
						 {
							 if (include_subfolders && copy_empty_folders  && Const.isEmpty(file_wildcard))
							 {
								 if (!file_name.exists())
								 {
									 log.logBasic(""," ------ ");
									 
									log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FolderCopiedInfos"),Messages.getString("JobCopyFiles.Log.FolderCopied1") +
												" [" +info.getFile().toString() + "] " + Messages.getString("JobCopyFiles.Log.FolderCopied2")  + " [" +file_name.toString() + "]");
									 
									 
									 returncode= true; 
								 }
								 else
								 {
									 log.logBasic(""," ------ ");
									 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FolderExistsInfos"), Messages.getString("JobCopyFiles.Log.FolderExists1") +
											 " [" + file_name.getName() + "] " + Messages.getString("JobCopyFiles.Log.FolderExists2"));
									 
									 if (overwrite_files)
									 {
										 
										 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FolderOverwriteInfos"),Messages.getString("JobCopyFiles.Log.FolderOverwrite1") +
											 		" [" +info.getFile().toString() + "] "  + Messages.getString("JobCopyFiles.Log.FolderOverwrite2") + " [" + file_name.toString() + "]");
											 
										 
										 returncode= true; 
									 }
								 }
								 
								 
							 
							 }
						 }
						 else
						 {
							 // file...Check if exists
							 if (GetFileWildcard(short_filename,file_wildcard))
							 {	
								 if (!file_name.exists())
								 {
										 
									 log.logBasic(""," ------ ");
									 
									 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileCopiedInfos"),Messages.getString("JobCopyFiles.Log.FileCopied1") +
												" [" +info.getFile().toString() + "] " + Messages.getString("JobCopyFiles.Log.FileCopied2")  + " [" +file_name.toString() + "]");
										
									 returncode= true;
									 
								 }
								 else
								 {
										
									 log.logBasic(""," ------ ");
		
									 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileExistsInfos"), Messages.getString("JobCopyFiles.Log.FileExists1") +
											 " [" + file_name.getName() + Messages.getString("JobCopyFiles.Log.FileExists2"));
									 
									 
									 if (overwrite_files)
									 {
										 
										 log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileExistsInfos"),Messages.getString("JobCopyFiles.Log.FileExists1") +
											 		" [" +info.getFile().toString() + "] "  + Messages.getString("JobCopyFiles.Log.FileExists2") + " [" + file_name.toString() + "]");
									
										 returncode= true; 
									 } 
									 
								 }
							 }
						 }
						 
						 
						
					 }
					
				}
				
			}
			catch (Exception e) 
			{
				

				log.logError(Messages.getString("JobCopyFiles.Error.Exception.CopyProcessError") , Messages.getString("JobCopyFiles.Error.Exception.CopyProcess1") 
					+ " ["+info.getFile().toString()+ "] " + Messages.getString("JobCopyFiles.Error.Exception.CopyProcess1") + " ["  
					+ file_name.toString() + "] " + Messages.getString("JobCopyFiles.Error.Exception.CopyProcess3") + e.getMessage());
				
				 returncode= false;
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
			if (returncode && remove_source_files)
			{
				// add this folder/file to remove files
				// This list will be fetched and all entries files
				// will be removed
				list_files_remove.add(info.getFile().toString());
			}
			
			if (returncode && add_result_filesname)
			{
				// add this folder/file to result files name
				list_add_result.add(file_name.toString());
			}
			
			
			return returncode;
		}

		public boolean traverseDescendents(FileSelectInfo info) 
		{
			return true;
		}
	}
	private class TextOneFileSelector implements FileSelector 
	{
		LogWriter log = LogWriter.getInstance();
		String filename=null,foldername=null,destfolder=null;
		
		public TextOneFileSelector(String sourcefolderin, String sourcefilenamein,String destfolderin) 
		 {
			 if ( !Const.isEmpty(sourcefilenamein))
			 {
				 filename=sourcefilenamein;
			 }
			 
			 if ( !Const.isEmpty(sourcefolderin))
			 {
				 foldername=sourcefolderin;
			 }
			 if ( !Const.isEmpty(destfolderin))
			 {
				 destfolder=destfolderin;
			 }
		 }
		 
		public boolean includeFile(FileSelectInfo info) 
		{
			boolean resultat=false;
			String fil_name=null;
			
			try
			{

				if (info.getFile().getType() == FileType.FILE) 
				{
					if (info.getFile().getName().getBaseName().equals(filename) && (info.getFile().getParent().toString().equals(foldername))) 
					{
						// check if the file exists
						fil_name=destfolder + Const.FILE_SEPARATOR + filename;
						
						if (KettleVFS.getFileObject(fil_name).exists())
						{
							log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileExistsInfos"), Messages.getString("JobCopyFiles.Log.FileExists1") +
									 " [" + fil_name + Messages.getString("JobCopyFiles.Log.FileExists2"));
							 
							if (overwrite_files) 
							{

								log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileOverwriteInfos"),Messages.getString("JobCopyFiles.Log.FileOverwrite1") +
									 		" [" +info.getFile().toString() + "] "  + Messages.getString("JobCopyFiles.Log.FileOverwrite2") + " [" + fil_name + "]");
							
								resultat=true;
							}
							
						}
						else
						{
		
							log.logBasic("      " + Messages.getString("JobCopyFiles.Log.FileCopiedInfos"),Messages.getString("JobCopyFiles.Log.FileCopied1") +
									" [" +info.getFile().toString() + "] " + Messages.getString("JobCopyFiles.Log.FileCopied2")  + " [" +fil_name + "]");
							
							
							resultat=true;
						}
							
					}
					
					if (resultat && remove_source_files)
					{
						// add this folder/file to remove files
						// This list will be fetched and all entries files
						// will be removed
						list_files_remove.add(info.getFile().toString());
					}
					
					if (resultat && add_result_filesname)
					{
						// add this folder/file to result files name
						list_add_result.add(KettleVFS.getFileObject(fil_name).toString());
					}
				}		
					
			}
			catch (Exception e) 
			{
				
				log.logError(Messages.getString("JobCopyFiles.Error.Exception.CopyProcessError") , Messages.getString("JobCopyFiles.Error.Exception.CopyProcess1") 
						+ " ["+info.getFile().toString()+ "] " + Messages.getString("JobCopyFiles.Error.Exception.CopyProcess1") + " ["  
						+ fil_name + "] " + Messages.getString("JobCopyFiles.Error.Exception.CopyProcess3") + e.getMessage());
					
				
				resultat= false;
			}
			
					
			return resultat;
			
		}

		public boolean traverseDescendents(FileSelectInfo info) 
		{
			return true;
		}
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
	

	public void setCopyEmptyFolders(boolean copy_empty_foldersin) 
	{
		this.copy_empty_folders = copy_empty_foldersin;
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
	
	public void setRemoveSourceFiles(boolean remove_source_filesin) 
	{
		this.remove_source_files = remove_source_filesin;
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
   
   
	  
}