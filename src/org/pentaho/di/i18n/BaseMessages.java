package org.pentaho.di.i18n;

import org.pentaho.di.laf.LAFFactory;
import org.pentaho.di.laf.LAFChangeListener;

/**
 * BaseMessage is called by all Message classes to enable the delegation of
 * message delivery, by key to be delegated to the appropriately authoritative
 * supplier as registered in the LAFFactory enabling both i18n as well as pluggable
 * look and feel (LAF)
 * 
 * @author dhushon
 *
 */
public class BaseMessages implements LAFChangeListener<MessageHandler> {
	static BaseMessages instance = null;
	protected MessageHandler handler = null;
	Class<MessageHandler> clazz = MessageHandler.class;
	
	static {
		getInstance();
	}
	
	private BaseMessages() {
		init();
	}
	
	private void init() {
		//counting on LAFFactory to return a class conforming to @see MessageHandler
		handler = (MessageHandler)LAFFactory.getHandler(clazz);
	}
	
	public static BaseMessages getInstance() {
		if (instance == null) {
			instance = new BaseMessages();
		}
		return instance;
	}
	
	protected MessageHandler getHandler() {
		return handler;
	}
	
	protected static MessageHandler getInstanceHandler() {
		return getInstance().getHandler();
	}
	
	public static String getString(String key) {
		return getInstanceHandler().getString(key);
	}
	
	public static String getString(String packageName, String key)
	{
	    return getInstanceHandler().getString(packageName, key);
	}

	public static String getString(String packageName, String key, String param1)
	{
		return getInstanceHandler().getString(packageName, key, param1);
	}

	public static String getString(String packageName, String key, String param1, String param2)
	{
		return getInstanceHandler().getString(packageName, key, param1, param2);
	}

	public static String getString(String packageName, String key, String param1, String param2, String param3)
	{
		return getInstanceHandler().getString(packageName, key, param1, param2, param3);
	}

	public static String getString(String packageName, String key, String param1, String param2, String param3, String param4)
	{
		return getInstanceHandler().getString(packageName, key, param1, param2, param3, param4);
	}

	public static String getString(String packageName, String key, String param1, String param2, String param3, String param4, String param5)
	{
		return getInstanceHandler().getString(packageName, key, param1, param2, param3, param4, param5);
	}

	public static String getString(String packageName, String key, String param1, String param2, String param3, String param4, String param5, String param6)
	{
		return getInstanceHandler().getString(packageName, key, param1, param2, param3, param4, param5, param6);
	}

	public void notify(MessageHandler changedObject) {
		handler = changedObject;
	}
}
