package org.pentaho.di.core.row;

import java.math.BigDecimal;
import java.util.Date;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;


public class ValueMetaAndData
{
    public static final String XML_TAG = "value";
    
    private ValueMetaInterface valueMeta;
    private Object valueData;
    
    public ValueMetaAndData()
    {
    }

    /**
     * @param valueMeta
     * @param valueData
     */
    public ValueMetaAndData(ValueMetaInterface valueMeta, Object valueData)
    {
        this.valueMeta = valueMeta;
        this.valueData = valueData;
    }
    
    public ValueMetaAndData(String valueName, Object valueData) throws KettleValueException
    {
        this.valueData = valueData;
        if (valueData instanceof String)
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_STRING);
        } 
        else if (valueData instanceof Double)
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_NUMBER);
        }
        else if (valueData instanceof Long)
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_INTEGER);
        }
        else if (valueData instanceof Date)
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_DATE);
        }
        else if (valueData instanceof BigDecimal)
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_BIGNUMBER);
        }
        else if (valueData instanceof Boolean)
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_BOOLEAN);
        }
        else if (valueData instanceof byte[])
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_BINARY);
        }
        else
        {
            this.valueMeta = new ValueMeta(valueName, ValueMetaInterface.TYPE_SERIALIZABLE);
        }
    }

    public Object clone()
    {
        ValueMetaAndData vmad = new ValueMetaAndData();
        try
        {
            vmad.valueData = valueMeta.cloneValueData(valueData);
        }
        catch (KettleValueException e)
        {
            vmad.valueData = null; // TODO: should we really do this?  Is it safe?
        }
        vmad.valueMeta = (ValueMetaInterface) valueMeta.clone();
        
        return vmad;
    }

    public ValueMetaAndData(Repository rep, long id_value) throws KettleException
    {
        try
        {
            RowMetaAndData r = rep.getValue(id_value);
            if (r!=null)
            {
                String name    = r.getString("NAME", null);
                int valtype    = ValueMeta.getType( r.getString("VALUE_TYPE", null) );
                boolean isNull = r.getBoolean("IS_NULL", false);
                valueMeta = new ValueMeta(name, valtype);

                if (isNull)
                {
                    valueData = null;
                }
                else
                {
                    ValueMetaInterface stringValueMeta = new ValueMeta(name, ValueMetaInterface.TYPE_STRING);
                    String stringValueData = r.getString("VALUE_STR", null);
                    valueData = valueMeta.convertData(stringValueMeta, stringValueData);
                }
            }
        }
        catch(KettleException dbe)
        {
            throw new KettleException("Unable to load Value from repository with id_value="+id_value, dbe);
        }
    }
    
    public String toString()
    {
        try
        {
            return valueMeta.getString(valueData);
        }
        catch (KettleValueException e)
        {
            return "<!["+e.getMessage()+"]!>";
        }
    }
    
    /**
     * Produce the XML representation of this value.
     * @return a String containing the XML to represent this Value.
     */
    public String getXML()
    {
        ValueMetaInterface meta = (ValueMetaInterface) valueMeta.clone();
        meta.setDecimalSymbol(".");
        meta.setGroupingSymbol(null);
        meta.setCurrencySymbol(null);
        
        StringBuffer retval = new StringBuffer(128);
        retval.append("<"+XML_TAG+">");
        retval.append(XMLHandler.addTagValue("name", meta.getName(), false));
        retval.append(XMLHandler.addTagValue("type", meta.getTypeDesc(), false));
        try
        {
            retval.append(XMLHandler.addTagValue("text", meta.getString(valueData), false));
        }
        catch (KettleValueException e)
        {
            LogWriter.getInstance().logError(toString(), Const.getStackTracker(e));
            retval.append(XMLHandler.addTagValue("text", "", false));
        }
        retval.append(XMLHandler.addTagValue("length", meta.getLength(), false));
        retval.append(XMLHandler.addTagValue("precision", meta.getPrecision(), false));
        retval.append(XMLHandler.addTagValue("isnull", meta.isNull(valueData), false));
        retval.append("</"+XML_TAG+">");

        return retval.toString();
    }
    
    /**
     * Construct a new Value and read the data from XML
     * @param valnode The XML Node to read from.
     */
    public ValueMetaAndData(Node valnode)
    {
        this();
        loadXML(valnode);
    }
    
    /**
     * Read the data for this Value from an XML Node
     * @param valnode The XML Node to read from
     * @return true if all went well, false if something went wrong.
     */
    public boolean loadXML(Node valnode)
    {
        valueMeta = new ValueMeta();
        
        try
        {
            String valname =  XMLHandler.getTagValue(valnode, "name");
            int valtype    =  ValueMeta.getType( XMLHandler.getTagValue(valnode, "type") );
            String text    =  XMLHandler.getTagValue(valnode, "text");
            boolean isnull =  "Y".equalsIgnoreCase(XMLHandler.getTagValue(valnode, "isnull"));
            int len        =  Const.toInt(XMLHandler.getTagValue(valnode, "length"), -1);
            int prec       =  Const.toInt(XMLHandler.getTagValue(valnode, "precision"), -1);

            valueMeta.setType(valtype);
            valueMeta.setName(valname);
            valueData = text;
            valueMeta.setLength(len);
            valueMeta.setPrecision(prec);

            if (valtype!=ValueMetaInterface.TYPE_STRING) 
            {
                ValueMetaInterface originMeta = new ValueMeta(valname, ValueMetaInterface.TYPE_STRING);
                if (valueMeta.isNumeric())
                {
                    originMeta.setDecimalSymbol(".");
                    originMeta.setGroupingSymbol(null);
                    originMeta.setCurrencySymbol(null);
                }
                valueData = ValueDataUtil.trim(text);
                valueData = valueMeta.convertData(originMeta, valueData);
            }

            if (isnull) valueData=null;
        }
        catch(Exception e)
        {
            valueData=null;
            return false;
        }

        return true;
    }
    
    public String toStringMeta()
    {
        return valueMeta.toStringMeta();
    }
    
    /**
     * @return the valueData
     */
    public Object getValueData()
    {
        return valueData;
    }

    /**
     * @param valueData the valueData to set
     */
    public void setValueData(Object valueData)
    {
        this.valueData = valueData;
    }

    /**
     * @return the valueMeta
     */
    public ValueMetaInterface getValueMeta()
    {
        return valueMeta;
    }

    /**
     * @param valueMeta the valueMeta to set
     */
    public void setValueMeta(ValueMetaInterface valueMeta)
    {
        this.valueMeta = valueMeta;
    }
    
    
}
