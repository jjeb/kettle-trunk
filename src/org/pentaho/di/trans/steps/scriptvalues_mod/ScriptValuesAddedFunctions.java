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
 /**********************************************************************
 **                                                                   **
 ** This Script has been modified for higher performance              **
 ** and more functionality in December-2006,                          **
 ** by proconis GmbH / Germany                                        **
 **                                                                   ** 
 ** http://www.proconis.de                                            **
 ** info@proconis.de                                                  **
 **                                                                   **
 **********************************************************************/

package org.pentaho.di.trans.steps.scriptvalues_mod;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;

public class ScriptValuesAddedFunctions extends ScriptableObject {

	public static final long serialVersionUID = 1L;

	public static final int STRING_FUNCTION = 0;
	public static final int NUMERIC_FUNCTION = 1;
	public static final int DATE_FUNCTION = 2;
	public static final int LOGIC_FUNCTION = 3;
	public static final int SPECIAL_FUNCTION = 4;	
		
	public static  String[] jsFunctionList = {
        "appendToFile", "getTransformationName","writeToLog","getFiscalDate", "getProcessCount", 
        "ceil","floor", "abs", "getDayNumber", "isWorkingDay", "fireToDB", "getNextWorkingDay", 
        "quarter", "dateDiff", "dateAdd", "fillString","isCodepage", "ltrim", "rtrim", "lpad", 
        "rpad", "week", "month", "year", "str2RegExp","fileExists", "touch", "isRegExp", "date2str",
        "str2date", "sendMail", "replace", "decode", "isNum","isDate", "lower", "upper", "str2num",
        "num2str", "Alert", "setEnvironmentVar", "getEnvironmentVar", "LoadScriptFile", "LoadScriptFromTab", 
        "print", "println", "resolveIP", "trim", "substr", 
        };
	
	// This is only used for reading, so no concurrency problems.
	// todo: move in the real variables of the step.
	private static VariableSpace variables = Variables.getADefaultVariableSpace();

	// Functions to Add
	// date2num, num2date,  
	// fisc_date, isNull
	// 
	
	public static Object getTransformationName(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			Object objTranName = Context.toString(actualObject.get("_TransformationName_", actualObject));
			return (String)objTranName;
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
	}
	
	public static void appendToFile(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(!isNull(ArgList) && !isUndefined(ArgList)){
			try{
				FileOutputStream file = new FileOutputStream(Context.toString(ArgList[0]), true);
				DataOutputStream out   = new DataOutputStream(file);
				out.writeBytes(Context.toString(ArgList[1]));
				out.flush();
				out.close();
			}catch(Exception er){
				throw Context.reportRuntimeError(er.toString());
			}
		}else{
			throw Context.reportRuntimeError("The function call appendToFile requires arguments.");
		}
	}
	
	public static Object getFiscalDate(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(ArgList.length==2){
			try{
				if(isNull(ArgList)) return null;
				else if(isUndefined(ArgList)) return Context.getUndefinedValue();
				java.util.Date dIn = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				Calendar startDate = Calendar.getInstance();
				Calendar fisStartDate = Calendar.getInstance();
				Calendar fisOffsetDate = Calendar.getInstance();
				startDate.setTime(dIn);
				Format dfFormatter = new SimpleDateFormat("dd.MM.yyyy");
				String strOffsetDate = Context.toString(ArgList[1]) + String.valueOf(startDate.get(Calendar.YEAR));
				java.util.Date dOffset = (java.util.Date)dfFormatter.parseObject(strOffsetDate);
				fisOffsetDate.setTime(dOffset);
				
				String strFisStartDate = "01.01." + String.valueOf(startDate.get(Calendar.YEAR)+1);
				fisStartDate.setTime((java.util.Date)dfFormatter.parseObject(strFisStartDate));
				int iDaysToAdd = (int)((startDate.getTimeInMillis() - fisOffsetDate.getTimeInMillis()) / 86400000);
				fisStartDate.add(Calendar.DATE, iDaysToAdd);
				return fisStartDate.getTime();
			}catch(Exception e){
				throw Context.reportRuntimeError(e.toString());
			}
		}else{
			throw Context.reportRuntimeError("The function call getFiscalDate requires 2 arguments.");
		}
		
	}
	
	public static double getProcessCount(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(ArgList.length==1){
			try{
				Object scmO = actualObject.get("_step_", actualObject);
				ScriptValuesMod scm = (ScriptValuesMod)Context.jsToJava(scmO, ScriptValuesMod.class);
				String strType = Context.toString(ArgList[0]);
				
				if(strType.toLowerCase().equals("i")) return (double)scm.getLinesInput();
				else if(strType.toLowerCase().equals("o")) return (double)scm.getLinesOutput();
				else if(strType.toLowerCase().equals("r")) return (double)scm.getLinesRead();
				else if(strType.toLowerCase().equals("u")) return (double)scm.getLinesUpdated();
				else if(strType.toLowerCase().equals("w")) return (double)scm.getLinesWritten();
                else if(strType.toLowerCase().equals("e")) return (double)scm.getLinesRejected();
				else return 0;
			}catch(Exception e){
				//throw Context.reportRuntimeError(e.toString());
				return 0;
			}
		}else{
			throw Context.reportRuntimeError("The function call getProcessCount requires 1 argument.");	
		}
	}
	
