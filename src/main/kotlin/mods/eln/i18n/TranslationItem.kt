package mods.eln.i18n

internal class TranslationItem(val key: String?, val text: String) : Comparable<TranslationItem> {

    constructor(text: String) : this(I18N.encodeLangKey(text), text)

    fun isValid(): Boolean = key != null

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

internal fun cartesianProduct(axes: List<List<String>>): List<List<String>> {
    if (axes.isEmpty()) return listOf(emptyList())
    val first = axes.first()
    val rest = cartesianProduct(axes.drop(1))
    return first.flatMap { head -> rest.map { tail -> listOf(head) + tail } }
}

internal fun substituteFormat(format: String, values: List<String>): String {
    var idx = 0
    val result = buildString {
        var i = 0
        while (i < format.length) {
            if (i + 1 < format.length && format[i] == '%' && format[i + 1] == 's') {
                if (idx < values.size) {
                    append(values[idx++])
                }
                i += 2
            } else {
                append(format[i])
                i++
            }
        }
    }
    if (idx != values.size) {
        println("WARNING: TR_EXPAND format '$format' has ${if (idx < values.size) "fewer" else "more"} %s placeholders than axis values provided (${values.size} values, $idx substituted)")
    }
    return result
}
