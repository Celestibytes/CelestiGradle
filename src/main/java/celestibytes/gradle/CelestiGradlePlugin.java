/*
 * Copyright (C) 2014 Celestibytes
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package celestibytes.gradle;

import celestibytes.pizzana.derp.DerpException;
import celestibytes.pizzana.json.JSONUtil;
import celestibytes.pizzana.version.Version;

import celestibytes.gradle.dependency.Dependency;
import celestibytes.gradle.reference.Projects;
import celestibytes.gradle.reference.Reference;
import celestibytes.gradle.reference.Versions;

import net.minecraftforge.gradle.CopyInto;
import net.minecraftforge.gradle.FileLogListenner;
import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedBase;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.abstractutil.DownloadTask;
import net.minecraftforge.gradle.tasks.dev.ChangelogTask;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Upload;
import org.gradle.api.tasks.bundling.Jar;

import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author PizzAna
 *
 */
public final class CelestiGradlePlugin implements Plugin<Project>, DelayedBase.IDelayedResolver<BaseExtension>
{
    private static Project projectStatic;
    private static boolean fg;

    private static String versionNumber;

    private static boolean isMinecraftMod = false;
    private static String minecraftVersion;
    private static boolean needsCore = false;
    private static String coreVersion = "";

    private static String basePackage;
    private static String dir;

    private static List<String> artifactsList = Lists.newArrayList();

    @SuppressWarnings("rawtypes")
    private static Closure manifest;
    private static boolean hasManifest = false;

    private static String coreArtifact = "";
    private static String coreDevArtifact = "";

    private static String versionCheckId;
    private static boolean hasVersionCheck = false;
    private static String versionDescription;

    private static String baublesVersion;
    private static String baublesMinecraft;
    private static boolean needsBaubles = false;

    private static boolean scala = false;

    private static List<String> libs = Lists.newArrayList();
    private static String libsFile;
    private static boolean useLibsFile = false;
    
    private static List<String> addedLibs = Lists.newArrayList();

    private static Map<String, Dependency> knownDeps = Maps.newHashMap();

    /**
     * First the alias, then the knownDeps key it leads to
     */
    private static Map<String, List<String>> knownAliases = Maps.newHashMap();

    private Project project;

    private String filesmaven;

    private boolean hasKeystore;

    private boolean isStable;

    @Override
    public void apply(Project arg)
    {
        project = arg;

        projectStatic = project;
        fg = project.getPlugins().hasPlugin("forge");

        FileLogListenner listener = new FileLogListenner(project.file(Constants.LOG));
        project.getLogging().addStandardOutputListener(listener);
        project.getLogging().addStandardErrorListener(listener);
        project.getGradle().addBuildListener(listener);

        addRepositories();
        resolveProperties();
        applyPlugins();

        if (!fg)
        {
            displayBanner();
        }

        addDependencies();

        if (needsBaubles)
        {
            makeBaublesTask();
        }

        makePackageTasks();
        makeSignTask();
        makeLifecycleTasks();
    }

    private void resolveProperties()
    {
        if (versionNumber == null)
        {
            throw new ProjectConfigurationException("You must set the version number!", new NullPointerException());
        }

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

        isStable = Version.parse(versionNumber).isStable();
    }

    private void applyPlugins()
    {
        if (!fg)
        {
            if (scala)
            {
                applyExternalPlugin("scala");
            }

            applyExternalPlugin("java");
            applyExternalPlugin("maven");
            applyExternalPlugin("eclipse");
            applyExternalPlugin("idea");

            if (isMinecraftMod)
            {
                applyExternalPlugin("forge");
                fg = true;
            }
            else
            {
                applyExternalPlugin("dummy");
                BasePlugin.displayBanner = false;
            }
        }
    }

