package mods.eln.i18n

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal object LanguageFileGenerator {
    private const val FILE_HEADER = "#<ELN_LANGFILE_V1_1>\n"

    @Throws(IOException::class)
    @JvmStatic
    fun updateFile(
        file: File,
        strings: Map<String, Set<TranslationItem>>,
        existingTranslations: Map<String, String>?
    ) {
        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
            // Write header.
            writer.append(FILE_HEADER)

            // For each source file with translations create the file comment.
            for (sourceFile in strings.keys) {
                // Standardise file paths for every platforms
                val sourcePath = sourceFile.replace("\\", "/")
                writer.append("\n# ").append(sourcePath).append("\n")

                // For each translated string in that source file, add translation text.
                for (text2Translate in strings[sourceFile]!!) {
                    val effectiveText = existingTranslations?.get(text2Translate.key) ?: text2Translate.text
                    val resolvedText = I18N.resolveUnicodeEscapes(effectiveText).replace("\\\"", "\"")
                    writer.append("${text2Translate.key}=$resolvedText\n")
                }
            }
        }
    }
}
