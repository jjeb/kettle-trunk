 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
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


package org.pentaho.di.core.variables;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.pentaho.di.core.util.StringUtil;


/**
 * This class is an implementation of VariableSpace
 * 
 * @author Sven Boden
 */
public class Variables implements VariableSpace 
{
    private Map<String, String> properties;
    private VariableSpace parent;
    private Map<String, String> injection;
    private boolean initialized;
    
    public Variables()
    {
        properties  = new Hashtable<String,String>();
        parent      = null;
        injection   = null;
        initialized = false;
    }

	public void copyVariablesFrom(VariableSpace space) {
		if ( space != null && this != space )
		{
			// If space is not null and this variable is not already
			// the same object as the argument.
			String[] variableNames = space.listVariables();
			for ( int idx = 0; idx < variableNames.length; idx++ )
			{
				properties.put(variableNames[idx], space.getVariable(variableNames[idx]));
			}		
		}
	}

	public VariableSpace getParentVariableSpace() {
		return parent;
	}

	public String getVariable(String variableName, String defaultValue) {
        String var = properties.get(variableName);
        if (var==null) return defaultValue;
        return var;
	}

	public String getVariable(String variableName) {
		return properties.get(variableName);
	}

	public void initializeVariablesFrom(VariableSpace parent) {
	   this.parent = parent;
	   
	   // Add all the system properties...
	   for (Object key : System.getProperties().keySet()) {
		   properties.put((String)key, System.getProperties().getProperty((String)key));
	   }
	   
	   if ( parent != null )
	   {
           copyVariablesFrom(parent);
	   }
	   if ( injection != null )
	   {
	       properties.putAll(injection);
	       injection = null;
	   }
	   initialized = true;
	}

	public String[] listVariables() {
		List<String> list = new ArrayList<String>();
		for ( String name : properties.keySet() )
		{
			list.add(name);
		}
		return (String[])list.toArray(new String[list.size()]);
	}

	public void setVariable(String variableName, String variableValue) 
	{
        if (variableValue!=null)
        {
            properties.put(variableName, variableValue);
        }
        else
        {
            properties.remove(variableName);
        }
    }
	
	public String environmentSubstitute(String aString)
	{
        if (aString==null || aString.length()==0) return aString;
        
        return StringUtil.environmentSubstitute(aString, properties);
	}
	
	public String[] environmentSubstitute(String string[])
	{
		String retval[] = new String[string.length];
		for (int i = 0; i < string.length; i++)
		{
			retval[i] = environmentSubstitute(string[i]);
		}
		return retval;
	}	

	public void shareVariablesWith(VariableSpace space) {
	    // not implemented in here... done by pointing to the same VariableSpace
		// implementation
	}

	public void injectVariables(Map<String, String> prop) {
		if ( initialized )
		{
		    // variables are already initialized
       	    if ( prop != null )
			{
			     properties.putAll(prop);
			     injection = null;			     
			}
		}
		else
		{
			// We have our own personal copy, so changes afterwards
			// to the input properties don't affect us.
			injection = new Hashtable<String, String>();
			injection.putAll(prop);
		}	
	}
	
	/**
	 * Get a default variable space as a placeholder. Everytime you 
	 * will get a new instance.
	 * 
	 * @return a default variable space.
	 */
	synchronized public static VariableSpace getADefaultVariableSpace()
	{
	    VariableSpace space = new Variables();
	    
	    space.initializeVariablesFrom(null);
	    
	    return space;
	}	
}