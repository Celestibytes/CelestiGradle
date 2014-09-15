package net.minecraftforge.gradle.common;

import org.gradle.api.Project;

public class JenkinsExtension
{
    private String server = "http://localhost:8080/";
    private String job;
    private String authName = "console_script";
    private String authPassword = "5a5a019fecd7f10b9f4109c6f94172791397228d";

    public JenkinsExtension(Project project)
    {
        job = System.getenv("JOB_NAME") == null ? project.getName() : System.getenv("JOB_NAME");
    }

    public String getServer()
    {
        return server;
    }

    public void setServer(String server)
    {
        this.server = server;
    }

    public String getJob()
    {
        return job;
    }

    public void setJob(String job)
    {
        this.job = job;
    }

    public String getAuthName()
    {
        return authName;
    }

    public void setAuthName(String authName)
    {
        this.authName = authName;
    }

    public String getAuthPassword()
    {
        return authPassword;
    }

    public void setAuthPassword(String authPassword)
    {
        this.authPassword = authPassword;
    }
}
