package mods.eln.misc

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Obj3DFolderPathParsingTest {
    @Test
    fun codeSourceLocationParserPreservesPlusAndDecodesEscapes() {
        val parsedPlus = Obj3DFolder.codeSourceLocationToFile("file:/tmp/llv29.2-tfc+/mods/Eln.jar")
        assertTrue(parsedPlus.path.contains("llv29.2-tfc+"), "Expected '+' to be preserved in path")

        val parsedEscapes = Obj3DFolder.codeSourceLocationToFile("file:/tmp/pack%20name/mod%231.jar")
        assertTrue(parsedEscapes.path.contains("pack name"), "Expected %20 to decode to space")
        assertTrue(parsedEscapes.path.contains("mod#1.jar"), "Expected %23 to decode to #")
    }

    @Test
    fun codeSourceLocationParserHandlesJarPrefixAndBangSuffix() {
        val expected = File("/tmp/llv29.2-tfc+/mods/Eln.jar").absoluteFile
        val parsed = Obj3DFolder.codeSourceLocationToFile("jar:file:/tmp/llv29.2-tfc+/mods/Eln.jar!/mods/eln/Eln.class")
        assertEquals(expected.path, parsed.absoluteFile.path)
    }

    @Test
    fun codeSourceLocationParserHandlesCommonNtfsSafeCharacters() {
        val chars = listOf("+", ",", ";", "=", "@", "!", "$", "&", "'", "(", ")", "[", "]")
        for (c in chars) {
            val expected = File("/tmp/ea${c}name/mod${c}file.jar").absoluteFile
            val parsed = Obj3DFolder.codeSourceLocationToFile(expected.toURI().toString())
            assertEquals(expected.path, parsed.absoluteFile.path, "Failed for character '$c'")
        }
    }
}
