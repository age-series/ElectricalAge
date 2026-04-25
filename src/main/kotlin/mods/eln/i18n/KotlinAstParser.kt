package mods.eln.i18n

import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtExpression
import java.io.File
import java.nio.charset.StandardCharsets

internal class KotlinAstParser {
    private val disposable = Disposer.newDisposable()
    private val environment: KotlinCoreEnvironment by lazy {
        val configuration = CompilerConfiguration()
        KotlinCoreEnvironment.createForProduction(
            disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
    private val psiFileFactory: PsiFileFactory by lazy {
        PsiFileFactory.getInstance(environment.project)
    }

    fun close() {
        Disposer.dispose(disposable)
    }

    fun parseFile(file: File): Set<TranslationItem> {
        try {
            val sourceText = file.readText(StandardCharsets.UTF_8)
            val ktFile = psiFileFactory.createFileFromText(
                file.name, KotlinLanguage.INSTANCE, sourceText
            ) as KtFile

            val importPaths = ktFile.importDirectives.mapNotNull { it.importPath?.pathStr }
            val importsTr = "mods.eln.i18n.I18N.tr" in importPaths
            val importsTR = "mods.eln.i18n.I18N.TR" in importPaths
            val importsTR_NAME = "mods.eln.i18n.I18N.TR_NAME" in importPaths
            val importsTR_DESC = "mods.eln.i18n.I18N.TR_DESC" in importPaths
            val importsI18N = "mods.eln.i18n.I18N" in importPaths

            val result = mutableSetOf<TranslationItem>()

            val calls = ktFile.collectDescendantsOfType<KtCallExpression>()
            for (call in calls) {
                val calleeText = call.calleeExpression?.text ?: continue
                val valueArgs = call.valueArguments
                val methodName = calleeText.substringAfterLast(".")
                val isQualified = "." in calleeText

                when (methodName) {
                    "tr" -> {
                        if (isQualified || importsTr || importsI18N) {
                            extractTrCall(valueArgs, result)
                        }
                    }
                    "TR" -> {
                        if (isQualified || importsTR || importsI18N) {
                            extractTrCall(valueArgs, result)
                        }
                    }
                    "TR_GROUP" -> {
                        if (isQualified || "mods.eln.i18n.I18N.TR_GROUP" in importPaths || importsI18N) {
                            extractTrGroupCall(valueArgs, result)
                        }
                    }
                    "TR_NAME" -> {
                        if (isQualified || importsTR_NAME || importsI18N) {
                            extractForgeCall(valueArgs, "name", result)
                        }
                    }
                    "TR_DESC" -> {
                        if (isQualified || importsTR_DESC || importsI18N) {
                            extractForgeCall(valueArgs, "desc", result)
                        }
                    }
                    "TR_EXPAND" -> {
                        if (isQualified || "mods.eln.i18n.I18N.TR_EXPAND" in importPaths || importsI18N) {
                            extractExpandCall(valueArgs, result)
                        }
                    }
                }
            }

            return result
        } catch (e: Exception) {
            println("Failed to parse Kotlin file: ${file.name}, error: ${e.message}")
            return emptySet()
        }
    }

    private fun extractTrGroupCall(
        args: List<KtValueArgument>,
        result: MutableSet<TranslationItem>
    ) {
        if (args.size < 2) return
        val groupIdExpr = args[0].getArgumentExpression() ?: return
        val groupId = resolveStringLiteral(groupIdExpr) ?: return
        val nameExpr = args[1].getArgumentExpression() ?: return
        val englishName = resolveStringLiteral(nameExpr) ?: return
        result.add(TranslationItem("itemGroup.$groupId", englishName))
    }

    private fun extractTrCall(
        args: List<KtValueArgument>,
        result: MutableSet<TranslationItem>
    ) {
        val argExpr = args.firstOrNull()?.getArgumentExpression() ?: return
        val text = resolveStringLiteral(argExpr) ?: return
        result.add(TranslationItem(text))
    }

    private fun extractForgeCall(
        args: List<KtValueArgument>,
        property: String,
        result: MutableSet<TranslationItem>
    ) {
        if (args.size < 2) return

        // First arg: Type enum
        val typeArg = args[0].getArgumentExpression()?.text ?: return
        val cleaned = typeArg.removePrefix("I18N.Type.").removePrefix("Type.")
        val type = try { I18N.Type.valueOf(cleaned) } catch (_: Exception) { return }

        // Second arg: string literal (possibly concatenated with +)
        val textExpr = args[1].getArgumentExpression() ?: return
        val text = resolveStringLiteral(textExpr) ?: return
        result.add(
            TranslationItem(
                type.prefix + I18N.encodeLangKey(text, type.isWhitespacesInFileReplaced()) + "." + property,
                text
            )
        )
    }

    private fun extractExpandCall(
        args: List<KtValueArgument>,
        result: MutableSet<TranslationItem>
    ) {
        if (args.size < 3) return

        val typeArg = args[0].getArgumentExpression()?.text ?: return
        val cleaned = typeArg.removePrefix("I18N.Type.").removePrefix("Type.")
        val type = try { I18N.Type.valueOf(cleaned) } catch (_: Exception) { return }

        val formatExpr = args[1].getArgumentExpression() ?: return
        val format = resolveStringLiteral(formatExpr) ?: return

        val axisArrays = mutableListOf<List<String>>()
        for (i in 2 until args.size) {
            val axisExpr = args[i].getArgumentExpression() ?: return
            val values = resolveStringArrayLiteral(axisExpr) ?: return
            axisArrays.add(values)
        }

        for (combo in cartesianProduct(axisArrays)) {
            val name = substituteFormat(format, combo)
            result.add(
                TranslationItem(
                    type.prefix + I18N.encodeLangKey(name, type.isWhitespacesInFileReplaced()) + ".name",
                    name
                )
            )
        }
    }

    private fun resolveStringArrayLiteral(expr: KtExpression): List<String>? {
        if (expr !is KtCallExpression) return null
        if (expr.calleeExpression?.text != "arrayOf") return null
        val values = expr.valueArguments.mapNotNull { arg ->
            arg.getArgumentExpression()?.let { resolveStringLiteral(it) }
        }
        return values.takeIf { it.size == expr.valueArguments.size }
    }

    private fun resolveStringLiteral(expr: KtExpression): String? {
        return when (expr) {
            is KtStringTemplateExpression -> {
                if (expr.hasInterpolation()) null
                else extractStringText(expr)
            }
            is KtBinaryExpression -> {
                if (expr.operationReference.text != "+") null
                else {
                    val left = expr.left?.let { resolveStringLiteral(it) } ?: return null
                    val right = expr.right?.let { resolveStringLiteral(it) } ?: return null
                    left + right
                }
            }
            else -> null
        }
    }

    private fun extractStringText(expr: KtStringTemplateExpression): String? {
        return expr.entries.mapNotNull { entry ->
            when (entry) {
                is KtLiteralStringTemplateEntry -> entry.text
                is KtEscapeStringTemplateEntry -> {
                    val text = entry.text
                    if (text == "\\$") "$" else text
                }
                else -> null
            }
        }.joinToString("")
    }
}
