package mods.eln.i18n

internal class TranslationItem(val key: String?, var text: String) : Comparable<TranslationItem> {

    constructor(text: String) : this(I18N.encodeLangKey(text), text)

    fun isValid(): Boolean = key != null

    fun applyExistingTranslationIfPresent(existing: Map<String, String>?) {
        if (existing != null && key != null) {
            val existingText = existing[key]
            if (existingText != null) {
                text = existingText
            }
        }
    }

    override fun compareTo(other: TranslationItem): Int {
        return key!!.compareTo(other.key!!)
    }

    override fun equals(other: Any?): Boolean {
        return other is TranslationItem && compareTo(other) == 0 ||
            other is String && key!!.compareTo(other) == 0
    }

    override fun hashCode(): Int {
        return key?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "$key=${text.replace("\\\"", "\"")}\n"
    }
}
