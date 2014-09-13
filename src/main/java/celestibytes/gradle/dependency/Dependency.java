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

package celestibytes.gradle.dependency;

import com.google.common.collect.Lists;

import java.util.List;

public final class Dependency
{
    private final String name;
    private final String group;
    private final String artifact;
    private final String version;
    private final List<String> aliases;
    
    public Dependency(String group, String artifact, String version, String... aliases)
    {
        this(artifact, group, artifact, version, aliases);
    }
    
    public Dependency(String name, String group, String artifact, String version, String... aliases)
    {
        this.name = name;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.aliases = Lists.newArrayList(aliases);
    }
    
    /**
     * Gives the name of the {@link Dependency}.
     *
     * @return the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gives the aliases of the {@link Dependency}.
     *
     * @return the aliases.
     */
    public List<String> getAliases()
    {
        return aliases;
    }
    
    /**
     * Gives the group of the {@link Dependency}.
     *
     * @return the group.
     */
    public String getGroup()
    {
        return group;
    }
    
    /**
     * Gives the artifact of the {@link Dependency}.
     *
     * @return the artifact.
     */
    public String getArtifact()
    {
        return artifact;
    }
    
    /**
     * Gives the version of the {@link Dependency}.
     *
     * @return the version.
     */
    public String getVersion()
    {
        return version;
    }
}
