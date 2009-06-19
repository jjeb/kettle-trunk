package org.pentaho.di.repository;

public enum RepositoryOperation {

	READ_TRANSFORMATION("Read transformation"),
	MODIFY_TRANSFORMATION("Modify transformation"),
	DELETE_TRANSFORMATION("Delete transformation"),
	EXECUTE_TRANSFORMATION("Execute transformation"),
	LOCK_TRANSFORMATION("Lock transformation"),
	
	READ_JOB("Read job"),
	MODIFY_JOB("Modify job"),
	DELETE_JOB("Delete job"),
	EXECUTE_JOB("Execute job"),
	LOCK_JOB("Lock job"),
	
	MODIFY_DATABASE("Modify database connection"),
	DELETE_DATABASE("Delete database connection"),
	EXPLORE_DATABASE("Explore database connection"),
	
	MODIFY_SLAVE_SERVER("Modify slave server"),
	DELETE_SLAVE_SERVER("Delete slave server"),

	MODIFY_CLUSTER_SCHEMA("Modify cluster schema"),
	DELETE_CLUSTER_SCHEMA("Delete cluster schema"),

	MODIFY_PARTITION_SCHEMA("Modify partition schema"),
	DELETE_PARTITION_SCHEMA("Delete partition schema"),
	
	CREATE_DIRECTORY("Create directory"),
	RENAME_DIRECTORY("Rename directory"),
	DELETE_DIRECTORY("Delete directory"),

	;

	private final String description;
	
	RepositoryOperation(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String toString() {
		return description;
	}
}
