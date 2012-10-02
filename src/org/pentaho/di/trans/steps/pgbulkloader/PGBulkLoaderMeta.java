/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.pgbulkloader;

import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.SQLStatement;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.trans.DatabaseImpact;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;


/**
 * Created on 20-feb-2007
 * 
 * @author Sven Boden (originally)
 */
public class PGBulkLoaderMeta extends BaseStepMeta implements StepMetaInterface
{
	private static Class<?> PKG = PGBulkLoaderMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    /** what's the schema for the target? */
    private String schemaName;

    /** what's the table for the target? */
	private String tableName;
	
	/** Path to the PsqlPath utility */
	private String PsqlPath;
	
	/** database connection */
	private DatabaseMeta databaseMeta;

    /** Field value to dateMask after lookup */
	private String fieldTable[];

    /** Field name in the stream */
	private String fieldStream[];

    /** boolean indicating if field needs to be updated */
	private String dateMask[];

	/** Load action */
	private String loadAction;	
		
	/** Database name override */
	private String dbNameOverride;
	
	/** The field delimiter to use for loading */
	private String delimiter;
	
	/** The enclosure to use for loading */
	private String enclosure;
	
	/** Stop On Error */
  private boolean stopOnError;
  
	/*
	 * Do not translate following values!!! They are will end up in the job export.
	 */
	public static final String ACTION_INSERT   = "INSERT";
	public static final String ACTION_TRUNCATE = "TRUNCATE";

	/*
	 * Do not translate following values!!! They are will end up in the job export.
	 */
	public static final String DATE_MASK_PASS_THROUGH   = "PASS THROUGH";
	public static final String DATE_MASK_DATE           = "DATE";
	public static final String DATE_MASK_DATETIME       = "DATETIME";
		
	public static final int NR_DATE_MASK_PASS_THROUGH   = 0;
	public static final int NR_DATE_MASK_DATE           = 1;
	public static final int NR_DATE_MASK_DATETIME       = 2;
	
	public PGBulkLoaderMeta()
	{
		super();
	}

    /**
     * @return Returns the database.
     */
    public DatabaseMeta getDatabaseMeta()
    {
        return databaseMeta;
    }

    /**
     * @param database The database to set.
     */
    public void setDatabaseMeta(DatabaseMeta database)
    {
        this.databaseMeta = database;
    }

    /**
     * @return Returns the tableName.
     */
    public String getTableName()
    {
        return tableName;
    }

    /**
     * @param tableName The tableName to set.
     */
    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

	public String getPsqlpath() {
		return PsqlPath;
	}

	public void setPsqlpath(String PsqlPath) {
		this.PsqlPath = PsqlPath;
	}    
    
    /**
     * @return Returns the fieldTable.
     */
    public String[] getFieldTable()
    {
        return fieldTable;
    }

    /**
     * @param fieldTable The fieldTable to set.
     */
    public void setFieldTable(String[] fieldTable)
    {
        this.fieldTable = fieldTable;
    }

    /**
     * @return Returns the fieldStream.
     */
    public String[] getFieldStream()
    {
        return fieldStream;
    }

    /**
     * @param fieldStream The fieldStream to set.
     */
    public void setFieldStream(String[] fieldStream)
    {
        this.fieldStream = fieldStream;
    }

	public String[] getDateMask() {
		return dateMask;
	}

