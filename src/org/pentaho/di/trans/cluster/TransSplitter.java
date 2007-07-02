package org.pentaho.di.trans.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepPartitioningMeta;
import org.pentaho.di.trans.steps.socketreader.SocketReaderMeta;
import org.pentaho.di.trans.steps.socketwriter.SocketWriterMeta;

/**
 * This class takes care of the separation of the original transformation into pieces that run on the different slave servers in the clusters used.
 * 
 * @author Matt
 *
 */
public class TransSplitter
{
    private static final int FANOUT = 30;
    private static final int SPLIT  = 120;
    
    private TransMeta  originalTransformation;
    private Map<ClusterSchema,Integer>        clusterPortMap;
    private Map<String,Integer>        clusterStepPortMap;
    private Map<SlaveServer,TransMeta>        slaveTransMap;
    private TransMeta  master;
    private StepMeta[] originalSteps;
    private Map<SlaveServer,Map<PartitionSchema,List<String>>>        slaveServerPartitionsMap;
    private Map<TransMeta,Map<StepMeta,String>>        slaveStepPartitionFlag;

    public TransSplitter()
    {
        clusterPortMap = new Hashtable<ClusterSchema,Integer>();
        clusterStepPortMap = new Hashtable<String,Integer>();
        
        slaveTransMap = new Hashtable<SlaveServer,TransMeta>();
        slaveStepPartitionFlag = new Hashtable<TransMeta,Map<StepMeta,String>>();
    }
    
    /**
     * @param originalTransformation
     */
    public TransSplitter(TransMeta originalTransformation)
    {
        this();
        this.originalTransformation = originalTransformation;
    }

    /**
     * @return the originalTransformation
     */
    public TransMeta getOriginalTransformation()
    {
        return originalTransformation;
    }

    /**
     * @param originalTransformation the originalTransformation to set
     */
    public void setOriginalTransformation(TransMeta originalTransformation)
    {
        this.originalTransformation = originalTransformation;
    }

    
    private void checkClusterConfiguration() throws KettleException
    {
        Map<String,ClusterSchema> map = new Hashtable<String,ClusterSchema>();
        StepMeta[] steps = originalTransformation.getStepsArray();
        for (int i=0;i<steps.length;i++)
        {
            ClusterSchema clusterSchema = steps[i].getClusterSchema(); 
            if (clusterSchema!=null)
            {
                map.put(steps[i].getClusterSchema().getName(), steps[i].getClusterSchema());
                
                if (clusterSchema.findMaster()==null)
                {
                    throw new KettleException("No master server was specified in cluster schema ["+clusterSchema+"]");
                }
            }
        }
        if (map.size()==0)
        {
            throw new KettleException("No cluster schemas are being used.  As such it is not possible to split and cluster this transformation.");
        }
        if (map.size()>1)
        {
            throw new KettleException("At this time we don't support the use of multiple cluster schemas in one and the same transformation.");
        }
    }

    private String getWriterName(String stepname, ClusterSchema clusterSchema, SlaveServer slaveServer)
    {
        return "Writer : "+getPort(clusterSchema, slaveServer, stepname);
    }
    
    private String getReaderName(String stepname, ClusterSchema clusterSchema, SlaveServer slaveServer)
    {
        return "Reader : "+getPort(clusterSchema, slaveServer, stepname);
    }

    private String getSlaveTransName(String transName, ClusterSchema clusterSchema, SlaveServer slaveServer)
    {
        return transName + " ("+clusterSchema+":"+slaveServer.getName()+")";
    }
    
    /**
     * Get the port for the given cluster schema, slave server and step
     * If a port was allocated, that is returned, otherwise a new one is allocated.
     * 
     * @param clusterSchema The cluster schema 
     * @param slaveServer The slave server
     * @param stepname the step name without reader/writer denotion.
     * 
     * @return the port to use for that step/slaveserver/cluster combination
     */
    private int getPort(ClusterSchema clusterSchema, SlaveServer slaveServer, String stepname)
    {
        String key = clusterSchema.getName()+" - "+slaveServer + " - " + stepname;
        int p;
        Integer port = (Integer) clusterStepPortMap.get(key);
        if (port==null)
        {
            p = getNextPort(clusterSchema);
            clusterStepPortMap.put(key, new Integer(p));
        }
        else
        {
            p = port.intValue();
        }
        
        return p;
    }
    
