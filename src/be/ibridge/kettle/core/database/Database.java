 /**********************************************************************
 **                                                                   **
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

import java.io.StringReader;
import java.sql.BatchUpdateException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.eclipse.core.runtime.IProgressMonitor;
import org.pentaho.core.util.DatasourceHelper;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.Counter;
import be.ibridge.kettle.core.DBCache;
import be.ibridge.kettle.core.DBCacheEntry;
import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Result;
import be.ibridge.kettle.core.Row;
import be.ibridge.kettle.core.database.map.DatabaseConnectionMap;
import be.ibridge.kettle.core.exception.KettleDatabaseBatchException;
import be.ibridge.kettle.core.exception.KettleDatabaseException;
import be.ibridge.kettle.core.util.StringUtil;
import be.ibridge.kettle.core.value.Value;
import be.ibridge.kettle.trans.step.dimensionlookup.DimensionLookupMeta;


/**
 * Database handles the process of connecting to, reading from, writing to and updating databases.
 * The database specific parameters are defined in DatabaseInfo.
 * 
 * @author Matt
 * @since 05-04-2003
 *
 */
public class Database
{
	private DatabaseMeta databaseMeta;
	
	private int    rowlimit;
	private int    commitsize;

	private Connection connection;
		
	private Statement sel_stmt;
	private PreparedStatement pstmt;
	private PreparedStatement prepStatementLookup;
	private PreparedStatement prepStatementUpdate;
	private PreparedStatement prepStatementInsert;
	private PreparedStatement pstmt_pun;
	private PreparedStatement pstmt_dup;
	private PreparedStatement pstmt_seq;
	private CallableStatement cstmt;
		
	// private ResultSetMetaData rsmd;
	private DatabaseMetaData  dbmd;
	
	private Row rowinfo; 
	
	private int written;
	
	private LogWriter log;
	
	/**
	 * Counts the number of rows written to a batch.
	 */
	private int batchCounter;

    /**
     * Number of times a connection was opened using this object.
     * Only used in the context of a database connection map
     */
    private int opened;

    /**
     * The copy is equal to opened at the time of creation.
     */
    private int copy; 

    private String connectionGroup;
    private boolean performRollbackAtLastDisconnect;
    private String partitionId;
    
    
	/**
	 * Construnct a new Database Connection
	 * @param inf The Database Connection Info to construct the connection with.
	 */
	public Database(DatabaseMeta inf)
	{
		log=LogWriter.getInstance();
		databaseMeta = inf;
		
		pstmt = null;
		rowinfo = null;
		dbmd = null;
		
		rowlimit=0;
		
		written=0;
        
        performRollbackAtLastDisconnect=false;
				
		log.logDetailed(toString(), "New database connection defined");
	}
    
    public boolean equals(Object obj)
    {
        Database other = (Database) obj;
        return other.databaseMeta.equals(other.databaseMeta);
    }
	
	/**
     * @return Returns the connection.
     */
    public Connection getConnection()
    {
        return connection;
    }
	
	/**
	 * Set the maximum number of records to retrieve from a query.
	 * @param rows
	 */
	public void setQueryLimit(int rows)
	{
		rowlimit = rows;
	}
	
	/**
     * @return Returns the prepStatementInsert.
     */
    public PreparedStatement getPrepStatementInsert()
    {
        return prepStatementInsert;
    }
    
    /**
     * @return Returns the prepStatementLookup.
     */
    public PreparedStatement getPrepStatementLookup()
    {
        return prepStatementLookup;
    }
    
    /**
     * @return Returns the prepStatementUpdate.
     */
    public PreparedStatement getPrepStatementUpdate()
    {
        return prepStatementUpdate;
    }

    /**
     * Open the database connection.
     * @throws KettleDatabaseException if something went wrong.
     */
    public void connect() throws KettleDatabaseException
    {
        connect(null);
    }

    /**
     * Open the database connection.
     * @param partitionId the partition ID in the cluster to connect to.
     * @throws KettleDatabaseException if something went wrong.
     */
    public void connect(String partitionId) throws KettleDatabaseException
    {
        connect(null, partitionId);
    }

    public synchronized void connect(String group, String partitionId) throws KettleDatabaseException
    {
        // Before anything else, let's see if we already have a connection defined for this group/partition!
        // The group is called after the thread-name of the transformation or job that is running
        // The name of that threadname is expected to be unique (it is in Kettle)
        // So the deal is that if there is another thread using that, we go for it. 
        // 
        if (!Const.isEmpty(group))
        {
            this.connectionGroup = group;
            this.partitionId = partitionId;
            
            DatabaseConnectionMap map = DatabaseConnectionMap.getInstance();
            
            // Try to find the conection for the group
            Database lookup = map.getDatabase(group, partitionId, this);
            if (lookup==null) // We already opened this connection for the partition & database in this group
            {
                // Do a normal connect and then store this database object for later re-use.
                normalConnect(partitionId);
                opened++;
                copy = opened;
                
                map.storeDatabase(group, partitionId, this);
            }
            else
            {
                connection = lookup.getConnection();
                lookup.setOpened(lookup.getOpened()+1); // if this counter hits 0 again, close the connection.
                copy = lookup.getOpened();
            }
        }
        else
        {
            // Proceed with a normal connect
            normalConnect(partitionId);
        }
    }
    
	/**
	 * Open the database connection.
     * @param partitionId the partition ID in the cluster to connect to.
	 * @throws KettleDatabaseException if something went wrong.
	 */
	public void normalConnect(String partitionId) throws KettleDatabaseException
	{
        if (databaseMeta==null)
        {
            throw new KettleDatabaseException("No valid database connection defined!");
        }
        
        try
		{
            // First see if we use connection pooling...
            //
            if ( databaseMeta.isUsingConnectionPool() &&  // default = false for backward compatibility
                 databaseMeta.getAccessType()!=DatabaseMeta.TYPE_ACCESS_JNDI // JNDI does pooling on it's own.
                )
            {
                try
                {
                    this.connection = ConnectionPoolUtil.getConnection(databaseMeta, partitionId);
                } 
                catch (Exception e)
                {
                    throw new KettleDatabaseException("Error occured while trying to connect to the database", e);
                }
            }
            else
            {
    			connectUsingClass(databaseMeta.getDriverClass(), partitionId );
    			log.logDetailed(toString(), "Connected to database.");
                
                // See if we need to execute extra SQL statemtent...
                String sql = StringUtil.environmentSubstitute( databaseMeta.getConnectSQL() ); 
                
                // only execute if the SQL is not empty, null and is not just a bunch of spaces, tabs, CR etc.
                if (!Const.isEmpty(sql) && !Const.onlySpaces(sql))
                {
                    execStatements(sql);
                    log.logDetailed(toString(), "Executed connect time SQL statements:"+Const.CR+sql);
                }
            }
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Error occured while trying to connect to the database", e);
		}
	}

	
	  private void initWithJNDI(String jndiName) throws KettleDatabaseException {
		  connection = null;
		  try {
			  DataSource dataSource = DatasourceHelper.getDataSourceFromJndi(jndiName);
			  if (dataSource != null) {
				  connection = dataSource.getConnection();
				  if (connection == null) {
					  throw new KettleDatabaseException( "Invalid JNDI connection"+ jndiName); //$NON-NLS-1$
				  }
		      } else {
				  throw new KettleDatabaseException( "Invalid JNDI connection"+ jndiName); //$NON-NLS-1$
		      }
		  } catch (Exception e) {
			  throw new KettleDatabaseException( "Invalid JNDI connection"+ jndiName + ": " + e.getMessage()); //$NON-NLS-1$
		  }
	  }

	/**
	 * Connect using the correct classname 
	 * @param classname for example "org.gjt.mm.mysql.Driver"
	 * @return true if the connect was succesfull, false if something went wrong.
	 */
	private void connectUsingClass(String classname, String partitionId) throws KettleDatabaseException
	{
		// Install and load the jdbc Driver

		// first see if this is a JNDI connection
		if( databaseMeta.getAccessType() == DatabaseMeta.TYPE_ACCESS_JNDI ) {
			initWithJNDI( databaseMeta.getDatabaseName() );
			return;
		}
		
		try 
		{
			Class.forName(classname);
		}
		catch(NoClassDefFoundError e)
		{ 
			throw new KettleDatabaseException("Exception while loading class", e);
		}
		catch(ClassNotFoundException e)
		{
			throw new KettleDatabaseException("Exception while loading class", e);
		}
		catch(Exception e)
		{ 
			throw new KettleDatabaseException("Exception while loading class", e);
		}

		try 
		{
            String url;
            
            if (databaseMeta.isPartitioned() && !Const.isEmpty(partitionId))
            {
                url = StringUtil.environmentSubstitute(databaseMeta.getURL(partitionId));
            }
            else
            {
                url = StringUtil.environmentSubstitute(databaseMeta.getURL()); 
            }
            
            String clusterUsername=null;
            String clusterPassword=null;
            if (databaseMeta.isPartitioned() && !Const.isEmpty(partitionId))
            {
                // Get the cluster information...
                PartitionDatabaseMeta partition = databaseMeta.getPartitionMeta(partitionId);
                if (partition!=null)
                {
                    clusterUsername = partition.getUsername();
                    clusterPassword = partition.getPassword();
                }
            }
            
			String username;
            String password;
            if (!Const.isEmpty(clusterUsername))
            {
                username = clusterUsername;
                password = clusterPassword; 
            }
            else
            {
                username = StringUtil.environmentSubstitute(databaseMeta.getUsername());
                password = StringUtil.environmentSubstitute(databaseMeta.getPassword());
            }

            if (databaseMeta.supportsOptionsInURL())
            {
                if (!Const.isEmpty(username) || !Const.isEmpty(password))
                {
                	// also allow for empty username with given password, in this case username must be given with one space 
                    connection = DriverManager.getConnection(url, Const.NVL(username, " "), Const.NVL(password, ""));
                }
                else
                {
                    // Perhaps the username is in the URL or no username is required...
                    connection = DriverManager.getConnection(url);
                }
            }
            else
            {
                Properties properties = databaseMeta.getConnectionProperties();
                if (!Const.isEmpty(username)) properties.put("user", username);
                if (!Const.isEmpty(password)) properties.put("password", password);
                
                connection = DriverManager.getConnection(url, properties);
            }
		} 
		catch(SQLException e) 
		{
			throw new KettleDatabaseException("Error connecting to database: (using class "+classname+")", e);
		}
        catch(Throwable e)
        {
            throw new KettleDatabaseException("Error connecting to database: (using class "+classname+")", e);
        }
	}

	/**
	 * Disconnect from the database and close all open prepared statements.
	 */
	public synchronized void disconnect()
	{	
		try
		{
			if (connection==null)
            {
                return ; // Nothing to do...
            }
			if (connection.isClosed())
            {
                return ; // Nothing to do...
            }

			if (pstmt    !=null) 
            { 
                pstmt.close(); 
                pstmt=null; 
            } 
			if (prepStatementLookup!=null) 
            { 
                prepStatementLookup.close(); 
                prepStatementLookup=null; 
            } 
			if (prepStatementInsert!=null) 
			{ 
                prepStatementInsert.close(); 
                prepStatementInsert=null; 
            } 
			if (prepStatementUpdate!=null) 
            { 
                prepStatementUpdate.close(); 
                prepStatementUpdate=null; 
            } 
			if (pstmt_seq!=null) 
            { 
                pstmt_seq.close(); 
                pstmt_seq=null; 
            } 
            
            // See if there are other steps using this connection in a connection group.
            // If so, we will hold commit & connection close until then.
            // 
            if (!Const.isEmpty(connectionGroup))
            {
                DatabaseConnectionMap map = DatabaseConnectionMap.getInstance();
                Database lookup = map.getDatabase(connectionGroup, partitionId, this);
                if (lookup!=null)
                {
                    lookup.opened--;
                    
                    if (lookup.opened>0)
                    {
                        return;
                    }
                    else
                    {
                        map.removeConnection(connectionGroup, partitionId, this); // remove the trace of it.
                        
                        // Before we close perform commit or rollback.
                        
                        if (performRollbackAtLastDisconnect)
                        {
                            rollback(true);
                        }
                        else
                        {
                            commit(true);
                        }
                    }
                }
            }
            else
            {
                if (!isAutoCommit()) // Do we really still need this commit??
                {
                    commit();
                }
            }

			if (connection!=null) 
            { 
                connection.close(); 
                if (!databaseMeta.isUsingConnectionPool()) 
                {
                    connection=null; 
                }
            } 
			log.logDetailed(toString(), "Connection to database closed!");
		}
		catch(SQLException ex) 
		{
			log.logError(toString(), "Error disconnecting from database:"+Const.CR+ex.getMessage());
            log.logError(toString(), Const.getStackTracker(ex));
		}
		catch(KettleDatabaseException dbe)
		{
			log.logError(toString(), "Error disconnecting from database:"+Const.CR+dbe.getMessage());
            log.logError(toString(), Const.getStackTracker(dbe));
		}
	}
	
    /**
     * Cancel the open/running queries on the database connection
     * @throws KettleDatabaseException
     */
	public void cancelQuery() throws KettleDatabaseException
	{
        cancelStatement(pstmt);
        cancelStatement(sel_stmt);
	}
    
    /**
     * Cancel an open/running SQL statement 
     * @param statement the statement to cancel
     * @throws KettleDatabaseException
     */
    public void cancelStatement(Statement statement) throws KettleDatabaseException
    {
        try
        {
            if (statement!=null) 
            { 
                statement.cancel(); 
            } 
            log.logDetailed(toString(), "Statement canceled!");
        }
        catch(SQLException ex) 
        {
            throw new KettleDatabaseException("Error cancelling statement", ex);
        }
    }

	/**
	 * Specify after how many rows a commit needs to occur when inserting or updating values.
	 * @param commsize The number of rows to wait before doing a commit on the connection.
	 */
	public void setCommit(int commsize)
	{
        commitsize=commsize;
        String onOff = (commitsize<=0?"on":"off");
        try
        {
            connection.setAutoCommit(commitsize<=0);
            log.logDetailed(toString(), "Auto commit "+onOff);
        }
        catch(Exception e)
        {
            log.logError(toString(), "Can't turn auto commit "+onOff);
        }
	}
	
    /**
     * Perform a commit the connection if this is supported by the database
     */
    public void commit() throws KettleDatabaseException
    {
        commit(false);
    }
    
	private void commit(boolean force) throws KettleDatabaseException
	{
		try
		{
		    // Don't do the commit, wait until the end of the transformation.  
            // When the last database copy (opened counter) is about to be closed, we do a commit 
            // There is one catch, we need to catch the rollback
            // The transformation will stop everything and then we'll do the rollback.
            // The flag is in "performRollback", private only
            //
            if (!Const.isEmpty(connectionGroup) && !force)
            {
                return; 
            }
			if (getDatabaseMetaData().supportsTransactions())
			{
				connection.commit();
			}
			else
			{
			    log.logDetailed(toString(), "No commit possible on database connection ["+toString()+"]");
			}
		}
		catch(Exception e)
		{
			if (databaseMeta.supportsEmptyTransactions())
				throw new KettleDatabaseException("Error comitting connection", e);
		}
	}

    public void rollback() throws KettleDatabaseException
    {
        rollback(false);
    }

	private void rollback(boolean force) throws KettleDatabaseException
	{
		try
		{
            if (!Const.isEmpty(connectionGroup) && !force)
            {
                performRollbackAtLastDisconnect=true;
                return; 
            }
            if (getDatabaseMetaData().supportsTransactions())
            {
                if (connection!=null) connection.rollback();
            }
            else
            {
                log.logDetailed(toString(), "No rollback possible on database connection ["+toString()+"]");
            }
			
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Error performing rollback on connection", e);
		}
	}

    /**
     * Prepare inserting values into a table, using the fields & values in a Row
     * @param r The row to determine which values need to be inserted
     * @param table The name of the table in which we want to insert rows
     * @throws KettleDatabaseException if something went wrong.
     */
    public void prepareInsert(Row r, String tableName) throws KettleDatabaseException
    {
        prepareInsert(r, null, tableName);
    }
    
	/**
	 * Prepare inserting values into a table, using the fields & values in a Row
	 * @param r The row to determine which values need to be inserted
     * @param schemaName The name of the schema in which we want to insert rows
	 * @param tableName The name of the table in which we want to insert rows
	 * @throws KettleDatabaseException if something went wrong.
	 */
	public void prepareInsert(Row r, String schemaName, String tableName) throws KettleDatabaseException
	{
		if (r.size()==0)
		{
			throw new KettleDatabaseException("No fields in row, can't insert!");
		}
		
		String ins = getInsertStatement(schemaName, tableName, r);
				
		log.logDetailed(toString(),"Preparing statement: "+Const.CR+ins);
		prepStatementInsert=prepareSQL(ins);
	}

    /**
     * Prepare a statement to be executed on the database. (does not return generated keys)
     * @param sql The SQL to be prepared
     * @return The PreparedStatement object.
     * @throws KettleDatabaseException
     */
	public PreparedStatement prepareSQL(String sql)
	 throws KettleDatabaseException
	{
		return prepareSQL(sql, false);
	}

    /**
     * Prepare a statement to be executed on the database.
     * @param sql The SQL to be prepared
     * @param returnKeys set to true if you want to return generated keys from an insert statement 
     * @return The PreparedStatement object.
     * @throws KettleDatabaseException
     */
	public PreparedStatement prepareSQL(String sql, boolean returnKeys) throws KettleDatabaseException
	{
		try
		{
			if (returnKeys)
			{
				return connection.prepareStatement(databaseMeta.stripCR(sql), Statement.RETURN_GENERATED_KEYS);
			}
			else
			{
				return connection.prepareStatement(databaseMeta.stripCR(sql));
			}
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Couldn't prepare statement:"+Const.CR+sql, ex);
		}
	}

