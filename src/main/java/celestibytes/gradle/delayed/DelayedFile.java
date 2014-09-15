
package celestibytes.gradle.delayed;

import org.gradle.api.Project;

import java.io.File;

public class DelayedFile extends DelayedBase<File>
{
    private static final long serialVersionUID = 7680683126322416778L;
    
    public DelayedFile(Project owner, String pattern)
    {
        super(owner, pattern);
    }
    
    public DelayedFile(Project owner, String pattern, IDelayedResolver... resolvers)
    {
        super(owner, pattern, resolvers);
    }
    
    @Override
    public File resolveDelayed()
    {
        return project.file(DelayedBase.resolve(pattern, project, resolvers));
    }
    
    public DelayedFileTree toZipTree()
    {
        return new DelayedFileTree(project, pattern, true, resolvers);
    }
    
    public DelayedFile forceResolving()
    {
        resolveOnce = false;
        return this;
    }
}
