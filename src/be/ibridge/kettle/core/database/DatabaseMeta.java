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

 

package be.ibridge.kettle.core.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Node;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Encr;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.SharedObjectBase;
import be.ibridge.kettle.core.SharedObjectInterface;
import be.ibridge.kettle.core.XMLHandler;
import be.ibridge.kettle.core.XMLInterface;
import be.ibridge.kettle.core.exception.KettleDatabaseException;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.core.exception.KettleXMLException;
import be.ibridge.kettle.core.util.StringUtil;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.repository.Repository;



/**
 * This class defines the database specific parameters for a certain database type.
 * It also provides static information regarding a number of well known databases.
 * 
 * @author Matt
 * @since 18-05-2003
 *
 */
public class DatabaseMeta extends SharedObjectBase implements Cloneable, XMLInterface, SharedObjectInterface
{
    public static final String XML_TAG = "connection";
    
	private DatabaseInterface databaseInterface;
	private static DatabaseInterface[] allDatabaseInterfaces;

	/**
	 * Indicates that the connections doesn't point to a type of database yet.
	 */
	public static final int TYPE_DATABASE_NONE        =  0;
	
	/**
	 * Connection to a MySQL database
	 */
	public static final int TYPE_DATABASE_MYSQL       =  1;

	/**
	 * Connection to an Oracle database
	 */
	public static final int TYPE_DATABASE_ORACLE      =  2;

	/**
	 * Connection to an AS/400 (IBM iSeries) DB400 database
	 */
	public static final int TYPE_DATABASE_AS400       =  3;

	/**
	 * Connection to an Microsoft Access database
	 */
	public static final int TYPE_DATABASE_ACCESS      =  4;

	/**
	 * Connection to a Microsoft SQL Server database
	 */
	public static final int TYPE_DATABASE_MSSQL       =  5;

	/**
	 * Connection to an IBM DB2 database
	 */
	public static final int TYPE_DATABASE_DB2         =  6;

	/**
	 * Connection to a PostgreSQL database
	 */
	public static final int TYPE_DATABASE_POSTGRES    =  7;

	/**
	 * Connection to an Intersystems Cache database
	 */
	public static final int TYPE_DATABASE_CACHE       =  8;

	/**
	 * Connection to an IBM Informix database
	 */
	public static final int TYPE_DATABASE_INFORMIX    =  9;

	/**
	 * Connection to a Sybase ASE database
	 */
	public static final int TYPE_DATABASE_SYBASE      = 10;

	/**
	 * Connection to a Gupta SQLBase database
	 */
	public static final int TYPE_DATABASE_GUPTA       = 11;

	/**
	 * Connection to a DBase III/IV/V database through JDBC
	 */
	public static final int TYPE_DATABASE_DBASE       = 12;

	/**
	 * Connection to a FireBird database
	 */
	public static final int TYPE_DATABASE_FIREBIRD    = 13;

	/**
	 * Connection to a SAP DB database
	 */
	public static final int TYPE_DATABASE_SAPDB       = 14;

	/**
	 * Connection to a Hypersonic java database
	 */
	public static final int TYPE_DATABASE_HYPERSONIC  = 15;

	/**
	 * Connection to a generic database
	 */
	public static final int TYPE_DATABASE_GENERIC     = 16;

    /**
     * Connection to an SAP R/3 system
     */
    public static final int TYPE_DATABASE_SAPR3       = 17;
    
    /**
     * Connection to an Ingress database
     */
    public static final int TYPE_DATABASE_INGRES      = 18;

    /**
     * Connection to a Borland Interbase database
     */
    public static final int TYPE_DATABASE_INTERBASE   = 19;

    /**
    * Connection to an ExtenDB database
    */
    public static final int TYPE_DATABASE_EXTENDB     = 20;
    
    /**
     * Connection to a Teradata database
     */
     public static final int TYPE_DATABASE_TERADATA   = 21;
     
     /**
      * Connection to an Oracle RDB database
      */
     public static final int TYPE_DATABASE_ORACLE_RDB = 22;
     
     /**
      * Connection to an H2 database
      */
     public static final int TYPE_DATABASE_H2         = 23;
     
 	/**
 	 * Connection to a Netezza database
 	 */
 	public static final int TYPE_DATABASE_NETEZZA     =  24;

    /**
     * Connection to an IBM UniVerse database
     */
    public static final int TYPE_DATABASE_UNIVERSE    =  25;

    /**
     * Connection to a SQLite database
     */
    public static final int TYPE_DATABASE_SQLITE      =  26;



	/**
	 * Connect natively through JDBC thin driver to the database.
	 */
	public static final int TYPE_ACCESS_NATIVE        =  0;
	
	/**
	 * Connect to the database using ODBC.
	 */
	public static final int TYPE_ACCESS_ODBC          =  1;
	
	/**
	 * Connect to the database using OCI. (Oracle only)
	 */
	public static final int TYPE_ACCESS_OCI           =  2;

    /**
     * Connect to the database using plugin specific method. (SAP R/3)
     */
    public static final int TYPE_ACCESS_PLUGIN        =  3;
    
    /**
     * Connect to the database using JNDI.
     */
    public static final int TYPE_ACCESS_JNDI        =  4;
    
    
	/**
	 * Short description of the access type, used in XML and the repository.
	 */
	public static final String dbAccessTypeCode[] = 
		{
		"Native",
			"ODBC",
			"OCI",
            "Plugin",
			"JNDI"
		};

	/**
	 * Longer description for user interactions.
	 */
	public static final String dbAccessTypeDesc[] = 
		{
			"Native (JDBC)",
			"ODBC",
			"OCI",
            "Plugin specific access method",
			"JNDI"
		};

	/**
	 * Use this length in a String value to indicate that you want to use a CLOB in stead of a normal text field.
	 */
	public static final int CLOB_LENGTH = 9999999;
	
    /**
     * The value to store in the attributes so that an empty value doesn't get lost...
     */
    public static final String EMPTY_OPTIONS_STRING = "><EMPTY><";
        
	/**
	 * Construct a new database connections.  Note that not all these parameters are not allways mandatory.
	 * 
	 * @param name The database name
	 * @param type The type of database
	 * @param access The type of database access
	 * @param host The hostname or IP address
	 * @param db The database name
	 * @param port The port on which the database listens.
	 * @param user The username
	 * @param pass The password
	 */
	public DatabaseMeta(String name, String type, String access, String host, String db, String port, String user, String pass)
	{
		setValues(name, type, access, host, db, port, user, pass);
        addOptions();
	}
	
	/**
	 * Create an empty database connection
	 *
	 */
	public DatabaseMeta()
	{
 		setDefault();
        addOptions();
	}
	
	/**
	 * Set default values for an Oracle database.
	 *
	 */
	public void setDefault()
	{
		setValues("", "Oracle", "Native", "", "", "1521", "", "");
	}
    
    /**
     * Add a list of common options for some databases.
     *
     */
    public void addOptions()
    {
        String mySQL = new MySQLDatabaseMeta().getDatabaseTypeDesc();
        
        addExtraOption(mySQL, "defaultFetchSize", "500");
        addExtraOption(mySQL, "useCursorFetch", "true");
    }
	
	/**
     * @return the system dependend database interface for this database metadata definition
	 */
    public DatabaseInterface getDatabaseInterface()
    {
        return databaseInterface;
    }
    
    /**
     * Set the system dependend database interface for this database metadata definition
     * @param databaseInterface the system dependend database interface
     */
    public void setDatabaseInterface(DatabaseInterface databaseInterface)
    {
        this.databaseInterface = databaseInterface;
    }
    
	/**
	 * Search for the right type of DatabaseInterface object and clone it.
	 * 
	 * @param databaseType the type of DatabaseInterface to look for (description)
	 * @return The requested DatabaseInterface
	 * 
	 * @throws KettleDatabaseException when the type could not be found or referenced.
	 */
	private static final DatabaseInterface getDatabaseInterface(String databaseType) throws KettleDatabaseException
	{
		return (DatabaseInterface)findDatabaseInterface(databaseType).clone();
	}
	
