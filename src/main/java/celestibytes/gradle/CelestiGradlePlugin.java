package celestibytes.gradle;

import net.minecraftforge.gradle.CopyInto;
import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.delayed.DelayedBase;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.abstractutil.DownloadTask;
import net.minecraftforge.gradle.tasks.dev.ChangelogTask;

import celestibytes.gradle.reference.Projects;
import celestibytes.gradle.reference.Reference;
import celestibytes.pizzana.version.Version;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import groovy.lang.Closure;
import io.github.pizzana.jkaffe.util.gradle.ProjectPropertyHelper;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CelestiGradlePlugin implements Plugin<Project>, DelayedBase.IDelayedResolver<BaseExtension>
{
    private Project project;
    private String projectName;

    private boolean core;
    private String coreArtifact;
    private String coreDevArtifact;
    private String coreVersion;

    private String basePackage;
    private Closure manifest;

    private String filesmaven;

    private boolean addManifest;
    private String dir;

    private String jsonName;
    private Version version;
    private boolean isStable;

    private String minecraftVersion;

    @Override
    public void apply(Project project)
    {
        this.project = project;
        projectName = project.getName();
        jsonName = projectName.toLowerCase();
        version = Version.parse((String) project.getVersion());

        resolveProperties();
        addRepositories();
        addDependencies();
        makeProjectTasks();
        makeLifecycleTasks();
    }

    private void resolveProperties()
    {
        if (projectName.toLowerCase().equals(Projects.CORE.toLowerCase()))
        {
            core = false;
        }
        else
        {
            if (project.hasProperty("core"))
            {
                core = (Boolean) project.property("core");
            }
            else
            {
                throw new NullPointerException("No boolean property \"core\" found from the project.");
            }
        }

        if (core)
        {
            if (project.hasProperty("coreVersion"))
            {
                coreVersion = (String) project.property("coreVersion");
            }
            else
            {
                throw new NullPointerException("No String property \"coreVersion\" found from the project.");
            }

            coreArtifact = "io.github.celestibytes:CelestiCore:" + coreVersion;
            coreDevArtifact = "io.github.celestibytes:CelestiCore:" + coreVersion + ":dev";
        }

        if (project.hasProperty("basePackage"))
        {
            basePackage = (String) project.property("basePackage");
        }
        else
        {
            throw new NullPointerException("No String property \"basePackage\" found from the project.");
        }

        if (project.hasProperty("manifest"))
        {
            manifest = (Closure) project.property("manifest");
        }
        else
        {
            manifest = null;
        }

        if (project.hasProperty("filesmaven"))
        {
            filesmaven = (String) project.property("filesmaven");
        }
        else
        {
            filesmaven = ".";
        }

        if (project.hasProperty("minecraftVersion"))
        {
            minecraftVersion = (String) project.property("minecraftVersion");
        }
        else
        {
            throw new NullPointerException("No String property \"minecraftVersion\" found from the project.");
        }

        // Always last
        addManifest = manifest != null;
        dir = basePackage.replace('.', '/');
        isStable = version.isStable();
    }

    private void addRepositories()
    {
        project.allprojects(new Action<Project>()
        {
            @Override
            public void execute(Project project)
            {
                addMavenRepo(project, "cbs", Reference.MAVEN);
            }
        });
    }

    private void addDependencies()
    {
        if (core)
        {
            project.getDependencies().add("compile", delayedString("{CORE_DEV_ARTIFACT}").call());
        }
    }

    private void makeProjectTasks()
    {
        List<String> artifacts = new ArrayList<String>();

        artifacts.add("javadoc");
        artifacts.add("sources");
        artifacts.add("dev");

        if (projectName.toLowerCase().equals(Projects.CORE.toLowerCase()))
        {
            makePackageTasks(artifacts);
            makeSignTask();
        }

        artifacts.add("api");

        if (projectName.toLowerCase().equals(Projects.CW.toLowerCase()))
        {
            makeBaublesTask();

            // TODO Remove
            manifest = new Closure<Object>(project)
            {
                @Override
                public Object call()
                {
                    Manifest manifest = (Manifest) getDelegate();
                    manifest.getAttributes().put("FMLCorePlugin", delayedString(
                            "celestibytes.celestialwizardry.codechicken.core.launch.DepLoader").call());
                    manifest.getAttributes().put("FMLCorePluginContainsFMLMod", delayedString("true").call());
                    return null;
                }
            };

            makePackageTasks(artifacts);
            makeSignTask();
        }

        if (projectName.toLowerCase().equals(Projects.DGC.toLowerCase()))
        {
            makePackageTasks(artifacts);
            makeSignTask();
        }
    }

    private void makeBaublesTask()
    {
        try
        {
            String baublesMc = ProjectPropertyHelper.Source.getCWVersion(project, "BAUBLES_MC");
            String baubles = ProjectPropertyHelper.Source.getCWVersion(project, "BAUBLES");
            String baublesFile = "Baubles-deobf-" + baublesMc + "-" + baubles + ".jar";
            String baublesRoot = ProjectPropertyHelper.Source.getProperty(project, "src/main/java/" + dir
                    + "/reference/Reference.java", "BAUBLES_ROOT");
            String baublesUrl = baublesRoot + baublesFile;
            final String baublesDest = "libs/" + baublesFile;

            File[] files = delayedFile("libs").call().listFiles();
            List<File> baubs = new ArrayList<File>();
            boolean hasUpToDateBaubles = false;

            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isFile())
                    {
                        if (file.getName().contains("Baubles"))
                        {
                            if (file.getName().equals(baublesFile))
                            {
                                hasUpToDateBaubles = true;
                            }
                            else
                            {
                                baubs.add(file);
                            }
                        }
                    }
                }
            }

            Delete cleanEveryBaubles = makeTask("cleanEveryBaubles", Delete.class);

            for (File file : baubs)
            {
                cleanEveryBaubles.delete(file);
            }

            if (hasUpToDateBaubles)
            {
                cleanEveryBaubles.delete(delayedFile(baublesDest).call());
            }

            cleanEveryBaubles.setDescription(
                    "Deletes all of the libraries containing \'Baubles\' in their name from the \'libs\' directory");
            cleanEveryBaubles.setGroup(Reference.NAME);

            Delete cleanBaubles = makeTask("cleanBaubles", Delete.class);

            for (File file : baubs)
            {
                cleanBaubles.delete(file);
            }

            cleanBaubles.setDescription(
                    "Deletes all of the libraries containing \'Baubles\' in their name from the \'libs\' directory " +
                            "(excluding the up-to-date one)");
            cleanBaubles.setGroup(Reference.NAME);

            DownloadTask getBaubles = makeTask("getBaubles", DownloadTask.class);
            getBaubles.setUrl(delayedString(baublesUrl));
            getBaubles.setOutput(delayedFile(baublesDest));
            getBaubles.getOutputs().upToDateWhen(new Spec<Task>()
            {
                @Override
                public boolean isSatisfiedBy(Task task)
                {
                    DelayedFile excepted = delayedFile(baublesDest);
                    return excepted.call().exists() && !excepted.call().isDirectory();
                }
            });
            getBaubles.setDescription("Downloads the correct version of Baubles");
            getBaubles.setGroup(Reference.NAME);
            getBaubles.dependsOn(cleanBaubles);

            project.getTasks().getByName("extractUserDev").dependsOn(getBaubles);
        }
        catch (IOException e)
        {
            project.getLogger().warn("Failed to get baubles properties");
            e.printStackTrace();
        }
    }

    private void makePackageTasks(List<String> artifacts)
    {
        String changelogFile = "{BUILD_DIR}/libs/" + project.getName() + "-" + project.getVersion() + "-changelog.txt";

        ChangelogTask changelog = makeTask("createChangelog", ChangelogTask.class);

        changelog.setServerRoot(delayedString("{JENKINS_SERVER}"));
        changelog.setJobName(delayedString("{JENKINS_JOB}"));
        changelog.setAuthName(delayedString("{JENKINS_AUTH_NAME}"));
        changelog.setAuthPassword(delayedString("{JENKINS_AUTH_PASSWORD}"));
        changelog.setTargetBuild(delayedString("{BUILD_NUM}"));
        changelog.setOutput(delayedFile(changelogFile));
        changelog.dependsOn("classes", "processResources");

        project.getTasks().getByName("build").dependsOn(changelog);

        Jar jar = (Jar) project.getTasks().getByName("jar");

        if (addManifest)
        {
            jar.manifest(manifest);
        }

        jar.dependsOn(changelog);

        if (artifacts.contains("javadoc"))
        {
            Jar javadocJar = makeTask("javadocJar", Jar.class);
            javadocJar.setClassifier("javadoc");
            javadocJar.from(delayedFile("{BUILD_DIR}/docs/javadoc/"));
            javadocJar.dependsOn(jar, "javadoc");
            javadocJar.setExtension("jar");
            project.getArtifacts().add("archives", javadocJar);
        }

        if (artifacts.contains("sources") || artifacts.contains("src"))
        {
            Jar sourcesJar = makeTask("sourcesJar", Jar.class);
            sourcesJar.setClassifier("sources");
            sourcesJar.from(delayedFile(changelogFile));
            sourcesJar.from(delayedFile("LICENSE"));
            sourcesJar.from(delayedFile("build.gradle"));
            sourcesJar.from(delayedFile("settings.gradle"));
            sourcesJar.from(delayedFile("{BUILD_DIR}/sources/java/"));
            sourcesJar.from(delayedFile("{BUILD_DIR}/resources/main/"));
            sourcesJar.from(delayedFile("gradlew"));
            sourcesJar.from(delayedFile("gradlew.bat"));
            sourcesJar.from(delayedFile("gradle/wrapper"), new CopyInto("gradle/wrapper"));
            sourcesJar.dependsOn(jar);
            sourcesJar.setExtension("jar");
            project.getArtifacts().add("archives", sourcesJar);
        }

        if (artifacts.contains("api"))
        {
            String apiDir = dir + "/api/";

            Jar apiJar = makeTask("apiJar", Jar.class);
            apiJar.setClassifier("api");
            apiJar.from(delayedFile("{BUILD_DIR}/sources/java/" + apiDir), new CopyInto(apiDir));
            apiJar.dependsOn(jar);
            apiJar.setExtension("jar");
            project.getArtifacts().add("archives", apiJar);
        }

        if (artifacts.contains("dev") || artifacts.contains("deobf"))
        {
            Jar devJar = makeTask("devJar", Jar.class);
            devJar.setClassifier("dev");
            devJar.from(delayedFile(changelogFile));
            devJar.from(delayedFile("LICENSE"));
            devJar.from(delayedFile("{BUILD_DIR}/classes/main/"));
            devJar.from(delayedFile("{BUILD_DIR}/classes/api/"));
            devJar.from(delayedFile("{BUILD_DIR}/resources/main/"));

            if (addManifest)
            {
                devJar.manifest(manifest);
            }

            devJar.dependsOn(jar);
            devJar.setExtension("jar");
            project.getArtifacts().add("archives", devJar);
        }
    }

    private void makeSignTask()
    {
        final Jar jar = (Jar) project.getTasks().getByName("jar");
        final File jarPath = jar.getArchivePath();
        final File keystoreLocation = project.file(project.getProperties().get("keystore_location"));
        final String keystoreAlias = (String) project.getProperties().get("keystore_alias");
        final String keystorePassword = (String) project.getProperties().get("keystore_password");

        DefaultTask signJar = makeTask("signJar");
        signJar.getInputs().file(jarPath);
        signJar.getInputs().file(keystoreLocation);
        signJar.getInputs().property("keystore_alias", keystoreAlias);
        signJar.getInputs().property("keystore_password", keystorePassword);
        signJar.getOutputs().file(jarPath);
        signJar.onlyIf(new Spec<Task>()
        {
            @Override
            public boolean isSatisfiedBy(Task task)
            {
                return !keystoreLocation.getPath().equals(".");
            }
        });
        signJar.doLast(new Action<Task>()
        {
            @Override
            public void execute(Task task)
            {
                Map<String, String> args = Maps.newHashMap();
                args.put("destDir", jar.getDestinationDir().getPath());
                args.put("jar", jarPath.getPath());
                args.put("keystore", keystoreLocation.getPath());
                args.put("alias", keystoreAlias);
                args.put("storepass", keystorePassword);
                invokeAnt("signjar", args);
            }
        });
        signJar.dependsOn("build");

        project.getTasks().getByName("uploadArchives").dependsOn(signJar);
    }

    private void makeLifecycleTasks()
    {
        DefaultTask processJson = makeTask("processJson");
        processJson.doLast(new Action<Task>()
        {
            @Override
            public void execute(Task task)
            {
                try
                {
                    URLConnection urlConnection = new URL(Reference.MAVEN + "data.json").openConnection();

                    urlConnection.setRequestProperty("User-Agent", System.getProperty("java.version"));
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();

                    String json = new String(ByteStreams.toByteArray(inputStream));

                    inputStream.close();

                    Map<String, Object> data = new Gson().fromJson(json, Map.class);

                    processMaps(data);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
        processJson.dependsOn("uploadArchives");

        DefaultTask release = makeTask("release");
        release.setDescription("Wrapper task for building release-ready archives.");
        release.setGroup(Reference.NAME);
        release.dependsOn(processJson);
    }

    private void processMaps(Map<String, Object> data) throws IOException
    {
        if (!data.containsKey(jsonName) || (data.containsKey(jsonName) && !(data.get(jsonName) instanceof Map)))
        {
            data.remove(jsonName);

            Map<String, Object> modNode = newMap();
            Map<String, Object> minecraftNode = newMap();
            Map<String, Object> channelsNode = newMap();
            Map<String, Object> stable = newMap();
            Map<String, Object> latest = newMap();

            // TODO Description
            if (isStable)
            {
                stable.put("major", version.major);
                stable.put("minor", version.minor);
                stable.put("patch", version.patch);
                stable.put("description", "");

                latest.put("major", 0);
                latest.put("minor", 0);
                latest.put("patch", 0);
                latest.put("postfix", null);
                latest.put("number", 0);
                latest.put("description", "");
            }
            else
            {
                latest.put("major", version.major);
                latest.put("minor", version.minor);
                latest.put("patch", version.patch);
                latest.put("postfix", version.getChannel().getKey());
                latest.put("number", version.number);
                latest.put("description", "");

                stable.put("major", 0);
                stable.put("minor", 0);
                stable.put("patch", 0);
                stable.put("description", "");
            }

            channelsNode.put("stable", stable);
            channelsNode.put("latest", latest);

            minecraftNode.put("channels", channelsNode);

            modNode.put(minecraftVersion, minecraftNode);

            data.put(jsonName, modNode);
        }
        else
        {
            Map<String, Object> modNode = (Map<String, Object>) data.get(jsonName);

            if (!modNode.containsKey(minecraftVersion) || (modNode.containsKey(minecraftVersion) && !(modNode.get(minecraftVersion) instanceof Map)))
            {
                modNode.remove(minecraftVersion);

                Map<String, Object> minecraftNode = newMap();
                Map<String, Object> channelsNode = newMap();
                Map<String, Object> stable = newMap();
                Map<String, Object> latest = newMap();

                // TODO Description
                if (isStable)
                {
                    stable.put("major", version.major);
                    stable.put("minor", version.minor);
                    stable.put("patch", version.patch);
                    stable.put("description", "");

                    latest.put("major", 0);
                    latest.put("minor", 0);
                    latest.put("patch", 0);
                    latest.put("postfix", null);
                    latest.put("number", 0);
                    latest.put("description", "");
                }
                else
                {
                    latest.put("major", version.major);
                    latest.put("minor", version.minor);
                    latest.put("patch", version.patch);
                    latest.put("postfix", version.getChannel().getKey());
                    latest.put("number", version.number);
                    latest.put("description", "");

                    stable.put("major", 0);
                    stable.put("minor", 0);
                    stable.put("patch", 0);
                    stable.put("description", "");
                }

                channelsNode.put("stable", stable);
                channelsNode.put("latest", latest);

                minecraftNode.put("channels", channelsNode);

                modNode.put(minecraftVersion, minecraftNode);

                data.put(jsonName, modNode);
            }
            else
            {
                Map<String, Object> minecraftNode = (Map<String, Object>) modNode.get(minecraftVersion);

                if (!minecraftNode.containsKey("channels") || (minecraftNode.containsKey("channels") && !(minecraftNode.get("channels") instanceof Map)))
                {
                    minecraftNode.remove("channels");

                    Map<String, Object> channelsNode = newMap();
                    Map<String, Object> stable = newMap();
                    Map<String, Object> latest = newMap();

                    // TODO Description
                    if (isStable)
                    {
                        stable.put("major", version.major);
                        stable.put("minor", version.minor);
                        stable.put("patch", version.patch);
                        stable.put("description", "");

                        latest.put("major", 0);
                        latest.put("minor", 0);
                        latest.put("patch", 0);
                        latest.put("postfix", null);
                        latest.put("number", 0);
                        latest.put("description", "");
                    }
                    else
                    {
                        latest.put("major", version.major);
                        latest.put("minor", version.minor);
                        latest.put("patch", version.patch);
                        latest.put("postfix", version.getChannel().getKey());
                        latest.put("number", version.number);
                        latest.put("description", "");

                        stable.put("major", 0);
                        stable.put("minor", 0);
                        stable.put("patch", 0);
                        stable.put("description", "");
                    }

                    channelsNode.put("stable", stable);
                    channelsNode.put("latest", latest);

                    minecraftNode.put("channels", channelsNode);

                    modNode.put(minecraftVersion, minecraftNode);

                    data.put(jsonName, modNode);
                }
                else
                {
                    Map<String, Object> channelsNode = (Map<String, Object>) minecraftNode.get("channels");

                    if (!channelsNode.containsKey("stable") || (channelsNode.containsKey("stable") && !(channelsNode.get("stable") instanceof Map)))
                    {
                        channelsNode.remove("stable");

                        Map<String, Object> stable = newMap();

                        // TODO Description
                        if (isStable)
                        {
                            stable.put("major", version.major);
                            stable.put("minor", version.minor);
                            stable.put("patch", version.patch);
                            stable.put("description", "");
                        }
                        else
                        {
                            stable.put("major", 0);
                            stable.put("minor", 0);
                            stable.put("patch", 0);
                            stable.put("description", "");
                        }

                        channelsNode.put("stable", stable);

                        minecraftNode.put("channels", channelsNode);

                        modNode.put(minecraftVersion, minecraftNode);

                        data.put(jsonName, modNode);
                    }
                    else
                    {
                        // TODO Description
                        if (isStable)
                        {
                            Map<String, Object> stable = (Map<String, Object>) channelsNode.get("stable");

                            stable.put("major", version.major);
                            stable.put("minor", version.minor);
                            stable.put("patch", version.patch);
                            stable.put("description", "");

                            channelsNode.put("stable", stable);

                            minecraftNode.put("channels", channelsNode);

                            modNode.put(minecraftVersion, minecraftNode);

                            data.put(jsonName, modNode);
                        }
                    }

                    if (!channelsNode.containsKey("latest") || (channelsNode.containsKey("latest") && !(channelsNode.get("latest") instanceof Map)))
                    {
                        channelsNode.remove("latest");

                        Map<String, Object> latest = newMap();

                        // TODO Description
                        if (isStable)
                        {
                            latest.put("major", 0);
                            latest.put("minor", 0);
                            latest.put("patch", 0);
                            latest.put("postfix", null);
                            latest.put("number", 0);
                            latest.put("description", "");
                        }
                        else
                        {
                            latest.put("major", version.major);
                            latest.put("minor", version.minor);
                            latest.put("patch", version.patch);
                            latest.put("postfix", version.getChannel().getKey());
                            latest.put("number", version.number);
                            latest.put("description", "");
                        }

                        channelsNode.put("latest", latest);

                        minecraftNode.put("channels", channelsNode);

                        modNode.put(minecraftVersion, minecraftNode);

                        data.put(jsonName, modNode);
                    }
                    else
                    {
                        // TODO Description
                        if (!isStable)
                        {
                            Map<String, Object> latest = (Map<String, Object>) channelsNode.get("latest");

                            latest.put("major", version.major);
                            latest.put("minor", version.minor);
                            latest.put("patch", version.patch);
                            latest.put("postfix", version.getChannel().getKey());
                            latest.put("number", version.number);
                            latest.put("description", "");

                            channelsNode.put("latest", latest);

                            minecraftNode.put("channels", channelsNode);

                            modNode.put(minecraftVersion, minecraftNode);

                            data.put(jsonName, modNode);
                        }
                    }
                }
            }
        }

        Map<String, String> args = Maps.newHashMap();
        args.put("file", filesmaven + "/data.json");
        invokeAnt("delete", args);

        File json = project.file(filesmaven + "/data.json");

        FileUtils.writeStringToFile(json, new Gson().toJson(data));
    }

    private <K, V> Map<K, V> newMap()
    {
        return new HashMap<K, V>();
    }

    public DefaultTask makeTask(String name)
    {
        return makeTask(name, DefaultTask.class);
    }

    public <T extends Task> T makeTask(String name, Class<T> type)
    {
        return makeTask(project, name, type);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Task> T makeTask(Project proj, String name, Class<T> type)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("type", type);
        return (T) proj.task(map, name);
    }

    public void invokeAnt(String task, Map<String, String> args)
    {
        project.getAnt().invokeMethod(task, args);
    }

    public void applyExternalPlugin(String plugin)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("plugin", plugin);
        project.apply(map);
    }

    public MavenArtifactRepository addMavenRepo(Project project, final String name, final String url)
    {
        return project.getRepositories().maven(new Action<MavenArtifactRepository>()
        {
            @Override
            public void execute(MavenArtifactRepository repo)
            {
                repo.setName(name);
                repo.setUrl(url);
            }
        });
    }

    public FlatDirectoryArtifactRepository addFlatRepo(Project project, final String name, final Object... dirs)
    {
        return project.getRepositories().flatDir(new Action<FlatDirectoryArtifactRepository>()
        {
            @Override
            public void execute(FlatDirectoryArtifactRepository repo)
            {
                repo.setName(name);
                repo.dirs(dirs);
            }
        });
    }

    protected DelayedString delayedString(String path)
    {
        return new DelayedString(project, path, this);
    }

    protected DelayedFile delayedFile(String path)
    {
        return new DelayedFile(project, path, this);
    }

    protected DelayedFileTree delayedFileTree(String path)
    {
        return new DelayedFileTree(project, path, this);
    }

    protected DelayedFileTree delayedZipTree(String path)
    {
        return new DelayedFileTree(project, path, true, this);
    }

    @Override
    public String resolve(String pattern, Project project, BaseExtension extension)
    {
        pattern = pattern.replace("{CORE_ARTIFACT}", coreArtifact);
        pattern = pattern.replace("{CORE_DEV_ARTIFACT}", coreDevArtifact);
        pattern = pattern.replace("{PATH}", project.getPath().replace('\\', '/'));
        pattern = pattern.replace("{CORE_VERSION}", coreVersion);
        pattern = pattern.replace("{CORE_NAME}", Projects.CORE);
        return pattern;
    }
}
