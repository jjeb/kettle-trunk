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
 
package org.pentaho.di.job.entries.mail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.vfs.FileObject;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryResult;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.spoon.job.JobTracker;
import org.w3c.dom.Node;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.util.StringUtil;




/**
 * Describes a Mail Job Entry.
 * 
 * @author Matt
 * Created on 17-06-2003
 *
 */
public class JobEntryMail extends JobEntryBase implements Cloneable, JobEntryInterface
{
	private String server;
	private String destination;
	private String destinationCc;
	private String destinationBCc;
	private String replyAddress;
	private String subject;
	private boolean includeDate;
	private String contactPerson;
	private String contactPhone;
	private String comment;
	
	private boolean includingFiles;
	private int fileType[];
	
	private boolean zipFiles;
	private String zipFilename;

    private boolean usingAuthentication;
    private String authenticationUser;
    private String authenticationPassword;
    
    private boolean onlySendComment;
    private boolean useHTML;
    private boolean usingSecureAuthentication;
    private String port;
    /** The encoding to use for reading: null or empty string means system default encoding */
    private String encoding;
    
	public JobEntryMail(String n)
	{
		super(n, "");
		setType(JobEntryInterface.TYPE_JOBENTRY_MAIL);
		allocate(0);
	}

	public JobEntryMail()
	{
		this("");
		allocate(0);
	}
	
	public JobEntryMail(JobEntryBase jeb)
	{
		super(jeb);
		allocate(0);
	}

    public Object clone()
    {
        JobEntryMail je = (JobEntryMail) super.clone();
        return je;
    }
    
	public String getXML()
	{
        StringBuffer retval = new StringBuffer(300);
		
		retval.append(super.getXML());

        retval.append("      ").append(XMLHandler.addTagValue("server", server));
        retval.append("      ").append(XMLHandler.addTagValue("port", port));
        retval.append("      ").append(XMLHandler.addTagValue("destination", destination));
		retval.append("      ").append(XMLHandler.addTagValue("destinationCc", destinationCc));
		retval.append("      ").append(XMLHandler.addTagValue("destinationBCc", destinationBCc));
        retval.append("      ").append(XMLHandler.addTagValue("replyto", replyAddress));
        retval.append("      ").append(XMLHandler.addTagValue("subject", subject));
        retval.append("      ").append(XMLHandler.addTagValue("include_date", includeDate));
        retval.append("      ").append(XMLHandler.addTagValue("contact_person", contactPerson));
        retval.append("      ").append(XMLHandler.addTagValue("contact_phone", contactPhone));
        retval.append("      ").append(XMLHandler.addTagValue("comment", comment));
        retval.append("      ").append(XMLHandler.addTagValue("include_files", includingFiles));
        retval.append("      ").append(XMLHandler.addTagValue("zip_files", zipFiles));
        retval.append("      ").append(XMLHandler.addTagValue("zip_name", zipFilename));

        retval.append("      ").append(XMLHandler.addTagValue("use_auth", usingAuthentication));
        retval.append("      ").append(XMLHandler.addTagValue("use_secure_auth", usingSecureAuthentication));
        retval.append("      ").append(XMLHandler.addTagValue("auth_user", authenticationUser));
        retval.append("      ").append(XMLHandler.addTagValue("auth_password", Encr.encryptPasswordIfNotUsingVariables(authenticationPassword)));

        retval.append("      ").append(XMLHandler.addTagValue("only_comment", onlySendComment));
        retval.append("      ").append(XMLHandler.addTagValue("use_HTML", useHTML));
        retval.append("    "+XMLHandler.addTagValue("encoding",        encoding));
        

        retval.append("      <filetypes>");
        if (fileType!=null)
        for (int i=0;i<fileType.length;i++)
        {
        	retval.append("        ").append(XMLHandler.addTagValue("filetype", ResultFile.getTypeCode(fileType[i])));
        }
        retval.append("      </filetypes>");

		return retval.toString();
	}
	
	public void allocate(int nrFileTypes)
	{
		fileType = new int[nrFileTypes];
	}
	