	/**
	 * Search for the right type of DatabaseInterface object and return it.
	 * 
	 * @param databaseType the type of DatabaseInterface to look for (description)
	 * @return The requested DatabaseInterface
	 * 
	 * @throws KettleDatabaseException when the type could not be found or referenced.
	 */
	private static final DatabaseInterface findDatabaseInterface(String databaseTypeDesc) throws KettleDatabaseException
	{
		DatabaseInterface di[] = getDatabaseInterfaces();
		for (int i=0;i<di.length;i++)
		{
			if (di[i].getDatabaseTypeDesc().equalsIgnoreCase(databaseTypeDesc) ||
				di[i].getDatabaseTypeDescLong().equalsIgnoreCase(databaseTypeDesc)
				) return di[i];
		}
		
		throw new KettleDatabaseException("database type ["+databaseTypeDesc+"] couldn't be found!");
	}

	/**
	 *  Load the Database Info 
	 */
	public DatabaseMeta(Repository rep, long id_database) throws KettleException
	{
        this();
        
		try
		{
			Row r = rep.getDatabase(id_database);
			
			if (r!=null)
			{
				long id_database_type    = r.getInteger("ID_DATABASE_TYPE", 0); // con_type
				String dbTypeDesc = rep.getDatabaseTypeCode(id_database_type);
				if (dbTypeDesc!=null)
				{
					databaseInterface = getDatabaseInterface(dbTypeDesc);
                    setAttributes(new Properties()); // new attributes
				}
				else
				{
					throw new KettleException("No database type was specified [id_database_type="+id_database_type+"]");
				}

				setID(id_database);
				setName( r.getString("NAME", "") );

				long id_database_contype = r.getInteger("ID_DATABASE_CONTYPE", 0); // con_access 
				setAccessType( getAccessType( rep.getDatabaseConTypeCode( id_database_contype)) );

				setHostname( r.getString("HOST_NAME", "") );
				setDBName( r.getString("DATABASE_NAME", "") );
				setDBPort( r.getString("PORT", "") );
				setUsername( r.getString("USERNAME", "") );
				setPassword( Encr.decryptPasswordOptionallyEncrypted( r.getString("PASSWORD", "") ) );
				setServername( r.getString("SERVERNAME", "") );
				setDataTablespace( r.getString("DATA_TBS", "") );
				setIndexTablespace( r.getString("INDEX_TBS", "") );
                
                // Also, load all the properties we can find...
				long ids[] = rep.getDatabaseAttributeIDs(id_database);
                for (int i=0;i<ids.length;i++)
                {
                    Row row = rep.getDatabaseAttribute(ids[i]);
                    String code = row.getString("CODE", "");
                    String attribute = row.getString("VALUE_STR", "");
                    // System.out.println("Attributes: "+(getAttributes()!=null)+", code: "+(code!=null)+", attribute: "+(attribute!=null));
                    getAttributes().put(code, Const.NVL(attribute, ""));
                }
			}
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Error loading database connection from repository (id_database="+id_database+")", dbe);
		}
	}
	
	/**
	 * Saves the database information into a given repository.
	 * 
	 * @param rep The repository to save the database into.
	 * 
	 * @throws KettleException if an error occurs.
	 */
	public void saveRep(Repository rep) throws KettleException
	{
		try
		{
            // If we don't have an ID, we don't know which entry in the database we need to update.
			// See if a database with the same name is already available...
			if (getID()<=0)
			{
				setID(rep.getDatabaseID(getName()));
			}
			
			// Still not found? --> Insert
			if (getID()<=0)
			{
				// Insert new Note in repository
				setID(rep.insertDatabase(	getName(), 
											getDatabaseTypeCode(getDatabaseType()), 
											getAccessTypeDesc(getAccessType()), 
											getHostname(), 
											getDatabaseName(), 
											getDatabasePortNumberString(), 
											getUsername(), 
											getPassword(),
											getServername(),
											getDataTablespace(),
											getIndexTablespace()
										)
					); 
			}
			else // --> found entry with the same name...
			{
				// Update the note...
				rep.updateDatabase(	getID(),
											getName(), 
											getDatabaseTypeCode(getDatabaseType()), 
											getAccessTypeDesc(getAccessType()), 
											getHostname(), 
											getDatabaseName(), 
											getDatabasePortNumberString(), 
											getUsername(), 
											getPassword(),
											getServername(),
											getDataTablespace(),
											getIndexTablespace()
										);
			}
            
            // For the extra attributes, just delete them and re-add them.
            rep.delDatabaseAttributes(getID());
            
            // OK, now get a list of all the attributes set on the database connection...
            // 
            Properties attributes = getAttributes();
            Enumeration keys = getAttributes().keys();
            while (keys.hasMoreElements())
            {
                String code = (String) keys.nextElement();
                String attribute = (String)attributes.get(code);
                
                // Save this attribute
                rep.insertDatabaseAttribute(getID(), code, attribute);
            }
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Error saving database connection or one of its attributes to the repository.", dbe);
		}
	}

	/**
	 * Returns the database ID of this database connection if a repository was used before.
	 * 
	 * @return the ID of the db connection.
	 */
	public long getID()
	{
		return databaseInterface.getId();
	}
	
	public void setID(long id)
	{
		databaseInterface.setId(id);
	}

	public Object clone()
	{
        DatabaseMeta databaseMeta = new DatabaseMeta();
        databaseMeta.replaceMeta(this);
        databaseMeta.setID(-1L);
		return databaseMeta; 
	}


    public void replaceMeta(DatabaseMeta databaseMeta)
    {
        this.setValues(databaseMeta.getName(), databaseMeta.getDatabaseTypeDesc(), databaseMeta.getAccessTypeDesc(), 
                databaseMeta.getHostname(), databaseMeta.getDatabaseName(), databaseMeta.getDatabasePortNumberString(), 
                databaseMeta.getUsername(), databaseMeta.getPassword()
                );
        this.setServername(databaseMeta.getServername());
        this.setDataTablespace( databaseMeta.getDataTablespace() );
        this.setIndexTablespace( databaseMeta.getIndexTablespace() );
    
        this.databaseInterface = (DatabaseInterface) databaseMeta.databaseInterface.clone();
        
        this.setID(databaseMeta.getID());
        this.setChanged();
    }
    
	public void setValues(String name, String type, String access, String host, String db, String port, String user, String pass)
	{
		try
		{
			databaseInterface = getDatabaseInterface(type);
		}
		catch(KettleDatabaseException kde)
		{
			throw new RuntimeException("Database type not found!", kde);
		}
		
		setName(name);
		setAccessType(getAccessType(access));
		setHostname(host);
		setDBName(db);
		setDBPort(port);
		setUsername(user);
		setPassword(pass);
		setServername(null);
		setChanged(false);
	}
	
	public void setDatabaseType(String type)
	{
		DatabaseInterface oldInterface = databaseInterface;
		
		try
		{
			databaseInterface = getDatabaseInterface(type);
		}
		catch(KettleDatabaseException kde)
		{
			throw new RuntimeException("Database type ["+type+"] not found!", kde);
		}
		
		setName(oldInterface.getName());
		setAccessType(oldInterface.getAccessType());
		setHostname(oldInterface.getHostname());
		setDBName(oldInterface.getDatabaseName());
		setDBPort(oldInterface.getDatabasePortNumberString());
		setUsername(oldInterface.getUsername());
		setPassword(oldInterface.getPassword());
		setServername(oldInterface.getServername());
		setDataTablespace(oldInterface.getDataTablespace());
		setIndexTablespace(oldInterface.getIndexTablespace());
		setChanged(oldInterface.isChanged());
	}

	public void setValues(DatabaseMeta info)
	{
		databaseInterface = (DatabaseInterface)info.databaseInterface.clone();
	}

	/**
	 * Sets the name of the database connection.  This name should be
	 * unique in a transformation and in general in a single repository.
	 * 
	 * @param name The name of the database connection
	 */
	public void setName(String name)
	{
		databaseInterface.setName(name);
	}
	
	/**
	 * Returns the name of the database connection
	 * @return The name of the database connection
	 */
	public String getName()
	{
		return databaseInterface.getName();
	}

	/**
	 * Returns the type of database, one of <p>
	 *     TYPE_DATABASE_MYSQL<p>
	 *     TYPE_DATABASE_ORACLE<p>
	 *     TYPE_DATABASE_...<p>
	 * 
	 * @return the database type
	 */
	public int getDatabaseType()
	{
		return databaseInterface.getDatabaseType();
	}
	