	public static void writeToLog(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		switch(ArgList.length){
			case 1:
				try{
					if(!isNull(ArgList) && !isUndefined(ArgList)){
						Object scmO = actualObject.get("_step_", actualObject);
						ScriptValuesMod scm = (ScriptValuesMod)Context.jsToJava(scmO, ScriptValuesMod.class);
						String strMessage = Context.toString(ArgList[0]);
						scm.logDebug(strMessage);
					}
				}catch(Exception e){
				}
			break;
			case 2:
				try{
					if(!isNull(ArgList) && !isUndefined(ArgList)){
						Object scmO = actualObject.get("_step_", actualObject);
						ScriptValuesMod scm = (ScriptValuesMod)Context.jsToJava(scmO, ScriptValuesMod.class);
			
						String strType = Context.toString(ArgList[0]);
						String strMessage = Context.toString(ArgList[1]);
						if(strType.toLowerCase().equals("b")) scm.logBasic(strMessage);
						else if(strType.toLowerCase().equals("d")) scm.logDebug(strMessage);
						else if(strType.toLowerCase().equals("l")) scm.logDetailed(strMessage);
						else if(strType.toLowerCase().equals("e")) scm.logError(strMessage);
						else if(strType.toLowerCase().equals("m")) scm.logMinimal(strMessage);
						else if(strType.toLowerCase().equals("r")) scm.logRowlevel(strMessage);
					}
				}catch(Exception e){
				}
			break;
			default:
				throw Context.reportRuntimeError("The function call writeToLog requires 1 or 2 arguments.");	
		}
	}
	
	private static boolean isUndefined(Object ArgList){
		return isUndefined(new Object[]{ArgList}, new int[]{0});
	}
	
	private static boolean isUndefined(Object[] ArgList, int[] iArrToCheck){
		for(int i=0;i<iArrToCheck.length;i++){
			if(ArgList[iArrToCheck[i]].equals(Context.getUndefinedValue())) return true;
		}
		return false;
	}

	private static boolean isNull(Object ArgList){
		return isNull(new Object[]{ArgList}, new int[]{0});
	}
	
	private static boolean isNull(Object[] ArgList){
		for(int i=0;i<ArgList.length;i++){
			if(ArgList[i] == null ||  ArgList[i].equals(null)  ) return true;
		}
		return false;
	}
	
	private static boolean isNull(Object[] ArgList, int[] iArrToCheck){
		for(int i=0;i<iArrToCheck.length;i++){
			if(ArgList[iArrToCheck[i]] == null ||  ArgList[iArrToCheck[i]].equals(null)  ) return true;
		}
		return false;
	}
	
