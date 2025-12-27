package mods.eln.sim

import mods.eln.Eln
import mods.eln.misc.Utils
import mods.eln.node.Node
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeElement
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.SubSystemDebugSnapshot
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.state.State
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logs the conductance matrices that the MNA solver builds for a given node or element.
 */
object MnaMatrixDebugger {
    private const val VALUE_FORMAT = "% .4e"
    private const val LOG_DIR = "simlog"
    private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT)

    fun dump(target: Any?, reason: String? = null) {
        if (!Eln.debugEnabled || !Eln.simSnapshotEnabled) {
            return
        }

        val systems = linkedSetOf<SubSystem>()
        collectSubsystems(target, systems)
        if (systems.isEmpty()) {
            Eln.LOGGER.warn(
                "MNA dump requested for {} but no subsystems were found (reason: {})",
                describe(target),
                reason ?: "n/a"
            )
            return
        }

        val logFiles = createLogFiles(target)
        val textFile = logFiles.first
        val dotFile = logFiles.second
        val builder = StringBuilder()
        val snapshotEntries = mutableListOf<SnapshotEntry>()

        systems.forEachIndexed { index, system ->
            val snapshot = runCatching { system.captureDebugSnapshot() }
                .onFailure {
                    Eln.LOGGER.error(
                        "Failed to capture MNA snapshot for {} ({})",
                        describe(target),
                        it.message,
                        it
                    )
                }
                .getOrNull() ?: return@forEachIndexed
            appendSnapshot(builder, target, snapshot, reason, index + 1, systems.size)
            snapshotEntries += SnapshotEntry(snapshot, index + 1)
        }

        if (builder.isEmpty()) {
            Eln.LOGGER.warn("MNA matrix dump for {} skipped; no data captured", describe(target))
            return
        }

        val textWritten = writeDump(textFile, builder.toString())

        if (!textWritten) {
            logFallback(builder)
        } else {
            Eln.LOGGER.warn(
                "MNA matrix dump for {} (reason: {}) saved to {}",
                describe(target),
                reason ?: "n/a",
                textFile!!.absolutePath
            )
        }

        val dotContent = buildDot(snapshotEntries, target, reason)
        if (dotContent != null) {
            if (writeDump(dotFile, dotContent)) {
                Eln.LOGGER.warn(
                    "MNA connectivity graph for {} saved to {}",
                    describe(target),
                    dotFile.absolutePath
                )
            } else {
                Eln.LOGGER.error("Failed to write MNA graph for {}", describe(target))
            }
        }
    }

    private fun appendSnapshot(
        builder: StringBuilder,
        target: Any?,
        snapshot: SubSystemDebugSnapshot,
        reason: String?,
        index: Int,
        total: Int
    ) {
        val header = buildHeader(target, snapshot, reason, index, total)
        builder.appendLine(header)
        if (snapshot.stateLabels.isNotEmpty()) {
            builder.append("  States: ").append(snapshot.stateLabels.joinToString(", ")).appendLine()
        } else {
            builder.appendLine("  States: <none>")
        }
        if (snapshot.componentLabels.isNotEmpty()) {
            builder.append("  Components: ").append(snapshot.componentLabels.joinToString(", ")).appendLine()
        }
        val matrix = snapshot.conductanceMatrix
        matrix.forEachIndexed { rowIndex, row ->
            val rowValues = row.joinToString(", ") { formatValue(it) }
            builder.append("    A[")
                .append(String.format(Locale.ROOT, "%02d", rowIndex))
                .append("]: ")
                .append(rowValues)
                .appendLine()
        }
        if (matrix.isEmpty()) {
            builder.appendLine("    <empty matrix>")
        }
        val rhs = snapshot.rhsVector
        if (rhs.isNotEmpty()) {
            val rhsText = rhs.joinToString(", ") { formatValue(it) }
            builder.append("    RHS: ").append(rhsText).appendLine()
        }
        builder.appendLine()
    }

    private fun buildHeader(
        target: Any?,
        snapshot: SubSystemDebugSnapshot,
        reason: String?,
        index: Int,
        total: Int
    ): String {
        return buildString {
            append("MNA matrix ")
            append(index).append('/').append(total)
            append(" for ").append(describe(target))
            if (!reason.isNullOrBlank()) {
                append(" (reason: ").append(reason).append(')')
            }
            if (snapshot.isSingular) {
                append(" [singular]")
            }
        }
    }

    private fun logFallback(builder: StringBuilder) {
        Eln.LOGGER.warn("MNA matrix dump fallback; writing to standard log")
        builder.lineSequence().forEach { line ->
            if (line.isNotEmpty()) {
                Eln.LOGGER.warn(line)
            }
        }
    }

    private fun createLogFiles(target: Any?): Pair<File, File> {
        val timestamp = TIMESTAMP_FORMAT.format(Date())
        val typeName = (target?.javaClass?.simpleName ?: "Unknown")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val basePath = "$LOG_DIR/$timestamp-$typeName"
        val textFile = resolveLogFile("$basePath.txt")
        val dotFile = resolveLogFile("$basePath.dot")
        return Pair(textFile, dotFile)
    }

    private fun resolveLogFile(path: String): File {
        return runCatching { Utils.getMapFile(path) }
            .getOrElse { File(path) }
    }

    private fun writeDump(file: File, contents: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(contents)
            true
        } catch (e: IOException) {
            Eln.LOGGER.error("Failed to write MNA dump to {}: {}", file.absolutePath, e.message)
            false
        }
    }

    private fun buildDot(
        snapshots: List<SnapshotEntry>,
        target: Any?,
        reason: String?
    ): String? {
        if (snapshots.isEmpty()) return null
        return buildString {
            append("graph \"").append(escapeDotLabel(describe(target))).appendLine("\" {")
            appendLine("  rankdir=LR;")
            val graphLabel = buildString {
                append("MNA snapshot for ").append(describe(target))
                if (!reason.isNullOrBlank()) {
                    append(" (reason: ").append(reason).append(')')
                }
            }
            append("  label=\"").append(escapeDotLabel(graphLabel)).appendLine("\";")
            snapshots.forEach { entry ->
                appendLine("  subgraph cluster_${entry.index} {")
                append("    label=\"Subsystem ").append(entry.index).append("\";").appendLine()
                val stateIds = mutableListOf<String>()
                entry.snapshot.stateLabels.forEachIndexed { idx, label ->
                    val nodeId = "s_${entry.index}_$idx"
                    stateIds += nodeId
                    append("    ")
                        .append(nodeId)
                        .append(" [shape=ellipse,label=\"")
                        .append(escapeDotLabel(label))
                        .append("\"];")
                        .appendLine()
                }
                entry.snapshot.componentLabels.forEachIndexed { idx, label ->
                    val nodeId = "c_${entry.index}_$idx"
                    append("    ")
                        .append(nodeId)
                        .append(" [shape=box,label=\"")
                        .append(escapeDotLabel(label))
                        .append("\"];")
                        .appendLine()
                    val connections = entry.snapshot.componentConnections.getOrNull(idx) ?: IntArray(0)
                    connections.forEachIndexed { connIndex, stateIndex ->
                        if (stateIndex >= 0 && stateIndex < stateIds.size) {
                            val stateNode = "s_${entry.index}_$stateIndex"
                            append("    ")
                                .append(nodeId)
                                .append(" -- ")
                                .append(stateNode)
                                .append(" [label=\"")
                                .append(connIndex)
                                .append("\"];")
                                .appendLine()
                        }
                    }
                }
                appendLine("  }")
            }
            appendLine("}")
        }
    }

    private fun escapeDotLabel(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    private data class SnapshotEntry(
        val snapshot: SubSystemDebugSnapshot,
        val index: Int
    )

    private fun collectSubsystems(source: Any?, out: MutableSet<SubSystem>) {
        when (source) {
            null -> Unit
            is SubSystem -> out.add(source)
            is State -> source.subSystem?.let(out::add)
            is Component -> source.subSystem?.let(out::add)
            is SixNodeElement -> {
                source.electricalLoadList.forEach { collectSubsystems(it, out) }
                source.electricalComponentList.forEach { collectSubsystems(it, out) }
            }
            is TransparentNodeElement -> {
                source.electricalLoadList.forEach { collectSubsystems(it, out) }
                source.electricalComponentList.forEach { collectSubsystems(it, out) }
            }
            is SixNode -> source.sideElementList.forEach { collectSubsystems(it, out) }
            is TransparentNode -> source.element?.let { collectSubsystems(it, out) }
            is Node -> {
                // Other Node subclasses should be handled by the more specific cases above.
            }
            is Iterable<*> -> source.forEach { collectSubsystems(it, out) }
            is Array<*> -> source.forEach { collectSubsystems(it, out) }
        }
    }

    private fun describe(target: Any?): String = when (target) {
        null -> "null"
        is SixNodeElement -> "${target.javaClass.simpleName}@${identity(target)} ${target.sixNode?.coordinate}"
        is TransparentNodeElement -> "${target.javaClass.simpleName}@${identity(target)} ${target.node?.coordinate}"
        is SixNode -> "${target.javaClass.simpleName}@${identity(target)} ${target.coordinate}"
        is TransparentNode -> "${target.javaClass.simpleName}@${identity(target)} ${target.coordinate}"
        is Node -> "${target.javaClass.simpleName}@${identity(target)} ${target.coordinate}"
        else -> "${target.javaClass.simpleName}@${identity(target)}"
    }

    private fun identity(target: Any) = Integer.toHexString(System.identityHashCode(target))

    private fun formatValue(value: Double) =
        String.format(Locale.ROOT, VALUE_FORMAT, value)
}
