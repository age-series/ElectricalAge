package mods.eln;

import mods.eln.registry.RegistryUtils;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

import static mods.eln.i18n.I18N.*;

public class Achievements {

    public static Achievement craft50VMacerator;
    public static AchievementPage achievementPageEln;

    public static void init() {
        // the last null is the parent achievement. TODO: Make actual achievements system :)
        craft50VMacerator = new Achievement(TR("achievement.craft_50v_macerator"),
            "craft_50v_macerator", 0, 2, RegistryUtils.findItemStack("50V Macerator", 0), null).registerStat();

        TR_DESC(Type.ACHIEVEMENT, "craft_50v_macerator");

        achievementPageEln = new AchievementPage(tr("Electrical Age [WIP]"),
            craft50VMacerator);

        AchievementPage.registerAchievementPage(achievementPageEln);
    }
}