	public static Object abs(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return new Double(Double.NaN);
				else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				else return new Double(Math.abs(Context.toNumber(ArgList[0])));
			}catch(Exception e){
				return null;
			}
		}else{
			throw Context.reportRuntimeError("The function call abs requires 1 argument.");
		}
	}
	
	public static Object ceil(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return new Double(Double.NaN);
				else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				else return new Double(Math.ceil(Context.toNumber(ArgList[0])));
			}catch(Exception e){
				return null;
			}
		}else{
			throw Context.reportRuntimeError("The function call ceil requires 1 argument.");
		}
	}
	
	public static Object floor(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return new Double(Double.NaN);
				else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				else return new Double(Math.floor(Context.toNumber(ArgList[0])));
			}catch(Exception e){
				return null;
				//throw Context.reportRuntimeError(e.toString());		
			}
		}else{
			throw Context.reportRuntimeError("The function call floor requires 1 argument.");
		}
	}
	
	public static Object getDayNumber(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		if(ArgList.length==2){
			try{
				if(isNull(ArgList[0])) return new Double(Double.NaN);
				else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				else{
					java.util.Date dIn = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
					String strType = Context.toString(ArgList[1]);
					Calendar startDate = Calendar.getInstance(); 
					startDate.setTime(dIn);
					if(strType.toLowerCase().equals("y")) return new Double(startDate.get(Calendar.DAY_OF_YEAR));
					else if(strType.toLowerCase().equals("m")) return new Double(startDate.get(Calendar.DAY_OF_MONTH));
					else if(strType.toLowerCase().equals("w")) return new Double(startDate.get(Calendar.DAY_OF_WEEK));
					else if(strType.toLowerCase().equals("wm")) return new Double(startDate.get(Calendar.DAY_OF_WEEK_IN_MONTH));
					return new Double(startDate.get(Calendar.DAY_OF_YEAR));
				}
			}catch(Exception e){
				return null;
				//throw Context.reportRuntimeError(e.toString());		
			}
		}else{
			throw Context.reportRuntimeError("The function call getDayNumber requires 2 arguments.");
		}
	}
	
	public static Object isWorkingDay(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return null;
				else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				else{
					java.util.Date dIn = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
					Calendar startDate = Calendar.getInstance(); 
					startDate.setTime(dIn);
					if(startDate.get(Calendar.DAY_OF_WEEK)!= Calendar.SATURDAY &&startDate.get(Calendar.DAY_OF_WEEK)!= Calendar.SUNDAY) return Boolean.TRUE;
					return Boolean.FALSE;
				}
			}catch(Exception e){
				return null;
			}
		}else{
			throw Context.reportRuntimeError("The function call isWorkingDay requires 1 argument.");		
		}
	}
	
	public static Object fireToDB(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		Object oRC = new Object();
		if(ArgList.length==2){
			try{
				Object scmO = actualObject.get("_step_", actualObject);
				ScriptValuesMod scm = (ScriptValuesMod)Context.jsToJava(scmO, ScriptValuesMod.class);
				String strDBName = Context.toString(ArgList[0]);
				String strSQL = Context.toString(ArgList[1]);			
				DatabaseMeta ci = DatabaseMeta.findDatabase(scm.getTransMeta().getDatabases(), strDBName);
			
				Database db=new Database(ci);
	    		db.setQueryLimit(0);
	    		try{
	    			db.connect();
	    			ResultSet rs = db.openQuery(strSQL);
	    			ResultSetMetaData resultSetMetaData = rs.getMetaData();
					int columnCount = resultSetMetaData.getColumnCount();
					if(rs!=null){
						List<Object[]> list = new ArrayList<Object[]>(); 
						while(rs.next()){
							Object[] objRow = new Object[columnCount];
							for(int i=0;i<columnCount;i++){
								objRow[i] = rs.getObject(i+1);
							}
							list.add(objRow);
						}
						Object[][] resultArr = new Object[list.size()][];
			            list.toArray(resultArr);
			            db.disconnect();
			            return resultArr;
					}
	    		}catch(Exception er){
	    			throw Context.reportRuntimeError(er.toString());
	    		}
			}catch(Exception e){
			}
		}else{
			throw Context.reportRuntimeError("The function call fireToDB requires 2 arguments.");	
		}
		return oRC;
	}
	
	public static Object dateDiff(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		if(ArgList.length==3){
			try{
				if(isNull(ArgList, new int[]{0,1,2})) return new Double(Double.NaN);
				else if(isUndefined(ArgList, new int[]{0,1,2})) return Context.getUndefinedValue();
				else{
					java.util.Date dIn1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
					java.util.Date dIn2 = (java.util.Date)Context.jsToJava(ArgList[1], java.util.Date.class);
					String strType = Context.toString(ArgList[2]);
					int iRC=0;

					Calendar startDate = Calendar.getInstance();
					Calendar endDate = Calendar.getInstance();
					startDate.setTime(dIn1);
					endDate.setTime(dIn2);

					/* Changed by: 	Ingo Klose, SHS VIVEON AG,
					 * Date:		27.04.2007
					 *
					 * Calculating time differences using getTimeInMillis() leads to false results
					 * when crossing Daylight Savingstime borders. In order to get correct results
					 * the time zone offsets have to be added.
					 *
					 * Fix: 		1. 	calculate correct milli seconds for start and end date
					 * 				2. 	replace endDate.getTimeInMillis() with endL
					 * 					and startDate.getTimeInMillis() with startL
					 */
					long endL = endDate.getTimeInMillis() + endDate.getTimeZone().getOffset( endDate.getTimeInMillis() );
					long startL = startDate.getTimeInMillis() + startDate.getTimeZone().getOffset( startDate.getTimeInMillis() );

					if(strType.toLowerCase().equals("y")){
						return new Double(endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR));
					}else if(strType.toLowerCase().equals("m")){
						int iMonthsToAdd = (int)(endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR)) * 12;
						return new Double((endDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH)) + iMonthsToAdd);
					}else if(strType.toLowerCase().equals("d")){
						return new Double(((endL - startL) / 86400000));
					}else if(strType.toLowerCase().equals("wd")){
						int iOffset = -1;
						if(endDate.before(startDate)) iOffset = 1;
						while (endL<startL || endL>startL) {
							int day = endDate.get(Calendar.DAY_OF_WEEK);
							if ((day != Calendar.SATURDAY) && (day != Calendar.SUNDAY))	iRC++;
							endDate.add(Calendar.DATE, iOffset);
						}
						return new Double(iRC);
					}else if(strType.toLowerCase().equals("w")){
						int iDays = (int)((endL - startL) / 86400000);
						return new Double(iDays/7);
					}else if(strType.toLowerCase().equals("ss")){
						return new Double(((endL - startL) / 1000));
					}else if(strType.toLowerCase().equals("mi")){
						return new Double(((endL - startL) / 60000));
					}else if(strType.toLowerCase().equals("hh")){
						return new Double(((endL - startL) / 3600000));
					}else{
						return new Double(((endL - startL) / 86400000));
					}
					/*
			         * End Bugfix
					 */
				}
			}catch(Exception e){
				throw Context.reportRuntimeError(e.toString());
			}
		}else{
			throw Context.reportRuntimeError("The function call dateDiff requires 3 arguments.");
		}
	}
	
	public static Object getNextWorkingDay(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		//(Date dIn){
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return null;
				else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				java.util.Date dIn = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				Calendar startDate = Calendar.getInstance(); 
			    startDate.setTime(dIn);
			    startDate.add(Calendar.DATE,1);
			    while(startDate.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY || startDate.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY){
			    	startDate.add(Calendar.DATE,1);
			    }
			    return startDate.getTime();
			}catch(Exception e){
				throw Context.reportRuntimeError(e.toString());
			}
		}else{
			throw Context.reportRuntimeError("The function call getNextWorkingDay requires 1 argument.");
		}
	}
	
	public static Object dateAdd(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		if(ArgList.length==3){
			try{
				if(isNull(ArgList, new int[]{0,1,2})) return null;
				else if(isUndefined(ArgList, new int[]{0,1,2})) return Context.getUndefinedValue();
				java.util.Date dIn = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				String strType = Context.toString(ArgList[1]);
				int iValue = (int)Context.toNumber(ArgList[2]);
				Calendar cal = Calendar.getInstance(); 
				cal.setTime(dIn);
				if(strType.toLowerCase().equals("y")) cal.add(Calendar.YEAR, iValue);
				else if(strType.toLowerCase().equals("m")) cal.add(Calendar.MONTH, iValue);
				else if(strType.toLowerCase().equals("d")) cal.add(Calendar.DATE, iValue);
				else if(strType.toLowerCase().equals("w")) cal.add(Calendar.WEEK_OF_YEAR, iValue);
				else if(strType.toLowerCase().equals("wd")){
					int iOffset=0;
					while(iOffset < iValue) {
						int day = cal.get(Calendar.DAY_OF_WEEK);
						cal.add(Calendar.DATE, 1);
						if ((day != Calendar.SATURDAY) && (day != Calendar.SUNDAY))iOffset++;
					}
				}else if(strType.toLowerCase().equals("hh")) cal.add(Calendar.HOUR, iValue);
				else if(strType.toLowerCase().equals("mi")) cal.add(Calendar.MINUTE, iValue);
				else if(strType.toLowerCase().equals("ss")) cal.add(Calendar.SECOND, iValue);
				return cal.getTime();
			}catch(Exception e){
				throw Context.reportRuntimeError(e.toString());
			}
		}else{
			throw Context.reportRuntimeError("The function call dateAdd requires 3 arguments.");
		}
  }
	
    public static String fillString(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
    	if(ArgList.length==2){
    		try{
    			if(isNull(ArgList, new int[]{0,1})) return null;
				else if(isUndefined(ArgList, new int[]{0,1})) return (String)Context.getUndefinedValue();
    			String fillChar = Context.toString(ArgList[0]);
    			int count = (int)Context.toNumber(ArgList[1]);
    			if(fillChar.length()!=1){
    				throw Context.reportRuntimeError("Please provide a valid Char to the fillString");
    			}else{
    				char[] chars = new char[count];
    				while (count>0) chars[--count] = fillChar.charAt(0);
    				return new String(chars);
    			}
    		}catch(Exception e){
    			throw Context.reportRuntimeError(e.toString());
    		}
    	}else{
    		throw Context.reportRuntimeError("The function call fillString requires 2 arguments.");
    	}
    }
	
	public static Object isCodepage(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		boolean bRC = false;
		if(ArgList.length==2){
			try{
				if(isNull(ArgList, new int[]{0,1})) return null;
				else if(isUndefined(ArgList, new int[]{0,1})) return Context.getUndefinedValue();
				String strValueToCheck = Context.toString(ArgList[0]);
				String strCodePage = Context.toString(ArgList[1]);
				byte bytearray []  = strValueToCheck.getBytes();
				CharsetDecoder d = Charset.forName(strCodePage).newDecoder();
				CharBuffer r = d.decode(ByteBuffer.wrap(bytearray));
	    	    r.toString();
	    	    bRC=true;
			}catch(Exception e){
				bRC=false;
			}
		}else{
			throw Context.reportRuntimeError("The function call isCodepage requires 2 arguments.");		
		}
		return Boolean.valueOf(bRC);
	}
	
	public static String ltrim(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
	     try{
	    	 if(ArgList.length==1){
	    		 if(isNull(ArgList[0])) return null;
	    		 else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
	    		 String strValueToTrim = Context.toString(ArgList[0]);
	    		 return strValueToTrim.replaceAll("^\\s+", "");
	    	 }   
             else
             {
				throw Context.reportRuntimeError("The function call ltrim requires 1 argument.");
			 }
	     }catch(Exception e){
	    	 throw Context.reportRuntimeError("The function call ltrim is not valid : " + e.getMessage());
	     }
	 }

	public static String rtrim(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
	     try{
	    	 if(ArgList.length==1){
	    		 if(isNull(ArgList[0])) return null;
	    		 else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
	    		 String strValueToTrim = Context.toString(ArgList[0]);
	    		 return strValueToTrim.replaceAll("\\s+$", "");
	    	 }
             else
             {
				throw Context.reportRuntimeError("The function call rtrim requires 1 argument.");
			 }	    	 
	     }catch(Exception e){
	    	 throw Context.reportRuntimeError("The function call rtrim is not valid : " + e.getMessage());
	     }
	 }
	 
	 public static String lpad(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		 
		 //(String valueToPad, String filler, int size) {
	     try{
	    	 if(ArgList.length==3){
	    		 if(isNull(ArgList, new int[]{0,1,2})) return null;
	    		 else if(isUndefined(ArgList, new int[]{0,1,2})) return (String)Context.getUndefinedValue();
	    		 String valueToPad = Context.toString(ArgList[0]);
	    		 String filler = Context.toString(ArgList[1]);
	    		 int size = (int)Context.toNumber(ArgList[2]);
	    		 
	    		 while (valueToPad.length() < size){
	    			 valueToPad = filler + valueToPad;
	    		 }
	    		 return valueToPad;
	    	 }
	     }catch(Exception e){
	    	 throw Context.reportRuntimeError("The function call lpad requires 3 arguments.");
	     }
	     return null;
	 }
	 
	 public static String rpad(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
	     try{
	    	 if(ArgList.length==3){
	    		 if(isNull(ArgList, new int[]{0,1,2})) return null;
	    		 else if(isUndefined(ArgList, new int[]{0,1,2})) return (String)Context.getUndefinedValue();
	    		 String valueToPad = Context.toString(ArgList[0]);
	    		 String filler = Context.toString(ArgList[1]);
	    		 int size = (int)Context.toNumber(ArgList[2]);
	    		 
	    	        while (valueToPad.length() < size) {
	    	            valueToPad = valueToPad+filler;
	    	        }
	    		 return valueToPad;
	    	 }
	     }catch(Exception e){
	    	 throw Context.reportRuntimeError("The function call rpad requires 3 arguments.");
	     }
	     return null;
	 }
	
	public static Object year(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length==1){
	    		if(isNull(ArgList[0])) return new Double(Double.NaN);
	    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				Calendar cal=Calendar.getInstance();
			    cal.setTime(dArg1);
			    return new Double(cal.get(Calendar.YEAR));
			}else{
				throw Context.reportRuntimeError("The function call year requires 1 argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
	}

	public static Object month(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length==1){
				if(isNull(ArgList[0])) return new Double(Double.NaN);
	    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				Calendar cal=Calendar.getInstance();
			    cal.setTime(dArg1);
			    return new Double(cal.get(Calendar.MONTH));
			}else{
				throw Context.reportRuntimeError("The function call month requires 1 argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
		
	}

	public static Object quarter(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length==1){
	    		if(isNull(ArgList[0])) return new Double(Double.NaN);
	    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				Calendar cal=Calendar.getInstance();
			    cal.setTime(dArg1);
			    int iMonth = cal.get(Calendar.MONTH);
			    if(iMonth<=3) return new Double(1);
			    else if(iMonth<=6) return new Double(2);
			    else if(iMonth<=9) return new Double(3);
			    else return new Double(4);
			}else{
				throw Context.reportRuntimeError("The function call quarter requires 1 argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
	}
	
	public static Object week(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length==1){
	    		if(isNull(ArgList[0])) return new Double(Double.NaN);
	    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
				Calendar cal=Calendar.getInstance();
			    cal.setTime(dArg1);
			    return new Double(cal.get(Calendar.WEEK_OF_YEAR));
			}else{
				throw Context.reportRuntimeError("The function call week requires 1 argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
	}
	
	public static Object str2RegExp(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		String[] strArr=null; 
		if(ArgList.length==2){
			try{
	    		if(isNull(ArgList,new int[]{0,1})) return null;
	    		else if(isUndefined(ArgList,new int[]{0,1})) return Context.getUndefinedValue();
				String strToMatch = Context.toString(ArgList[0]);
				Pattern p = Pattern.compile(Context.toString(ArgList[1]));
				Matcher m = p.matcher(strToMatch);
				if(m.matches() && m.groupCount()>0){
					strArr = new String[m.groupCount()];
					for(int i=1;i<=m.groupCount();i++){
						strArr[i-1] = m.group(i);
					}
				}
			}catch(Exception e){
				throw Context.reportRuntimeError(e.toString());
			}
		}else{
			throw Context.reportRuntimeError("The function call str2RegExp requires 2 arguments.");
		}
		return strArr;
	}
	
	public static void touch(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length==1 && !isNull(ArgList[0]) && !isUndefined(ArgList[0])){
    			File file = new File(Context.toString(ArgList[0]));
    			boolean success = file.createNewFile();
    			if (!success) {
    				file.setLastModified(System.currentTimeMillis());
    			}
			}else{
				throw Context.reportRuntimeError("The function call touch requires 1 valid argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
	}

	public static Object fileExists(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length==1 && !isNull(ArgList[0]) && !isUndefined(ArgList[0])){
				if(ArgList[0].equals(null)) return null;
				File file = new File(Context.toString(ArgList[0]));
		        return Boolean.valueOf(file.isFile());
			}else{
				throw Context.reportRuntimeError("The function call fileExists requires 1 valid argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError(e.toString());
		}
	}
	
	public static Object str2date(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		Object oRC=new Object();
		switch(ArgList.length){
			case 0:
				throw  Context.reportRuntimeError("Please provide a valid string to the function call str2date.");
			case 1:
				try{
					if(isNull(ArgList[0])) return null;
		    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
					String sArg1 = Context.toString(ArgList[0]);
					Format dfFormatter = new SimpleDateFormat();
					oRC = dfFormatter.parseObject(sArg1);
					//if(Double.isNaN(sArg1))	throw Context.reportRuntimeError("The first Argument must be a Number.");
					//DecimalFormat formatter = new DecimalFormat();
					//sRC= formatter.format(sArg1); 
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not apply local format : " + e.getMessage());
				}
				break;
			case 2:
				try{
					if(isNull(ArgList,new int[]{0,1})) return null;
		    		else if(isUndefined(ArgList,new int[]{0,1})) return Context.getUndefinedValue();
					String sArg1 = Context.toString(ArgList[0]);
					String sArg2 = Context.toString(ArgList[1]);
					Format dfFormatter = new SimpleDateFormat(sArg2);
					oRC = dfFormatter.parseObject(sArg1);
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not apply the given format on the string : " + e.getMessage());
				}
				break;
			case 3:
				try{
					if(isNull(ArgList,new int[]{0,1,2})) return null;
		    		else if(isUndefined(ArgList,new int[]{0,1,2})) return Context.getUndefinedValue();
					String sArg1 = Context.toString(ArgList[0]);
					Format dfFormatter;
					String sArg2 = Context.toString(ArgList[1]);
					String sArg3 = Context.toString(ArgList[2]);
					if(sArg3.length() == 2){
						Locale dfLocale = new Locale(sArg3);
						dfFormatter = new SimpleDateFormat(sArg2, dfLocale);
						oRC = dfFormatter.parseObject(sArg1);
					}else{
						throw Context.reportRuntimeError("");
					}
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not apply the local format : " + e.getMessage());
				}
				break;				
			default:
				throw Context.reportRuntimeError("The function call str2date requires 1, 2, or 3 arguments.");
		}
		return oRC;
	}
		
	public static Object date2str(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		Object oRC=new Object();
		switch(ArgList.length){
			case 0:
				throw  Context.reportRuntimeError("Please provide a valid date to the function call date2str.");
			case 1:
				try{
					if(isNull(ArgList)) return null;
		    		else if(isUndefined(ArgList)) return Context.getUndefinedValue();
					java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class); 
					if(dArg1.equals(null)) return null;
					Format dfFormatter = new SimpleDateFormat();
					oRC = dfFormatter.format(dArg1);
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not convert to local format.");
				}
				break;
			case 2:
				try{
					if(isNull(ArgList,new int[]{0,1})) return null;
		    		else if(isUndefined(ArgList,new int[]{0,1})) return Context.getUndefinedValue();
					java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class); 
					String sArg2 = Context.toString(ArgList[1]);
					Format dfFormatter = new SimpleDateFormat(sArg2);
					oRC = dfFormatter.format(dArg1);
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not convert to the given format.");
				}
				break;
			case 3:
				try{
					if(isNull(ArgList,new int[]{0,1,2})) return null;
		    		else if(isUndefined(ArgList,new int[]{0,1,2})) return Context.getUndefinedValue();
					java.util.Date dArg1 = (java.util.Date)Context.jsToJava(ArgList[0], java.util.Date.class);
					Format dfFormatter;
					String sArg2 = Context.toString(ArgList[1]);
					String sArg3 = Context.toString(ArgList[2]);
					if(sArg3.length() == 2){
						Locale dfLocale = new Locale(sArg3.toLowerCase());
						dfFormatter = new SimpleDateFormat(sArg2, dfLocale);
						oRC = dfFormatter.format(dArg1);
					}else{
						throw Context.reportRuntimeError("");
					}
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not convert to the given local format.");
				}
				break;				
			default:
				throw Context.reportRuntimeError("The function call date2str requires 1, 2, or 3 arguments.");
		}
		return oRC;
	}
	
	public static Object isRegExp(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(ArgList.length>=2){
			if(isNull(ArgList,new int[]{0,1})) return null;
    		else if(isUndefined(ArgList,new int[]{0,1})) return Context.getUndefinedValue();
			String strToMatch = Context.toString(ArgList[0]);
			for(int i=1;i<ArgList.length;i++){
				Pattern p = Pattern.compile(Context.toString(ArgList[i]));
				Matcher m = p.matcher(strToMatch);
				if(m.matches()) return new Double(i);
			}
		}
		return new Double(-1);
	}

	public static void sendMail(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		boolean debug = false;
		
		// Arguments:
		// String smtp, String from, String recipients[ ], String subject, String message 
		if(ArgList.length==5){
			
			try{
				//Set the host smtp address
				Properties props = new Properties();
				props.put("mail.smtp.host", ArgList[0]);
		
				// create some properties and get the default Session
				Session session = Session.getDefaultInstance(props, null);
				session.setDebug(debug);

				// create a message
				Message msg = new MimeMessage(session);

				// set the from and to address
				InternetAddress addressFrom = new InternetAddress((String)ArgList[1]);
				msg.setFrom(addressFrom);

				// Get Recipients
				String strArrRecipients[] = ((String)ArgList[2]).split(",");
	    	
				InternetAddress[] addressTo = new InternetAddress[strArrRecipients.length];
				for (int i = 0; i < strArrRecipients.length; i++){
					addressTo[i] = new InternetAddress(strArrRecipients[i]);
				}
				msg.setRecipients(Message.RecipientType.TO, addressTo);
	   

				// Optional : You can also set your custom headers in the Email if you Want
				msg.addHeader("MyHeaderName", "myHeaderValue");

				// Setting the Subject and Content Type
				msg.setSubject((String)ArgList[3]);
				msg.setContent((String)ArgList[4], "text/plain");
				Transport.send(msg);
			}catch(Exception e){
				throw Context.reportRuntimeError("sendMail: "+e.toString() );
			}
		}else{
			throw Context.reportRuntimeError("The function call sendMail requires 5 arguments.");
		}
	}

	public static String upper(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		String sRC="";
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return null;
	    		else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
				sRC = Context.toString(ArgList[0]);
				sRC = sRC.toUpperCase();
			}catch(Exception e){
				throw Context.reportRuntimeError("The function call upper is not valid : " + e.getMessage());
			}
		}else{
			throw Context.reportRuntimeError("The function call upper requires 1 argument.");
		}
		return sRC;
	}

	public static String lower(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		String sRC="";
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return null;
	    		else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
				sRC = Context.toString(ArgList[0]);
				sRC = sRC.toLowerCase();
			}catch(Exception e){
				throw Context.reportRuntimeError("The function call lower is not valid : " + e.getMessage());
			}
		}else{
			throw Context.reportRuntimeError("The function call lower requires 1 argument.");
		}
		return sRC;
	}
    
	// Converts the given Numeric to a JScript String
	public static String num2str(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		String sRC="";
		switch(ArgList.length){
			case 0:
				throw  Context.reportRuntimeError("The function call num2str requires at least 1 argument.");
			case 1:
				try{
					if(isNull(ArgList[0])) return null;
		    		else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
					double sArg1 = Context.toNumber(ArgList[0]);
					if(Double.isNaN(sArg1))	throw Context.reportRuntimeError("The first Argument must be a Number.");
					DecimalFormat formatter = new DecimalFormat();
					sRC= formatter.format(sArg1); 
				}catch(IllegalArgumentException e){
					throw Context.reportRuntimeError("Could not apply the given format on the number : " + e.getMessage());
				}
				break;
			case 2:
				try{
					if(isNull(ArgList,new int[]{0,1})) return null;
		    		else if(isUndefined(ArgList,new int[]{0,1})) return (String)Context.getUndefinedValue();
					double sArg1 = Context.toNumber(ArgList[0]);
					if(Double.isNaN(sArg1))	throw Context.reportRuntimeError("The first Argument must be a Number.");
					String sArg2 = Context.toString(ArgList[1]);
					DecimalFormat formatter = new DecimalFormat(sArg2);
					sRC= formatter.format(sArg1); 
				}catch(IllegalArgumentException e){
					throw Context.reportRuntimeError("Could not apply the given format on the number : " + e.getMessage());
				}
				break;
			case 3:
				try{
					if(isNull(ArgList,new int[]{0,1,2})) return null;
		    		else if(isUndefined(ArgList,new int[]{0,1,2})) return (String)Context.getUndefinedValue();
					double sArg1 = Context.toNumber(ArgList[0]);
					if(Double.isNaN(sArg1))	throw Context.reportRuntimeError("The first Argument must be a Number.");
					String sArg2 = Context.toString(ArgList[1]);
					String sArg3 = Context.toString(ArgList[2]);
					if(sArg3.length() == 2){
						DecimalFormatSymbols dfs = new DecimalFormatSymbols(new Locale(sArg3.toLowerCase()));
						DecimalFormat formatter = new DecimalFormat(sArg2, dfs);
						sRC = formatter.format(sArg1); 
					}
				}catch(Exception e){
					throw Context.reportRuntimeError(e.toString());
				}
				break;			
			default:
				throw Context.reportRuntimeError("The function call num2str requires 1, 2, or 3 arguments.");
		}
		
		return sRC;
	}
	
	// Converts the given String to a JScript Numeric
	public static Object str2num(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		double dRC=0.00;
		switch(ArgList.length){
			case 0:
				throw  Context.reportRuntimeError("The function call str2num requires at least 1 argument.");
			case 1:
				try{
					if(isNull(ArgList[0])) return new Double(Double.NaN);
		    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
					if(ArgList[0].equals(null)) return null;
					String sArg1 = Context.toString(ArgList[0]);
					DecimalFormat formatter = new DecimalFormat();
					dRC= (formatter.parse(sArg1)).doubleValue(); 
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not convert the given String : " + e.getMessage());
				}
				break;
			case 2:
				try{
					if(isNull(ArgList, new int[]{0,1}))return new Double(Double.NaN);
		    		else if(isUndefined(ArgList,new int[]{0,1})) return Context.getUndefinedValue();
					String sArg1 = Context.toString(ArgList[0]);
					String sArg2 = Context.toString(ArgList[1]);
					if(sArg1.equals("null") || sArg2.equals("null")) return null;
					DecimalFormat formatter = new DecimalFormat(sArg2);
					dRC= (formatter.parse(sArg1)).doubleValue();
					return new Double(dRC);
				}catch(Exception e){
					throw Context.reportRuntimeError("Could not convert the String with the given format :" + e.getMessage());
				}
				//break;
			case 3:
				try{
					if(isNull(ArgList,new int[]{0,1,2}))return new Double(Double.NaN);
		    		else if(isUndefined(ArgList,new int[]{0,1,2})) return Context.getUndefinedValue();
					String sArg1 = Context.toString(ArgList[0]);
					String sArg2 = Context.toString(ArgList[1]);
					String sArg3 = Context.toString(ArgList[2]);
					if(sArg3.length() == 2){
						DecimalFormatSymbols dfs = new DecimalFormatSymbols(new Locale(sArg3.toLowerCase()));
						DecimalFormat formatter = new DecimalFormat(sArg2, dfs);
						dRC= (formatter.parse(sArg1)).doubleValue(); 
						return new Double(dRC);
					}
				}catch(Exception e){
					throw Context.reportRuntimeError(e.getMessage());
				}
				break;				
			default:
				throw Context.reportRuntimeError("The function call str2num requires 1, 2, or 3 arguments.");
		}
		return new Double(dRC);
	}
	
	public static Object isNum(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return null;
	    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				double sArg1 = Context.toNumber(ArgList[0]);
				if(Double.isNaN(sArg1)) return Boolean.FALSE;
				else return Boolean.TRUE;
			}catch(Exception e){
				return Boolean.FALSE;
			}
		}else{
			throw Context.reportRuntimeError("The function call isNum requires 1 argument.");
		}
	}
	
	public static Object isDate(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		if(ArgList.length==1){
			try{
				if(isNull(ArgList[0])) return null;
	    		else if(isUndefined(ArgList[0])) return Context.getUndefinedValue();
				/* java.util.Date d = (java.util.Date)*/ Context.jsToJava(ArgList[0], java.util.Date.class); 
				return Boolean.TRUE;
			}catch(Exception e){
				return Boolean.FALSE;
			}
		}else{
			throw Context.reportRuntimeError("The function call isDate requires 1 argument.");
		}
	}
	
	public static Object decode(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length>=2){
				if(isNull(ArgList,new int[]{0,1})) return null;
	    		else if(isUndefined(ArgList,new int[]{0,1})) return Context.getUndefinedValue();
				Object objToCompare = ArgList[0];
				for(int i=1;i<ArgList.length-1;i=i+2)	if(ArgList[i].equals(objToCompare)) return ArgList[i+1];
				if(ArgList.length%2==0)return ArgList[ArgList.length-1]; 
				else return objToCompare;
			}else{
				throw Context.reportRuntimeError("The function call decode requires more than 1 argument.");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError("The function call decode is not valid : " + e.getMessage());
		}
	}
	
	public static String replace(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		try{
			if(ArgList.length>=2 && (ArgList.length-1)%2==0){
				if(isNull(ArgList,new int[]{0,1})) return null;
	    		else if(isUndefined(ArgList,new int[]{0,1})) return (String)Context.getUndefinedValue();
				String objForReplace = Context.toString(ArgList[0]);
				for(int i=1;i<ArgList.length-1;i=i+2) objForReplace=objForReplace.replaceAll(Context.toString(ArgList[i]),Context.toString(ArgList[i+1]));
				return objForReplace;
			}else{
				throw Context.reportRuntimeError("The function call replace is not valid (wrong number of arguments)");
			}
		}catch(Exception e){
			throw Context.reportRuntimeError("Function call replace is not valid : " + e.getMessage());
		}
	}
	
	// Implementation of the JS AlertBox
	public static String Alert(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		
		SpoonInterface spoon = SpoonFactory.getInstance();
		if( ArgList.length==1 && spoon != null ) 
		{
			String strMessage = Context.toString(ArgList[0]);
			spoon.messageBox(strMessage, "Alert", false, Const.INFO);
		}
		
		return "";
	}
	
	// Setting EnvironmentVar
	public static void setEnvironmentVar(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		String sArg1 = "";
		String sArg2 = "";
		if(ArgList.length==2){
			try{
				sArg1 = Context.toString(ArgList[0]);
				sArg2 = Context.toString(ArgList[1]);
				System.setProperty(sArg1, sArg2);
			}catch(Exception e){
				throw Context.reportRuntimeError(e.toString());
			}
		}
		else
		{
			throw Context.reportRuntimeError("The function call setEnvironmentVar requires 2 arguments.");
		}
	}
	
	// Returning EnvironmentVar
	public static String getEnvironmentVar(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
		String sRC="";
		if(ArgList.length==1){
			try{
				String sArg1 = Context.toString(ArgList[0]);
				sRC = variables.getVariable(sArg1,"");
			}catch(Exception e){
				sRC="";
			}
		}
        else
        {
		    throw Context.reportRuntimeError("The function call getEnvironmentVar requires 1 argument.");
        }		
		return sRC;
	}
    
    public static String trim(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
        String sRC="";
        if(ArgList.length==1)
        {
            try{
                if(isNull(ArgList[0])) return null;
                else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
                sRC = Context.toString(ArgList[0]);
                sRC = Const.trim(sRC);
            }catch(Exception e){
                throw Context.reportRuntimeError("The function call trim is not valid : " + e.getMessage());
            }
        }
        else
        {
		    throw Context.reportRuntimeError("The function call trim requires 1 argument.");
        }
        return sRC;
    }

    public static String substr(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext){
        String sRC="";
        if ( ArgList.length == 2 ) 
        {
            try
            {
                if(isNull(ArgList[0])) return null;
                else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
                sRC = Context.toString(ArgList[0]);
                int from = (int)Math.round(Context.toNumber(ArgList[1]));
                sRC = sRC.substring(from);
            }
            catch(Exception e)
            {
                throw Context.reportRuntimeError("The function call substr is not valid : " + e.getMessage());
            }
        } 
        else if ( ArgList.length == 3) 
        {
            try
            {
                if(isNull(ArgList[0])) return null;
                else if(isUndefined(ArgList[0])) return (String)Context.getUndefinedValue();
                sRC = Context.toString(ArgList[0]);
                int from = (int)Math.round(Context.toNumber(ArgList[1]));
                int to   = (int)Math.round(Context.toNumber(ArgList[2]));
                sRC = sRC.substring(from, to);
            }
            catch(Exception e)
            {
                throw Context.reportRuntimeError("The function call substr is not valid : " + e.getMessage());
            }
        } 
        else 
        {
			throw Context.reportRuntimeError("The function call substr requires 2 or 3 arguments.");
        }
        return sRC;
    }
	
	// Resolve an IP address
	public static String resolveIP(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext) {
		String sRC="";
		if(ArgList.length==2)
		{
			try{
				InetAddress addr = InetAddress.getByName(Context.toString(ArgList[0]));
				if(Context.toString(ArgList[1]).equals("IP")) sRC = addr.getHostName();
				else sRC = addr.getHostAddress();
				if(sRC.equals(Context.toString(ArgList[0]))) sRC="-";
			}catch(Exception e){
				sRC="-";
			}
		}
        else {
			throw Context.reportRuntimeError("The function call resolveIP requires 2 arguments.");
        }

		return sRC;
	}
	
	
	// Loading additional JS Files inside the JavaScriptCode
	public static void LoadScriptFile(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext) {
		for (int i = 0; i < ArgList.length; i++) { // don't worry about "undefined" arguments
			checkAndLoadJSFile(actualContext,actualObject,Context.toString(ArgList[i]));
		}
	}
	
	// Adding the ScriptsItemTab to the actual running Context
	public static void LoadScriptFromTab(Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext) {
		try{
			for (int i = 0; i < ArgList.length; i++) { // don't worry about "undefined" arguments
				String strToLoad  = Context.toString(ArgList[i]);
				String strScript = actualObject.get(strToLoad,actualObject).toString();
				actualContext.evaluateString(actualObject, strScript, "_" + strToLoad + "_", 0, null);
			}
		}catch(Exception e){
			//System.out.println(e.toString());
		}
	}
	
	// Print
	public static void print (Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext) {
		for (int i = 0; i < ArgList.length; i++) { // don't worry about "undefined" arguments
			java.lang.System.out.print(Context.toString(ArgList[i]));
		}
	}
	  
	// Prints Line to the actual System.out
	public static void println (Context actualContext, Scriptable actualObject, Object[] ArgList, Function FunctionContext) {
		print(actualContext,actualObject,ArgList,FunctionContext);
		java.lang.System.out.println();
	}
	
	// Returns the actual ClassName
	public String getClassName () {
		return "SciptValuesAddedFunctions";
	};
	
	// Evaluates the given ScriptFile
	private static void checkAndLoadJSFile(Context actualContext, Scriptable eval_scope, String FileName) {
	    FileReader InStream = null;
	    try {
	      InStream = new FileReader(FileName);
	      actualContext.evaluateReader(eval_scope, InStream, FileName, 1, null);
	    } catch (FileNotFoundException Signal) {
	    	Context.reportError("Unable to open file \"" + FileName + "\" (reason: \"" + Signal.getMessage() + "\")");
	    } catch (WrappedException Signal) {
	    	Context.reportError("WrappedException while evaluating file \"" + FileName + "\" (reason: \"" + Signal.getMessage() + "\")");
	    } catch (EvaluatorException Signal) {
	    	Context.reportError("EvaluatorException while evaluating file \"" + FileName + "\" (reason: \"" + Signal.getMessage() + "\")");
	    } catch (JavaScriptException Signal) {
	    	Context.reportError("JavaScriptException while evaluating file \"" + FileName + "\" (reason: \"" + Signal.getMessage() + "\")");
	    } catch (IOException Signal) {
	    	Context.reportError("Error while reading file \"" + FileName + "\" (reason: \"" + Signal.getMessage() + "\")"	      );
	    } finally {
	      try {
	        if (InStream != null) InStream.close();
	      } catch (Exception Signal) {
	        /* nop */
	      };
	    };
	  };
}