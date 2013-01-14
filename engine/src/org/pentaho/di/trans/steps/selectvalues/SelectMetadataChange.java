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

package org.pentaho.di.trans.steps.selectvalues;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.xml.XMLInterface;
import org.pentaho.di.trans.step.StepAttributesInterface;
import org.w3c.dom.Node;

public class SelectMetadataChange implements Cloneable, XMLInterface{
	
	public static final String XML_TAG = "meta";
	
	// META-DATA mode
	/** Fields of which we want to change the meta data  */
	private String name;
	/** Meta: new name of field  */
	private String rename;
	/** Meta: new Value type for this field or TYPE_NONE if no change needed!  */
	private int    type;
	/** Meta: new length of field  */
	private int    length;
	/** Meta: new precision of field (for numbers)  */
	private int    precision;
	/** Meta: the storage type, NORMAL or BINARY_STRING  */
	private int    storageType;
	/** The conversion metadata if any conversion needs to take place */
	private String conversionMask;
	/** Treat the date format as lenient */
	private boolean dateFormatLenient;
	
	/** This is the locale to use for date parsing */
	private String dateFormatLocale;
	
	/** This is the time zone to use for date parsing */
	private String dateFormatTimeZone;
	
	/** Treat string to number format as lenient */
	private boolean lenientStringToNumber;
	/** The decimal symbol for number conversions */
	private String decimalSymbol; 
	/** The grouping symbol for number conversions */
	private String groupingSymbol; 
	/** The currency symbol for number conversions */
	private String currencySymbol;
	/** The encoding to use when decoding binary data to Strings */
	private String encoding;
  

  private StepAttributesInterface attributesInterface;
	
	public SelectMetadataChange(StepAttributesInterface attributesInterface) {
	  this.attributesInterface = attributesInterface;
		storageType=-1; // storage type is not used by default!
	}

	/**
	 * @Deprecated
	 * This method is left here for external code that may be using it.
	 * It may be removed in the future.
	 * @see #SelectMetadataChange(StepAttributesInterface, String, String, int, int, int, int, String, boolean, String, String, String)
	 */
	public SelectMetadataChange(StepAttributesInterface attributesInterface, String name, String rename, int type, int length, int precision, int storageType,
      String conversionMask, String decimalSymbol, String groupingSymbol, String currencySymbol) {
	  this(attributesInterface, name, rename, type, length, precision, storageType,
      conversionMask, false, null, null, false, decimalSymbol, groupingSymbol, currencySymbol);
	}

	/**
	 * 
	 * @param attributesInterface
	 * @param name
	 * @param rename
	 * @param type
	 * @param length
	 * @param precision
	 * @param storageType
	 * @param conversionMask
	 * @param dateFormatLenient
	 * @param dateFormatLocale
	 * @param dateFormatTimeZone
	 * @param lenientStringToNumber
	 * @param decimalSymbol
	 * @param groupingSymbol
	 * @param currencySymbol
	 */
	public SelectMetadataChange(StepAttributesInterface attributesInterface, String name, String rename, int type, int length, int precision, int storageType,
			String conversionMask, boolean dateFormatLenient, String dateFormatLocale, String dateFormatTimeZone, boolean lenientStringToNumber, String decimalSymbol, String groupingSymbol, String currencySymbol) {
	    this(attributesInterface);
		this.name = name;
		this.rename = rename;
		this.type = type;
		this.length = length;
		this.precision = precision;
		this.storageType = storageType;
		this.conversionMask = conversionMask;
		this.dateFormatLenient = dateFormatLenient;
		this.dateFormatLocale = dateFormatLocale;
		this.dateFormatTimeZone = dateFormatTimeZone;
		this.lenientStringToNumber = lenientStringToNumber;
		this.decimalSymbol = decimalSymbol;
		this.groupingSymbol = groupingSymbol;
		this.currencySymbol = currencySymbol;
	}
	