	/*
	 * Sets the type of database.
	 * @param db_type The database type
	public void setDatabaseType(int db_type)
	{
		databaseInterface
		this.databaseType = db_type;
	}
   */
	
	/**
	 * Return the type of database access. One of <p>
	 *      TYPE_ACCESS_NATIVE<p>
	 * 		TYPE_ACCESS_ODBC<p>
	 * 		TYPE_ACCESS_OCI<p>
	 * @return The type of database access.
	 */
	public int getAccessType()
	{
		return databaseInterface.getAccessType();
	}
	
	/**
	 * Set the type of database access.
	 * @param access_type The access type.
	 */
	public void setAccessType(int access_type)
	{
		databaseInterface.setAccessType(access_type);
	}
	
	/**
	 * Returns a short description of the type of database.
	 * @return A short description of the type of database.
	 */
	public String getDatabaseTypeDesc()
	{
		return databaseInterface.getDatabaseTypeDesc();
	}

	/**
	 * Gets you a short description of the type of database access.
	 * @return A short description of the type of database access.
	 */
	public String getAccessTypeDesc()
	{
		return dbAccessTypeCode[getAccessType()];
	}

	/**
	 * Return the hostname of the machine on which the database runs.
	 * @return The hostname of the database.
	 */
	public String getHostname()
	{
		return databaseInterface.getHostname();
	}
	
	/**
	 * Sets the hostname of the machine on which the database runs.
	 * @param hostname The hostname of the machine on which the database runs.
	 */
	public void setHostname(String hostname)
	{
		databaseInterface.setHostname(hostname);
	}
	
	/**
	 * Return the port on which the database listens as a String. Allows for parameterisation.
	 * @return The database port.
	 */
	public String getDatabasePortNumberString()
	{
		return databaseInterface.getDatabasePortNumberString();
	}
	
	/**
	 * Sets the port on which the database listens.
	 * 
	 * @param db_port The port number on which the database listens
	 */
	public void setDBPort(String db_port)
	{
		databaseInterface.setDatabasePortNumberString(db_port);
	}
	
	/**
	 * Return the name of the database.
	 * @return The database name.
	 */
	public String getDatabaseName()
	{
		return databaseInterface.getDatabaseName();
	}
	
	/**
	 * Set the name of the database.
	 * @param databaseName The new name of the database
	 */
	public void setDBName(String databaseName)
	{
		databaseInterface.setDatabaseName(databaseName);
	}

	/**
	 * Get the username to log into the database on this connection.
	 * @return The username to log into the database on this connection.
	 */
	public String getUsername()
	{
		return databaseInterface.getUsername();
	}
	
	/**
	 * Sets the username to log into the database on this connection.
	 * @param username The username
	 */
	public void setUsername(String username)
	{
		databaseInterface.setUsername(username);
	}

	/**
	 * Get the password to log into the database on this connection.
	 * @return the password to log into the database on this connection.
	 */
	public String getPassword()
	{
		return databaseInterface.getPassword();
	}
	
	/**
	 * Sets the password to log into the database on this connection.
	 * @param password the password to log into the database on this connection.
	 */
	public void setPassword(String password)
	{
		databaseInterface.setPassword(password);
	}
	
	/**
	 * @param servername the Informix servername
	 */
	public void setServername(String servername)
	{
		databaseInterface.setServername(servername);
	}
	
	/**
	 * @return the Informix servername
	 */
	public String getServername()
	{
		return databaseInterface.getServername();
	}
	
	public String getDataTablespace()
	{
		return databaseInterface.getDataTablespace();
	}
	
	public void setDataTablespace(String data_tablespace)
	{
		databaseInterface.setDataTablespace(data_tablespace);
	}
	
	public String getIndexTablespace()
	{
		return databaseInterface.getIndexTablespace();
	}

	public void setIndexTablespace(String index_tablespace)
	{
		databaseInterface.setIndexTablespace(index_tablespace);
	}
	
	public void setChanged()
	{
		setChanged(true);
	}
	
	public void setChanged(boolean ch)
	{
		databaseInterface.setChanged(ch);
	}
	
	public boolean hasChanged()
	{
		return databaseInterface.isChanged();
	}
	
	public String toString()
	{
		return getName();
	}

    /**
    * @return The extra attributes for this database connection
    */
   public Properties getAttributes()
   {
       return databaseInterface.getAttributes();
   }
   
   /**
    * Set extra attributes on this database connection
    * @param attributes The extra attributes to set on this database connection.
    */
   public void setAttributes(Properties attributes)
   {
       databaseInterface.setAttributes(attributes);
   }

	
	/**
	 * Constructs a new database using an XML string snippet.
	 * It expects the snippet to be enclosed in <code>connection</code> tags.
	 * @param xml The XML string to parse
	 * @throws KettleXMLException
	 */
	public DatabaseMeta(String xml)
		throws KettleXMLException
	{
		this( XMLHandler.getSubNode(XMLHandler.loadXMLString(xml), "connection") );
	}
	
	/**
	 * Reads the information from an XML Node into this new database connection.
	 * @param con The Node to read the data from
	 * @throws KettleXMLException
	 */
	public DatabaseMeta(Node con) throws KettleXMLException
	{
        this();
        
		try
		{
			String type = XMLHandler.getTagValue(con, "type");
			try
			{
				databaseInterface = getDatabaseInterface(type);

			}
			catch(KettleDatabaseException kde)
			{
				throw new KettleXMLException("Unable to create new database interface", kde);
			}
			
			setName( XMLHandler.getTagValue(con, "name") );
			setHostname( XMLHandler.getTagValue(con, "server") );
			String acc  = XMLHandler.getTagValue(con, "access");
			setAccessType( getAccessType(acc) );

			setDBName( XMLHandler.getTagValue(con, "database") );
			setDBPort( XMLHandler.getTagValue(con, "port") );
			setUsername( XMLHandler.getTagValue(con, "username") );
			setPassword( Encr.decryptPasswordOptionallyEncrypted( XMLHandler.getTagValue(con, "password") ) );
			setServername( XMLHandler.getTagValue(con, "servername") );
			setDataTablespace( XMLHandler.getTagValue(con, "data_tablespace") );
			setIndexTablespace( XMLHandler.getTagValue(con, "index_tablespace") );
				
            // Also, read the database attributes...
            Node attrsnode = XMLHandler.getSubNode(con, "attributes");
            if (attrsnode!=null)
            {
                int nr = XMLHandler.countNodes(attrsnode, "attribute");
                for (int i=0;i<nr;i++)
                {
                    Node attrnode = XMLHandler.getSubNodeByNr(attrsnode, "attribute", i);
                    String code      = XMLHandler.getTagValue(attrnode, "code");
                    String attribute = XMLHandler.getTagValue(attrnode, "attribute");
                    if (code!=null && attribute!=null) getAttributes().put(code, attribute);
                }
            }
		}
		catch(Exception e)
		{
			throw new KettleXMLException("Unable to load database connection info from XML node", e);
		}
	}
	
	public String getXML()
	{
        StringBuffer retval = new StringBuffer(250);
		
		retval.append("  <").append(XML_TAG).append('>').append(Const.CR);
		retval.append("    ").append(XMLHandler.addTagValue("name",       getName()));
		retval.append("    ").append(XMLHandler.addTagValue("server",     getHostname()));
		retval.append("    ").append(XMLHandler.addTagValue("type",       getDatabaseTypeDesc()));
		retval.append("    ").append(XMLHandler.addTagValue("access",     getAccessTypeDesc()));
		retval.append("    ").append(XMLHandler.addTagValue("database",   getDatabaseName()));
		retval.append("    ").append(XMLHandler.addTagValue("port",       getDatabasePortNumberString()));
		retval.append("    ").append(XMLHandler.addTagValue("username",   getUsername()));
        
        retval.append("    ").append(XMLHandler.addTagValue("password",         Encr.encryptPasswordIfNotUsingVariables(getPassword())));	
		retval.append("    ").append(XMLHandler.addTagValue("servername",       getServername()));
		retval.append("    ").append(XMLHandler.addTagValue("data_tablespace",  getDataTablespace()));
		retval.append("    ").append(XMLHandler.addTagValue("index_tablespace", getIndexTablespace()));
        
        retval.append("    <attributes>").append(Const.CR);
        List list = new ArrayList( getAttributes().keySet() );
        Collections.sort(list);  // Sort the entry-sets to make sure we can compare XML strings: if the order is different, the XML is different.  
        
        for (Iterator iter = list.iterator(); iter.hasNext();)
        {
            String code = (String) iter.next();
            String attribute = (String) getAttributes().getProperty(code);
            if (!Const.isEmpty(attribute))
            {
                retval.append("      <attribute>"+
                                    XMLHandler.addTagValue("code", code, false)+
                                    XMLHandler.addTagValue("attribute", attribute, false)+
                               "</attribute>"+Const.CR);
            }
        }
        retval.append("    </attributes>").append(Const.CR);
        
		retval.append("  </"+XML_TAG+">").append(Const.CR);
		return retval.toString();
	}
	
