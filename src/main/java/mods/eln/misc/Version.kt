package mods.eln.misc;

import mods.eln.Tags;
import org.semver4j.Semver;

import static mods.eln.i18n.I18N.tr;

/**
 *
 * ====================================================================================
 * WARNING! DO NOT CONVERT THIS TO KOTLIN WITHOUT CHECKING HOW GRADLE PARSES THIS FILE!
 * ====================================================================================
 * Current mod version. Used to check if a new mod version is available. Must be
 * set correctly for each mod release.
 *
 * @author metc
 */
public final class Version {

    /**
     * SemVer Version
     */
    public final static Semver SEMVER = Semver.parse(Tags.VERSION);

    /**
     * Major version code.
     */
    public final static int MAJOR = SEMVER.getMajor();

    /**
     * Minor version code.
     */
    public final static int MINOR = SEMVER.getMinor();

    /**
     * Revision version code.
     */
    public final static int REVISION = SEMVER.getPatch();

    // These attributes make the build not reproducible, should not bake in build-host information
    //public final static String BUILD_HOST = parseAdditionals("@BUILD_HOST@");
    //public final static String BUILD_DATE = parseAdditionals("@BUILD_DATE@");
    //public final static String JAVA_VERSION = parseAdditionals("@JAVA_VERSION@");
    public final static String BUILD_HOST = "";
    public final static String BUILD_DATE = "";
    public final static String JAVA_VERSION = "";

    // RFG doesn't output the build information in the semver build field, it puts it in the second prerelease field
    // instead for some reason
    public final static String GIT_REVISION = SEMVER.getPreRelease().size()>=2 ? SEMVER.getPreRelease().get(1) : "";

    /**
     * Unique version code. Must be a String for annotations. Used to check if a
     * new version if available. Each update must increment this number.
     */
    public final static int UNIQUE_VERSION = 1000000 * MAJOR + 1000 * MINOR + REVISION;

    public final static String VERSION_STRING = MAJOR + "." + MINOR + "." + REVISION;

    public static String getSimpleVersionName() {
        return VERSION_STRING;
    }

    public static String print() {
        return tr("mod.name") + " " + getSimpleVersionName();
    }

    public static String printColor() {
        return FC.WHITE + tr("mod.name") + " version "
            + FC.ORANGE + getSimpleVersionName();
    }

    public static void main(String... args) {
        System.out.print(getSimpleVersionName());
    }
}