	public String getXML() {
		StringBuffer retval = new StringBuffer();
		retval.append("      ").append(XMLHandler.openTag(XML_TAG)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_NAME"),            name)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_RENAME"),          rename)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_TYPE"),            ValueMeta.getTypeDesc(type)) ); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_LENGTH"),          length)); //$NON-NLS-1$ 
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_PRECISION"),       precision)); //$NON-NLS-1$ 
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_CONVERSION_MASK"), conversionMask)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_DATE_FORMAT_LENIENT"),   Boolean.toString(dateFormatLenient))); //$NON-NLS-1$
    retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_DATE_FORMAT_LOCALE"),   dateFormatLocale)); //$NON-NLS-1$
    retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_DATE_FORMAT_TIMEZONE"),   dateFormatTimeZone)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_LENIENT_STRING_TO_NUMBER"),   Boolean.toString(lenientStringToNumber))); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_ENCODING"),        encoding)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_DECIMAL"),         decimalSymbol)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_GROUPING"),        groupingSymbol)); //$NON-NLS-1$
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_CURRENCY"),        currencySymbol)); //$NON-NLS-1$		
		retval.append("        ").append(XMLHandler.addTagValue(attributesInterface.getXmlCode("META_STORAGE_TYPE"),    ValueMeta.getStorageTypeCode(storageType))); //$NON-NLS-1$		
		retval.append("      ").append(XMLHandler.closeTag(XML_TAG)); //$NON-NLS-1$
		return retval.toString();
	}
	
	public void loadXML(Node metaNode) {
		name           = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_NAME")); //$NON-NLS-1$
		rename         = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_RENAME")); //$NON-NLS-1$
		type           = ValueMeta.getType(XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_TYPE"))); //$NON-NLS-1$
		length         = Const.toInt(XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_LENGTH")), -2); //$NON-NLS-1$
		precision      = Const.toInt(XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_PRECISION")), -2); //$NON-NLS-1$
		storageType    = ValueMeta.getStorageType( XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_STORAGE_TYPE")) ); //$NON-NLS-1$
		conversionMask = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_CONVERSION_MASK")); //$NON-NLS-1$
		dateFormatLenient = Boolean.parseBoolean(XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_DATE_FORMAT_LENIENT"))); //$NON-NLS-1$
    dateFormatLocale = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_DATE_FORMAT_LOCALE")); //$NON-NLS-1$
    dateFormatTimeZone = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_DATE_FORMAT_TIMEZONE")); //$NON-NLS-1$
    encoding       = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_ENCODING")); //$NON-NLS-1$
		lenientStringToNumber = Boolean.parseBoolean(XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_LENIENT_STRING_TO_NUMBER"))); //$NON-NLS-1$
		encoding       = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_ENCODING")); //$NON-NLS-1$
		decimalSymbol  = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_DECIMAL")); //$NON-NLS-1$
		groupingSymbol = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_GROUPING")); //$NON-NLS-1$
		currencySymbol = XMLHandler.getTagValue(metaNode, attributesInterface.getXmlCode("META_CURRENCY")); //$NON-NLS-1$
	}
	
	public SelectMetadataChange clone() {
		try {
			return (SelectMetadataChange) super.clone();
		}
		catch(CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the rename
	 */
	public String getRename() {
		return rename;
	}
	
	/**
	 * @param rename the rename to set
	 */
	public void setRename(String rename) {
		this.rename = rename;
	}
	
	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * @param length the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}
	
	/**
	 * @return the precision
	 */
	public int getPrecision() {
		return precision;
	}
	
	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(int precision) {
		this.precision = precision;
	}
	
	/**
	 * @return the storageType
	 */
	public int getStorageType() {
		return storageType;
	}
	
	/**
	 * @param storageType the storageType to set
	 */
	public void setStorageType(int storageType) {
		this.storageType = storageType;
	}
	
	/**
	 * @return the conversionMask
	 */
	public String getConversionMask() {
		return conversionMask;
	}
	
	/**
	 * @param conversionMask the conversionMask to set
	 */
	public void setConversionMask(String conversionMask) {
		this.conversionMask = conversionMask;
	}
	
	/**
	 * 
	 * @return whether date conversion from string is lenient or not
	 */
	public boolean isDateFormatLenient() {
    return dateFormatLenient;
  }

	/**
	 * @param dateFormatLenient whether date conversion from string is lenient or not
	 */
  public void setDateFormatLenient(boolean dateFormatLenient) {
    this.dateFormatLenient = dateFormatLenient;
  }

  /**
	 * @return the decimalSymbol
	 */
	public String getDecimalSymbol() {
		return decimalSymbol;
	}
	
	/**
	 * @param decimalSymbol the decimalSymbol to set
	 */
	public void setDecimalSymbol(String decimalSymbol) {
		this.decimalSymbol = decimalSymbol;
	}
	
	/**
	 * @return the groupingSymbol
	 */
	public String getGroupingSymbol() {
		return groupingSymbol;
	}
	
	/**
	 * @param groupingSymbol the groupingSymbol to set
	 */
	public void setGroupingSymbol(String groupingSymbol) {
		this.groupingSymbol = groupingSymbol;
	}
	
	/**
	 * @return the currencySymbol
	 */
	public String getCurrencySymbol() {
		return currencySymbol;
	}
	
	/**
	 * @param currencySymbol the currencySymbol to set
	 */
	public void setCurrencySymbol(String currencySymbol) {
		this.currencySymbol = currencySymbol;
	}

	/**
	 * @return the encoding to use when decoding binary data to strings
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding
	 *            the encoding to use when decoding binary data to strings
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * @return the lenientStringToNumber
	 */
	public boolean isLenientStringToNumber() {
		return lenientStringToNumber;
	}

	/**
	 * @param lenientStringToNumber
	 *            the lenientStringToNumber to set
	 */
	public void setLenientStringToNumber(boolean lenientStringToNumber) {
		this.lenientStringToNumber = lenientStringToNumber;
	}

  /**
   * @return the dateFormatLocale
   */
  public String getDateFormatLocale() {
    return dateFormatLocale;
  }

  /**
   * @param dateFormatLocale the dateFormatLocale to set
   */
  public void setDateFormatLocale(String dateFormatLocale) {
    this.dateFormatLocale = dateFormatLocale;
  }

  /**
   * @return the dateFormatTimeZone
   */
  public String getDateFormatTimeZone() {
    return dateFormatTimeZone;
  }

  /**
   * @param dateFormatTimeZone the dateFormatTimeZone to set
   */
  public void setDateFormatTimeZone(String dateFormatTimeZone) {
    this.dateFormatTimeZone = dateFormatTimeZone;
  }	
}
