package org.pentaho.di.trans.steps.systemdata;

public class SystemDataMetaFunction
{
    int    type;
    String code;
    String description;
    
    /**
     * @param type
     * @param code
     * @param description
     */
    public SystemDataMetaFunction(int type, String code, String description)
    {
        super();
        this.type = type;
        this.code = code;
        this.description = description;
    }

    /**
     * @return the code
     */
    public String getCode()
    {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code)
    {
        this.code = code;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
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
    
    
}