	public int hashCode()
	{
		return getName().hashCode(); // name of connection is unique!
	}
	
	public boolean equals(Object obj)
	{
		return getName().equals( ((DatabaseMeta)obj).getName() );
	}

    public String getURL() throws KettleDatabaseException
    {
        return getURL(null);
    }

	public String getURL(String partitionId) throws KettleDatabaseException
	{
        String baseUrl;
        if (isPartitioned() && !Const.isEmpty(partitionId))
        {
            // Get the cluster information...
            PartitionDatabaseMeta partition = getPartitionMeta(partitionId);
            String hostname = partition.getHostname();
            String port = partition.getPort();
            String databaseName = partition.getDatabaseName();
            
            baseUrl = databaseInterface.getURL(hostname, port, databaseName);
        }
        else
        {
            baseUrl = databaseInterface.getURL(getHostname(), getDatabasePortNumberString(), getDatabaseName());
        }
		StringBuffer url=new StringBuffer( baseUrl );
        
        if (databaseInterface.supportsOptionsInURL())
        {
            // OK, now add all the options...
            String optionIndicator = getExtraOptionIndicator();
            String optionSeparator = getExtraOptionSeparator();
            String valueSeparator = getExtraOptionValueSeparator();
            
            Map map = getExtraOptions();
            if (map.size()>0)
            {
                Iterator iterator = map.keySet().iterator();
                boolean first=true;
                while (iterator.hasNext())
                {
                    String typedParameter=(String)iterator.next();
                    int dotIndex = typedParameter.indexOf('.');
                    if (dotIndex>=0)
                    {
                        String typeCode = typedParameter.substring(0,dotIndex);
                        String parameter = typedParameter.substring(dotIndex+1);
                        String value = (String) map.get(typedParameter);
                        
                        // Only add to the URL if it's the same database type code...
                        //
                        if (databaseInterface.getDatabaseTypeDesc().equals(typeCode))
                        {
                            if (first) url.append(optionIndicator);
                            else url.append(optionSeparator);

                            url.append(parameter);
                            if (!Const.isEmpty(value) && !value.equals(EMPTY_OPTIONS_STRING))
                            {
                                url.append(valueSeparator).append(value);
                            }
                            first=false;
                        }
                    }
                }
            }
        }
        else
        {
            // We need to put all these options in a Properties file later (Oracle & Co.)
            // This happens at connect time...
        }
        
        return url.toString();
	}
    

    public Properties getConnectionProperties()
    {
        Properties properties =new Properties();
        
        Map map = getExtraOptions();
        if (map.size()>0)
        {
            Iterator iterator = map.keySet().iterator();
            while (iterator.hasNext())
            {
                String typedParameter=(String)iterator.next();
                int dotIndex = typedParameter.indexOf('.');
                if (dotIndex>=0)
                {
                    String typeCode = typedParameter.substring(0,dotIndex);
                    String parameter = typedParameter.substring(dotIndex+1);
                    String value = (String) map.get(typedParameter);
                    
                    // Only add to the URL if it's the same database type code...
                    //
                    if (databaseInterface.getDatabaseTypeDesc().equals(typeCode))
                    {
                        if (value!=null && value.equals(EMPTY_OPTIONS_STRING)) value="";
                        properties.put(parameter, StringUtil.environmentSubstitute(Const.NVL(value, "")));
                    }
                }
            }
        }
        
        return properties;
    }
    

    public String getExtraOptionIndicator()
    {
        return databaseInterface.getExtraOptionIndicator();
    }
    
    /**
     * @return The extra option separator in database URL for this platform (usually this is semicolon ; ) 
     */
	public String getExtraOptionSeparator()
    {
        return databaseInterface.getExtraOptionSeparator();
    }
    
    /**
     * @return The extra option value separator in database URL for this platform (usually this is the equal sign = ) 
     */
    public String getExtraOptionValueSeparator()
    {
        return databaseInterface.getExtraOptionValueSeparator();
    }

    /**
     * Add an extra option to the attributes list
     * @param databaseTypeCode The database type code for which the option applies
     * @param option The option to set
     * @param value The value of the option
     */
    public void addExtraOption(String databaseTypeCode, String option, String value)
    {
        databaseInterface.addExtraOption(databaseTypeCode, option, value);
    }
    
    /**
	 * @deprecated because the same database can support transactions or not.  It all depends on the database setup.  Therefor, we look at the database metadata
	 * DatabaseMetaData.supportsTransactions() in stead of this.
	 * @return true if the database supports transactions
	 */
	public boolean supportsTransactions()
	{
		return databaseInterface.supportsTransactions();
	}
	
	public boolean supportsAutoinc()
	{
		return databaseInterface.supportsAutoInc();
	}

	public boolean supportsSequences()
	{
		return databaseInterface.supportsSequences();
	}

    public String getSQLSequenceExists(String sequenceName)
    {
        return databaseInterface.getSQLSequenceExists(sequenceName);
    }

	public boolean supportsBitmapIndex()
	{
		return databaseInterface.supportsBitmapIndex();
	}
	
	public boolean supportsSetLong()
	{
		return databaseInterface.supportsSetLong();
	}

	/**
	 * @return true if the database supports schemas
	 */
	public boolean supportsSchemas()
	{
		return databaseInterface.supportsSchemas();
	}
	
    /**
     * @return true if the database supports catalogs
     */
    public boolean supportsCatalogs()
    {
        return databaseInterface.supportsCatalogs();
    }

	/**
	 * 
	 * @return true when the database engine supports empty transaction.
	 * (for example Informix does not on a non-ANSI database type!)
	 */
	public boolean supportsEmptyTransactions()
	{
		return databaseInterface.supportsEmptyTransactions();
	}

	/**
	 * See if this database supports the setCharacterStream() method on a PreparedStatement.
	 * 
	 * @return true if we can set a Stream on a field in a PreparedStatement.  False if not. 
	 */
	public boolean supportsSetCharacterStream()
	{
		return databaseInterface.supportsSetCharacterStream(); 
	}
	
	/**
	 * Get the maximum length of a text field for this database connection.
	 * This includes optional CLOB, Memo and Text fields. (the maximum!)
	 * @return The maximum text field length for this database type. (mostly CLOB_LENGTH)
	 */
	public int getMaxTextFieldLength()
	{
		return databaseInterface.getMaxTextFieldLength();
	}
	
	public final static int getDatabaseType(String dbTypeDesc)
	{ 
		// Backward compatibility...
		if (dbTypeDesc.equalsIgnoreCase("ODBC-ACCESS")) return TYPE_DATABASE_ACCESS;

		try
		{
			DatabaseInterface di = getDatabaseInterface(dbTypeDesc);
			return di.getDatabaseType();
		}
		catch(KettleDatabaseException kde)
		{
			return TYPE_DATABASE_NONE;
		}
	}

    /**
     * Get a string representing the unqiue database type code
     * @param dbtype the database type to get the code of
     * @return The database type code
     * @deprecated please use getDatabaseTypeCode()
     */
    public final static String getDBTypeDesc(int dbtype)
    {
        return getDatabaseTypeCode(dbtype);
    }
    
    /**
     * Get a string representing the unqiue database type code
     * @param dbtype the database type to get the code of
     * @return The database type code
     */
 	public final static String getDatabaseTypeCode(int dbtype)
	{
		// Find the DatabaseInterface for this type...
		DatabaseInterface[] di = getDatabaseInterfaces();
		
		for (int i=0;i<di.length;i++)
		{
			if (di[i].getDatabaseType() == dbtype) return di[i].getDatabaseTypeDesc();
		}
		
		return null;
	}

