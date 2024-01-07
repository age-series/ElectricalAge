package mods.eln.misc

import mods.eln.Tags
import mods.eln.i18n.I18N.tr
import org.semver4j.Semver

/**
 *
 * Current mod version. Used to check if a new mod version is available. Must be
 * set correctly for each mod release.
 *
 * @author metc
 */
object Version {
    /**
     * SemVer Version
     */
    val SEMVER = Semver.parse(Tags.VERSION.replace(".dirty", ""))

    /**
     * Major version code.
     */
    val MAJOR = SEMVER.major

    /**
     * Minor version code.
     */
    val MINOR = SEMVER.minor

    /**
     * Revision version code.
     */
    val REVISION = SEMVER.patch

    /**
     * If there are uncommited changes in the project
     */
    val DIRTY = Tags.VERSION.contains(".dirty")

    // RFG doesn't output the build information in the semver build field, it puts it in the second prerelease field
    // instead for some reason
    val GIT_REVISION = if (SEMVER.preRelease.size >= 2) SEMVER.preRelease[1] else ""

    /**
     * Unique version code. Must be a String for annotations. Used to check if a
     * new version if available. Each update must increment this number.
     */
    @JvmField
    val UNIQUE_VERSION = 1000000 * MAJOR + 1000 * MINOR + REVISION
    val simpleVersionName = if (!DIRTY) "$MAJOR.$MINOR.$REVISION" else "$MAJOR.$MINOR.$REVISION (Dirty)"
    @JvmStatic
    fun print(): String {
        return tr("mod.name") + " " + simpleVersionName
    }

    @JvmStatic
    fun printColor(): String {
        return (FC.WHITE + tr("mod.name") + " version "
                + FC.ORANGE + simpleVersionName)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        print(simpleVersionName)
    }
}
