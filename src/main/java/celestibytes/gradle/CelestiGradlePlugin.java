package celestibytes.gradle;

import net.minecraftforge.gradle.CopyInto;
import net.minecraftforge.gradle.FileLogListenner;
import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedBase;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.abstractutil.DownloadTask;
import net.minecraftforge.gradle.tasks.dev.ChangelogTask;

import celestibytes.gradle.reference.Projects;
import celestibytes.gradle.reference.Reference;
import celestibytes.gradle.reference.Versions;
import celestibytes.pizzana.derp.DerpException;
import celestibytes.pizzana.version.Version;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import groovy.lang.Closure;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
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

/**
 * TODO Version check json handling
 */
public final class CelestiGradlePlugin implements Plugin<Project>, DelayedBase.IDelayedResolver<BaseExtension>
{
    private static final String CHANNELS = "channels";
    private static final String STABLE = "stable";
    private static final String LATEST = "latest";
    private static final String MAJOR = "major";
    private static final String MINOR = "minor";
    private static final String PATCH = "patch";
    private static final String POSTFIX = "postfix";
    private static final String NUMBER = "number";
    private static final String DESCRIPTION = "description";

    private static Project projectStatic;
    private static boolean fg;

    private static String versionNumber;

    private static boolean isMinecraftMod = false;
    private static String minecraftVersion;
    private static boolean needsCore = false;
    private static String coreVersion;

    private static String basePackage;
    private static String dir;

    private static List<String> artifactsList = new ArrayList<String>();

    private static Closure manifest;
    private static boolean hasManifest = false;

    private Project project;
    private String projectName;
    private String jsonName;

    private Version version;

    private String coreArtifact;
    private String coreDevArtifact;

    private String filesmaven;

    private boolean hasKeystore;

    private boolean isStable;

    @Override
    public void apply(Project arg)
    {
        project = arg;

        projectStatic = project;
        fg = project.getPlugins().hasPlugin("forge");
        projectName = project.getName();
        jsonName = projectName.toLowerCase();

        FileLogListenner listener = new FileLogListenner(project.file(Constants.LOG));
        project.getLogging().addStandardOutputListener(listener);
        project.getLogging().addStandardErrorListener(listener);
        project.getGradle().addBuildListener(listener);

        applyPlugins();
        addRepositories();
        resolveProperties();

        if (!fg)
        {
            displayBanner();
        }

        addDependencies();
        makeProjectTasks();
        makeLifecycleTasks();
    }

    private void resolveProperties()
    {
        if (versionNumber == null)
        {
            throw new ProjectConfigurationException("You must set the version number!", new NullPointerException());
        }

        version = Version.parse(versionNumber);

        if (fg && !isMinecraftMod)
        {
            throw new ProjectConfigurationException("Project has forge plugin but isn't a Minecraft mod!",
                                                    new DerpException());
        }

        if (isMinecraftMod)
        {
            if (needsCore)
            {
                if (coreVersion == null)
                {
                    throw new ProjectConfigurationException("You must set the core version number!",
                                                            new NullPointerException());
                }

                coreArtifact = "io.github.celestibytes:CelestiCore:" + coreVersion;
                coreDevArtifact = "io.github.celestibytes:CelestiCore:" + coreVersion + ":dev";
            }

            if (minecraftVersion == null)
            {
                throw new ProjectConfigurationException("You must set the minecraft version number!",
                                                        new NullPointerException());
            }
        }

        if (basePackage == null)
        {
            throw new ProjectConfigurationException("You must set the base package!", new NullPointerException());
        }

        if (project.hasProperty("filesmaven"))
        {
            filesmaven = (String) project.property("filesmaven");
        }
        else
        {
            filesmaven = "./files";
        }

        hasKeystore = project.hasProperty("keystoreLocation");

        isStable = version.isStable();
    }

    private void applyPlugins()
    {
        if (!fg)
        {
            applyExternalPlugin("java");
            applyExternalPlugin("maven");
            applyExternalPlugin("eclipse");
            applyExternalPlugin("idea");
        }
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
        if (needsCore)
        {
            project.getDependencies().add("compile", delayedString("{CORE_DEV_ARTIFACT}").call());
        }
    }

