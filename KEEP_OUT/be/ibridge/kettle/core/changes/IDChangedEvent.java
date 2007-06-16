package be.ibridge.kettle.core.changes;


/**
 * In case the ID of an Object changes (name, ID, in general the unique identifier) this is the event that is fired.
 * 
 * @author Matt
 * @since 2007-03-20
 */
public class IDChangedEvent
{
    public String oldID;
    public String newID;
    
    public Object object;

    /**
     * @param oldID
     * @param newID
     * @param object
     */
    public IDChangedEvent(String oldID, String newID, Object object)
    {
        this.oldID = oldID;
        this.newID = newID;
        this.object = object;
    }    
}