    /**
     * Get a description of the database type
     * @param dbtype the database type to get the description for
     * @return The database type description
     */
     public final static String getDatabaseTypeDesc(int dbtype)
    {
        // Find the DatabaseInterface for this type...
        DatabaseInterface[] di = getDatabaseInterfaces();
        
        for (int i=0;i<di.length;i++)
        {
            if (di[i].getDatabaseType() == dbtype) return di[i].getDatabaseTypeDescLong();
        }
        
        return null;
    }

	public final static int getAccessType(String dbaccess)
	{ 
		int i;
		
		if (dbaccess==null) return TYPE_ACCESS_NATIVE;
		
		for (i=0;i<dbAccessTypeCode.length;i++)
		{
			if (dbAccessTypeCode[i].equalsIgnoreCase(dbaccess))
			{
				return i;
			}
		}
		for (i=0;i<dbAccessTypeDesc.length;i++)
		{
			if (dbAccessTypeDesc[i].equalsIgnoreCase(dbaccess))
			{
				return i;
			}
		}
		
		return TYPE_ACCESS_NATIVE;
	}

	public final static String getAccessTypeDesc(int dbaccess)
	{ 
		if (dbaccess<0) return null;
		if (dbaccess>dbAccessTypeCode.length) return null;
		
		return dbAccessTypeCode[dbaccess];
	}

	public final static String getAccessTypeDescLong(int dbaccess)
	{ 
		if (dbaccess<0) return null;
		if (dbaccess>dbAccessTypeDesc.length) return null;
		
		return dbAccessTypeDesc[dbaccess];
	}
	
	public final static String[] getDBTypeDescLongList()
	{
		DatabaseInterface[] di = getDatabaseInterfaces();
		
		String[] retval = new String[di.length];
		for (int i=0;i<di.length;i++)
		{
			retval[i] = di[i].getDatabaseTypeDescLong();
		}
		
		return retval;
	}

	public final static String[] getDBTypeDescList()
	{
		DatabaseInterface[] di = getDatabaseInterfaces();
		
		String[] retval = new String[di.length];
		for (int i=0;i<di.length;i++)
		{
			retval[i] = di[i].getDatabaseTypeDesc();
		}
		
		return retval;
	}
	
	public static final DatabaseInterface[] getDatabaseInterfaces()
	{
		if (allDatabaseInterfaces!=null) return allDatabaseInterfaces;
		
		Class ic[] = DatabaseInterface.implementingClasses;
		allDatabaseInterfaces = new DatabaseInterface[ic.length];
		for (int i=0;i<ic.length;i++)
		{
			try
			{
				Class.forName(ic[i].getName());
				allDatabaseInterfaces[i] = (DatabaseInterface)ic[i].newInstance();
			}
			catch(Exception e)
			{
				throw new RuntimeException("Error creating class for : "+ic[i].getName(), e);
			}
		}
		return allDatabaseInterfaces;
	}

	public final static int[] getAccessTypeList(String dbTypeDesc)
	{
		try
		{
			DatabaseInterface di = findDatabaseInterface(dbTypeDesc);
			return di.getAccessTypeList();
		}
		catch(KettleDatabaseException kde)
		{
			return null;
		}
	}

	public static final int getPortForDBType(String strtype, String straccess)
	{
		try
		{
			DatabaseInterface di = getDatabaseInterface(strtype);
			di.setAccessType(getAccessType(straccess));
			return di.getDefaultDatabasePort();
		}
		catch(KettleDatabaseException kde)
		{
			return -1;
		}
	}
	
	public int getDefaultDatabasePort()
	{
		return databaseInterface.getDefaultDatabasePort();
	}
	
	public int getNotFoundTK(boolean use_autoinc)
	{
		return databaseInterface.getNotFoundTK(use_autoinc);
	}
	
	public String getDriverClass()
	{
		return databaseInterface.getDriverClass();
	}
	
	public String stripCR(String sbsql)
	{
		return stripCR(new StringBuffer(sbsql));
	}
	
	public String stripCR(StringBuffer sbsql)
	{
		// DB2 Can't handle \n in SQL Statements...
		if (getDatabaseType() == DatabaseMeta.TYPE_DATABASE_DB2 ||
			getDatabaseType() == DatabaseMeta.TYPE_DATABASE_CACHE ||
            getDatabaseType() == DatabaseMeta.TYPE_DATABASE_UNIVERSE
		   )
		{
			// Remove CR's
			for (int i=sbsql.length()-1;i>=0;i--)
			{
				if (sbsql.charAt(i)=='\n' || sbsql.charAt(i)=='\r') sbsql.setCharAt(i, ' ');
			}
		}
		
		return sbsql.toString();
	}

	public String getSeqNextvalSQL(String sequenceName)
	{
		return databaseInterface.getSQLNextSequenceValue(sequenceName);
	}
    
    public String getSQLCurrentSequenceValue(String sequenceName)
    {
        return databaseInterface.getSQLCurrentSequenceValue(sequenceName);
    }

	
	public boolean isFetchSizeSupported()
	{
		return databaseInterface.isFetchSizeSupported();
	}
	
	/**
	 * Indicates the need to insert a placeholder (0) for auto increment fields.
	 * @return true if we need a placeholder for auto increment fields in insert statements.
	 */
	public boolean needsPlaceHolder()
	{
		return databaseInterface.needsPlaceHolder();
	}
	
	public String getFunctionSum()
    {
		return databaseInterface.getFunctionSum();
	}

	public String getFunctionAverage()
	{
		return databaseInterface.getFunctionAverage();
	}

	public String getFunctionMaximum()
	{
		return databaseInterface.getFunctionMaximum();
	}
	
	public String getFunctionMinimum()
	{
		return databaseInterface.getFunctionMinimum();
	}

	public String getFunctionCount()
	{
		return databaseInterface.getFunctionCount();
	}
	
    /**
     * Check the database connection parameters and give back an array of remarks
     * @return an array of remarks Strings
     */
	public String[] checkParameters()
	{
        ArrayList remarks = new ArrayList();
        
		if (getDatabaseType()==TYPE_DATABASE_NONE) 
        {
            remarks.add("No database type was choosen");
        }
        
		if (getName()==null || getName().length()==0) 
        {
            remarks.add("Please give this database connection a name");
        }
        
        if (!isPartitioned() && getDatabaseType()!=TYPE_DATABASE_SAPR3 && getDatabaseType()!=TYPE_DATABASE_GENERIC)
        {
            if (getDatabaseName()==null || getDatabaseName().length()==0) 
            {
                remarks.add("Please specify the name of the database");
            }
        }
		
		return (String[])remarks.toArray(new String[remarks.size()]);
	}
	
    /**
     * Calculate the schema-table combination, usually this is the schema and table separated with a dot. (schema.table)
     * @param schemaName the schema-name or null if no schema is used.
     * @param tableName the table name
     * @return the schemaname-tablename combination
     */
	public String getSchemaTableCombination(String schemaName, String tableName)
	{
        if (Const.isEmpty(schemaName)) return tableName; // no need to look further
		return databaseInterface.getSchemaTableCombination(schemaName, tableName);
	}


	public boolean isClob(Value v)
	{
		boolean retval=true;
		
		if (v==null || v.getLength()<DatabaseMeta.CLOB_LENGTH)
		{
			retval=false;
		}
		else
		{
			return true;
		}
		return retval;
	}
	
	public String getFieldDefinition(Value v, String tk, String pk, boolean use_autoinc)
	{
		return getFieldDefinition(v, tk, pk, use_autoinc, true, true);
	}

	public String getFieldDefinition(Value v, String tk, String pk, boolean use_autoinc, boolean add_fieldname, boolean add_cr)
	{
		return databaseInterface.getFieldDefinition(v, tk, pk, use_autoinc, add_fieldname, add_cr);
	}

	public String getLimitClause(int nrRows)
	{
		return databaseInterface.getLimitClause(nrRows);
	}
	
    /**
     * @param tableName The table or schema-table combination.  We expect this to be quoted properly already!
     * @return the SQL for to get the fields of this table.
     */
	public String getSQLQueryFields(String tableName)
	{
	    return databaseInterface.getSQLQueryFields(tableName);
	}
	