    /**
     * Allocates a new port for the cluster.  This port has to be unique in the master, so it has to increase with each call.
     * @param clusterSchema the cluster schema to allocate for.  Each clusterSchema should have it's own range.
     * @return the next port number
     */
    private int getNextPort(ClusterSchema clusterSchema)
    {
        int p;
        Integer port = (Integer) clusterPortMap.get(clusterSchema);
        if (port==null)
        {
            p = Integer.parseInt( clusterSchema.environmentSubstitute(clusterSchema.getBasePort())  );
        }
        else
        {
            p = port.intValue()+1;
        }
        clusterPortMap.put(clusterSchema, new Integer(p));
        return p;
    }
    
    /**
     * Create or get a slave transformation for the specified cluster & slave server
     * @param clusterSchema the cluster schema to reference
     * @param slaveServer the slave server to reference
     * @return
     */
    private TransMeta getSlaveTransformation(ClusterSchema clusterSchema, SlaveServer slaveServer) throws KettleException
    {
        TransMeta slave = (TransMeta) slaveTransMap.get(slaveServer);
        if (slave==null)
        {
            slave = getOriginalCopy(true, clusterSchema, slaveServer);
            slaveTransMap.put(slaveServer, slave);
        }
        return slave;
    }

    private TransMeta getOriginalCopy(boolean isSlaveTrans, ClusterSchema clusterSchema, SlaveServer slaveServer) throws KettleException
    {
        TransMeta transMeta = new TransMeta();
        if (isSlaveTrans)
        {
            transMeta.setName(getSlaveTransName(originalTransformation.getName(), clusterSchema, slaveServer));
            
            NotePadMeta slaveNote = new NotePadMeta("This is a generated slave transformation.\nIt will be run on slave server: "+slaveServer, 0, 0, -1, -1);
            transMeta.addNote(slaveNote);
            
            // add the slave database partitioning schema's here.
            for (int i=0;i<originalSteps.length;i++)
            {
                StepMeta stepMeta = originalSteps[i];
                verifySlavePartitioningConfiguration(transMeta, stepMeta, clusterSchema, slaveServer);
            }
        }
        else
        {
            transMeta.setName(originalTransformation.getName()+" (master)");

            NotePadMeta masterNote = new NotePadMeta("This is a generated master transformation.\nIt will be run on server: "+getMasterServer(), 0, 0, -1, -1);
            transMeta.addNote(masterNote);

            transMeta.setPartitionSchemas(originalTransformation.getPartitionSchemas());
        }
        transMeta.setClusterSchemas(originalTransformation.getClusterSchemas());
        transMeta.setDatabases(originalTransformation.getDatabases());

        // Feedback
        transMeta.setFeedbackShown(originalTransformation.isFeedbackShown());
        transMeta.setFeedbackSize(originalTransformation.getFeedbackSize());
        
        // Priority management
        transMeta.setUsingThreadPriorityManagment(originalTransformation.isUsingThreadPriorityManagment());

        // Unique connections
        transMeta.setUsingUniqueConnections(originalTransformation.isUsingUniqueConnections());

        return transMeta;
    }
    
    private void verifySlavePartitioningConfiguration(TransMeta slave, StepMeta stepMeta, ClusterSchema clusterSchema, SlaveServer slaveServer)
    {
        Map<StepMeta,String> stepPartitionFlag = slaveStepPartitionFlag.get(slave); 
        if (stepPartitionFlag==null)
        {
            stepPartitionFlag = new Hashtable<StepMeta,String>();
            slaveStepPartitionFlag.put(slave, stepPartitionFlag);
        }
        if (stepPartitionFlag.get(stepMeta)!=null) return; // already done;
        
        StepPartitioningMeta partitioningMeta = stepMeta.getStepPartitioningMeta();
        if (partitioningMeta!=null && partitioningMeta.getMethod()!=StepPartitioningMeta.PARTITIONING_METHOD_NONE && partitioningMeta.getPartitionSchema()!=null)
        {
            // Find the schemaPartitions map to use
            Map<PartitionSchema,List<String>> schemaPartitionsMap = slaveServerPartitionsMap.get(slaveServer);
            if (schemaPartitionsMap!=null)
            {
                PartitionSchema partitionSchema = partitioningMeta.getPartitionSchema();
                List<String> partitionsList = schemaPartitionsMap.get(partitionSchema);
                if (partitionsList!=null) 
                {
                    // We found a list of partitions, now let's create a new partition schema with this data.
                    String partIds[] =  partitionsList.toArray(new String[partitionsList.size()]);
                    String targetSchemaName = partitionSchema.getName()+" (slave)";
                    PartitionSchema targetSchema = slave.findPartitionSchema(targetSchemaName);
                    if (targetSchema==null)
                    {
                        targetSchema = new PartitionSchema(targetSchemaName, partIds);
                        slave.getPartitionSchemas().add(targetSchema); // add it to the slave if it doesn't exist.
                    }
                }
            }
        }
        
        stepPartitionFlag.put(stepMeta, "Y"); // is done.
    }

