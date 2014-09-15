package net.minecraftforge.gradle.tasks.dev;

import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;

import com.google.common.io.Files;

import groovy.lang.Closure;
import net.minecraftforge.gradle.JsonUtil;
import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedFile;
import static net.minecraftforge.gradle.common.Constants.NEWLINE;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class GenDevProjectsTask extends DefaultTask
{
    protected DelayedFile targetDir;

    @Input
    protected DelayedFile json;

    private List<DelayedFile> sources = new ArrayList<DelayedFile>();
    private List<DelayedFile> resources = new ArrayList<DelayedFile>();
    private List<DelayedFile> testSources = new ArrayList<DelayedFile>();
    private List<DelayedFile> testResources = new ArrayList<DelayedFile>();

    private final ArrayList<String> deps = new ArrayList<String>();

    public GenDevProjectsTask()
    {
        this.getOutputs().file(getTargetFile());
    }

    @TaskAction
    public void doTask() throws IOException, InvalidSyntaxException
    {
        parseJson();
        writeFile();
    }

    private void parseJson() throws IOException, InvalidSyntaxException
    {
        JsonNode node = Constants.PARSER.parse(Files.newReader(getJson(), Charset.defaultCharset()));

        for (JsonNode lib : node.getArrayNode("libraries"))
        {
            if (lib.getStringValue("name").contains("fixed") || lib.isNode("natives") || lib.isNode("extract"))
            {
                continue;
            }
            else if (!lib.isNode("rules") || JsonUtil.ruleMatches(lib.getArrayNode("rules")))
            {
                deps.add(lib.getStringValue("name"));
            }
        }
    }

    private void writeFile() throws IOException
    {
        File file = getProject().file(getTargetFile().call());
        file.getParentFile().mkdirs();
        Files.touch(file);

        // prepare file string for writing.
        StringBuilder o = new StringBuilder();
        
        a(o, 
            "apply plugin: 'java' ",
            "apply plugin: 'eclipse'",
            "",
            "sourceCompatibility = '1.6'",
            "targetCompatibility = '1.6'",
            "",
            "repositories",
            "{",
            "    maven",
            "    {",
            "        name 'forge'",
            "        url 'http://files.minecraftforge.net/maven'",
            "    }",
            "    mavenCentral()",
            "    maven",
            "    {",
            "        name 'sonatypeSnapshot'",
            "        url 'https://oss.sonatype.org/content/repositories/snapshots/'",
            "    }",
            "    maven",
            "    {",
            "        name 'minecraft'",
            "        url '" + Constants.LIBRARY_URL + "'",
            "    }",
            "}",
            "",
            "dependencies",
            "{"
        );
        
        // read json, output json in gradle freindly format...
        for (String dep : deps)
        {
            o.append("    compile '").append(dep).append('\'').append(NEWLINE);
        }

        a(o, 
            "",
            "    testCompile 'junit:junit:4.5'", 
            "}",
            ""
        );

        URI base = targetDir.call().toURI();

        if (resources.size() > 0 || sources.size() > 0 || testSources.size() > 0 || testResources.size() > 0)
        {
            a(o, "sourceSets");
            a(o, "{");
            a(o, "    main");
            a(o, "    {");
            if (sources.size() > 0)
            {
                a(o, "        java");
                a(o, "        {");
                for (DelayedFile src : sources)
                {
                    o.append("            srcDir '").append(relative(base, src)).append('\'').append(NEWLINE);
                }
                a(o, "        }");
            }
            if (resources.size() > 0)
            {
                a(o, "        resources");
                a(o, "        {");
                for (DelayedFile src : resources)
                {
                    o.append("            srcDir '").append(relative(base, src)).append('\'').append(NEWLINE);
                }
                a(o, "        }");
            }
            a(o, "    }");
            a(o, "    test");
            a(o, "    {");
            if (testSources.size() > 0)
            {
                a(o, "        java");
                a(o, "        {");
                for (DelayedFile src : testSources)
                {
                    o.append("            srcDir '").append(relative(base, src)).append('\'').append(NEWLINE);
                }
                a(o, "        }");
            }
            if (testResources.size() > 0)
            {
                a(o, "        resources");
                a(o, "        {");
                for (DelayedFile src : testResources)
                {
                    o.append("            srcDir '").append(relative(base, src)).append('\'').append(NEWLINE);
                }
                a(o, "        }");
            }
            a(o, "    }");
            a(o, "}");
        }
        
        // and now eclipse hacking
        a(o,
                "def links = []",
                "def dupes = []",
                "eclipse.project.file.withXml { provider ->",
                "    def node = provider.asNode()",
                "    links = []",
                "    dupes = []",
                "    node.linkedResources.link.each { child ->",
                "        def path = child.location.text()",
                "        if (path in dupes) {",
                "            child.replaceNode {}",
                "        } else {",
                "            dupes.add(path)",
                "            def newName = path.split('/')[-2..-1].join('/')",
                "            links += newName",
                "            child.replaceNode {",
                "                link{",
                "                    name(newName)",
                "                    type('2')",
                "                    location(path)",
                "                }",
                "            }",
                "        }",
                "    }",
                "}",
                "",
                "eclipse.classpath.file.withXml {",
                "    def node = it.asNode()",
                "    node.classpathentry.each { child -> ",
                "        if (child.@kind == 'src' && !child.@path.contains('/')) child.replaceNode {}",
                "        if (child.@path in links) links.remove(child.@path)",
                "    }",
                "    links.each { link -> node.appendNode('classpathentry', [kind:'src', path:link]) }",
                "}",
                "tasks.eclipseClasspath.dependsOn 'eclipseProject' //Make them run in correct order"
        );

        Files.write(o.toString(), file, Charset.defaultCharset());
    }

    private String relative(URI base, DelayedFile src)
    {
        String relative = base.relativize(src.call().toURI()).getPath().replace('\\', '/');
        if (!relative.endsWith("/")) relative += "/";
        return relative;
    }

    private void a(StringBuilder out, String... lines)
    {
        for (String line : lines)
        {
            out.append(line).append(NEWLINE);
        }
    }

    private Closure<File> getTargetFile()
    {
        return new Closure<File>(this)
        {
            private static final long serialVersionUID = -6333350974905684295L;

            @Override
            public File call()
            {
                return new File(getTargetDir(), "build.gradle");
            }

            @Override
            public File call(Object obj)
            {
                return new File(getTargetDir(), "build.gradle");
            }
        };
    }

    public File getTargetDir()
    {
        return targetDir.call();
    }

    public void setTargetDir(DelayedFile targetDir)
    {
        this.targetDir = targetDir;
    }

    public GenDevProjectsTask addSource(DelayedFile source)
    {
        sources.add(source);
        return this;
    }

    public GenDevProjectsTask addResource(DelayedFile resource)
    {
        resources.add(resource);
        return this;
    }
    
    public GenDevProjectsTask addTestSource(DelayedFile source)
    {
        testSources.add(source);
        return this;
    }

    public GenDevProjectsTask addTestResource(DelayedFile resource)
    {
        testResources.add(resource);
        return this;
    }

    public File getJson()
    {
        return json.call();
    }

    public void setJson(DelayedFile json)
    {
        this.json = json;
    }
}