	public String getAddColumnStatement(String tablename, Value v, String tk, boolean use_autoinc, String pk, boolean semicolon)
	{
		String retval = databaseInterface.getAddColumnStatement(tablename, v, tk, use_autoinc, pk, semicolon);
		retval+=Const.CR;
		if (semicolon) retval+=";"+Const.CR;
		return retval;
	}

	public String getDropColumnStatement(String tablename, Value v, String tk, boolean use_autoinc, String pk, boolean semicolon)
	{
		String retval = databaseInterface.getDropColumnStatement(tablename, v, tk, use_autoinc, pk, semicolon);
		retval+=Const.CR;
		if (semicolon) retval+=";"+Const.CR;
		return retval;
	}
	
	public String getModifyColumnStatement(String tablename, Value v, String tk, boolean use_autoinc, String pk, boolean semicolon)
	{
		String retval = databaseInterface.getModifyColumnStatement(tablename, v, tk, use_autoinc, pk, semicolon);
		retval+=Const.CR;
		if (semicolon) retval+=";"+Const.CR;
		
		return retval;
	}
	
	/**
	 * @return an array of reserved words for the database type...
	 */
	public String[] getReservedWords()
	{
		return databaseInterface.getReservedWords();
	}
	
	/**
	 * @return true if reserved words need to be double quoted ("password", "select", ...)
	 */
	public boolean quoteReservedWords()
	{
		return databaseInterface.quoteReservedWords();
	}
	
	/**
	 * @return The start quote sequence, mostly just double quote, but sometimes [, ...
	 */
	public String getStartQuote()
	{
		return databaseInterface.getStartQuote();
	}
	
	/**
	 * @return The end quote sequence, mostly just double quote, but sometimes ], ...
	 */
	public String getEndQuote()
	{
		return databaseInterface.getEndQuote();
	}
	
    /**
     * Returns a quoted field if this is needed: contains spaces, is a reserved word, ...
     * @param field The fieldname to check for quoting
     * @return The quoted field (if this is needed.
     */
	public String quoteField(String field)
	{
        if (Const.isEmpty(field)) return null;
		if (isReservedWord(field) && quoteReservedWords())
        {
            return handleCase(getStartQuote()+field+getEndQuote());            
        }
        else
        {
            if (hasSpacesInField(field) || hasSpecialCharInField(field) || hasDotInField(field))
            {
                return getStartQuote()+field+getEndQuote();
            }
            else
            {
                return field;
            }
        }
	}
    
    private String handleCase(String field)
    {
        if (databaseInterface.isDefaultingToUppercase()) 
        {
            return field.toUpperCase();
        }
        else
        {
            return field.toLowerCase();
        }
    }
	
    /**
     * Determines whether or not this field is in need of quoting:<br> 
     * - When the fieldname contains spaces<br>
     * - When the fieldname is a reserved word<br>
     * @param fieldname the fieldname to check if there is a need for quoting
     * @return true if the fieldname needs to be quoted.
     */
    public boolean isInNeedOfQuoting(String fieldname)
    {
        return isReservedWord(fieldname) || hasSpacesInField(fieldname);
    }
    
	/**
	 * Returns true if the string specified is a reserved word on this database type.
	 * @param word The word to check
	 * @return true if word is a reserved word on this database.
	 */
	public boolean isReservedWord(String word)
	{
		String reserved[] = getReservedWords();
		if (Const.indexOfString(word, reserved)>=0) return true;
		return false;
	}

    /**
     * Detects if a field has spaces in the name.  We need to quote the field in that case. 
     * @param fieldname The fieldname to check for spaces
     * @return true if the fieldname contains spaces
     */
    public boolean hasSpacesInField(String fieldname)
    {
    		if( fieldname == null ) return false;
    		if (fieldname.indexOf(' ')>=0) return true; 
        return false;
    }
    
    /**
     * Detects if a field has spaces in the name.  We need to quote the field in that case. 
     * @param fieldname The fieldname to check for spaces
     * @return true if the fieldname contains spaces
     */
    public boolean hasSpecialCharInField(String fieldname)
    {
    		if(fieldname==null) return false;
        if (fieldname.indexOf('/')>=0) return true; 
        if (fieldname.indexOf('-')>=0) return true; 
        if (fieldname.indexOf('+')>=0) return true; 
        if (fieldname.indexOf(',')>=0) return true; 
        if (fieldname.indexOf('*')>=0) return true; 
        if (fieldname.indexOf('(')>=0) return true; 
        if (fieldname.indexOf(')')>=0) return true; 
        if (fieldname.indexOf('{')>=0) return true; 
        if (fieldname.indexOf('}')>=0) return true; 
        if (fieldname.indexOf('[')>=0) return true; 
        if (fieldname.indexOf(']')>=0) return true; 
        if (fieldname.indexOf('%')>=0) return true; 
        if (fieldname.indexOf('@')>=0) return true; 
        return false;
    }
    
    public boolean hasDotInField(String fieldname)
    {
		if(fieldname==null) return false;
        if (fieldname.indexOf('.')>=0) return true;
        return false;
    }
    
	/**
	 * Checks the fields specified for reserved words and quotes them.
	 * @param fields the list of fields to check
	 * @return true if one or more values have a name that is a reserved word on this database type.
	 */
	public boolean replaceReservedWords(Row fields)
	{
		boolean hasReservedWords=false;
		for (int i=0;i<fields.size();i++)
		{
			Value v = fields.getValue(i);
			if (isReservedWord(v.getName()))
			{
				hasReservedWords = true;
				v.setName( quoteField(v.getName()) );
			}
		}
		return hasReservedWords;
	}
	
	/**
	 * Checks the fields specified for reserved words
	 * @param fields the list of fields to check
	 * @return The nr of reserved words for this database.
	 */
	public int getNrReservedWords(Row fields)
	{
		int nrReservedWords=0;
		for (int i=0;i<fields.size();i++)
		{
			Value v = fields.getValue(i);
			if (isReservedWord(v.getName())) nrReservedWords++;
		}
		return nrReservedWords;
	}

	/** 
	 * @return a list of types to get the available tables
	 */
	public String[] getTableTypes()
	{
		return databaseInterface.getTableTypes();
	}
	
	/** 
	 * @return a list of types to get the available views
	 */
	public String[] getViewTypes()
	{
		return databaseInterface.getViewTypes();
	}

	/** 
	 * @return a list of types to get the available synonyms
	 */
	public String[] getSynonymTypes()
	{
		return databaseInterface.getSynonymTypes();
	}

	/**
	 * @return true if we need to supply the schema-name to getTables in order to get a correct list of items.
	 */
	public boolean useSchemaNameForTableList()
	{
		return databaseInterface.useSchemaNameForTableList();
	}
	
	/**
	 * @return true if the database supports views
	 */
	public boolean supportsViews()
	{
		return databaseInterface.supportsViews();
	}
	
	/**
	 * @return true if the database supports synonyms
	 */
	public boolean supportsSynonyms()
	{
		return databaseInterface.supportsSynonyms();
	}
	
	/**
	 * 
	 * @return The SQL on this database to get a list of stored procedures.
	 */
	public String getSQLListOfProcedures()
	{
		return databaseInterface.getSQLListOfProcedures();
	}
	
	/**
	 * @param tableName The tablename to be truncated
	 * @return The SQL statement to remove all rows from the specified statement, if possible without using transactions
	 */
	public String getTruncateTableStatement(String schema, String tableName)
	{		
	    return databaseInterface.getTruncateTableStatement(getQuotedSchemaTableCombination(schema, tableName));
	}

    /**
     * @return true if the database rounds floating point numbers to the right precision.
     * For example if the target field is number(7,2) the value 12.399999999 is converted into 12.40
     */
    public boolean supportsFloatRoundingOnUpdate()
    {
        return databaseInterface.supportsFloatRoundingOnUpdate();
    }
    
    /**
     * @param tableNames The names of the tables to lock
     * @return The SQL commands to lock database tables for write purposes.
     *         null is returned in case locking is not supported on the target database.
     */
    public String getSQLLockTables(String tableNames[])
    {
        return databaseInterface.getSQLLockTables(tableNames);
    }
    
