package be.ibridge.kettle.www;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Appender;

import be.ibridge.kettle.trans.Trans;
import be.ibridge.kettle.trans.TransConfiguration;

/**
 * This is a map between the transformation name and the (running/waiting/finished) transformation.
 * 
 * @author Matt
 *
 */
public class TransformationMap
{
    private Map transformationMap;
    private Map configurationMap;
    private Map loggingMap;
    
    private String parentThreadName;
    
    public TransformationMap(String parentThreadName)
    {
        this.parentThreadName = parentThreadName;
        
        transformationMap = new Hashtable();
        configurationMap  = new Hashtable();
        loggingMap        = new Hashtable();
    }
    
    public synchronized void addTransformation(String transformationName, Trans trans, TransConfiguration transConfiguration)
    {
        transformationMap.put(transformationName, trans);
        configurationMap.put(transformationName, transConfiguration);
    }
    
    public synchronized Trans getTransformation(String transformationName)
    {
        return (Trans)transformationMap.get(transformationName);
    }
    
    public synchronized TransConfiguration getConfiguration(String transformationName)
    {
        return (TransConfiguration)configurationMap.get(transformationName);
    }

    public synchronized void removeTransformation(String transformationName)
    {
        transformationMap.remove(transformationName);
        configurationMap.remove(transformationName);
    }
    
    public synchronized Appender getAppender(String transformationName)
    {
        return (Appender)loggingMap.get(transformationName);
    }
    
    public synchronized void addAppender(String transformationName, Appender appender)
    {
        loggingMap.put(transformationName, appender);
    }

    public synchronized void removeAppender(String transformationName)
    {
        loggingMap.remove(transformationName);
    }
    
    public String[] getTransformationNames()
    {
        Set keySet = transformationMap.keySet();
        return (String[]) keySet.toArray(new String[keySet.size()]);
    }

    /**
     * @return the parentThreadName
     */
    public String getParentThreadName()
    {
        return parentThreadName;
    }

    /**
     * @return the configurationMap
     */
    public Map getConfigurationMap()
    {
        return configurationMap;
    }

    /**
     * @param configurationMap the configurationMap to set
     */
    public void setConfigurationMap(Map configurationMap)
    {
        this.configurationMap = configurationMap;
    }
}
