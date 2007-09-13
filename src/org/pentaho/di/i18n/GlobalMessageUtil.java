
package org.pentaho.di.i18n;


import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class GlobalMessageUtil {


  public static String formatErrorMessage(String key, String msg) {
    String s2 = key.substring(0, key.indexOf('.')+"ERROR_0000".length()+1); //$NON-NLS-1$ //$NON-NLS-2$
    return BaseMessages.getString("MESSUTIL.ERROR_FORMAT_MASK", s2, msg); //$NON-NLS-1$
  }

  public static String getString(ResourceBundle bundle, String key) throws MissingResourceException 
  {
    // try {
      return MessageFormat.format(bundle.getString(key), new Object[] {});
    // } catch (Exception e) {
    //   return '!' + key + '!';
    //}
  }

  public static String getErrorString(ResourceBundle bundle, String key) {
    return formatErrorMessage(key, getString(bundle, key));
  }

  public static String getString(ResourceBundle bundle, String key, String param1) {
    try {
      Object[] args = {param1};
      return MessageFormat.format(bundle.getString(key), args);      
    } catch (Exception e) {
      return '!' + key + '!';
    }
  }

  public static String getErrorString(ResourceBundle bundle, String key, String param1) {
    return formatErrorMessage(key, getString(bundle, key, param1));
  }

  public static String getString(ResourceBundle bundle, String key, String param1, String param2) {
    try {
      Object[] args = {param1, param2};
      return MessageFormat.format(bundle.getString(key), args);      
    } catch (Exception e) {
      return '!' + key + '!';
    }
  }
  
  public static String getErrorString(ResourceBundle bundle, String key, String param1, String param2) {
    return formatErrorMessage(key, getString(bundle, key, param1, param2));
  }

  public static String getString(ResourceBundle bundle, String key, String param1, String param2, String param3) {
    try {
      Object[] args = {param1, param2, param3};
      return MessageFormat.format(bundle.getString(key), args);      
    } catch (Exception e) {
      return '!' + key + '!';
    }
  }
  
  public static String getErrorString(ResourceBundle bundle, String key, String param1, String param2, String param3) {
    return formatErrorMessage(key, getString(bundle, key, param1, param2, param3));
  }

  public static String getString(ResourceBundle bundle, String key, String param1, String param2, String param3, String param4) {
    try {
      Object[] args = {param1, param2, param3, param4};
      return MessageFormat.format(bundle.getString(key), args);      
    } catch (Exception e) {
      return '!' + key + '!';
    }
  }
  
  public static String getString(ResourceBundle bundle, String key, String param1, String param2, String param3, String param4,String param5) {
	    try {
	      Object[] args = {param1, param2, param3, param4,param5};
	      return MessageFormat.format(bundle.getString(key), args);      
	    } catch (Exception e) {
	      return '!' + key + '!';
	    }
	  }
  
  public static String getString(ResourceBundle bundle, String key, String param1, String param2, String param3, String param4,String param5,String param6) {
	    try {
	      Object[] args = {param1, param2, param3, param4,param5,param6};
	      return MessageFormat.format(bundle.getString(key), args);      
	    } catch (Exception e) {
	      return '!' + key + '!';
	    }
	  }

  public static String getErrorString(ResourceBundle bundle, String key, String param1, String param2, String param3, String param4) {
    return formatErrorMessage(key, getString(bundle, key, param1, param2, param3, param4));
  }
  
  /*
  public static String formatMessage(String pattern) {
      try {
        Object[] args = {};
        return MessageFormat.format(pattern, args);      
      } catch (Exception e) {
        return '!' + pattern + '!';
      }
    }

  public static String formatMessage(String pattern, String param1) {
    try {
      Object[] args = {param1};
      return MessageFormat.format(pattern, args);      
    } catch (Exception e) {
      return '!' + pattern + '!';
    }
  }
  
  public static String formatMessage(String pattern, String param1, String param2) {
    try {
      Object[] args = {param1, param2};
      return MessageFormat.format(pattern, args);      
    } catch (Exception e) {
      return '!' + pattern + '!';
    }
    
  }
  
  public static String formatMessage(String pattern, String param1, String param2, String param3) {
    try {
      Object[] args = {param1, param2, param3};
      return MessageFormat.format(pattern, args);      
    } catch (Exception e) {
      return '!' + pattern + '!';
    }
  }

  public static String formatMessage(String pattern, String param1, String param2, String param3, String param4) {
    try {
      Object[] args = {param1, param2, param3, param4};
      return MessageFormat.format(pattern, args);      
    } catch (Exception e) {
      return '!' + pattern + '!';
    }
  }
  */
}