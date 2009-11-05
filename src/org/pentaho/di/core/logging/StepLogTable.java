package org.pentaho.di.core.logging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.w3c.dom.Node;

/**
 * This class describes a logging channel logging table
 * 
 * @author matt
 *
 */
public class StepLogTable extends BaseLogTable implements Cloneable, LogTableInterface {

	private static Class<?> PKG = StepLogTable.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	public static final String	XML_TAG	= "step-log-table";
	
	public enum ID {
		
		ID_BATCH("ID_BATCH"),
		CHANNEL_ID("CHANNEL_ID"),
		LOG_DATE("LOG_DATE"),
		TRANSNAME("TRANSNAME"),
		STEPNAME("STEPNAME"),
		STEP_COPY("STEP_COPY"),
		LINES_READ("LINES_READ"),
		LINES_WRITTEN("LINES_WRITTEN"),
		LINES_UPDATED("LINES_UPDATED"),
		LINES_INPUT("LINES_INPUT"),
		LINES_OUTPUT("LINES_OUTPUT"),
		LINES_REJECTED("LINES_REJECTED"),
		ERRORS("ERRORS"),
		LOG_FIELD("LOG_FIELD");
		;
		
		private String id;
		private ID(String id) {
			this.id = id;
		}

		public String toString() {
			return id;
		}
	}
		
	/**
	 * Create a new transformation logging table description.
	 * It contains an empty list of log table fields.
	 * 
	 * @param databaseMeta
	 * @param schemaName
	 * @param tableName
	 */
	public StepLogTable(DatabaseMeta databaseMeta, String schemaName, String tableName) {
		super(databaseMeta, schemaName, tableName);
	}
	
	public StepLogTable() {
		this(null, null, null);
	}
	
	@Override
	public Object clone() {
		try {
			StepLogTable table = (StepLogTable) super.clone();
			table.fields = new ArrayList<LogTableField>();
			for (LogTableField field : this.fields) {
				table.fields.add((LogTableField) field.clone());
			}
			return table;
		}
		catch(CloneNotSupportedException e) {
			return null;
		}
	}

