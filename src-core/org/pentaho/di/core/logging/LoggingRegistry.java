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

package org.pentaho.di.core.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.util.EnvUtil;

/**
 * This singleton class contains the logging registry.
 * It's a hash-map containing the hierarchy for a certain UUID that is generated when a job, job-entry, transformation or step is being created.
 * With it, you can see for each log record to which step-mapping-xform-entry-job-job hiearchy it belongs.
 * 
 * @author matt
 *
 */
public class LoggingRegistry {
	
	private static LoggingRegistry registry;
	
	/**
	 * The map that links the log channel ID to the logging objects themselves
	 */
	private Map<String, LoggingObjectInterface> map;
	
	/**
   * The map that contains the direct log channel children for a log channel
   */
  private Map<String, List<String>> childrenMap;
  
	private Date lastModificationTime;
	
	private int maxSize;
	private final int DEFAULT_MAX_SIZE = 10000;
	
	private LoggingRegistry() {
		map = new ConcurrentHashMap<String, LoggingObjectInterface>();	
    childrenMap = new ConcurrentHashMap<String, List<String>>();
    
		lastModificationTime = new Date();
		maxSize = Const.toInt(EnvUtil.getSystemProperty(Const.KETTLE_MAX_LOGGING_REGISTRY_SIZE), DEFAULT_MAX_SIZE);
	}
	
	public static LoggingRegistry getInstance() {
		if (registry!=null) {
			return registry;
		}
		registry = new LoggingRegistry();
		return registry;
	}
	
	/**
	 * This methods registers a new logging source, stores it in the registry.
	 * 
	 * @param object The logging source
	 * @param int The logging level for this logging source
	 * @return a new ID (UUID)
	 */
	public String registerLoggingSource(Object object) {
		
		// recalc max size: see PDI-7270
		maxSize = Const.toInt(EnvUtil.getSystemProperty(Const.KETTLE_MAX_LOGGING_REGISTRY_SIZE), DEFAULT_MAX_SIZE);

		// Extract the core logging information from the object itself, including the hierarchy.
		//
		LoggingObject loggingSource = new LoggingObject(object);
		
		// First do a sanity check to see if the object is not already present in the registry
		// This will prevent excessive memory leakage in the registry map too... 
		//
		LoggingObjectInterface found = findExistingLoggingSource(loggingSource);
		if (found!=null && found.getParent()!=null) {
		  // Return the previous log channel ID
		  //
			return found.getLogChannelId();
		}

		// Nothing was found, generate a new ID and store it in the registry...
		//
		String logChannelId = UUID.randomUUID().toString();
		loggingSource.setLogChannelId(logChannelId);

		map.put(logChannelId, loggingSource);
		
		
		// See if we need to update the parent's list of children
		//
		if (loggingSource.getParent()!=null) {
		  String parentLogChannelId = loggingSource.getParent().getLogChannelId();
		  List<String> parentChildren = childrenMap.get(parentLogChannelId);
		  if (parentChildren==null) {
		    parentChildren = new ArrayList<String>();
		    childrenMap.put(parentLogChannelId, parentChildren);
		  }
		  parentChildren.add(logChannelId);		  
		}
		
		lastModificationTime = new Date();
		loggingSource.setRegistrationDate(lastModificationTime);
		
		// Validate that we're not leaking references.  If the size of the map becomes too large we opt to remove the oldest...
		//
		if (maxSize>0 && map.size()>maxSize+1000) {
		  
      long cleanStart = System.currentTimeMillis();
		  
		  // Remove 250 and trim it back to maxSize
		  //
		  List<LoggingObjectInterface> all = new ArrayList<LoggingObjectInterface>(map.values());
		  Collections.sort(all, new Comparator<LoggingObjectInterface>() {
		    @Override
		    public int compare(LoggingObjectInterface o1, LoggingObjectInterface o2) {
		      if (o1==null && o2!=null) return -1;
          if (o1!=null && o2==null) return 1;
          if (o1==null && o2==null) return 0;
		      return o1.getRegistrationDate().compareTo(o2.getRegistrationDate());
		    }
      });
		  
		  // Remove 1000 entries...
		  //
		  for (int i=0;i<1000;i++) {
		    LoggingObjectInterface toRemove = all.get(i);
		    map.remove(toRemove.getLogChannelId());

		    // Remove the children map as well, won't be found anyway.
		    //
		    childrenMap.remove(toRemove.getLogChannelId());
		  }

      long cleanEnd = System.currentTimeMillis();
      LogChannel.GENERAL.snap(Metrics.METRIC_LOGGING_REGISTRY_CLEAN_TIME, cleanEnd-cleanStart);
      LogChannel.GENERAL.snap(Metrics.METRIC_LOGGING_REGISTRY_CLEAN_COUNT);
      // System.out.println("------->>>>> Cleaned out old logging registry entries: "+(cleanEnd-cleanStart)+"ms");
		}
		
		return logChannelId;
	}

