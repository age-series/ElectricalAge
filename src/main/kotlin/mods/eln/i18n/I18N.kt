package mods.eln.i18n

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.registry.LanguageRegistry
import mods.eln.misc.Utils

/**
 * Internationalization and localization helper class.
 */
object I18N {
    private val languageRegistry: LanguageRegistry? by lazy {
        try { LanguageRegistry.instance() } catch (_: Throwable) { null }
    }

    /**
     * Defines the different translatable types.
     */
    enum class Type(
        /** Prefix for the type of translatable text. */
        val prefix: String,
        private val encodeAtRuntime: Boolean,
        private val replaceWhitespacesInFile: Boolean
    ) {
        /**
         * The text to translate is not related to a particular translatable type, so basically only the ".name" suffix
         * is added to the translation key.
         */
        NONE("", false, true),

        /**
         * The text to translate is related to an item. The "item." runtimePrefix will be added to the translation key.
         */
        ITEM("item.", false, false),

        /**
         * The text to translate is related to a tile. The "tile." runtimePrefix will be added to the translation key.
         */
        TILE("tile.", false, false),

        /**
         * The text to translate is related to an achievement. The "achievement." runtimePrefix will be added to the
         * translation key.
         */
        ACHIEVEMENT("achievement.", true, true),

        /**
         * The text to translate is related to an entity. The "entity." runtimePrefix will be added to the translation key.
         */
        ENTITY("entity.", false, false),

        /**
         * The text to translate is related to a death attack. The "death.attack" runtimePrefix will be added to the
         * translation key.
         */
        DEATH_ATTACK("death.attack.", false, false),

        /**
         * The text to translate is related to an item group. The "itemGroup." runtimePrefix will be added to the translation
         * key.
         */
        ITEM_GROUP("itemGroup.", false, false),

        /**
         * The text to translate is related to a container. The "container." runtimePrefix will be added to the translation
         * key.
         */
        CONTAINER("container.", false, false),

        /**
         * The text to translate is related to an block. The "block." runtimePrefix will be added to the translation key.
         */
        BLOCK("block.", false, false),

        SIX_NODE("eln.sixnode.", false, true),

        NODE("eln.node.", false, true);

        fun isEncodedAtRuntime(): Boolean = encodeAtRuntime

        fun isWhitespacesInFileReplaced(): Boolean = replaceWhitespacesInFile
    }

    @JvmStatic
    fun getCurrentLanguage(): String {
        return FMLCommonHandler.instance().currentLanguage
    }

    internal fun encodeLangKey(key: String): String {
        return encodeLangKey(key, true)!!
    }

    internal fun encodeLangKey(key: String?, replaceWhitespaces: Boolean): String? {
        if (key != null) {
            var result = resolveUnicodeEscapes(key)
            if (replaceWhitespaces) {
                result = result.replace(' ', '_')
            }
            return result
                .replace("=", "\\=")
                .replace(":", "\\:")
                .replace("\n", "\\n")
        } else {
            return null
        }
    }

    internal fun resolveUnicodeEscapes(s: String): String {
        if (!s.contains("\\u")) return s
        return UNICODE_ESCAPE_REGEX.replace(s) { match ->
            val codePoint = match.groupValues[1].toInt(16)
            codePoint.toChar().toString()
        }
    }

    private val UNICODE_ESCAPE_REGEX = Regex("(?<!\\\\)\\\\u([0-9a-fA-F]{4})")

    /**
     * Translates the given string. You can pass arguments to the method and reference them in the string using
     * the placeholders %N$ whereas N is the index of the actual parameter **starting at 1**.
     *
     * Example: tr("You have %1$ lives left", 4);
     *
     * IT IS IMPORTANT THAT YOU PASS THE **STRING LITERALS** AT LEAST ONCE AS THE FIRST PARAMETER TO THIS METHOD or
     * you call the method TR() with the actual string literal in order to register the translation text automatically!
     * Otherwise the translation will not be added to the language files. There is no problem to use the tr() method
     * afterwards using an already registered string in the code using a string variable as the first parameter.
     *
     * @param text    Text to translate
     * @param objects Arguments to integrate into the text.
     * @return Translated text or original text (Argument placeholders are replaced by the actual arguments
     * anyway) if no translation is present.
     */
    @JvmStatic
    fun tr(text: String, vararg objects: Any?): String {
        // Try to find the translation for the string using forge API.
        var translation: String? = null
        try {
            translation = languageRegistry?.getStringLocalization(encodeLangKey(text))
        } catch (_: Throwable) {
            // Forge classes may not be available in test environments (ExceptionInInitializerError,
            // NoClassDefFoundError, etc.). Fall back to the original text.
        }

        // If no translation was found, just use the original text.
        if (translation == null || "" == translation) {
            translation = text
        } else {
            // Replace placeholders.
            translation = translation.replace("\\n", "\n").replace("\\:", ":")
        }

        // Replace placeholders in string by actual string values of the passed objects.
        for (i in objects.indices) {
            translation = translation!!.replace("%${i + 1}\$", objects[i].toString())
        }

        return translation!!
    }

