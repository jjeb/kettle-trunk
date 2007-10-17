package org.pentaho.di.core.row;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.pentaho.di.compatibility.Value;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.Messages;
import org.w3c.dom.Node;

public class ValueMeta implements ValueMetaInterface
{
    public static final String DEFAULT_DATE_FORMAT_MASK = "yyyy/MM/dd HH:mm:ss.SSS";
    
	public static final String XML_META_TAG = "value-meta";
	public static final String XML_DATA_TAG = "value-data";
	
    private String   name;
    private int      length;
    private int      precision;
    private int      type;
    private int      trimType;
    private int      storageType;
    private String   origin;
    private String   comments;
    private Object[] index;
    private String   conversionMask;
    private String   stringEncoding;
    private String   decimalSymbol;
    private String   groupingSymbol;
    private String   currencySymbol;
    private boolean  caseInsensitive;
    private boolean  sortedDescending;
    private boolean  outputPaddingEnabled;
    private boolean  largeTextField;
    private Locale   dateFormatLocale;
    private boolean  dateFormatLenient;
    
    private SimpleDateFormat dateFormat;
    private boolean dateFormatChanged;
    
    private DecimalFormat    decimalFormat;
    private boolean decimalFormatChanged;
    
    private ValueMetaInterface storageMetadata;
    private ValueMetaInterface stringMetadata;
    private boolean identicalFormat;

	private ValueMetaInterface nativeMetadata;
	
	/**
	 * The trim type codes
	 */
	public final static String trimTypeCode[] = { "none", "left", "right", "both" };

	/** 
	 * The trim description
	 */
	public final static String trimTypeDesc[] = { Messages.getString("ValueMeta.TrimType.None"), Messages.getString("ValueMeta.TrimType.Left"),
		Messages.getString("ValueMeta.TrimType.Right"), Messages.getString("ValueMeta.TrimType.Both") };

    public ValueMeta()
    {
        this(null, ValueMetaInterface.TYPE_NONE, -1, -1);
    }
    
    public ValueMeta(String name)
    {
        this(name, ValueMetaInterface.TYPE_NONE, -1, -1);
    }

    public ValueMeta(String name, int type)
    {
        this(name, type, -1, -1);
    }
    
    public ValueMeta(String name, int type, int storageType)
    {
        this(name, type, -1, -1);
        this.storageType = storageType;
    }
    
    public ValueMeta(String name, int type, int length, int precision)
    {
        this.name = name;
        this.type = type;
        this.length = length;
        this.precision = precision;
        this.storageType=STORAGE_TYPE_NORMAL;
        this.sortedDescending=false;
        this.outputPaddingEnabled=false;
        this.decimalSymbol = ""+Const.DEFAULT_DECIMAL_SEPARATOR;
        this.groupingSymbol = ""+Const.DEFAULT_GROUPING_SEPARATOR;
        this.dateFormatLocale = Locale.getDefault();
        this.identicalFormat = true;
    }
    
