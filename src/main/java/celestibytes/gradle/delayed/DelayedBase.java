
package celestibytes.gradle.delayed;

import org.gradle.api.Project;

import groovy.lang.Closure;

public abstract class DelayedBase<V> extends Closure<V>
{
    private static final long serialVersionUID = 8355106838557170647L;
    
    protected Project project;
    private V resolved;
    protected String pattern;
    public boolean resolveOnce = true;
    protected IDelayedResolver[] resolvers;
    
    public static final IDelayedResolver RESOLVER = new IDelayedResolver()
    {
        @Override
        public String resolve(String pattern, Project project)
        {
            return pattern;
        }
    };
    
    public DelayedBase(Project owner, String pattern)
    {
        this(owner, pattern, RESOLVER);
    }
    
    public DelayedBase(Project owner, String pattern, IDelayedResolver... resolvers)
    {
        super(owner);
        this.project = owner;
        this.pattern = pattern;
        this.resolvers = resolvers;
    }
    
    public abstract V resolveDelayed();
    
    @Override
    public final V call()
    {
        if (resolved == null || !resolveOnce)
        {
            resolved = resolveDelayed();
        }
        
        return resolved;
    }
    
    @Override
    public String toString()
    {
        return call().toString();
    }
    
    // interface
    public static interface IDelayedResolver
    {
        public String resolve(String pattern, Project project);
    }
    
    public static String resolve(String patern, Project project, IDelayedResolver... resolvers)
    {
        project.getLogger().info("Resolving: " + patern);
        
        String build = "0";
        
        if (System.getenv().containsKey("BUILD_NUMBER"))
        {
            build = System.getenv("BUILD_NUMBER");
        }
        
        // resolvers first
        for (IDelayedResolver r : resolvers)
        {
            patern = r.resolve(patern, project);
        }
        
        patern = patern.replace("{CACHE_DIR}",
                project.getGradle().getGradleUserHomeDir().getAbsolutePath().replace('\\', '/') + "/caches");
        patern = patern.replace("{BUILD_DIR}", project.getBuildDir().getAbsolutePath().replace('\\', '/'));
        patern = patern.replace("{BUILD_NUM}", build);
        patern = patern.replace("{PROJECT}", project.getName());
        
        patern = patern.replace("{JENKINS_SERVER}", "http://localhost:8080/");
        patern = patern.replace("{JENKINS_JOB}",
                System.getenv("JOB_NAME") == null ? project.getName() : System.getenv("JOB_NAME"));
        patern = patern.replace("{JENKINS_AUTH_NAME}", "console_script");
        patern = patern.replace("{JENKINS_AUTH_PASSWORD}", "5a5a019fecd7f10b9f4109c6f94172791397228d");
        
        project.getLogger().info("Resolved:  " + patern);
        
        return patern;
    }
}
