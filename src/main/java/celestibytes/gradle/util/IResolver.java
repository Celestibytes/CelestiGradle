package celestibytes.gradle.util;

import net.minecraftforge.gradle.common.BaseExtension;
import net.minecraftforge.gradle.delayed.DelayedBase;

import celestibytes.gradle.common.AbstractExtension;
import org.gradle.api.Project;

public interface IResolver<K extends AbstractExtension> extends DelayedBase.IDelayedResolver<BaseExtension>
{
    String resolve(String pattern, Project project, K extension);
}
