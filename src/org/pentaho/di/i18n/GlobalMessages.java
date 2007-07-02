package org.pentaho.di.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.trans.StepLoader;


public class GlobalMessages
{
    private static final ThreadLocal<Locale> threadLocales         = new ThreadLocal<Locale>();

    private static final LanguageChoice  langChoice        = LanguageChoice.getInstance();

    private static final String      SYSTEM_BUNDLE_PACKAGE = GlobalMessages.class.getPackage().getName();

    private static final String      BUNDLE_NAME           = "messages.messages";                                  //$NON-NLS-1$

    private static final Map<String,ResourceBundle>         locales               = Collections.synchronizedMap(new HashMap<String,ResourceBundle>());

    public static final String[] localeCodes = { "en_US", "nl_NL", "zh_CN", "es_ES", "fr_FR", "de_DE", "pt_BR" };
    
    public static final String[] localeDescr = { "English (US)", "Nederlands", "Simplified Chinese", "Espa\u00F1ol", "Fran\u00E7ais", "Deutsch", "Portuguese (Brazil)" };
    
    protected static Map<String, ResourceBundle> getLocales()
    {
        return locales;
    }

    public static Locale getLocale()
    {
        Locale rtn = threadLocales.get();
        if (rtn != null) { return rtn; }

        setLocale(langChoice.getDefaultLocale());
        return langChoice.getDefaultLocale();
    }

    public static void setLocale(Locale newLocale)
    {
        threadLocales.set(newLocale);
    }
    
    private static String getLocaleString(Locale locale)
    {
        String locString = locale.toString();
        if (locString.length()==5 && locString.charAt(2)=='_') // Force upper-lowercase format
        {
            locString=locString.substring(0,2).toLowerCase()+"_"+locString.substring(3).toUpperCase();
            // System.out.println("locString="+locString);
        }
        return locString;
    }

    private static String buildHashKey(Locale locale, String packageName)
    {
        return packageName + "_" + getLocaleString(locale);
    }

    private static String buildBundleName(String packageName)
    {
        return packageName + "." + BUNDLE_NAME;
    }

    private static ResourceBundle getBundle(Locale locale, String packageName) throws MissingResourceException
    {
    	String filename = buildHashKey(locale, packageName);
    	filename = "/"+filename.replace('.', '/') + ".properties";
    	
    	try
    	{
    	    ResourceBundle bundle = locales.get(filename);
            if (bundle == null)
            {
                InputStream inputStream = LanguageChoice.getInstance().getClass().getResourceAsStream(filename);
                if (inputStream==null) // Try in the step plugin list: look in the jars over there
                {
                    inputStream = StepLoader.getInstance().getInputStreamForFile(filename);
                }
                
            	if (inputStream!=null)
            	{
            		bundle = new PropertyResourceBundle(inputStream);
            		locales.put(filename, bundle);
            	}
            	else
            	{
            		throw new MissingResourceException("Unable to find properties file ["+filename+"]", locale.toString(), packageName);
            	}
            }
            return bundle;
    	}
    	catch(IOException e)
    	{
    		throw new MissingResourceException("Unable to find properties file ["+filename+"] : "+e.toString(), locale.toString(), packageName);
    	}
    }

