package org.pentaho.di.repository.kdr.delegates;

import java.util.List;

import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleDependencyException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;

public class KettleDatabaseRepositoryClusterSchemaDelegate extends KettleDatabaseRepositoryBaseDelegate {

//	private static Class<?> PKG = ClusterSchema.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	public KettleDatabaseRepositoryClusterSchemaDelegate(KettleDatabaseRepository repository) {
		super(repository);
	}
	
    public RowMetaAndData getClusterSchema(ObjectId id_cluster_schema) throws KettleException
    {
        return repository.connectionDelegate.getOneRow(quoteTable(KettleDatabaseRepository.TABLE_R_CLUSTER), quote(KettleDatabaseRepository.FIELD_CLUSTER_ID_CLUSTER), id_cluster_schema);
    }
	
    public synchronized ObjectId getClusterID(String name) throws KettleException
    {
        return repository.connectionDelegate.getIDWithValue(quoteTable(KettleDatabaseRepository.TABLE_R_CLUSTER), quote(KettleDatabaseRepository.FIELD_CLUSTER_ID_CLUSTER), quote(KettleDatabaseRepository.FIELD_CLUSTER_NAME), name);
    }

    public ClusterSchema loadClusterSchema(ObjectId id_cluster_schema, List<SlaveServer> slaveServers) throws KettleException
    {
    	ClusterSchema clusterSchema = new ClusterSchema();
        RowMetaAndData row = getClusterSchema(id_cluster_schema);
            
        clusterSchema.setName( row.getString(KettleDatabaseRepository.FIELD_CLUSTER_NAME, null) );
        clusterSchema.setBasePort( row.getString(KettleDatabaseRepository.FIELD_CLUSTER_BASE_PORT, null) );
        clusterSchema.setSocketsBufferSize( row.getString(KettleDatabaseRepository.FIELD_CLUSTER_SOCKETS_BUFFER_SIZE, null) );
        clusterSchema.setSocketsFlushInterval( row.getString(KettleDatabaseRepository.FIELD_CLUSTER_SOCKETS_FLUSH_INTERVAL, null) );
        clusterSchema.setSocketsCompressed( row.getBoolean(KettleDatabaseRepository.FIELD_CLUSTER_SOCKETS_COMPRESSED, true) );
        clusterSchema.setDynamic( row.getBoolean(KettleDatabaseRepository.FIELD_CLUSTER_DYNAMIC, true) );
            
        ObjectId[] pids = repository.getClusterSlaveIDs(id_cluster_schema);
        for (int i=0;i<pids.length;i++)
        {
            SlaveServer slaveServer = repository.loadSlaveServer(pids[i], null);  // Load last version
            SlaveServer reference = SlaveServer.findSlaveServer(slaveServers, slaveServer.getName());
            if (reference!=null) 
                clusterSchema.getSlaveServers().add(reference);
            else 
                clusterSchema.getSlaveServers().add(slaveServer);
        }
        
        return clusterSchema;
    }

    public void saveClusterSchema(ClusterSchema clusterSchema, String versionComment) throws KettleException
    {
        saveClusterSchema(clusterSchema, versionComment, null, false);
    }

    public void saveClusterSchema(ClusterSchema clusterSchema, String versionComment, ObjectId id_transformation, boolean isUsedByTransformation) throws KettleException
    {
        clusterSchema.setObjectId(getClusterID(clusterSchema.getName()));
        if (clusterSchema.getObjectId()==null)
        {
            // Save the cluster
        	clusterSchema.setObjectId(insertCluster(clusterSchema));
        }
        else
        {
            repository.delClusterSlaves(clusterSchema.getObjectId());
        }
        
        // Also save the used slave server references.
        for (int i=0;i<clusterSchema.getSlaveServers().size();i++)
        {
            SlaveServer slaveServer = clusterSchema.getSlaveServers().get(i);
            if (slaveServer.getObjectId()==null) // oops, not yet saved!
            {
            	repository.save(slaveServer, versionComment, null, id_transformation, isUsedByTransformation);
            }
            repository.insertClusterSlave(clusterSchema, slaveServer);
        }
        
        // Save a link to the transformation to keep track of the use of this partition schema
        // Only save it if it's really used by the transformation
        if (isUsedByTransformation)
        {
            repository.insertTransformationCluster(id_transformation, clusterSchema.getObjectId());
        }
    }

    private synchronized ObjectId insertCluster(ClusterSchema clusterSchema) throws KettleException
    {
    	ObjectId id = repository.connectionDelegate.getNextClusterID();

        RowMetaAndData table = new RowMetaAndData();

        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_ID_CLUSTER, ValueMetaInterface.TYPE_INTEGER), id);
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_NAME, ValueMetaInterface.TYPE_STRING), clusterSchema.getName());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_BASE_PORT, ValueMetaInterface.TYPE_STRING), clusterSchema.getBasePort());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_SOCKETS_BUFFER_SIZE, ValueMetaInterface.TYPE_STRING), clusterSchema.getSocketsBufferSize());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_SOCKETS_FLUSH_INTERVAL, ValueMetaInterface.TYPE_STRING), clusterSchema.getSocketsFlushInterval());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_SOCKETS_COMPRESSED, ValueMetaInterface.TYPE_BOOLEAN), Boolean.valueOf(clusterSchema.isSocketsCompressed()));
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CLUSTER_DYNAMIC, ValueMetaInterface.TYPE_BOOLEAN), Boolean.valueOf(clusterSchema.isDynamic()));

        repository.connectionDelegate.getDatabase().prepareInsert(table.getRowMeta(), KettleDatabaseRepository.TABLE_R_CLUSTER);
        repository.connectionDelegate.getDatabase().setValuesInsert(table);
        repository.connectionDelegate.getDatabase().insertRow();
        repository.connectionDelegate.getDatabase().closeInsert();

        return id;
    }

    public synchronized void delClusterSchema(ObjectId id_cluster) throws KettleException
    {
        // First, see if the schema is still used by other objects...
        // If so, generate an error!!
        //
        // We look in table R_TRANS_CLUSTER to see if there are any transformations using this schema.
        String[] transList = repository.getTransformationsUsingCluster(id_cluster);

        if (transList.length==0)
        {
            repository.connectionDelegate.getDatabase().execStatement("DELETE FROM "+quoteTable(KettleDatabaseRepository.TABLE_R_CLUSTER)+" WHERE "+quote(KettleDatabaseRepository.FIELD_CLUSTER_ID_CLUSTER)+" = " + id_cluster);
        }
        else
        {
            StringBuffer message = new StringBuffer();
            
            message.append("The cluster schema is used by the following transformations:").append(Const.CR);
            for (int i = 0; i < transList.length; i++)
            {
                message.append("  ").append(transList[i]).append(Const.CR);
            }
            message.append(Const.CR);
            
            KettleDependencyException e = new KettleDependencyException(message.toString());
            throw new KettleDependencyException("This cluster schema is still in use by one or more transformations ("+transList.length+") :", e);
        }
    }


}