	public void closeLookup() throws KettleDatabaseException
	{
		closePreparedStatement(pstmt);
        pstmt=null;
	}

	public void closePreparedStatement(PreparedStatement ps) throws KettleDatabaseException

	{
		if (ps!=null)
		{
			try 
			{
				ps.close();
			}
			catch(SQLException e)
			{
				throw new KettleDatabaseException("Error closing prepared statement", e);
			}
		}
	}
	
	
	public void closeInsert() throws KettleDatabaseException

	{
		if (prepStatementInsert!=null)
		{
			try 
			{
				prepStatementInsert.close();
				prepStatementInsert = null;
			}
			catch(SQLException e)
			{
				throw new KettleDatabaseException("Error closing insert prepared statement.", e);
			}
		}
	}
	
	public void closeUpdate() throws KettleDatabaseException
	{
		if (prepStatementUpdate!=null)
		{
			try 
			{
				prepStatementUpdate.close();
                prepStatementUpdate=null;
			}
			catch(SQLException e)
			{
				throw new KettleDatabaseException("Error closing update prepared statement.", e);
			}
		}
	}


	public void setValues(Row r)
		throws KettleDatabaseException
	{
		setValues(r, pstmt);
	}

	public void setValuesInsert(Row r)
		throws KettleDatabaseException
	{
		setValues(r, prepStatementInsert);
	}

	public void setValuesUpdate(Row r)
		throws KettleDatabaseException
	{
		setValues(r, prepStatementUpdate);
	}
	
	public void setValuesLookup(Row r)
		throws KettleDatabaseException
	{
		setValues(r, prepStatementLookup);
	}
	
	public void setProcValues(Row r, int argnrs[], String argdir[], boolean result) throws KettleDatabaseException
	{
		int pos;
		
		if (result) pos=2; else pos=1;
		
		for (int i=0;i<argnrs.length;i++)
		{
			if (argdir[i].equalsIgnoreCase("IN") || argdir[i].equalsIgnoreCase("INOUT"))
			{
				Value v=r.getValue(argnrs[i]);
				setValue(cstmt, v, pos);
				pos++;
			}
		}
	}

	public void setValue(PreparedStatement ps, Value v, int pos)
		throws KettleDatabaseException
	{
		String debug = "";

		try
		{
			switch(v.getType())
			{
			case Value.VALUE_TYPE_NUMBER :
			    debug="Number";
                if (!v.isNull()) 
                {
                    debug="Number, not null, getting number from value";
                    double num = v.getNumber();
                    if (databaseMeta.supportsFloatRoundingOnUpdate() && v.getPrecision()>=0)
                    {
                        debug="Number, rounding to precision ["+v.getPrecision()+"]";
                        num = Const.round(num, v.getPrecision());
                    }
                    debug="Number, setting ["+num+"] on position #"+pos+" of the prepared statement";
                    ps.setDouble(pos, num);
                }
				else 
                {
                    ps.setNull(pos, java.sql.Types.DOUBLE);   
                }
				break;
			case Value.VALUE_TYPE_INTEGER:
				debug="Integer";
				if (!v.isNull()) 
				{
					if (databaseMeta.supportsSetLong())
					{
                        ps.setLong(pos, v.getInteger() );
					}
					else
					{
					    if (databaseMeta.supportsFloatRoundingOnUpdate() && v.getPrecision()>=0)
                        {
                            ps.setDouble(pos, v.getNumber() );
                        }
                        else
                        {
                            ps.setDouble(pos, Const.round( v.getNumber(), v.getPrecision() ) );
                        }
					}
				}
				else 
                {
                    ps.setNull(pos, java.sql.Types.INTEGER);   
                }
				break;
			case Value.VALUE_TYPE_STRING : 
				debug="String";
				if (v.getLength()<DatabaseMeta.CLOB_LENGTH)
				{
					if (!v.isNull() && v.getString()!=null) 
					{
						ps.setString(pos, v.getString());
					}
					else 
					{
						ps.setNull(pos, java.sql.Types.VARCHAR);
					}
				}
				else
				{
					if (!v.isNull())
					{
						int maxlen = databaseMeta.getMaxTextFieldLength();
						int len    = v.getStringLength();
						
						// Take the last maxlen characters of the string...
						int begin  = len - maxlen;
						if (begin<0) begin=0;
						
						// Get the substring!
						String logging = v.getString().substring(begin);
	
						if (databaseMeta.supportsSetCharacterStream())
						{
                            StringReader sr = new StringReader(logging);
							ps.setCharacterStream(pos, sr, logging.length());
						}
						else
						{
							ps.setString(pos, logging);
						}
					}
					else
					{
						ps.setNull(pos, java.sql.Types.VARCHAR);
					}
				}
				break;
			case Value.VALUE_TYPE_DATE   : 
				debug="Date";
				// VALUE_TYPE_DATE: Date with Time component.
				if (!v.isNull() && v.getDate()!=null) 
				{
					long dat = v.getDate().getTime();
					if(v.getPrecision()==1 || !databaseMeta.supportsTimeStampToDateConversion())
                    {
                        //       Convert to DATE!
					    java.sql.Date ddate = new java.sql.Date(dat);
                        ps.setDate(pos, ddate);
                    }
                    else
					{
                        java.sql.Timestamp sdate = new java.sql.Timestamp(dat);
					    ps.setTimestamp(pos, sdate);
					}
				}
				else
                {
                    if(v.getPrecision()==1 || !databaseMeta.supportsTimeStampToDateConversion())
                    {
                        ps.setNull(pos, java.sql.Types.DATE);
                    }
                    else
                    {
                        ps.setNull(pos, java.sql.Types.TIMESTAMP);
                    }
                }
				break;
			case Value.VALUE_TYPE_BOOLEAN:
				debug="Boolean";
                if (databaseMeta.supportsBooleanDataType())
                {
                    if (!v.isNull())
                    {
                        ps.setBoolean(pos, v.getBoolean());
                    }
                    else 
                    {
                        ps.setNull(pos, java.sql.Types.BOOLEAN);
                    }
                }
                else
                {
    				if (!v.isNull())
                    {
                        ps.setString(pos, v.getBoolean()?"Y":"N");
                    }
    				else 
                    {
                        ps.setNull(pos, java.sql.Types.CHAR);
                    }
                }
				break;
            case Value.VALUE_TYPE_BIGNUMBER:
                debug="BigNumber";
                if (!v.isNull()) 
                {
                    ps.setBigDecimal(pos, v.getBigNumber());
                }
                else 
                {
                    ps.setNull(pos, java.sql.Types.DECIMAL);   
                }
                break;
			case Value.VALUE_TYPE_BINARY:
				debug="Binary";
				if (!v.isNull() && v.getBytes()!=null) 
				{
                    ps.setBytes(pos, v.getBytes());
				}
				else
				{
                    ps.setNull(pos, java.sql.Types.BINARY);   
                }
				break;                
			default:
				debug="default";
            // placeholder
                ps.setNull(pos, java.sql.Types.VARCHAR);
 				break;
			}
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Error setting value #"+pos+" ["+v.toString()+"] on prepared statement ("+debug+")"+Const.CR+ex.toString(), ex);
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Error setting value #"+pos+" ["+(v==null?"NULL":v.toString())+"] on prepared statement ("+debug+")"+Const.CR+e.toString(), e);
		}
	}
	
	// Sets the values of the preparedStatement pstmt.
	public void setValues(Row r, PreparedStatement ps)
		throws KettleDatabaseException
	{
		int i;		
		Value v;

		// now set the values in the row!
		for (i=0;i<r.size();i++)
		{
			v=r.getValue(i);
			try
			{
				//System.out.println("Setting value ["+v+"] on preparedStatement, position="+i);
				setValue(ps, v, i+1);
			}
			catch(KettleDatabaseException e)
			{
				throw new KettleDatabaseException("offending row : "+r, e);
			}
		}
	}

	public void setDimValues(Row r, Value dateval)
		throws KettleDatabaseException
	{
		setDimValues(r, dateval, prepStatementLookup);
	}
	
	// Sets the values of the preparedStatement pstmt.
	public void setDimValues(Row r, Value dateval, PreparedStatement ps)
		throws KettleDatabaseException
	{
		int i;		
		Value v;
		long dat;

		// now set the values in the row!
		for (i=0;i<r.size();i++)
		{
			v=r.getValue(i);
			try
			{
				setValue(ps, v, i+1);
			}
			catch(KettleDatabaseException e)
			{
				throw new KettleDatabaseException("Unable to set value #"+i+" on dimension using row :"+r, e);
			}
		}
		if (dateval!=null && dateval.getDate()!=null && !dateval.isNull())
		{
			dat = dateval.getDate().getTime();
		}
		else
		{
			Calendar cal=Calendar.getInstance();  // use system date!
			dat = cal.getTime().getTime();
		}
		java.sql.Timestamp sdate = new java.sql.Timestamp(dat);
			
		try
		{
			ps.setTimestamp(r.size()+1, sdate); // ? >  date_from
			ps.setTimestamp(r.size()+2, sdate); // ? <= date_to
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to set timestamp on fromdate or todate", ex);
		}
	}			

    public void dimUpdate(Row row, String tableName, String fieldlookup[], int fieldnrs[], String returnkey, Value dimkey) throws KettleDatabaseException
    {
        dimUpdate(row, null, tableName, fieldlookup, fieldnrs, returnkey, dimkey);
    }
    
	public void dimUpdate(Row row, String schemaName, String tableName, String fieldlookup[], int fieldnrs[], String returnkey, Value dimkey) throws KettleDatabaseException
	{
		String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		
		if (pstmt_dup==null) // first time: construct prepared statement
		{
			// Construct the SQL statement...
			/*
			 * UPDATE d_customer
			 * SET    fieldlookup[] = row.getValue(fieldnrs)
			 * WHERE  returnkey = dimkey
			 * ;
			 */
			 
			String sql="UPDATE "+schemaTable+Const.CR+"SET ";
			
			for (int i=0;i<fieldlookup.length;i++)
			{
				if (i>0) sql+=", "; else sql+="  ";
				sql+= databaseMeta.quoteField(fieldlookup[i])+" = ?"+Const.CR;
			}
			sql+="WHERE  "+databaseMeta.quoteField(returnkey)+" = ?";

			try
			{
                if (log.isDebug()) log.logDebug(toString(), "Preparing statement: ["+sql+"]");
				pstmt_dup=connection.prepareStatement(databaseMeta.stripCR(sql));
			}
			catch(SQLException ex) 
			{
				throw new KettleDatabaseException("Couldn't prepare statement :"+Const.CR+sql, ex);
			}
		}
		
		// Assemble information
		// New
		Row rupd=new Row();
		for (int i=0;i<fieldnrs.length;i++)
		{
			rupd.addValue( row.getValue(fieldnrs[i]));
		}
		rupd.addValue( dimkey );
		
		setValues(rupd, pstmt_dup);
		insertRow(pstmt_dup);
	}

    public void dimInsert(Row row, String tableName, boolean newentry, String keyfield, boolean autoinc, Value technicalKey, String versionfield,
            Value val_version, String datefrom, Value val_datfrom, String dateto, Value val_datto, String fieldlookup[], int fieldnrs[],
            String key[], String keylookup[], int keynrs[]) throws KettleDatabaseException

    {
        dimInsert(row, null, tableName, newentry, keyfield, autoinc, technicalKey, versionfield, val_version, datefrom, val_datfrom, dateto, val_datto, fieldlookup, fieldnrs, key, keylookup, keynrs);
    }

	// This inserts new record into dimension
	// Optionally, if the entry already exists, update date range from previous version
	// of the entry.
	// 
	public void dimInsert(Row row, 
                             String schemaName,
	                         String tableName,
	                         boolean newentry, 
	                         String keyfield, 
	                         boolean autoinc,
	                         Value  technicalKey, 
	                         String versionfield, 
	                         Value  val_version,
	                         String datefrom, 
	                         Value  val_datfrom, 
	                         String dateto, 
	                         Value  val_datto, 
	                         String fieldlookup[], 
	                         int    fieldnrs[],
	                         String key[], 
	                         String keylookup[],
	                         int    keynrs[]
							) throws KettleDatabaseException
	{
		String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		
		if (prepStatementInsert==null && prepStatementUpdate==null) // first time: construct prepared statement
		{
			/* Construct the SQL statement...
			 *
			 * INSERT INTO 
			 * d_customer(keyfield, versionfield, datefrom,    dateto,   key[], fieldlookup[])
			 * VALUES    (val_key ,val_version , val_datfrom, val_datto, keynrs[], fieldnrs[])
			 * ;
			 */
			 
			String sql="INSERT INTO "+schemaTable+"( ";
			
			if (!autoinc) sql+=databaseMeta.quoteField(keyfield)+", "; // NO AUTOINCREMENT
			else
			if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_INFORMIX) sql+="0, "; // placeholder on informix!
			
			sql+=databaseMeta.quoteField(versionfield)+", "+databaseMeta.quoteField(datefrom)+", "+databaseMeta.quoteField(dateto);

			for (int i=0;i<keylookup.length;i++)
			{
				sql+=", "+databaseMeta.quoteField(keylookup[i]);
			}
			
			for (int i=0;i<fieldlookup.length;i++)
			{
				sql+=", "+databaseMeta.quoteField(fieldlookup[i]);
			}
			sql+=") VALUES(";
			
			if (!autoinc) sql+="?, ";
			sql+="?, ?, ?";

			for (int i=0;i<keynrs.length;i++)
			{
				sql+=", ?";
			}
			
			for (int i=0;i<fieldnrs.length;i++)
			{
				sql+=", ?";
			}
			sql+=" )";

			try
			{
				if (keyfield==null)
				{
					log.logDetailed(toString(), "SQL w/ return keys=["+sql+"]");
					prepStatementInsert=connection.prepareStatement(databaseMeta.stripCR(sql), Statement.RETURN_GENERATED_KEYS);
				}
				else
				{
					log.logDetailed(toString(), "SQL=["+sql+"]");
					prepStatementInsert=connection.prepareStatement(databaseMeta.stripCR(sql));
				}
				//pstmt=con.prepareStatement(sql, new String[] { "klant_tk" } );
			}
			catch(SQLException ex) 
			{
				throw new KettleDatabaseException("Unable to prepare dimension insert :"+Const.CR+sql, ex);
			}

			/* 
			* UPDATE d_customer
			* SET    dateto = val_datnow
			* WHERE  keylookup[] = keynrs[]
			* AND    versionfield = val_version - 1
			* ;
			*/

			String sql_upd="UPDATE "+schemaTable+Const.CR+"SET "+databaseMeta.quoteField(dateto)+" = ?"+Const.CR;
			sql_upd+="WHERE ";
			for (int i=0;i<keylookup.length;i++)
			{
				if (i>0) sql_upd+="AND   ";
				sql_upd+=databaseMeta.quoteField(keylookup[i])+" = ?"+Const.CR;
			}
			sql_upd+="AND   "+databaseMeta.quoteField(versionfield)+" = ? ";

			try
			{
				log.logDetailed(toString(), "Preparing update: "+Const.CR+sql_upd+Const.CR);
				prepStatementUpdate=connection.prepareStatement(databaseMeta.stripCR(sql_upd));
			}
			catch(SQLException ex) 
			{
				throw new KettleDatabaseException("Unable to prepare dimension update :"+Const.CR+sql_upd, ex);
			}
		}
		
		Row rins=new Row();
		if (!autoinc) rins.addValue(technicalKey);
		if (!newentry)
		{
			Value val_new_version = new Value(val_version);
			val_new_version.setValue( val_new_version.getNumber()+1 ); // determine next version
			rins.addValue(val_new_version);
		}
		else
		{
			rins.addValue(val_version);
		}
		rins.addValue(val_datfrom);
		rins.addValue(val_datto);
		for (int i=0;i<keynrs.length;i++)
		{
			rins.addValue( row.getValue(keynrs[i]));
		}
		for (int i=0;i<fieldnrs.length;i++)
		{
			Value val = row.getValue(fieldnrs[i]);
			rins.addValue( val );
		}
		
		if (log.isDebug()) log.logDebug(toString(), "rins, size="+rins.size()+", values="+rins.toString());
		
		// INSERT NEW VALUE!
		setValues(rins, prepStatementInsert);
		insertRow(prepStatementInsert);
			
        if (log.isDebug()) log.logDebug(toString(), "Row inserted!");
		if (keyfield==null)
		{
			try
			{
				Row keys = getGeneratedKeys(prepStatementInsert);
				if (keys.size()>0)
				{
					technicalKey.setValue(keys.getValue(0).getInteger());
				}
				else
				{
					throw new KettleDatabaseException("Unable to retrieve value of auto-generated technical key : no value found!");
				}
			}
			catch(Exception e)
			{
				throw new KettleDatabaseException("Unable to retrieve value of auto-generated technical key : unexpected error: ", e);
			}
		}
		
