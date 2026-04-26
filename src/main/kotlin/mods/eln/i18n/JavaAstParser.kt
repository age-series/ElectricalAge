package mods.eln.i18n

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.ArrayCreationExpr
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

                    methodName == "TR_GROUP" -> {
                        if (call.arguments.size != 2) continue

                        val groupIdArg = call.arguments[0] ?: continue
                        val groupId = resolveStringLiteral(groupIdArg, sourceLines) ?: continue

                        val nameArg = call.arguments[1] ?: continue
                        val englishName = resolveStringLiteral(nameArg, sourceLines) ?: continue

                        results.add(TranslationItem("itemGroup.$groupId", englishName))
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

                    methodName == "TR_EXPAND" -> {
                        if (call.arguments.size < 3) continue

                        val typeArg = call.arguments[0] as? FieldAccessExpr ?: continue
                        val type = try {
                            I18N.Type.valueOf(typeArg.nameAsString)
                        } catch (_: IllegalArgumentException) {
                            continue
                        }

                        val formatArg = call.arguments[1] ?: continue
                        val format = resolveStringLiteral(formatArg, sourceLines) ?: continue

                        val axisArrays = mutableListOf<List<String>>()
                        for (i in 2 until call.arguments.size) {
                            val axisArg = call.arguments[i] ?: continue
                            val values = resolveStringArrayLiteral(axisArg, sourceLines) ?: continue
                            axisArrays.add(values)
                        }

                        for (combo in cartesianProduct(axisArrays)) {
                            val name = substituteFormat(format, combo)
                            val key = type.prefix +
                                I18N.encodeLangKey(name, type.isWhitespacesInFileReplaced()) +
                                ".name"
                            results.add(TranslationItem(key, name))
                        }
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

    private fun resolveStringArrayLiteral(expr: Expression, sourceLines: List<String>): List<String>? {
        return when (expr) {
            is MethodCallExpr -> {
                if (expr.nameAsString != "arrayOf") return null
                val values = expr.arguments.mapNotNull { arg ->
                    resolveStringLiteral(arg, sourceLines)
                }
                values.takeIf { it.size == expr.arguments.size }
            }
            is ArrayCreationExpr -> {
                val initializer = expr.initializer.orElse(null) ?: return null
                val values = initializer.values.mapNotNull { arg ->
                    resolveStringLiteral(arg, sourceLines)
                }
                values.takeIf { it.size == initializer.values.size }
            }
            else -> null
        }
    }
}
