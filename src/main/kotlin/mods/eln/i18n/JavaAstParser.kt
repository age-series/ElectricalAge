package mods.eln.i18n

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import java.io.File
import java.nio.charset.StandardCharsets

internal object JavaAstParser {

    fun parseFile(file: File): Set<TranslationItem> {
        try {
            val sourceText = file.readText(StandardCharsets.UTF_8)
            val sourceLines = sourceText.lines()
            val cu = StaticJavaParser.parse(sourceText)

            val hasI18NImport = cu.imports.any {
                it.nameAsString == "mods.eln.i18n.I18N" && !it.isStatic
            }
            val hasStaticTrImport = cu.imports.any {
                it.isStatic && it.nameAsString == "mods.eln.i18n.I18N.tr"
            }
            val hasStaticTRImport = cu.imports.any {
                it.isStatic && it.nameAsString == "mods.eln.i18n.I18N.TR"
            }

            val results = mutableSetOf<TranslationItem>()

            for (call in cu.findAll(MethodCallExpr::class.java)) {
                val methodName = call.nameAsString

                when {
                    methodName == "tr" || methodName == "TR" -> {
                        val scope = call.scope.map { it.toString() }.orElse("")
                        val isI18NCall = when {
                            scope == "I18N" && hasI18NImport -> true
                            scope == "" && methodName == "tr" && hasStaticTrImport -> true
                            scope == "" && methodName == "TR" && hasStaticTRImport -> true
                            else -> false
                        }
                        if (!isI18NCall) continue

                        val firstArg = call.arguments.firstOrNull() ?: continue
                        val text = resolveStringLiteral(firstArg, sourceLines) ?: continue
                        results.add(TranslationItem(text))
                    }

                    methodName == "TR_NAME" || methodName == "TR_DESC" -> {
                        val property = methodName.removePrefix("TR_").lowercase()
                        if (call.arguments.size != 2) continue

                        val typeArg = call.arguments[0] as? FieldAccessExpr ?: continue
                        val valueName = typeArg.nameAsString
                        val type = try {
                            I18N.Type.valueOf(valueName)
                        } catch (_: IllegalArgumentException) {
                            continue
                        }

                        val stringArg = call.arguments[1] ?: continue
                        val text = resolveStringLiteral(stringArg, sourceLines) ?: continue

                        val key = type.prefix +
                            I18N.encodeLangKey(text, type.isWhitespacesInFileReplaced()) +
                            "." + property
                        results.add(TranslationItem(key, text))
                    }
                }
            }

            return results
        } catch (e: Exception) {
            println("Failed to parse Java file: ${file.name}, error: ${e.message}")
            return emptySet()
        }
    }

    private fun resolveStringLiteral(expr: Expression, sourceLines: List<String>): String? {
        return when (expr) {
            is StringLiteralExpr -> extractRawSourceText(expr, sourceLines)
            is BinaryExpr -> {
                if (expr.operator != BinaryExpr.Operator.PLUS) null
                else {
                    val left = resolveStringLiteral(expr.left, sourceLines) ?: return null
                    val right = resolveStringLiteral(expr.right, sourceLines) ?: return null
                    left + right
                }
            }
            else -> null
        }
    }

    /**
     * Extracts the raw source text of a string literal by using the AST node's range
     * to slice the original source file. This preserves escape sequences exactly as
     * written in source (e.g., `\u2126`, `\n`, `\\`, `\"`) matching what the regex
     * approach would capture.
     *
     * JavaParser's `getValue()` decodes escape sequences (e.g., `\u2126` → Ω),
     * which makes it impossible to reconstruct the original source text faithfully.
     * By reading directly from source at the node's position, we get byte-identical
     * results to the regex-based approach.
     */
    private fun extractRawSourceText(literal: StringLiteralExpr, sourceLines: List<String>): String {
        val range = literal.range.orElse(null) ?: return literal.value
        val beginLine = range.begin.line - 1  // 0-indexed
        val beginCol = range.begin.column - 1  // 0-indexed, points to opening quote
        val endLine = range.end.line - 1
        val endCol = range.end.column  // points past closing quote

        if (beginLine == endLine) {
            val line = sourceLines[beginLine]
            return line.substring(beginCol + 1, endCol - 1)
        }

        val sb = StringBuilder()
        sb.append(sourceLines[beginLine].substring(beginCol + 1))
        for (i in (beginLine + 1) until endLine) {
            sb.append('\n')
            sb.append(sourceLines[i])
        }
        sb.append('\n')
        sb.append(sourceLines[endLine].substring(0, endCol - 1))
        return sb.toString()
    }
}