		if (!newentry) // we have to update the previous version in the dimension! 
		{
			/* 
			* UPDATE d_customer
			* SET    dateto = val_datfrom
			* WHERE  keylookup[] = keynrs[]
			* AND    versionfield = val_version - 1
			* ;
			*/
			Row rupd = new Row();
			rupd.addValue(val_datfrom);
			for (int i=0;i<keynrs.length;i++)
			{
				rupd.addValue( row.getValue(keynrs[i]));
			}
			rupd.addValue(val_version);
			
			if (log.isRowLevel()) log.logRowlevel(toString(), "UPDATE using rupd="+rupd.toString());

			// UPDATE VALUES
			setValues(rupd, prepStatementUpdate);  // set values for update
            if (log.isDebug()) log.logDebug(toString(), "Values set for update ("+rupd.size()+")");
			insertRow(prepStatementUpdate); // do the actual update
            if (log.isDebug()) log.logDebug(toString(), "Row updated!");
		}
	}

	/** 
	 * @param ps  The prepared insert statement to use
	 * @return The generated keys in auto-increment fields
	 * @throws KettleDatabaseException in case something goes wrong retrieving the keys.
	 */
	public Row getGeneratedKeys(PreparedStatement ps) throws KettleDatabaseException 
	{
		ResultSet keys = null;
		try
		{
			keys=ps.getGeneratedKeys(); // 1 row of keys
			ResultSetMetaData resultSetMetaData = keys.getMetaData();
			Row rowInfo = getRowInfo(resultSetMetaData);

			return getRow(keys, resultSetMetaData, rowInfo);
		}
		catch(Exception ex) 
		{
			throw new KettleDatabaseException("Unable to retrieve key(s) from auto-increment field(s)", ex);
		}
		finally
		{
			if (keys!=null)
			{
				try
				{
					keys.close();
				}
				catch(SQLException e)
				{
					throw new KettleDatabaseException("Unable to close resultset of auto-generated keys", e);
				}
			
			}
		}
	}

    public void dimPunchThrough(Row row, String tableName, int fieldupdate[], String fieldlookup[], int fieldnrs[], String key[], String keylookup[], int keynrs[]) throws KettleDatabaseException
    {
        dimPunchThrough(row, null, tableName, fieldupdate, fieldlookup, fieldnrs, key, keylookup, keynrs);
    }
    
	// This updates all versions of a dimension entry.
	// 
	public void dimPunchThrough(Row row, String schemaName, String tableName, int fieldupdate[], String fieldlookup[], int fieldnrs[], String key[], String keylookup[], int keynrs[]) throws KettleDatabaseException
	{
		boolean first=false;
		String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		if (pstmt_pun==null) // first time: construct prepared statement
		{
			/* 
			* UPDATE table
			* SET    punchv1 = fieldx, ...
			* WHERE  keylookup[] = keynrs[]
			* ;
			*/

			String sql_upd="UPDATE "+schemaTable+Const.CR;
			sql_upd+="SET ";
			first=true;
			for (int i=0;i<fieldlookup.length;i++)
			{
				if (fieldupdate[i]==DimensionLookupMeta.TYPE_UPDATE_DIM_PUNCHTHROUGH)
				{
					if (!first) sql_upd+=", "; else sql_upd+="  ";
					first=false;
					sql_upd+=fieldlookup[i]+" = ?"+Const.CR;
				}
			}
			sql_upd+="WHERE ";
			for (int i=0;i<keylookup.length;i++)
			{
				if (i>0) sql_upd+="AND   ";
				sql_upd+=keylookup[i]+" = ?"+Const.CR;
			}

			try
			{
				pstmt_pun=connection.prepareStatement(databaseMeta.stripCR(sql_upd));
			}
			catch(SQLException ex) 
			{
				throw new KettleDatabaseException("Unable to prepare dimension punchThrough update statement : "+Const.CR+sql_upd, ex);
			}
		}
		
		Row rupd=new Row();
		for (int i=0;i<fieldlookup.length;i++)
		{
			if (fieldupdate[i]==DimensionLookupMeta.TYPE_UPDATE_DIM_PUNCHTHROUGH)
			{
				rupd.addValue( row.getValue(fieldnrs[i]));
			}
		}
		for (int i=0;i<keynrs.length;i++)
		{
			rupd.addValue( row.getValue(keynrs[i]));
		}

		// UPDATE VALUES
		setValues(rupd, pstmt_pun);  // set values for update
		insertRow(pstmt_pun); // do the actual update
	}

	
    /**
     * This inserts new record into a junk dimension
     */
    public void combiInsert(   Row     row, 
                               String  tableName,
                               String  keyfield, 
                               boolean autoinc,
                               Value   val_key, 
                               String  keylookup[], 
                               int     keynrs[],
                               boolean crc,
                               String  crcfield,
                               Value   val_crc
                              ) throws KettleDatabaseException
    {
        combiInsert(row, null, tableName, keyfield, autoinc, val_key, keylookup, keynrs, crc, crcfield, val_crc);
    }
    
	/**
	 * This inserts new record into a junk dimension
	 */
	public void combiInsert(   Row     row, 
                               String  schemaName,
							   String  tableName,
							   String  keyfield, 
	                           boolean autoinc,
							   Value   val_key, 
							   String  keylookup[], 
							   int     keynrs[],
							   boolean crc,
							   String  crcfield,
							   Value   val_crc
							  ) throws KettleDatabaseException
	{
		String debug="Combination insert";
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		try
		{
			boolean comma;
			
			if (prepStatementInsert==null) // first time: construct prepared statement
			{
				debug="First: construct prepared statement";
				/* Construct the SQL statement...
				 *
				 * INSERT INTO 
				 * d_test(keyfield, [crcfield,] keylookup[])
				 * VALUES(val_key, [val_crc], row values with keynrs[])
				 * ;
				 */
				 
				StringBuffer sql = new StringBuffer(100);
				sql.append("INSERT INTO ").append(schemaTable).append("( ");
				comma=false;
	
				if (!autoinc) // NO AUTOINCREMENT 
				{
					sql.append(databaseMeta.quoteField(keyfield));
					comma=true;
				}
				else
				if (databaseMeta.needsPlaceHolder()) 
				{
					sql.append('0');   // placeholder on informix!  Will be replaced in table by real autoinc value.
					comma=true;
				} 
				
				if (crc)
				{
					if (comma) sql.append(", ");
					sql.append(databaseMeta.quoteField(crcfield));
					comma=true;
				}
				
				for (int i=0;i<keylookup.length;i++)
				{
					if (comma) sql.append(", "); 
					sql.append(databaseMeta.quoteField(keylookup[i]));
					comma=true;
				}
				
				sql.append(") VALUES (");
				
				comma=false;
				
				if (keyfield!=null)
				{
					sql.append('?');
					comma=true;
				}
				if (crc)
				{
					if (comma) sql.append(',');
					sql.append('?');
					comma=true;
				}
	
				for (int i=0;i<keylookup.length;i++)
				{
					if (comma) sql.append(','); else comma=true;
					sql.append('?');
				}
				
				sql.append(" )");
	
				String sqlStatement = sql.toString();
				try
				{
					debug="First: prepare statement";
					if (keyfield==null)
					{
						log.logDetailed(toString(), "SQL with return keys: "+sqlStatement);
						prepStatementInsert=connection.prepareStatement(databaseMeta.stripCR(sqlStatement), Statement.RETURN_GENERATED_KEYS);
					}
					else
					{
						log.logDetailed(toString(), "SQL without return keys: "+sqlStatement);
						prepStatementInsert=connection.prepareStatement(databaseMeta.stripCR(sqlStatement));
					}
				}
				catch(SQLException ex) 
				{
					throw new KettleDatabaseException("Unable to prepare combi insert statement : "+Const.CR+sqlStatement, ex);
				}
				catch(Exception ex)
				{
					throw new KettleDatabaseException("Unable to prepare combi insert statement : "+Const.CR+sqlStatement, ex);
				}
			}
			
			debug="Create new insert row rins";
			Row rins=new Row();
			
			if (!autoinc) rins.addValue(val_key);
			if (crc)
			{
				rins.addValue(val_crc);
			}
			for (int i=0;i<keynrs.length;i++)
			{
				rins.addValue( row.getValue(keynrs[i]));
			}
			
			if (log.isRowLevel()) log.logRowlevel(toString(), "rins="+rins.toString());
			
			debug="Set values on insert";
			// INSERT NEW VALUE!
			setValues(rins, prepStatementInsert);
			
			debug="Insert row";
			insertRow(prepStatementInsert);
			
			debug="Retrieve key";
			if (keyfield==null)
			{
				ResultSet keys = null;
				try
				{
					keys=prepStatementInsert.getGeneratedKeys(); // 1 key
					if (keys.next()) val_key.setValue(keys.getDouble(1));
					else 
	                {
	                    throw new KettleDatabaseException("Unable to retrieve auto-increment of combi insert key : "+keyfield+", no fields in resultset");
	                }					
				}
				catch(SQLException ex) 
				{
					throw new KettleDatabaseException("Unable to retrieve auto-increment of combi insert key : "+keyfield, ex);
				}
				finally 
				{
					try 
					{
					    if ( keys != null ) keys.close();
					}
					catch(SQLException ex) 
					{
						throw new KettleDatabaseException("Unable to retrieve auto-increment of combi insert key : "+keyfield, ex);
					}					    
				}
			}
		}
		catch(Exception e)
		{
			log.logError(toString(), Const.getStackTracker(e));
			throw new KettleDatabaseException("Unexpected error in combination insert in part ["+debug+"] : "+e.toString(), e);
			
		}
	}

    public Value getNextSequenceValue(String sequenceName, String keyfield) throws KettleDatabaseException
    {
        return getNextSequenceValue(null, sequenceName, keyfield);
    }
    
	public Value getNextSequenceValue(String schemaName, String sequenceName, String keyfield) throws KettleDatabaseException
	{
        String schemaSequence = databaseMeta.getQuotedSchemaTableCombination(schemaName, sequenceName);
        Value retval=null;
		
		try
		{
			if (pstmt_seq==null)
			{
				pstmt_seq=connection.prepareStatement(databaseMeta.getSeqNextvalSQL(databaseMeta.stripCR(schemaSequence)));
			}
			ResultSet rs=null;
			try 
			{
				rs = pstmt_seq.executeQuery();
			    if (rs.next())
			    {
				    long next = rs.getLong(1);
				    retval=new Value(keyfield, next);
                    retval.setLength(9,0);
			    }
			}
			finally 
			{
			    if ( rs != null ) rs.close();
			}
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Unable to get next value for sequence : "+schemaSequence, ex);
		}
		
		return retval;
	}
	
	public void insertRow(String tableName, Row fields) throws KettleDatabaseException
	{
		prepareInsert(fields, tableName);
		setValuesInsert(fields);
		insertRow();
		closeInsert();
	}

    public String getInsertStatement(String tableName, Row fields)
    {
        return getInsertStatement(null, tableName, fields);
    }

	public String getInsertStatement(String schemaName, String tableName, Row fields)
	{
		StringBuffer ins=new StringBuffer(128);
		
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		ins.append("INSERT INTO ").append(schemaTable).append('(');
		
		// now add the names in the row:
		for (int i=0;i<fields.size();i++)
		{
			if (i>0) ins.append(", ");
			String name = fields.getValue(i).getName();
			ins.append(databaseMeta.quoteField(name));
		}
		ins.append(") VALUES (");
		
		// Add placeholders...
		for (int i=0;i<fields.size();i++)
		{
			if (i>0) ins.append(", ");
			ins.append(" ?");
		}
		ins.append(')');
		
		return ins.toString();
	}

	public void insertRow()
		throws KettleDatabaseException
	{
		insertRow(prepStatementInsert);	
	}

	public void insertRow(boolean batch) throws KettleDatabaseException
    {
        insertRow(prepStatementInsert, batch);
    }

	public void updateRow()
		throws KettleDatabaseException
	{
		insertRow(prepStatementUpdate);	
	}

	public void insertRow(PreparedStatement ps)
		throws KettleDatabaseException
	{
		insertRow(ps, false);
	}
	
	/**
     * @param batchCounter The batchCounter to set.
     */
    public void setBatchCounter(int batchCounter)
    {
        this.batchCounter = batchCounter;
    }
    
    /**
     * @return Returns the batchCounter.
     */
    public int getBatchCounter()
    {
        return batchCounter;
    }
    
    //private long testCounter = 0;

    /**
     * Insert a row into the database using a prepared statement that has all values set.
     * @param ps The prepared statement
     * @param batch True if you want to use batch inserts (size = commitsize)
     * @return true if the rows are safe: if batch of rows was sent to the database OR if a commit was done.
     * @throws KettleDatabaseException
     */
	public boolean insertRow(PreparedStatement ps, boolean batch) throws KettleDatabaseException
	{
	    String debug="insertRow start";
        boolean rowsAreSafe=false;
		try
		{
            // Unique connections and Batch inserts don't mix when you want to roll back on certain databases.
            // That's why we disable the batch insert in that case.
            //
            boolean useBatchInsert = batch && getDatabaseMetaData().supportsBatchUpdates() && databaseMeta.supportsBatchUpdates() && Const.isEmpty(connectionGroup);
            
			//
			// Add support for batch inserts...
			//
		    if (!isAutoCommit())
		    {
				if (useBatchInsert)
				{
				    debug="insertRow add batch";
				    batchCounter++;
					ps.addBatch(); // Add the batch, but don't forget to run the batch
                    //testCounter++;
                    // System.out.println("testCounter is at "+testCounter);
				}
				else
				{
				    debug="insertRow exec update";
					ps.executeUpdate();
				}
		    }
		    else
		    {
		        ps.executeUpdate();
		    }

			written++;
			
			if (!isAutoCommit() && (written%commitsize)==0)
			{
				if (useBatchInsert)
				{
					debug="insertRow executeBatch commit";
                    ps.executeBatch();
					commit();
                    ps.clearBatch();

					batchCounter=0;
				}
				else
				{
				    debug="insertRow normal commit";
                    commit();
				}
                rowsAreSafe=true;
			}
            return rowsAreSafe;
		}
		catch(BatchUpdateException ex)
		{
		    //System.out.println("Batch update exception "+ex.getMessage());
			KettleDatabaseBatchException kdbe = new KettleDatabaseBatchException("Error updating batch", ex);
		    kdbe.setUpdateCounts(ex.getUpdateCounts());
            List exceptions = new ArrayList();
            SQLException nextException;
            while ( (nextException = ex.getNextException())!=null)
            {
                exceptions.add(nextException);
            }
            kdbe.setExceptionsList(exceptions);
		    throw kdbe;
		}
		catch(SQLException ex) 
		{
		    // log.logError(toString(), Const.getStackTracker(ex));
			throw new KettleDatabaseException("Error inserting row", ex);
		}
		catch(Exception e)
		{
		    // System.out.println("Unexpected exception in ["+debug+"] : "+e.getMessage());
			throw new KettleDatabaseException("Unexpected error inserting row in part ["+debug+"]", e);
		}
	}
	
    /**
     * Clears batch of insert prepared statement
     * @deprecated
     * @throws KettleDatabaseException
     */
	public void clearInsertBatch() throws KettleDatabaseException
	{
        clearBatch(prepStatementInsert);
	}
    
    public void clearBatch(PreparedStatement preparedStatement) throws KettleDatabaseException
    {
        try
        {
            preparedStatement.clearBatch();
        }
        catch(SQLException e)
        {
            throw new KettleDatabaseException("Unable to clear batch for prepared statement", e);
        }
    }

	public void insertFinished(boolean batch) throws KettleDatabaseException
	{
	    insertFinished(prepStatementInsert, batch);
		prepStatementInsert = null;
	}
	
	public void insertFinished(PreparedStatement ps, boolean batch) throws KettleDatabaseException
	{		
		try
		{
			if (ps!=null)
			{
			    if (!isAutoCommit())
			    {
					if (batch && getDatabaseMetaData().supportsBatchUpdates() && batchCounter>0)
					{
					    //System.out.println("Executing batch with "+batchCounter+" elements...");
						ps.executeBatch();
						commit();
					}
					else
					{
						commit();
					}
			    }
	
				ps.close();
			}
		}
        catch(BatchUpdateException ex)
        {
            //System.out.println("Batch update exception "+ex.getMessage());
            KettleDatabaseBatchException kdbe = new KettleDatabaseBatchException("Error updating batch", ex);
            kdbe.setUpdateCounts(ex.getUpdateCounts());
            List exceptions = new ArrayList();
            SQLException nextException = ex.getNextException();
            SQLException oldException = null;
            
            // This construction is specifically done for some JDBC drivers, these drivers
            // always return the same exception on getNextException() (and thus go into an infinite loop).
            // So it's not "equals" but != (comments from Sven Boden).
            while ( (nextException != null) && (oldException != nextException) )
            {
                exceptions.add(nextException);
                oldException = nextException;
               	nextException = nextException.getNextException();
            }
            kdbe.setExceptionsList(exceptions);
            throw kdbe;
        }
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to commit connection after having inserted rows.", ex);
		}
	}
	
	

    /**
     * Execute an SQL statement on the database connection (has to be open)
     * @param sql The SQL to execute
     * @return a Result object indicating the number of lines read, deleted, inserted, updated, ...
     * @throws KettleDatabaseException in case anything goes wrong.
     */
	public Result execStatement(String sql) throws KettleDatabaseException
	{
		return execStatement(sql, null);
	}
	
	public Result execStatement(String sql, Row params) throws KettleDatabaseException
	{
        Result result = new Result();
		try
		{
            boolean resultSet;
            int count;
			if (params!=null)
			{
				PreparedStatement prep_stmt = connection.prepareStatement(databaseMeta.stripCR(sql));
				setValues(params, prep_stmt); // set the parameters!
				resultSet = prep_stmt.execute();
                count = prep_stmt.getUpdateCount();
				prep_stmt.close();
			}
			else
			{
                String sqlStripped = databaseMeta.stripCR(sql);
                // log.logDetailed(toString(), "Executing SQL Statement: ["+sqlStripped+"]");
				Statement stmt = connection.createStatement();
                resultSet = stmt.execute(sqlStripped);
                count = stmt.getUpdateCount();
				stmt.close();
			}
            if (resultSet)
            {
                // the result is a resultset, but we don't do anything with it!
                // You should have called something else!
                // log.logDetailed(toString(), "What to do with ResultSet??? (count="+count+")");
            }
            else
            {
                if (count > 0)
                {
                    if (sql.toUpperCase().startsWith("INSERT")) result.setNrLinesOutput((long) count);
                    if (sql.toUpperCase().startsWith("UPDATE")) result.setNrLinesUpdated((long) count);
                    if (sql.toUpperCase().startsWith("DELETE")) result.setNrLinesDeleted((long) count);
                }
            }
            
            // See if a cache needs to be cleared...
            if (sql.toUpperCase().startsWith("ALTER TABLE") || 
                sql.toUpperCase().startsWith("DROP TABLE") ||
                sql.toUpperCase().startsWith("CREATE TABLE")
                )
            {
                DBCache.getInstance().clear(databaseMeta.getName());
            }
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Couldn't execute SQL: "+sql+Const.CR, ex);
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Unexpected error executing SQL: "+Const.CR, e);
		}
        
        return result;
	}

    /**
     * Execute a series of SQL statements, separated by ;
     * 
     * We are already connected...
     
     * Multiple statements have to be split into parts
     * We use the ";" to separate statements...
     *
     * We keep the results in Result object from Jobs
     *
     * @param script The SQL script to be execute
     * @throws KettleDatabaseException In case an error occurs
     * @return A result with counts of the number or records updates, inserted, deleted or read.
     */
	public Result execStatements(String script) throws KettleDatabaseException
	{
        Result result = new Result();
        
		String all = script;
		int from=0;
		int to=0;
		int length = all.length();
		int nrstats = 0;
			
		while (to<length)
		{
			char c = all.charAt(to);
			if (c=='"')
			{
				to++;
				c=' ';
				while (to<length && c!='"') { c=all.charAt(to); to++; }
			}
			else
			if (c=='\'') // skip until next '
			{
				to++;
				c=' ';
				while (to<length && c!='\'') { c=all.charAt(to); to++; }
			}
			else
			if (all.substring(to).startsWith("--"))  // -- means: ignore comment until end of line...
			{
				to++;
				while (to<length && c!='\n' && c!='\r') { c=all.charAt(to); to++; }
			}
			if (c==';' || to>=length-1) // end of statement
			{
				if (to>=length-1) to++; // grab last char also!
                
                String stat;
                if (to<=length) stat = all.substring(from, to);
                else stat = all.substring(from);
                
                // If it ends with a ; remove that ;
                // Oracle for example can't stand it when this happens...
                if (stat.length()>0 && stat.charAt(stat.length()-1)==';')
                {
                    stat = stat.substring(0,stat.length()-1);
                }
                
				if (!Const.onlySpaces(stat))
				{
					String sql=Const.trim(stat);
					if (sql.toUpperCase().startsWith("SELECT"))
					{
						// A Query
						log.logDetailed(toString(), "launch SELECT statement: "+Const.CR+sql);
						
						nrstats++;
						ResultSet rs = null;
						try 
						{
							rs = openQuery(sql);
							if (rs!=null)
							{
								Row r = getRow(rs);
								while (r!=null)
								{
									result.setNrLinesRead(result.getNrLinesRead()+1);
									log.logDetailed(toString(), r.toString());
									r=getRow(rs);
								}
								
							}
							else
							{
                                if (log.isDebug()) log.logDebug(toString(), "Error executing query: "+Const.CR+sql);
							}
						}
						finally 
						{
							try 
							{
							   if ( rs != null ) rs.close();
							}
							catch (SQLException ex )
							{
                                if (log.isDebug()) log.logDebug(toString(), "Error closing query: "+Const.CR+sql);
							}
						}						
					}
                    else // any kind of statement
                    {
                        log.logDetailed(toString(), "launch DDL statement: "+Const.CR+sql);

                        // A DDL statement
                        nrstats++;
                        Result res = execStatement(sql);
                        result.add(res);
                    }
				}
				to++;
				from=to;
			}
			else
			{
				to++;
			}
		}
		
		log.logDetailed(toString(), nrstats+" statement"+(nrstats==1?"":"s")+" executed");
        
        return result;
	}


	public ResultSet openQuery(String sql)
		throws KettleDatabaseException
	{
		return openQuery(sql, null);
	}

    /**
     * Open a query on the database with a set of parameters stored in a Kettle Row
     * @param sql The SQL to launch with question marks (?) as placeholders for the parameters
     * @param params The parameters or null if no parameters are used.
     * @return A JDBC ResultSet
     * @throws KettleDatabaseException when something goes wrong with the query.
     */
	public ResultSet openQuery(String sql, Row params) throws KettleDatabaseException
	{
		return openQuery(sql, params, ResultSet.FETCH_FORWARD);
	}

	public ResultSet openQuery(String sql, Row params, int fetch_mode) throws KettleDatabaseException
	{
		ResultSet res;
		String debug = "Start";
		
		// Create a Statement
		try
		{
			if (params!=null)
			{
				debug = "P create prepared statement (con==null? "+(connection==null)+")";
				pstmt = connection.prepareStatement(databaseMeta.stripCR(sql), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				debug = "P Set values";
				setValues(params); // set the dates etc!
				if (canWeSetFetchSize(pstmt) )  
				{
					debug = "P Set fetchsize";
                    int fs = Const.FETCH_SIZE<=pstmt.getMaxRows()?pstmt.getMaxRows():Const.FETCH_SIZE;
                    
                    // System.out.println("Setting pstmt fetchsize to : "+fs);
                    {
                        if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL && databaseMeta.isStreamingResults())
                        {
                            pstmt.setFetchSize(Integer.MIN_VALUE);
                        }
                        else
                        pstmt.setFetchSize(fs);
                    }
					debug = "P Set fetch direction";
					pstmt.setFetchDirection(fetch_mode);
				} 
				debug = "P Set max rows";
				if (rowlimit>0 && databaseMeta.supportsSetMaxRows()) pstmt.setMaxRows(rowlimit);
				debug = "exec query";
				res = pstmt.executeQuery();
			}
			else
			{
				debug = "create statement";
				sel_stmt = connection.createStatement();
                if (canWeSetFetchSize(sel_stmt)) 
				{
					debug = "Set fetchsize";
                    int fs = Const.FETCH_SIZE<=sel_stmt.getMaxRows()?sel_stmt.getMaxRows():Const.FETCH_SIZE;
                    if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL && databaseMeta.isStreamingResults())
                    {
                        sel_stmt.setFetchSize(Integer.MIN_VALUE);
                    }
                    else
                    {
                        sel_stmt.setFetchSize(fs);
                    }
					debug = "Set fetch direction";
					sel_stmt.setFetchDirection(fetch_mode);
				} 
				debug = "Set max rows";
				if (rowlimit>0 && databaseMeta.supportsSetMaxRows()) sel_stmt.setMaxRows(rowlimit);

				debug = "exec query";
				res=sel_stmt.executeQuery(databaseMeta.stripCR(sql));
			}
			debug = "openQuery : get rowinfo";
            
            // MySQL Hack only. It seems too much for the cursor type of operation on MySQL, to have another cursor opened
            // to get the length of a String field.  So, on MySQL, we ingore the length of Strings in result rows.
            // 
			rowinfo = getRowInfo(res.getMetaData(), databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL);
		}
		catch(SQLException ex)
		{
			// log.logError(toString(), "ERROR executing ["+sql+"]");
			// log.logError(toString(), "ERROR in part: ["+debug+"]");
			// printSQLException(ex);
            throw new KettleDatabaseException("An error occurred executing SQL: "+Const.CR+sql, ex);
		}
		catch(Exception e)
		{
			log.logError(toString(), "ERROR executing query: "+e.toString());
			log.logError(toString(), "ERROR in part: "+debug);
            throw new KettleDatabaseException("An error occurred executing SQL in part ["+debug+"]:"+Const.CR+sql, e);
		}

		return res;
	}

	private boolean canWeSetFetchSize(Statement statement) throws SQLException
    {
        return databaseMeta.isFetchSizeSupported() && 
            ( statement.getMaxRows()>0 || 
              databaseMeta.getDatabaseType() == DatabaseMeta.TYPE_DATABASE_POSTGRES || 
              ( databaseMeta.getDatabaseType() == DatabaseMeta.TYPE_DATABASE_MYSQL && databaseMeta.isStreamingResults() ) 
            );     
    }
    
    public ResultSet openQuery(PreparedStatement ps, Row params) throws KettleDatabaseException
	{
		ResultSet res;
		String debug = "Start";
		
		// Create a Statement
		try
		{
			debug = "OQ Set values";
			setValues(params, ps); // set the parameters!
			
			if (canWeSetFetchSize(ps)) 
			{
				debug = "OQ Set fetchsize";
                int fs = Const.FETCH_SIZE<=ps.getMaxRows()?ps.getMaxRows():Const.FETCH_SIZE;
                if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL && databaseMeta.isStreamingResults())
                {
                    ps.setFetchSize(Integer.MIN_VALUE);
                }
                else
                {
                    ps.setFetchSize(fs);
                }
				
				debug = "OQ Set fetch direction";
				ps.setFetchDirection(ResultSet.FETCH_FORWARD);
			} 
			
			debug = "OQ Set max rows";
			if (rowlimit>0 && databaseMeta.supportsSetMaxRows()) ps.setMaxRows(rowlimit);
			
			debug = "OQ exec query";
			res = ps.executeQuery();

			debug = "OQ getRowInfo()";
			// rowinfo = getRowInfo(res.getMetaData());
            
             // MySQL Hack only. It seems too much for the cursor type of operation on MySQL, to have another cursor opened
            // to get the length of a String field.  So, on MySQL, we ingore the length of Strings in result rows.
            // 
            rowinfo = getRowInfo(res.getMetaData(), databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL);
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("ERROR executing query in part["+debug+"]", ex);
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("ERROR executing query in part["+debug+"]", e);
		}

		return res;
	}
	
	public Row getTableFields(String tablename) throws KettleDatabaseException
	{
		return getQueryFields(databaseMeta.getSQLQueryFields(tablename), false);
	}

	public Row getQueryFields(String sql, boolean param) throws KettleDatabaseException
	{
		return getQueryFields(sql, param, null);
	}
    
	/**
	 * See if the table specified exists by reading
	 * @param tablename The name of the table to check.
	 * @return true if the table exists, false if it doesn't.
	 */
	public boolean checkTableExists(String tablename) throws KettleDatabaseException
	{
		try
		{
			log.logDebug(toString(), "Checking if table ["+tablename+"] exists!");
			
            // Just try to read from the table.
            String sql = databaseMeta.getSQLTableExists(tablename);
            try
            {
                getOneRow(sql);
                return true;
            }
            catch(KettleDatabaseException e)
            {
                return false;
            }
            
            /*
			if (getDatabaseMetaData()!=null)
			{
				ResultSet alltables = getDatabaseMetaData().getTables(null, null, "%" , new String[] { "TABLE", "VIEW", "SYNONYM" } );
				boolean found = false;
				if (alltables!=null)
				{
                    while (alltables.next() && !found)
					{
                        String schemaName = alltables.getString("TABLE_SCHEM");
						String name       = alltables.getString("TABLE_NAME");
						if ( tablename.equalsIgnoreCase(name) || 
                             ( schemaName!=null && tablename.equalsIgnoreCase( databaseMeta.getSchemaTableCombination(schemaName, name)) )
                           )
						{
							log.logDebug(toString(), "table ["+tablename+"] was found!");
							found=true;
						}
					}
					alltables.close();

					return found;
				}
				else
				{
					throw new KettleDatabaseException("Unable to read table-names from the database meta-data.");
				}
			}
			else
			{
				throw new KettleDatabaseException("Unable to get database meta-data from the database.");
			}
            */
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Unable to check if table ["+tablename+"] exists on connection ["+databaseMeta.getName()+"]", e);
		}
	}

    /**
     * Check whether the sequence exists, Oracle only! 
     * @param sequenceName The name of the sequence
     * @return true if the sequence exists.
     */
    public boolean checkSequenceExists(String sequenceName) throws KettleDatabaseException
    {
        return checkSequenceExists(null, sequenceName);
    }
    
	/**
	 * Check whether the sequence exists, Oracle only! 
	 * @param sequenceName The name of the sequence
	 * @return true if the sequence exists.
	 */
	public boolean checkSequenceExists(String schemaName, String sequenceName) throws KettleDatabaseException
	{
		boolean retval=false;
		
		if (!databaseMeta.supportsSequences()) return retval;
		
        String schemaSequence = databaseMeta.getQuotedSchemaTableCombination(schemaName, sequenceName);
		try
		{
			//
			// Get the info from the data dictionary...
			//
			String sql = databaseMeta.getSQLSequenceExists(schemaSequence);
			ResultSet res = openQuery(sql);
			if (res!=null)
			{
				Row row = getRow(res);
				if (row!=null)
				{
					retval=true;
				}
				closeQuery(res);
			}
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Unexpected error checking whether or not sequence ["+schemaSequence+"] exists", e);
		}
		
		return retval;
	}
    
    /**
     * Check if an index on certain fields in a table exists.
     * @param tableName The table on which the index is checked
     * @param idx_fields The fields on which the indexe is checked
     * @return True if the index exists
     */
    public boolean checkIndexExists(String tableName, String idx_fields[]) throws KettleDatabaseException
    {
        return checkIndexExists(null, tableName, idx_fields);
    }
	
	/**
	 * Check if an index on certain fields in a table exists.
	 * @param tablename The table on which the index is checked
	 * @param idx_fields The fields on which the indexe is checked
	 * @return True if the index exists
	 */
	public boolean checkIndexExists(String schemaName, String tableName, String idx_fields[]) throws KettleDatabaseException
	{
        String tablename = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		if (!checkTableExists(tablename)) return false;
		
		log.logDebug(toString(), "CheckIndexExists() tablename = "+tablename+" type = "+databaseMeta.getDatabaseTypeDesc());
		
		boolean exists[] = new boolean[idx_fields.length];
		for (int i=0;i<exists.length;i++) exists[i]=false;
		try
		{
			switch(databaseMeta.getDatabaseType())
			{
			case DatabaseMeta.TYPE_DATABASE_MSSQL:
				{
					//
					// Get the info from the data dictionary...
					//
					StringBuffer sql = new StringBuffer(128);
					sql.append("select i.name table_name, c.name column_name ");
					sql.append("from     sysindexes i, sysindexkeys k, syscolumns c ");
					sql.append("where    i.name = '"+tablename+"' ");
					sql.append("AND      i.id = k.id ");
					sql.append("AND      i.id = c.id ");
					sql.append("AND      k.colid = c.colid ");
					
					ResultSet res = null;
					try 
					{
						res = openQuery(sql.toString());
						if (res!=null)
						{
							Row row = getRow(res);
							while (row!=null)
							{
								String column = row.getString("column_name", "");
								int idx = Const.indexOfString(column, idx_fields);
								if (idx>=0) exists[idx]=true;
								
								row = getRow(res);
							}							
						}
						else
						{
							return false;
						}
					}
					finally
					{
						if ( res != null ) closeQuery(res);
					}
				}
				break;
				
				
			case DatabaseMeta.TYPE_DATABASE_ORACLE:
				{
					//
					// Get the info from the data dictionary...
					//
					String sql = "SELECT * FROM USER_IND_COLUMNS WHERE TABLE_NAME = '"+tableName+"'";
					ResultSet res = null;
					try {
						res = openQuery(sql);
						if (res!=null)
						{
							Row row = getRow(res);
							while (row!=null)
							{
								String column = row.getString("COLUMN_NAME", "");
								int idx = Const.indexOfString(column, idx_fields);
								if (idx>=0) 
								{
									exists[idx]=true;
								}
								
								row = getRow(res);
							}
							
						}
						else
						{
							return false;
						}
					}
					finally
					{
						if ( res != null ) closeQuery(res);
					}
				}
				break;
				
			case DatabaseMeta.TYPE_DATABASE_ACCESS:
				{
					// Get a list of all the indexes for this table
			        ResultSet indexList = null;
			        try 
			        {
			        	indexList = getDatabaseMetaData().getIndexInfo(null,null,tablename,false,true);
			        	while (indexList.next())
			        	{
			        		// String tablen  = indexList.getString("TABLE_NAME");
			        		// String indexn  = indexList.getString("INDEX_NAME");
			        		String column  = indexList.getString("COLUMN_NAME");
			        		// int    pos     = indexList.getShort("ORDINAL_POSITION");
			        		// int    type    = indexList.getShort("TYPE");
			        		
			        		int idx = Const.indexOfString(column, idx_fields);
			        		if (idx>=0)
			        		{
			        			exists[idx]=true;
			        		}
			        	}
			        }
			        finally 
			        {
			        	if ( indexList != null ) indexList.close();		   				
				    }
				}
				break;

			default:
				{
					// Get a list of all the indexes for this table
			        ResultSet indexList = null;
			        try  
			        {
			        	indexList = getDatabaseMetaData().getIndexInfo(null,null,tablename,false,true);
			        	while (indexList.next())
			        	{
			        		// String tablen  = indexList.getString("TABLE_NAME");
			        		// String indexn  = indexList.getString("INDEX_NAME");
			        		String column  = indexList.getString("COLUMN_NAME");
			        		// int    pos     = indexList.getShort("ORDINAL_POSITION");
			        		// int    type    = indexList.getShort("TYPE");
			        		
			        		int idx = Const.indexOfString(column, idx_fields);
			        		if (idx>=0)
			        		{
			        			exists[idx]=true;
			        		}
			        	}
			        }
			        finally 
			        {
			            if ( indexList != null ) indexList.close();
			        }
				}
				break;
			}
			
	        // See if all the fields are indexed...
	        boolean all=true;
	        for (int i=0;i<exists.length && all;i++) if (!exists[i]) all=false;
	        
			return all;
		}
		catch(Exception e)
		{
            log.logError(toString(), Const.getStackTracker(e));
			throw new KettleDatabaseException("Unable to determine if indexes exists on table ["+tablename+"]", e);
		}
	}

    public String getCreateIndexStatement(String tablename, String indexname, String idx_fields[], boolean tk, boolean unique, boolean bitmap, boolean semi_colon)
    {
        return getCreateIndexStatement(null, tablename, indexname, idx_fields, tk, unique, bitmap, semi_colon);
    }
    
	public String getCreateIndexStatement(String schemaname, String tablename, String indexname, String idx_fields[], boolean tk, boolean unique, boolean bitmap, boolean semi_colon)
	{
		String cr_index="";
		
		cr_index += "CREATE ";
	
		if (unique || ( tk && databaseMeta.getDatabaseType() == DatabaseMeta.TYPE_DATABASE_SYBASE))
			cr_index += "UNIQUE ";
		
		if (bitmap && databaseMeta.supportsBitmapIndex()) 
			cr_index += "BITMAP ";
		
		cr_index += "INDEX "+databaseMeta.quoteField(indexname)+Const.CR+" ";
		cr_index += "ON ";
  	    // assume table has already been quoted (and possibly includes schema)
		cr_index += tablename;
		cr_index += Const.CR + "( "+Const.CR;
		for (int i=0;i<idx_fields.length;i++)
		{
			if (i>0) cr_index+=", "; else cr_index+="  ";
			cr_index += databaseMeta.quoteField(idx_fields[i])+Const.CR;
		}
		cr_index+=")"+Const.CR;
		
		if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_ORACLE &&
			databaseMeta.getIndexTablespace()!=null && databaseMeta.getIndexTablespace().length()>0)
		{
			cr_index+="TABLESPACE "+databaseMeta.quoteField(databaseMeta.getIndexTablespace());
		}
		
		if (semi_colon)
		{
			cr_index+=";"+Const.CR;
		}

		return cr_index;
	}
	
    public String getCreateSequenceStatement(String sequence, long start_at, long increment_by, long max_value, boolean semi_colon)
    {
        return getCreateSequenceStatement(null, sequence, start_at, increment_by, max_value, semi_colon);
    }
    
	public String getCreateSequenceStatement(String schemaName, String sequenceName, long start_at, long increment_by, long max_value, boolean semi_colon)
	{
		String cr_seq="";
		
		if (Const.isEmpty(sequenceName)) return cr_seq;
		
		if (databaseMeta.supportsSequences())
		{
            String schemaSequence = databaseMeta.getQuotedSchemaTableCombination(schemaName, sequenceName);
			cr_seq += "CREATE SEQUENCE "+schemaSequence+" "+Const.CR;  // Works for both Oracle and PostgreSQL :-)
			cr_seq += "START WITH "+start_at+" "+Const.CR;
			cr_seq += "INCREMENT BY "+increment_by+" "+Const.CR;
			if (max_value>0) cr_seq += "MAXVALUE "+max_value+Const.CR;
			
			if (semi_colon) cr_seq+=";"+Const.CR;
		}

		return cr_seq;
	}
	
	public Row getQueryFields(String sql, boolean param, Row inform) throws KettleDatabaseException
	{
		Row fields;
		DBCache dbcache = DBCache.getInstance();
		
		DBCacheEntry entry=null;
		
		// Check the cache first!
		if (dbcache!=null)
		{
			entry = new DBCacheEntry(databaseMeta.getName(), sql);
			fields = dbcache.get(entry);
			if (fields!=null) 
			{
				return fields;
			} 
		}
		if (connection==null) return null; // Cache test without connect.

		// No cache entry found 
		
		String debug="";
		try
		{
			if (inform==null 
					// Hack for MSSQL jtds 1.2 when using xxx NOT IN yyy we have to use a prepared statement (see BugID 3214)
					&& databaseMeta.getDatabaseType()!=DatabaseMeta.TYPE_DATABASE_MSSQL
					)
			{
				debug="inform==null";
				sel_stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				
				debug="isFetchSizeSupported()";
				if (databaseMeta.isFetchSizeSupported() && sel_stmt.getMaxRows()>=1)
				{
					debug = "Set fetchsize";
                    if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL)
                    {
                        sel_stmt.setFetchSize(Integer.MIN_VALUE);
                    }
                    else
                    {
                        sel_stmt.setFetchSize(1);
                    }
				}
				debug = "Set max rows to 1";
                if (databaseMeta.supportsSetMaxRows()) sel_stmt.setMaxRows(1);
				
				debug = "exec query";
				ResultSet r=sel_stmt.executeQuery(databaseMeta.stripCR(sql));
                debug = "getQueryFields get row info";
				fields = getRowInfo(r.getMetaData());
				debug="close resultset";
				r.close();
				debug="close statement";
				sel_stmt.close();
				sel_stmt=null;
			}
			else
			{
				debug="prepareStatement";
				PreparedStatement ps = connection.prepareStatement(databaseMeta.stripCR(sql));
				if (param)
				{
					Row par = inform;
					
					debug="getParameterMetaData()";
					if (par==null || par.isEmpty()) par = getParameterMetaData(ps);
                    
					debug="getParameterMetaData()";
					if (par==null || par.isEmpty()) par = getParameterMetaData(sql, inform);
	
					setValues(par, ps);
				}
				debug="executeQuery()";
				ResultSet r = ps.executeQuery();
				debug="getRowInfo";
				fields=getRowInfo(ps.getMetaData());
				debug="close resultset";
				r.close();
				debug="close preparedStatement";
				ps.close();
			}
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Couldn't get field info from ["+sql+"]"+Const.CR+"Location: "+debug, ex);
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Couldn't get field info in part ["+debug+"]", e);
		}
		
		// Store in cache!!
		if (dbcache!=null && entry!=null)
		{
			if (fields!=null)
			{
				dbcache.put(entry, fields);
			}
		}
		
		return fields;
	}


	public void closeQuery(ResultSet res) throws KettleDatabaseException
	{
		// close everything involved in the query!
		try
		{
			if (res!=null) res.close();
			if (sel_stmt!=null) { sel_stmt.close(); sel_stmt=null; } 
			if (pstmt!=null) { pstmt.close(); pstmt=null;} 
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Couldn't close query: resultset or prepared statements", ex);
		}
	}

    private Row getRowInfo(ResultSetMetaData rm) throws KettleDatabaseException
    {
        return getRowInfo(rm, false);
    }

	/**
	 * Build the row using ResultSetMetaData rsmd
     * @param rm The resultset metadata to inquire
     * @param ignoreLength true if you want to ignore the length (workaround for MySQL bug/problem)
	 */
	private Row getRowInfo(ResultSetMetaData rm, boolean ignoreLength) throws KettleDatabaseException
	{
		int nrcols;
		int i;
		String name;
        int fieldNr=1;
		
		if (rm==null) return null;
		
		rowinfo = new Row();
		
		try
		{
			nrcols=rm.getColumnCount();	
			for (i=1;i<=nrcols;i++)
			{
				name=new String(rm.getColumnName(i));
                
                // Check the name, sometimes it's empty.
                //
                if (Const.isEmpty(name) || Const.onlySpaces(name))
                {
                    name = "Field"+fieldNr;
                    fieldNr++;
                }
                
				Value v = getValueFromSQLType(name, rm, i, ignoreLength);
				rowinfo.addValue(v);			
			}
			return rowinfo;
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Error getting row information from database: ", ex);
		}
	}

	public Value getValueFromSQLType(String name, ResultSetMetaData rm, int i, boolean ignoreLength) throws SQLException
    {
        int length=-1; 
        int precision=-1;
        int valtype=Value.VALUE_TYPE_NONE;

        int type = rm.getColumnType(i);
        switch(type)
        {
        case java.sql.Types.CHAR:
        case java.sql.Types.VARCHAR: 
        case java.sql.Types.LONGVARCHAR:  // Character Large Object
            valtype=Value.VALUE_TYPE_STRING;
            if (!ignoreLength) length=rm.getColumnDisplaySize(i);
            // System.out.println("Display of "+name+" = "+precision);
            // System.out.println("Precision of "+name+" = "+rm.getPrecision(i));
            // System.out.println("Scale of "+name+" = "+rm.getScale(i));
            break;
            
        case java.sql.Types.CLOB:  
            valtype=Value.VALUE_TYPE_STRING;
            length=DatabaseMeta.CLOB_LENGTH;
            break;

        case java.sql.Types.BIGINT:
            valtype=Value.VALUE_TYPE_INTEGER;
            precision=0;   // Max 9.223.372.036.854.775.807
            length=15;
            break;
            
        case java.sql.Types.INTEGER:
            valtype=Value.VALUE_TYPE_INTEGER;
            precision=0;    // Max 2.147.483.647
            length=9;
            break;
            
        case java.sql.Types.SMALLINT:
            valtype=Value.VALUE_TYPE_INTEGER;
            precision=0;   // Max 32.767
            length=4;
            break;
            
        case java.sql.Types.TINYINT: 
            valtype=Value.VALUE_TYPE_INTEGER;
            precision=0;   // Max 127
            length=2;
            break;
            
        case java.sql.Types.DECIMAL:
        case java.sql.Types.DOUBLE:
        case java.sql.Types.FLOAT:
        case java.sql.Types.REAL:
        case java.sql.Types.NUMERIC:
            valtype=Value.VALUE_TYPE_NUMBER;
            length=rm.getPrecision(i); 
            precision=rm.getScale(i);
            if (length    >=126) length=-1;
            if (precision >=126) precision=-1;
            
            if (type==java.sql.Types.DOUBLE || type==java.sql.Types.FLOAT || type==java.sql.Types.REAL)
            {
                if (precision==0) 
                {
                    precision=-1; // precision is obviously incorrect if the type if Double/Float/Real
                }
                
                // If were dealing with Postgres and double precision types 
                if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_POSTGRES && type==java.sql.Types.DOUBLE && precision==16 && length==16)
                {
                    precision=-1;
                    length=-1;
                }
                
                // MySQL: max resolution is double precision floating point (double)
                // The (12,31) that is given back is not correct
                if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL)
                {
                    precision=-1;
                    length=-1;
                }
            }
            else
            {
                if (precision==0 && length<18 && length>0)  // Among others Oracle is affected here.  
                {
                    valtype=Value.VALUE_TYPE_INTEGER;
                }
            }
            if (length>18 || precision>18) valtype=Value.VALUE_TYPE_BIGNUMBER;
            
            if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_ORACLE)
            {
            	if (precision == 0 && length == 38 )
            	{
            		valtype=Value.VALUE_TYPE_INTEGER;
            	}
                if (precision<=0 && length<=0) // undefined size: BIGNUMBER
                {
                    valtype=Value.VALUE_TYPE_BIGNUMBER;
                    length=-1;
                    precision=-1;
                }
            }
            break;

        case java.sql.Types.DATE:
        case java.sql.Types.TIME:
        case java.sql.Types.TIMESTAMP: 
            valtype=Value.VALUE_TYPE_DATE; 
            break;

        case java.sql.Types.BOOLEAN:
        case java.sql.Types.BIT:
            valtype=Value.VALUE_TYPE_BOOLEAN;
            break;
            
        case java.sql.Types.BINARY:
        case java.sql.Types.BLOB:
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            valtype=Value.VALUE_TYPE_BINARY;
            
            if (databaseMeta.getDatabaseType() == DatabaseMeta.TYPE_DATABASE_DB2 &&
                (2 * rm.getPrecision(i)) == rm.getColumnDisplaySize(i)) 
            {
                // set the length for "CHAR(X) FOR BIT DATA"
                length = rm.getPrecision(i);
            }
            else
            if (databaseMeta.getDatabaseType() == DatabaseMeta.TYPE_DATABASE_ORACLE &&
                ( type==java.sql.Types.VARBINARY || type==java.sql.Types.LONGVARBINARY )
               )
            {
                // set the length for Oracle "RAW" or "LONGRAW" data types
                valtype = Value.VALUE_TYPE_STRING;
                length = rm.getColumnDisplaySize(i);
            }
            else
            {
                length=-1; 
            }
            precision=-1;
            break;

        default:
            valtype=Value.VALUE_TYPE_STRING;
            precision=rm.getScale(i);                    
            break;
        }
        // TODO: grab the comment as a description to the field, later
        // comment=rm.getColumnLabel(i);

        Value v=new Value(name, valtype);
        v.setLength(length, precision);
        
        return v;
    }

    public boolean absolute(ResultSet rs, int position) throws KettleDatabaseException
	{
		try
		{
			return rs.absolute(position);
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Unable to move resultset to position "+position, e);
		}
	}

	public boolean relative(ResultSet rs, int rows)  throws KettleDatabaseException
	{
		try
		{
			return rs.relative(rows);
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Unable to move the resultset forward "+rows+" rows", e);
		}
	}

	public void afterLast(ResultSet rs)
		throws KettleDatabaseException
	{
		try
		{
			rs.afterLast();
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Unable to move resultset to after the last position", e);
		}
	}

	public void first(ResultSet rs) throws KettleDatabaseException
	{
		try
		{
			rs.first();
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Unable to move resultset to the first position", e);
		}
	}

	/**
	 * Get a row from the resultset.
	 * @param rs The resultset to get the row from
	 * @return one row or null if no row was found on the resultset or if an error occurred.
	 */
	public Row getRow(ResultSet rs) throws KettleDatabaseException
	{
        ResultSetMetaData rsmd = null;
		try
		{
			rsmd = rs.getMetaData();
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Unable to retrieve metadata from resultset", e);
		}

        if (rowinfo==null)
        {
			rowinfo = getRowInfo(rsmd);
        }

		return getRow(rs, rsmd, rowinfo);
	}

	/**
	 * Get a row from the resultset.
	 * @param rs The resultset to get the row from
	 * @return one row or null if no row was found on the resultset or if an error occurred.
	 */
	public Row getRow(ResultSet rs, ResultSetMetaData resultSetMetaData, Row rowInfo) throws KettleDatabaseException
	{
		Row row;
		int nrcols, i;
		Value val;
		
		try
		{
			nrcols=resultSetMetaData.getColumnCount();
						
			if (rs.next())
			{
				row=new Row();
				for (i=0;i<nrcols;i++)
				{
					val=new Value(rowInfo.getValue(i)); // copy info from meta-data.
					switch(val.getType())
					{
					case Value.VALUE_TYPE_BOOLEAN   : val.setValue( rs.getBoolean(i+1) ); break;
					case Value.VALUE_TYPE_NUMBER    : val.setValue( rs.getDouble(i+1) ); break;
                    case Value.VALUE_TYPE_BIGNUMBER : val.setValue( rs.getBigDecimal(i+1) ); break;
					case Value.VALUE_TYPE_INTEGER   : val.setValue( rs.getLong(i+1) ); break;
					case Value.VALUE_TYPE_STRING    : val.setValue( rs.getString(i+1) ); break;
					case Value.VALUE_TYPE_BINARY    : 
                        {
                            if (databaseMeta.supportsGetBlob())
                            {
                                Blob blob = rs.getBlob(i+1);
                                if (blob!=null)
                                {
                                    val.setValue( blob.getBytes(1L, (int)blob.length()) );
                                }
                                else
                                {
                                    val.setNull();
                                }
                            }
                            else
                            {
                                val.setValue( rs.getBytes(i+1) );
                            }
                        }
                        break;
					case Value.VALUE_TYPE_DATE      :
                        if (databaseMeta.supportsTimeStampToDateConversion())
                        {
                            val.setValue( rs.getTimestamp(i+1) ); break;
                        }
                        else
                        {
                            val.setValue( rs.getDate(i+1) ); break;
                        }
					default: break;
					}
					if (rs.wasNull()) val.setNull(); // null value!
					
					row.addValue(val);
				}
			}
			else
			{
				row=null;
			}
			
			return row;
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Couldn't get row from result set", ex);
		}
	}

	public void printSQLException(SQLException ex)
	{
		log.logError(toString(), "==> SQLException: ");
		while (ex != null) 
		{
			log.logError(toString(), "Message:   " + ex.getMessage ());
			log.logError(toString(), "SQLState:  " + ex.getSQLState ());
			log.logError(toString(), "ErrorCode: " + ex.getErrorCode ());
			ex = ex.getNextException();
			log.logError(toString(), "");
		}
	}

	public void setLookup(String table, String codes[], String condition[], 
            String gets[], String rename[], String orderby
            ) throws KettleDatabaseException
    {
		setLookup(table, codes, condition, gets, rename, orderby, false);
    }
    
    public void setLookup(String schema, String table, String codes[], String condition[], 
            String gets[], String rename[], String orderby
            ) throws KettleDatabaseException
    {
        setLookup(schema, table, codes, condition, gets, rename, orderby, false);
    }

    public void setLookup(String tableName, String codes[], String condition[], 
            String gets[], String rename[], String orderby,
            boolean checkForMultipleResults) throws KettleDatabaseException
    {
        setLookup(null, tableName, codes, condition, gets, rename, orderby, checkForMultipleResults);
    }

	// Lookup certain fields in a table
	public void setLookup(String schemaName, String tableName, String codes[], String condition[], 
	                      String gets[], String rename[], String orderby,
	                      boolean checkForMultipleResults) throws KettleDatabaseException
	{
        String table = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
        
		String sql = "SELECT ";
		
		for (int i=0;i<gets.length;i++)
		{
			if (i!=0) sql += ", ";
			sql += databaseMeta.quoteField(gets[i]);
			if (rename!=null && rename[i]!=null && !gets[i].equalsIgnoreCase(rename[i]))
			{
				sql+=" AS "+databaseMeta.quoteField(rename[i]);
			}
		}

		sql += " FROM "+table+" WHERE ";

		for (int i=0;i<codes.length;i++)
		{
			if (i!=0) sql += " AND ";
			sql += databaseMeta.quoteField(codes[i]);
			if ("BETWEEN".equalsIgnoreCase(condition[i]))
			{
				sql+=" BETWEEN ? AND ? ";
			}
			else
			if ("IS NULL".equalsIgnoreCase(condition[i]) || "IS NOT NULL".equalsIgnoreCase(condition[i]))
			{
				sql+=" "+condition[i]+" ";
			}
			else
			{
				sql+=" "+condition[i]+" ? ";
			}
		}

		if (orderby!=null && orderby.length()!=0)
		{
			sql += " ORDER BY "+orderby;
		}

		try
		{
			log.logDetailed(toString(), "Setting preparedStatement to ["+sql+"]");
			prepStatementLookup=connection.prepareStatement(databaseMeta.stripCR(sql));
			if (!checkForMultipleResults && databaseMeta.supportsSetMaxRows())
			{
				prepStatementLookup.setMaxRows(1); // alywas get only 1 line back!
			}
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to prepare statement for update ["+sql+"]", ex);
		}
	}

    public boolean prepareUpdate(String table, String codes[], String condition[], String sets[])
    {
        return prepareUpdate(null, table, codes, condition, sets);
    }
    
	// Lookup certain fields in a table
	public boolean prepareUpdate(String schemaName, String tableName, String codes[], String condition[], String sets[])
	{
		StringBuffer sql = new StringBuffer(128);

        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
        
		sql.append("UPDATE ").append(schemaTable).append(Const.CR).append("SET ");

		for (int i=0;i<sets.length;i++)
		{
			if (i!=0) sql.append(",   ");
			sql.append(databaseMeta.quoteField(sets[i]));
			sql.append(" = ?").append(Const.CR);
		}

		sql.append("WHERE ");

		for (int i=0;i<codes.length;i++)
		{
			if (i!=0) sql.append("AND   ");
			sql.append(databaseMeta.quoteField(codes[i]));
			if ("BETWEEN".equalsIgnoreCase(condition[i]))
			{
				sql.append(" BETWEEN ? AND ? ");
			}
			else
			if ("IS NULL".equalsIgnoreCase(condition[i]) || "IS NOT NULL".equalsIgnoreCase(condition[i]))
			{
				sql.append(' ').append(condition[i]).append(' ');
			}
			else
			{
				sql.append(' ').append(condition[i]).append(" ? ");
			}
		}

		try
		{
			String s = sql.toString();
			log.logDetailed(toString(), "Setting update preparedStatement to ["+s+"]");
			prepStatementUpdate=connection.prepareStatement(databaseMeta.stripCR(s));
		}
		catch(SQLException ex) 
		{
			printSQLException(ex);
			return false;
		}

		return true;
	}


    /**
     * Prepare a delete statement by giving it the tablename, fields and conditions to work with.
     * @param table The table-name to delete in
     * @param codes  
     * @param condition
     * @return true when everything went OK, false when something went wrong.
     */
    public boolean prepareDelete(String table, String codes[], String condition[])
    {
        return prepareDelete(null, table, codes, condition);
    }
    
    /**
     * Prepare a delete statement by giving it the tablename, fields and conditions to work with.
     * @param schemaName the schema-name to delete in
     * @param tableName The table-name to delete in
     * @param codes  
     * @param condition
     * @return true when everything went OK, false when something went wrong.
     */
    public boolean prepareDelete(String schemaName, String tableName, String codes[], String condition[])
    {
        String sql;

        String table = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
        sql = "DELETE FROM "+table+Const.CR;
        sql+= "WHERE ";
        
        for (int i=0;i<codes.length;i++)
        {
            if (i!=0) sql += "AND   ";
            sql += codes[i];
            if ("BETWEEN".equalsIgnoreCase(condition[i]))
            {
                sql+=" BETWEEN ? AND ? ";
            }
            else
            if ("IS NULL".equalsIgnoreCase(condition[i]) || "IS NOT NULL".equalsIgnoreCase(condition[i]))
            {
                sql+=" "+condition[i]+" ";
            }
            else
            {
                sql+=" "+condition[i]+" ? ";
            }
        }

        try
        {
            log.logDetailed(toString(), "Setting update preparedStatement to ["+sql+"]");
            prepStatementUpdate=connection.prepareStatement(databaseMeta.stripCR(sql));
        }
        catch(SQLException ex) 
        {
            printSQLException(ex);
            return false;
        }

        return true;
    }

    public void setProcLookup(String proc, String arg[], String argdir[], int argtype[], String returnvalue, int returntype)
		throws KettleDatabaseException
	{
		String sql;
		int pos=0;
		
		sql = "{ ";
		if (returnvalue!=null && returnvalue.length()!=0)
		{
			sql+="? = ";
		}
		sql+="call "+proc+" ";
		
		if (arg.length>0) sql+="(";
		
		for (int i=0;i<arg.length;i++)
		{
			if (i!=0) sql += ", ";
			sql += " ?";
		}
		
		if (arg.length>0) sql+=")"; 
		
		sql+="}";
		
		try
		{
			log.logDetailed(toString(), "DBA setting callableStatement to ["+sql+"]");
			cstmt=connection.prepareCall(sql);
			pos=1;
			if (!Const.isEmpty(returnvalue))
			{
				switch(returntype)
				{
				case Value.VALUE_TYPE_NUMBER    : cstmt.registerOutParameter(pos, java.sql.Types.DOUBLE); break;
                case Value.VALUE_TYPE_BIGNUMBER : cstmt.registerOutParameter(pos, java.sql.Types.DECIMAL); break;
				case Value.VALUE_TYPE_INTEGER   : cstmt.registerOutParameter(pos, java.sql.Types.BIGINT); break;
				case Value.VALUE_TYPE_STRING    : cstmt.registerOutParameter(pos, java.sql.Types.VARCHAR);	break;
				case Value.VALUE_TYPE_DATE      : cstmt.registerOutParameter(pos, java.sql.Types.TIMESTAMP); break;
				case Value.VALUE_TYPE_BOOLEAN   : cstmt.registerOutParameter(pos, java.sql.Types.BOOLEAN); break;
				default: break;
				}
				pos++;
			}
			for (int i=0;i<arg.length;i++)
			{
				if (argdir[i].equalsIgnoreCase("OUT") || argdir[i].equalsIgnoreCase("INOUT"))
				{
					switch(argtype[i])
					{
					case Value.VALUE_TYPE_NUMBER    : cstmt.registerOutParameter(i+pos, java.sql.Types.DOUBLE); break;
                    case Value.VALUE_TYPE_BIGNUMBER : cstmt.registerOutParameter(i+pos, java.sql.Types.DECIMAL); break;
					case Value.VALUE_TYPE_INTEGER   : cstmt.registerOutParameter(i+pos, java.sql.Types.BIGINT); break;
					case Value.VALUE_TYPE_STRING    : cstmt.registerOutParameter(i+pos, java.sql.Types.VARCHAR); break;
					case Value.VALUE_TYPE_DATE      : cstmt.registerOutParameter(i+pos, java.sql.Types.TIMESTAMP); break;
					case Value.VALUE_TYPE_BOOLEAN   : cstmt.registerOutParameter(i+pos, java.sql.Types.BOOLEAN); break;
					default: break;
					}
				}
			}
		}
		catch(SQLException ex)
		{
			throw new KettleDatabaseException("Unable to prepare database procedure call", ex);
		}
	}
	
    public boolean setDimLookup(String table, String keys[], String tk, String version, String extra[], String extraRename[], String datefrom, String dateto, boolean performsCaching) throws KettleDatabaseException
    {
        return setDimLookup(null, table, keys, tk, version, extra, extraRename, datefrom, dateto, performsCaching);
    }

    /*
     * table: dimension table keys[]: which dim-fields do we use to look up key? retval: name of the key to return
     * datefield: do we have a datefield? datefrom, dateto: date-range, if any.
     */
	public boolean setDimLookup(String schemaName, String tableName, 
								String keys[], String tk, String version, String extra[], 
                                String extraRename[], String datefrom, String dateto,
                                boolean performsCaching
							   ) throws KettleDatabaseException
	{
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		String sql;
	
		/* 
		 * SELECT <tk>, <version>, ... 
		 * FROM <table> 
		 * WHERE key1=keys[1] 
		 * AND key2=keys[2] ...
		 * AND <datefield> BETWEEN <datefrom> AND <dateto>
		 * ;
		 * 
		 */
		sql = "SELECT "+databaseMeta.quoteField(tk)+", "+databaseMeta.quoteField(version);
		
		if (extra!=null)
		{
			for (int i=0;i<extra.length;i++)
			{
				if (extra[i]!=null && extra[i].length()!=0)
                {
					sql+=", "+databaseMeta.quoteField(extra[i]);
                    if (extraRename[i]!=null && 
                        extraRename[i].length()>0 && 
                        !extra[i].equals(extraRename[i]))
                    {
                        sql+=" AS "+databaseMeta.quoteField(extraRename[i]);
                    }
                }
                
			}
		}
        if (performsCaching)
        {
            sql+=", "+databaseMeta.quoteField(datefrom)+", "+databaseMeta.quoteField(dateto);
        }
		
		sql+= " FROM "+schemaTable+" WHERE ";
		
		for (int i=0;i<keys.length;i++)
		{
			if (i!=0) sql += " AND ";
			sql += databaseMeta.quoteField(keys[i])+" = ? ";
		}
		
		sql += " AND ? >= "+databaseMeta.quoteField(datefrom)+" AND ? < "+databaseMeta.quoteField(dateto);
	
		try
		{
			log.logDetailed(toString(), "Dimension Lookup setting preparedStatement to ["+sql+"]");
			prepStatementLookup=connection.prepareStatement(databaseMeta.stripCR(sql));
			if (databaseMeta.supportsSetMaxRows())
            {
                prepStatementLookup.setMaxRows(1); // alywas get only 1 line back!
            }
            if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MYSQL)
            {
                prepStatementLookup.setFetchSize(0); // Make sure to DISABLE Streaming Result sets
            }
            log.logDetailed(toString(), "Finished preparing dimension lookup statement.");
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to prepare dimension lookup", ex);
		}
		
		return true;
	}

    /*  CombinationLookup
     * table: dimension table
     * keys[]: which dim-fields do we use to look up key?
     * retval: name of the key to return
     */
    public void setCombiLookup(String tableName, String keys[], String retval, boolean crc, String crcfield) throws KettleDatabaseException
    {
        setCombiLookup(null, tableName, keys, retval, crc, crcfield);
    }

	/*  CombinationLookup
	 * table: dimension table
	 * keys[]: which dim-fields do we use to look up key?
	 * retval: name of the key to return
	 */
	public void setCombiLookup(String schemaName, String tableName, String keys[], String retval, boolean crc, String crcfield) throws KettleDatabaseException
	{
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
        
		StringBuffer sql = new StringBuffer(100);
		boolean comma;
		
		/* 
		 * SELECT <retval> 
		 * FROM   <table> 
		 * WHERE  ( ( <key1> = ? ) OR ( <key1> IS NULL AND ? IS NULL ) )  
		 * AND    ( ( <key2> = ? ) OR ( <key1> IS NULL AND ? IS NULL ) )  
		 * ...
		 * ;
		 * 
		 * OR
		 * 
		 * SELECT <retval> 
		 * FROM   <table> 
		 * WHERE  <crcfield> = ?  
		 * AND    ( ( <key1> = ? ) OR ( <key1> IS NULL AND ? IS NULL ) )
		 * AND    ( ( <key2> = ? ) OR ( <key1> IS NULL AND ? IS NULL ) )
		 * ...
		 * ;
		 * 
		 */
		sql.append("SELECT ").append(databaseMeta.quoteField(retval)).append(Const.CR);
        sql.append("FROM ").append(schemaTable).append(Const.CR);
        sql.append("WHERE ");
		comma=false;
		
		if (crc)
		{
			sql.append(databaseMeta.quoteField(crcfield)).append(" = ? ").append(Const.CR);
			comma=true;
		}
		else
		{
			sql.append("( ( ");
		}
		
		for (int i=0;i<keys.length;i++)
		{
			if (comma)
			{
				sql.append(" AND ( ( ");
			}
			else
			{ 
				comma=true; 
			}
			sql.append(databaseMeta.quoteField(keys[i])).append(" = ? ) OR ( ").append(databaseMeta.quoteField(keys[i]));
            sql.append(" IS NULL AND ");
            if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_DB2)
            {
                sql.append("CAST(? AS VARCHAR(256)) IS NULL");
            }
            else
            {
                sql.append("? IS NULL");
            }
            
            sql.append(" ) )");
            sql.append(Const.CR);
		}
		
		try
		{
			String sqlStatement = sql.toString();
            if (log.isDebug()) log.logDebug(toString(), "preparing combi-lookup statement:"+Const.CR+sqlStatement);
			prepStatementLookup=connection.prepareStatement(databaseMeta.stripCR(sqlStatement));
			if (databaseMeta.supportsSetMaxRows())
            {
                prepStatementLookup.setMaxRows(1); // alywas get only 1 line back!
            }
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to prepare combi-lookup statement", ex);
		}
	}

	public Row callProcedure(String arg[], String argdir[], int argtype[], 
	                         String resultname, int resulttype)
		throws KettleDatabaseException
	{
		Row ret;
		try
		{
			cstmt.execute();
			
			ret=new Row();
			int pos=1;
			if (resultname!=null && resultname.length()!=0)
			{
				Value v=new Value(resultname, Value.VALUE_TYPE_NONE);
				switch(resulttype)
				{
				case Value.VALUE_TYPE_BOOLEAN   : v.setValue( cstmt.getBoolean(pos)   ); break;
				case Value.VALUE_TYPE_NUMBER    : v.setValue( cstmt.getDouble(pos)    ); break;
                case Value.VALUE_TYPE_BIGNUMBER : v.setValue( cstmt.getBigDecimal(pos)); break;
				case Value.VALUE_TYPE_INTEGER   : v.setValue( cstmt.getLong(pos)      ); break;
				case Value.VALUE_TYPE_STRING    : v.setValue( cstmt.getString(pos)    ); break;
				case Value.VALUE_TYPE_DATE      : v.setValue( cstmt.getTimestamp(pos) ); break;
				}
				ret.addValue(v);
				pos++;
			}
			for (int i=0;i<arg.length;i++)
			{
				if (argdir[i].equalsIgnoreCase("OUT") || argdir[i].equalsIgnoreCase("INOUT"))
				{
					Value v=new Value(arg[i], Value.VALUE_TYPE_NONE);
					switch(argtype[i])
					{
					case Value.VALUE_TYPE_BOOLEAN   : v.setValue( cstmt.getBoolean(pos+i)   ); break;
					case Value.VALUE_TYPE_NUMBER    : v.setValue( cstmt.getDouble(pos+i)    ); break;
                    case Value.VALUE_TYPE_BIGNUMBER : v.setValue( cstmt.getBigDecimal(pos+i)); break;
					case Value.VALUE_TYPE_INTEGER   : v.setValue( cstmt.getLong(pos+i)      ); break;
					case Value.VALUE_TYPE_STRING    : v.setValue( cstmt.getString(pos+i)    ); break;
					case Value.VALUE_TYPE_DATE      : v.setValue( cstmt.getTimestamp(pos+i) ); break;
					}
					ret.addValue(v);
				}
			}
			
			return ret;
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to call procedure", ex);
		}
	}
	
	public Row getLookup() throws KettleDatabaseException
	{
		return getLookup(prepStatementLookup);
	}

    public Row getLookup(boolean failOnMultipleResults) throws KettleDatabaseException
    {
        return getLookup(prepStatementLookup, failOnMultipleResults);
    }

    public Row getLookup(PreparedStatement ps) throws KettleDatabaseException
    {
        return getLookup(ps, false);
    }

	public Row getLookup(PreparedStatement ps, boolean failOnMultipleResults) throws KettleDatabaseException
	{
		String debug = "start";
		Row ret;
        ResultSet res = null;
		try
		{
			debug = "pstmt.executeQuery()";
			res = ps.executeQuery();
			
			debug = "getRowInfo()";
			rowinfo = getRowInfo(res.getMetaData());
			
			debug = "getRow(res)";
			ret=getRow(res);
			
            if (failOnMultipleResults)
            {
                if (ret != null && res.next()) 
                {
                	// if the previous row was null, there's no reason to try res.next() again.
                	// on DB2 this will even cause an exception (because of the buggy DB2 JDBC driver).
                    throw new KettleDatabaseException("Only 1 row was expected as a result of a lookup, and at least 2 were found!");
                }
            }			
			return ret;
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Error looking up row in database ("+debug+")", ex);
		}
        finally
        {
            try
            {
            	debug = "res.close()";
                if (res!=null) res.close(); // close resultset!
            }
            catch(SQLException e)
            {
                throw new KettleDatabaseException("Unable to close resultset after looking up data", e);
            }
        }
	}
	
	public DatabaseMetaData getDatabaseMetaData()
		throws KettleDatabaseException
	{
		try
		{
			if (dbmd==null) dbmd = connection.getMetaData(); // Only get the metadata once!
		}
		catch(Exception e)
		{
			throw new KettleDatabaseException("Unable to get database metadata from this database connection", e);
		}
		
		return dbmd;
	}
	
	public String getDDL(String tablename, Row fields)
		throws KettleDatabaseException
	{
		return getDDL(tablename, fields, null, false, null, true);
	}

	public String getDDL(String tablename, Row fields, String tk, boolean use_autoinc, String pk) throws KettleDatabaseException
	{
		return getDDL(tablename, fields, tk, use_autoinc, pk, true);
	}

	public String getDDL(String tableName, Row fields, String tk, boolean use_autoinc, String pk, boolean semicolon) throws KettleDatabaseException
	{
		String retval;
		
		// First, check for reserved SQL in the input row r...
		databaseMeta.quoteReservedWords(fields);
		String quotedTk = tk != null ? databaseMeta.quoteField(tk) : null;
		
		if (checkTableExists(tableName))
		{
			retval=getAlterTableStatement(tableName, fields, quotedTk, use_autoinc, pk, semicolon);
		}
		else
		{
			retval=getCreateTableStatement(tableName, fields, quotedTk, use_autoinc, pk, semicolon);
		}
		
		return retval;
	}
	
    /**
     * Generates SQL
     * @param tableName the table name or schema/table combination: this needs to be quoted properly in advance.
     * @param fields the fields
     * @param tk the name of the technical key field
     * @param use_autoinc true if we need to use auto-increment fields for a primary key
     * @param pk the name of the primary/technical key field
     * @param semicolon append semicolon to the statement
     * @return the SQL needed to create the specified table and fields.
     */
	public String getCreateTableStatement(String tableName, Row fields, String tk, boolean use_autoinc, String pk, boolean semicolon)
	{
		String retval;
		
		retval = "CREATE TABLE "+tableName+Const.CR;
		retval+= "("+Const.CR;
		for (int i=0;i<fields.size();i++)
		{
			if (i>0) retval+=", "; else retval+="  ";
			
			Value v=fields.getValue(i);
			retval+=databaseMeta.getFieldDefinition(v, tk, pk, use_autoinc);
		}
		// At the end, before the closing of the statement, we might need to add some constraints...
		// Technical keys
		if (tk!=null)
		{
			if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_CACHE)
			{
				retval+=", PRIMARY KEY ("+tk+")"+Const.CR;
			}
		}
		
		// Primary keys
		if (pk!=null)
		{
			if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_ORACLE)
			{
				retval+=", PRIMARY KEY ("+pk+")"+Const.CR;
			}
		}
		retval+= ")"+Const.CR;
		
		if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_ORACLE &&
				databaseMeta.getIndexTablespace()!=null && databaseMeta.getIndexTablespace().length()>0)
		{
			retval+="TABLESPACE "+databaseMeta.getDataTablespace();
		}

		if (semicolon) retval+=";";
		retval+=Const.CR;
		
		return retval;
	}

	public String getAlterTableStatement(String tableName, Row fields, String tk, boolean use_autoinc, String pk, boolean semicolon) throws KettleDatabaseException
	{
		String retval="";
		
		// Get the fields that are in the table now:
		Row tabFields = getTableFields(tableName);
		
        // Don't forget to quote these as well...
        databaseMeta.quoteReservedWords(tabFields);
        
		// Find the missing fields
		Row missing = new Row();
		for (int i=0;i<fields.size();i++)
		{
			Value v = fields.getValue(i);
			// Not found?
			if (tabFields.searchValue( v.getName() )==null )
			{
				missing.addValue(v); // nope --> Missing!
			}
		}

		if (missing.size()!=0)
		{
			for (int i=0;i<missing.size();i++)
			{
				Value v=missing.getValue(i);
				retval+=databaseMeta.getAddColumnStatement(tableName, v, tk, use_autoinc, pk, true);
			}
		}

		// Find the surplus fields
		Row surplus = new Row();
		for (int i=0;i<tabFields.size();i++)
		{
			Value v = tabFields.getValue(i);
		    // Found in table, not in input ?        
		    if (fields.searchValue( v.getName() )==null )
		    {
			    surplus.addValue(v); // yes --> surplus!
		    }
		}

		if (surplus.size()!=0)
		{
			for (int i=0;i<surplus.size();i++)
			{
				Value v=surplus.getValue(i);
				retval+=databaseMeta.getDropColumnStatement(tableName, v, tk, use_autoinc, pk, true);
			}
		}
		
		//
		// OK, see if there are fields for wich we need to modify the type... (length, precision)
		//
		Row modify = new Row();
		for (int i=0;i<fields.size();i++)
		{
			Value desiredField = fields.getValue(i);
			Value currentField = tabFields.searchValue( desiredField.getName());
			if (currentField!=null)
			{
                boolean mod = false;
                
                mod |= ( currentField.getLength()    < desiredField.getLength()    ) && desiredField.getLength()>0; 
                mod |= ( currentField.getPrecision() < desiredField.getPrecision() ) && desiredField.getPrecision()>0;
				
				// Numeric values...
				mod |= ( currentField.getType() != desiredField.getType() ) && ( currentField.isNumber()^desiredField.isNumeric() );
				
                // TODO: this is not an optimal way of finding out changes.
                // Perhaps we should just generate the data types strings for existing and new data type and see if anything changed.
                // 
                
				if (mod)
				{
                    // System.out.println("Desired field: ["+desiredField.toStringMeta()+"], current field: ["+currentField.toStringMeta()+"]");
                    modify.addValue(desiredField);
				}
			}
		}
		
		if (modify.size()>0)
		{
			for (int i=0;i<modify.size();i++)
			{
				Value v=modify.getValue(i);
				retval+=databaseMeta.getModifyColumnStatement(tableName, v, tk, use_autoinc, pk, true);
			}
		}

		return retval;
	}

    public void checkDimZero(String tableName, String tk, String version, boolean use_autoinc) throws KettleDatabaseException
    {
        checkDimZero(null, tableName, tk, version, use_autoinc);
    }

	public void checkDimZero(String schemaName, String tableName, String tk, String version, boolean use_autoinc) throws KettleDatabaseException
	{
		int start_tk = databaseMeta.getNotFoundTK(use_autoinc);
				
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
        String sql = "SELECT count(*) FROM "+schemaTable+" WHERE "+databaseMeta.quoteField(tk)+" = "+start_tk;
		Row r = getOneRow(sql); 
		Value count = r.getValue(0);
		if (count.getNumber() == 0)
		{
			try
			{
				Statement st = connection.createStatement();
				String isql;
				if (!databaseMeta.supportsAutoinc() || !use_autoinc)
				{
					isql = "insert into "+schemaTable+"("+databaseMeta.quoteField(tk)+", "+databaseMeta.quoteField(version)+") values (0, 1)";
				}
				else
				{
					switch(databaseMeta.getDatabaseType())
					{
					case DatabaseMeta.TYPE_DATABASE_CACHE       :
					case DatabaseMeta.TYPE_DATABASE_GUPTA     :
					case DatabaseMeta.TYPE_DATABASE_ORACLE      :  isql = "insert into "+schemaTable+"("+databaseMeta.quoteField(tk)+", "+databaseMeta.quoteField(version)+") values (0, 1)"; break; 
					case DatabaseMeta.TYPE_DATABASE_INFORMIX    : 
					case DatabaseMeta.TYPE_DATABASE_MYSQL       :  isql = "insert into "+schemaTable+"("+databaseMeta.quoteField(tk)+", "+databaseMeta.quoteField(version)+") values (1, 1)"; break;
					case DatabaseMeta.TYPE_DATABASE_MSSQL       :  
					case DatabaseMeta.TYPE_DATABASE_DB2         : 
					case DatabaseMeta.TYPE_DATABASE_DBASE       :  
					case DatabaseMeta.TYPE_DATABASE_GENERIC     :  
					case DatabaseMeta.TYPE_DATABASE_SYBASE      :
					case DatabaseMeta.TYPE_DATABASE_ACCESS      :  isql = "insert into "+schemaTable+"("+databaseMeta.quoteField(version)+") values (1)"; break;
					default: isql = "insert into "+schemaTable+"("+databaseMeta.quoteField(tk)+", "+databaseMeta.quoteField(version)+") values (0, 1)"; break;
					}					
				}
				
				st.executeUpdate(databaseMeta.stripCR(isql));
			}
			catch(SQLException e)
			{
				throw new KettleDatabaseException("Error inserting 'unknown' row in dimension ["+schemaTable+"] : "+sql, e);
			}
		}
	}

    /**
     * @param seqname The sequence to check
     * @return the current sequence value
     * @throws KettleDatabaseException in case something goes wrong (and something always does in this case)
     * @deprecated because it causes an error if you read the current value and the next val has not yet been called in the current session (is usually the case) 
     * 
     */
	public Value checkSequence(String seqname) throws KettleDatabaseException
	{
		String sql=null;
		if (databaseMeta.supportsSequences())
		{
			sql = databaseMeta.getSQLCurrentSequenceValue(seqname);

			ResultSet rs = openQuery(sql, null);
			Row r = getRow(rs); // One value: a number;
			if (r!=null)
			{
				Value last = r.getValue(0);
				// errorstr="Sequence is at number: "+last.toString();
				return last;
			}
			else
			{
				return null;
			}
		}
		else
		{
			throw new KettleDatabaseException("Sequences are only available for Oracle databases.");
		}
	}

	public void truncateTable(String tablename) throws KettleDatabaseException
	{
        if (Const.isEmpty(connectionGroup))
        {
            execStatement(databaseMeta.getTruncateTableStatement(null, tablename));
        }
        else
        {
            execStatement("DELETE FROM "+databaseMeta.quoteField(tablename));
        }
	}
	
	public void truncateTable(String schema, String tablename) throws KettleDatabaseException
	{
        if (Const.isEmpty(connectionGroup))
        {
            execStatement(databaseMeta.getTruncateTableStatement(schema, tablename));
        }
        else
        {
            execStatement("DELETE FROM "+databaseMeta.getQuotedSchemaTableCombination(schema, tablename));
        }
	}
	
	/**
	 * Execute a query and return at most one row from the resultset
	 * @param sql The SQL for the query
	 * @return one Row with data or null if nothing was found.
	 */
	public Row getOneRow(String sql) throws KettleDatabaseException
	{
		ResultSet rs = openQuery(sql, null);
		if (rs!=null)
		{
			Row r = getRow(rs); // One row only;
			try { rs.close(); } catch(Exception e) { throw new KettleDatabaseException("Unable to close resultset", e); }
			
			if (pstmt!=null)
			{
				try { pstmt.close(); } catch(Exception e) { throw new KettleDatabaseException("Unable to close prepared statement pstmt", e); }
				pstmt=null;
			}
			if (sel_stmt!=null)
			{
				try { sel_stmt.close(); } catch(Exception e) { throw new KettleDatabaseException("Unable to close prepared statement sel_stmt", e); }
				sel_stmt=null;
			}
			return r;
		}
		else
		{
			throw new KettleDatabaseException("error opening resultset for query: "+sql);
		}
	}

	public Row getOneRow(String sql, Row param)
		throws KettleDatabaseException
	{
		ResultSet rs = openQuery(sql, param);
		if (rs!=null)
		{		
			Row r = getRow(rs); // One value: a number;
			try { rs.close(); } catch(Exception e) { throw new KettleDatabaseException("Unable to close resultset", e); }
			
			if (pstmt!=null)
			{
				try { pstmt.close(); } catch(Exception e) { throw new KettleDatabaseException("Unable to close prepared statement pstmt", e); }
				pstmt=null;
			}
			if (sel_stmt!=null)
			{
				try { sel_stmt.close(); } catch(Exception e) { throw new KettleDatabaseException("Unable to close prepared statement sel_stmt", e); }
				sel_stmt=null;
			}
            rowinfo=null;
            
			return r;
		}
		else
		{
			return null;
		}
	}
	
	public Row getParameterMetaData(PreparedStatement ps)
	{
		Row par = new Row();
		try
		{
			ParameterMetaData pmd = ps.getParameterMetaData();
			for (int i=1;i<=pmd.getParameterCount();i++)
			{
				String name    = "par"+i;
				int    sqltype = pmd.getParameterType(i);
                int length   = pmd.getPrecision(i);
                int precision = pmd.getScale(i);
				Value val;
				
				switch(sqltype)
				{
				case java.sql.Types.CHAR:
				case java.sql.Types.VARCHAR: 
					val=new Value(name, Value.VALUE_TYPE_STRING);
					break;
				case java.sql.Types.BIGINT:
				case java.sql.Types.INTEGER:
				case java.sql.Types.NUMERIC:
				case java.sql.Types.SMALLINT:
				case java.sql.Types.TINYINT: 
					val=new Value(name, Value.VALUE_TYPE_INTEGER);
					break;
				case java.sql.Types.DECIMAL:
				case java.sql.Types.DOUBLE:
				case java.sql.Types.FLOAT:
				case java.sql.Types.REAL:
					val=new Value(name, Value.VALUE_TYPE_NUMBER);
					break;
				case java.sql.Types.DATE:
				case java.sql.Types.TIME:
				case java.sql.Types.TIMESTAMP: 
					val=new Value(name, Value.VALUE_TYPE_DATE); 
					break;
				case java.sql.Types.BOOLEAN:
				case java.sql.Types.BIT:
					val=new Value(name, Value.VALUE_TYPE_BOOLEAN);
					break;
				default:
					val=new Value(name, Value.VALUE_TYPE_NONE);
					break;
				}
                
                if (val.isNumeric() && ( length>18 || precision>18) )
                {
                    val = new Value(name, Value.VALUE_TYPE_BIGNUMBER);
                }
                
				val.setNull();
				par.addValue(val);
			}
		}
		// Oops: probably the database or JDBC doesn't support it.
		catch(AbstractMethodError e) { return null;	}
		catch(SQLException e) { return null; }
		catch(Exception e) { return null;	}
		
		return par;
	}
	
	public int countParameters(String sql)
	{
		int q=0;
		boolean quote_opened=false;
		boolean dquote_opened=false;
		
		for (int x=0;x<sql.length();x++)
		{
			char c = sql.charAt(x);
			
			switch(c)
			{
			case '\'':  quote_opened= !quote_opened; break;
			case '"' :  dquote_opened=!dquote_opened; break;
			case '?' :  if (!quote_opened && !dquote_opened) q++; break;
			}
		}
		
		return q;
	}

	// Get the fields back from an SQL query
	public Row getParameterMetaData(String sql, Row inform)
	{
		// The database couldn't handle it: try manually!
		int q=countParameters(sql);
		
		Row par=new Row();
		
		if (inform!=null && q==inform.size())
		{
			for (int i=0;i<q;i++)
			{
				Value inf=inform.getValue(i);
				Value v = new Value(inf);
				par.addValue(v);
			}
		}
		else
		{
			for (int i=0;i<q;i++)
			{
				Value v = new Value("name"+i, Value.VALUE_TYPE_NUMBER);
				v.setValue( 0.0 );
				par.addValue(v);
			}
		}

		return par;
	}

	public static final Row getTransLogrecordFields(boolean use_batchid, boolean use_logfield)
	{
		Row r = new Row();
		Value v;
		
		if (use_batchid)
		{
			v=new Value("ID_BATCH",    Value.VALUE_TYPE_INTEGER); v.setLength(8,0);   r.addValue(v);
		}
		
		v=new Value("TRANSNAME",       Value.VALUE_TYPE_STRING ); v.setLength(50);   r.addValue(v);
		v=new Value("STATUS",          Value.VALUE_TYPE_STRING ); v.setLength(15);   r.addValue(v);
		v=new Value("LINES_READ",      Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_WRITTEN",   Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_UPDATED",   Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_INPUT",     Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_OUTPUT",    Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("ERRORS",          Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("STARTDATE",       Value.VALUE_TYPE_DATE   );                    r.addValue(v);
		v=new Value("ENDDATE",         Value.VALUE_TYPE_DATE   );                    r.addValue(v);
		v=new Value("LOGDATE",         Value.VALUE_TYPE_DATE   );                    r.addValue(v);
		v=new Value("DEPDATE",         Value.VALUE_TYPE_DATE   );                    r.addValue(v);
		v=new Value("REPLAYDATE",      Value.VALUE_TYPE_DATE   );                    r.addValue(v);

		if (use_logfield)
		{
			v=new Value("LOG_FIELD",   Value.VALUE_TYPE_STRING); 
			v.setLength(DatabaseMeta.CLOB_LENGTH,0);   
			r.addValue(v);
		}

		return r;
	}

	public static final Row getJobLogrecordFields(boolean use_jobid, boolean use_logfield)
	{
		Row r = new Row();
		Value v;

		if (use_jobid)
		{
			v=new Value("ID_JOB",      Value.VALUE_TYPE_INTEGER); v.setLength(8,0);   r.addValue(v);
		}
		
		v=new Value("JOBNAME",         Value.VALUE_TYPE_STRING); v.setLength(50);    r.addValue(v);
		v=new Value("STATUS",          Value.VALUE_TYPE_STRING); v.setLength(15);    r.addValue(v);
		v=new Value("LINES_READ",      Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_WRITTEN",   Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_UPDATED",   Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_INPUT",     Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("LINES_OUTPUT",    Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("ERRORS",          Value.VALUE_TYPE_INTEGER); v.setLength(10,0); r.addValue(v);
		v=new Value("STARTDATE",       Value.VALUE_TYPE_DATE  );                     r.addValue(v);
		v=new Value("ENDDATE",         Value.VALUE_TYPE_DATE  );                     r.addValue(v);
		v=new Value("LOGDATE",         Value.VALUE_TYPE_DATE  );                     r.addValue(v);
		v=new Value("DEPDATE",         Value.VALUE_TYPE_DATE  );                     r.addValue(v);
        v=new Value("REPLAYDATE",      Value.VALUE_TYPE_DATE  );                     r.addValue(v);

		if (use_logfield)
		{
			v=new Value("LOG_FIELD",   Value.VALUE_TYPE_STRING); 
			v.setLength(DatabaseMeta.CLOB_LENGTH,0);   
			r.addValue(v);
		}

		return r;
	}
	
	public void writeLogRecord(   String logtable,
		                          boolean use_id,
		                          long id,
								  boolean job,
								  String name, 
								  String status,
								  long read, long written, long updated, 
								  long input, long output, long errors,
								  java.util.Date startdate, java.util.Date enddate,
								  java.util.Date logdate,   java.util.Date depdate,
								  java.util.Date replayDate, 
								  String log_string
								  )
		throws KettleDatabaseException
	{
        if (use_id && log_string!=null && !status.equalsIgnoreCase("start"))
        {
            String sql = "UPDATE "+logtable+" SET STATUS=?, LINES_READ=?, LINES_WRITTEN=?, LINES_INPUT=?," +
                    " LINES_OUTPUT=?, LINES_UPDATED=?, ERRORS=?, STARTDATE=?, ENDDATE=?, LOGDATE=?, DEPDATE=?, REPLAYDATE=?, LOG_FIELD=? " +
                    "WHERE ";
            if (job) sql+="ID_JOB=?"; else sql+="ID_BATCH=?";

            Row r = new Row();
            r.addValue( new Value("STATUS",          status       ));
            r.addValue( new Value("LINES_READ",      (long)read   ));
            r.addValue( new Value("LINES_WRITTEN",   (long)written));
            r.addValue( new Value("LINES_INPUT",     (long)input  ));
            r.addValue( new Value("LINES_OUTPUT",    (long)output ));
            r.addValue( new Value("LINES_UPDATED",   (long)updated));
            r.addValue( new Value("ERRORS",          (long)errors ));
            r.addValue( new Value("STARTDATE",       startdate    ));
            r.addValue( new Value("ENDDATE",         enddate      ));
            r.addValue( new Value("LOGDATE",         logdate      ));
            r.addValue( new Value("DEPDATE",         depdate      ));
            r.addValue( new Value("REPLAYDATE", 	 replayDate      ));
            Value logfield = new Value("LOG_FIELD",       log_string);
            logfield.setLength(DatabaseMeta.CLOB_LENGTH);
            r.addValue( logfield );
            r.addValue( new Value("ID_BATCH",        id           ));
            
            execStatement(sql, r);
        }
        else
        {
    		int parms;
            
    		String sql = "INSERT INTO "+logtable+" ( ";
    		if (job)
    		{
    			if (use_id) 
    			{
    				sql+="ID_JOB, JOBNAME";
    				parms=14;
    			} 
    			else 
    			{
    				sql+="JOBNAME";
    				parms=13;
    			} 
    		}
    		else
    		{
    			if (use_id) 
    			{
    				sql+="ID_BATCH, TRANSNAME";
    				parms=14;
    			} 
    			else 
    			{ 
    				sql+="TRANSNAME";
    				parms=13;
    			} 
    		}
    		
    		sql+=", STATUS, LINES_READ, LINES_WRITTEN, LINES_UPDATED, LINES_INPUT, LINES_OUTPUT, ERRORS, STARTDATE, ENDDATE, LOGDATE, DEPDATE, REPLAYDATE";
    		
    		if (log_string!=null && log_string.length()>0) sql+=", LOG_FIELD";  // This is possibly a CLOB!
    		
    		sql+=") VALUES(";
    		for (int i=0;i<parms;i++) if (i==0) sql+="?"; else sql+=", ?";
    		
    		if (log_string!=null && log_string.length()>0) sql+=", ?";
    		
    		sql+=")";
    		try
    		{
    			pstmt = connection.prepareStatement(databaseMeta.stripCR(sql));
    			
    			Row r = new Row();
    			if (job)
    			{
    				if (use_id)
    				{
    					r.addValue( new Value("ID_BATCH",          	id          	));
    				}
    				r.addValue( new Value("TRANSNAME",            name           ));
    			}
    			else
    			{
    				if (use_id)
    				{
    					r.addValue( new Value("ID_JOB",          	id          	));
    				}
    				r.addValue( new Value("JOBNAME",            name           ));
    			}
    			r.addValue( new Value("STATUS",          status       ));
    			r.addValue( new Value("LINES_READ",      (long)read   ));
    			r.addValue( new Value("LINES_WRITTEN",   (long)written));
    			r.addValue( new Value("LINES_UPDATED",   (long)updated));
    			r.addValue( new Value("LINES_INPUT",     (long)input  ));
    			r.addValue( new Value("LINES_OUTPUT",    (long)output ));
    			r.addValue( new Value("ERRORS",          (long)errors ));
    			r.addValue( new Value("STARTDATE",       startdate    ));
    			r.addValue( new Value("ENDDATE",         enddate      ));
    			r.addValue( new Value("LOGDATE",         logdate      ));
    			r.addValue( new Value("DEPDATE",         depdate      ));
    			r.addValue( new Value("REPLAYDATE",      replayDate      ));
    
    			if (log_string!=null && log_string.length()>0)
    			{
    				Value large = new Value("LOG_FIELD",       log_string     );
    				large.setLength(DatabaseMeta.CLOB_LENGTH);
    				r.addValue( large );
    			}
    
    			setValues(r);
    
    			pstmt.executeUpdate();
    			pstmt.close(); pstmt=null;
    			
    		}
    		catch(SQLException ex) 
    		{
    			throw new KettleDatabaseException("Unable to write log record to log table "+logtable, ex);
    		}
        }
	}
	
	public Row getLastLogDate( String logtable, 
							   String name,
							   boolean job, 
							   String status
							  )
		throws KettleDatabaseException
	{
		Row row=null;
		String jobtrans = job?"JOBNAME":"TRANSNAME";
		
		String sql = "";
		sql+=" SELECT ENDDATE, DEPDATE, STARTDATE";
		sql+=" FROM "+logtable;
		sql+=" WHERE  ERRORS    = 0";
		sql+=" AND    STATUS    = 'end'";
		sql+=" AND    "+jobtrans+" = ?";
		sql+=" ORDER BY LOGDATE DESC, ENDDATE DESC";

		try
		{
			pstmt = connection.prepareStatement(databaseMeta.stripCR(sql));
			
			Row r = new Row();
			r.addValue( new Value("TRANSNAME", name      ));
			
			setValues(r);
			
			ResultSet res = pstmt.executeQuery();
			if (res!=null)
			{
				rowinfo = getRowInfo(res.getMetaData());
				row = getRow(res);
				res.close();
			}
			pstmt.close(); pstmt=null;
		}
		catch(SQLException ex) 
		{
			throw new KettleDatabaseException("Unable to obtain last logdate from table "+logtable, ex);
		}
		
		return row;
	}

    public synchronized void getNextValue(Hashtable counters, String tableName, Value val_key) throws KettleDatabaseException
    {
        getNextValue(counters, null, tableName, val_key);
    }
    
	public synchronized void getNextValue(Hashtable counters, String schemaName, String tableName, Value val_key) throws KettleDatabaseException
	{
        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(schemaName, tableName);
		String lookup = schemaTable+"."+databaseMeta.quoteField(val_key.getName());
		
		// Try to find the previous sequence value...
		Counter counter = null;
        if (counters!=null) counter=(Counter)counters.get(lookup);
        
		if (counter==null)
		{
			Row r = getOneRow("SELECT MAX("+databaseMeta.quoteField(val_key.getName())+") FROM "+schemaTable);
			if (r!=null)
			{
				counter = new Counter(r.getValue(0).getInteger()+1, 1);
				val_key.setValue(counter.next());
				if (counters!=null) counters.put(lookup, counter);
			}
			else
			{
				throw new KettleDatabaseException("Couldn't find maximum key value from table "+schemaTable);
			}
		}
		else
		{
			val_key.setValue(counter.next());
		}
	}
			
	public String toString()
	{
		if (databaseMeta!=null) return databaseMeta.getName();
		else return "-";
	}

	public boolean isSystemTable(String table_name)
	{
		if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_MSSQL)
		{
			if ( table_name.startsWith("sys")) return true;
			if ( table_name.equals("dtproperties")) return true;
		}
		else
		if (databaseMeta.getDatabaseType()==DatabaseMeta.TYPE_DATABASE_GUPTA)
		{
			if ( table_name.startsWith("SYS")) return true;
		}
		return false;
	}
	
	/** Reads the result of an SQL query into an ArrayList
	 * 
	 * @param sql The SQL to launch
	 * @param limit <=0 means unlimited, otherwise this specifies the maximum number of rows read.
	 * @return An ArrayList of rows.
	 * @throws KettleDatabaseException if something goes wrong.
	 */
	public ArrayList getRows(String sql, int limit) throws KettleDatabaseException
	{
	    return getRows(sql, limit, null);
	}

	/** Reads the result of an SQL query into an ArrayList
	 * 
	 * @param sql The SQL to launch
	 * @param limit <=0 means unlimited, otherwise this specifies the maximum number of rows read.
	 * @param monitor The progress monitor to update while getting the rows.
	 * @return An ArrayList of rows.
	 * @throws KettleDatabaseException if something goes wrong.
	 */
	public ArrayList getRows(String sql, int limit, IProgressMonitor monitor) throws KettleDatabaseException
	{
		if (monitor!=null) monitor.setTaskName("Opening query...");
		ResultSet rset = openQuery(sql);
		
        return getRows(rset, limit, monitor);
	}

    /** Reads the result of a ResultSet into an ArrayList
     * 
     * @param rset the ResultSet to read out
     * @param limit <=0 means unlimited, otherwise this specifies the maximum number of rows read.
     * @param monitor The progress monitor to update while getting the rows.
     * @return An ArrayList of rows.
     * @throws KettleDatabaseException if something goes wrong.
     */
	public ArrayList getRows(ResultSet rset, int limit, IProgressMonitor monitor) throws KettleDatabaseException
    {
        try
        {
            ArrayList result = new ArrayList();
            boolean stop=false;
            int i=0;
            
            if (rset!=null)
            {
                if (monitor!=null && limit>0) monitor.beginTask("Reading rows...", limit);
                while ((limit<=0 || i<limit) && !stop)
                {
                    Row row = getRow(rset);
                    if (row!=null)
                    {
                        result.add(row);
                        i++;
                    }
                    else
                    {
                        stop=true;
                    }
                    if (monitor!=null && limit>0) monitor.worked(1);
                }
                closeQuery(rset);
                if (monitor!=null) monitor.done();
            }
            
            return result;
        }
        catch(Exception e)
        {
            throw new KettleDatabaseException("Unable to get list of rows from ResultSet : ", e);
        }
    }

    public ArrayList getFirstRows(String table_name, int limit) throws KettleDatabaseException
	{
	    return getFirstRows(table_name, limit, null);
	}


    /**
     * Get the first rows from a table (for preview) 
     * @param table_name The table name (or schema/table combination): this needs to be quoted properly
     * @param limit limit <=0 means unlimited, otherwise this specifies the maximum number of rows read.
     * @param monitor The progress monitor to update while getting the rows.
     * @return An ArrayList of rows.
     * @throws KettleDatabaseException in case something goes wrong
     */
	public ArrayList getFirstRows(String table_name, int limit, IProgressMonitor monitor) throws KettleDatabaseException
	{
		String sql = "SELECT * FROM "+table_name;
		
        if (limit>0)
		{
		    sql+=databaseMeta.getLimitClause(limit);
		}
		
		return getRows(sql, limit, monitor);
	}
	
	public Row getReturnRow()
	{
		return rowinfo;
	}
	
	public String[] getTableTypes() throws KettleDatabaseException
	{
		try
		{
			ArrayList types = new ArrayList();
		
			ResultSet rstt = getDatabaseMetaData().getTableTypes();
	        while(rstt.next()) 
	        {
	            String ttype = rstt.getString("TABLE_TYPE");
	            types.add(ttype);
	        }
	        
	        return (String[])types.toArray(new String[types.size()]);
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Unable to get table types from database!", e);
		}
	}
		
	public String[] getTablenames()
		throws KettleDatabaseException
	{
		String schemaname = null;
		if (databaseMeta.useSchemaNameForTableList()) schemaname = databaseMeta.getUsername().toUpperCase();

		ArrayList names = new ArrayList();
		ResultSet alltables=null;
		try
		{
			alltables = getDatabaseMetaData().getTables(null, schemaname, null, databaseMeta.getTableTypes() );
			while (alltables.next())
			{
				String table = alltables.getString("TABLE_NAME");
                if (log.isRowLevel()) log.logRowlevel(toString(), "got table from meta-data: "+table);
				names.add(table);
			}
		}
		catch(SQLException e)
		{
			log.logError(toString(), "Error getting tablenames from schema ["+schemaname+"]");
		}
		finally
		{
			try
			{
				if (alltables!=null) alltables.close();
			}
			catch(SQLException e)
			{
                throw new KettleDatabaseException("Error closing resultset after getting views from schema ["+schemaname+"]", e);
			}
		}

		log.logDetailed(toString(), "read :"+names.size()+" table names from db meta-data.");

		return (String[])names.toArray(new String[names.size()]);
	}
	
	public String[] getViews()
		throws KettleDatabaseException
	{
		if (!databaseMeta.supportsViews()) return new String[] {};

		String schemaname = null;
		if (databaseMeta.useSchemaNameForTableList()) schemaname=databaseMeta.getUsername().toUpperCase();

		ArrayList names = new ArrayList();
		ResultSet alltables=null;
		try
		{
			alltables = dbmd.getTables(null, schemaname, null, databaseMeta.getViewTypes() );
			while (alltables.next())
			{
				String table = alltables.getString("TABLE_NAME");
                if (log.isRowLevel()) log.logRowlevel(toString(), "got view from meta-data: "+table);
				names.add(table);
			}
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Error getting views from schema ["+schemaname+"]", e);
		}
		finally
		{
			try
			{
				if (alltables!=null) alltables.close();
			}
			catch(SQLException e)
			{
				throw new KettleDatabaseException("Error closing resultset after getting views from schema ["+schemaname+"]", e);
			}
		}

		log.logDetailed(toString(), "read :"+names.size()+" views from db meta-data.");

		return (String[])names.toArray(new String[names.size()]);
	}

	public String[] getSynonyms() throws KettleDatabaseException
	{
		if (!databaseMeta.supportsSynonyms()) return new String[] {};
		
		String schemaname = null;
		if (databaseMeta.useSchemaNameForTableList()) schemaname=databaseMeta.getUsername().toUpperCase();
	
		ArrayList names = new ArrayList();
		ResultSet alltables=null;
		try
		{
			alltables = dbmd.getTables(null, schemaname, null, databaseMeta.getSynonymTypes() );
			while (alltables.next())
			{
				String table = alltables.getString("TABLE_NAME");
                if (log.isRowLevel()) log.logRowlevel(toString(), "got view from meta-data: "+table);
				names.add(table);
			}
		}
		catch(SQLException e)
		{
			throw new KettleDatabaseException("Error getting synonyms from schema ["+schemaname+"]", e);
		}
		finally
		{
			try
			{
				if (alltables!=null) alltables.close();
			}
			catch(SQLException e)
			{
				throw new KettleDatabaseException("Error closing resultset after getting synonyms from schema ["+schemaname+"]", e);
			}
		}
	
		log.logDetailed(toString(), "read :"+names.size()+" views from db meta-data.");
	
		return (String[])names.toArray(new String[names.size()]);
	}
	
	public String[] getProcedures() throws KettleDatabaseException
	{
		String sql = databaseMeta.getSQLListOfProcedures();
		if (sql!=null)
		{
			//System.out.println("SQL= "+sql);
			ArrayList procs = getRows(sql, 1000);
			//System.out.println("Found "+procs.size()+" rows");
			String[] str = new String[procs.size()];
			for (int i=0;i<procs.size();i++)
			{
				str[i] = ((Row)procs.get(i)).getValue(0).getString();
			}
			return str;
		}
        else
        {
            ResultSet rs = null;
            try
            {
                DatabaseMetaData dbmd = getDatabaseMetaData();
                rs = dbmd.getProcedures(null, null, null);
                ArrayList rows = getRows(rs, 0, null);
                String result[] = new String[rows.size()];
                for (int i=0;i<rows.size();i++)
                {
                    Row row = (Row)rows.get(i);
                    String procCatalog = row.getString("PROCEDURE_CAT", null);
                    String procSchema  = row.getString("PROCEDURE_SCHEMA", null);
                    String procName    = row.getString("PROCEDURE_NAME", "");
 
                    String name = "";
                    if (procCatalog!=null) name+=procCatalog+".";
                    else if (procSchema!=null) name+=procSchema+".";
                    
                    name+=procName;
                    
                    result[i] = name;
                }
                return result;
            }
            catch(Exception e)
            {
                throw new KettleDatabaseException("Unable to get list of procedures from database meta-data: ", e);
            }
            finally
            {
                if (rs!=null) try { rs.close(); } catch(Exception e) {}
            }
        }
	}

    public boolean isAutoCommit()
    {
        return commitsize<=0;
    }

    /**
     * @return Returns the databaseMeta.
     */
    public DatabaseMeta getDatabaseMeta()
    {
        return databaseMeta;
    }

    /**
     * Lock a tables in the database for write operations
     * @param tableNames The tables to lock
     * @throws KettleDatabaseException
     */
    public void lockTables(String tableNames[]) throws KettleDatabaseException
    {
        String sql = databaseMeta.getSQLLockTables(tableNames);
        if (sql!=null)
        {
            execStatements(sql);
        }
    }
	
    /**
     * Unlock certain tables in the database for write operations
     * @param tableNames The tables to unlock
     * @throws KettleDatabaseException
     */
    public void unlockTables(String tableNames[]) throws KettleDatabaseException
    {
        String sql = databaseMeta.getSQLUnlockTables(tableNames);
        if (sql!=null)
        {
            execStatement(sql);
        }
    }

    /**
     * @return the opened
     */
    public int getOpened()
    {
        return opened;
    }

    /**
     * @param opened the opened to set
     */
    public void setOpened(int opened)
    {
        this.opened = opened;
    }

    /**
     * @return the connectionGroup
     */
    public String getConnectionGroup()
    {
        return connectionGroup;
    }

    /**
     * @param connectionGroup the connectionGroup to set
     */
    public void setConnectionGroup(String connectionGroup)
    {
        this.connectionGroup = connectionGroup;
    }

    /**
     * @return the partitionId
     */
    public String getPartitionId()
    {
        return partitionId;
    }

    /**
     * @param partitionId the partitionId to set
     */
    public void setPartitionId(String partitionId)
    {
        this.partitionId = partitionId;
    }

    /**
     * @return the copy
     */
    public int getCopy()
    {
        return copy;
    }

    /**
     * @param copy the copy to set
     */
    public void setCopy(int copy)
    {
        this.copy = copy;
    }
}