@file:JvmName("LanguageFileUpdater")

package mods.eln.i18n

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun main(args: Array<String>) {
    try {
        if (args.size != 2)
            System.exit(1)

        val srcFolder = File(args[0])
        val languageFileOrFolder = File(args[1])

        // Check if the source folder is present.
        if (!srcFolder.isDirectory)
            System.exit(1)

        // Get the strings to translate from the actual source code.
        val stringsToTranslate = SourceCodeParser.parseSourceFolder(srcFolder).toMutableMap()

        // If a single file is passed to the main method, we just update that particular file.
        if (languageFileOrFolder.isFile) {
            updateLanguageFile(languageFileOrFolder, stringsToTranslate)
        } else if (languageFileOrFolder.isDirectory) {
            val files = languageFileOrFolder.listFiles() ?: return
            for (file in files) {
                if (file.name.endsWith(".lang") && !file.name.startsWith("_")) {
                    updateLanguageFile(file, stringsToTranslate)
                }
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        Runtime.getRuntime().exit(1)
    }
}

@Throws(IOException::class)
private fun updateLanguageFile(languageFile: File, stringsToTranslate: Map<String, Set<TranslationItem>>) {
    val existingTranslations = parseLangFile(languageFile)

    // Update the existing language file.
    LanguageFileGenerator.updateFile(languageFile, stringsToTranslate, existingTranslations)
}

/**
 * Parses a .lang file into a key-value map.
 *
 * Unlike `Properties.load()` which decodes `\n` → newline, `\=` → `=`, `\:` → `:`,
 * this parser preserves keys exactly as written in the file. This ensures keys
 * match the format produced by `I18N.encodeLangKey()`, so existing translations
 * are correctly found during regeneration.
 *
 * Unicode escape sequences (`\uXXXX`) in both keys and values are resolved via
 * `I18N.resolveUnicodeEscapes()` so they are handled consistently with the
 * source code extraction pipeline.
 */
@Throws(IOException::class)
private fun parseLangFile(file: File): Map<String, String> {
    val result = LinkedHashMap<String, String>()
    val reader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))

    reader.use {
        var line: String? = it.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val separatorIndex = findUnescapedEquals(line)
                if (separatorIndex > 0) {
                    val key = I18N.resolveUnicodeEscapes(line.substring(0, separatorIndex))
                    val value = I18N.resolveUnicodeEscapes(line.substring(separatorIndex + 1))
                    result[key] = value
                }
            }
            line = it.readLine()
        }
    }

    return result
}

/**
 * Finds the index of the first '=' that is not preceded by '\' in the given string.
 * Returns -1 if not found.
 */
private fun findUnescapedEquals(line: String): Int {
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\\' && i + 1 < line.length) {
            i += 2
        } else if (c == '=') {
            return i
        } else {
            i++
        }
    }
    return -1
}
