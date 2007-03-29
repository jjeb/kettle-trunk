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
 
package be.ibridge.kettle.job.entry.zipfile;

import java.io.*;
import java.util.Date;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Node;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Result;
import be.ibridge.kettle.core.XMLHandler;
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
import be.ibridge.kettle.core.vfs.KettleVFS;
import org.apache.commons.vfs.FileObject;



/**
 * This defines a 'zip file' job entry. Its main use would be to 
 * zip files in a directory and process zipped files (deleted or move)
 * 
 * @author Samatar Hassan
 * @since 27-02-2007
 *
 */
public class JobEntryZipFile extends JobEntryBase implements Cloneable, JobEntryInterface
{
	private String ZipFilename;
	public int compressionrate;
	public int ifzipfileexists;
	public int afterzip;
	private String wildcard;
	private String wildcardexclude;
	private String targetdirectory;
	private String movetodirectory;
	

	public JobEntryZipFile(String n)
	{
		super(n, "");
		ZipFilename=null;
		ifzipfileexists=2;
		afterzip=0;
		compressionrate=1;
		wildcard=null;
		wildcardexclude=null;
		targetdirectory=null;
		movetodirectory=null;
		setID(-1L);
		setType(JobEntryInterface.TYPE_JOBENTRY_ZIP_FILE);
	}

	public JobEntryZipFile()
	{
		this("");
	}

	public JobEntryZipFile(JobEntryBase jeb)
	{
		super(jeb);
	}

    public Object clone()
    {
        JobEntryZipFile je = (JobEntryZipFile) super.clone();
        return je;
    }
    
	public String getXML()
	{
        StringBuffer retval = new StringBuffer(50);
		
		retval.append(super.getXML());		
		retval.append("      ").append(XMLHandler.addTagValue("ZipFilename",   ZipFilename));
		retval.append("      ").append(XMLHandler.addTagValue("compressionrate",  compressionrate));
		retval.append("      ").append(XMLHandler.addTagValue("ifzipfileexists",  ifzipfileexists));
		retval.append("      ").append(XMLHandler.addTagValue("wildcard",     wildcard));
		retval.append("      ").append(XMLHandler.addTagValue("wildcardexclude",     wildcardexclude));
		retval.append("      ").append(XMLHandler.addTagValue("targetdirectory",     targetdirectory));
		retval.append("      ").append(XMLHandler.addTagValue("movetodirectory",     movetodirectory));
		retval.append("      ").append(XMLHandler.addTagValue("afterzip",  afterzip));
		return retval.toString();
	}
	
	public void loadXML(Node entrynode, ArrayList databases, Repository rep)
		throws KettleXMLException
	{
		try
		{
			super.loadXML(entrynode, databases);
			ZipFilename = XMLHandler.getTagValue(entrynode, "ZipFilename");
			compressionrate     = Const.toInt(XMLHandler.getTagValue(entrynode, "compressionrate"), -1);
			ifzipfileexists     = Const.toInt(XMLHandler.getTagValue(entrynode, "ifzipfileexists"), -1);
			afterzip     = Const.toInt(XMLHandler.getTagValue(entrynode, "afterzip"), -1);

    		wildcard = XMLHandler.getTagValue(entrynode, "wildcard");
			wildcardexclude = XMLHandler.getTagValue(entrynode, "wildcardexclude");
			targetdirectory = XMLHandler.getTagValue(entrynode, "targetdirectory");
			movetodirectory = XMLHandler.getTagValue(entrynode, "movetodirectory");

		}
		catch(KettleXMLException xe)
		{
			throw new KettleXMLException("Unable to load job entry of type 'create file' from XML node", xe);
		}
	}

