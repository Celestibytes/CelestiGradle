package net.minecraftforge.gradle.user.cg;

import net.minecraftforge.gradle.CopyInto;
import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.dev.ChangelogTask;

import groovy.lang.Closure;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.tasks.bundling.Jar;

@Deprecated
public class CWPackagePlugin implements Plugin<Project>
{
    public Project project;

    @Override
    public void apply(Project project)
    {
        this.project = project;
        createPackageTasks();
    }

    private void createPackageTasks()
    {
        String changelogFile = "{BUILD_DIR}/libs/" + project.getName() + "-" + project.getVersion() + "-changelog.txt";

        ChangelogTask changelog = makeTask("createChangelog", ChangelogTask.class);

        changelog.setServerRoot(delayedString("{JENKINS_SERVER}"));
        changelog.setJobName(delayedString("{JENKINS_JOB}"));
        changelog.setAuthName(delayedString("{JENKINS_AUTH_NAME}"));
        changelog.setAuthPassword(delayedString("{JENKINS_AUTH_PASSWORD}"));
        changelog.setTargetBuild(delayedString("{BUILD_NUM}"));
        changelog.setOutput(delayedFile(changelogFile));

        project.getTasks().getByName("build").dependsOn(changelog);

        Closure<Object> commonManifest = new Closure<Object>(project)
        {
            @Override
            public Object call()
            {
                Manifest manifest = (Manifest) getDelegate();
                manifest.getAttributes().put("FMLCorePlugin",
                                             delayedString("celestialwizardry.codechicken.core.launch.DepLoader")
                                                     .call());
                manifest.getAttributes().put("FMLCorePluginContainsFMLMod", delayedString("true").call());
                return null;
            }
        };

        Jar jarTask = (Jar) project.getTasks().getByName("jar");
        jarTask.manifest(commonManifest);
        jarTask.dependsOn(changelog);

        Jar javadoc = makeTask("javadocJar", Jar.class);
        javadoc.setClassifier("javadoc");
        javadoc.from(delayedFile("{BUILD_DIR}/docs/javadoc/"));
        javadoc.dependsOn(jarTask, "javadoc");
        javadoc.setExtension("jar");
        project.getArtifacts().add("archives", javadoc);

        Jar sources = makeTask("sourcesJar", Jar.class);
        sources.setClassifier("sources");
        sources.from(delayedFile(changelogFile));
        sources.from(delayedFile("LICENSE"));
        sources.from(delayedFile("build.gradle"));
        sources.from(delayedFile("settings.gradle"));
        sources.from(delayedFile("{BUILD_DIR}/sources/java/"));
        sources.from(delayedFile("{BUILD_DIR}/resources/main/"));
        sources.from(delayedFile("gradlew"));
        sources.from(delayedFile("gradlew.bat"));
        sources.from(delayedFile("gradle/wrapper"), new CopyInto("gradle/wrapper"));
        sources.dependsOn(jarTask);
        sources.setExtension("jar");
        project.getArtifacts().add("archives", sources);

        Jar api = makeTask("apiJar", Jar.class);
        api.setClassifier("api");
        api.from(delayedFile("{BUILD_DIR}/sources/java/celestialwizardry/api/"),
                 new CopyInto("celestialwizardry/api/"));
        api.from(delayedFile("{BUILD_DIR}/sources/java/celestialwizardry/crystal/api/"),
                 new CopyInto("celestialwizardry/crystal/api/"));
        api.dependsOn(jarTask);
        api.setExtension("jar");
        project.getArtifacts().add("archives", api);

        Jar deobf = makeTask("deobfJar", Jar.class);
        deobf.setClassifier("deobf");
        sources.from(delayedFile(changelogFile));
        sources.from(delayedFile("LICENSE"));
        deobf.from(delayedFile("{BUILD_DIR}/classes/main/"));
        deobf.from(delayedFile("{BUILD_DIR}/classes/api/"));
        deobf.manifest(commonManifest);
        deobf.dependsOn(jarTask);
        deobf.setExtension("jar");
        project.getArtifacts().add("archives", deobf);
    }

    public DefaultTask makeTask(String name)
    {
        return makeTask(name, DefaultTask.class);
    }

    public <T extends Task> T makeTask(String name, Class<T> type)
    {
        return BasePlugin.makeTask(project, name, type);
    }

    protected DelayedString delayedString(String path)
    {
        return new DelayedString(project, path);
    }

    protected DelayedFile delayedFile(String path)
    {
        return new DelayedFile(project, path);
    }

    protected DelayedFileTree delayedFileTree(String path)
    {
        return new DelayedFileTree(project, path);
    }

    protected DelayedFileTree delayedZipTree(String path)
    {
        return new DelayedFileTree(project, path, true);
    }
}
