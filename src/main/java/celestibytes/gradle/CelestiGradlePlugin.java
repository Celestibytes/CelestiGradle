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

import celestibytes.gradle.dependency.Dependency;
import celestibytes.gradle.reference.Projects;
import celestibytes.gradle.reference.Reference;
import celestibytes.lib.derp.DerpException;
import celestibytes.lib.version.SemanticVersion;
import celestibytes.lib.version.Version;
import celestibytes.lib.version.VersionFormatException;
import celestibytes.lib.version.Versions;

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
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.bundling.Jar;

import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main class of the Celestibytes Gradle {@link Plugin}.
 *
 * @author PizzAna
 */
public final class CelestiGradlePlugin implements Plugin<Project>, DelayedBase.IDelayedResolver<BaseExtension>
{
    /**
     * A {@code static} instance of the {@link Project}.
     */
    private static Project projectStatic;
    
    /**
     * A {@code boolean} that tells if CelestiGradle should initialize
     * ForgeGradle.
     */
    private static boolean fg;
    
    /**
     * The project's version number.
     */
    private static String versionNumber;
    
    /**
     * The project's {@link Version}.
     * <p/>
     * The field's name can't be {@code version} because it would mess up with
     * Gradle's own {@code version} field.
     */
    private static Version versionObj;
    
    /**
     * A {@code boolean} that tells if the project is a Minecraft mod.
     */
    private static boolean isMinecraftMod = false;
    
    /**
     * The Minecraft's version number. Only needed if the project is a Minecraft
     * mod.
     */
    private static String minecraftVersion;
    
    /**
     * A {@code boolean} that tells if the project needs CelestiCore.
     */
    private static boolean needsCore = false;
    
    /**
     * The CelestiCore version number. Only needed if the project needs
     * CelestiCore.
     */
    private static String coreVersion = "";
    
    /**
     * The base package of the project. Used for packaging project archives.
     */
    private static String basePackage;
    
    /**
     * The base directory of the project. Used for packaging project archives.
     */
    private static String dir;
    
    /**
     * The {@link List} of artifacts that should be build.
     */
    private static List<String> artifactsList = Lists.newArrayList();
    
    /**
     * A {@link Closure} that acts as a manifest for project archives if needed.
     */
    @SuppressWarnings("rawtypes")
    private static Closure manifest;
    
    /**
     * A {@code boolean} that tells if the project has custom manifest.
     */
    private static boolean hasManifest = false;
    
    /**
     * The id used to create remote version check files for the project.
     * <p/>
     * TODO Think if this is really needed.
     *
     * @deprecated
     */
    @SuppressWarnings("unused")
    @Deprecated
    private static String versionCheckUrl1;
    
    /**
     * A {@code boolean} that tells if the project has a remote version check.
     */
    private static boolean hasVersionCheck = false;
    
    /**
     * The Baubles version number. Only needed if the project needs Baubles.
     */
    private static String baublesVersion;
    
    /**
     * The Baubles Minecraft version number. Only needed if the project needs
     * Baubles.
     */
    private static String baublesMinecraft;
    
    /**
     * A {@code boolean} that tells if the project needs Baubles.
     */
    private static boolean needsBaubles = false;
    
    /**
     * A {@code boolean} that tells if the project uses scala.
     */
    private static boolean scala = false;
    
    /**
     * The {@link List} of dependencies for this project specified in the build
     * script.
     */
    private static List<String> deps = Lists.newArrayList();
    
    /**
     * The relative path to the dependencies file. Only needed if the project
     * uses the feature.
     */
    private static String depsFile;
    
    /**
     * A {@code boolean} that tells if the project uses the external
     * dependencies file feature.
     */
    private static boolean useDepsFile = false;
    
    /**
     * The {@link List} of dependencies already added to this project through
     * the {@link CelestiGradlePlugin}.
     */
    private static List<String> addedLibs = Lists.newArrayList();
    
    /**
     * The {@link Map} of registered dependencies.
     * <p/>
     * First the name, then the {@link Dependency}.
     */
    private static Map<String, Dependency> knownDeps = Maps.newHashMap();
    
