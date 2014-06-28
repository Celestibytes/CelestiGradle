package net.minecraftforge.gradle.user.cg;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BaublesPlugin implements Plugin<Project>
{
    public Project project;

    @Override
    public void apply(Project project)
    {
        this.project = project;
    }
}
