package org.pentaho.di.shared;

public class SharedObjectBase
{
    private boolean shared;
        
    public boolean isShared()
    {
        return shared;
    }

    public void setShared(boolean shared)
    {
        this.shared = shared;
    }
}
