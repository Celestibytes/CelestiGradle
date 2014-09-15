
package celestibytes.gradle.delayed;

import celestibytes.gradle.util.ZipFileTree;

import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.collections.FileTreeAdapter;

public class DelayedFileTree extends DelayedBase<FileTree>
{
    private static final long serialVersionUID = -4804556622609889787L;
    
    private boolean zipTree = false;
    
    public DelayedFileTree(Project owner, String pattern)
    {
        super(owner, pattern);
    }
    
    public DelayedFileTree(Project owner, String pattern, boolean zipTree)
    {
        super(owner, pattern);
        this.zipTree = zipTree;
    }
    
    public DelayedFileTree(Project owner, String pattern, IDelayedResolver... resolvers)
    {
        super(owner, pattern, resolvers);
    }
    
    public DelayedFileTree(Project owner, String pattern, boolean zipTree, IDelayedResolver... resolvers)
    {
        super(owner, pattern, resolvers);
        this.zipTree = zipTree;
    }
    
    @Override
    public FileTree resolveDelayed()
    {
        if (zipTree)
        {
            // resolved = project.zipTree(DelayedString.resolve(pattern,
            // project, resolvers));
            return new FileTreeAdapter(new ZipFileTree(project.file(DelayedBase.resolve(pattern, project, resolvers))));
        }
        else
        {
            return project.fileTree(DelayedBase.resolve(pattern, project, resolvers));
        }
    }
}
