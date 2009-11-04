package org.pentaho.di.core.logging;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.performance.StepPerformanceSnapShot;
import org.w3c.dom.Node;

/**
 * This class describes a step performance logging table
 * 
 * @author matt
 *
 */
public class PerformanceLogTable extends BaseLogTable implements Cloneable, LogTableInterface {

	private static Class<?> PKG = PerformanceLogTable.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	public static final String	XML_TAG	= "perf-log-table";
	
	public enum ID {
		
		ID_BATCH("ID_BATCH"),
		SEQ_NR("SEQ_NR"),
		LOGDATE("LOGDATE"),
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
		INPUT_BUFFER_ROWS("INPUT_BUFFER_ROWS"),
		OUTPUT_BUFFER_ROWS("OUTPUT_BUFFER_ROWS"),
		;
		
		private String id;
		private ID(String id) {
			this.id = id;
		}

		public String toString() {
			return id;
		}
	}
	
	private String logInterval;
		
	/**
	 * Create a new transformation logging table description.
	 * It contains an empty list of log table fields.
	 * 
	 * @param databaseMeta
	 * @param schemaName
	 * @param tableName
	 */
	public PerformanceLogTable(DatabaseMeta databaseMeta, String schemaName, String tableName) {
		super(databaseMeta, schemaName, tableName);
		this.logInterval = null;
	}
	
	public PerformanceLogTable() {
		this(null, null, null);
	}
	
	@Override
	public Object clone() {
		try {
			PerformanceLogTable table = (PerformanceLogTable) super.clone();
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
        retval.append(XMLHandler.addTagValue("interval", logInterval)); //$NON-NLS-1$ //$NON-NLS-2$
		retval.append(super.getFieldsXML());
		retval.append(XMLHandler.closeTag(XML_TAG)).append(Const.CR);
		
		return retval.toString();
	}
	
	public void loadXML(Node node, List<DatabaseMeta> databases) {
		databaseMeta = DatabaseMeta.findDatabase(databases, XMLHandler.getTagValue(node, "connection"));
		schemaName = XMLHandler.getTagValue(node, "schema");
		tableName = XMLHandler.getTagValue(node, "table");
		logInterval = XMLHandler.getTagValue(node, "interval");
		
		for (int i=0;i<fields.size();i++) {
			LogTableField field = fields.get(i);
			Node fieldNode = XMLHandler.getSubNodeByNr(node, BaseLogTable.XML_TAG, i);
			field.setFieldName( XMLHandler.getTagValue(fieldNode, "name") );
			field.setEnabled( "Y".equalsIgnoreCase(XMLHandler.getTagValue(fieldNode, "enabled")) );
		}
	}

	public static PerformanceLogTable getDefault() {
		PerformanceLogTable table = new PerformanceLogTable();
		
		table.fields.add( new LogTableField(ID.ID_BATCH.id, true, false, "ID_BATCH", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.BatchID"), ValueMetaInterface.TYPE_INTEGER, 8) );
		table.fields.add( new LogTableField(ID.SEQ_NR.id, true, false, "SEQ_NR", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.SeqNr"), ValueMetaInterface.TYPE_INTEGER, 8) );
		table.fields.add( new LogTableField(ID.LOGDATE.id, true, false, "LOGDATE", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LogDate"), ValueMetaInterface.TYPE_DATE, -1) );
		table.fields.add( new LogTableField(ID.TRANSNAME.id, true, false, "TRANSNAME", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.TransName"), ValueMetaInterface.TYPE_STRING, 255) );
		table.fields.add( new LogTableField(ID.STEPNAME.id, true, false, "STEPNAME", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.StepName"), ValueMetaInterface.TYPE_STRING, 255) );
		table.fields.add( new LogTableField(ID.STEP_COPY.id, true, false, "STEP_COPY", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.StepCopy"), ValueMetaInterface.TYPE_INTEGER, 8) );
		table.fields.add( new LogTableField(ID.LINES_READ.id, true, false, "LINES_READ", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LinesRead"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_WRITTEN.id, true, false, "LINES_WRITTEN", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LinesWritten"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_UPDATED.id, true, false, "LINES_UPDATED", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LinesUpdated"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_INPUT.id, true, false, "LINES_INPUT", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LinesInput"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_OUTPUT.id, true, false, "LINES_OUTPUT", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LinesOutput"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.LINES_REJECTED.id, true, false, "LINES_REJECTED", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.LinesRejected"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.ERRORS.id, true, false, "ERRORS", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.Errors"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.INPUT_BUFFER_ROWS.id, true, false, "INPUT_BUFFER_ROWS", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.InputBufferRows"), ValueMetaInterface.TYPE_INTEGER, 18) );
		table.fields.add( new LogTableField(ID.OUTPUT_BUFFER_ROWS.id, true, false, "OUTPUT_BUFFER_ROWS", BaseMessages.getString(PKG, "PerformanceLogTable.FieldDescription.OutputBufferRows"), ValueMetaInterface.TYPE_INTEGER, 18) );
		
		table.findField(ID.ID_BATCH.id).setKey(true);
		
		return table;
	}
		
	/**
	 * Sets the logging interval in seconds.
	 * Disabled if the logging interval is <=0.
	 * 
	 * @param logInterval The log interval value.  A value higher than 0 means that the log table is updated every 'logInterval' seconds.
	 */
	public void setLogInterval(String logInterval) {
		this.logInterval = logInterval;
	}

	/**
	 * Get the logging interval in seconds.
	 * Disabled if the logging interval is <=0.
	 * A value higher than 0 means that the log table is updated every 'logInterval' seconds.
	 * 
	 * @param logInterval The log interval, 
	 */
	public String getLogInterval() {
		return logInterval;
	}

	/**
	 * This method calculates all the values that are required
	 * @param id the id to use or -1 if no id is needed
	 * @param status the log status to use
	 */
	public RowMetaAndData getLogRecord(LogStatus status, Object subject) {
		if (subject==null || subject instanceof StepPerformanceSnapShot) {
			StepPerformanceSnapShot snapShot = (StepPerformanceSnapShot) subject;
			
			RowMetaAndData row = new RowMetaAndData();
			
			for (LogTableField field : fields) {
				if (field.isEnabled()) {
					Object value = null;
					if (subject!=null) {
						switch(ID.valueOf(field.getId())){
						
						case ID_BATCH : value = new Long(snapShot.getBatchId()); break;
						case SEQ_NR :  value = new Long(snapShot.getSeqNr()); break;
						case LOGDATE: value = snapShot.getDate(); break;
						case TRANSNAME : value = snapShot.getTransName(); break;
						case STEPNAME:  value = snapShot.getStepName(); break;
						case STEP_COPY:  value = new Long(snapShot.getStepCopy()); break;
						case LINES_READ : value = new Long(snapShot.getLinesRead()); break;
						case LINES_WRITTEN : value = new Long(snapShot.getLinesWritten()); break;
						case LINES_INPUT : value = new Long(snapShot.getLinesInput()); break;
						case LINES_OUTPUT : value = new Long(snapShot.getLinesOutput()); break;
						case LINES_UPDATED : value = new Long(snapShot.getLinesUpdated()); break;
						case LINES_REJECTED : value = new Long(snapShot.getLinesRejected()); break;
						case ERRORS: value = new Long(snapShot.getErrors()); break;
						case INPUT_BUFFER_ROWS: value = new Long(snapShot.getInputBufferSize()); break;
						case OUTPUT_BUFFER_ROWS: value = new Long(snapShot.getOutputBufferSize()); break;
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
		return BaseMessages.getString(PKG, "PerformanceLogTable.Type.Description");
	}
	
}
