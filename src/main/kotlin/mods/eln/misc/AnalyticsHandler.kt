package mods.eln.misc

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.misc.Version.simpleVersionName
import net.minecraft.client.Minecraft
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.IOException

object AnalyticsHandler {
    private fun sendHttpRequest(url: String) {
        try {
            val client = HttpClientBuilder.create().build()
            val response = client.execute(HttpGet(url))

            val repCode = response.statusLine.statusCode
            if (repCode != HttpStatus.SC_OK) throw IOException("HTTP error $repCode")

            response.close()
            client.close()
        } catch (e: Exception) {
            val error = "Unable to send analytics data: " + e.message + "."
            System.err.println(error)
        }
    }

    fun submitUpstreamAnalytics() {
        val URL = "http://mc.electrical-age.net/version.php?id=%s&v=%s&l=%s"
        val analyticsThread = Thread {
            // Prepare get parameters
            val version = simpleVersionName.replace("\\s+".toRegex(), "")
            val lang = I18N.getCurrentLanguage()
            val url = String.format(URL, Eln.playerUUID, version, lang)
            sendHttpRequest(url)
        }
        analyticsThread.start()
    }

    /**
    Things I am gathering (and why):

    * version: to be able to know what versions of the mod are being used
    * lang: to know what language packs I should be having people focus on (perhaps advertise they can add language translations by contacting us)
    * playerUUID: to be able to tell different analytics requests apart
    * cableFactor: to know how many people are using this feature (I plan to remove it)
    * cableResistanceMultiplier: to know if people are experimenting or using a different value than default (better stability with higher values?)
    * explosionEnable: to know if explosions are something people are disabling because of a nuisance (I plan to redo them a bit)
    * replicatorPop: how many people (like me) don't care for replicators (perhaps we can make a more fun passive mob!) *Electropuppy!*

    TODO: Remove cableFactor, cableResistanceMultiplier. Add watchdog enabled status.

    As I see fit, I may add more things in newer releases to see what people are doing with the mod, and what direction to go in.
    */
    fun submitAgeSeriesAnalytics() {
        val ageSeriesAnalyticsThread = Thread {
            var url: String? = ""
            val version = simpleVersionName.replace("\\s+".toRegex(), "")
            val lang = I18N.getCurrentLanguage()

            if (Eln.analyticsPlayerUUIDOptIn && FMLCommonHandler.instance().effectiveSide == Side.CLIENT) {
                // PLAYER HAS OPTED INTO SENDING THEIR UUID (and is not a server)
                val formatUrl = "%s?version=%s&lang=%s&uuid=%s&name=%s"
                url = String.format(
                    formatUrl,
                    Eln.analyticsURL,
                    version,
                    lang,
                    Minecraft.getMinecraft().session.func_148256_e().id.toString(),
                    Minecraft.getMinecraft().session.playerID
                )
            } else {
                // PLAYER HAS NOT OPTED INTO SENDING THEIR UUID
                val formatUrl = "%s?version=%s&lang=%s&uuid=%s"
                url = String.format(formatUrl, Eln.analyticsURL, version, lang, Eln.playerUUID)
            }
            sendHttpRequest(url)
        }
        ageSeriesAnalyticsThread.start()
    }
}
