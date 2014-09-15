
package celestibytes.gradle.delayed;

import org.gradle.api.Project;

public class DelayedObject extends DelayedBase<Object>
{
    private static final long serialVersionUID = 4766042704072994689L;
    
    Object obj;
    
    public DelayedObject(Object obj, Project owner)
    {
        super(owner, "");
        this.obj = obj;
    }
    
    @Override
    public Object resolveDelayed()
    {
        return obj;
    }
    
}