    private void addRepositories()
    {
        project.allprojects(new Action<Project>()
        {
            @Override
            public void execute(Project project)
            {
                addMavenRepo(project, "cbts", Reference.MAVEN);
                project.getRepositories().mavenCentral();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addDependencies()
    {
        if (useLibsFile)
        {
            try
            {
                List<String> lines = FileUtils.readLines(project.file(libsFile));
                
                for (String line : lines)
                {
                    line = line.trim();
                    
                    addDependency(line.trim());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        if (needsCore)
        {
            addDependency(delayedString("{CORE_DEV_ARTIFACT}").call());
        }

        if (!fg)
        {
            addDependency(project.fileTree("libs"));
        }

        registerCelestiDep("CelestiCore", "");

        registerDep("lzma", "com.github.jponge", "lzma-java", "1.3");
        registerDep("asm", "org.ow2.asm", "asm-debug-all", "5.0.3", "asm-debug");
        registerDep("akka", "org.typesafe.akka", "akka-actor_2.11", "2.3.5", "akka-actor");
        registerDep("jopt", "net.sf.jopt-simple", "jopt-simple", "4.7");
        registerDep("trove", "net.sf.trove4j", "trove4j", "3.0.3");
        registerDep("netty", "io.netty", "netty-all", "4.0.23.Final");

        registerDep2("jinput", "net.java.jinput", "2.0.6");
        registerDep2("lwjgl", "org.lwjgl.lwjgl", "2.9.1");
        registerDep2("lwjgl_util", "org.lwjgl.lwjgl", "2.9.1", "lwjgl");
        registerDep2("config", "org.typesafe", "1.2.1");
        registerDep2("vecmath", "java3d", "1.3.1");
        registerDep2("jutils", "net.java.jutils", "1.0.0");
        registerDep2("gson", "com.google.code.gson", "2.3");
        registerDep2("guava", "com.google.guava", "18.0");
        registerDep2("argo", "net.sourceforge.argo", "3.12");
        registerDep2("diff4j", "com.cloudbees", "1.2");
        registerDep2("jAstyle", "com.github.abrarsyed.jastyle", "1.2", "jastyle");
        registerDep2("javaxdelta", "com.nothome", "2.0.1");
        registerDep2("named-regexp", "com.github.tony19", "0.2.3");
        registerDep2("localizer", "org.jvnet.localizer", "1.12");
        registerDep2("jsch", "com.jcraft", "0.1.51");
        registerDep2("javaEWAH", "com.googlecode.javaewah", "0.8.12", "javaewah");

        registerScala2("library", "library");
        registerScala2("reflect", "reflect");
        registerScala2("compiler", "compiler");
        registerScala2("actors", "actors");

        // TODO Add every commons library because why not :P
        registerCommons("lang", "lang3", "3.3.2");
        registerCommons("compress", "compress", "1.8.1");
        registerCommons("exec", "exec", "1.2");
        registerCommons("math", "math3", "3.3");
        registerCommons("collections", "collections4", "4.0");

        registerCommons2("io", "io", "2.4");
        registerCommons2("codec", "codec", "1.9");
        registerCommons2("logging", "logging", "1.2");

        registerHttp("core", "4.3.2");

        registerHttp2("client");
        registerHttp2("mime");

        registerLog4j("core");
        registerLog4j("api");

        if (scala)
        {
            // TODO Automatically add scala deps
        }
        
        for (String s : libs)
        {
            addDependency(s);
        }
    }

    private void makeBaublesTask()
    {
        String baublesFile = "Baubles-deobf-" + baublesMinecraft + "-" + baublesVersion + ".jar";
        final String baublesDest = "libs/" + baublesFile;
        String baublesUrl = "https://dl.dropboxusercontent.com/u/47135879/" + baublesFile;

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

        cleanEveryBaubles
        .setDescription("Deletes all of the libraries containing \'Baubles\' in their name from the \'libs\' directory");
        cleanEveryBaubles.setGroup(Reference.NAME);

        Delete cleanBaubles = makeTask("cleanBaubles", Delete.class);

        for (File file : baubs)
        {
            cleanBaubles.delete(file);
        }

        cleanBaubles
        .setDescription("Deletes all of the libraries containing \'Baubles\' in their name from the \'libs\' directory "
                + "(excluding the up-to-date one)");
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

    private void makePackageTasks()
    {
        String changelogFile = "{BUILD_DIR}/libs/" + project.getName() + "-" + project.getVersion() + "-changelog.txt";

        final boolean isJenkins = System.getenv("JOB_NAME") != null && project.hasProperty("jenkins_server")
                && project.hasProperty("jenkins_password") && project.hasProperty("jenkins_user");

        ChangelogTask changelog = makeTask("createChangelog", ChangelogTask.class);

        if (isJenkins)
        {
            changelog.setServerRoot(delayedString("{JENKINS_SERVER}"));
            changelog.setJobName(delayedString("{JENKINS_JOB}"));
            changelog.setAuthName(delayedString("{JENKINS_AUTH_NAME}"));
            changelog.setAuthPassword(delayedString("{JENKINS_AUTH_PASSWORD}"));
            changelog.setTargetBuild(delayedString("{BUILD_NUM}"));
            changelog.setOutput(delayedFile(changelogFile));
        }

        changelog.onlyIf(new Spec<Task>()
                {
            @Override
            public boolean isSatisfiedBy(Task task)
            {
                return isJenkins;
            }
                });

        changelog.dependsOn("classes", "processResources");

        project.getTasks().getByName("build").dependsOn(changelog);

        Jar jar = (Jar) project.getTasks().getByName("jar");

        if (isJenkins)
        {
            jar.from(delayedFile(changelogFile));
        }

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

            if (isJenkins)
            {
                sourcesJar.from(delayedFile(changelogFile));
            }

            sourcesJar.from(delayedFile("LICENSE"));
            sourcesJar.from(delayedFile("build.gradle"));
            sourcesJar.from(delayedFile("settings.gradle"));

            if (scala)
            {
                sourcesJar.from(delayedFile("{BUILD_DIR}/sources/scala/"));
            }

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

            if (scala)
            {
                apiJar.from(delayedFile("{BUILD_DIR}/sources/scala/" + apiDir), new CopyInto(apiDir));
            }

            apiJar.from(delayedFile("{BUILD_DIR}/sources/java/" + apiDir), new CopyInto(apiDir));
            apiJar.dependsOn(jar);
            apiJar.setExtension("jar");
            project.getArtifacts().add("archives", apiJar);
        }

        if (artifactsList.contains("dev") || artifactsList.contains("deobf"))
        {
            Jar devJar = makeTask("devJar", Jar.class);
            devJar.setClassifier("dev");

            if (isJenkins)
            {
                devJar.from(delayedFile(changelogFile));
            }

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
                return hasVersionCheck && isStable;
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

                    String separator;
                    String summary;

                    if (data.containsKey(JSONUtil.SEPARATOR) && data.get(JSONUtil.SEPARATOR) instanceof String)
                    {
                        separator = (String) data.get(JSONUtil.SEPARATOR);
                    }
                    else
                    {
                        throw new DerpException("No separator specified in data json", new NullPointerException());
                    }

                    if (data.containsKey(JSONUtil.SUMMARY) && data.get(JSONUtil.SUMMARY) instanceof String)
                    {
                        summary = (String) data.get(JSONUtil.SUMMARY);
                    }
                    else
                    {
                        throw new DerpException("No summary specified in data json", new NullPointerException());
                    }

                    urlConnection = new URL(Reference.MAVEN + versionCheckId + ".json").openConnection();

                    urlConnection.setRequestProperty("User-Agent", System.getProperty("java.version"));
                    urlConnection.connect();

                    inputStream = urlConnection.getInputStream();

                    json = new String(ByteStreams.toByteArray(inputStream));

                    inputStream.close();

                    data = new Gson().fromJson(json, Map.class);

                    Map<String, String> args = newHashMap();
                    args.put("file", filesmaven + "/" + versionCheckId + ".json");
                    invokeAnt("delete", args);

                    writeJsonToFile(project.file(filesmaven + "/" + versionCheckId + ".json"),
                            processMaps(data, separator, summary));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                catch (DerpException e)
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

    private Map<String, Object> processMaps(Map<String, Object> data, String separator, String summary)
            throws IOException, DerpException
            {
        StringBuilder builder = new StringBuilder();

        if (isMinecraftMod)
        {
            builder.append(minecraftVersion);
        }
        else
        {
            builder.append("version");
        }

        String s = builder.toString();

        data.put(s, versionNumber);

        builder.append(separator);
        builder.append(summary);

        s = builder.toString();

        data.put(s, versionDescription);

        return data;
            }

    @SuppressWarnings("unused")
    private static <K, V> Map<K, V> newMap()
    {
        return new HashMap<K, V>();
    }

    private static <K, V> Map<K, V> newHashMap()
    {
        return Maps.newHashMap();
    }

    private static File writeJsonToFile(File file, Map<String, Object> data) throws IOException
    {
        FileUtils.writeStringToFile(file, new Gson().toJson(data));
        return file;
    }

    private void addDependency(String group, String name, String version)
    {
        addDependency(project, group, name, version);
    }

    private void addDependency(Object dependency)
    {
        addDependency(project, dependency);
    }

    public static void addDependency(Project project, String group, String name, String version)
    {
        addDependency(project, group + ":" + name + ":" + version);
    }

    public static void addDependency(Project project, Object dependency)
    {
        if (dependency instanceof String && addedLibs.contains(dependency))
        {
            return;
        }
        
        project.getDependencies().add("compile", dependency);
        
        if (dependency instanceof String)
        {
            addedLibs.add((String) dependency);
        }
    }

    /*
     * private void registerDep(String name, String group, String artifact,
     * String version, String... aliases) { registerDep(project, name, group,
     * artifact, version, aliases); }
     */

    public static void registerCelestiDep(String name, String version, String... aliases)
    {
        registerDep(name, "io.github.celestibytes", name, version, aliases);
    }

    public static void registerScala2(String name, String artifact, String... aliases)
    {
        registerScala(name, artifact, "2.11.2", aliases);
    }

    public static void registerScala(String name, String artifact, String version, String... aliases)
    {
        String[] array = new String[aliases.length + 1];

        int i = 0;

        for (String alias : aliases)
        {
            array[i] = alias;
            i++;
        }

        array[i++] = "scala";

        registerDep("scala-" + name, "org.scala-lang", "scala-" + artifact, version, array);
    }

    public static void registerCommons(String name, String artifact, String version, String... aliases)
    {
        String[] array = new String[aliases.length + 2];

        int i = 0;

        for (String alias : aliases)
        {
            array[i] = alias;
            i++;
        }

        array[i++] = "apache-commons";
        array[i++] = "commons";

        registerDep("commons-" + name, "org.apache.commons", "commons-" + artifact, version, array);
    }

    public static void registerCommons2(String name, String artifact, String version, String... aliases)
    {
        String[] array = new String[aliases.length + 2];

        int i = 0;

        for (String alias : aliases)
        {
            array[i] = alias;
            i++;
        }

        array[i++] = "apache-commons";
        array[i++] = "commons";

        registerDep("commons-" + name, "commons-" + name, "commons-" + artifact, version, array);
    }

    public static void registerHttp2(String name, String... aliases)
    {
        registerHttp(name, "4.3.5", aliases);
    }

    public static void registerHttp(String name, String version, String... aliases)
    {
        String[] array = new String[aliases.length + 2];

        int i = 0;

        for (String alias : aliases)
        {
            array[i] = alias;
            i++;
        }

        array[i++] = "http";
        array[i++] = "http-" + name;

        registerDep("http" + name, "org.apache.httpcomponents", "http" + name, version, array);
    }

    public static void registerLog4j(String name, String... aliases)
    {
        String[] array = new String[aliases.length + 1];

        int i = 0;

        for (String alias : aliases)
        {
            array[i] = alias;
            i++;
        }

        array[i++] = "log4j";

        registerDep("log4j-" + name, "org.apache.logging.log4j", "log4j-" + name, "2.0.2", array);
    }

    public static void registerDep2(String name, String group, String version, String... aliases)
    {
        registerDep(name, group, name, version, aliases);
    }

    public static void registerDep(String name, String group, String artifact, String version, String... aliases)
    {
        if (knownDeps.containsKey(name))
        {
            return;
        }

        knownDeps.put(name, new Dependency(name, group, artifact, version, aliases));

        List<String> list;

        if (knownAliases.containsKey(name))
        {
            list = knownAliases.get(name);
        }
        else
        {
            list = Lists.newArrayList();
        }

        if (!list.contains(name))
        {
            list.add(name);
        }

        knownAliases.put(name, list);

        if (knownAliases.containsKey(artifact))
        {
            list = knownAliases.get(artifact);
        }
        else
        {
            list = Lists.newArrayList();
        }

        if (!list.contains(name))
        {
            list.add(name);
        }

        knownAliases.put(artifact, list);

        for (String alias : aliases)
        {
            if (knownAliases.containsKey(alias))
            {
                list = knownAliases.get(alias);
            }
            else
            {
                list = Lists.newArrayList();
            }

            if (!list.contains(name))
            {
                list.add(name);
            }

            knownAliases.put(alias, list);
        }
    }

    public static void displayBanner()
    {
        try
        {
            URLConnection urlConnection = new URL(Reference.VERSION_CHECK_URL).openConnection();
            urlConnection.setRequestProperty("User-Agent", System.getProperty("java.version"));
            urlConnection.connect();
            
            InputStream inputStream = urlConnection.getInputStream();
            
            String data = new String(ByteStreams.toByteArray(inputStream));
            
            inputStream.close();
            
            Version remote = Version.parse(data);
            
            if (Version.parse(Versions.VERSION).compareTo(remote) < 0)
            {
                projectStatic.getLogger().lifecycle("****************************");
                projectStatic.getLogger().lifecycle(" A new version of " + Reference.NAME_FULL + " is available");
                projectStatic.getLogger().lifecycle(" " + data);
            }
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        projectStatic.getLogger().lifecycle("****************************");
        projectStatic.getLogger().lifecycle(" Welcome to " + Reference.NAME_FULL);
        projectStatic.getLogger().lifecycle(" Version " + Versions.VERSION);
        projectStatic.getLogger().lifecycle(" Project version " + versionNumber);

        if (fg)
        {
            projectStatic.getLogger().lifecycle(" ForgeGradle enabled        ");
            projectStatic.getLogger().lifecycle(" Minecraft version " + minecraftVersion);

            if (needsCore)
            {
                projectStatic.getLogger().lifecycle(" Celestibytes Core version " + coreVersion);
            }

            if (needsBaubles)
            {
                projectStatic.getLogger().lifecycle(" Baubles version " + baublesVersion);
            }
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
        invokeAnt(project, task, args);
    }

    public static void invokeAnt(Project project, String task, Map<String, String> args)
    {
        project.getAnt().invokeMethod(task, args);
    }

    public void applyExternalPlugin(String plugin)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("plugin", plugin);
        project.apply(map);
    }

    public static MavenArtifactRepository addMavenRepo(Project project, final String name, final String url)
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

    public static FlatDirectoryArtifactRepository addFlatRepo(Project project, final String name, final Object... dirs)
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
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/celestibytes/celestialwizardry/reference/Versions.java", field);
    }

    public static String getDgCVersion(Project project, String field) throws IOException
    {
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/pizzana/doughcraft/reference/Versions.java", field);
    }

    public static String getCGVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/main/java/celestibytes/gradle/reference/Versions.java", field);
    }

    public static String getCoreVersion(Project project, String field) throws IOException
    {
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/celestibytes/core/reference/Versions.java", field);
    }

    public static String getTTVersion(Project project, String field) throws IOException
    {
        // String s = scala ? "scala" : "java";
        return getProperty(project, "src/celestibytes/tankytanks/reference/Versions.java", field);
    }

    @SuppressWarnings("unchecked")
    public static String getProperty(Project project, String file, String field) throws IOException
    {
        String property = "unknown";

        String prefix = scala ? "final val " + field + ": String" : "public static final String " + field;
        List<String> lines = FileUtils.readLines(project.file(file));

        for (String line : lines)
        {
            line = line.trim();

            if (line.startsWith(prefix))
            {
                line = line.substring(prefix.length(), line.length() - 1);
                line = line.replace('=', ' ').replace('"', ' ').replaceAll(" +", " ").trim();
                property = line;
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
        coreArtifact = "io.github.celestibytes:CelestiCore:" + coreVersion;
        coreDevArtifact = "io.github.celestibytes:CelestiCore:" + coreVersion + ":dev";
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

    @SuppressWarnings("rawtypes")
    public static Closure manifest(Closure c)
    {
        return setManifest(c);
    }

    @SuppressWarnings("rawtypes")
    public static Closure setManifest(Closure c)
    {
        hasManifest = true;
        manifest = c;
        return c;
    }

    public static String versionCheck(Project p)
    {
        return setVersionCheck(p);
    }

    public static String versionCheck(String id)
    {
        return setVersionCheck(id);
    }

    public static String setVersionCheck(Project p)
    {
        return setVersionCheck(p.getName().toLowerCase());
    }

    public static String setVersionCheck(String id)
    {
        return setVersionCheck(id, "null");
    }

    public static String versionCheck(Project p, String desc)
    {
        return setVersionCheck(p, desc);
    }

    public static String versionCheck(String id, String desc)
    {
        return setVersionCheck(id, desc);
    }

    public static String setVersionCheck(Project p, String desc)
    {
        return setVersionCheck(p.getName().toLowerCase(), desc);
    }

    public static String setVersionCheck(String id, String desc)
    {
        versionCheckId = id;
        hasVersionCheck = true;
        versionDescription = desc;
        return id;
    }

    public static String baubles(String s)
    {
        return setBaubles(s);
    }

    public static String baubles(String s, String mc)
    {
        return setBaubles(s, mc);
    }

    public static String setBaubles(String s)
    {
        return setBaubles(s, minecraftVersion);
    }

    public static String setBaubles(String s, String mc)
    {
        baublesVersion = s;
        baublesMinecraft = mc;
        needsBaubles = true;
        return s;
    }

    public static boolean scala()
    {
        return scala(true);
    }

    public static boolean setScala()
    {
        return setScala(true);
    }

    public static boolean scala(boolean b)
    {
        return setScala(b);
    }

    public static boolean setScala(boolean b)
    {
        scala = b;
        return b;
    }

    public static List<String> libsList(List<String> list)
    {
        return setLibsList(list);
    }

    public static List<String> setLibsList(List<String> list)
    {
        libs = list;
        return list;
    }

    public static String lib(String s)
    {
        return setLib(s);
    }

    public static String setLib(String s)
    {
        libs.add(s);
        return s;
    }
    
    public static String libsFile()
    {
        return setLibsFile(Reference.DEFAULT_DEPS_FILE);
    }
    
    public static String setLibsFile()
    {
        return setLibsFile(Reference.DEFAULT_DEPS_FILE);
    }
    
    public static String libsFile(String s)
    {
        return setLibsFile(s);
    }
    
    public static String setLibsFile(String s)
    {
        libsFile = s;
        useLibsFile = true;
        return s;
    }
}
