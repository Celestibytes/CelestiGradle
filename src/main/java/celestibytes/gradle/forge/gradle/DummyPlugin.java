/*
 * Copyright (C) 2014 Celestibytes
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package celestibytes.gradle.forge.gradle;

import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.common.BasePlugin;
import net.minecraftforge.gradle.common.Constants;
import net.minecraftforge.gradle.delayed.DelayedFile;

public class DummyPlugin extends BasePlugin<BaseExtension>
{
    @Override
    public void applyPlugin()
    {
    }

    @Override
    public void applyOverlayPlugin()
    {
    }

    /**
     * return true if this plugin can be applied over another BasePlugin.
     */
    @Override
    public boolean canOverlayPlugin()
    {
        return false;
    }

    @Override
    protected DelayedFile getDevJson()
    {
        return null;
    }

    /**
     * @return the extension object with name EXT_NAME_MC
     *
     * @see Constants#EXT_NAME_MC
     */
    @Override
    protected BaseExtension getOverlayExtension()
    {
        return null;
    }
}