    public static String getSystemString(String key)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getDefaultLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }

        /*
        try
        {
            ResourceBundle bundle = getBundle(langChoice.getDefaultLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE));
            return bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            // OK, try to find the key in the alternate failover locale
            try
            {
                ResourceBundle bundle = getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE));
                return bundle.getString(key);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
        */
    }

    public static String getSystemString(String key, String param1)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getDefaultLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
    }

    public static String getSystemString(String key, String param1, String param2)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
    }
    
    public static String getSystemString(String key, String param1, String param2, String param3)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
    }

    public static String getSystemString(String key, String param1, String param2, String param3, String param4)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3, param4);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3, param4);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
    }

    public static String getSystemString(String key, String param1, String param2, String param3, String param4, String param5)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3, param4, param5);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3, param4, param5);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
    }

    public static String getSystemString(String key, String param1, String param2, String param3, String param4, String param5, String param6)
    {
        try
        {
            return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3, param4, param5, param6);
        }
        catch (MissingResourceException e)
        {
            try
            {
                return GlobalMessageUtil.getString(getBundle(langChoice.getFailoverLocale(), buildBundleName(SYSTEM_BUNDLE_PACKAGE)), key, param1, param2, param3, param4, param5, param6);
            }
            catch (MissingResourceException fe)
            {
                LogWriter.getInstance().logError("Internationalisation/Translation error", Const.getStackTracker(e));
                return '!' + key + '!';
            }
        }
    }
    
    private static String findString(String packageName, Locale locale, String key, Object[] parameters) throws MissingResourceException
    {
        try
        {
            ResourceBundle bundle = getBundle(locale, packageName + "." + BUNDLE_NAME);
            String unformattedString = bundle.getString(key);
            String string = MessageFormat.format(unformattedString, parameters);
            return string;
        }
        catch(IllegalArgumentException e)
        {
            String message = "Format problem with key=["+key+"], locale=["+locale+"], package="+packageName+" : "+e.toString();
            LogWriter.getInstance().logError("i18n", message);
            LogWriter.getInstance().logError("i18n", Const.getStackTracker(e));
            throw new MissingResourceException(message, packageName, key);
        }
    }
    
    private static String calculateString(String packageName, String key, Object[] parameters)
    {
        String string=null;
        
        // First try the standard locale, in the local package
        try { string = findString(packageName, langChoice.getDefaultLocale(), key, parameters); } catch(MissingResourceException e) {};
        if (string!=null) return string;
        
        // Then try to find it in the i18n package, in the system messages of the preferred language.
        try { string = findString(SYSTEM_BUNDLE_PACKAGE, langChoice.getDefaultLocale(), key, parameters); } catch(MissingResourceException e) {};
        if (string!=null) return string;
        
        // Then try the failover locale, in the local package
        try { string = findString(packageName, langChoice.getFailoverLocale(), key, parameters); } catch(MissingResourceException e) {};
        if (string!=null) return string;
        
        // Then try to find it in the i18n package, in the system messages of the failover language.
        try { string = findString(SYSTEM_BUNDLE_PACKAGE, langChoice.getFailoverLocale(), key, parameters); } catch(MissingResourceException e) {};
        if (string!=null) return string;
        
        string = "!"+key+"!";
        String message = "Message not found in the preferred and failover locale: key=["+key+"], package="+packageName;
        LogWriter.getInstance().logError("i18n", Const.getStackTracker(new KettleException(message)));

        return string;
    }
    
    public static String getString(String packageName, String key) 
    {
        Object[] parameters = new Object[] {};
        return calculateString(packageName, key, parameters);
    }

    public static String getString(String packageName, String key, String param1)
    {
        Object[] parameters = new Object[] { param1 };
        return calculateString(packageName, key, parameters);
    }

    public static String getString(String packageName, String key, String param1, String param2)
    {
        Object[] parameters = new Object[] { param1, param2 };
        return calculateString(packageName, key, parameters);
    }

    public static String getString(String packageName, String key, String param1, String param2, String param3)
    {
        Object[] parameters = new Object[] { param1, param2, param3 };
        return calculateString(packageName, key, parameters);
    }
    
    public static String getString(String packageName, String key, String param1, String param2, String param3,String param4)
    {
        Object[] parameters = new Object[] { param1, param2, param3, param4 };
        return calculateString(packageName, key, parameters);
    }
    
    public static String getString(String packageName, String key, String param1, String param2, String param3, String param4, String param5)
    {
        Object[] parameters = new Object[] { param1, param2, param3, param4, param5 };
        return calculateString(packageName, key, parameters);
    }
    
    public static String getString(String packageName, String key, String param1, String param2, String param3,String param4,String param5,String param6)
    {
        Object[] parameters = new Object[] { param1, param2, param3, param4, param5, param6 };
        return calculateString(packageName, key, parameters);
    }
}
