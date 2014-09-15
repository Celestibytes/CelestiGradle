
package celestibytes.gradle.delayed;

import org.gradle.api.Project;

public class DelayedString extends DelayedBase<String>
{
    private static final long serialVersionUID = 751150650008864108L;
    
    public DelayedString(Project owner, String pattern)
    {
        super(owner, pattern);
    }
    
    public DelayedString(Project owner, String pattern, IDelayedResolver... resolvers)
    {
        super(owner, pattern, resolvers);
    }
    
    @Override
    public String resolveDelayed()
    {
        return DelayedBase.resolve(pattern, project, resolvers);
    }
    
    public DelayedString forceResolving()
    {
        resolveOnce = false;
        return this;
    }
}