    /**
     * @param tableNames The names of the tables to unlock
     * @return The SQL commands to unlock databases tables. 
     *         null is returned in case locking is not supported on the target database.
     */
    public String getSQLUnlockTables(String tableNames[])
    {
        return databaseInterface.getSQLUnlockTables(tableNames);
    }

    
    /**
     * @return a feature list for the choosen database type.
     */
    public List getFeatureSummary()
    {
        ArrayList list = new ArrayList();
        Row r =null;
        final String par = "Parameter";
        final String val = "Value";

        Value testValue =  new Value("FIELD", Value.VALUE_TYPE_STRING);
        testValue.setLength(30);

        if (databaseInterface!=null)
        {
            // Type of database
            r = new Row(); r.addValue(new Value(par, "Database type")); r.addValue(new Value(val, getDatabaseTypeDesc())); list.add(r);
            // Type of access
            r = new Row(); r.addValue(new Value(par, "Access type")); r.addValue(new Value(val, getAccessTypeDesc())); list.add(r);
            // Name of database
            r = new Row(); r.addValue(new Value(par, "Database name")); r.addValue(new Value(val, getDatabaseName())); list.add(r);
            // server hostname
            r = new Row(); r.addValue(new Value(par, "Server hostname")); r.addValue(new Value(val, getHostname())); list.add(r);
            // Port number
            r = new Row(); r.addValue(new Value(par, "Service port")); r.addValue(new Value(val, getDatabasePortNumberString())); list.add(r);
            // Username
            r = new Row(); r.addValue(new Value(par, "Username")); r.addValue(new Value(val, getUsername())); list.add(r);
            // Informix server
            r = new Row(); r.addValue(new Value(par, "Informix server name")); r.addValue(new Value(val, getServername())); list.add(r);
            // Other properties...
            Enumeration keys = getAttributes().keys();
            while (keys.hasMoreElements())
            {
                String key = (String) keys.nextElement();
                String value = getAttributes().getProperty(key);
                r = new Row(); r.addValue(new Value(par, "Extra attribute ["+key+"]")); r.addValue(new Value(val, value)); list.add(r);
            }
            
            // driver class
            r = new Row(); r.addValue(new Value(par, "Driver class")); r.addValue(new Value(val, getDriverClass())); list.add(r);
            // URL
            String pwd = getPassword();
            setPassword("password"); // Don't give away the password in the URL!
            String url = "";
            try { url = getURL(); } catch(KettleDatabaseException e) {}
            r = new Row(); r.addValue(new Value(par, "URL")); r.addValue(new Value(val, url)); list.add(r);
            setPassword(pwd);
            // SQL: Next sequence value
            r = new Row(); r.addValue(new Value(par, "SQL: next sequence value")); r.addValue(new Value(val, getSeqNextvalSQL("SEQUENCE"))); list.add(r);
            // is set fetchsize supported 
            r = new Row(); r.addValue(new Value(par, "supported: set fetch size")); r.addValue(new Value(val, isFetchSizeSupported())); list.add(r);
            // needs placeholder for auto increment 
            r = new Row(); r.addValue(new Value(par, "auto increment field needs placeholder")); r.addValue(new Value(val, needsPlaceHolder())); list.add(r);
            // Sum function 
            r = new Row(); r.addValue(new Value(par, "SUM aggregate function")); r.addValue(new Value(val, getFunctionSum())); list.add(r);
            // Avg function 
            r = new Row(); r.addValue(new Value(par, "AVG aggregate function")); r.addValue(new Value(val, getFunctionAverage())); list.add(r);
            // Minimum function 
            r = new Row(); r.addValue(new Value(par, "MIN aggregate function")); r.addValue(new Value(val, getFunctionMinimum())); list.add(r);
            // Maximum function 
            r = new Row(); r.addValue(new Value(par, "MAX aggregate function")); r.addValue(new Value(val, getFunctionMaximum())); list.add(r);
            // Count function 
            r = new Row(); r.addValue(new Value(par, "COUNT aggregate function")); r.addValue(new Value(val, getFunctionCount())); list.add(r);
            // Schema-table comination
            r = new Row(); r.addValue(new Value(par, "Schema / Table combination")); r.addValue(new Value(val, getSchemaTableCombination("SCHEMA", "TABLE"))); list.add(r);
            // Limit clause 
            r = new Row(); r.addValue(new Value(par, "LIMIT clause for 100 rows")); r.addValue(new Value(val, getLimitClause(100))); list.add(r);
            // add column statement 
            r = new Row(); r.addValue(new Value(par, "Add column statement")); r.addValue(new Value(val, getAddColumnStatement("TABLE", testValue, null, false, null, false))); list.add(r);
            // drop column statement 
            r = new Row(); r.addValue(new Value(par, "Drop column statement")); r.addValue(new Value(val, getDropColumnStatement("TABLE", testValue, null, false, null, false))); list.add(r);
            // Modify column statement 
            r = new Row(); r.addValue(new Value(par, "Modify column statement")); r.addValue(new Value(val, getModifyColumnStatement("TABLE", testValue, null, false, null, false))); list.add(r);
            
            // List of reserved words 
            String reserved = "";
            if (getReservedWords()!=null) for (int i=0;i<getReservedWords().length;i++) reserved+=(i>0?", ":"")+getReservedWords()[i];
            r = new Row(); r.addValue(new Value(par, "List of reserved words")); r.addValue(new Value(val, reserved)); list.add(r);
            
            // Quote reserved words?
            r = new Row(); r.addValue(new Value(par, "Quote reserved words?")); r.addValue(new Value(val, quoteReservedWords())); list.add(r);
            // Start Quote
            r = new Row(); r.addValue(new Value(par, "Start quote for reserved words")); r.addValue(new Value(val, getStartQuote())); list.add(r);
            // End Quote
            r = new Row(); r.addValue(new Value(par, "End quote for reserved words")); r.addValue(new Value(val, getEndQuote())); list.add(r);
            
            // List of table types
            String types = "";
            String slist[] = getTableTypes(); 
            if (slist!=null) for (int i=0;i<slist.length;i++) types+=(i>0?", ":"")+slist[i];
            r = new Row(); r.addValue(new Value(par, "List of JDBC table types")); r.addValue(new Value(val, types)); list.add(r);
            
            // List of view types
            types = "";
            slist = getViewTypes(); 
            if (slist!=null) for (int i=0;i<slist.length;i++) types+=(i>0?", ":"")+slist[i];
            r = new Row(); r.addValue(new Value(par, "List of JDBC view types")); r.addValue(new Value(val, types)); list.add(r);
            
            // List of synonym types
            types = "";
            slist = getSynonymTypes(); 
            if (slist!=null) for (int i=0;i<slist.length;i++) types+=(i>0?", ":"")+slist[i];
            r = new Row(); r.addValue(new Value(par, "List of JDBC synonym types")); r.addValue(new Value(val, types)); list.add(r);
            
            // Use schema-name to get list of tables?
            r = new Row(); r.addValue(new Value(par, "use schema name to get table list?")); r.addValue(new Value(val, useSchemaNameForTableList())); list.add(r);
            // supports view?
            r = new Row(); r.addValue(new Value(par, "supports views?")); r.addValue(new Value(val, supportsViews())); list.add(r);
            // supports synonyms?
            r = new Row(); r.addValue(new Value(par, "supports synonyms?")); r.addValue(new Value(val, supportsSynonyms())); list.add(r);
            // SQL: get list of procedures?
            r = new Row(); r.addValue(new Value(par, "SQL: list of procedures")); r.addValue(new Value(val, getSQLListOfProcedures())); list.add(r);
            // SQL: get truncate table statement?
            r = new Row(); r.addValue(new Value(par, "SQL: truncate table")); r.addValue(new Value(val, getTruncateTableStatement(null, "TABLE"))); list.add(r);
            // supports float rounding on update?
            r = new Row(); r.addValue(new Value(par, "supports floating point rounding on update/insert")); r.addValue(new Value(val, supportsFloatRoundingOnUpdate())); list.add(r);
            // supports time stamp to date conversion
            r = new Row(); r.addValue(new Value(par, "supports timestamp-date conversion")); r.addValue(new Value(val, supportsTimeStampToDateConversion())); list.add(r);
            // supports batch updates
            r = new Row(); r.addValue(new Value(par, "supports batch updates")); r.addValue(new Value(val, supportsBatchUpdates())); list.add(r);
            // supports boolean values
            r = new Row(); r.addValue(new Value(par, "supports boolean data type")); r.addValue(new Value(val, supportsBooleanDataType())); list.add(r);
        }
        
        return list;
    }

