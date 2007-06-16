package be.ibridge.kettle.core;

import be.ibridge.kettle.repository.RepositoryDirectory;

public class LastUsedFile
{
    public static final String FILE_TYPE_TRANSFORMATION = "Trans";
    public static final String FILE_TYPE_JOB            = "Job";
    public static final String FILE_TYPE_SCHEMA         = "Schema";
    
    private String  fileType;
    private String  filename;
    private String  directory;
    private boolean sourceRepository;
    private String  repositoryName;

    /**
     * @param fileType The type of file to use (FILE_TYPE_TRANSFORMATION, FILE_TYPE_JOB, ...)
     * @param filename
     * @param directory
     * @param sourceRepository
     * @param repositoryName
     */
    public LastUsedFile(String fileType, String filename, String directory, boolean sourceRepository, String repositoryName)
    {
        this.fileType = fileType;
        this.filename = filename;
        this.directory = directory;
        this.sourceRepository = sourceRepository;
        this.repositoryName = repositoryName;
    }
    
    public String toString()
    {
        String string = "";
        
        if (sourceRepository && !Const.isEmpty(directory) && !Const.isEmpty(repositoryName))
        {
            string+="["+repositoryName+"] "; 
            
            if (directory.endsWith(RepositoryDirectory.DIRECTORY_SEPARATOR))
            {
                string+=": "+directory+filename;
            }
            else
            {
                string+=": "+RepositoryDirectory.DIRECTORY_SEPARATOR+filename;
            }
        }
        else
        {
            string+=filename;
        }
            
        return string;
    }
    
    public int hashCode()
    {
        return (getFileType()+toString()).hashCode();
    }
    
    public boolean equals(Object obj)
    {
        LastUsedFile file = (LastUsedFile) obj;
        return getFileType().equals(file.getFileType()) && toString().equals(file.toString());
    }

    /**
     * @return the directory
     */
    public String getDirectory()
    {
        return directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    /**
     * @return the filename
     */
    public String getFilename()
    {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * @return the repositoryName
     */
    public String getRepositoryName()
    {
        return repositoryName;
    }

    /**
     * @param repositoryName the repositoryName to set
     */
    public void setRepositoryName(String repositoryName)
    {
        this.repositoryName = repositoryName;
    }

    /**
     * @return the sourceRepository
     */
    public boolean isSourceRepository()
    {
        return sourceRepository;
    }

    /**
     * @param sourceRepository the sourceRepository to set
     */
    public void setSourceRepository(boolean sourceRepository)
    {
        this.sourceRepository = sourceRepository;
    }

    /**
     * @return the fileType
     */
    public String getFileType()
    {
        return fileType;
    }

    /**
     * @param fileType the fileType to set
     */
    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }
    
    public boolean isTransformation()
    {
        return FILE_TYPE_TRANSFORMATION.equalsIgnoreCase(fileType);
    }

    public boolean isJob()
    {
        return FILE_TYPE_JOB.equalsIgnoreCase(fileType);
    }
    
    public boolean isSchema()
    {
        return FILE_TYPE_SCHEMA.equalsIgnoreCase(fileType);
    }
}
