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

import celestibytes.lib.version.SemanticVersion;
import celestibytes.lib.version.Version;
import celestibytes.lib.version.VersionFormatException;
import celestibytes.lib.version.Versions;

import celestibytes.gradle.dependency.Dependency;
import celestibytes.gradle.reference.Reference;

import net.minecraftforge.gradle.CopyInto;
import net.minecraftforge.gradle.FileLogListenner;
import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedBase;
import net.minecraftforge.gradle.delayed.DelayedFile;
import net.minecraftforge.gradle.delayed.DelayedFileTree;
import net.minecraftforge.gradle.delayed.DelayedString;
import net.minecraftforge.gradle.tasks.dev.ChangelogTask;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.bundling.Jar;

import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CelestiGradlePlugin implements Plugin<Project>, DelayedBase.IDelayedResolver<BaseExtension>
{
    private static Project projectStatic;
    
    private static String versionNumber;
    private static Version versionObj;
    private static String basePackage;
    private static String dir;
    private static List<String> artifactsList = Lists.newArrayList();
    
    @SuppressWarnings("rawtypes")
    private static Closure manifest;
    private static boolean hasManifest = false;
    
    private static boolean hasVersionCheck = false;
    private static boolean scala = false;
    private static List<String> deps = new ArrayList<String>();
    private static String depsFile;
    private static boolean useDepsFile = false;
    private static List<String> addedLibs = new ArrayList<String>();
    private static Map<String, Dependency> knownDeps = new HashMap<String, Dependency>();
    private static Map<String, List<String>> knownAliases = new HashMap<String, List<String>>();
    
    private Project project;
    private boolean hasKeystore;
    private boolean isStable;
    
    @Override
    public void apply(Project target)
    {
        project = target;
        
        projectStatic = project;
        
        FileLogListenner listener = new FileLogListenner(project.file(Constants.LOG));
        project.getLogging().addStandardOutputListener(listener);
        project.getLogging().addStandardErrorListener(listener);
        project.getGradle().addBuildListener(listener);
        
        applyPlugins();
        addRepositories();
        resolveProperties();
        
        project.afterEvaluate(new Action<Project>()
                {
                    @Override
                    public void execute(Project arg0)
                    {
                        displayBanner();
                    }
                });
        
        addDependencies();
        
        makePackageTasks();
        makeSignTask();
        makeVersionTask();
        makeLifecycleTasks();
    }
    
    private void applyPlugins()
    {
        if (scala)
        {
            applyExternalPlugin("scala");
        }
        
        applyExternalPlugin("java");
        applyExternalPlugin("maven");
        applyExternalPlugin("eclipse");
        applyExternalPlugin("idea");
        applyExternalPlugin("dummy");
        BasePlugin.displayBanner = false;
    }
    
    private void addRepositories()
    {
        project.allprojects(new Action<Project>()
        {
            @Override
            public void execute(Project project)
            {
                project.getRepositories().mavenCentral();
                addMavenRepo(project, "Sonatype Snapshot Repository",
                        "https://oss.sonatype.org/content/repositories/snapshots/");
                project.getRepositories().mavenLocal();
            }
        });
    }
    
    private void resolveProperties()
    {
        if (versionNumber == null)
        {
            throw new ProjectConfigurationException("You must set the version number!", new NullPointerException());
        }
        
        if (basePackage == null)
        {
            throw new ProjectConfigurationException("You must set the base package!", new NullPointerException());
        }
        
        hasKeystore = project.hasProperty("keystoreLocation");
        
        if (versionObj instanceof SemanticVersion)
        {
            isStable = ((SemanticVersion) versionObj).isStable();
        }
        else
        {
            isStable = false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addDependencies()
    {
        addDependency(project.fileTree("libs"));
        
        registerCelestiDep("CelestiCore", "0.6.0", "celesticore", "core");
        registerCelestiDep("CelestiLib", "0.4.0", "celestilib", "lib");
        
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
        registerDep2("junit", "junit", "4.11", "JUnit");
        
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
        
        if (useDepsFile)
        {
            try
            {
                List<String> lines = FileUtils.readLines(project.file(depsFile));
                
                for (String line : lines)
                {
                    if (line.equals("") || line == null)
                    {
                        continue;
                    }
                    
                    if (line.trim().startsWith("#"))
                    {
                        addDependency(line.trim().replace("#", ""));
                    }
                    else if (line.contains(":"))
                    {
                        String[] array = line.trim().split(":");
                        
                        if (array.length == 2)
                        {
                            addDependency(array[0], array[1]);
                        }
                        else if (array.length == 3)
                        {
                            if (knownAliases.containsKey(array[0]))
                            {
                                for (String s : knownAliases.get(array[0]))
                                {
                                    addDependency(knownDeps.get(s).getGroup() + ":" + knownDeps.get(s).getArtifact()
                                            + ":" + array[1] + ":" + array[2]);
                                }
                            }
                        }
                    }
                    else
                    {
                        addDependency(line.trim());
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        if (scala)
        {
            addDependency("scala");
        }
        
        for (String s : deps)
        {
            if (s.contains(":"))
            {
                String[] array = s.trim().split(":");
                addDependency(array[0], array[1]);
            }
            else
            {
                addDependency(s.trim());
            }
        }
    }
    
    @SuppressWarnings("unused")
    @Deprecated
    private void makeBaublesTask()
    {}
    
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
    
    private void makeVersionTask()
    {
        DefaultTask processVersionFile = makeTask("processVersionFile");
        processVersionFile.onlyIf(new Spec<Task>()
        {
            @Override
            public boolean isSatisfiedBy(Task task)
            {
                return hasVersionCheck && isStable;
            }
        });
        processVersionFile.doLast(new Action<Task>()
        {
            @Override
            public void execute(Task task)
            {
                try
                {
                    Map<String, String> args = Maps.newHashMap();
                    args.put("file", "./version.txt");
                    invokeAnt("delete", args);
                    FileUtils.writeStringToFile(project.file("./version.txt"), versionNumber);
                }
                catch (IOException e)
                {
                    project.getLogger().lifecycle("Unable to process project's version check file");
                    e.printStackTrace();
                }
            }
        });
        processVersionFile.dependsOn("uploadArchives");
    }
    
    private void makeLifecycleTasks()
    {
        DefaultTask release = makeTask("release");
        release.setDescription("Wrapper task for building release-ready archives.");
        release.setGroup(Reference.NAME);
        release.dependsOn("processVersionFile");
    }
    
    private void addDependency(String name)
    {
        if (knownAliases.containsKey(name))
        {
            for (String s : knownAliases.get(name))
            {
                addDependency(knownDeps.get(s));
            }
        }
        else
        {
            addDependency(new Dependency(name.split(":")[0], name.split(":")[1], name.split(":")[2]));
        }
    }
    
    private void addDependency(Dependency dependency)
    {
        addDependency(dependency.getGroup(), dependency.getArtifact(), dependency.getVersion());
    }
    
    private void addDependency(String name, String version)
    {
        if (knownAliases.containsKey(name))
        {
            for (String s : knownAliases.get(name))
            {
                addDependency(knownDeps.get(s), version);
            }
        }
    }
    
    private void addDependency(Dependency dependency, String version)
    {
        addDependency(dependency.getGroup(), dependency.getArtifact(), version);
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
        
        project.getLogger().lifecycle("Adding dependency: " + dependency);
        
        if (dependency instanceof String)
        {
            addedLibs.add((String) dependency);
        }
        
        project.getDependencies().add("compile", dependency);
    }
    
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
            list = new ArrayList<String>();
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
            list = new ArrayList<String>();
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
                list = new ArrayList<String>();
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
            Version remote = Versions.parseFromUrl(Reference.VERSION_CHECK_URL);
            Version local = Versions.parse(Reference.VERSION);
            
            if (Versions.comparator.compare(remote, local) > 0)
            {
                projectStatic.getLogger().lifecycle("****************************");
                projectStatic.getLogger().lifecycle(" A new version of " + Reference.NAME_FULL + " is available:");
                projectStatic.getLogger().lifecycle(" " + remote.toString());
            }
            else
            {
                projectStatic.getLogger().lifecycle("****************************");
                projectStatic.getLogger().lifecycle(" " + Reference.NAME_FULL + " is up to date");
            }
        }
        catch (VersionFormatException e)
        {
            e.printStackTrace();
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
        projectStatic.getLogger().lifecycle(" Version " + Reference.VERSION);
        projectStatic.getLogger().lifecycle(" Project version " + versionNumber);
        projectStatic.getLogger().lifecycle("****************************");
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
    private static <T extends Task> T makeTask(Project project, String name, Class<T> type)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("type", type);
        return (T) project.task(map, name);
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
        if (!project.getPlugins().hasPlugin(plugin))
        {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("plugin", plugin);
            project.apply(map);
        }
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
        pattern = pattern.replace("{PATH}", project.getPath().replace('\\', '/'));
        return pattern;
    }
    
    public static String getCTIEVersion(Project project, String field) throws IOException
    {
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/celestibytes/ctie/reference/Reference.java", field);
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
    
    public static String versionNumber(String s)
    {
        return setVersionNumber(s);
    }
    
    public static String setVersionNumber(String s)
    {
        versionNumber = s;
        versionObj = Versions.parse(s);
        return versionNumber;
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
    
    public static void versionCheck()
    {
        setVersionCheck();
    }
    
    public static void setVersionCheck()
    {
        hasVersionCheck = true;
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
    
    public static List<String> depsList(List<String> list)
    {
        return setDepsList(list);
    }
    
    public static List<String> setDepsList(List<String> list)
    {
        deps = list;
        return list;
    }
    
    public static String dep(String s)
    {
        return setDep(s);
    }
    
    public static String setDep(String s)
    {
        deps.add(s);
        return s;
    }
    
    public static String depsFile()
    {
        return setDepsFile(Reference.DEFAULT_DEPS_FILE);
    }
    
    public static String setDepsFile()
    {
        return setDepsFile(Reference.DEFAULT_DEPS_FILE);
    }
    
    public static String depsFile(String s)
    {
        return setDepsFile(s);
    }
    
    public static String setDepsFile(String s)
    {
        depsFile = s;
        useDepsFile = true;
        return s;
    }
}