    /**
     * @return the master
     */
    public TransMeta getMaster()
    {
        return master;
    }

    /**
     * @return the slaveTransMap : the mapping between a slaveServer and the transformation
     *
     */
    public Map<SlaveServer, TransMeta> getSlaveTransMap()
    {
        return slaveTransMap;
    }

    
    public TransMeta[] getSlaves()
    {
        Collection<TransMeta> collection = slaveTransMap.values();
        return collection.toArray(new TransMeta[collection.size()]);
    }
    
    public SlaveServer[] getSlaveTargets()
    {
        Set<SlaveServer> set = slaveTransMap.keySet();
        return set.toArray(new SlaveServer[set.size()]);
        /*
        SlaveServer slaves[] = new SlaveServer[set.size()];
        int i=0;
        for (Iterator iter = set.iterator(); iter.hasNext(); i++)
        {
            ClusterSchemaSlaveServer key = (ClusterSchemaSlaveServer) iter.next();
            slaves[i] = key.getSlaveServer();
        }
        return slaves;
        */
    }
    
    public SlaveServer getMasterServer() throws KettleException
    {
        StepMeta[] steps = originalTransformation.getStepsArray();
        for (int i=0;i<steps.length;i++)
        {
            ClusterSchema clusterSchema = steps[i].getClusterSchema(); 
            if (clusterSchema!=null)
            {
                return clusterSchema.findMaster();
            }
        }
        throw new KettleException("No master server could be found in the original transformation");
    }
    
    
    public void splitOriginalTransformation() throws KettleException
    {
        // Mixing clusters is not supported at the moment
        // Perform some basic checks on the cluster configuration.
        // 
        findUsedOriginalSteps();
        checkClusterConfiguration(); 
        generateSlaveDatabasePartitions();
        
        try
        {
            master = getOriginalCopy(false, null, null);
            
            for (int i=0;i<originalSteps.length;i++)
            {
                StepMeta originalStep = originalSteps[i];
                ClusterSchema originalClusterSchema = originalStep.getClusterSchema(); 

                int nrPreviousSteps = originalTransformation.findNrPrevSteps(originalStep);
                for (int p=0;p<nrPreviousSteps;p++)
                {
                    StepMeta previousStep = originalTransformation.findPrevStep(originalStep, p);

                    ClusterSchema previousClusterSchema = previousStep.getClusterSchema();
                    if (originalClusterSchema==null)
                    {
                        if (previousClusterSchema==null)
                        {
                            // No clustering involved here: just add the original step to the master
                            //
                            StepMeta target = master.findStep(originalStep.getName());
                            if (target==null)
                            {
                                target = (StepMeta) originalStep.clone();
                                master.addStep(target);
                            }
                            
                            StepMeta source = master.findStep(previousStep.getName());
                            if (source==null)
                            {
                                source = (StepMeta) previousStep.clone();
                                master.addStep(source);
                            }
                            
                            // Add a hop too...
                            TransHopMeta masterHop = new TransHopMeta(source, target);
                            master.addTransHop(masterHop);
                        }
                        else
                        {
                            // original step is NOT clustered
                            // Previous step is clustered
                            // --> We read from the slave server using socket readers.
                            //     We need a reader for each slave server in the cluster
                            //
                            
                            // Also add the original step
                            //
                            StepMeta target = master.findStep(originalStep.getName());
                            if (target==null)
                            {
                                target = (StepMeta) originalStep.clone();
                                target.setLocation(target.getLocation().x+(SPLIT/2), target.getLocation().y);
                                master.addStep(target);
                            }

                            // Then add the readers
                            int nrSlaves = previousClusterSchema.getSlaveServers().size();
                            for (int s=0;s<nrSlaves;s++)
                            {
                                SlaveServer slaveServer = (SlaveServer) previousClusterSchema.getSlaveServers().get(s);
                                
                                if (!slaveServer.isMaster())
                                {
                                    // MASTER
                                    SocketReaderMeta socketReaderMeta = new SocketReaderMeta();
                                    socketReaderMeta.setHostname(slaveServer.getHostname());
                                    socketReaderMeta.setPort(""+getPort(previousClusterSchema, slaveServer, originalStep.getName()));
                                    socketReaderMeta.setBufferSize(previousClusterSchema.getSocketsBufferSize());
                                    socketReaderMeta.setCompressed(previousClusterSchema.isSocketsCompressed());

                                    StepMeta readerStep = new StepMeta(getReaderName(originalStep.getName(), previousClusterSchema, slaveServer), socketReaderMeta);
                                    readerStep.setLocation(originalStep.getLocation().x-(SPLIT/2), originalStep.getLocation().y + (s*FANOUT*2)-(nrSlaves*FANOUT/2));
                                    readerStep.setDraw(originalStep.isDrawn());
                                    master.addStep(readerStep);
                                    
                                    TransHopMeta masterHop = new TransHopMeta(readerStep, target);
                                    master.addTransHop(masterHop);
                                    
                                    // SLAVE
                                    TransMeta slave = getSlaveTransformation(previousClusterSchema, slaveServer);
                                    
                                    SocketWriterMeta socketWriterMeta = new SocketWriterMeta();
                                    socketWriterMeta.setPort(""+getPort(previousClusterSchema, slaveServer, originalStep.getName()));
                                    socketWriterMeta.setBufferSize(previousClusterSchema.getSocketsBufferSize());
                                    socketWriterMeta.setFlushInterval(previousClusterSchema.getSocketsFlushInterval());
                                    socketWriterMeta.setCompressed(previousClusterSchema.isSocketsCompressed());
                                    
                                    StepMeta writerStep = new StepMeta(getWriterName(originalStep.getName(), previousClusterSchema, slaveServer), socketWriterMeta);
                                    writerStep.setLocation(originalStep.getLocation().x, originalStep.getLocation().y);
                                    writerStep.setDraw(originalStep.isDrawn());
                                    slave.addStep(writerStep);
                                    
                                    // See if we can add a hop to the previous
                                    StepMeta previous = slave.findStep(previousStep.getName());
                                    if (previous==null)
                                    {
                                        previous = (StepMeta) previousStep.clone();
                                        previous.setLocation(previousStep.getLocation().x+(SPLIT/2), previousStep.getLocation().y);
    
                                        slave.addStep(previous);
                                    }
                                    TransHopMeta slaveHop = new TransHopMeta(previous, writerStep);
                                    slave.addTransHop(slaveHop);
                                    
                                    // Verify the database partitioning for this step.
                                    // verifySlavePartitioningConfiguration(target, previousClusterSchema, slaveServer);
                                }
                            }
                        }
                    }
                    else
                    {
                        if (previousClusterSchema==null)
                        {
                            // originalStep is clustered
                            // previousStep is not clustered
                            // --> Add a socket writer for each slave server
                            //
                            int nrSlaves = originalClusterSchema.getSlaveServers().size();
                            for (int s=0;s<nrSlaves;s++)
                            {
                                SlaveServer slaveServer = (SlaveServer) originalClusterSchema.getSlaveServers().get(s);

                                if (!slaveServer.isMaster())
                                {
                                    // MASTER
                                    SocketWriterMeta socketWriterMeta = new SocketWriterMeta();
                                    socketWriterMeta.setPort(""+getPort(originalClusterSchema, slaveServer, originalStep.getName()));
                                    socketWriterMeta.setBufferSize(originalClusterSchema.getSocketsBufferSize());
                                    socketWriterMeta.setFlushInterval(originalClusterSchema.getSocketsFlushInterval());
                                    socketWriterMeta.setCompressed(originalClusterSchema.isSocketsCompressed());

                                    StepMeta writerStep = new StepMeta(getWriterName( originalStep.getName(), originalClusterSchema, slaveServer ), socketWriterMeta);
                                    writerStep.setLocation(originalStep.getLocation().x, originalStep.getLocation().y + (s*FANOUT*2)-(nrSlaves*FANOUT/2));
                                    writerStep.setDraw(originalStep.isDrawn());
            
                                    master.addStep(writerStep);
                                    
                                    // The previous step: add a hop to it.
                                    // It still has the original name as it is not clustered.
                                    // 
                                    StepMeta previous = master.findStep(previousStep.getName());
                                    if (previous==null)
                                    {
                                        previous = (StepMeta) previousStep.clone();
                                        master.addStep(previous); 
                                    }
                                    TransHopMeta masterHop = new TransHopMeta(previous, writerStep);
                                    master.addTransHop(masterHop);
                                    
                                    // SLAVE
                                    TransMeta slave = getSlaveTransformation(originalClusterSchema, slaveServer);
                                    
                                    SocketReaderMeta socketReaderMeta = new SocketReaderMeta();
                                    SlaveServer masterServer = originalClusterSchema.findMaster();
                                    if (masterServer==null)
                                    {
                                        throw new KettleException("No master server set for cluster schema ["+originalClusterSchema.getName()+"]");
                                    }
                                    socketReaderMeta.setHostname(masterServer.getHostname());
                                    socketReaderMeta.setPort(""+getPort(originalClusterSchema, slaveServer, originalStep.getName()));
                                    socketReaderMeta.setBufferSize(originalClusterSchema.getSocketsBufferSize());
                                    socketReaderMeta.setCompressed(originalClusterSchema.isSocketsCompressed());

                                    StepMeta readerStep = new StepMeta(getReaderName(originalStep.getName(), originalClusterSchema, slaveServer ), socketReaderMeta);
                                    readerStep.setLocation(originalStep.getLocation().x-(SPLIT/2), originalStep.getLocation().y);
                                    readerStep.setDraw(originalStep.isDrawn());
                                    slave.addStep(readerStep);
                                    
                                    // also add the step itself.
                                    StepMeta slaveStep = slave.findStep(originalStep.getName());
                                    if (slaveStep==null)
                                    {
                                        slaveStep = (StepMeta) originalStep.clone();
                                        slaveStep.setLocation(originalStep.getLocation().x+(SPLIT/2), originalStep.getLocation().y);
                                        slave.addStep(slaveStep);
                                    }
                                    
                                    // And a hop from the 
                                    TransHopMeta slaveHop = new TransHopMeta(readerStep, slaveStep);
                                    slave.addTransHop(slaveHop);
                                    
                                    // Verify the database partitioning for this slave step.
                                    // verifySlavePartitioningConfiguration(slaveStep, originalClusterSchema, slaveServer);
                                }
                            }
                        }
                        else
                        {
                            // originalStep is clustered
                            // previousStep is clustered
                            // --> Add original step to the slave transformation(s)
                            //
                            int nrSlaves = originalClusterSchema.getSlaveServers().size();
                            for (int s=0;s<nrSlaves;s++)
                            {
                                SlaveServer slaveServer = (SlaveServer) originalClusterSchema.getSlaveServers().get(s);
                                
                                if (!slaveServer.isMaster())
                                {
                                    // SLAVE
                                    TransMeta slave = getSlaveTransformation(originalClusterSchema, slaveServer);
                                    StepMeta target = slave.findStep(originalStep.getName());
                                    if (target==null)
                                    {
                                        target = (StepMeta) originalStep.clone();
                                        slave.addStep(target);
                                    }
                                    
                                    StepMeta source = slave.findStep(previousStep.getName());
                                    if (source==null)
                                    {
                                        source = (StepMeta) previousStep.clone();
                                        slave.addStep(source);
                                    }
                                    
                                    TransHopMeta slaveHop = new TransHopMeta(source, target);
                                    slave.addTransHop(slaveHop);
                                    
                                    // Verify the database partitioning 
                                    verifyStepPartitioning(slave, target, originalClusterSchema, slaveServer);
                                }
                            }
                        }
                    }
                }
                
                if (nrPreviousSteps==0)
                {
                    if (originalClusterSchema==null)
                    {
                        // Not clustered, simply add the step.
                        if (master.findStep(originalStep.getName())==null) master.addStep(originalStep);
                    }
                    else
                    {
                        int nrSlaves = originalClusterSchema.getSlaveServers().size();
                        for (int s=0;s<nrSlaves;s++)
                        {
                            SlaveServer slaveServer = (SlaveServer) originalClusterSchema.getSlaveServers().get(s);

                            if (!slaveServer.isMaster())
                            {
                                // SLAVE
                                TransMeta slave = getSlaveTransformation(originalClusterSchema, slaveServer);
                                slave.addStep(originalStep);
                            }
                        }
                    }
                }
            }
             
            for (int i=0;i<originalSteps.length;i++)
            {
                StepMeta originalStep = originalSteps[i];
                ClusterSchema originalClusterSchema = originalStep.getClusterSchema(); 

                // Also take care of the info steps...
                // For example: StreamLookup, Table Input, etc.
                //
                StepMeta infoSteps[] = originalTransformation.getInfoStep(originalStep);
                for (int p=0;infoSteps!=null && p<infoSteps.length;p++)
                {
                    StepMeta infoStep = infoSteps[p];
                    
                    ClusterSchema infoClusterSchema = infoStep.getClusterSchema();
                    if (originalClusterSchema==null)
                    {
                        if (infoClusterSchema==null)
                        {
                            // No clustering involved here: just add a link between the original step and the infostep
                            //
                            StepMeta target = master.findStep(originalStep.getName());
                            StepMeta source = master.findStep(infoStep.getName());
                            
                            // Add a hop too...
                            TransHopMeta masterHop = new TransHopMeta(source, target);
                            master.addTransHop(masterHop);
                        }
                        else
                        {
                            /*
                            // original step is NOT clustered
                            // Previous step is clustered
                            // --> We read from the slave server using socket readers.
                            //     We need a reader for each slave server in the cluster
                            //
                             */
                        }
                    }
                    else
                    {
                        if (infoClusterSchema==null)
                        {
                            // originalStep is clustered
                            // infoStep is not clustered
                            // --> Add a socket writer for each slave server
                            //
                            int nrSlaves = originalClusterSchema.getSlaveServers().size();
                            for (int s=0;s<nrSlaves;s++)
                            {
                                SlaveServer slaveServer = (SlaveServer) originalClusterSchema.getSlaveServers().get(s);
                                
                                if (!slaveServer.isMaster())
                                {
                                    // MASTER
                                    SocketWriterMeta socketWriterMeta = new SocketWriterMeta();
                                    socketWriterMeta.setPort(""+getPort(originalClusterSchema, slaveServer, originalStep.getName()));
                                    socketWriterMeta.setBufferSize(originalClusterSchema.getSocketsBufferSize());
                                    socketWriterMeta.setFlushInterval(originalClusterSchema.getSocketsFlushInterval());
                                    socketWriterMeta.setCompressed(originalClusterSchema.isSocketsCompressed());

                                    StepMeta writerStep = new StepMeta(getWriterName( originalStep.getName(), originalClusterSchema, slaveServer ), socketWriterMeta);
                                    writerStep.setLocation(originalStep.getLocation().x, originalStep.getLocation().y + (s*FANOUT*2)-(nrSlaves*FANOUT/2));
                                    writerStep.setDraw(originalStep.isDrawn());
            
                                    master.addStep(writerStep);
                                    
                                    // The previous step: add a hop to it.
                                    // It still has the original name as it is not clustered.
                                    // 
                                    StepMeta previous = master.findStep(infoStep.getName());
                                    if (previous==null)
                                    {
                                        previous = (StepMeta) infoStep.clone();
                                        master.addStep(previous); 
                                    }
                                    TransHopMeta masterHop = new TransHopMeta(previous, writerStep);
                                    master.addTransHop(masterHop);
                                    
                                    // SLAVE
                                    TransMeta slave = getSlaveTransformation(originalClusterSchema, slaveServer);
                                    
                                    SocketReaderMeta socketReaderMeta = new SocketReaderMeta();
                                    SlaveServer masterServer = originalClusterSchema.findMaster();
                                    if (masterServer==null)
                                    {
                                        throw new KettleException("No master server set for cluster schema ["+originalClusterSchema.getName()+"]");
                                    }
                                    socketReaderMeta.setHostname(masterServer.getHostname());
                                    socketReaderMeta.setPort(""+getPort(originalClusterSchema, slaveServer, originalStep.getName()));
                                    socketReaderMeta.setBufferSize(originalClusterSchema.getSocketsBufferSize());
                                    socketReaderMeta.setCompressed(originalClusterSchema.isSocketsCompressed());
                                    
                                    StepMeta readerStep = new StepMeta(getReaderName(originalStep.getName(), originalClusterSchema, slaveServer ), socketReaderMeta);
                                    readerStep.setLocation(originalStep.getLocation().x-(SPLIT/2), originalStep.getLocation().y);
                                    readerStep.setDraw(originalStep.isDrawn());
                                    slave.addStep(readerStep);
                                    
                                    // also add the step itself.
                                    StepMeta slaveStep = slave.findStep(originalStep.getName());
                                    if (slaveStep==null)
                                    {
                                        slaveStep = (StepMeta) originalStep.clone();
                                        slaveStep.setLocation(originalStep.getLocation().x+(SPLIT/2), originalStep.getLocation().y);
                                        slave.addStep(slaveStep);
                                    }
                                    
                                    // And a hop from the 
                                    TransHopMeta slaveHop = new TransHopMeta(readerStep, slaveStep);
                                    slave.addTransHop(slaveHop);
                                    
                                    // 
                                    // Now we have to explain to the slaveStep that it has to source from previous
                                    // 
                                    String infoStepNames[] = slaveStep.getStepMetaInterface().getInfoSteps();
                                    if (infoStepNames!=null)
                                    {
                                        StepMeta is[] = new StepMeta[infoStepNames.length];
                                        for (int n=0;n<infoStepNames.length;n++)
                                        {
                                            is[n] = slave.findStep(infoStepNames[n]); // OK, info steps moved to the slave steps
                                            if (infoStepNames[n].equals(infoStep.getName()))  
                                            {
                                                // We want to replace this one with the reader step: that's where we source from now
                                                infoSteps[n] = readerStep;
                                            }
                                        }
                                        slaveStep.getStepMetaInterface().setInfoSteps(infoSteps);
                                    }
                                }
                            }
                        }
                        else
                        {
                            /*
                            // originalStep is clustered
                            // previousStep is clustered
                            // --> Add original step to the slave transformation(s)
                            //
                             */
                            // 
                            // Now we have to explain to the slaveStep that it has to source from previous
                            //
                            int nrSlaves = originalClusterSchema.getSlaveServers().size();
                            for (int s=0;s<nrSlaves;s++)
                            {
                                SlaveServer slaveServer = (SlaveServer) originalClusterSchema.getSlaveServers().get(s);
                                if (!slaveServer.isMaster())
                                {
                                    TransMeta slave = getSlaveTransformation(originalClusterSchema, slaveServer);
                                    StepMeta slaveStep = slave.findStep(originalStep.getName());
                                    String infoStepNames[] = slaveStep.getStepMetaInterface().getInfoSteps();
                                    if (infoStepNames!=null)
                                    {
                                        StepMeta is[] = new StepMeta[infoStepNames.length];
                                        for (int n=0;n<infoStepNames.length;n++)
                                        {
                                            is[n] = slave.findStep(infoStepNames[n]); // OK, info steps moved to the slave steps
                                            
                                            // Hang on... is there a hop to the previous step?
                                            if (slave.findTransHop(is[n], slaveStep)==null)
                                            {
                                                TransHopMeta infoHop = new TransHopMeta(is[n], slaveStep);
                                                slave.addTransHop(infoHop);
                                            }
                                        }
                                        slaveStep.getStepMetaInterface().setInfoSteps(infoSteps);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw new KettleException("Unexpected problem while generating master transformation", e);
        }
    }

    private void verifyStepPartitioning(TransMeta slave, StepMeta stepMeta, ClusterSchema clusterSchema, SlaveServer slaveServer) throws KettleException
    {
        StepPartitioningMeta partitioningMeta = stepMeta.getStepPartitioningMeta();
        if (partitioningMeta!=null && partitioningMeta.getMethod()!=StepPartitioningMeta.PARTITIONING_METHOD_NONE)
        {
            // Point this partitioning method to the target schema called schemaName + " (slave)"
            //
            String schemaName = partitioningMeta.getPartitionSchema().getName()+" (slave)";
            PartitionSchema targetSchema = slave.findPartitionSchema(schemaName);
            
            if (targetSchema==null)
            {
                throw new KettleException("Internal error: unable to find required Partitioning schema ["+schemaName+"]");
            }
            
            StepPartitioningMeta targetPartitioningMeta = new StepPartitioningMeta(
                    partitioningMeta.getMethod(), 
                    partitioningMeta.getFieldName(), 
                    targetSchema);
            
            stepMeta.setStepPartitioningMeta(targetPartitioningMeta);
        }

        
    }

    private void findUsedOriginalSteps()
    {
        List<StepMeta> transHopSteps = originalTransformation.getTransHopSteps(false);
        originalSteps = transHopSteps.toArray(new StepMeta[transHopSteps.size()]);
    }
    
    /**
     * We want to devide the available database partitions over the slaves.
     * Let's create a hashtable that contains the partition schema's
     * Since we can only use a single cluster, we can divide them all over a single set of slave servers. 
     * 
     * @throws KettleException
     */
    private void generateSlaveDatabasePartitions() throws KettleException
    {
        slaveServerPartitionsMap = new Hashtable<SlaveServer,Map<PartitionSchema,List<String>>>();
        
        for (int i=0;i<originalSteps.length;i++)
        {
            StepMeta stepMeta = originalSteps[i];
            StepPartitioningMeta stepPartitioningMeta = stepMeta.getStepPartitioningMeta();
            
            if (stepPartitioningMeta==null) continue;
            if (stepPartitioningMeta.getMethod()==StepPartitioningMeta.PARTITIONING_METHOD_NONE) continue;
            
            ClusterSchema clusterSchema = stepMeta.getClusterSchema();
            if (clusterSchema==null) continue;
            
            PartitionSchema partitionSchema = stepPartitioningMeta.getPartitionSchema();
            int nrPartitions = partitionSchema.getPartitionIDs().length;
            int nrSlaves = clusterSchema.findNrSlaves();
            
            if (nrSlaves==0) continue; // no slaves: ignore this situation too
            
            if (nrPartitions<nrSlaves)
            {
                throw new KettleException("It doesn't make sense to have a database partitioned, clustered step with less partitions ("+nrPartitions+") than that there are slave servers ("+nrSlaves+")");
            }

            int s=0;
            for (int p=0;p<nrPartitions;p++)
            {
                String partitionId = partitionSchema.getPartitionIDs()[p];
                
                SlaveServer slaveServer = clusterSchema.getSlaveServers().get(s);
                if (slaveServer.isMaster())
                {
                    s++;
                    if (s>=clusterSchema.getSlaveServers().size()) s=0; // re-start
                    slaveServer = (SlaveServer) clusterSchema.getSlaveServers().get(s);
                }

                // System.out.println("Step ["+stepMeta.getName()+"] : selected slave server ["+slaveServer+"]");

                Map<PartitionSchema,List<String>> schemaPartitionsMap =  slaveServerPartitionsMap.get(slaveServer);
                if (schemaPartitionsMap==null)
                {
                    // Add this map
                    schemaPartitionsMap = new HashMap<PartitionSchema,List<String>>();
                    slaveServerPartitionsMap.put(slaveServer, schemaPartitionsMap);
                    // System.out.println("Added new schemaPartitions map for slave server ["+slaveServer+"]");
                }
                
                // See if we find a list of partitions
                List<String> partitions = schemaPartitionsMap.get(partitionSchema);
                if (partitions==null)
                {
                    partitions = new ArrayList<String>();
                    schemaPartitionsMap.put(partitionSchema, partitions);
                }
                
                // Add the partitionId to the appropriate list
                if (partitions.indexOf(partitionId)<0)
                {
                    partitions.add(partitionId);
                    // System.out.println("Added partition ["+partitionId+"] to slave server ["+slaveServer+"] for db part schema ["+partitionSchema+"]");
                }

                // Switch to next slave.
                s++;
                if (s>=clusterSchema.getSlaveServers().size()) s=0; // re-start
            }
        }
        // System.out.println("We have "+(slaveServerPartitionsMap.size())+" entries in the slave server partitions map");
    }
}
