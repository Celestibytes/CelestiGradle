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
