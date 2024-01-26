package mods.eln

import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import net.minecraft.init.Items
import net.minecraft.stats.Achievement
import net.minecraftforge.common.AchievementPage

object Achievements {
    @JvmField
    var openGuide: Achievement? = null
    @JvmField
    var craft50VMacerator: Achievement? = null
    var achievementPageEln: AchievementPage? = null

    @JvmStatic
    fun init() {
        openGuide = Achievement(
            tr("achievement.open_guide"),
            "open_guide", 0, 0, Items.book, null
        ).registerStat()

        I18N.TR_DESC(I18N.Type.ACHIEVEMENT, "open_guide")

        craft50VMacerator = Achievement(
            tr("achievement.craft_50v_macerator"),
            "craft_50v_macerator", 0, 2, Eln.findItemStack("50V Macerator", 0), openGuide
        ).registerStat()

        I18N.TR_DESC(I18N.Type.ACHIEVEMENT, "craft_50v_macerator")

        achievementPageEln = AchievementPage(
            tr("Electrical Age [WIP]"),
            openGuide, craft50VMacerator
        )

        AchievementPage.registerAchievementPage(achievementPageEln)
    }
}
