package org.pentaho.di.core.logging;


/**
 * The logging hierarchy of a transformation or job
 * 
 * @author matt
 *
 */
public class LoggingHierarchy {
	private String rootChannelId; // from the xform or job
	private long   batchId; // from the xform or job
	private LoggingObjectInterface loggingObject;
	/**
	 * @return the rootChannelId
	 */
	public String getRootChannelId() {
		return rootChannelId;
	}
	/**
	 * @param rootChannelId the rootChannelId to set
	 */
	public void setRootChannelId(String rootChannelId) {
		this.rootChannelId = rootChannelId;
	}
	/**
	 * @return the batchId
	 */
	public long getBatchId() {
		return batchId;
	}
	/**
	 * @param batchId the batchId to set
	 */
	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}
	/**
	 * @return the loggingObject
	 */
	public LoggingObjectInterface getLoggingObject() {
		return loggingObject;
	}
	/**
	 * @param loggingObject the loggingObject to set
	 */
	public void setLoggingObject(LoggingObjectInterface loggingObject) {
		this.loggingObject = loggingObject;
	}
	/**
	 * @param rootChannelId
	 * @param batchId
	 * @param loggingObject
	 */
	public LoggingHierarchy(String rootChannelId, long batchId, LoggingObjectInterface loggingObject) {
		this.rootChannelId = rootChannelId;
		this.batchId = batchId;
		this.loggingObject = loggingObject;
	}

	
}
