package celestibytes.gradle.util;

import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.delayed.DelayedBase;

import celestibytes.gradle.CelestiExtension;
import org.gradle.api.Project;

public interface IResolver<K extends CelestiExtension> extends DelayedBase.IDelayedResolver<BaseExtension>
{
    String resolve(String pattern, Project project, K extension);
}