    public ValueMeta clone()
    {
        try
        {
            ValueMeta valueMeta = (ValueMeta) super.clone();
            valueMeta.dateFormat = null;
            valueMeta.decimalFormat = null;
            if (dateFormatLocale!=null) valueMeta.dateFormatLocale = (Locale) dateFormatLocale.clone();
            if (storageMetadata!=null) valueMeta.storageMetadata = storageMetadata.clone();
            
            valueMeta.compareStorageAndActualFormat();
            
            return valueMeta;
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }

    /**
     * @return the comments
     */
    public String getComments()
    {
        return comments;
    }
    
    /**
     * @param comments the comments to set
     */
    public void setComments(String comments)
    {
        this.comments = comments;
    }
    
    /**
     * @return the index
     */
    public Object[] getIndex()
    {
        return index;
    }
    
    /**
     * @param index the index to set
     */
    public void setIndex(Object[] index)
    {
        this.index = index;
    }
    
    /**
     * @return the length
     */
    public int getLength()
    {
        return length;
    }
    
    /**
     * @param length the length to set
     */
    public void setLength(int length)
    {
        this.length = length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(int length, int precision)
    {
        this.length = length;
        this.precision = precision;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    /**
     * @return the origin
     */
    public String getOrigin()
    {
        return origin;
    }
    
    /**
     * @param origin the origin to set
     */
    public void setOrigin(String origin)
    {
        this.origin = origin;
    }
    
    /**
     * @return the precision
     */
    public int getPrecision()
    {
    	// For backward compatibility we need to tweak a bit...
    	//
    	if (isInteger() || isBinary()) return 0;
    	if (isString() || isBoolean()) return -1;
    	
        return precision;
    }
    
    /**
     * @param precision the precision to set
     */
    public void setPrecision(int precision)
    {
        this.precision = precision;
    }
    
    /**
     * @return the storageType
     */
    public int getStorageType()
    {
        return storageType;
    }
    
    /**
     * @param storageType the storageType to set
     */
    public void setStorageType(int storageType)
    {
        this.storageType = storageType;
    }

    public boolean isStorageNormal()
    {
        return storageType == STORAGE_TYPE_NORMAL;
    }

    public boolean isStorageIndexed()
    {
        return storageType == STORAGE_TYPE_INDEXED;
    }

    public boolean isStorageBinaryString()
    {
        return storageType == STORAGE_TYPE_BINARY_STRING;
    }


    /**
     * @return the type
     */
    public int getType()
    {
        return type;
    }
    
    /**
     * @param type the type to set
     */
    public void setType(int type)
    {
        this.type = type;
    }
    
    /**
     * @return the conversionMask
     */
    public String getConversionMask()
    {
        return conversionMask;
    }
    
    /**
     * @param conversionMask the conversionMask to set
     */
    public void setConversionMask(String conversionMask)
    {
        this.conversionMask = conversionMask;
        dateFormatChanged = true;
        decimalFormatChanged = true;
        compareStorageAndActualFormat();
    }
    
    /**
     * @return the encoding
     */
    public String getStringEncoding()
    {
        return stringEncoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setStringEncoding(String encoding)
    {
        this.stringEncoding = encoding;
        compareStorageAndActualFormat();
    }
    
    /**
     * @return the decimalSymbol
     */
    public String getDecimalSymbol()
    {
        return decimalSymbol;
    }

    /**
     * @param decimalSymbol the decimalSymbol to set
     */
    public void setDecimalSymbol(String decimalSymbol)
    {
        this.decimalSymbol = decimalSymbol;
        decimalFormatChanged = true;
        compareStorageAndActualFormat();
    }

    /**
     * @return the groupingSymbol
     */
    public String getGroupingSymbol()
    {
        return groupingSymbol;
    }

    /**
     * @param groupingSymbol the groupingSymbol to set
     */
    public void setGroupingSymbol(String groupingSymbol)
    {
        this.groupingSymbol = groupingSymbol;
        decimalFormatChanged = true;
        compareStorageAndActualFormat();
    }
    

    /**
     * @return the currencySymbol
     */
    public String getCurrencySymbol()
    {
        return currencySymbol;
    }

    /**
     * @param currencySymbol the currencySymbol to set
     */
    public void setCurrencySymbol(String currencySymbol)
    {
        this.currencySymbol = currencySymbol;
        decimalFormatChanged = true;
    }
    
    /**
     * @return the caseInsensitive
     */
    public boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    /**
     * @param caseInsensitive the caseInsensitive to set
     */
    public void setCaseInsensitive(boolean caseInsensitive)
    {
        this.caseInsensitive = caseInsensitive;
    }

    
    /**
     * @return the sortedDescending
     */
    public boolean isSortedDescending()
    {
        return sortedDescending;
    }

    /**
     * @param sortedDescending the sortedDescending to set
     */
    public void setSortedDescending(boolean sortedDescending)
    {
        this.sortedDescending = sortedDescending;
    }
    

    /**
     * @return true if output padding is enabled (padding to specified length)
     */
    public boolean isOutputPaddingEnabled()
    {
        return outputPaddingEnabled;
    }

    /**
     * @param outputPaddingEnabled Set to true if output padding is to be enabled (padding to specified length)
     */
    public void setOutputPaddingEnabled(boolean outputPaddingEnabled)
    {
        this.outputPaddingEnabled = outputPaddingEnabled;
    }

    /**
     * @return true if this is a large text field (CLOB, TEXT) with arbitrary length.
     */
    public boolean isLargeTextField()
    {
        return largeTextField;
    }

    /**
     * @param largeTextField Set to true if this is to be a large text field (CLOB, TEXT) with arbitrary length.
     */
    public void setLargeTextField(boolean largeTextField)
    {
        this.largeTextField = largeTextField;
    }
    
    /**
     * @return the dateFormatLenient
     */
    public boolean isDateFormatLenient()
    {
        return dateFormatLenient;
    }

    /**
     * @param dateFormatLenient the dateFormatLenient to set
     */
    public void setDateFormatLenient(boolean dateFormatLenient)
    {
        this.dateFormatLenient = dateFormatLenient;
        dateFormatChanged=true;
    }

    /**
     * @return the dateFormatLocale
     */
    public Locale getDateFormatLocale()
    {
        return dateFormatLocale;
    }

    /**
     * @param dateFormatLocale the dateFormatLocale to set
     */
    public void setDateFormatLocale(Locale dateFormatLocale)
    {
        this.dateFormatLocale = dateFormatLocale;
        dateFormatChanged=true;
    }
    
    

    // DATE + STRING


    private synchronized String convertDateToString(Date date)
    {
        if (date==null) return null;
        
        return getDateFormat().format(date);
    }

    private synchronized Date convertStringToDate(String string) throws KettleValueException
    {
        if (Const.isEmpty(string)) return null;
        
        string = trim(string); // see if  trimming needs to be performed before conversion

        try
        {
            return getDateFormat().parse(string);
        }
        catch (ParseException e)
        {
            throw new KettleValueException(toString()+" : couldn't convert string ["+string+"] to a date", e);
        }
    }

    // DATE + NUMBER 

    private Double convertDateToNumber(Date date)
    {
        return new Double( date.getTime() );
    }

    private Date convertNumberToDate(Double number)
    {
        return new Date( number.longValue() );
    }

    // DATE + INTEGER
    
    private Long convertDateToInteger(Date date)
    {
        return new Long( date.getTime() );
    }

    private Date convertIntegerToDate(Long number)
    {
        return new Date( number.longValue() );
    }

    // DATE + BIGNUMBER
    
    private BigDecimal convertDateToBigNumber(Date date)
    {
        return new BigDecimal( date.getTime() );
    }

    private Date convertBigNumberToDate(BigDecimal number)
    {
        return new Date( number.longValue() );
    }

    private synchronized String convertNumberToString(Double number) throws KettleValueException
    {
        if (number==null) {
        	if (!outputPaddingEnabled || length<1) {
        		return null;
        	}
        	else {
        		// Return strings padded to the specified length...
        		// This is done for backward compatibility with 2.5.x 
        		// We just optimized this a bit...
        		//
        		String[] emptyPaddedStrings = Const.getEmptyPaddedStrings();
        		if (length<emptyPaddedStrings.length) {
        			return emptyPaddedStrings[length];
        		}
        		else {
        			return Const.rightPad("", length);
        		}
        	}
        }
        
        try
        {
            return getDecimalFormat().format(number);
        }
        catch(Exception e)
        {
            throw new KettleValueException(toString()+" : couldn't convert Number to String ", e);
        }
    }
    
    private synchronized Double convertStringToNumber(String string) throws KettleValueException
    {
        if (Const.isEmpty(string)) return null;
        
        string = trim(string); // see if  trimming needs to be performed before conversion

        try
        {
            return new Double( getDecimalFormat().parse(string).doubleValue() );
        }
        catch(Exception e)
        {
            throw new KettleValueException(toString()+" : couldn't convert String to number ", e);
        }
    }
    
    public synchronized SimpleDateFormat getDateFormat()
    {
    	// If we have a Date that is represented as a String
    	// In that case we can set the format of the original Date on the String value metadata in the form of a storage metadata object.
    	// That way, we can always convert from Date to String and back without a problem, no matter how complex the format was.
    	// As such, we should return the date SimpleDateFormat of the storage metadata.
    	//
    	if (storageMetadata!=null) {
    		return storageMetadata.getDateFormat();
    	}
    	
        if (dateFormat==null || dateFormatChanged)
        {
        	// This may not become static as the class is not thread-safe!
            dateFormat = new SimpleDateFormat();
            String mask;
            if (Const.isEmpty(conversionMask))
            {
                mask = DEFAULT_DATE_FORMAT_MASK;
            }
            else
            {
                mask = conversionMask;
            }
            
            if (dateFormatLocale==null || dateFormatLocale.equals(Locale.getDefault()))
            {
                dateFormat = new SimpleDateFormat(mask);
            }
            else
            {
                dateFormat = new SimpleDateFormat(mask, dateFormatLocale);
            }
            
            dateFormatChanged=false;
        }
        return dateFormat;
    }

    public synchronized DecimalFormat getDecimalFormat()
    {
    	// If we have an Integer that is represented as a String
    	// In that case we can set the format of the original Integer on the String value metadata in the form of a storage metadata object.
    	// That way, we can always convert from Integer to String and back without a problem, no matter how complex the format was.
    	// As such, we should return the decimal format of the storage metadata.
    	//
    	if (storageMetadata!=null) {
    		return storageMetadata.getDecimalFormat();
    	}
    	
    	// Calculate the decimal format as few times as possible.
    	// That is because creating or changing a DecimalFormat object is very CPU hungry.
    	//
        if (decimalFormat==null || decimalFormatChanged)
        {
            decimalFormat        = (DecimalFormat)NumberFormat.getInstance();
            DecimalFormatSymbols decimalFormatSymbols = decimalFormat.getDecimalFormatSymbols();
        
            if (!Const.isEmpty(currencySymbol)) decimalFormatSymbols.setCurrencySymbol( currencySymbol );
            if (!Const.isEmpty(groupingSymbol)) decimalFormatSymbols.setGroupingSeparator( groupingSymbol.charAt(0) );
            if (!Const.isEmpty(decimalSymbol)) decimalFormatSymbols.setDecimalSeparator( decimalSymbol.charAt(0) );
            decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
            
            // Apply the conversion mask if we have one...
            //
            if (!Const.isEmpty(conversionMask)) {
            	decimalFormat.applyPattern(conversionMask);
            }
            else {
            	switch(type) {
            	case TYPE_INTEGER:
	            	{
		            	if (length<1) {
		            		decimalFormat.applyPattern(" ###############0;-###############0"); // Same as before version 3.0
		            	}
		            	else {
		    				StringBuffer integerPattern=new StringBuffer();
		    				
		    				// First the format for positive integers...
		    				//
		    				integerPattern.append(" ");
		    				for (int i=0;i<getLength();i++) integerPattern.append('0'); // all zeroes.
		    				integerPattern.append(";");
		    				
		    				// Then the format for the negative numbers...
		    				//
		    				integerPattern.append("-");
		    				for (int i=0;i<getLength();i++) integerPattern.append('0'); // all zeroes.
		    				decimalFormat.applyPattern(integerPattern.toString());
		            	}
	            	}
	            	break;
            	case TYPE_NUMBER:
	            	{
	            		if (length<1) {
	            			decimalFormat.applyPattern(" ##########0.0########;-#########0.0########");
	            		}
	            		else {
	    					StringBuffer numberPattern=new StringBuffer();

	    					// First do the format for positive numbers...
	    					//
	    					numberPattern.append(' '); // to compensate for minus sign.
	    					if (precision<0)  // Default: two decimals
	    					{
	    						for (int i=0;i<length;i++) numberPattern.append('0');
	    						numberPattern.append(".00"); // for the .00
	    					}
	    					else  // Floating point format   00001234,56  --> (12,2)
	    					{
	    						for (int i=0;i<=length;i++) numberPattern.append('0'); // all zeroes.
	    						int pos = length-precision+1;
	    						if (pos>=0 && pos <numberPattern.length())
	    						{
	    							numberPattern.setCharAt(length-precision+1, '.'); // one 'comma'
	    						}
	    					}

	    					// Now do the format for negative numbers...
	    					//
	    					StringBuffer negativePattern = new StringBuffer(numberPattern);
	    					negativePattern.setCharAt(0, '-');

	    					numberPattern.append(";");
	    					numberPattern.append(negativePattern);
	    					
	    					// Apply the pattern...
	    					//
	    					decimalFormat.applyPattern(numberPattern.toString());
	            		}
	            	}
            	}

            }
            
            decimalFormatChanged=false;
        }
        return decimalFormat;
    }
   
    private synchronized String convertIntegerToString(Long integer) throws KettleValueException
    {
        if (integer==null) {
        	if (!outputPaddingEnabled || length<1) {
        		return null;
        	}
        	else {
        		// Return strings padded to the specified length...
        		// This is done for backward compatibility with 2.5.x 
        		// We just optimized this a bit...
        		//
        		String[] emptyPaddedStrings = Const.getEmptyPaddedStrings();
        		if (length<emptyPaddedStrings.length) {
        			return emptyPaddedStrings[length];
        		}
        		else {
        			return Const.rightPad("", length);
        		}
        	}
        }

        try
        {
            return getDecimalFormat().format(integer);
        }
        catch(Exception e)
        {
            throw new KettleValueException(toString()+" : couldn't convert Long to String ", e);
        }
    }
    
    private synchronized Long convertStringToInteger(String string) throws KettleValueException
    {
        if (Const.isEmpty(string)) return null;
        
        string = trim(string); // see if  trimming needs to be performed before conversion

        try
        {
        	return new Long( getDecimalFormat().parse(string).longValue() );
        }
        catch(Exception e)
        {
            throw new KettleValueException(toString()+" : couldn't convert String to Integer", e);
        }
    }
    
    private synchronized String convertBigNumberToString(BigDecimal number) throws KettleValueException
    {
        if (number==null) return null;

        String string = number.toString();
        /*
        if ( !Const.isEmpty(decimalSymbol) && !".".equalsIgnoreCase(decimalSymbol) )
        {
            string = Const.replace(string, ".", decimalSymbol.substring(0, 1));
        }
        */
        return string;
    }
    
    private synchronized BigDecimal convertStringToBigNumber(String string) throws KettleValueException
    {
        if (Const.isEmpty(string)) return null;

        string = trim(string); // see if  trimming needs to be performed before conversion

        /*
        if (!".".equalsIgnoreCase(decimalSymbol))
        {
            string = Const.replace(string, decimalSymbol.substring(0, 1), ".");
        }
        */
        
        return new BigDecimal( string );
    }

    // BOOLEAN + STRING
    
    private String convertBooleanToString(Boolean bool)
    {
        if (length>=3)
        {
            return bool.booleanValue()?"true":"false";
        }
        else
        {
            return bool.booleanValue()?"Y":"N";
        }
    }
    
    public static Boolean convertStringToBoolean(String string)
    {
        return Boolean.valueOf( "Y".equalsIgnoreCase(string) || "TRUE".equalsIgnoreCase(string) || "YES".equalsIgnoreCase(string) || "1".equals(string) );
    }
    
    // BOOLEAN + NUMBER
    
    private Double convertBooleanToNumber(Boolean bool)
    {
        return new Double( bool.booleanValue() ? 1.0 : 0.0 );
    }
    
    private Boolean convertNumberToBoolean(Double number)
    {
        return Boolean.valueOf( number.intValue() != 0 );
    }

    // BOOLEAN + INTEGER

    private Long convertBooleanToInteger(Boolean bool)
    {
        return Long.valueOf( bool.booleanValue() ? 1L : 0L );
    }

    private Boolean convertIntegerToBoolean(Long number)
    {
        return Boolean.valueOf( number.longValue() != 0 );
    }
    
    // BOOLEAN + BIGNUMBER
    
    private BigDecimal convertBooleanToBigNumber(Boolean bool)
    {
        return new BigDecimal( bool.booleanValue() ? 1.0 : 0.0 );
    }
    
    private Boolean convertBigNumberToBoolean(BigDecimal number)
    {
        return Boolean.valueOf( number.intValue() != 0 );
    }    
    
    private String convertBinaryStringToString(byte[] binary) throws KettleValueException
    {
        // OK, so we have an internal representation of the original object, read from file.
        // Before we release it back, we have to see if we don't have to do a String-<type>-String 
        // conversion with different masks.
        // This obviously only applies to numeric data and dates.
        // We verify if this is true or false in advance for performance reasons
        //
    	if (binary==null) return null;
    	
        if (identicalFormat) {
        	String string;
            if (Const.isEmpty(stringEncoding))
            {
                string = new String(binary);
            }
            else
            {
                try
                {
                    string = new String(binary, stringEncoding);
                }
                catch(UnsupportedEncodingException e)
                {
                    throw new KettleValueException(toString()+" : couldn't convert binary value to String with specified string encoding ["+stringEncoding+"]", e);
                }
            }

        	return string;
        }
        else {
        	// Do 2 conversions in one go.
        	// 
        	// First convert from the binary format to the current data type...
        	//
        	Object nativeType = convertData(storageMetadata, binary);
        	
        	if (nativeMetadata==null) {
        		nativeMetadata = this.clone();
        		nativeMetadata.setStorageType(STORAGE_TYPE_NORMAL);
        	}
        	
        	// Then convert it back to string in the correct layout...
        	//
        	if (stringMetadata==null) {
        		// storageMetadata thinks it's in binary[] format, so we need to change this somehow.
        		// We cache this conversion metadata for re-use..
        		//
        		stringMetadata = storageMetadata.clone();
        		stringMetadata.setStorageType(STORAGE_TYPE_NORMAL);
        	}
        	return (String)stringMetadata.convertData(nativeMetadata, nativeType);
        }
    }

    private byte[] convertStringToBinaryString(String string) throws KettleValueException
    {
        if (Const.isEmpty(stringEncoding))
        {
            return string.getBytes();
        }
        else
        {
            try
            {
                return string.getBytes(stringEncoding);
            }
            catch(UnsupportedEncodingException e)
            {
                throw new KettleValueException(toString()+" : couldn't convert String to Binary with specified string encoding ["+stringEncoding+"]", e);
            }
        }
    }
    
    /**
     * Clones the data.  Normally, we don't have to do anything here, but just for arguments and safety, 
     * we do a little extra work in case of binary blobs and Date objects.
     * We should write a programmers manual later on to specify in all clarity that 
     * "we always overwrite/replace values in the Object[] data rows, we never modify them".
     * 
     * @return a cloned data object if needed
     */
    public Object cloneValueData(Object object) throws KettleValueException
    {
        if (object==null) return null;
        
        if (storageType==STORAGE_TYPE_NORMAL)
        {
            switch(getType())
            {
            case ValueMeta.TYPE_STRING: 
            case ValueMeta.TYPE_NUMBER: 
            case ValueMeta.TYPE_INTEGER: 
            case ValueMeta.TYPE_BOOLEAN:
            case ValueMeta.TYPE_BIGNUMBER: // primitive data types: we can only overwrite these, not change them
                return object;

            case ValueMeta.TYPE_DATE:
                return new Date( ((Date)object).getTime() ); // just to make sure: very inexpensive too.

            case ValueMeta.TYPE_BINARY:
                byte[] origin = (byte[]) object;
                byte[] target = new byte[origin.length];
                System.arraycopy(origin, 0, target, 0, origin.length);
                return target;

            default: throw new KettleValueException(toString()+": unable to make copy of value type: "+getType());
            }
        }
        else {
        	
        	return object;
        	
        }
    }

    public String getString(Object object) throws KettleValueException
    {
        try
        {
            String string;
            
            switch(type)
            {
            case TYPE_STRING:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = (String)object; break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : (String) index[((Integer)object).intValue()];  break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                string = trim(string);
                break;
                
            case TYPE_DATE:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = convertDateToString((Date)object); break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : convertDateToString((Date)index[((Integer)object).intValue()]); break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            case TYPE_NUMBER:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = convertNumberToString((Double)object); break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : convertNumberToString((Double)index[((Integer)object).intValue()]); break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            case TYPE_INTEGER:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = convertIntegerToString((Long)object); break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : convertIntegerToString((Long)index[((Integer)object).intValue()]); break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            case TYPE_BIGNUMBER:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = convertBigNumberToString((BigDecimal)object); break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : convertBigNumberToString((BigDecimal)index[((Integer)object).intValue()]); break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            case TYPE_BOOLEAN:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = convertBooleanToString((Boolean)object); break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : convertBooleanToString((Boolean)index[((Integer)object).intValue()]); break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            case TYPE_BINARY:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : convertBinaryStringToString((byte[])index[((Integer)object).intValue()]); break;
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            case TYPE_SERIALIZABLE:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         string = object.toString();  break; // just go for the default toString()
                case STORAGE_TYPE_BINARY_STRING:  string = convertBinaryStringToString((byte[])object); break;
                case STORAGE_TYPE_INDEXED:        string = object==null ? null : index[((Integer)object).intValue()].toString();  break; // just go for the default toString()
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                break;
    
            default: 
                throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
            }
            
            if (isOutputPaddingEnabled() && getLength()>0)
            {
                string = ValueDataUtil.rightPad(string, getLength());
            }

            return string;
        }
        catch(ClassCastException e)
        {
            throw new KettleValueException(toString()+" : There was a data type error: the data type of "+object.getClass().getName()+" object ["+object+"] does not correspond to value meta ["+toStringMeta()+"]");
        }
    }

    private String trim(String string) {
        switch(getTrimType()) {
        case TRIM_TYPE_NONE : break;
        case TRIM_TYPE_RIGHT : string = ValueDataUtil.rightTrim(string); break;
        case TRIM_TYPE_LEFT  : string = ValueDataUtil.leftTrim(string); break;
        case TRIM_TYPE_BOTH  : string = ValueDataUtil.trim(string); break;
        default: break;
        }
        return string;
	}

	public Double getNumber(Object object) throws KettleValueException
    {
        if (object==null) // NULL 
        {
            return null;
        }
        switch(type)
        {
        case TYPE_NUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return (Double)object;
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToNumber(convertBinaryStringToString((byte[])object));
            case STORAGE_TYPE_INDEXED:        return (Double)index[((Integer)object).intValue()];
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_STRING:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertStringToNumber((String)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToNumber(convertBinaryStringToString((byte[])object));
            case STORAGE_TYPE_INDEXED:        return convertStringToNumber((String) index[((Integer)object).intValue()]); 
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_DATE:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertDateToNumber((Date)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertDateToNumber(convertStringToDate(convertBinaryStringToString((byte[])object)));
            case STORAGE_TYPE_INDEXED:        return new Double( ((Date)index[((Integer)object).intValue()]).getTime() );  
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_INTEGER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return new Double( ((Long)object).doubleValue() );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToNumber(convertBinaryStringToString((byte[])object)).doubleValue();
            case STORAGE_TYPE_INDEXED:        return new Double( ((Long)index[((Integer)object).intValue()]).doubleValue() );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BIGNUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return new Double( ((BigDecimal)object).doubleValue() );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToBigNumber(convertBinaryStringToString((byte[])object)).doubleValue();
            case STORAGE_TYPE_INDEXED:        return new Double( ((BigDecimal)index[((Integer)object).intValue()]).doubleValue() );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BOOLEAN:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertBooleanToNumber( (Boolean)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertBooleanToNumber( convertStringToBoolean(convertBinaryStringToString((byte[])object)) );
            case STORAGE_TYPE_INDEXED:        return convertBooleanToNumber( (Boolean)index[((Integer)object).intValue()] );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BINARY:
            throw new KettleValueException(toString()+" : I don't know how to convert binary values to numbers.");
        case TYPE_SERIALIZABLE:
            throw new KettleValueException(toString()+" : I don't know how to convert serializable values to numbers.");
        default:
            throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
        }
    }

    public Long getInteger(Object object) throws KettleValueException
    {
        if (object==null) // NULL 
        {
            return null;
        }
        switch(type)
        {
        case TYPE_INTEGER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return (Long)object;
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToInteger(convertBinaryStringToString((byte[])object));
            case STORAGE_TYPE_INDEXED:        return (Long)index[((Integer)object).intValue()];
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_STRING:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertStringToInteger((String)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToInteger(convertBinaryStringToString((byte[])object));
            case STORAGE_TYPE_INDEXED:        return convertStringToInteger((String) index[((Integer)object).intValue()]); 
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_NUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return new Long( ((Double)object).longValue() );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToNumber(convertBinaryStringToString((byte[])object)).longValue();
            case STORAGE_TYPE_INDEXED:        return new Long( ((Double)index[((Integer)object).intValue()]).longValue() );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_DATE:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertDateToInteger( (Date)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToDate(convertBinaryStringToString((byte[])object)).getTime();
            case STORAGE_TYPE_INDEXED:        return convertDateToInteger( (Date)index[((Integer)object).intValue()]);  
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BIGNUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return new Long( ((BigDecimal)object).longValue() );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToBigNumber(convertBinaryStringToString((byte[])object)).longValue();
            case STORAGE_TYPE_INDEXED:        return new Long( ((BigDecimal)index[((Integer)object).intValue()]).longValue() );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BOOLEAN:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertBooleanToInteger( (Boolean)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertBooleanToInteger( convertStringToBoolean(convertBinaryStringToString((byte[])object)) );
            case STORAGE_TYPE_INDEXED:        return convertBooleanToInteger( (Boolean)index[((Integer)object).intValue()] );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BINARY:
            throw new KettleValueException(toString()+" : I don't know how to convert binary values to integers.");
        case TYPE_SERIALIZABLE:
            throw new KettleValueException(toString()+" : I don't know how to convert serializable values to integers.");
        default:
            throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
        }
    }

    public BigDecimal getBigNumber(Object object) throws KettleValueException
    {
        if (object==null) // NULL 
        {
            return null;
        }
        switch(type)
        {
        case TYPE_BIGNUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return (BigDecimal)object;
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToBigNumber(convertBinaryStringToString((byte[])object));
            case STORAGE_TYPE_INDEXED:        return (BigDecimal)index[((Integer)object).intValue()];
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_STRING:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertStringToBigNumber( (String)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToBigNumber( convertBinaryStringToString((byte[])object) );
            case STORAGE_TYPE_INDEXED:        return convertStringToBigNumber((String) index[((Integer)object).intValue()]); 
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_INTEGER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return new BigDecimal( ((Long)object).doubleValue() );
            case STORAGE_TYPE_BINARY_STRING:  return new BigDecimal( convertStringToInteger( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return new BigDecimal( ((Long)index[((Integer)object).intValue()]).doubleValue() );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_NUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return new BigDecimal( ((Double)object).doubleValue() );
            case STORAGE_TYPE_BINARY_STRING:  return new BigDecimal( convertStringToNumber( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return new BigDecimal( ((Double)index[((Integer)object).intValue()]).doubleValue() );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_DATE:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertDateToBigNumber( (Date)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertDateToBigNumber( convertStringToDate( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertDateToBigNumber( (Date)index[((Integer)object).intValue()] );  
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BOOLEAN:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertBooleanToBigNumber( (Boolean)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertBooleanToBigNumber( convertStringToBoolean( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertBooleanToBigNumber( (Boolean)index[((Integer)object).intValue()] );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BINARY:
            throw new KettleValueException(toString()+" : I don't know how to convert binary values to integers.");
        case TYPE_SERIALIZABLE:
            throw new KettleValueException(toString()+" : I don't know how to convert serializable values to integers.");
        default:
            throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
        }
    }
    
    public Boolean getBoolean(Object object) throws KettleValueException
    {
        if (object==null) // NULL 
        {
            return null;
        }
        switch(type)
        {
        case TYPE_BOOLEAN:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return (Boolean)object;
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToBoolean( convertBinaryStringToString((byte[])object) );
            case STORAGE_TYPE_INDEXED:        return (Boolean)index[((Integer)object).intValue()];
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_STRING:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertStringToBoolean( trim((String)object) );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToBoolean( convertBinaryStringToString((byte[])object) );
            case STORAGE_TYPE_INDEXED:        return convertStringToBoolean( trim((String) index[((Integer)object).intValue()] )); 
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_INTEGER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertIntegerToBoolean( (Long)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertIntegerToBoolean( convertStringToInteger( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertIntegerToBoolean( (Long)index[((Integer)object).intValue()] );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_NUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertNumberToBoolean( (Double)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertNumberToBoolean( convertStringToNumber( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertNumberToBoolean( (Double)index[((Integer)object).intValue()] );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BIGNUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertBigNumberToBoolean( (BigDecimal)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertBigNumberToBoolean( convertStringToBigNumber( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertBigNumberToBoolean( (BigDecimal)index[((Integer)object).intValue()] );
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_DATE:
            throw new KettleValueException(toString()+" : I don't know how to convert date values to booleans.");
        case TYPE_BINARY:
            throw new KettleValueException(toString()+" : I don't know how to convert binary values to booleans.");
        case TYPE_SERIALIZABLE:
            throw new KettleValueException(toString()+" : I don't know how to convert serializable values to booleans.");
        default:
            throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
        }
    }
    
    public Date getDate(Object object) throws KettleValueException
    {
        if (object==null) // NULL 
        {
            return null;
        }
        switch(type)
        {
        case TYPE_DATE:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return (Date)object;
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToDate( convertBinaryStringToString((byte[])object) );
            case STORAGE_TYPE_INDEXED:        return (Date)index[((Integer)object).intValue()];  
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_STRING:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertStringToDate( (String)object );
            case STORAGE_TYPE_BINARY_STRING:  return convertStringToDate( convertBinaryStringToString((byte[])object) );
            case STORAGE_TYPE_INDEXED:        return convertStringToDate( (String) index[((Integer)object).intValue()] ); 
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_NUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertNumberToDate((Double)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertNumberToDate( convertStringToNumber( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertNumberToDate((Double)index[((Integer)object).intValue()]);
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_INTEGER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertIntegerToDate((Long)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertIntegerToDate( convertStringToInteger( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertIntegerToDate((Long)index[((Integer)object).intValue()]);
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BIGNUMBER:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertBigNumberToDate((BigDecimal)object);
            case STORAGE_TYPE_BINARY_STRING:  return convertBigNumberToDate( convertStringToBigNumber( convertBinaryStringToString((byte[])object) ) );
            case STORAGE_TYPE_INDEXED:        return convertBigNumberToDate((BigDecimal)index[((Integer)object).intValue()]);
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_BOOLEAN:
            throw new KettleValueException(toString()+" : I don't know how to convert a boolean to a date.");
        case TYPE_BINARY:
            throw new KettleValueException(toString()+" : I don't know how to convert a binary value to date.");
        case TYPE_SERIALIZABLE:
            throw new KettleValueException(toString()+" : I don't know how to convert a serializable value to date.");
            
        default: 
            throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
        }
    }

    public byte[] getBinary(Object object) throws KettleValueException
    {
        if (object==null) // NULL 
        {
            return null;
        }
        switch(type)
        {
        case TYPE_BINARY:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return (byte[])object;
            case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
            case STORAGE_TYPE_INDEXED:        return (byte[])index[((Integer)object).intValue()];  
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_DATE:
            throw new KettleValueException(toString()+" : I don't know how to convert a date to binary.");
        case TYPE_STRING:
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString( (String)object );
            case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
            case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString( (String) index[((Integer)object).intValue()] ); 
            default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
            }
        case TYPE_NUMBER:
            throw new KettleValueException(toString()+" : I don't know how to convert a number to binary.");
        case TYPE_INTEGER:
            throw new KettleValueException(toString()+" : I don't know how to convert an integer to binary.");
        case TYPE_BIGNUMBER:
            throw new KettleValueException(toString()+" : I don't know how to convert a bignumber to binary.");
        case TYPE_BOOLEAN:
            throw new KettleValueException(toString()+" : I don't know how to convert a boolean to binary.");
        case TYPE_SERIALIZABLE:
            throw new KettleValueException(toString()+" : I don't know how to convert a serializable to binary.");
            
        default: 
            throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
        }
    }
    
    public byte[] getBinaryString(Object object) throws KettleValueException
    {
        try
        {
            if (object==null) // NULL 
            {
                return null;
            }
            
            switch(type)
            {
            case TYPE_STRING:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString((String)object);
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString((String) index[((Integer)object).intValue()]);
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
                
            case TYPE_DATE:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString(convertDateToString((Date)object));
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString(convertDateToString((Date)index[((Integer)object).intValue()]));
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            case TYPE_NUMBER:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString(convertNumberToString((Double)object));
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString(convertNumberToString((Double)index[((Integer)object).intValue()]));
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            case TYPE_INTEGER:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString(convertIntegerToString((Long)object));
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString(convertIntegerToString((Long)index[((Integer)object).intValue()]));
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            case TYPE_BIGNUMBER:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString(convertBigNumberToString((BigDecimal)object));
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString(convertBigNumberToString((BigDecimal)index[((Integer)object).intValue()]));
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            case TYPE_BOOLEAN:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString(convertBooleanToString((Boolean)object));
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString(convertBooleanToString((Boolean)index[((Integer)object).intValue()]));
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            case TYPE_BINARY:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return (byte[])object;
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return (byte[])index[((Integer)object).intValue()];
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            case TYPE_SERIALIZABLE:
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:         return convertStringToBinaryString(object.toString());
                case STORAGE_TYPE_BINARY_STRING:  return (byte[])object;
                case STORAGE_TYPE_INDEXED:        return convertStringToBinaryString( index[((Integer)object).intValue()].toString() );
                default: throw new KettleValueException(toString()+" : Unknown storage type "+storageType+" specified.");
                }
    
            default: 
                throw new KettleValueException(toString()+" : Unknown type "+type+" specified.");
            }
        }
        catch(ClassCastException e)
        {
            throw new KettleValueException(toString()+" : There was a data type error: the data type of "+object.getClass().getName()+" object ["+object+"] does not correspond to value meta ["+toStringMeta()+"]");
        }
    }
    
    
    /**
     * Checks wheter or not the value is a String.
     * @return true if the value is a String.
     */
    public boolean isString()
    {
        return type==TYPE_STRING;
    }

    /**
     * Checks whether or not this value is a Date
     * @return true if the value is a Date
     */
    public boolean isDate()
    {
        return type==TYPE_DATE;
    }

    /**
     * Checks whether or not the value is a Big Number
     * @return true is this value is a big number
     */
    public boolean isBigNumber()
    {
        return type==TYPE_BIGNUMBER;
    }

    /**
     * Checks whether or not the value is a Number
     * @return true is this value is a number
     */
    public boolean isNumber()
    {
        return type==TYPE_NUMBER;
    }

    /**
     * Checks whether or not this value is a boolean
     * @return true if this value has type boolean.
     */
    public boolean isBoolean()
    {
        return type==TYPE_BOOLEAN;
    }

    /**
     * Checks whether or not this value is of type Serializable
     * @return true if this value has type Serializable
     */
    public boolean isSerializableType() {
        return type == TYPE_SERIALIZABLE;
    }

    /**
     * Checks whether or not this value is of type Binary
     * @return true if this value has type Binary
     */
    public boolean isBinary() {
        return type == TYPE_BINARY;
    }   
    
    /**
     * Checks whether or not this value is an Integer
     * @return true if this value is an integer
     */
    public boolean isInteger()
    {
        return type==TYPE_INTEGER;
    }

    /**
     * Checks whether or not this Value is Numeric
     * A Value is numeric if it is either of type Number or Integer
     * @return true if the value is either of type Number or Integer
     */
    public boolean isNumeric()
    {
        return isInteger() || isNumber() || isBigNumber();
    }
    
    /**
     * Checks whether or not the specified type is either Integer or Number
     * @param t the type to check
     * @return true if the type is Integer or Number
     */
    public static final boolean isNumeric(int t)
    {
        return t==TYPE_INTEGER || t==TYPE_NUMBER || t==TYPE_BIGNUMBER;
    }
    
    public boolean isSortedAscending()
    {
        return !isSortedDescending();
    }
    
    /**
     * Return the type of a value in a textual form: "String", "Number", "Integer", "Boolean", "Date", ...
     * @return A String describing the type of value.
     */
    public String getTypeDesc()
    {
        return typeCodes[type];
    }

    /**
     * Return the storage type of a value in a textual form: "normal", "binary-string", "indexes"
     * @return A String describing the storage type of the value metadata
     */
    public String getStorageTypeDesc()
    {
        return storageTypeCodes[type];
    }


    public String toString()
    {
        return name+" "+toStringMeta();
    }

    
    /**
     * a String text representation of this Value, optionally padded to the specified length
     * @return a String text representation of this Value, optionally padded to the specified length
     */
    public String toStringMeta()
    {
        // We (Sven Boden) did explicit performance testing for this
        // part. The original version used Strings instead of StringBuffers,
        // performance between the 2 does not differ that much. A few milliseconds
        // on 100000 iterations in the advantage of StringBuffers. The
        // lessened creation of objects may be worth it in the long run.
        StringBuffer retval=new StringBuffer(getTypeDesc());

        switch(getType())
        {
        case TYPE_STRING :
            if (getLength()>0) retval.append('(').append(getLength()).append(')');  
            break;
        case TYPE_NUMBER :
        case TYPE_BIGNUMBER :
            if (getLength()>0)
            {
                retval.append('(').append(getLength());
                if (getPrecision()>0)
                {
                    retval.append(", ").append(getPrecision());
                }
                retval.append(')');
            }
            break;
        case TYPE_INTEGER:
            if (getLength()>0)
            {
                retval.append('(').append(getLength()).append(')');
            }
            break;
        default: break;
        }

        return retval.toString();
    }

    public void writeData(DataOutputStream outputStream, Object object) throws KettleFileException
    {
        try
        {
            // Is the value NULL?
            outputStream.writeBoolean(object==null);

            if (object!=null) // otherwise there is no point
            {
                switch(storageType)
                {
                case STORAGE_TYPE_NORMAL:
                    // Handle Content -- only when not NULL
                    switch(getType())
                    {
                    case TYPE_STRING     : writeString(outputStream, (String)object); break;
                    case TYPE_NUMBER     : writeNumber(outputStream, (Double)object); break;
                    case TYPE_INTEGER    : writeInteger(outputStream, (Long)object); break;
                    case TYPE_DATE       : writeDate(outputStream, (Date)object); break;
                    case TYPE_BIGNUMBER  : writeBigNumber(outputStream, (BigDecimal)object); break;
                    case TYPE_BOOLEAN    : writeBoolean(outputStream, (Boolean)object); break;
                    case TYPE_BINARY     : writeBinary(outputStream, (byte[])object); break;
                    default: throw new KettleFileException(toString()+" : Unable to serialize data type "+getType());
                    }
                    break;
                    
                case STORAGE_TYPE_BINARY_STRING:
                    // Handle binary string content -- only when not NULL
                	// In this case, we opt not to convert anything at all for speed.
                	// That way, we can save on CPU power.
                	// Since the streams can be compressed, volume shouldn't be an issue at all.
                	//
                	writeBinaryString(outputStream, (byte[])object);
                    break;
                    
                case STORAGE_TYPE_INDEXED:
                    writeInteger(outputStream, (Integer)object); // just an index 
                    break;
                    
                default: throw new KettleFileException(toString()+" : Unknown storage type "+getStorageType());
                }
            }
        }
        catch(IOException e)
        {
            throw new KettleFileException(toString()+" : Unable to write value data to output stream", e);
        }
        
    }
    
    public Object readData(DataInputStream inputStream) throws KettleFileException, KettleEOFException, SocketTimeoutException
    {
        try
        {
            // Is the value NULL?
            if (inputStream.readBoolean()) return null; // done

            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:
                // Handle Content -- only when not NULL
                switch(getType())
                {
                case TYPE_STRING     : return readString(inputStream);
                case TYPE_NUMBER     : return readNumber(inputStream);
                case TYPE_INTEGER    : return readInteger(inputStream);
                case TYPE_DATE       : return readDate(inputStream);
                case TYPE_BIGNUMBER  : return readBigNumber(inputStream);
                case TYPE_BOOLEAN    : return readBoolean(inputStream);
                case TYPE_BINARY     : return readBinary(inputStream);
                default: throw new KettleFileException(toString()+" : Unable to de-serialize data of type "+getType());
                }
            
            case STORAGE_TYPE_BINARY_STRING:
                return readBinaryString(inputStream);
                
            case STORAGE_TYPE_INDEXED:
                return readSmallInteger(inputStream); // just an index: 4-bytes should be enough.
                
            default: throw new KettleFileException(toString()+" : Unknown storage type "+getStorageType());
            }
        }
        catch(EOFException e)
        {
        	throw new KettleEOFException(e);
        }
        catch(SocketTimeoutException e)
        {
        	throw e;
        }
        catch(IOException e)
        {
            throw new KettleFileException(toString()+" : Unable to read value data from input stream", e);
        }
    }

    
    private void writeString(DataOutputStream outputStream, String string) throws IOException
    {
        // Write the length and then the bytes
        if (string==null)
        {
            outputStream.writeInt(-1);
        }
        else
        {
            byte[] chars = string.getBytes(Const.XML_ENCODING);
            outputStream.writeInt(chars.length);
            outputStream.write(chars);
        }
    }
    
    private void writeBinaryString(DataOutputStream outputStream, byte[] binaryString) throws IOException
    {
        // Write the length and then the bytes
        if (binaryString==null)
        {
            outputStream.writeInt(-1);
        }
        else
        {
            outputStream.writeInt(binaryString.length);
            outputStream.write(binaryString);
        }
    }

    private String readString(DataInputStream inputStream) throws IOException
    {
        // Read the length and then the bytes
        int length = inputStream.readInt();
        if (length<0) 
        {
            return null;
        }
        
        byte[] chars = new byte[length];
        inputStream.readFully(chars);

        String string = new String(chars, Const.XML_ENCODING);         
        // System.out.println("Read string("+getName()+"), length "+length+": "+string);
        return string;
    }
    
    private byte[] readBinaryString(DataInputStream inputStream) throws IOException
    {
        // Read the length and then the bytes
        int length = inputStream.readInt();
        if (length<0) 
        {
            return null;
        }
        
        byte[] chars = new byte[length];
        inputStream.readFully(chars);

        return chars;
    }

    private void writeBigNumber(DataOutputStream outputStream, BigDecimal number) throws IOException
    {
        String string = number.toString();
        writeString(outputStream, string);
    }

    private BigDecimal readBigNumber(DataInputStream inputStream) throws IOException
    {
        String string = readString(inputStream);
        // System.out.println("Read big number("+getName()+") ["+string+"]");
        return new BigDecimal(string);
    }

    private void writeDate(DataOutputStream outputStream, Date date) throws IOException
    {
        outputStream.writeLong(date.getTime());
    }
    
    private Date readDate(DataInputStream inputStream) throws IOException
    {
        long time = inputStream.readLong();
        // System.out.println("Read Date("+getName()+") ["+new Date(time)+"]");
        return new Date(time);
    }

    private void writeBoolean(DataOutputStream outputStream, Boolean bool) throws IOException
    {
        outputStream.writeBoolean(bool.booleanValue());
    }
    
    private Boolean readBoolean(DataInputStream inputStream) throws IOException
    {
        Boolean bool = Boolean.valueOf( inputStream.readBoolean() );
        // System.out.println("Read boolean("+getName()+") ["+bool+"]");
        return bool;
    }
    
    private void writeNumber(DataOutputStream outputStream, Double number) throws IOException
    {
        outputStream.writeDouble(number.doubleValue());
    }

    private Double readNumber(DataInputStream inputStream) throws IOException
    {
        Double d = new Double( inputStream.readDouble() );
        // System.out.println("Read number("+getName()+") ["+d+"]");
        return d;
    }

    private void writeInteger(DataOutputStream outputStream, Long number) throws IOException
    {
        outputStream.writeLong(number.longValue());
    }

    private Long readInteger(DataInputStream inputStream) throws IOException
    {
        Long l = new Long( inputStream.readLong() );
        // System.out.println("Read integer("+getName()+") ["+l+"]");
        return l;
    }

    private void writeInteger(DataOutputStream outputStream, Integer number) throws IOException
    {
        outputStream.writeInt(number.intValue());
    }
    
    private Integer readSmallInteger(DataInputStream inputStream) throws IOException
    {
        Integer i = Integer.valueOf( inputStream.readInt() );
        // System.out.println("Read index integer("+getName()+") ["+i+"]");
        return i;
    }
    
    private void writeBinary(DataOutputStream outputStream, byte[] binary) throws IOException
    {
        outputStream.writeInt(binary.length);
        outputStream.write(binary);
    }
    
    private byte[] readBinary(DataInputStream inputStream) throws IOException
    {
        int size = inputStream.readInt();
        byte[] buffer = new byte[size];
        inputStream.readFully(buffer);
        
        // System.out.println("Read binary("+getName()+") with size="+size);

        return buffer;
    }


    public void writeMeta(DataOutputStream outputStream) throws KettleFileException
    {
        try
        {
            int type=getType();
    
            // Handle type
            outputStream.writeInt(type);
            
            // Handle storage type
            outputStream.writeInt(storageType);
            
            switch(storageType) {
            case STORAGE_TYPE_INDEXED:
	            {
	                // Save the indexed strings...
	                if (index==null)
	                {
	                    outputStream.writeInt(-1); // null
	                }
	                else
	                {
	                    outputStream.writeInt(index.length);
	                    for (int i=0;i<index.length;i++)
	                    {
	                        switch(type)
	                        {
	                        case TYPE_STRING:    writeString(outputStream, (String)index[i]); break; 
	                        case TYPE_NUMBER:    writeNumber(outputStream, (Double)index[i]); break; 
	                        case TYPE_INTEGER:   writeInteger(outputStream, (Long)index[i]); break; 
	                        case TYPE_DATE:      writeDate(outputStream, (Date)index[i]); break; 
	                        case TYPE_BIGNUMBER: writeBigNumber(outputStream, (BigDecimal)index[i]); break; 
	                        case TYPE_BOOLEAN:   writeBoolean(outputStream, (Boolean)index[i]); break; 
	                        case TYPE_BINARY:    writeBinary(outputStream, (byte[])index[i]); break;
	                        default: throw new KettleFileException(toString()+" : Unable to serialize indexe storage type for data type "+getType());
	                        }
	                    }
	                }
	            }
	            break;
	        
            case STORAGE_TYPE_BINARY_STRING:
	            {
	            	// Save the storage meta data...
	            	//
	            	outputStream.writeBoolean(storageMetadata!=null);
	            	
	            	if (storageMetadata!=null) {
	            		storageMetadata.writeMeta(outputStream);
	            	}
	            }
	            break;
	            
	       default:
	    	   break;
           }
            
            // Handle name-length
            writeString(outputStream, name);  
            
            // length & precision
            outputStream.writeInt(getLength());
            outputStream.writeInt(getPrecision());

            // Origin
            writeString(outputStream, origin);

            // Comments
            writeString(outputStream, comments);
            
            // formatting Mask, decimal, grouping, currency
            writeString(outputStream, conversionMask);
            writeString(outputStream, decimalSymbol);
            writeString(outputStream, groupingSymbol);
            writeString(outputStream, currencySymbol);
            outputStream.writeInt(trimType);
            
            // Case sensitivity of compare
            outputStream.writeBoolean(caseInsensitive);  
            
            // Sorting information
            outputStream.writeBoolean(sortedDescending); 

            // Padding information
            outputStream.writeBoolean(outputPaddingEnabled); 
            
            // date format lenient?
            outputStream.writeBoolean(dateFormatLenient);
            
            // date format locale?
            writeString(outputStream, dateFormatLocale!=null ? dateFormatLocale.toString() : null);
            
        }
        catch(IOException e)
        {
            throw new KettleFileException(toString()+" : Unable to write value metadata to output stream", e);
        }
    }
    
    public ValueMeta(DataInputStream inputStream) throws KettleFileException, KettleEOFException
    {
        this();
        
        try
        {
            // Handle type
            type=inputStream.readInt();
    
            // Handle storage type
            storageType = inputStream.readInt();
            
            // Read the data in the index
            switch(storageType) {
            case STORAGE_TYPE_INDEXED:
	            {
	                int indexSize = inputStream.readInt();
	                if (indexSize<0)
	                {
	                    index=null;
	                }
	                else
	                {
	                    index=new Object[indexSize];
	                    for (int i=0;i<indexSize;i++)
	                    {
	                        switch(type)
	                        {
	                        case TYPE_STRING:    index[i] = readString(inputStream); break; 
	                        case TYPE_NUMBER:    index[i] = readNumber(inputStream); break; 
	                        case TYPE_INTEGER:   index[i] = readInteger(inputStream); break; 
	                        case TYPE_DATE:      index[i] = readDate(inputStream); break; 
	                        case TYPE_BIGNUMBER: index[i] = readBigNumber(inputStream); break; 
	                        case TYPE_BOOLEAN:   index[i] = readBoolean(inputStream); break; 
	                        case TYPE_BINARY:    index[i] = readBinary(inputStream); break;
	                        default: throw new KettleFileException(toString()+" : Unable to de-serialize indexed storage type for data type "+getType());
	                        }
	                    }
	                }
	            }
	            break;
	            
            case STORAGE_TYPE_BINARY_STRING:
	            {
	            	// In case we do have storage metadata defined, we read that back in as well..
	            	if (inputStream.readBoolean()) {
	            		storageMetadata = new ValueMeta(inputStream);
	            	}
	            }
	            break;
	            
	        default:
	        	break;
            }
            
            // name
            name = readString(inputStream);  
            
            // length & precision
            length = inputStream.readInt();
            precision = inputStream.readInt();
            
            // Origin
            origin = readString(inputStream);

            // Comments
            comments=readString(inputStream);
            
            // formatting Mask, decimal, grouping, currency
            
            conversionMask=readString(inputStream);
            decimalSymbol=readString(inputStream);
            groupingSymbol=readString(inputStream);
            currencySymbol=readString(inputStream);
            trimType=inputStream.readInt();
            
            // Case sensitivity
            caseInsensitive = inputStream.readBoolean();
            
            // Sorting type
            sortedDescending = inputStream.readBoolean();
            
            // Output padding?
            outputPaddingEnabled = inputStream.readBoolean();
            
            // is date parsing lenient?
            dateFormatLenient = inputStream.readBoolean();
            
            String strDateFormatLocale = readString(inputStream);
            if (Const.isEmpty(strDateFormatLocale)) 
            {
                dateFormatLocale = null; 
            }
            else
            {
                dateFormatLocale = new Locale(strDateFormatLocale);
            }
        }
        catch(EOFException e)
        {
        	throw new KettleEOFException(e);
        }
        catch(IOException e)
        {
            throw new KettleFileException(toString()+" : Unable to read value metadata from input stream", e);
        }
    }
    
    public String getMetaXML() throws IOException
    {
    	StringBuffer xml = new StringBuffer();
    	
    	xml.append(XMLHandler.openTag(XML_META_TAG));
    	
        xml.append( XMLHandler.addTagValue("type", getTypeDesc()) ) ;
        xml.append( XMLHandler.addTagValue("storagetype", getStorageType()) );

        switch(storageType) {
        case STORAGE_TYPE_INDEXED:
            {
            	xml.append( XMLHandler.openTag("index"));

                // Save the indexed strings...
            	//
                if (index!=null)
                {
                    for (int i=0;i<index.length;i++)
                    {
                        switch(type)
                        {
                        case TYPE_STRING:    xml.append( XMLHandler.addTagValue( "value", (String)index[i]) ); break; 
                        case TYPE_NUMBER:    xml.append( XMLHandler.addTagValue( "value",  (Double)index[i]) ); break; 
                        case TYPE_INTEGER:   xml.append( XMLHandler.addTagValue( "value", (Long)index[i]) ); break; 
                        case TYPE_DATE:      xml.append( XMLHandler.addTagValue( "value", (Date)index[i]) ); break; 
                        case TYPE_BIGNUMBER: xml.append( XMLHandler.addTagValue( "value", (BigDecimal)index[i]) ); break; 
                        case TYPE_BOOLEAN:   xml.append( XMLHandler.addTagValue( "value", (Boolean)index[i]) ); break; 
                        case TYPE_BINARY:    xml.append( XMLHandler.addTagValue( "value", (byte[])index[i]) ); break;
                        default: throw new IOException(toString()+" : Unable to serialize indexe storage type to XML for data type "+getType());
                        }
                    }
                }
            	
            	xml.append( XMLHandler.closeTag("index"));
            }
            break;
        
        case STORAGE_TYPE_BINARY_STRING:
            {
            	// Save the storage meta data...
            	//
            	if (storageMetadata!=null)
            	{
            		xml.append(XMLHandler.openTag("storage-meta"));
            		xml.append(storageMetadata.getMetaXML());
            		xml.append(XMLHandler.closeTag("storage-meta"));
            	}
            }
            break;
            
       default:
    	   break;
       }
        
        xml.append( XMLHandler.addTagValue("name", name) );  
        xml.append( XMLHandler.addTagValue("length", length) );  
        xml.append( XMLHandler.addTagValue("precision", precision) );  
        xml.append( XMLHandler.addTagValue("origin", origin) );  
        xml.append( XMLHandler.addTagValue("comments", comments) );  
        xml.append( XMLHandler.addTagValue("conversion_Mask", conversionMask) );  
        xml.append( XMLHandler.addTagValue("decimal_symbol", decimalSymbol) );  
        xml.append( XMLHandler.addTagValue("grouping_symbol", groupingSymbol) );  
        xml.append( XMLHandler.addTagValue("currency_symbol", currencySymbol) );  
        xml.append( XMLHandler.addTagValue("trim_type", getTrimTypeCode(trimType)) );
        xml.append( XMLHandler.addTagValue("case_insensitive", caseInsensitive) );
        xml.append( XMLHandler.addTagValue("sort_descending", sortedDescending) );
        xml.append( XMLHandler.addTagValue("output_padding", outputPaddingEnabled) );
        xml.append( XMLHandler.addTagValue("date_format_lenient", dateFormatLenient) );
        xml.append( XMLHandler.addTagValue("date_format_locale", dateFormatLocale.toString()) );
        
    	xml.append(XMLHandler.closeTag(XML_META_TAG));
    	
    	return xml.toString();
    }
    
    public ValueMeta(Node node) throws IOException 
    {
    	this();
    	
        type = getType( XMLHandler.getTagValue(node, "type") ) ;
        storageType = getStorageType( XMLHandler.getTagValue(node, "storagetype") );

        switch(storageType) {
        case STORAGE_TYPE_INDEXED:
            {
            	Node indexNode = XMLHandler.getSubNode(node, "index");
            	int nrIndexes = XMLHandler.countNodes(indexNode, "value");
            	index = new Object[nrIndexes];
            	
        	    for (int i=0;i<index.length;i++)
                {
        	    	Node valueNode = XMLHandler.getSubNodeByNr(indexNode, "value", i);
        	    	String valueString = XMLHandler.getNodeValue(valueNode);
        	    	if (Const.isEmpty(valueString))
        	    	{
        	    		index[i] = null;
        	    	}
        	    	else
        	    	{
	                    switch(type)
	                    {
	                    case TYPE_STRING:    index[i] = valueString; break; 
	                    case TYPE_NUMBER:    index[i] = Double.parseDouble( valueString ); break; 
	                    case TYPE_INTEGER:   index[i] = Long.parseLong( valueString ); break; 
	                    case TYPE_DATE:      index[i] = XMLHandler.stringToDate( valueString ); ; break; 
	                    case TYPE_BIGNUMBER: index[i] = new BigDecimal( valueString ); ; break; 
	                    case TYPE_BOOLEAN:   index[i] = Boolean.valueOf("Y".equalsIgnoreCase( valueString)); break; 
	                    case TYPE_BINARY:    index[i] = XMLHandler.stringToBinary( valueString ); break;
	                    default: throw new IOException(toString()+" : Unable to de-serialize indexe storage type from XML for data type "+getType());
	                    }
        	    	}
                }
            }
            break;
        
        case STORAGE_TYPE_BINARY_STRING:
            {
            	// Save the storage meta data...
            	//
            	Node storageMetaNode = XMLHandler.getSubNode(node, "storage-meta");
            	if (storageMetaNode!=null)
            	{
            		storageMetadata = new ValueMeta(storageMetaNode);
            	}
            }
            break;
            
       default:
    	   break;
       }
        
        name = XMLHandler.getTagValue(node, "name");  
        length =  Integer.parseInt( XMLHandler.getTagValue(node, "length") );  
        precision = Integer.parseInt( XMLHandler.getTagValue(node, "precision") );  
        origin = XMLHandler.getTagValue(node, "origin");  
        comments = XMLHandler.getTagValue(node, "comments");  
        conversionMask = XMLHandler.getTagValue(node, "conversion_Mask");  
        decimalSymbol = XMLHandler.getTagValue(node, "decimal_symbol");  
        groupingSymbol = XMLHandler.getTagValue(node, "grouping_symbol");  
        currencySymbol = XMLHandler.getTagValue(node, "currency_symbol");  
        trimType = getTrimTypeByCode( XMLHandler.getTagValue(node, "trim_type") );
        caseInsensitive = "Y".equalsIgnoreCase( XMLHandler.getTagValue(node, "case_insensitive") );
        sortedDescending = "Y".equalsIgnoreCase( XMLHandler.getTagValue(node, "sort_descending") );
        outputPaddingEnabled = "Y".equalsIgnoreCase( XMLHandler.getTagValue(node, "output_padding") );
        dateFormatLenient = "Y".equalsIgnoreCase( XMLHandler.getTagValue(node, "date_format_lenient") );
        String dateFormatLocaleString = XMLHandler.getTagValue(node, "date_format_locale");
        if (!Const.isEmpty( dateFormatLocaleString ))
        {
        	dateFormatLocale = new Locale(dateFormatLocaleString);
        }
	}

    public String getDataXML(Object object) throws IOException
    {
    	StringBuffer xml = new StringBuffer();
    	
    	xml.append(XMLHandler.openTag(XML_DATA_TAG));
    	
        if (object!=null) // otherwise there is no point
        {
            switch(storageType)
            {
            case STORAGE_TYPE_NORMAL:
                // Handle Content -- only when not NULL
            	//
                switch(getType())
                {
                case TYPE_STRING     : xml.append( XMLHandler.addTagValue("string-value", (String)object) ); break;
                case TYPE_NUMBER     : xml.append( XMLHandler.addTagValue("number-value", (Double)object) ); break;
                case TYPE_INTEGER    : xml.append( XMLHandler.addTagValue("integer-value", (Long)object) ); break;
                case TYPE_DATE       : xml.append( XMLHandler.addTagValue("date-value", (Date)object) ); break;
                case TYPE_BIGNUMBER  : xml.append( XMLHandler.addTagValue("bignumber-value", (BigDecimal)object) ); break;
                case TYPE_BOOLEAN    : xml.append( XMLHandler.addTagValue("boolean-value", (Boolean)object) ); break;
                case TYPE_BINARY     : xml.append( XMLHandler.addTagValue("binary-value", (byte[])object) ); break;
                default: throw new IOException(toString()+" : Unable to serialize data type to XML "+getType());
                }
                break;
                
            case STORAGE_TYPE_BINARY_STRING:
                // Handle binary string content -- only when not NULL
            	// In this case, we opt not to convert anything at all for speed.
            	// That way, we can save on CPU power.
            	// Since the streams can be compressed, volume shouldn't be an issue at all.
            	//
            	xml.append( XMLHandler.addTagValue("binary-string", (byte[])object) );
                break;
                
            case STORAGE_TYPE_INDEXED:
            	xml.append( XMLHandler.addTagValue("index-value", (Integer)object) ); // just an index 
                break;
                
            default: throw new IOException(toString()+" : Unknown storage type "+getStorageType());
            }
        }
    	
    	xml.append(XMLHandler.closeTag(XML_META_TAG));
    	
    	return xml.toString();
    }

    /**
     * Convert a data XML node to an Object that corresponds to the metadata.
     * This is basically String to Object conversion that is being done.
     * @param node the node to retrieve the data value from
     * @return the converted data value
     * @throws IOException thrown in case there is a problem with the XML to object conversion
     */
	public Object getValue(Node node) throws IOException {
		
        switch(storageType)
        {
        case STORAGE_TYPE_NORMAL:
    		String valueString = XMLHandler.getTagValue(node, "value");
    		if (Const.isEmpty(valueString)) return null;
    		
            // Handle Content -- only when not NULL
        	//
            switch(getType())
            {
            case TYPE_STRING:    return valueString;
            case TYPE_NUMBER:    return Double.parseDouble( valueString ); 
            case TYPE_INTEGER:   return Long.parseLong( valueString );
            case TYPE_DATE:      return XMLHandler.stringToDate( valueString ); 
            case TYPE_BIGNUMBER: return new BigDecimal( valueString );
            case TYPE_BOOLEAN:   return Boolean.valueOf("Y".equalsIgnoreCase( valueString)); 
            case TYPE_BINARY:    return XMLHandler.stringToBinary( valueString );
            default: throw new IOException(toString()+" : Unable to de-serialize '"+valueString+"' from XML for data type "+getType());
            }
            
        case STORAGE_TYPE_BINARY_STRING:
            // Handle binary string content -- only when not NULL
        	// In this case, we opt not to convert anything at all for speed.
        	// That way, we can save on CPU power.
        	// Since the streams can be compressed, volume shouldn't be an issue at all.
        	//
        	String binaryString = XMLHandler.getTagValue(node, "binary-string");
    		if (Const.isEmpty(binaryString)) return null;
    		
    		return XMLHandler.stringToBinary(binaryString);
            
        case STORAGE_TYPE_INDEXED:
        	String indexString = XMLHandler.getTagValue(node, "index-value");
    		if (Const.isEmpty(indexString)) return null;

    		return Integer.parseInt(indexString); 
            
        default: throw new IOException(toString()+" : Unknown storage type "+getStorageType());
        }

	}



	/**
     * get an array of String describing the possible types a Value can have.
     * @return an array of String describing the possible types a Value can have.
     */
    public static final String[] getTypes()
    {
        String retval[] = new String[typeCodes.length-1];
        System.arraycopy(typeCodes, 1, retval, 0, typeCodes.length-1);
        return retval;
    }
    
    /**
     * Get an array of String describing the possible types a Value can have.
     * @return an array of String describing the possible types a Value can have.
     */
    public static final String[] getAllTypes()
    {
        String retval[] = new String[typeCodes.length];
        System.arraycopy(typeCodes, 0, retval, 0, typeCodes.length);
        return retval;
    }
    
    /**
     * TODO: change Desc to Code all over the place.  Make sure we can localise this stuff later on.
     * 
     * @param type the type 
     * @return the description (code) of the type
     */
    public static final String getTypeDesc(int type)
    {
        return typeCodes[type];
    }

    /**
     * Convert the String description of a type to an integer type.
     * @param desc The description of the type to convert
     * @return The integer type of the given String.  (ValueMetaInterface.TYPE_...)
     */
    public static final int getType(String desc)
    {
        for (int i=1;i<typeCodes.length;i++)
        {
            if (typeCodes[i].equalsIgnoreCase(desc))
            {
                return i; 
            }
        }

        return TYPE_NONE;
    }
    

    /**
     * Convert the String description of a storage type to an integer type.
     * @param desc The description of the storage type to convert
     * @return The integer storage type of the given String.  (ValueMetaInterface.STORAGE_TYPE_...)
     */
    public static final int getStorageType(String desc)
    {
        for (int i=0;i<storageTypeCodes.length;i++)
        {
            if (storageTypeCodes[i].equalsIgnoreCase(desc))
            {
                return i; 
            }
        }

        return STORAGE_TYPE_NORMAL;
    }
    
    /**
     * Determine if an object is null.
     * This is the case if data==null or if it's an empty string.
     * @param data the object to test
     * @return true if the object is considered null.
     */
    public boolean isNull(Object data)
    {
        if (data==null) return true;
        if (isString()) {
        	if (isStorageNormal() && ((String)data).length()==0) return true;
        	if (isStorageBinaryString()) {
        		try{
        			if ( ((byte[])data).length==0 ) return true;
        		}
        		catch(ClassCastException e)
        		{
        			throw e;
        		}
        	}
        }
        return false;
    }
    
    /**
     * Compare 2 values of the same data type
     * @param data1 the first value
     * @param data2 the second value
     * @return 0 if the values are equal, -1 if data1 is smaller than data2 and +1 if it's larger.
     * @throws KettleValueException In case we get conversion errors
     */
    public int compare(Object data1, Object data2) throws KettleValueException
    {
        boolean n1 = isNull(data1);
        boolean n2 = isNull(data2);

        // null is always smaller!
        if (n1 && !n2) return -1;
        if (!n1 && n2) return 1;
        if (n1 && n2) return 0;

        int cmp=0;
        switch (getType())
        {
        case TYPE_STRING:
            {
            	String one = Const.rtrim(getString(data1));
                String two = Const.rtrim(getString(data2));
    
                if (caseInsensitive)
                {
                    cmp = one.compareToIgnoreCase(two);
                }
                else
                {
                    cmp = one.compareTo(two);
                }
            }
            break;

        case TYPE_INTEGER:
            {
                long compare = getInteger(data1).longValue() - getInteger(data2).longValue();
                if (compare<0) cmp=-1;
                else if (compare>0) cmp=1;
                else cmp=0;
            }
            break;

        case TYPE_NUMBER:
            {
                cmp=Double.compare(getNumber(data1).doubleValue(), getNumber(data2).doubleValue());
            }
            break;

        case TYPE_DATE:
            {
            	long compare =  getDate(data1).getTime() - getDate(data2).getTime();
                if (compare<0) cmp=-1;
                else if (compare>0) cmp=1;
                else cmp=0;
            }
            break;

        case TYPE_BIGNUMBER:
            {
                cmp=getBigNumber(data1).compareTo(getBigNumber(data2));
            }
            break;

        case TYPE_BOOLEAN:
            {
                if (getBoolean(data1).booleanValue() == getBoolean(data2).booleanValue()) cmp=0; // true == true, false == false
                else if (getBoolean(data1).booleanValue() && !getBoolean(data2).booleanValue()) cmp=1; // true  > false
                else cmp=-1; // false < true
            }
            break;

        case TYPE_BINARY:
            {
                byte[] b1 = (byte[]) data1;
                byte[] b2 = (byte[]) data2;
                
                int length= b1.length < b2.length ? b1.length : b2.length;
                
                for (int i=0;i<length;i++)
                {
                    cmp = b1[i] - b2[i];
                    if (cmp!=0)
                    {
                        cmp = Math.abs(cmp);
                        break;
                    }
                }
            }
            break;
        default: 
            throw new KettleValueException(toString()+" : Comparing values can not be done with data type : "+getType());
        }
        
        if (isSortedDescending())
        {
            return -cmp;
        }
        else
        {
            return cmp;
        }
    }
    
    /**
     * Compare 2 values of the same data type
     * @param data1 the first value
     * @param meta2 the second value's metadata
     * @param data2 the second value
     * @return 0 if the values are equal, -1 if data1 is smaller than data2 and +1 if it's larger.
     * @throws KettleValueException In case we get conversion errors
     */
    public int compare(Object data1, ValueMetaInterface meta2, Object data2) throws KettleValueException
    {
        // Before we can compare data1 to data2 we need to make sure they have the same data type etc.
        if (getType()==meta2.getType()) return compare(data1, data2);
        
        // If the data types are not the same, the first one is the driver...
        // The second data type is converted to the first one.
        //
        return compare(data1, convertData(meta2, data2));
    }

    /**
     * Convert the specified data to the data type specified in this object.
     * @param meta2 the metadata of the object to be converted
     * @param data2 the data of the object to be converted
     * @return the object in the data type of this value metadata object
     * @throws KettleValueException in case there is a data conversion error
     */
    public Object convertData(ValueMetaInterface meta2, Object data2) throws KettleValueException
    {
        switch(getType())
        {
        case TYPE_STRING    : return meta2.getString(data2);
        case TYPE_NUMBER    : return meta2.getNumber(data2);
        case TYPE_INTEGER   : return meta2.getInteger(data2);
        case TYPE_DATE      : return meta2.getDate(data2);
        case TYPE_BIGNUMBER : return meta2.getBigNumber(data2);
        case TYPE_BOOLEAN   : return meta2.getBoolean(data2);
        case TYPE_BINARY    : return meta2.getBinary(data2);
        default: 
            throw new KettleValueException(toString()+" : I can't convert the specified value to data type : "+getType());
        }
    }

    /**
     * Convert an object to the data type specified in the storage metadata
     * @param data The data
     * @return The data converted to the storage data type
     * @throws KettleValueException in case there is a conversion error.
     */
    public Object convertDataUsingStorageMetaData(Object data2) throws KettleValueException {
    	if (storageMetadata==null) {
    		throw new KettleValueException("API coding error: please specify a storage metadata before attempting to convert value "+name);
    	}
    	
    	// Suppose we have an Integer 123, length 5
    	// The string variation of this is " 00123"
    	// To convert this back to an Integer we use the storage metadata
    	// Specifically, in method convertStringToInteger() we consult the storageMetaData to get the correct conversion mask
    	// That way we're always sure that a conversion works both ways.
    	// 
    	
    	switch(storageMetadata.getType()) {
        case TYPE_STRING    : return getString(data2);
        case TYPE_INTEGER   : return getInteger(data2); 
        case TYPE_NUMBER    : return getNumber(data2);
        case TYPE_DATE      : return getDate(data2);
        case TYPE_BIGNUMBER : return getBigNumber(data2);
        case TYPE_BOOLEAN   : return getBoolean(data2);
        case TYPE_BINARY    : return getBinary(data2);
        default: 
            throw new KettleValueException(toString()+" : I can't convert the specified value to data type : "+storageMetadata.getType());
        }
    }

    /**
     * Convert the specified string to the data type specified in this object.
     * @param pol the string to be converted
     * @param convertMeta the metadata of the object (only string type) to be converted
     * @param nullIf set the object to null if pos equals nullif (IgnoreCase)
     * @param ifNull set the object to ifNull when pol is empty or null
     * @param trim_type the trim type to be used (ValueMetaInterface.TRIM_TYPE_XXX)
     * @return the object in the data type of this value metadata object
     * @throws KettleValueException in case there is a data conversion error
     */
    public Object convertDataFromString(String pol, ValueMetaInterface convertMeta, String nullIf, String ifNull, int trim_type) throws KettleValueException
    {
        // null handling and conversion of value to null
        //
		String null_value = nullIf;
		if (null_value == null)
		{
			switch (convertMeta.getType())
			{
			case Value.VALUE_TYPE_BOOLEAN:
				null_value = Const.NULL_BOOLEAN;
				break;
			case Value.VALUE_TYPE_STRING:
				null_value = Const.NULL_STRING;
				break;
			case Value.VALUE_TYPE_BIGNUMBER:
				null_value = Const.NULL_BIGNUMBER;
				break;
			case Value.VALUE_TYPE_NUMBER:
				null_value = Const.NULL_NUMBER;
				break;
			case Value.VALUE_TYPE_INTEGER:
				null_value = Const.NULL_INTEGER;
				break;
			case Value.VALUE_TYPE_DATE:
				null_value = Const.NULL_DATE;
				break;
			case Value.VALUE_TYPE_BINARY:
				null_value = Const.NULL_BINARY;
				break;				
			default:
				null_value = Const.NULL_NONE;
				break;
			}
		}

    	// See if we need to convert a null value into a String
		// For example, we might want to convert null into "Empty".
    	//
        if (!Const.isEmpty(ifNull)) {
			String nullCmp = Const.rightPad(new StringBuffer(null_value), pol.length());
			if (Const.isEmpty(pol) || pol.equalsIgnoreCase(nullCmp))
			{
				pol = ifNull;
			}
        }

		// This looks like the same condition as above but r
		if (Const.isEmpty(pol) || pol.equalsIgnoreCase(Const.rightPad(new StringBuffer(null_value), pol.length())) )
		{
            return null;
        }
 
        
        // Trimming
        switch (trim_type)
        {
        case ValueMetaInterface.TRIM_TYPE_LEFT:
            {
                StringBuffer strpol = new StringBuffer(pol);
                while (strpol.length() > 0 && strpol.charAt(0) == ' ')
                    strpol.deleteCharAt(0);
                pol=strpol.toString();
            }
            break;
        case ValueMetaInterface.TRIM_TYPE_RIGHT:
            {
                StringBuffer strpol = new StringBuffer(pol);
                while (strpol.length() > 0 && strpol.charAt(strpol.length() - 1) == ' ')
                    strpol.deleteCharAt(strpol.length() - 1);
                pol=strpol.toString();
            }
            break;
        case ValueMetaInterface.TRIM_TYPE_BOTH:
            StringBuffer strpol = new StringBuffer(pol);
            {
                while (strpol.length() > 0 && strpol.charAt(0) == ' ')
                    strpol.deleteCharAt(0);
                while (strpol.length() > 0 && strpol.charAt(strpol.length() - 1) == ' ')
                    strpol.deleteCharAt(strpol.length() - 1);
                pol=strpol.toString();
            }
            break;
        default:
            break;
        }
        
        // On with the regular program...
        // Simply call the ValueMeta routines to do the conversion
        // We need to do some effort here: copy all 
        //
        return convertData(convertMeta, pol); 
    }
    
    /**
     * Calculate the hashcode of the specified data object
     * @param object the data value to calculate a hashcode for 
     * @return the calculated hashcode
     * @throws KettleValueException 
     */
    public int hashCode(Object object) throws KettleValueException
    {
        int hash=0;
        
        if (isNull(object))
        {
            switch(getType())
            {
            case TYPE_BOOLEAN   : hash^= 1; break;
            case TYPE_DATE      : hash^= 2; break;
            case TYPE_NUMBER    : hash^= 4; break;
            case TYPE_STRING    : hash^= 8; break;
            case TYPE_INTEGER   : hash^=16; break;
            case TYPE_BIGNUMBER : hash^=32; break;
            case TYPE_NONE      : break;
            default: break;
            }
        }
        else
        {
            switch(getType())
            {
            case TYPE_BOOLEAN   : hash^=getBoolean(object).hashCode(); break;
            case TYPE_DATE      : hash^=getDate(object).hashCode(); break;
            case TYPE_INTEGER   : hash^=getInteger(object).hashCode(); break;
            case TYPE_NUMBER    : hash^=getNumber(object).hashCode(); break;
            case TYPE_STRING    : hash^=getString(object).hashCode(); break;
            case TYPE_BIGNUMBER : hash^=getBigNumber(object).hashCode(); break;
            case TYPE_NONE      : break;
            default: break;
            }
        }

        return hash;
    }

    /**
     * Create an old-style value for backward compatibility reasons
     * @param data the data to store in the value
     * @return a newly created Value object
     * @throws KettleValueException  case there is a data conversion problem
     */
    public Value createOriginalValue(Object data) throws KettleValueException
    {
       Value value = new Value(name, type);
       value.setLength(length, precision);
       
       if (isNull(data))
       {
           value.setNull();
       }
       else
       {
           switch(value.getType())
           {
           case TYPE_STRING       : value.setValue( getString(data) ); break;
           case TYPE_NUMBER       : value.setValue( getNumber(data).doubleValue() ); break;
           case TYPE_INTEGER      : value.setValue( getInteger(data).longValue() ); break;
           case TYPE_DATE         : value.setValue( getDate(data) ); break;
           case TYPE_BOOLEAN      : value.setValue( getBoolean(data).booleanValue() ); break;
           case TYPE_BIGNUMBER    : value.setValue( getBigNumber(data) ); break;
           case TYPE_BINARY       : value.setValue( getBinary(data) ); break;
           default: throw new KettleValueException(toString()+" : We can't convert data type "+getTypeDesc()+" to an original (V2) Value");
           }
       }
       return value;
    }
    
    
    /**
     * Extracts the primitive data from an old style Value object 
     * @param value the old style Value object 
     * @return the value's data, NOT the meta data.
     * @throws KettleValueException  case there is a data conversion problem
     */
    public Object getValueData(Value value) throws KettleValueException
    {
       if (value==null || value.isNull()) return null;
       
       // So far the old types and the new types map to the same thing.
       // For compatibility we just ask the old-style value to convert to the new one.
       // In the old transformation this would happen sooner or later anyway.
       // It doesn't throw exceptions or complain either (unfortunately).
       //
       
       switch(getType())
       {
       case ValueMetaInterface.TYPE_STRING       : return value.getString();
       case ValueMetaInterface.TYPE_NUMBER       : return value.getNumber();
       case ValueMetaInterface.TYPE_INTEGER      : return value.getInteger();
       case ValueMetaInterface.TYPE_DATE         : return value.getDate();
       case ValueMetaInterface.TYPE_BOOLEAN      : return value.getBoolean();
       case ValueMetaInterface.TYPE_BIGNUMBER    : return value.getBigNumber();
       case ValueMetaInterface.TYPE_BINARY       : return value.getBytes();
       default: throw new KettleValueException(toString()+" : We can't convert original data type "+value.getTypeDesc()+" to a primitive data type");
       }
    }

	/**
	 * @return the storageMetadata
	 */
	public ValueMetaInterface getStorageMetadata() {
		return storageMetadata;
	}

	/**
	 * @param storageMetadata the storageMetadata to set
	 */
	public void setStorageMetadata(ValueMetaInterface storageMetadata) {
		this.storageMetadata = storageMetadata;
		compareStorageAndActualFormat();
	}

	private void compareStorageAndActualFormat() {
		
		if (storageMetadata==null) {
			identicalFormat = true;
		} 
		else {
			
			// If a trim type is set, we need to at least try to trim the strings.
			// In that case, we have to set the identical format off.
			//
			if (trimType!=TRIM_TYPE_NONE) {
				identicalFormat = false;
			}
			else {
			
				// If there is a string encoding set and it's the same encoding in the binary string, then we don't have to convert
				// If there are no encodings set, then we're certain we don't have to convert as well.
				//
				if (getStringEncoding()!=null && getStringEncoding().equals(storageMetadata.getStringEncoding()) || 
					getStringEncoding()==null && storageMetadata.getStringEncoding()==null) {
					
					// However, perhaps the conversion mask changed since we read the binary string?
					// The output can be different from the input.  If the mask is different, we need to do conversions.
					// Otherwise, we can just ignore it...
					//
					if (isDate()) {
						if ( (getConversionMask()!=null && getConversionMask().equals(storageMetadata.getConversionMask())) ||
							(getConversionMask()==null && storageMetadata.getConversionMask()==null) ) {
							identicalFormat = true;
						}
						else {
							identicalFormat = false;
						}
					}
					else if (isNumeric()) {
						// For the same reasons as above, if the conversion mask, the decimal or the grouping symbol changes
						// we need to convert from the binary strings to the target data type and then back to a string in the required format.
						//
						if ( (getConversionMask()!=null && getConversionMask().equals(storageMetadata.getConversionMask()) ||
								(getConversionMask()==null && storageMetadata.getConversionMask()==null))
						   ) {
							if ( (getGroupingSymbol()!=null && getGroupingSymbol().equals(storageMetadata.getGroupingSymbol())) || 
									(getConversionMask()==null && storageMetadata.getConversionMask()==null) ) {
								if ( (getDecimalFormat()!=null && getDecimalFormat().equals(storageMetadata.getDecimalFormat())) || 
										(getDecimalFormat()==null && storageMetadata.getDecimalFormat()==null) ) {
									identicalFormat = true;
								}
								else {
									identicalFormat = false;
								}
							} 
							else {
								identicalFormat = false;
							}
						}
						else {
							identicalFormat = false;
						}
					}
				}
			}
		}
	}

	/**
	 * @return the trimType
	 */
	public int getTrimType() {
		return trimType;
	}

	/**
	 * @param trimType the trimType to set
	 */
	public void setTrimType(int trimType) {
		this.trimType = trimType;
	}
	
	public final static int getTrimTypeByCode(String tt)
	{
		if (tt == null) return 0;

		for (int i = 0; i < trimTypeCode.length; i++)
		{
			if (trimTypeCode[i].equalsIgnoreCase(tt)) return i;
		}
		return 0;
	}

	public final static int getTrimTypeByDesc(String tt)
	{
		if (tt == null) return 0;

		for (int i = 0; i < trimTypeDesc.length; i++)
		{
			if (trimTypeDesc[i].equalsIgnoreCase(tt)) return i;
		}

        // If this fails, try to match using the code.
        return getTrimTypeByCode(tt);
	}

	public final static String getTrimTypeCode(int i)
	{
		if (i < 0 || i >= trimTypeCode.length) return trimTypeCode[0];
		return trimTypeCode[i];
	}

	public final static String getTrimTypeDesc(int i)
	{
		if (i < 0 || i >= trimTypeDesc.length) return trimTypeDesc[0];
		return trimTypeDesc[i];
	}
}