	public String getXML() {
		StringBuffer retval = new StringBuffer();

		retval.append(XMLHandler.openTag(XML_TAG));
        retval.append(XMLHandler.addTagValue("connection", databaseMeta==null ?  null  : databaseMeta.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        retval.append(XMLHandler.addTagValue("schema", schemaName)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append(XMLHandler.addTagValue("table", tableName)); //$NON-NLS-1$ //$NON-NLS-2$
		retval.append(super.getFieldsXML());
		retval.append(XMLHandler.closeTag(XML_TAG)).append(Const.CR);
		
		return retval.toString();
	}
	
	public void loadXML(Node node, List<DatabaseMeta> databases) {
		databaseMeta = DatabaseMeta.findDatabase(databases, XMLHandler.getTagValue(node, "connection"));
		schemaName = XMLHandler.getTagValue(node, "schema");
		tableName = XMLHandler.getTagValue(node, "table");
		
		for (int i=0;i<fields.size();i++) {
			LogTableField field = fields.get(i);
			Node fieldNode = XMLHandler.getSubNodeByNr(node, BaseLogTable.XML_TAG, i);
			field.setFieldName( XMLHandler.getTagValue(fieldNode, "name") );
			field.setEnabled( "Y".equalsIgnoreCase(XMLHandler.getTagValue(fieldNode, "enabled")) );
		}
	}

	public static StepLogTable getDefault() {
		StepLogTable table = new StepLogTable();
		
		table.fields.add( new LogTableField(ID.ID_BATCH.id, true, false, "ID_BATCH", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.IdBatch"), ValueMetaInterface.TYPE_INTEGER, 8) );
		table.fields.add( new LogTableField(ID.CHANNEL_ID.id, true, false, "CHANNEL_ID", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.ChannelId"), ValueMetaInterface.TYPE_STRING, 255) );
		table.fields.add( new LogTableField(ID.LOG_DATE.id, true, false, "LOG_DATE", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LogDate"), ValueMetaInterface.TYPE_DATE, -1) );
		table.fields.add( new LogTableField(ID.TRANSNAME.id, true, false, "TRANSNAME", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.TransName"), ValueMetaInterface.TYPE_STRING, 255) );
		table.fields.add( new LogTableField(ID.STEPNAME.id, true, false, "STEPNAME", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.StepName"), ValueMetaInterface.TYPE_STRING, 255) );
		table.fields.add( new LogTableField(ID.STEP_COPY.id, true, false, "STEP_COPY", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.StepCopy"), ValueMetaInterface.TYPE_INTEGER, 3) );
		table.fields.add( new LogTableField(ID.LINES_READ.id, true, false, "LINES_READ", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LinesRead"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_WRITTEN.id, true, false, "LINES_WRITTEN", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LinesWritten"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_UPDATED.id, true, false, "LINES_UPDATED", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LinesUpdated"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_INPUT.id, true, false, "LINES_INPUT", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LinesInput"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_OUTPUT.id, true, false, "LINES_OUTPUT", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LinesOutput"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_REJECTED.id, true, false, "LINES_REJECTED", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LinesRejected"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.ERRORS.id, true, false, "ERRORS", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.Errors"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LOG_FIELD.id, false, false, "LOG_FIELD", BaseMessages.getString(PKG, "StepLogTable.FieldDescription.LogField"), ValueMetaInterface.TYPE_STRING, DatabaseMeta.CLOB_LENGTH) );
		
		return table;
	}
		
	/**
	 * This method calculates all the values that are required
	 * @param id the id to use or -1 if no id is needed
	 * @param status the log status to use
	 */
	public RowMetaAndData getLogRecord(LogStatus status, Object subject) {
		if (subject==null || subject instanceof StepMetaDataCombi) {
			
			StepMetaDataCombi combi = (StepMetaDataCombi) subject;
			
			RowMetaAndData row = new RowMetaAndData();
			
			for (LogTableField field : fields) {
				if (field.isEnabled()) {
					Object value = null;
					if (subject!=null) {
						switch(ID.valueOf(field.getId())){
						
						case ID_BATCH : value = new Long(combi.step.getTrans().getBatchId()); break;
						case CHANNEL_ID : value = combi.step.getLogChannel().getLogChannelId(); break;
						case LOG_DATE : value = new Date(); break;
						case TRANSNAME : value = combi.step.getTrans().getName(); break;
						case STEPNAME : value = combi.stepname; break;
						case STEP_COPY : value = new Long(combi.copy); break;
						case LINES_READ : value = new Long(combi.step.getLinesRead()); break;
						case LINES_WRITTEN : value = new Long(combi.step.getLinesWritten()); break;
						case LINES_UPDATED : value = new Long(combi.step.getLinesUpdated()); break;
						case LINES_INPUT : value = new Long(combi.step.getLinesInput()); break;
						case LINES_OUTPUT : value = new Long(combi.step.getLinesOutput()); break;
						case LINES_REJECTED : value = new Long(combi.step.getLinesRejected()); break;
						case ERRORS : value = new Long(combi.step.getErrors()); break;
						case LOG_FIELD : 
							StringBuffer buffer = CentralLogStore.getAppender().getBuffer(combi.step.getLogChannel().getLogChannelId(), false);
							value = buffer.toString();
							break;
						}
					}

					row.addValue(field.getFieldName(), field.getDataType(), value);
					row.getRowMeta().getValueMeta(row.size()-1).setLength(field.getLength());
				}
			}
				
			return row;
		}
		else {
			return null;
		}
	}

	public String getLogTableType() {
		return BaseMessages.getString(PKG, "ChannelLogTable.Type.Description");
	}

	
}