  /**
	 * See if the registry already contains the specified logging object.  If so, return the one in the registry.
	 * You can use this to verify existence prior to assigning a new channel ID.
	 * @param loggingObject The logging object to verify
	 * @return the existing object or null if none is present.
	 */
	public LoggingObjectInterface findExistingLoggingSource(LoggingObjectInterface loggingObject) {
		LoggingObjectInterface found = null;
		for (LoggingObjectInterface verify : map.values()) {

			if (loggingObject.equals(verify)) {
				found = verify;
				break;
			}
		}
		return found;
	}

	/**
	 * Get the logging source object for a certain logging id
	 * @param logChannelId the logging channel id to look for
	 * @return the logging source of null if nothing was found
	 */
	public LoggingObjectInterface getLoggingObject(String logChannelId) {
		return map.get(logChannelId);
	}
	
	public Map<String, LoggingObjectInterface> getMap() {
		return map;
	}

	/**
	 * In a situation where you have a job or transformation, you want to get a list of ALL the children where the parent is the channel ID.
	 * The parent log channel ID is added
	 * 
	 * @param parentLogChannelId The parent log channel ID
	 * @return the list of child channel ID
	 */
	public List<String> getLogChannelChildren(String parentLogChannelId) {
		if (parentLogChannelId==null) {
			return null;
		}
		List<String> list = getLogChannelChildren(new ArrayList<String>(), parentLogChannelId);
		list.add(parentLogChannelId);
		return list;
	}

	/**
	 * In a situation where you have a job or transformation, you want to get a list of ALL the children where the parent is the channel ID.
	 * 
	 * @param parentLogChannelId The parent log channel ID
	 * @return the list of child channel ID, not including the parent.
	 */
	private List<String> getLogChannelChildren(List<String> children, String parentLogChannelId) {

	  long getStart = System.currentTimeMillis();

	  synchronized(childrenMap) {
  		List<String> list = childrenMap.get(parentLogChannelId);
  		if (list==null) {
  		  list = new ArrayList<String>();
  		  // This is the only place where we'll add something: at the bottom of the tree.
  		  // This means that we won't have to do duplicate detection anymore.
  		  //
		    children.add(parentLogChannelId);
  		  return list;
  		}
  		
  		Iterator<String> kids = list.iterator();
  		while (kids.hasNext()) {
  		  String logChannelId = kids.next();

  		  // Search deeper into the tree...
  		  //
        getLogChannelChildren(children, logChannelId);
  		}
  		
	  }
		
    long getStop = System.currentTimeMillis();

    LogChannel.GENERAL.snap(Metrics.METRIC_LOGGING_REGISTRY_GET_CHILDREN_TIME, getStop-getStart);
    LogChannel.GENERAL.snap(Metrics.METRIC_LOGGING_REGISTRY_GET_CHILDREN_COUNT);
    
		return children;
	}
	
	public Date getLastModificationTime() {
		return lastModificationTime;
	}

	public String dump(boolean includeGeneral){
		StringBuffer out  = new StringBuffer(50000);
		for (LoggingObjectInterface o : map.values()){
			if (!includeGeneral && o.getObjectType().equals(LoggingObjectType.GENERAL)){
				continue;
			}
			out.append(o.getContainerObjectId());
			out.append("\t");
			out.append(o.getLogChannelId());
			out.append("\t");
			out.append(o.getObjectType().name());
			out.append("\t");
			out.append(o.getObjectName());
			out.append("\t");
			out.append((o.getParent()!=null)?o.getParent().getLogChannelId():"-");
			out.append("\t");
			out.append((o.getParent()!=null)?o.getParent().getObjectType().name():"-");
			out.append("\t");
			out.append((o.getParent()!=null)?o.getParent().getObjectName():"-");
			out.append("\n");
			
		}
		return out.toString();
	}
	
	/**
	 * Removes the logging registry entry and all its children from the registry.
	 * 
	 * @param logChannelId
	 */
  public void removeIncludingChildren(String logChannelId) {
    synchronized(map) {
      List<String> children = getLogChannelChildren(logChannelId);
      for (String child : children) {
        map.remove(child);
      }
      map.remove(logChannelId);
    }
  }
}