    /**
     * The {@link Map} of alternative names for registered dependencies.
     * <p/>
     * First the alias, then the knownDeps key it leads to.
     */
    private static Map<String, List<String>> knownAliases = Maps.newHashMap();
    
    /**
     * An instance of the {@link Project}.
     */
    private Project project;
    
    /**
     * A {@code boolean} that tells if the project has a Java keystore property.
     */
    private boolean hasKeystore;
    
    /**
     * A {@code boolean} that tells if the project is stable.
     */
    private boolean isStable;
    
    /**
     * Apply this plugin to the given target object.
     *
     * @param target
     *            the target object.
     */
    @Override
    public void apply(Project target)
    {
        project = target;
        
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
        makeVersionTask();
        makeLifecycleTasks();
    }
    
    /**
     * Adds required maven repositories to the {@link Project}.
     */
    private void addRepositories()
    {
        project.allprojects(new Action<Project>()
        {
            @Override
            public void execute(Project project)
            {
                project.getRepositories().mavenCentral();
            }
        });
    }
    
    /**
     * Checks that all properties of the {@link Project} are set correctly and
     * also initializes some properties.
     */
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
    
    /**
     * Applies required external and internal {@link Plugin}s to the
     * {@link Project}.
     */
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
    
    /**
     * Registers a list of dependecies to this {@link Plugin} and adds required
     * dependencies to the {@link Project}.
     */
    @SuppressWarnings("unchecked")
    private void addDependencies()
    {
        if (!fg)
        {
            addDependency(project.fileTree("libs"));
        }
        
        registerCelestiDep("CelestiCore", "0.6.0", "celesticore", "core");
        registerCelestiDep("CelestiLib", "0.3.0", "celestilib", "lib");
        
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
        
        if (needsCore)
        {
            addDependency("CelestiCore", coreVersion);
        }
        
        if (useDepsFile)
        {
            try
            {
                List<String> lines = FileUtils.readLines(project.file(depsFile));
                
                for (String line : lines)
                {
                    line = line.trim();
                    
                    if (line.equals("") || line == null)
                    {
                        continue;
                    }
                    
                    if (line.contains(":"))
                    {
                        String[] array = line.split(":");
                        addDependency(array[0], array[1]);
                    }
                    else
                    {
                        addDependency(line);
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
            s = s.trim();
            
            if (s.contains(":"))
            {
                String[] array = s.split(":");
                addDependency(array[0], array[1]);
            }
            else
            {
                addDependency(s);
            }
        }
    }
    
    /**
     * Makes the tasks for setting up the Baubles dependency.
     */
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
    
    /**
     * Makes the tasks for building the {@link Project} archives.
     */
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
    
    /**
     * Makes the task for signing the {@link Project} archives.
     */
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
    
    /**
     * Makes the task for processing the {@link Project}'s version check files.
     */
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
                    
                    if (isMinecraftMod)
                    {
                        args.put("file", "./" + minecraftVersion + ".txt");
                    }
                    else
                    {
                        args.put("file", "./version.txt");
                    }
                    
                    invokeAnt("delete", args);
                    
                    if (isMinecraftMod)
                    {
                        FileUtils.writeStringToFile(project.file("./" + minecraftVersion + ".txt"), versionNumber);
                    }
                    else
                    {
                        FileUtils.writeStringToFile(project.file("./version.txt"), versionNumber);
                    }
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
    
    /**
     * Makes the task for processing the {@link Project}'s version check files.
     *
     * @deprecated
     */
    @SuppressWarnings("unused")
    @Deprecated
    private void makeJsonTask1()
    {
    }
    
    /**
     * Makes the tasks related to build's lifecycle.
     */
    private void makeLifecycleTasks()
    {
        DefaultTask release = makeTask("release");
        release.setDescription("Wrapper task for building release-ready archives.");
        release.setGroup(Reference.NAME);
        release.dependsOn("processVersionFile");
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param name
     *            the dependency name.
     */
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
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param dependency
     *            the dependency.
     */
    private void addDependency(Dependency dependency)
    {
        addDependency(dependency.getGroup(), dependency.getArtifact(), dependency.getVersion());
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param name
     *            the dependency name.
     * @param version
     *            the dependency version.
     */
    private void addDependency(String name, String version)
    {
        addDependency(knownDeps.get(name), version);
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param dependency
     *            the dependency.
     * @param version
     *            the dependency version.
     */
    private void addDependency(Dependency dependency, String version)
    {
        addDependency(dependency.getGroup(), dependency.getArtifact(), version);
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param group
     *            the dependency group.
     * @param name
     *            the dependency name.
     * @param version
     *            the dependency version.
     */
    private void addDependency(String group, String name, String version)
    {
        addDependency(project, group, name, version);
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param dependency
     *            the dependency.
     */
    private void addDependency(Object dependency)
    {
        addDependency(project, dependency);
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param project
     *            the {@link Project}.
     * @param group
     *            the dependency group.
     * @param name
     *            the dependency name.
     * @param version
     *            the dependency version.
     */
    public static void addDependency(Project project, String group, String name, String version)
    {
        addDependency(project, group + ":" + name + ":" + version);
    }
    
    /**
     * Adds a dependency to the {@link Project}.
     *
     * @param project
     *            the {@link Project}.
     * @param dependency
     *            the dependency.
     */
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
    
    /**
     * Registers a Celestibytes {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
    public static void registerCelestiDep(String name, String version, String... aliases)
    {
        registerDep(name, "io.github.celestibytes", name, version, aliases);
    }
    
    /**
     * Registers a Scala {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param artifact
     *            the dependency artifact.
     * @param aliases
     *            the dependency aliases.
     */
    public static void registerScala2(String name, String artifact, String... aliases)
    {
        registerScala(name, artifact, "2.11.2", aliases);
    }
    
    /**
     * Registers a Scala {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param artifact
     *            the dependency artifact.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
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
    
    /**
     * Registers an Apache Commons {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param artifact
     *            the dependency artifact.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
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
    
    /**
     * Registers an Apache Commons {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param artifact
     *            the dependency artifact.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
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
    
    /**
     * Registers an Apache HTTP {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param aliases
     *            the dependency aliases.
     */
    public static void registerHttp2(String name, String... aliases)
    {
        registerHttp(name, "4.3.5", aliases);
    }
    
    /**
     * Registers an Apache HTTP {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
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
    
    /**
     * Registers an Apache Log4J {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param aliases
     *            the dependency aliases.
     */
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
    
    /**
     * Registers a {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param group
     *            the dependency group.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
    public static void registerDep2(String name, String group, String version, String... aliases)
    {
        registerDep(name, group, name, version, aliases);
    }
    
    /**
     * Registers a {@link Dependency}.
     *
     * @param name
     *            the dependency name.
     * @param group
     *            the dependency group.
     * @param artifact
     *            the dependency artifact.
     * @param version
     *            the dependency version.
     * @param aliases
     *            the dependency aliases.
     */
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
    
    /**
     * Displays the banner in the beginning of the build.
     */
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
    
    /**
     * Makes a {@link DefaultTask}.
     *
     * @param name
     *            the name.
     * @return the {@link DefaultTask}.
     */
    public DefaultTask makeTask(String name)
    {
        return makeTask(name, DefaultTask.class);
    }
    
    /**
     * Makes a {@link Task}.
     *
     * @param name
     *            the name.
     * @param type
     *            the type.
     * @return the {@link Task}.
     */
    public <T extends Task> T makeTask(String name, Class<T> type)
    {
        return makeTask(project, name, type);
    }
    
    /**
     * Makes a {@link Task}.
     *
     * @param project
     *            the {@link Project}.
     * @param name
     *            the name.
     * @param type
     *            the type.
     * @return the {@link Task}.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Task> T makeTask(Project project, String name, Class<T> type)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("type", type);
        return (T) project.task(map, name);
    }
    
    /**
     * Invokes an ant task.
     *
     * @param task
     *            the task.
     * @param args
     *            the arguments.
     */
    public void invokeAnt(String task, Map<String, String> args)
    {
        invokeAnt(project, task, args);
    }
    
    /**
     * Invokes an ant task.
     *
     * @param project
     *            the {@link Project}.
     * @param task
     *            the task.
     * @param args
     *            the arguments.
     */
    public static void invokeAnt(Project project, String task, Map<String, String> args)
    {
        project.getAnt().invokeMethod(task, args);
    }
    
    /**
     * Applies an external {@link Plugin} to the {@link Project}.
     *
     * @param plugin
     *            the {@link Plugin}.
     */
    public void applyExternalPlugin(String plugin)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("plugin", plugin);
        project.apply(map);
    }
    
    /**
     * Adds a Maven repository to the {@link Project}.
     *
     * @param project
     *            the {@link Project}
     * @param name
     *            the name.
     * @param url
     *            the url.
     * @return the {@link MavenArtifactRepository}.
     */
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
    
    /**
     * Adds a flat file repository to the {@link Project}.
     *
     * @param project
     *            the {@link Project}
     * @param name
     *            the name.
     * @param dirs
     *            the directories.
     * @return the {@link FlatDirectoryArtifactRepository}.
     */
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
    
    /**
     * Creates a new {@link DelayedString}.
     *
     * @param path
     *            the path.
     * @return the {@link DelayedString}.
     */
    protected DelayedString delayedString(String path)
    {
        return new DelayedString(project, path, this);
    }
    
    /**
     * Creates a new {@link DelayedFile}.
     *
     * @param path
     *            the path.
     * @return the {@link DelayedFile}.
     */
    protected DelayedFile delayedFile(String path)
    {
        return new DelayedFile(project, path, this);
    }
    
    /**
     * Creates a new {@link DelayedFileTree}.
     *
     * @param path
     *            the path.
     * @return the {@link DelayedFileTree}.
     */
    protected DelayedFileTree delayedFileTree(String path)
    {
        return new DelayedFileTree(project, path, this);
    }
    
    /**
     * Creates a new {@link DelayedFileTree}.
     *
     * @param path
     *            the path.
     * @return the {@link DelayedFileTree}.
     */
    protected DelayedFileTree delayedZipTree(String path)
    {
        return new DelayedFileTree(project, path, true, this);
    }
    
    /**
     * Resolves a pattern.
     */
    @Override
    public String resolve(String pattern, Project project, BaseExtension extension)
    {
        pattern = pattern.replace("{PATH}", project.getPath().replace('\\', '/'));
        pattern = pattern.replace("{CORE_VERSION}", coreVersion);
        pattern = pattern.replace("{CORE_NAME}", Projects.CORE);
        return pattern;
    }
    
    /**
     * Gets a property from CelestialWizardry's version source file.
     *
     * @param project
     *            the {@link Project}.
     * @param field
     *            the field.
     * @return the property.
     * @throws IOException
     */
    public static String getCWVersion(Project project, String field) throws IOException
    {
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/celestibytes/celestialwizardry/reference/Versions.java", field);
    }
    
    /**
     * Gets a property from Doughcraft's version source file.
     *
     * @param project
     *            the {@link Project}.
     * @param field
     *            the field.
     * @return the property.
     * @throws IOException
     */
    public static String getDgCVersion(Project project, String field) throws IOException
    {
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/pizzana/doughcraft/reference/Versions.java", field);
    }
    
    /**
     * Gets a property from CelestiGradle's version source file.
     *
     * @param project
     *            the {@link Project}.
     * @param field
     *            the field.
     * @return the property.
     * @throws IOException
     */
    public static String getCGVersion(Project project, String field) throws IOException
    {
        return getProperty(project, "src/main/java/celestibytes/gradle/reference/Versions.java", field);
    }
    
    /**
     * Gets a property from CelestiCore's version source file.
     *
     * @param project
     *            the {@link Project}.
     * @param field
     *            the field.
     * @return the property.
     * @throws IOException
     */
    public static String getCoreVersion(Project project, String field) throws IOException
    {
        String s = scala ? "scala" : "java";
        return getProperty(project, "src/main/" + s + "/celestibytes/core/reference/Versions.java", field);
    }
    
    /**
     * Gets a property from a Scala or Java source file.
     *
     * @param project
     *            the {@link Project}.
     * @param file
     *            the relative file.
     * @param field
     *            the field.
     * @return the property.
     * @throws IOException
     */
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
    
    /**
     * Sets the {@link Project}'s version number.
     *
     * @param s
     *            the version number.
     * @return the version number.
     */
    public static String versionNumber(String s)
    {
        return setVersionNumber(s);
    }
    
    /**
     * Sets the {@link Project}'s version number.
     *
     * @param s
     *            the version number.
     * @return the version number.
     */
    public static String setVersionNumber(String s)
    {
        versionNumber = s;
        versionObj = Versions.parse(s);
        return versionNumber;
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} is a
     * Minecraft mod.
     *
     * @return the Minecraft version.
     */
    public static String mc()
    {
        return setMc();
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} is a
     * Minecraft mod.
     *
     * @param version
     *            the Minecraft version.
     * @return the Minecraft version.
     */
    public static String mc(String version)
    {
        return setMc(version);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} is a
     * Minecraft mod.
     *
     * @return the Minecraft version.
     */
    public static String setMc()
    {
        return setMc(Reference.DEFAULT_MINECRAFT_VERSION);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} is a
     * Minecraft mod.
     *
     * @param version
     *            the Minecraft version.
     * @return the Minecraft version.
     */
    public static String setMc(String version)
    {
        isMinecraftMod = true;
        minecraftVersion = version;
        return version;
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} depends on
     * CelestiCore.
     *
     * @param version
     *            the CelestiCore version.
     * @return the CelestiCore version.
     */
    public static String core(String version)
    {
        return setCore(version);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} depends on
     * CelestiCore.
     *
     * @param version
     *            the CelestiCore version.
     * @return the CelestiCore version.
     */
    public static String setCore(String version)
    {
        needsCore = true;
        coreVersion = version;
        return version;
    }
    
    /**
     * Sets the base package for this {@link Project}.
     *
     * @param s
     *            the base package.
     * @return the base package.
     */
    public static String basePackage(String s)
    {
        return setBasePackage(s);
    }
    
    /**
     * Sets the base package for this {@link Project}.
     *
     * @param s
     *            the base package.
     * @return the base package.
     */
    public static String setBasePackage(String s)
    {
        basePackage = s;
        dir = basePackage.replace('.', '/');
        return s;
    }
    
    /**
     * Sets the artifacts {@link List} for this {@link Project}.
     *
     * @param list
     *            the artifacts {@link List}.
     * @return the artifacts {@link List}.
     */
    public static List<String> artifactsList(List<String> list)
    {
        return setArtifactsList(list);
    }
    
    /**
     * Sets the artifacts {@link List} for this {@link Project}.
     *
     * @param list
     *            the artifacts {@link List}.
     * @return the artifacts {@link List}.
     */
    public static List<String> setArtifactsList(List<String> list)
    {
        artifactsList = list;
        return list;
    }
    
    /**
     * Adds an artifact to the artifacts {@link List} of this {@link Project}.
     *
     * @param s
     *            the artifact.
     * @return the artifact.
     */
    public static String artifact(String s)
    {
        return setArtifact(s);
    }
    
    /**
     * Adds an artifact to the artifacts {@link List} of this {@link Project}.
     *
     * @param s
     *            the artifact.
     * @return the artifact.
     */
    public static String setArtifact(String s)
    {
        artifactsList.add(s);
        return s;
    }
    
    /**
     * Sets a custom manifest to this {@link Project}.
     *
     * @param c
     *            the manifest.
     * @return the manifest.
     */
    @SuppressWarnings("rawtypes")
    public static Closure manifest(Closure c)
    {
        return setManifest(c);
    }
    
    /**
     * Sets a custom manifest to this {@link Project}.
     *
     * @param c
     *            the manifest.
     * @return the manifest.
     */
    @SuppressWarnings("rawtypes")
    public static Closure setManifest(Closure c)
    {
        hasManifest = true;
        manifest = c;
        return c;
    }
    
    /**
     * Enables the version check feature for this {@link Project}.
     */
    public static void versionCheck()
    {
        setVersionCheck();
    }
    
    /**
     * Enables the version check feature for this {@link Project}.
     */
    public static void setVersionCheck()
    {
        hasVersionCheck = true;
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} depends on
     * Baubles.
     *
     * @param s
     *            the Baubles version.
     * @return the Baubles version.
     */
    public static String baubles(String s)
    {
        return setBaubles(s);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} depends on
     * Baubles.
     *
     * @param s
     *            the Baubles version.
     * @param mc
     *            the Baubles Minecraft version.
     * @return the Baubles version.
     */
    public static String baubles(String s, String mc)
    {
        return setBaubles(s, mc);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} depends on
     * Baubles.
     *
     * @param s
     *            the Baubles version.
     * @return the Baubles version.
     */
    public static String setBaubles(String s)
    {
        return setBaubles(s, minecraftVersion);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} depends on
     * Baubles.
     *
     * @param s
     *            the Baubles version.
     * @param mc
     *            the Baubles Minecraft version.
     * @return the Baubles version.
     */
    public static String setBaubles(String s, String mc)
    {
        baublesVersion = s;
        baublesMinecraft = mc;
        needsBaubles = true;
        return s;
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} uses Scala.
     *
     * @return {@code true} if the {@link Project} uses Scala, otherwise
     *         {@code false}.
     */
    public static boolean scala()
    {
        return scala(true);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} uses Scala.
     *
     * @return {@code true} if the {@link Project} uses Scala, otherwise
     *         {@code false}.
     */
    public static boolean setScala()
    {
        return setScala(true);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} uses Scala.
     *
     * @param b
     *            is Scala enabled.
     * @return {@code true} if the {@link Project} uses Scala, otherwise
     *         {@code false}.
     */
    public static boolean scala(boolean b)
    {
        return setScala(b);
    }
    
    /**
     * Tells {@link CelestiGradlePlugin} that this {@link Project} uses Scala.
     *
     * @param b
     *            is Scala enabled.
     * @return {@code true} if the {@link Project} uses Scala, otherwise
     *         {@code false}.
     */
    public static boolean setScala(boolean b)
    {
        scala = b;
        return b;
    }
    
    /**
     * Sets the dependencies {@link List} for this {@link Project}.
     *
     * @param list
     *            the dependencies {@link List}.
     * @return the dependencies {@link List}.
     */
    public static List<String> depsList(List<String> list)
    {
        return setDepsList(list);
    }
    
    /**
     * Sets the dependencies {@link List} for this {@link Project}.
     *
     * @param list
     *            the dependencies {@link List}.
     * @return the dependencies {@link List}.
     */
    public static List<String> setDepsList(List<String> list)
    {
        deps = list;
        return list;
    }
    
    /**
     * Adds an dependency to the dependencies {@link List} of this
     * {@link Project}.
     *
     * @param s
     *            the dependency.
     * @return the dependency.
     */
    public static String dep(String s)
    {
        return setDep(s);
    }
    
    /**
     * Adds an dependency to the dependencies {@link List} of this
     * {@link Project}.
     *
     * @param s
     *            the dependency.
     * @return the dependency.
     */
    public static String setDep(String s)
    {
        deps.add(s);
        return s;
    }
    
    /**
     * Adds an dependency list file to this {@link Project}.
     *
     * @return the relative file.
     */
    public static String depsFile()
    {
        return setDepsFile(Reference.DEFAULT_DEPS_FILE);
    }
    
    /**
     * Adds an dependency list file to this {@link Project}.
     *
     * @return the relative file.
     */
    public static String setDepsFile()
    {
        return setDepsFile(Reference.DEFAULT_DEPS_FILE);
    }
    
    /**
     * Adds an dependency list file to this {@link Project}.
     *
     * @param s
     *            the relative file.
     * @return the relative file.
     */
    public static String depsFile(String s)
    {
        return setDepsFile(s);
    }
    
    /**
     * Adds an dependency list file to this {@link Project}.
     *
     * @param s
     *            the relative file.
     * @return the relative file.
     */
    public static String setDepsFile(String s)
    {
        depsFile = s;
        useDepsFile = true;
        return s;
    }
}