    /**
     * @return true if the database resultsets support getTimeStamp() to retrieve date-time. (Date)
     */
    public boolean supportsTimeStampToDateConversion()
    {
        return databaseInterface.supportsTimeStampToDateConversion();
    }
    
    /**
     * @return true if the database JDBC driver supports batch updates
     * For example Interbase doesn't support this!
     */
    public boolean supportsBatchUpdates()
    {
        return databaseInterface.supportsBatchUpdates();
    }

   
    /**
     * @return true if the database supports a boolean, bit, logical, ... datatype
     */
    public boolean supportsBooleanDataType()
    {
        return databaseInterface.supportsBooleanDataType();
    }

    /**
     * Changes the names of the fields to their quoted equivalent if this is needed
     * @param fields The row of fields to change
     */
    public void quoteReservedWords(Row fields)
    {
        for (int i=0;i<fields.size();i++)
        {
            Value v = fields.getValue(i);
            v.setName( quoteField(v.getName()) );
        }
    }

    /**
     * @return a map of all the extra URL options you want to set.
     */
    public Map getExtraOptions()
    {
        return databaseInterface.getExtraOptions();
    }
    
    /**
     * @return true if the database supports connection options in the URL, false if they are put in a Properties object.
     */
    public boolean supportsOptionsInURL()
    {
        return databaseInterface.supportsOptionsInURL();
    }
    
    /**
     * @return extra help text on the supported options on the selected database platform.
     */
    public String getExtraOptionsHelpText()
    {
        return databaseInterface.getExtraOptionsHelpText();
    }

    /**
     * @return true if the database JDBC driver supports getBlob on the resultset.  If not we must use getBytes() to get the data.
     */
    public boolean supportsGetBlob()
    {
        return databaseInterface.supportsGetBlob();
    }
    
    /**
     * @return The SQL to execute right after connecting
     */
    public String getConnectSQL()
    {
        return databaseInterface.getConnectSQL();
    }

    /**
     * @param sql The SQL to execute right after connecting
     */
    public void setConnectSQL(String sql)
    {
        databaseInterface.setConnectSQL(sql);
    }
    
    /**
     * @return true if the database supports setting the maximum number of return rows in a resultset.
     */
    public boolean supportsSetMaxRows()
    {
        return databaseInterface.supportsSetMaxRows();
    }

    /**
     * Verify the name of the database and if required, change it if it already exists in the list of databases.
     * @param databases the databases to check against.
     * @param oldname the old name of the database
     * @return the new name of the database connection
     */
    public String verifyAndModifyDatabaseName(ArrayList databases, String oldname)
    {
        String name = getName();
        if (name.equalsIgnoreCase(oldname)) return name; // nothing to see here: move along!
        
        int nr = 2;
        while (DatabaseMeta.findDatabase(databases, getName())!=null)
        {
            setName(name+" "+nr);
            nr++;
        }
        return getName();
    }

    /**
     * @return true if we want to use a database connection pool
     */
    public boolean isUsingConnectionPool()
    {
        return databaseInterface.isUsingConnectionPool();
    }
    
    /**
     * @param usePool true if we want to use a database connection pool
     */
    public void setUsingConnectionPool(boolean usePool)
    {
        databaseInterface.setUsingConnectionPool(usePool);
    }

    
    /**
     * @return the maximum pool size
     */
    public int getMaximumPoolSize()
    {
        return databaseInterface.getMaximumPoolSize();
    }

    /**
     * @param maximumPoolSize the maximum pool size
     */
    public void setMaximumPoolSize(int maximumPoolSize)
    {
        databaseInterface.setMaximumPoolSize(maximumPoolSize);
    }

    /**
     * @return the initial pool size
     */
    public int getInitialPoolSize()
    {
        return databaseInterface.getInitialPoolSize();
    }
    
    /**
     * @param initalPoolSize the initial pool size
     */
    public void setInitialPoolSize(int initalPoolSize)
    {
        databaseInterface.setInitialPoolSize(initalPoolSize);
    }
    
    /**
     * @return true if the connection contains partitioning information
     */
    public boolean isPartitioned()
    {
        return databaseInterface.isPartitioned();
    }
    
    /**
     * @param partitioned true if the connection is set to contain partitioning information
     */
    public void setPartitioned(boolean partitioned)
    {
        databaseInterface.setPartitioned(partitioned);
    }
    
    /**
     * @return the available partition/host/databases/port combinations in the cluster
     */
    public PartitionDatabaseMeta[] getPartitioningInformation()
    {
        return databaseInterface.getPartitioningInformation();
    }
    
    /**
     * @param partitionInfo the available partition/host/databases/port combinations in the cluster
     */
    public void setPartitioningInformation(PartitionDatabaseMeta[] partitionInfo)
    {
        databaseInterface.setPartitioningInformation(partitionInfo);
    }
    
    /**
     * Finds the partition metadata for the given partition iD
     * @param partitionId The partition ID to look for
     * @return the partition database metadata or null if nothing was found.
     */
    public PartitionDatabaseMeta getPartitionMeta(String partitionId)
    {
        PartitionDatabaseMeta[] partitionInfo = getPartitioningInformation();
        for (int i=0;i<partitionInfo.length;i++)
        {
            if (partitionInfo[i].getPartitionId().equals(partitionId)) return partitionInfo[i];
        }
        return null;
    }

    public Properties getConnectionPoolingProperties()
    {
        return databaseInterface.getConnectionPoolingProperties();
    }

    public void setConnectionPoolingProperties(Properties properties)
    {
        databaseInterface.setConnectionPoolingProperties(properties);
    }

    public String getSQLTableExists(String tablename)
    {
        return databaseInterface.getSQLTableExists(tablename);
    }

    public boolean needsToLockAllTables()
    {
        return databaseInterface.needsToLockAllTables();
    }

    public String getQuotedSchemaTableCombination(String schemaName, String tableName)
    {
        return getSchemaTableCombination(quoteField(schemaName), quoteField(tableName));
    }

    /**
     * @return true if the database is streaming results (normally this is an option just for MySQL).
     */
    public boolean isStreamingResults()
    {
        return databaseInterface.isStreamingResults();
    }
    
    /**
     * @param useStreaming true if we want the database to stream results (normally this is an option just for MySQL).
     */
    public void setStreamingResults(boolean useStreaming)
    {
        databaseInterface.setStreamingResults(useStreaming);
    }

    /**
     * Find a database with a certain name in an arraylist of databases.
     * @param databases The ArrayList of databases
     * @param dbname The name of the database connection
     * @return The database object if one was found, null otherwise.
     */
    public static final DatabaseMeta findDatabase(List databases, String dbname)
    {
        if (databases == null)
            return null;

        for (int i = 0; i < databases.size(); i++)
        {
            DatabaseMeta ci = (DatabaseMeta) databases.get(i);
            if (ci.getName().equalsIgnoreCase(dbname))
                return ci;
        }
        return null;
    }
    
    /**
     * Find a database with a certain name in an arraylist of databases.
     * @param databases The ArrayList of databases
     * @param dbname The name of the database connection
     * @param exclude the name of the database connection to exclude from the search
     * @return The database object if one was found, null otherwise.
     */
    public static final DatabaseMeta findDatabase(List databases, String dbname, String exclude)
    {
        if (databases == null)
            return null;

        for (int i = 0; i < databases.size(); i++)
        {
            DatabaseMeta ci = (DatabaseMeta) databases.get(i);
            if (ci.getName().equalsIgnoreCase(dbname))
                return ci;
        }
        return null;
    }

    /**
     * Find a database with a certain ID in an arraylist of databases.
     * @param databases The ArrayList of databases
     * @param id The id of the database connection
     * @return The database object if one was found, null otherwise.
     */
    public static final DatabaseMeta findDatabase(List databases, long id)
    {
        if (databases == null)
            return null;

        for (int i = 0; i < databases.size(); i++)
        {
            DatabaseMeta ci = (DatabaseMeta) databases.get(i);
            if (ci.getID() == id)
                return ci;
        }
        return null;
    }
}
