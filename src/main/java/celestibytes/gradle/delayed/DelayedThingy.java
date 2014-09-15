
package celestibytes.gradle.delayed;

import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import groovy.lang.Closure;

public class DelayedThingy extends Closure<Object>
{
    private static final long serialVersionUID = -6777882663512149880L;
    
    private Object thing;
    
    public DelayedThingy(Object thing)
    {
        super(null);
        this.thing = thing;
    }
    
    @Override
    public Object call(Object... objects)
    {
        if (thing instanceof AbstractArchiveTask)
        {
            return ((AbstractArchiveTask) thing).getArchivePath();
        }
        else if (thing instanceof PublishArtifact)
        {
            return ((PublishArtifact) thing).getFile();
        }
        
        return thing;
    }
    
    @Override
    public String toString()
    {
        return call().toString();
    }
}