	public void loadRep(Repository rep, long id_jobentry, ArrayList databases)
		throws KettleException
	{
		try
		{
			super.loadRep(rep, id_jobentry, databases);
			ZipFilename = rep.getJobEntryAttributeString(id_jobentry, "ZipFilename");
			compressionrate=Const.toInt(rep.getJobEntryAttributeString(id_jobentry, "compressionrate"),-1);
			ifzipfileexists=Const.toInt(rep.getJobEntryAttributeString(id_jobentry, "ifzipfileexists"),-1);
			afterzip=Const.toInt(rep.getJobEntryAttributeString(id_jobentry, "afterzip"),-1);
			wildcard = rep.getJobEntryAttributeString(id_jobentry, "wildcard");
			wildcardexclude = rep.getJobEntryAttributeString(id_jobentry, "wildcardexclude");
			targetdirectory = rep.getJobEntryAttributeString(id_jobentry, "targetdirectory");
			movetodirectory = rep.getJobEntryAttributeString(id_jobentry, "movetodirectory");
		
		}
		catch(KettleException dbe)
		{
			throw new KettleException("Unable to load job entry of type 'create file' from the repository for id_jobentry="+id_jobentry, dbe);
		}
	}
	
	public void saveRep(Repository rep, long id_job)
		throws KettleException
	{
		try
		{
			super.saveRep(rep, id_job);
			
			rep.saveJobEntryAttribute(id_job, getID(), "ZipFilename", ZipFilename);
			rep.saveJobEntryAttribute(id_job, getID(), "compressionrate", compressionrate);
			rep.saveJobEntryAttribute(id_job, getID(), "ifzipfileexists", ifzipfileexists);
			rep.saveJobEntryAttribute(id_job, getID(), "afterzip", afterzip);

			rep.saveJobEntryAttribute(id_job, getID(), "wildcard", wildcard);
			rep.saveJobEntryAttribute(id_job, getID(), "wildcardexclude", wildcardexclude);
			rep.saveJobEntryAttribute(id_job, getID(), "targetdirectory", targetdirectory);
			rep.saveJobEntryAttribute(id_job, getID(), "movetodirectory", movetodirectory);

		
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to save job entry of type 'create file' to the repository for id_job="+id_job, dbe);
		}
	}


	
	public Result execute(Result prev_result, int nr, Repository rep, Job parentJob)
	{
		LogWriter log = LogWriter.getInstance();
		Result result = new Result(nr);
		result.setResult( false );
		boolean Fileexists=false;

		String realZipfilename        = StringUtil.environmentSubstitute(ZipFilename);
		String realWildcard        = StringUtil.environmentSubstitute(wildcard);
		String realWildcardExclude        = StringUtil.environmentSubstitute(wildcardexclude);
		String realTargetdirectory        = StringUtil.environmentSubstitute(targetdirectory);
		String realMovetodirectory        = StringUtil.environmentSubstitute(movetodirectory);

			
	
		if (ZipFilename!=null)
		{
        
            FileObject fileObject = null;
			try {
				fileObject = KettleVFS.getFileObject(realZipfilename);
				// Check if Zip File exists
				if ( fileObject.exists() )
				{
					Fileexists =true;
					log.logDebug(toString(), Messages.getString("JobZipFiles.Zip_FileExists1.Label")+ ZipFilename 
											+ Messages.getString("JobZipFiles.Zip_FileExists2.Label"));

				}
				else
				{
					Fileexists =false;
				}

	
				// Let's start the process now
				if (ifzipfileexists==3 && Fileexists)
				{
					// the zip file exists and user want to Fail
					result.setResult( false );
					result.setNrErrors(1);

				}
				else if(ifzipfileexists==2 && Fileexists)
				{
					// the zip file exists and user want to do nothing
					result.setResult( true );

				}
				else if(afterzip==2 && realMovetodirectory== null)
				{
					// After Zip, Move files..User must give a destination Folder
					result.setResult( false );
					result.setNrErrors(1);
					log.logError(toString(), Messages.getString("JobZipFiles.AfterZip_No_DestinationFolder_Defined.Label"));

				}
				
				else 
					// After Zip, Move files..User must give a destination Folder

				{

					if(ifzipfileexists==0 && Fileexists)
					{

						// the zip file exists and user want to create new one with unique name
						//Format Date
		
						DateFormat dateFormat = new SimpleDateFormat("hhmmss_mmddyyyy");
						ZipFilename=ZipFilename + "_" + dateFormat.format(new Date())+".zip";		
						log.logDebug(toString(), Messages.getString("JobZipFiles.Zip_FileNameChange1.Label") + ZipFilename + 
												Messages.getString("JobZipFiles.Zip_FileNameChange1.Label"));


					}
					else if(ifzipfileexists==1 && Fileexists)
					{
						log.logDebug(toString(), Messages.getString("JobZipFiles.Zip_FileAppend1.Label") + ZipFilename + 
										Messages.getString("JobZipFiles.Zip_FileAppend2.Label"));
					}
				

					// Get all the files in the directory...

					File f = new File(realTargetdirectory);

					String [] filelist = f.list();

					log.logDetailed(toString(), Messages.getString("JobZipFiles.Files_Found1.Label") +filelist.length+ 
										Messages.getString("JobZipFiles.Files_Found2.Label") + realTargetdirectory + 
										Messages.getString("JobZipFiles.Files_Found3.Label"));



					Pattern pattern = null;
					if (!Const.isEmpty(realWildcard)) 
					{
						pattern = Pattern.compile(realWildcard);
				
					}
					Pattern patternexclude = null;
					if (!Const.isEmpty(realWildcardExclude)) 
					{
						patternexclude = Pattern.compile(realWildcardExclude);
				
					}

					// Prepare Zip File
					byte[] buffer = new byte[18024];
					
					FileOutputStream dest = new FileOutputStream(ZipFilename);
					BufferedOutputStream buff = new BufferedOutputStream(dest);
					ZipOutputStream out = new ZipOutputStream(buff);


					// Set the method
					out.setMethod(ZipOutputStream.DEFLATED);

					// Set the compression level
					if (compressionrate==0)
					{
						out.setLevel(Deflater.NO_COMPRESSION);
					}
					else if (compressionrate==1)
					{
						out.setLevel(Deflater.DEFAULT_COMPRESSION);
					}
					if (compressionrate==2)
					{
						out.setLevel(Deflater.BEST_COMPRESSION);
					}
					if (compressionrate==3)
					{
						out.setLevel(Deflater.BEST_SPEED);
					}


					// Specify Zipped files (After that we will move,delete them...)
					String[] ZippedFiles = new String[filelist.length];
					int FileNum=0;

				

					// Get the files in the list...
					for (int i=0;i<filelist.length && !parentJob.isStopped();i++)
					{
						boolean getIt = true;
						boolean getItexclude = false;
			
				
						// First see if the file matches the regular expression!
						if (pattern!=null)
						{
							Matcher matcher = pattern.matcher(filelist[i]);
							getIt = matcher.matches();
						}

						if (patternexclude!=null)
						{
							Matcher matcherexclude = patternexclude.matcher(filelist[i]);
							getItexclude = matcherexclude.matches();
						}
						
						// Get processing File
						String targetFilename = realTargetdirectory+Const.FILE_SEPARATOR+filelist[i];
						File file = new File(targetFilename);

						if (getIt && !getItexclude && !file.isDirectory())
						{

							// We can add the file to the Zip Archive

							log.logDebug(toString(), Messages.getString("JobZipFiles.Add_FilesToZip1.Label")+filelist[i]+
										Messages.getString("JobZipFiles.Add_FilesToZip2.Label")+realTargetdirectory+
										Messages.getString("JobZipFiles.Add_FilesToZip3.Label"));
							
							// Associate a file input stream for the current file
							FileInputStream in = new FileInputStream(targetFilename);

							// Add ZIP entry to output stream.
							out.putNextEntry(new ZipEntry(filelist[i]));

	
							int len;
							while ((len = in.read(buffer)) > 0)
							{
								out.write(buffer, 0, len);
							}

							out.closeEntry();

							// Close the current file input stream
							in.close(); 

							// Get Zipped File
							ZippedFiles[FileNum] = filelist[i];
							FileNum=FileNum+1;
							
	
						}

					}
						
					// Close the ZipOutPutStream
					out.close();

					


					//-----Get the list of Zipped Files and Move or Delete Them
					if (afterzip == 1 || afterzip==2)
					{
						// iterate through the array of Zipped files
						for (int i = 0; i < ZippedFiles.length; i++) 
						{
							if ( ZippedFiles[i] != null)
							{
								// Delete File
								FileObject fileObjectd = KettleVFS.getFileObject(realTargetdirectory+Const.FILE_SEPARATOR+ZippedFiles[i]);

								// Here we can move, delete files
								if (afterzip == 1)
								{
									// Delete File
									boolean deleted = fileObjectd.delete();
									if ( ! deleted )
									{	
						    			result.setResult( false );
										result.setNrErrors(1);
										log.logError(toString(), Messages.getString("JobZipFiles.Cant_Delete_File1.Label")+
											realTargetdirectory+Const.FILE_SEPARATOR+ZippedFiles[i]+
												Messages.getString("JobZipFiles.Cant_Delete_File2.Label"));

									}
									// File deleted
									log.logDebug(toString(), Messages.getString("JobZipFiles.File_Deleted1.Label") + 
										realTargetdirectory+Const.FILE_SEPARATOR+ZippedFiles[i] + 
										Messages.getString("JobZipFiles.File_Deleted2.Label"));




								}
								else if(afterzip == 2)
								{
									// Move File	
									try
									{
										FileObject fileObjectm = KettleVFS.getFileObject(realMovetodirectory + Const.FILE_SEPARATOR+ZippedFiles[i]);
										fileObjectd.moveTo(fileObjectm);
									}
									catch (IOException e) 
									{
										log.logError(toString(), Messages.getString("JobZipFiles.Cant_Move_File1.Label") +ZippedFiles[i]+
											Messages.getString("JobZipFiles.Cant_Move_File2.Label") + e.getMessage());
										result.setResult( false );
										result.setNrErrors(1);				
	

									}
									// File moved
									log.logDebug(toString(), Messages.getString("JobZipFiles.File_Moved1.Label") + ZippedFiles[i] + 
										Messages.getString("JobZipFiles.File_Moved2.Label"));

								 }
							}
						}
					}
					result.setResult( true );
				}
				}
			catch (IOException e) 
			{
       			log.logError(toString(), Messages.getString("JobZipFiles.Cant_CreateZipFile1.Label") +realZipfilename+
		       							 Messages.getString("JobZipFiles.Cant_CreateZipFile2.Label") + e.getMessage());
				result.setResult( false );
				result.setNrErrors(1);				
			}
			
			finally 
			{
				if ( fileObject != null )
				{
					try  
					{
						fileObject.close();
					}
					catch ( IOException ex ) {};
				}
			}
		}
		else
		{	
			result.setResult( false );
			result.setNrErrors(1);
			log.logError(toString(), Messages.getString("JobZipFiles.No_ZipFile_Defined.Label"));
		}
		
		return result;
	}

	public boolean evaluates()
	{
		return true;
	}
    
    public JobEntryDialogInterface getDialog(Shell shell,JobEntryInterface jei,JobMeta jobMeta,String jobName,Repository rep) {
        return new JobEntryZipFileDialog(shell,this,jobMeta);
    }

	public void setZipFilename(String ZipFilename)
	{
		this.ZipFilename = ZipFilename;
	}
	
	public void setWildcard(String wildcard)
	{
		this.wildcard = wildcard;
	}
	public void setWildcardExclude(String wildcardexclude)
	{
		this.wildcardexclude = wildcardexclude;
	}
	
	public void setTargetDirectory(String targetdirectory)
	{
		this.targetdirectory = targetdirectory;
	}
	
	public void setMoveToDirectory(String movetodirectory)
	{
		this.movetodirectory = movetodirectory;
	}
	
	public String getTargetDirectory()
	{
		return targetdirectory;
	}

	public String getMoveToDirectory()
	{
		return movetodirectory;
	}

	public String getZipFilename()
	{
		return ZipFilename;
	}

	public String getWildcard()
	{
		return wildcard;
	}
	
	public String getWildcardExclude()
	{
		return wildcardexclude;
	}
}