	public void setDateMask(String[] dateMask) {
		this.dateMask = dateMask;
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
		throws KettleXMLException
	{
		readData(stepnode, databases);
	}

	public void allocate(int nrvalues)
	{
		fieldTable  = new String[nrvalues];
		fieldStream = new String[nrvalues];
		dateMask    = new String[nrvalues];
	}

	public Object clone()
	{
		PGBulkLoaderMeta retval = (PGBulkLoaderMeta)super.clone();
		int nrvalues  = fieldTable.length;

		retval.allocate(nrvalues);

		for (int i=0;i<nrvalues;i++)
		{
			retval.fieldTable[i]  = fieldTable[i];
			retval.fieldStream[i] = fieldStream[i];
			retval.dateMask[i]    = dateMask[i];
		}
		return retval;
	}

	private void readData(Node stepnode, List<? extends SharedObjectInterface> databases)
		throws KettleXMLException
	{
		try
		{
			String con     = XMLHandler.getTagValue(stepnode, "connection");   //$NON-NLS-1$
			databaseMeta   = DatabaseMeta.findDatabase(databases, con);

			schemaName     = XMLHandler.getTagValue(stepnode, "schema");       //$NON-NLS-1$
			tableName      = XMLHandler.getTagValue(stepnode, "table");        //$NON-NLS-1$

            enclosure     = XMLHandler.getTagValue(stepnode, "enclosure");       //$NON-NLS-1$
			delimiter     = XMLHandler.getTagValue(stepnode, "delimiter");        //$NON-NLS-1$

			loadAction     = XMLHandler.getTagValue(stepnode, "load_action");  //$NON-NLS-1$			
			PsqlPath         = XMLHandler.getTagValue(stepnode, "PsqlPath");       //$NON-NLS-1$
			dbNameOverride = XMLHandler.getTagValue(stepnode, "dbname_override");  //$NON-NLS-1$
      stopOnError    = "Y".equalsIgnoreCase( XMLHandler.getTagValue(stepnode, "stop_on_error")); //$NON-NLS-1$

			int nrvalues = XMLHandler.countNodes(stepnode, "mapping");      //$NON-NLS-1$
			allocate(nrvalues);

			for (int i=0;i<nrvalues;i++)
			{
				Node vnode = XMLHandler.getSubNodeByNr(stepnode, "mapping", i);    //$NON-NLS-1$

				fieldTable[i]      = XMLHandler.getTagValue(vnode, "stream_name"); //$NON-NLS-1$
				fieldStream[i]     = XMLHandler.getTagValue(vnode, "field_name");  //$NON-NLS-1$
				if (fieldStream[i]==null) fieldStream[i]=fieldTable[i];            // default: the same name!
				String locDateMask = XMLHandler.getTagValue(vnode, "date_mask");   //$NON-NLS-1$
				if(locDateMask==null) {
					dateMask[i] = "";
				} 
				else
                {
                    if (PGBulkLoaderMeta.DATE_MASK_DATE.equals(locDateMask) || PGBulkLoaderMeta.DATE_MASK_PASS_THROUGH.equals(locDateMask) ||
                        PGBulkLoaderMeta.DATE_MASK_DATETIME.equals(locDateMask) )
                    {
                        dateMask[i] = locDateMask;
                    }
                    else
                    {
                    	dateMask[i] = "";
                    }
				}
			}
		}
		catch(Exception e)
		{
			throw new KettleXMLException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.UnableToReadStepInfoFromXML"), e); //$NON-NLS-1$
		}
	}

	public void setDefault()
	{
		fieldTable   = null;
		databaseMeta = null;
        schemaName   = "";                //$NON-NLS-1$
		tableName    = BaseMessages.getString(PKG, "GPBulkLoaderMeta.DefaultTableName"); //$NON-NLS-1$
		PsqlPath       = "PsqlPath";                              //$NON-NLS-1$
		dbNameOverride = "";
		delimiter = ";";
		enclosure = "\"";
		stopOnError = false;
		int nrvalues = 0;
		allocate(nrvalues);
	}

	public String getXML()
	{
        StringBuffer retval = new StringBuffer(300);

		retval.append("    ").append(XMLHandler.addTagValue("connection",      databaseMeta==null?"":databaseMeta.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        retval.append("    ").append(XMLHandler.addTagValue("schema",          schemaName));    //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("    ").append(XMLHandler.addTagValue("table",           tableName));     //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("    ").append(XMLHandler.addTagValue("load_action",     loadAction));    //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("    ").append(XMLHandler.addTagValue("PsqlPath",        PsqlPath));        //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("    ").append(XMLHandler.addTagValue("dbname_override", dbNameOverride));      //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("    ").append(XMLHandler.addTagValue("enclosure",       enclosure));        //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("    ").append(XMLHandler.addTagValue("delimiter",       delimiter));      //$NON-NLS-1$ //$NON-NLS-2$
    retval.append("    ").append(XMLHandler.addTagValue("stop_on_error",   stopOnError));    //$NON-NLS-1$ //$NON-NLS-2$
		
		for (int i=0;i<fieldTable.length;i++)
		{
			retval.append("      <mapping>").append(Const.CR); //$NON-NLS-1$
			retval.append("        ").append(XMLHandler.addTagValue("stream_name", fieldTable[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("field_name",  fieldStream[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        ").append(XMLHandler.addTagValue("date_mask",   dateMask[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("      </mapping>").append(Const.CR); //$NON-NLS-1$
		}

		return retval.toString();
	}

	public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters)
		throws KettleException
	{
		try
		{
			databaseMeta = rep.loadDatabaseMetaFromStepAttribute(id_step, "id_connection", databases);  //$NON-NLS-1$
            schemaName     =      rep.getStepAttributeString(id_step,  "schema");         //$NON-NLS-1$
			tableName      =      rep.getStepAttributeString(id_step,  "table");          //$NON-NLS-1$
			loadAction     =      rep.getStepAttributeString(id_step,  "load_action");    //$NON-NLS-1$
			PsqlPath         =      rep.getStepAttributeString(id_step,  "PsqlPath");         //$NON-NLS-1$
      stopOnError    =      rep.getStepAttributeBoolean(id_step,  "stop_on_error");    //$NON-NLS-1$

			dbNameOverride =      rep.getStepAttributeString(id_step,  "dbname_override");//$NON-NLS-1$			
         enclosure = rep.getStepAttributeString(id_step, "enclosure");//$NON-NLS-1$
         delimiter = rep.getStepAttributeString(id_step, "delimiter");//$NON-NLS-1$

			int nrvalues = rep.countNrStepAttributes(id_step, "stream_name");             //$NON-NLS-1$

			allocate(nrvalues);

			for (int i=0;i<nrvalues;i++)
			{
				fieldTable[i]  = rep.getStepAttributeString(id_step, i, "stream_name");   //$NON-NLS-1$
				fieldStream[i] = rep.getStepAttributeString(id_step, i, "field_name");    //$NON-NLS-1$
				dateMask[i]    = rep.getStepAttributeString(id_step, i, "date_mask");     //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.UnexpectedErrorReadingStepInfoFromRepository"), e); //$NON-NLS-1$
		}
	}

	public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step)
		throws KettleException
	{
		try
		{
			rep.saveDatabaseMetaStepAttribute(id_transformation, id_step, "id_connection", databaseMeta);

            rep.saveStepAttribute(id_transformation, id_step, "schema",          schemaName);    //$NON-NLS-1$
			rep.saveStepAttribute(id_transformation, id_step, "table",           tableName);     //$NON-NLS-1$
			
			rep.saveStepAttribute(id_transformation, id_step, "load_action",     loadAction);    //$NON-NLS-1$
			rep.saveStepAttribute(id_transformation, id_step, "PsqlPath",          PsqlPath);        //$NON-NLS-1$

			rep.saveStepAttribute(id_transformation, id_step, "dbname_override", dbNameOverride);//$NON-NLS-1$
         rep.saveStepAttribute(id_transformation, id_step, "enclosure", enclosure);//$NON-NLS-1$
         rep.saveStepAttribute(id_transformation, id_step, "delimiter", delimiter);//$NON-NLS-1$
      rep.saveStepAttribute(id_transformation, id_step, "stop_on_error", stopOnError);//$NON-NLS-1$

			for (int i=0;i<fieldTable.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "stream_name", fieldTable[i]);  //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "field_name",  fieldStream[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "date_mask",   dateMask[i]);    //$NON-NLS-1$
			}

			// Also, save the step-database relationship!
			if (databaseMeta!=null) rep.insertStepDatabase(id_transformation, id_step, databaseMeta.getObjectId());
		}
		catch(Exception e)
		{
			throw new KettleException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.UnableToSaveStepInfoToRepository")+id_step, e); //$NON-NLS-1$
		}
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException
	{
		// Default: nothing changes to rowMeta
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		String error_message = ""; //$NON-NLS-1$

		if (databaseMeta!=null)
		{
			Database db = new Database(loggingObject, databaseMeta);
			db.shareVariablesWith(transMeta);
			try
			{
				db.connect();

				if (!Const.isEmpty(tableName))
				{
					cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.TableNameOK"), stepMeta); //$NON-NLS-1$
					remarks.add(cr);

					boolean first=true;
					boolean error_found=false;
					error_message = ""; //$NON-NLS-1$
					
					// Check fields in table
                    String schemaTable = databaseMeta.getQuotedSchemaTableCombination(
                    		                   transMeta.environmentSubstitute(schemaName), 
                    		                   transMeta.environmentSubstitute(tableName));
					RowMetaInterface r = db.getTableFields(schemaTable);
					if (r!=null)
					{
						cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.TableExists"), stepMeta); //$NON-NLS-1$
						remarks.add(cr);

						// How about the fields to insert/dateMask in the table?
						first=true;
						error_found=false;
						error_message = ""; //$NON-NLS-1$
						
						for (int i=0;i<fieldTable.length;i++)
						{
							String field = fieldTable[i];

							ValueMetaInterface v = r.searchValueMeta(field);
							if (v==null)
							{
								if (first)
								{
									first=false;
									error_message+=BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.MissingFieldsToLoadInTargetTable")+Const.CR; //$NON-NLS-1$
								}
								error_found=true;
								error_message+="\t\t"+field+Const.CR;  //$NON-NLS-1$
							}
						}
						if (error_found)
						{
							cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
						}
						else
						{
							cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.AllFieldsFoundInTargetTable"), stepMeta); //$NON-NLS-1$
						}
						remarks.add(cr);
					}
					else
					{
						error_message=BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.CouldNotReadTableInfo"); //$NON-NLS-1$
						cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
						remarks.add(cr);
					}
				}

				// Look up fields in the input stream <prev>
				if (prev!=null && prev.size()>0)
				{
					cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.StepReceivingDatas",prev.size()+""), stepMeta); //$NON-NLS-1$ //$NON-NLS-2$
					remarks.add(cr);

					boolean first=true;
					error_message = ""; //$NON-NLS-1$
					boolean error_found = false;

					for (int i=0;i<fieldStream.length;i++)
					{
						ValueMetaInterface v = prev.searchValueMeta(fieldStream[i]);
						if (v==null)
						{
							if (first)
							{
								first=false;
								error_message+=BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.MissingFieldsInInput")+Const.CR; //$NON-NLS-1$
							}
							error_found=true;
							error_message+="\t\t"+fieldStream[i]+Const.CR;  //$NON-NLS-1$
						}
					}
					if (error_found)
 					{
						cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
					}
					else
					{
						cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.AllFieldsFoundInInput"), stepMeta); //$NON-NLS-1$
					}
					remarks.add(cr);
				}
				else
				{
					error_message=BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.MissingFieldsInInput3")+Const.CR; //$NON-NLS-1$
					cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
					remarks.add(cr);
				}
			}
			catch(KettleException e)
			{
				error_message = BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.DatabaseErrorOccurred")+e.getMessage(); //$NON-NLS-1$
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
				remarks.add(cr);
			}
			finally
			{
				db.disconnect();
			}
		}
		else
		{
			error_message = BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.InvalidConnection"); //$NON-NLS-1$
			cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
			remarks.add(cr);
		}

		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.StepReceivingInfoFromOtherSteps"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "GPBulkLoaderMeta.CheckResult.NoInputError"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
	}

	public SQLStatement getSQLStatements(TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev) throws KettleStepException
	{
		SQLStatement retval = new SQLStatement(stepMeta.getName(), databaseMeta, null); // default: nothing to do!

		if (databaseMeta!=null)
		{
			if (prev!=null && prev.size()>0)
			{
                // Copy the row
                RowMetaInterface tableFields = new RowMeta();

                // Now change the field names
                for (int i=0;i<fieldTable.length;i++)
                {
                    ValueMetaInterface v = prev.searchValueMeta(fieldStream[i]);
                    if (v!=null)
                    {
                        ValueMetaInterface tableField = v.clone();
                        tableField.setName(fieldTable[i]);
                        tableFields.addValueMeta(tableField);
                    }
                    else
                    {
                        throw new KettleStepException("Unable to find field ["+fieldStream[i]+"] in the input rows");
                    }
                }

				if (!Const.isEmpty(tableName))
				{
                    Database db = new Database(loggingObject, databaseMeta);
                    db.shareVariablesWith(transMeta);
					try
					{
						db.connect();

                        String schemaTable = databaseMeta.getQuotedSchemaTableCombination(transMeta.environmentSubstitute(schemaName), 
                        		                                                          transMeta.environmentSubstitute(tableName));                        
						String sql = db.getDDL(schemaTable,
													tableFields,
													null,
													false,
													null,
													true
													);

						if (sql.length()==0) retval.setSQL(null); else retval.setSQL(sql);
					}
					catch(KettleException e)
					{
						retval.setError(BaseMessages.getString(PKG, "GPBulkLoaderMeta.GetSQL.ErrorOccurred")+e.getMessage()); //$NON-NLS-1$
					}
				}
				else
				{
					retval.setError(BaseMessages.getString(PKG, "GPBulkLoaderMeta.GetSQL.NoTableDefinedOnConnection")); //$NON-NLS-1$
				}
			}
			else
			{
				retval.setError(BaseMessages.getString(PKG, "GPBulkLoaderMeta.GetSQL.NotReceivingAnyFields")); //$NON-NLS-1$
			}
		}
		else
		{
			retval.setError(BaseMessages.getString(PKG, "GPBulkLoaderMeta.GetSQL.NoConnectionDefined")); //$NON-NLS-1$
		}

		return retval;
	}

	public void analyseImpact(List<DatabaseImpact> impact, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) throws KettleStepException
    {
        if (prev != null)
        {
            /* DEBUG CHECK THIS */
            // Insert dateMask fields : read/write
            for (int i = 0; i < fieldTable.length; i++)
            {
                ValueMetaInterface v = prev.searchValueMeta(fieldStream[i]);

                DatabaseImpact ii = new DatabaseImpact(DatabaseImpact.TYPE_IMPACT_READ_WRITE, transMeta.getName(), stepMeta.getName(), databaseMeta
                        .getDatabaseName(), transMeta.environmentSubstitute(tableName), fieldTable[i], fieldStream[i], v!=null?v.getOrigin():"?", "", "Type = " + v.toStringMeta()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                impact.add(ii);
            }
        }
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new PGBulkLoader(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new PGBulkLoaderData();
	}

    public DatabaseMeta[] getUsedDatabaseConnections()
    {
        if (databaseMeta!=null)
        {
            return new DatabaseMeta[] { databaseMeta };
        }
        else
        {
            return super.getUsedDatabaseConnections();
        }
    }

    public RowMetaInterface getRequiredFields(VariableSpace space) throws KettleException
    {
    	String realTableName = space.environmentSubstitute(tableName);
    	String realSchemaName = space.environmentSubstitute(schemaName);

        if (databaseMeta!=null)
        {
            Database db = new Database(loggingObject, databaseMeta);
            try
            {
                db.connect();

                if (!Const.isEmpty(realTableName))
                {
                    String schemaTable = databaseMeta.getQuotedSchemaTableCombination(realSchemaName, realTableName);

                    // Check if this table exists...
                    if (db.checkTableExists(schemaTable))
                    {
                        return db.getTableFields(schemaTable);
                    }
                    else
                    {
                        throw new KettleException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.TableNotFound"));
                    }
                }
                else
                {
                    throw new KettleException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.TableNotSpecified"));
                }
            }
            catch(Exception e)
            {
                throw new KettleException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.ErrorGettingFields"), e);
            }
            finally
            {
                db.disconnect();
            }
        }
        else
        {
            throw new KettleException(BaseMessages.getString(PKG, "GPBulkLoaderMeta.Exception.ConnectionNotDefined"));
        }

    }

    /**
     * @return the schemaName
     */
    public String getSchemaName()
    {
        return schemaName;
    }

    /**
     * @param schemaName the schemaName to set
     */
    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

	public void setLoadAction(String action)
	{
	    this.loadAction = action;
	}

	public String getLoadAction()
	{
	    return this.loadAction;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public String getEnclosure() {
		return enclosure;
	}

	public String getDbNameOverride() {
		return dbNameOverride;
	}

	public void setDbNameOverride(String dbNameOverride) {
		this.dbNameOverride = dbNameOverride;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setEnclosure(String enclosure) {
		this.enclosure = enclosure;
	}	
	
	public boolean isStopOnError() {
	  return this.stopOnError;
	}
	
	public void setStopOnError(Boolean value) {
	  this.stopOnError = value;
	}
	
}