    /**
     * This method can be used to mark an unlocalized text in order to add it to the generated language files.
     * The method does not actually translate the text - it marks the text literal only to be translated afterwards.
     * A common use case is to add text to the language file which is translated using a text variable with the
     * method tr().
     *
     * @param text String LITERAL to add to the language files.
     * @return Exactly the same text as given to the method.
     */
    @JvmStatic
    fun TR(text: String): String {
        return encodeLangKey(text)
    }

    /**
     * Used to register a name to translate. The forge mechanisms are used in order to translate the name.
     *
     * @param type Type the translatable name is related to.
     * @param text String LITERAL to register for translation.
     * @return Returns the same text literal, forge will translate the name magically.
     */
    @JvmStatic
    fun TR_NAME(type: Type, text: String): String {
        if (type.isEncodedAtRuntime()) {
            return "${type.prefix}${encodeLangKey(text)}.name"
        } else {
            return text
        }
    }

    /**
     * Used to register a description to translate. The forge mechanisms are used in order to translate the description.
     *
     * @param type Type the translatable description is related to.
     * @param text String LITERAL to register for translation.
     * @return Returns the same text literal, forge will translate the description magically.
     */
    @JvmStatic
    fun TR_DESC(type: Type, text: String): String {
        if (type.isEncodedAtRuntime()) {
            return "${type.prefix}${encodeLangKey(text)}.desc"
        } else {
            return text
        }
    }

    /**
     * Registers an item group (creative tab) translation.
     *
     * The [groupId] is the label passed to `CreativeTabs` constructor (e.g. "ElnCables").
     * Forge automatically looks up the translation key `itemGroup.{groupId}` at runtime.
     * The [englishName] is the display name used as the value in the generated language files.
     *
     * Example: `TR_GROUP("ElnCables", "Electrical Age - Cables")`
     * → generates key `itemGroup.ElnCables` with value `Electrical Age - Cables`
     *
     * @param groupId     The creative tab label (without the `itemGroup.` prefix).
     * @param englishName The English display name for this creative tab.
     * @return The group ID, so it can be passed directly to the `GenericCreativeTab` constructor.
     */
    @JvmStatic
    fun TR_GROUP(groupId: String, englishName: String): String {
        return groupId
    }

    /**
     * Registers dynamically generated names by declaring a format string and the axis arrays
     * whose Cartesian product produces every concrete name.
     *
     * The [format] string uses `%s` placeholders which are substituted with values from [axes]
     * in positional order. The AST parser expands all combinations and generates a
     * [TranslationItem] for each, using the same key encoding as [TR_NAME].
     *
     * Example:
     * ```kotlin
     * TR_EXPAND(Type.NONE, "%s %s Cable %s",
     *     arrayOf("Copper", "Aluminum"),
     *     arrayOf("26 AWG", "24 AWG"),
     *     arrayOf("Bare", "300V", "Melted")
     * )
     * ```
     * This generates 12 translation keys:
     * `Copper_26_AWG_Cable_Bare.name`, `Copper_26_AWG_Cable_300V.name`, …
     *
     * At runtime this method returns [format] unchanged — it is purely a marker for the
     * build-time language file generator.
     *
     * @param type   The translation type (determines key prefix and encoding).
     * @param format Format string with `%s` placeholders, one per axis.
     * @param axes   String arrays whose Cartesian product fills the placeholders.
     * @return The format string unchanged.
     */
    @JvmStatic
    fun TR_EXPAND(type: Type, format: String, vararg axes: Array<String>): String {
        return format
    }
}