	public void loadXML(Node entrynode, ArrayList databases, Repository rep) throws KettleXMLException
	{
		try
		{
			super.loadXML(entrynode, databases);
			setServer       ( XMLHandler.getTagValue(entrynode, "server") );
            setPort         ( XMLHandler.getTagValue(entrynode, "port") );
			setDestination  ( XMLHandler.getTagValue(entrynode, "destination") );
			setDestinationCc  ( XMLHandler.getTagValue(entrynode, "destinationCc") );
			setDestinationBCc  ( XMLHandler.getTagValue(entrynode, "destinationBCc") );
			setReplyAddress ( XMLHandler.getTagValue(entrynode, "replyto") );
			setSubject      ( XMLHandler.getTagValue(entrynode, "subject") );
			setIncludeDate  ( "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "include_date")) );
			setContactPerson( XMLHandler.getTagValue(entrynode, "concact_person") );
			setContactPhone ( XMLHandler.getTagValue(entrynode, "concact_phone") );
			setComment      ( XMLHandler.getTagValue(entrynode, "comment") );
			setIncludingFiles ( "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "include_files")) );

            setUsingAuthentication( "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "use_auth")) );
            setUsingSecureAuthentication( "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "use_secure_auth")) );
            setAuthenticationUser( XMLHandler.getTagValue(entrynode, "auth_user") );
            setAuthenticationPassword( Encr.decryptPasswordOptionallyEncrypted( XMLHandler.getTagValue(entrynode, "auth_password") ) );

            setOnlySendComment( "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "only_comment") ) );
            setUseHTML( "Y".equalsIgnoreCase( XMLHandler.getTagValue(entrynode, "use_HTML") ) );
            
            
            setEncoding      ( XMLHandler.getTagValue(entrynode, "encoding") );
            
			Node ftsnode = XMLHandler.getSubNode(entrynode, "filetypes");
			int nrTypes = XMLHandler.countNodes(ftsnode, "filetype");
			allocate(nrTypes);
			for (int i=0;i<nrTypes;i++)
			{
				Node ftnode = XMLHandler.getSubNodeByNr(ftsnode, "filetype", i); 
				fileType[i]=ResultFile.getType(XMLHandler.getNodeValue(ftnode));
			}
			
			setZipFiles( "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "zip_files")) );
			setZipFilename( XMLHandler.getTagValue(entrynode, "zip_name") );

		}
		catch(KettleException xe)
		{
			throw new KettleXMLException("Unable to load job entry of type 'mail' from XML node", xe);			
		}
	}

	public void loadRep(Repository rep, long id_jobentry, ArrayList databases)
		throws KettleException
	{
		try
		{
			super.loadRep(rep, id_jobentry, databases);

        	// First load the common parts like name & description, then the attributes...
			//
			server          = rep.getJobEntryAttributeString (id_jobentry, "server");
            port            = rep.getJobEntryAttributeString (id_jobentry, "port");
			destination     = rep.getJobEntryAttributeString (id_jobentry, "destination");
			destinationCc     = rep.getJobEntryAttributeString (id_jobentry, "destinationCc");
			destinationBCc     = rep.getJobEntryAttributeString (id_jobentry, "destinationBCc");
			replyAddress    = rep.getJobEntryAttributeString (id_jobentry, "replyto");
			subject         = rep.getJobEntryAttributeString (id_jobentry, "subject");
			includeDate     = rep.getJobEntryAttributeBoolean(id_jobentry, "include_date");
			contactPerson   = rep.getJobEntryAttributeString (id_jobentry, "contact_person");
			contactPhone    = rep.getJobEntryAttributeString (id_jobentry, "contact_phone");
			comment         = rep.getJobEntryAttributeString (id_jobentry, "comment");
			encoding         = rep.getJobEntryAttributeString (id_jobentry, "encoding");
			includingFiles  = rep.getJobEntryAttributeBoolean(id_jobentry, "include_files");
			
            usingAuthentication = rep.getJobEntryAttributeBoolean(id_jobentry, "use_auth");
            usingSecureAuthentication = rep.getJobEntryAttributeBoolean(id_jobentry, "use_secure_auth");
            authenticationUser = rep.getJobEntryAttributeString(id_jobentry, "auth_user");
            authenticationPassword = Encr.decryptPasswordOptionallyEncrypted( rep.getJobEntryAttributeString(id_jobentry, "auth_password") );

            onlySendComment = rep.getJobEntryAttributeBoolean(id_jobentry, "only_comment");
            useHTML = rep.getJobEntryAttributeBoolean(id_jobentry, "use_HTML");
            
            

			int nrTypes = rep.countNrJobEntryAttributes(id_jobentry, "file_type");
			allocate(nrTypes);
			
            for (int i=0;i<nrTypes;i++)
            {
            	String typeCode = rep.getJobEntryAttributeString(id_jobentry, i, "file_type");
            	fileType[i] = ResultFile.getType(typeCode);
	        }

			zipFiles       = rep.getJobEntryAttributeBoolean(id_jobentry, "zip_files");
			zipFilename    = rep.getJobEntryAttributeString(id_jobentry, "zip_name");
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to load job entry of type 'mail' from the repository with id_jobentry="+id_jobentry, dbe);
		}

	}
	
	public void saveRep(Repository rep, long id_job)
		throws KettleException
	{
		try
		{
			super.saveRep(rep, id_job);
			
			rep.saveJobEntryAttribute(id_job, getID(), "server", server);
            rep.saveJobEntryAttribute(id_job, getID(), "port", port);
			rep.saveJobEntryAttribute(id_job, getID(), "destination", destination);
			rep.saveJobEntryAttribute(id_job, getID(), "destinationCc", destinationCc);
			rep.saveJobEntryAttribute(id_job, getID(), "destinationBCc", destinationBCc);
			rep.saveJobEntryAttribute(id_job, getID(), "replyto", replyAddress);
			rep.saveJobEntryAttribute(id_job, getID(), "subject", subject);
			rep.saveJobEntryAttribute(id_job, getID(), "include_date", includeDate);
			rep.saveJobEntryAttribute(id_job, getID(), "contact_person", contactPerson);
			rep.saveJobEntryAttribute(id_job, getID(), "contact_phone", contactPhone);
			rep.saveJobEntryAttribute(id_job, getID(), "comment", comment);
			rep.saveJobEntryAttribute(id_job, getID(), "encoding", encoding);
			rep.saveJobEntryAttribute(id_job, getID(), "include_files", includingFiles);
            rep.saveJobEntryAttribute(id_job, getID(), "use_auth", usingAuthentication);
            rep.saveJobEntryAttribute(id_job, getID(), "use_secure_auth", usingSecureAuthentication);
            rep.saveJobEntryAttribute(id_job, getID(), "auth_user", authenticationUser);
            rep.saveJobEntryAttribute(id_job, getID(), "auth_password", Encr.encryptPasswordIfNotUsingVariables( authenticationPassword ));
            
            rep.saveJobEntryAttribute(id_job, getID(), "only_comment", onlySendComment);
            rep.saveJobEntryAttribute(id_job, getID(), "use_HTML", useHTML);
            
            
			
			if (fileType!=null)
			{
				for (int i=0;i<fileType.length;i++)
				{
					rep.saveJobEntryAttribute(id_job, getID(), i, "file_type", ResultFile.getTypeCode(fileType[i]));
				}
			}
			
			rep.saveJobEntryAttribute(id_job, getID(), "zip_files", zipFiles);
			rep.saveJobEntryAttribute(id_job, getID(), "zip_name", zipFilename);

		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to save job entry of type 'mail' to the repository for id_job="+id_job, dbe);
		}
			
	}
	
	public void setServer(String s)
	{
		server=s;
	}
	
	public String getServer()
	{
		return server;
	}

	public void setDestination(String dest)
	{
		destination=dest;
	}
	public void setDestinationCc(String destCc)
	{
		destinationCc=destCc;
	}
	public void setDestinationBCc(String destBCc)
	{
		destinationBCc=destBCc;
	}
	public String getDestination()
	{
		return destination;
	}
	public String getDestinationCc()
	{
			return destinationCc;
	}
	public String getDestinationBCc()
	{
	
		return destinationBCc;
	}
	public void setReplyAddress(String reply)
	{
		replyAddress=reply;
	}
	
	public String getReplyAddress()
	{
		return replyAddress;
	}

	public void setSubject(String subj)
	{
		subject=subj;
	}
	
	public String getSubject()
	{
		return subject;
	}

	public void setIncludeDate(boolean incl)
	{
		includeDate=incl;
	}
	
	public boolean getIncludeDate()
	{
		return includeDate;
	}

	public void setContactPerson(String person)
	{
		contactPerson=person;
	}
	
	public String getContactPerson()
	{
		return contactPerson;
	}

	public void setContactPhone(String phone)
	{
		contactPhone=phone;
	}
	
	public String getContactPhone()
	{
		return contactPhone;
	}

	public void setComment(String comm)
	{
		comment = comm;
	}
	
	public String getComment()
	{
		return comment;
	}	
    
    /**
     * @return the result file types to select for attachement </b>
     * @see ResultFile
     */
    public int[] getFileType()
    {
        return fileType;
    }

    /**
     * @param fileType the result file types to select for attachement
     * @see ResultFile
     */
    public void setFileType(int[] fileType)
    {
        this.fileType = fileType;
    }

    public boolean isIncludingFiles() {
        return includingFiles;
    }

    public void setIncludingFiles(boolean includeFiles) {
        this.includingFiles = includeFiles;
    }
    
    /**
     * @return Returns the zipFilename.
     */
    public String getZipFilename()
    {
        return zipFilename;
    }

    /**
     * @param zipFilename The zipFilename to set.
     */
    public void setZipFilename(String zipFilename)
    {
        this.zipFilename = zipFilename;
    }

    /**
     * @return Returns the zipFiles.
     */
    public boolean isZipFiles()
    {
        return zipFiles;
    }

    /**
     * @param zipFiles The zipFiles to set.
     */
    public void setZipFiles(boolean zipFiles)
    {
        this.zipFiles = zipFiles;
    }

    /**
     * @return Returns the authenticationPassword.
     */
    public String getAuthenticationPassword()
    {
        return authenticationPassword;
    }

    /**
     * @param authenticationPassword The authenticationPassword to set.
     */
    public void setAuthenticationPassword(String authenticationPassword)
    {
        this.authenticationPassword = authenticationPassword;
    }

    /**
     * @return Returns the authenticationUser.
     */
    public String getAuthenticationUser()
    {
        return authenticationUser;
    }

    /**
     * @param authenticationUser The authenticationUser to set.
     */
    public void setAuthenticationUser(String authenticationUser)
    {
        this.authenticationUser = authenticationUser;
    }

    /**
     * @return Returns the usingAuthentication.
     */
    public boolean isUsingAuthentication()
    {
        return usingAuthentication;
    }

    /**
     * @param usingAuthentication The usingAuthentication to set.
     */
    public void setUsingAuthentication(boolean usingAuthentication)
    {
        this.usingAuthentication = usingAuthentication;
    }

    /**
     * @return the onlySendComment flag
     */
    public boolean isOnlySendComment()
    {
        return onlySendComment;
    }

    /**
     * @param onlySendComment the onlySendComment flag to set
     */
    public void setOnlySendComment(boolean onlySendComment)
    {
        this.onlySendComment = onlySendComment;
    }
    
    
    /**
     * @return the useHTML flag
     */
    public boolean isUseHTML()
    {
        return useHTML;
    }

    /**
     * @param useHTML the useHTML to set
     */
    public void setUseHTML(boolean UseHTML)
    {
        this.useHTML = UseHTML;
    }
    
    
    /**
     * @return the encoding
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
    
    
	public Result execute(Result result, int nr, Repository rep, Job parentJob)
	{
		LogWriter log = LogWriter.getInstance();

		File masterZipfile=null;
		
		// Send an e-mail...
		// create some properties and get the default Session
		Properties props = new Properties();
        if (Const.isEmpty(server))
        {
            log.logError(toString(), "Unable to send the mail because the mail-server (SMTP host) is not specified");
            result.setNrErrors(1L);
            result.setResult(false);
            return result;
        }
        
        String protocol = "smtp";
        if (usingSecureAuthentication)
        {
            protocol="smtps";
        }
        
		props.put("mail."+protocol+".host", StringUtil.environmentSubstitute(server));
        if (!Const.isEmpty(port)) props.put("mail."+protocol+".port", StringUtil.environmentSubstitute(port));
        boolean debug = log.getLogLevel()>=LogWriter.LOG_LEVEL_DEBUG;
		
		if (debug) props.put("mail.debug", "true");
        
        if (usingAuthentication)
        {
        	props.put("mail."+protocol+".auth", "true"); 

            /*
            authenticator = new Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(
                                StringUtil.environmentSubstitute(Const.NVL(authenticationUser, "")), 
                                StringUtil.environmentSubstitute(Const.NVL(authenticationPassword, ""))
                            );
                }
            };
            */
        }

		Session session = Session.getInstance(props);
		session.setDebug(debug);
		
		try 
		{
			// create a message
		    Message msg = new MimeMessage(session);
		    
	
		    
		    String email_address = StringUtil.environmentSubstitute(replyAddress);
		    if ( !Const.isEmpty(email_address) )
		    {
		        msg.setFrom(new InternetAddress(email_address));
		    }
		    else
		    {
				throw new MessagingException("reply e-mail address is not filled in");			
		    }
            
            // Split the mail-address: space separated
            String destinations[] = StringUtil.environmentSubstitute(destination).split(" ");
		    InternetAddress[] address = new InternetAddress[destinations.length];
            for (int i=0;i<destinations.length;i++) address[i] = new InternetAddress(destinations[i]);
            
		    msg.setRecipients(Message.RecipientType.TO, address);
		
			if (!Const.isEmpty(destinationCc))
			{
				// Split the mail-address Cc: space separated
				String destinationsCc[] = StringUtil.environmentSubstitute(destinationCc).split(" ");
				InternetAddress[] addressCc = new InternetAddress[destinationsCc.length];
				for (int i=0;i<destinationsCc.length;i++) addressCc[i] = new InternetAddress(destinationsCc[i]);
	            
				msg.setRecipients(Message.RecipientType.CC, addressCc);
			}

			if (!Const.isEmpty(destinationBCc))
			{
				// Split the mail-address BCc: space separated
				String destinationsBCc[] = StringUtil.environmentSubstitute(destinationBCc).split(" ");
				InternetAddress[] addressBCc = new InternetAddress[destinationsBCc.length];
				for (int i=0;i<destinationsBCc.length;i++) addressBCc[i] = new InternetAddress(destinationsBCc[i]);
	            
				msg.setRecipients(Message.RecipientType.BCC, addressBCc);
			}

			if (!Const.isEmpty(StringUtil.environmentSubstitute(subject)))
			{
				msg.setSubject(StringUtil.environmentSubstitute(subject));
			}
					
			msg.setSentDate(new Date());
			StringBuffer messageText = new StringBuffer();

			
			

		    if (comment!=null)
		    {
		        messageText.append(StringUtil.environmentSubstitute(comment)).append(Const.CR).append(Const.CR);
		    }

            if (!onlySendComment)
            {
    	        messageText.append("Job:").append(Const.CR);
    	        messageText.append("-----").append(Const.CR);
    	        messageText.append("Name       : ").append(parentJob.getJobMeta().getName()).append(Const.CR);
    	        messageText.append("Directory  : ").append(parentJob.getJobMeta().getDirectory()).append(Const.CR);
    	        messageText.append("JobEntry   : ").append(getName()).append(Const.CR);
    	        messageText.append(Const.CR);
            }

		    if (includeDate) 
		    {
		        messageText.append("Message date: ").append(XMLHandler.date2string(new Date())).append(Const.CR).append(Const.CR);
		    }
		    if (!onlySendComment && result!=null)
		    {
		        messageText.append("Previous result:").append(Const.CR);
		        messageText.append("-----------------").append(Const.CR);
		        messageText.append("Job entry nr         : ").append(result.getEntryNr()).append(Const.CR);
			    messageText.append("Errors               : ").append(result.getNrErrors()).append(Const.CR);
			    messageText.append("Lines read           : ").append(result.getNrLinesRead()).append(Const.CR);
			    messageText.append("Lines written        : ").append(result.getNrLinesWritten()).append(Const.CR);
			    messageText.append("Lines input          : ").append(result.getNrLinesInput()).append(Const.CR);
			    messageText.append("Lines output         : ").append(result.getNrLinesOutput()).append(Const.CR);
			    messageText.append("Lines updated        : ").append(result.getNrLinesUpdated()).append(Const.CR);
			    messageText.append("Script exit status   : ").append(result.getExitStatus()).append(Const.CR);
			    messageText.append("Result               : ").append(result.getResult()).append(Const.CR);
			    messageText.append(Const.CR);
		    }

		    if (!onlySendComment && ( !Const.isEmpty(StringUtil.environmentSubstitute(contactPerson)) || !Const.isEmpty(StringUtil.environmentSubstitute(contactPhone)) ))
		    {
		        messageText.append("Contact information :").append(Const.CR);
		        messageText.append("---------------------").append(Const.CR);
		        messageText.append("Person to contact : ").append(StringUtil.environmentSubstitute(contactPerson)).append(Const.CR);
		        messageText.append("Telephone number  : ").append(StringUtil.environmentSubstitute(contactPhone)).append(Const.CR);
			    messageText.append(Const.CR);
		    }

		    // Include the path to this job entry...
            if (!onlySendComment)
            {
    		    JobTracker jobTracker = parentJob.getJobTracker();
    		    if (jobTracker!=null)
    		    {
    		        messageText.append("Path to this job entry:").append(Const.CR);
    		        messageText.append("------------------------").append(Const.CR);

                    addBacktracking(jobTracker, messageText);
    		    }
            }

		    Multipart parts = new MimeMultipart();
			MimeBodyPart part1 = new MimeBodyPart(); // put the text in the
 	 												 // 1st part
			
			if (useHTML)
			{
				
				if (!Const.isEmpty(getEncoding()))
				{
					part1.setContent(messageText.toString(), "text/html; " + "charset=" + getEncoding()); 
				}
				else
				{
					part1.setContent(messageText.toString(), "text/html; " + "charset=ISO-8859-1"); 
				}
				
			}
				
			else
				part1.setText(messageText.toString());
							
			
			
			parts.addBodyPart(part1);
			
			
			if (includingFiles && result != null)
		    {
				List resultFiles = result.getResultFilesList();
				if (resultFiles!=null && resultFiles.size() > 0) 
				{
					if (!zipFiles)
					{
						// Add all files to the message...
						//
						for (Iterator iter = resultFiles.iterator(); iter.hasNext();) 
						{
							ResultFile resultFile = (ResultFile) iter.next();
							FileObject file = resultFile.getFile();
							if (file != null && file.exists()) 
							{
                                boolean found=false;
                                for (int i=0;i<fileType.length;i++)
                                {
                                    if (fileType[i]==resultFile.getType()) found=true;
                                }
                                if (found)
                                {
    								// create a data source
    								MimeBodyPart files = new MimeBodyPart();
                                    URLDataSource fds = new URLDataSource(file.getURL());

                                    // get a data Handler to manipulate this file type;
    								files.setDataHandler(new DataHandler(fds));
    								// include the file in the data source
    								files.setFileName(file.getName().getBaseName());
    								// add the part with the file in the BodyPart();
    								parts.addBodyPart(files);
                                    
                                    log.logBasic(toString(), "Added file '"+fds.getName()+"' to the mail message.");
                                }
							}
						}
					}
					else
					{
						// create a single ZIP archive of all files
						masterZipfile = new File(System.getProperty("java.io.tmpdir")+Const.FILE_SEPARATOR+StringUtil.environmentSubstitute(zipFilename));
						ZipOutputStream zipOutputStream = null;
						try
						{
							zipOutputStream = new ZipOutputStream(new FileOutputStream(masterZipfile));
                            
							for (Iterator iter = resultFiles.iterator(); iter.hasNext();) 
							{
                                ResultFile resultFile = (ResultFile) iter.next();
                                
                                boolean found=false;
                                for (int i=0;i<fileType.length;i++)
                                {
                                    if (fileType[i]==resultFile.getType()) found=true;
                                }
                                if (found)
                                {
    								FileObject file = resultFile.getFile();
    								ZipEntry zipEntry = new ZipEntry(file.getName().getBaseName());
    								zipOutputStream.putNextEntry(zipEntry);
    
    								// Now put the content of this file into this archive...
    								BufferedInputStream inputStream = new BufferedInputStream(file.getContent().getInputStream());
    								int c;
    								while ( (c=inputStream.read())>=0)
    								{
    									zipOutputStream.write(c);
    								}
    								inputStream.close();
    								zipOutputStream.closeEntry();
                                    
                                    log.logBasic(toString(), "Added file '"+file.getName().getURI()+"' to the mail message in a zip archive.");
                                }
							}
						}
						catch(Exception e)
						{
							log.logError(toString(), "Error zipping attachement files into file ["+masterZipfile.getPath()+"] : "+e.toString());
							log.logError(toString(), Const.getStackTracker(e));
							result.setNrErrors(1);
						}
						finally
						{
							if (zipOutputStream!=null)
							{
								try
								{
									zipOutputStream.finish();
									zipOutputStream.close();
								}
								catch(IOException e)
								{
									log.logError(toString(), "Unable to close attachement zip file archive : "+e.toString());
									log.logError(toString(), Const.getStackTracker(e));
									result.setNrErrors(1);
								}
							}
						}

						// Now attach the master zip file to the message.
						if (result.getNrErrors()==0)
						{
							// create a data source
							MimeBodyPart files = new MimeBodyPart();
							FileDataSource fds = new FileDataSource(masterZipfile);
							// get a data Handler to manipulate this file type;
							files.setDataHandler(new DataHandler(fds));
							// include the file in th e data source
							files.setFileName(fds.getName());
							// add the part with the file in the BodyPart();
							parts.addBodyPart(files);
						}
					}
			    }
			}
		    msg.setContent(parts);
			

            Transport transport = null;
            try
            {
                transport = session.getTransport(protocol);
                if (usingAuthentication)
                {
                    if (!Const.isEmpty(port))
                    {
                        transport.connect(
                                StringUtil.environmentSubstitute(Const.NVL(server, "")), 
                                Integer.parseInt( StringUtil.environmentSubstitute(Const.NVL(port, "")) ), 
                                StringUtil.environmentSubstitute(Const.NVL(authenticationUser, "")), 
                                StringUtil.environmentSubstitute(Const.NVL(authenticationPassword, "")) 
                                );
                    }
                    else
                    {
                        transport.connect(
                            StringUtil.environmentSubstitute(Const.NVL(server, "")), 
                            StringUtil.environmentSubstitute(Const.NVL(authenticationUser, "")), 
                            StringUtil.environmentSubstitute(Const.NVL(authenticationPassword, "")) 
                            );
                    }
                }
                else
                {
                    transport.connect();
                }
                transport.sendMessage(msg, msg.getAllRecipients());
            }
            finally
            {
                if (transport!=null) transport.close();
            }
		}
        catch(IOException e)
        {
            log.logError(toString(), "Problem while sending message: "+e.toString());
            result.setNrErrors(1);
        }
		catch (MessagingException mex) 
		{
		    log.logError(toString(), "Problem while sending message: "+mex.toString());
			result.setNrErrors(1);

		    Exception ex = mex;
		    do 
		    {
				if (ex instanceof SendFailedException) 
				{
				    SendFailedException sfex = (SendFailedException)ex;

				    Address[] invalid = sfex.getInvalidAddresses();
				    if (invalid != null) 
				    {
				    	log.logError(toString(), "    ** Invalid Addresses");
     		    		for (int i = 0; i < invalid.length; i++)
			    		{
			    			log.logError(toString(), "         " + invalid[i]);
			    			result.setNrErrors(1);
			    		}
				    }

				    Address[] validUnsent = sfex.getValidUnsentAddresses();
				    if (validUnsent != null) 
				    {
				    	log.logError(toString(), "    ** ValidUnsent Addresses");
			    		for (int i = 0; i < validUnsent.length; i++)
			    		{
			    			log.logError(toString(), "         "+validUnsent[i]);
			    			result.setNrErrors(1);
			    		}
				    }

				    Address[] validSent = sfex.getValidSentAddresses();
				    if (validSent != null) 
				    {
				    	//System.out.println("    ** ValidSent Addresses");
			    		for (int i = 0; i < validSent.length; i++)
			    		{
			    			log.logError(toString(), "         "+validSent[i]);
			    			result.setNrErrors(1);
			    		}
				    }
				}
				if (ex instanceof MessagingException)
				{
				    ex = ((MessagingException)ex).getNextException();
				}
				else
				{
				    ex = null;
				}
		    } while (ex != null);
		}
		finally
		{
			if (masterZipfile!=null && masterZipfile.exists())
			{
				masterZipfile.delete();
			}
        }

		if (result.getNrErrors() > 0)
		{
			result.setResult( false );
		}
		else
		{
			result.setResult( true );
		}

		return result;
	}

   
    

    private void addBacktracking(JobTracker jobTracker, StringBuffer messageText)
    {
        addBacktracking(jobTracker, messageText, 0);
    }

	private void addBacktracking(JobTracker jobTracker, StringBuffer messageText, int level)    
    {
       int nr = jobTracker.nrJobTrackers();

       messageText.append(Const.rightPad(" ", level*2));
       messageText.append(Const.NVL( jobTracker.getJobName(), "-") );
       JobEntryResult jer = jobTracker.getJobEntryResult(); 
       if (jer!=null)
       {
           messageText.append(" : ");
           if (jer.getJobEntry()!=null && jer.getJobEntry().getName()!=null)
           {
               messageText.append(" : ");
               messageText.append(jer.getJobEntry().getName());
           }
           if (jer.getResult()!=null)
           {
               messageText.append(" : ");
               messageText.append("["+jer.getResult().toString()+"]");
           }
           if (jer.getReason()!=null)
           {
               messageText.append(" : ");
               messageText.append(jer.getReason());
           }
           if (jer.getComment()!=null)
           {
               messageText.append(" : ");
               messageText.append(jer.getComment());
           }
           if (jer.getLogDate()!=null)
           {
               messageText.append(" (");
               messageText.append( XMLHandler.date2string( jer.getLogDate() ) ); // $NON-NLS-1$
               messageText.append(')');
           }
       }
       messageText.append(Const.CR);

       for (int i=0;i<nr;i++)
       {
           JobTracker jt = jobTracker.getJobTracker(i);
           addBacktracking(jt, messageText, level+1);
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
        return new JobEntryMailDialog(shell,this);
    }

    /**
     * @return the usingSecureAuthentication
     */
    public boolean isUsingSecureAuthentication()
    {
        return usingSecureAuthentication;
    }

    /**
     * @param usingSecureAuthentication the usingSecureAuthentication to set
     */
    public void setUsingSecureAuthentication(boolean usingSecureAuthentication)
    {
        this.usingSecureAuthentication = usingSecureAuthentication;
    }

    /**
     * @return the port
     */
    public String getPort()
    {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port)
    {
        this.port = port;
    }
    

    

}   

    

   