    private void makeProjectTasks()
    {
        if (projectName.toLowerCase().equals(Projects.CORE.toLowerCase()))
        {
            makePackageTasks();
            makeSignTask();
        }

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

            makePackageTasks();
            makeSignTask();
        }

        if (projectName.toLowerCase().equals(Projects.DGC.toLowerCase()))
        {
            makePackageTasks();
            makeSignTask();
        }
    }

    private void makeBaublesTask()
    {
        try
        {
            String baublesMc = getCWVersion(project, "BAUBLES_MC");
            String baubles = getCWVersion(project, "BAUBLES");
            String baublesFile = "Baubles-deobf-" + baublesMc + "-" + baubles + ".jar";
            String baublesRoot = getProperty(project, "src/main/java/" + dir + "/reference/Reference.java",
                                             "BAUBLES_ROOT");
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

    private void makePackageTasks()
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

        if (hasManifest)
        {
            jar.manifest(manifest);
        }

        jar.dependsOn(changelog);

        if (artifactsList.contains("javadoc"))
        {
            Jar javadocJar = makeTask("javadocJar", Jar.class);
            javadocJar.setClassifier("javadoc");
            javadocJar.from(delayedFile("{BUILD_DIR}/docs/javadoc/"));
            javadocJar.dependsOn(jar, "javadoc");
            javadocJar.setExtension("jar");
            project.getArtifacts().add("archives", javadocJar);
        }

        if (artifactsList.contains("sources") || artifactsList.contains("src"))
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

        if (artifactsList.contains("api"))
        {
            String apiDir = dir + "/api/";

            Jar apiJar = makeTask("apiJar", Jar.class);
            apiJar.setClassifier("api");
            apiJar.from(delayedFile("{BUILD_DIR}/sources/java/" + apiDir), new CopyInto(apiDir));
            apiJar.dependsOn(jar);
            apiJar.setExtension("jar");
            project.getArtifacts().add("archives", apiJar);
        }

        if (artifactsList.contains("dev") || artifactsList.contains("deobf"))
        {
            Jar devJar = makeTask("devJar", Jar.class);
            devJar.setClassifier("dev");
            devJar.from(delayedFile(changelogFile));
            devJar.from(delayedFile("LICENSE"));
            devJar.from(delayedFile("{BUILD_DIR}/classes/main/"));
            devJar.from(delayedFile("{BUILD_DIR}/classes/api/"));
            devJar.from(delayedFile("{BUILD_DIR}/resources/main/"));

            if (hasManifest)
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
        DefaultTask signJar = makeTask("signJar");

        if (hasKeystore)
        {
            final Jar jar = (Jar) project.getTasks().getByName("jar");
            final File jarPath = jar.getArchivePath();
            final File keystoreLocation = project.file(project.getProperties().get("keystore_location"));
            final String keystoreAlias = (String) project.getProperties().get("keystore_alias");
            final String keystorePassword = (String) project.getProperties().get("keystore_password");

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
                    return hasKeystore;
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
        }

        signJar.dependsOn("build");

        project.getTasks().getByName("uploadArchives").dependsOn(signJar);
    }

    private void makeLifecycleTasks()
    {
        DefaultTask processJson = makeTask("processJson");
        processJson.onlyIf(new Spec<Task>()
        {
            @Override
            public boolean isSatisfiedBy(Task task)
            {
                return true; // hasVersionCheck;
            }
        });
        processJson.doLast(new Action<Task>()
        {
            @Override
            @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private void processMaps(Map<String, Object> data) throws IOException
    {
        if (!data.containsKey(jsonName) || (data.containsKey(jsonName) && !(data.get(jsonName) instanceof Map)))
        {
            data.remove(jsonName);

            Map<String, Object> modNode = newMap();
            Map<String, Object> minecraftNode = newMap();
            Map<String, Object> channelsNode = newMap();

            minecraftNode.put(CHANNELS, initChannels(channelsNode));

            modNode.put(minecraftVersion, minecraftNode);

            data.put(jsonName, modNode);
        }
        else
        {
            Map<String, Object> modNode = (Map<String, Object>) data.get(jsonName);

            if (!modNode.containsKey(minecraftVersion) || (modNode.containsKey(minecraftVersion) && !(modNode
                    .get(minecraftVersion) instanceof Map)))
            {
                modNode.remove(minecraftVersion);

                Map<String, Object> minecraftNode = newMap();
                Map<String, Object> channelsNode = newMap();

                minecraftNode.put(CHANNELS, initChannels(channelsNode));

                modNode.put(minecraftVersion, minecraftNode);

                data.put(jsonName, modNode);
            }
            else
            {
                Map<String, Object> minecraftNode = (Map<String, Object>) modNode.get(minecraftVersion);

                if (!minecraftNode.containsKey(CHANNELS) || (minecraftNode.containsKey(CHANNELS) && !(minecraftNode
                        .get(CHANNELS) instanceof Map)))
                {
                    minecraftNode.remove(CHANNELS);

                    Map<String, Object> channelsNode = newMap();

                    minecraftNode.put(CHANNELS, initChannels(channelsNode));

                    modNode.put(minecraftVersion, minecraftNode);

                    data.put(jsonName, modNode);
                }
                else
                {
                    Map<String, Object> channelsNode = (Map<String, Object>) minecraftNode.get(CHANNELS);

                    if (!channelsNode.containsKey(STABLE) || (channelsNode.containsKey(STABLE) && !(channelsNode
                            .get(STABLE) instanceof Map)))
                    {
                        channelsNode.remove(STABLE);

                        channelsNode.put(STABLE, initStable());

                        minecraftNode.put(CHANNELS, channelsNode);

                        modNode.put(minecraftVersion, minecraftNode);

                        data.put(jsonName, modNode);
                    }
                    else
                    {
                        if (isStable)
                        {
                            Map<String, Object> stable = (Map<String, Object>) channelsNode.get(STABLE);

                            channelsNode.put(STABLE, processStable(stable));

                            minecraftNode.put(CHANNELS, channelsNode);

                            modNode.put(minecraftVersion, minecraftNode);

                            data.put(jsonName, modNode);
                        }
                    }

                    if (!channelsNode.containsKey(LATEST) || (channelsNode.containsKey(LATEST) && !(channelsNode
                            .get(LATEST) instanceof Map)))
                    {
                        channelsNode.remove(LATEST);

                        channelsNode.put(LATEST, initLatest());

                        minecraftNode.put(CHANNELS, channelsNode);

                        modNode.put(minecraftVersion, minecraftNode);

                        data.put(jsonName, modNode);
                    }
                    else
                    {
                        if (!isStable)
                        {
                            Map<String, Object> latest = (Map<String, Object>) channelsNode.get(LATEST);

                            channelsNode.put(LATEST, processLatest(latest));

                            minecraftNode.put(CHANNELS, channelsNode);

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

    private Map<String, Object> initChannels(Map<String, Object> channelsNode)
    {
        channelsNode.put(STABLE, initStable());
        channelsNode.put(LATEST, initLatest());

        return channelsNode;
    }

    private Map<String, Object> initStable()
    {
        Map<String, Object> stable = newMap();

        return processStable(stable);
    }

    private Map<String, Object> processStable(Map<String, Object> stable)
    {
        // TODO Description
        if (isStable)
        {
            stable.put(MAJOR, version.major);
            stable.put(MINOR, version.minor);
            stable.put(PATCH, version.patch);
            stable.put(DESCRIPTION, "");
        }
        else
        {
            stable.put(MAJOR, 0);
            stable.put(MINOR, 0);
            stable.put(PATCH, 0);
            stable.put(DESCRIPTION, "");
        }

        return stable;
    }

    private Map<String, Object> initLatest()
    {
        Map<String, Object> latest = newMap();

        return processLatest(latest);
    }

    private Map<String, Object> processLatest(Map<String, Object> latest)
    {
        // TODO Description
        if (isStable)
        {
            latest.put(MAJOR, 0);
            latest.put(MINOR, 0);
            latest.put(PATCH, 0);
            latest.put(POSTFIX, null);
            latest.put(NUMBER, 0);
            latest.put(DESCRIPTION, "");
        }
        else
        {
            latest.put(MAJOR, version.major);
            latest.put(MINOR, version.minor);
            latest.put(PATCH, version.patch);
            latest.put(POSTFIX, version.getChannel().getKey());
            latest.put(NUMBER, version.number);
            latest.put(DESCRIPTION, "");
        }

        return latest;
    }

    private <K, V> Map<K, V> newMap()
    {
        return new HashMap<K, V>();
    }

    public static void displayBanner()
    {
        projectStatic.getLogger().lifecycle("****************************");
        projectStatic.getLogger().lifecycle(" Welcome to " + Reference.NAME_FULL);
        projectStatic.getLogger().lifecycle(" Version " + Versions.VERSION);
        projectStatic.getLogger().lifecycle(" Project version " + versionNumber);

        if (fg)
        {
            projectStatic.getLogger().lifecycle(" ForgeGradle enabled        ");
        }
        else
        {
            projectStatic.getLogger().lifecycle("****************************");
        }
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
    private static <T extends Task> T makeTask(Project proj, String name, Class<T> type)
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

    public static String getCWVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/main/java/celestibytes/celestialwizardry/reference/Versions.java", field);
    }

    public static String getDgCVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/main/java/pizzana/doughcraft/reference/Versions.java", field);
    }

    public static String getCGVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/main/java/celestibytes/gradle/reference/Versions.java", field);
    }

    public static String getCoreVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/main/java/celestibytes/core/reference/Versions.java", field);
    }

    public static String getTTVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/celestibytes/tankytanks/reference/Versions.java", field);
    }

    @SuppressWarnings("unchecked")
    public static String getProperty(Project project, String file, String field) throws IOException
    {
        String property = "unknown";

        String prefix = "public static final String " + field;
        List<String> lines = (List<String>) FileUtils.readLines(project.file(file));

        for (String s : lines)
        {
            s = s.trim();

            if (s.startsWith(prefix))
            {
                s = s.substring(prefix.length(), s.length() - 1);
                s = s.replace('=', ' ').replace('"', ' ').replaceAll(" +", " ").trim();
                property = s;
                break;
            }
        }

        return property;
    }

    public static String versionNumber(String version)
    {
        return setVersionNumber(version);
    }

    public static String setVersionNumber(String version)
    {
        versionNumber = version;
        return versionNumber;
    }

    public static String mc(String version)
    {
        return setMc(version);
    }

    public static String setMc()
    {
        return setMc(Versions.DEFAULT_MINECRAFT_VERSION);
    }

    public static String setMc(String version)
    {
        isMinecraftMod = true;
        minecraftVersion = version;
        return version;
    }

    public static String core(String version)
    {
        return setCore(version);
    }

    public static String setCore(String version)
    {
        needsCore = true;
        coreVersion = version;
        return version;
    }

    public static String basePackage(String s)
    {
        return setBasePackage(s);
    }

    public static String setBasePackage(String s)
    {
        basePackage = s;
        dir = basePackage.replace('.', '/');
        return s;
    }

    public static List<String> artifactsList(List<String> list)
    {
        return setArtifactsList(list);
    }

    public static List<String> setArtifactsList(List<String> list)
    {
        artifactsList = list;
        return list;
    }

    public static String artifact(String s)
    {
        return setArtifact(s);
    }

    public static String setArtifact(String s)
    {
        artifactsList.add(s);
        return s;
    }

    public static Closure manifest(Closure c)
    {
        return setManifest(c);
    }

    public static Closure setManifest(Closure c)
    {
        hasManifest = true;
        manifest = c;
        return c;
    }
}
