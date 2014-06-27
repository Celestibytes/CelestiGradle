package net.minecraftforge.gradle.user.cg.util;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import java.io.IOException;
import java.util.List;

public final class GrepJava
{
    public static String getCWVersionFromJava(Project project, String field) throws IOException
    {
        return getPropertyFromJava(project, "src/main/java/celestialwizardry/reference/Version.java", field);
    }

    @SuppressWarnings("unchecked")
    public static String getPropertyFromJava(Project project, String file, String field) throws IOException
    {
        String version = "unknown";

        String prefix = "public static final String " + field;
        List<String> lines = (List<String>) FileUtils.readLines(project.file(file));

        for (String s : lines)
        {
            s = s.trim();

            if (s.startsWith(prefix))
            {
                s = s.substring(prefix.length(), s.length() - 1);
                s = s.replace('=', ' ').replace('"', ' ').replaceAll(" +", " ").trim();
                version = s;
                break;
            }
        }

        return version;
    }
}
