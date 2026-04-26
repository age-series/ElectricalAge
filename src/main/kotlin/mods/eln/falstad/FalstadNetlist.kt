package mods.eln.falstad

import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import mods.eln.i18n.I18N.tr
import kotlin.math.max
import kotlin.math.min

data class FalstadPoint(val x: Int, val y: Int)

data class FalstadComponent(
    val code: String,
    val start: FalstadPoint,
    val end: FalstadPoint,
    val params: List<String>,
    val lineNumber: Int,
    val raw: String
)

data class FalstadParseResult(
    val components: List<FalstadComponent>,
    val warnings: List<String>
)

enum class FalstadNodeKind {
    NORMAL,
    GROUND
}

enum class FalstadPlacedKind {
    RESISTOR,
    CAPACITOR,
    INDUCTOR,
    DIODE,
    VOLTAGE_SOURCE,
    SINGLE_PORT_VOLTAGE_SOURCE,
    CURRENT_SOURCE,
    SWITCH,
    PROBE_DISPLAY,
    FALSTAD_AND_GATE,
    FALSTAD_NAND_GATE,
    FALSTAD_OR_GATE,
    FALSTAD_NOR_GATE,
    FALSTAD_XOR_GATE,
    FALSTAD_NOT_GATE,
    SIGNAL_INPUT,
    SIGNAL_OUTPUT
}

enum class FalstadAxis {
    HORIZONTAL,
    VERTICAL
}

data class FalstadPlacedComponent(
    val kind: FalstadPlacedKind,
    val cell: FalstadPoint,
    val start: FalstadPoint,
    val end: FalstadPoint,
    val axis: FalstadAxis,
    val source: FalstadComponent,
    val substitutions: List<String> = emptyList(),
    val forcedSwitchState: Boolean? = null,
    val extraPoints: List<FalstadPoint> = emptyList()
)

data class FalstadLayoutPlan(
    val width: Int,
    val height: Int,
    val nodes: Map<FalstadPoint, FalstadNodeKind>,
    val wires: Set<FalstadPoint>,
    val components: List<FalstadPlacedComponent>,
    val warnings: List<String>
)

fun FalstadLayoutPlan.rotatedClockwise(): FalstadLayoutPlan {
    fun rotate(point: FalstadPoint): FalstadPoint = FalstadPoint(height - 1 - point.y, point.x)

    return FalstadLayoutPlan(
        width = height,
        height = width,
        nodes = nodes.entries.associate { rotate(it.key) to it.value },
        wires = wires.mapTo(linkedSetOf()) { rotate(it) },
        components = components.map { component ->
            val rotatedStart = rotate(component.start)
            val rotatedEnd = rotate(component.end)
            FalstadPlacedComponent(
                kind = component.kind,
                cell = rotate(component.cell),
                start = rotatedStart,
                end = rotatedEnd,
                axis = if (component.axis == FalstadAxis.HORIZONTAL) FalstadAxis.VERTICAL else FalstadAxis.HORIZONTAL,
                source = component.source,
                substitutions = component.substitutions,
                forcedSwitchState = component.forcedSwitchState,
                extraPoints = component.extraPoints.map { rotate(it) }
            )
        },
        warnings = warnings
    )
}

object FalstadNetlistParser {
    fun parse(text: String): FalstadParseResult {
        val components = mutableListOf<FalstadComponent>()
        val warnings = mutableListOf<String>()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("$")) return@forEachIndexed

            val parts = line.split(Regex("\\s+"))
            if (parts.isEmpty()) return@forEachIndexed

            val code = parts[0]
            if (code == "g") {
                val x = parts.getOrNull(1)?.toIntOrNull()
                val y = parts.getOrNull(2)?.toIntOrNull()
                if (x == null || y == null) {
                    warnings += tr("Line %1$: malformed ground coordinates", lineNumber)
                    return@forEachIndexed
                }
                components += FalstadComponent(code, FalstadPoint(x, y), FalstadPoint(x, y), parts.drop(3), lineNumber, line)
                return@forEachIndexed
            }

            if (parts.size < 5) {
                warnings += tr("Line %1$: too few fields for component '%2$'", lineNumber, code)
                return@forEachIndexed
            }

            val x1 = parts[1].toIntOrNull()
            val y1 = parts[2].toIntOrNull()
            val x2 = parts[3].toIntOrNull()
            val y2 = parts[4].toIntOrNull()
            if (x1 == null || y1 == null || x2 == null || y2 == null) {
                warnings += tr("Line %1$: malformed coordinates", lineNumber)
                return@forEachIndexed
            }

            val rawParams = parts.drop(5)
            val normalized = normalizeFalstadComponent(code, rawParams)
            normalized.warning?.let { warnings += tr("Line %1$: %2$", lineNumber, it) }

            components += FalstadComponent(
                code = normalized.code,
                start = FalstadPoint(x1, y1),
                end = FalstadPoint(x2, y2),
                params = normalized.params,
                lineNumber = lineNumber,
                raw = line
            )
        }

        return FalstadParseResult(components, warnings)
    }

    private data class NormalizedFalstadComponent(
        val code: String,
        val params: List<String>,
        val warning: String? = null
    )

    private fun normalizeFalstadComponent(code: String, params: List<String>): NormalizedFalstadComponent {
        if (code != "172") {
            return NormalizedFalstadComponent(code, params)
        }

        val initialValue = params.getOrNull(2)?.toDoubleOrNull()
            ?: params.getOrNull(3)?.toDoubleOrNull()
            ?: 0.0

        return NormalizedFalstadComponent(
            code = "vs",
            params = listOf("0", "0", "0", initialValue.toString(), "0"),
            warning = tr("adjustable voltage source imported as a fixed DC source")
        )
    }
}

object FalstadDeviceParser {
    fun parse(text: String): FalstadParseResult {
        val trimmed = text.trimStart()
        return if (trimmed.startsWith("<cir")) {
            parseXmlCircuit(trimmed)
        } else {
            FalstadNetlistParser.parse(text)
        }
    }

    private fun parseXmlCircuit(text: String): FalstadParseResult {
        val root = FalstadXmlParser.parse(text)
        require(root.name == "cir") { "Expected <cir> root element" }

        val warnings = mutableListOf<String>()
        val components = mutableListOf<FalstadComponent>()

        for ((index, child) in root.children.withIndex()) {
            val code = child.name.lowercase()
            if (code in setOf("o", "adj", "h")) continue

            val points = child.pointListAttribute("x")
            if (code != "g" && (points == null || points.size != 4)) {
                warnings += tr("Element %1$: missing or invalid x attribute for <%2$>", index + 1, code)
                continue
            }

            val start = if (code == "g") {
                val x = child.intAttribute("x") ?: child.pointListAttribute("x")?.getOrNull(0)
                val y = child.intAttribute("y") ?: child.pointListAttribute("x")?.getOrNull(1)
                if (x == null || y == null) {
                    warnings += tr("Element %1$: malformed ground element", index + 1)
                    continue
                }
                FalstadPoint(x, y)
            } else {
                FalstadPoint(points!![0], points[1])
            }
            val end = if (code == "g") start else FalstadPoint(points!![2], points[3])

            val params = when (code) {
                "r" -> listOf(child.attribute("f").orEmpty(), child.attribute("r").orEmpty())
                "c" -> listOf(
                    child.attribute("f").orEmpty(),
                    child.attribute("c").orEmpty(),
                    child.attribute("iv").orEmpty()
                )
                "l" -> listOf(
                    child.attribute("f").orEmpty(),
                    child.attribute("l").orEmpty(),
                    child.attribute("ic").orEmpty()
                )
                "v" -> listOf(
                    child.attribute("f").orEmpty(),
                    child.attribute("maxv").orEmpty(),
                    child.attribute("freq").orEmpty(),
                    child.attribute("phase").orEmpty(),
                    child.attribute("wf").orEmpty()
                )
                "i" -> listOf(child.attribute("f").orEmpty(), child.attribute("i").orEmpty())
                "s" -> listOf(child.attribute("f").orEmpty(), child.attribute("p").orEmpty())
                "p" -> listOf(child.attribute("f").orEmpty())
                "w" -> listOf(child.attribute("f").orEmpty())
                "d" -> listOf(child.attribute("f").orEmpty())
                "z" -> listOf(child.attribute("f").orEmpty(), child.attribute("vz").orEmpty())
                "g" -> listOf(child.attribute("f").orEmpty())
                else -> {
                    warnings += tr("Element %1$: unsupported <%2$> element", index + 1, code)
                    continue
                }
            }

            components += FalstadComponent(
                code = code,
                start = start,
                end = end,
                params = params,
                lineNumber = index + 1,
                raw = buildString {
                    append('<')
                    append(child.name)
                    append('>')
                }
            )
        }

        return FalstadParseResult(components, warnings)
    }
}

object FalstadLayoutPlanner {
    fun plan(parseResult: FalstadParseResult): FalstadLayoutPlan {
        val warnings = parseResult.warnings.toMutableList()
        val components = parseResult.components
        if (components.isEmpty()) {
            return FalstadLayoutPlan(0, 0, emptyMap(), emptySet(), emptyList(), warnings)
        }

        val xValues = components.flatMap { listOf(it.start.x, it.end.x) }.distinct().sorted()
        val yValues = components.flatMap { listOf(it.start.y, it.end.y) }.distinct().sorted()
        val xMap = xValues.withIndex().associate { it.value to it.index * 2 }
        val yMap = yValues.withIndex().associate { it.value to it.index * 2 }
        val mappedXPositions = xMap.values.toSet()
        val mappedYPositions = yMap.values.toSet()

        val nodes = linkedMapOf<FalstadPoint, FalstadNodeKind>()
        val wires = linkedSetOf<FalstadPoint>()
        val placed = mutableListOf<FalstadPlacedComponent>()

        fun mapped(point: FalstadPoint) = FalstadPoint(xMap.getValue(point.x), yMap.getValue(point.y))

        fun markNode(point: FalstadPoint, kind: FalstadNodeKind = FalstadNodeKind.NORMAL) {
            val existing = nodes[point]
            nodes[point] = if (existing == FalstadNodeKind.GROUND || kind == FalstadNodeKind.GROUND) {
                FalstadNodeKind.GROUND
            } else {
                FalstadNodeKind.NORMAL
            }
        }

        fun fillWirePath(start: FalstadPoint, end: FalstadPoint) {
            if (start.x == end.x) {
                for (y in min(start.y, end.y)..max(start.y, end.y)) {
                    val point = FalstadPoint(start.x, y)
                    if (y in mappedYPositions) markNode(point) else wires += point
                }
            } else {
                for (x in min(start.x, end.x)..max(start.x, end.x)) {
                    val point = FalstadPoint(x, start.y)
                    if (x in mappedXPositions) markNode(point) else wires += point
                }
            }
        }

        fun fillComponentLeadPath(start: FalstadPoint, end: FalstadPoint, cell: FalstadPoint) {
            if (start.x == end.x) {
                for (y in min(start.y, end.y) + 1 until max(start.y, end.y)) {
                    val point = FalstadPoint(start.x, y)
                    if (point != cell) wires += point
                }
            } else {
                for (x in min(start.x, end.x) + 1 until max(start.x, end.x)) {
                    val point = FalstadPoint(x, start.y)
                    if (point != cell) wires += point
                }
            }
        }

        fun fillOrthogonalPath(start: FalstadPoint, bend: FalstadPoint, end: FalstadPoint) {
            if (start != bend) fillWirePath(start, bend)
            if (bend != end) fillWirePath(bend, end)
        }

        fun parseFalstadSwitchState(component: FalstadComponent): Boolean {
            val booleanParam = component.params.firstOrNull { it.equals("true", ignoreCase = true) || it.equals("false", ignoreCase = true) }
            return when {
                booleanParam.equals("true", ignoreCase = true) -> true
                booleanParam.equals("false", ignoreCase = true) -> false
                else -> {
                    val numeric = component.params.lastOrNull { it.toIntOrNull() != null }?.toIntOrNull() ?: 0
                    numeric != 0
                }
            }
        }

        fun placeSpdtSwitch(component: FalstadComponent, start: FalstadPoint, end: FalstadPoint, axis: FalstadAxis) {
            val substitutions = listOf(tr("SPDT switch substituted with two complementary maintained switches"))
            val selectedAlternate = parseFalstadSwitchState(component)

            if (axis == FalstadAxis.VERTICAL) {
                val endIndex = xValues.indexOf(component.end.x)
                if (endIndex <= 0 || endIndex >= xValues.lastIndex) {
                    warnings += tr("Line %1$: SPDT switch requires connections on both throws", component.lineNumber)
                    return
                }
                val throwXs = listOf(xMap.getValue(xValues[endIndex - 1]), xMap.getValue(xValues[endIndex + 1]))
                val throwClosed = if (selectedAlternate) listOf(true, false) else listOf(false, true)

                for ((index, throwX) in throwXs.withIndex()) {
                    val branchStart = FalstadPoint(throwX, start.y)
                    val throwPoint = FalstadPoint(throwX, end.y)
                    val cell = FalstadPoint(throwX, (start.y + end.y) / 2)
                    markNode(branchStart)
                    markNode(throwPoint)
                    fillWirePath(start, branchStart)
                    fillComponentLeadPath(branchStart, throwPoint, cell)
                    placed += FalstadPlacedComponent(
                        kind = FalstadPlacedKind.SWITCH,
                        cell = cell,
                        start = branchStart,
                        end = throwPoint,
                        axis = FalstadAxis.VERTICAL,
                        source = component,
                        substitutions = if (index == 0) substitutions else emptyList(),
                        forcedSwitchState = throwClosed[index]
                    )
                }
                markNode(start)
                return
            }

            val endIndex = yValues.indexOf(component.end.y)
            if (endIndex <= 0 || endIndex >= yValues.lastIndex) {
                warnings += tr("Line %1$: SPDT switch requires connections on both throws", component.lineNumber)
                return
            }
            val throwYs = listOf(yMap.getValue(yValues[endIndex - 1]), yMap.getValue(yValues[endIndex + 1]))
            val throwClosed = if (selectedAlternate) listOf(true, false) else listOf(false, true)

            for ((index, throwY) in throwYs.withIndex()) {
                val branchStart = FalstadPoint(start.x, throwY)
                val throwPoint = FalstadPoint(end.x, throwY)
                val cell = FalstadPoint((start.x + end.x) / 2, throwY)
                markNode(branchStart)
                markNode(throwPoint)
                fillWirePath(start, branchStart)
                fillComponentLeadPath(branchStart, throwPoint, cell)
                placed += FalstadPlacedComponent(
                    kind = FalstadPlacedKind.SWITCH,
                    cell = cell,
                    start = branchStart,
                    end = throwPoint,
                    axis = FalstadAxis.HORIZONTAL,
                    source = component,
                    substitutions = if (index == 0) substitutions else emptyList(),
                    forcedSwitchState = throwClosed[index]
                )
            }
            markNode(start)
        }

        fun parseFalstadLogicState(component: FalstadComponent): Boolean {
            val booleanParam = component.params.firstOrNull { it.equals("true", ignoreCase = true) || it.equals("false", ignoreCase = true) }
            return booleanParam.equals("true", ignoreCase = true)
        }

        fun nearestConnectionOnAxis(excluding: FalstadComponent, x: Int, y: Int, above: Boolean): FalstadPoint? {
            val candidates = components
                .asSequence()
                .filter { it !== excluding }
                .flatMap { sequenceOf(mapped(it.start), mapped(it.end)) }
                .filter { it.x == x && if (above) it.y < y else it.y > y }
                .distinct()
            return if (above) candidates.maxByOrNull { it.y } else candidates.minByOrNull { it.y }
        }

        fun placeFalstadTwoInputGate(component: FalstadComponent, start: FalstadPoint, end: FalstadPoint, kind: FalstadPlacedKind, chipName: String) {
            if (start.y != end.y) {
                warnings += tr("Line %1$: gate '%2$' is only supported horizontally", component.lineNumber, component.code)
                return
            }

            val upperInput = nearestConnectionOnAxis(component, start.x, start.y, above = true)
            val lowerInput = nearestConnectionOnAxis(component, start.x, start.y, above = false)
            if (upperInput == null || lowerInput == null) {
                warnings += tr("Line %1$: gate '%2$' is missing stretched input connections", component.lineNumber, component.code)
                return
            }

            val cell = start
            val upperPin = FalstadPoint(cell.x, cell.y - 1)
            val lowerPin = FalstadPoint(cell.x, cell.y + 1)
            val outputPin = FalstadPoint(cell.x + 1, cell.y)

            markNode(upperInput)
            markNode(lowerInput)
            markNode(end)
            fillWirePath(upperInput, upperPin)
            fillWirePath(lowerInput, lowerPin)
            fillWirePath(outputPin, end)

            placed += FalstadPlacedComponent(
                kind = kind,
                cell = cell,
                start = upperInput,
                end = lowerInput,
                axis = FalstadAxis.HORIZONTAL,
                source = component,
                substitutions = listOf(tr("Falstad gate %1$ substituted with %2$", component.code, chipName)),
                extraPoints = listOf(outputPin, upperPin, lowerPin)
            )
        }

        fun placeFalstadInverter(component: FalstadComponent, start: FalstadPoint, end: FalstadPoint) {
            val horizontal = start.y == end.y
            val cell = FalstadPoint((start.x + end.x) / 2, (start.y + end.y) / 2)
            val inputPin = if (horizontal) {
                FalstadPoint(cell.x + if (start.x < end.x) -1 else 1, cell.y)
            } else {
                FalstadPoint(cell.x, cell.y + if (start.y < end.y) -1 else 1)
            }
            val outputPin = if (horizontal) {
                FalstadPoint(cell.x + if (start.x < end.x) 1 else -1, cell.y)
            } else {
                FalstadPoint(cell.x, cell.y + if (start.y < end.y) 1 else -1)
            }

            markNode(start)
            markNode(end)
            fillWirePath(start, inputPin)
            fillWirePath(outputPin, end)

            placed += FalstadPlacedComponent(
                kind = FalstadPlacedKind.FALSTAD_NOT_GATE,
                cell = cell,
                start = start,
                end = end,
                axis = if (horizontal) FalstadAxis.HORIZONTAL else FalstadAxis.VERTICAL,
                source = component,
                substitutions = listOf(tr("Falstad gate %1$ substituted with NOT Chip", component.code)),
                extraPoints = listOf(outputPin, inputPin)
            )
        }

        fun placeFalstadSignalInput(component: FalstadComponent, start: FalstadPoint, end: FalstadPoint, axis: FalstadAxis) {
            val cell = end
            val pin = if (axis == FalstadAxis.HORIZONTAL) {
                FalstadPoint(end.x + if (start.x > end.x) 1 else -1, end.y)
            } else {
                FalstadPoint(end.x, end.y + if (start.y > end.y) 1 else -1)
            }
            markNode(start)
            fillWirePath(start, pin)
            placed += FalstadPlacedComponent(
                kind = FalstadPlacedKind.SIGNAL_INPUT,
                cell = cell,
                start = start,
                end = end,
                axis = axis,
                source = component,
                substitutions = listOf(tr("Falstad logic input substituted with Signal Switch")),
                forcedSwitchState = parseFalstadLogicState(component)
            )
        }

        fun placeFalstadSignalOutput(component: FalstadComponent, start: FalstadPoint, end: FalstadPoint, axis: FalstadAxis) {
            val cell = end
            val pin = if (axis == FalstadAxis.HORIZONTAL) {
                FalstadPoint(end.x + if (start.x > end.x) 1 else -1, end.y)
            } else {
                FalstadPoint(end.x, end.y + if (start.y > end.y) 1 else -1)
            }
            markNode(start)
            fillWirePath(start, pin)
            placed += FalstadPlacedComponent(
                kind = FalstadPlacedKind.SIGNAL_OUTPUT,
                cell = cell,
                start = start,
                end = end,
                axis = axis,
                source = component,
                substitutions = listOf(tr("Falstad logic output substituted with LED vuMeter"))
            )
        }

        for (component in components) {
            val rawCode = component.code
            val code = rawCode.lowercase()
            val start = mapped(component.start)
            val end = mapped(component.end)

            if (code == "x") {
                continue
            }

            if (code == "g") {
                markNode(start, FalstadNodeKind.GROUND)
                continue
            }

            val horizontal = start.y == end.y
            val vertical = start.x == end.x
            if (!horizontal && !vertical) {
                warnings += tr("Line %1$: diagonal component '%2$' is not supported", component.lineNumber, component.code)
                continue
            }

            if (rawCode == "S") {
                placeSpdtSwitch(component, start, end, if (horizontal) FalstadAxis.HORIZONTAL else FalstadAxis.VERTICAL)
                continue
            }
            if (rawCode == "150") {
                placeFalstadTwoInputGate(component, start, end, FalstadPlacedKind.FALSTAD_XOR_GATE, "XOR Chip")
                continue
            }
            if (rawCode == "151") {
                placeFalstadTwoInputGate(component, start, end, FalstadPlacedKind.FALSTAD_NAND_GATE, "NAND Chip")
                continue
            }
            if (rawCode == "152") {
                placeFalstadTwoInputGate(component, start, end, FalstadPlacedKind.FALSTAD_OR_GATE, "OR Chip")
                continue
            }
            if (rawCode == "153") {
                placeFalstadTwoInputGate(component, start, end, FalstadPlacedKind.FALSTAD_NOR_GATE, "NOR Chip")
                continue
            }
            if (rawCode == "154") {
                placeFalstadTwoInputGate(component, start, end, FalstadPlacedKind.FALSTAD_AND_GATE, "AND Chip")
                continue
            }
            if (rawCode == "I") {
                placeFalstadInverter(component, start, end)
                continue
            }
            if (rawCode == "L") {
                placeFalstadSignalInput(component, start, end, if (horizontal) FalstadAxis.HORIZONTAL else FalstadAxis.VERTICAL)
                continue
            }
            if (rawCode == "M") {
                placeFalstadSignalOutput(component, start, end, if (horizontal) FalstadAxis.HORIZONTAL else FalstadAxis.VERTICAL)
                continue
            }

            if (code == "w") {
                fillWirePath(start, end)
                continue
            }

            val substitutions = mutableListOf<String>()
            val kind = when (code) {
                "r" -> FalstadPlacedKind.RESISTOR
                "c" -> FalstadPlacedKind.CAPACITOR
                "l" -> FalstadPlacedKind.INDUCTOR
                "d" -> FalstadPlacedKind.DIODE
                "z" -> {
                    substitutions += tr("Zener diode substituted with a standard diode")
                    FalstadPlacedKind.DIODE
                }
                "v" -> FalstadPlacedKind.VOLTAGE_SOURCE
                "vs" -> FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE
                "i" -> FalstadPlacedKind.CURRENT_SOURCE
                "s" -> FalstadPlacedKind.SWITCH
                "o" -> FalstadPlacedKind.PROBE_DISPLAY
                "p" -> {
                    substitutions += tr("Push switch substituted with a maintained switch")
                    FalstadPlacedKind.SWITCH
                }
                else -> {
                    warnings += tr("Line %1$: unsupported component '%2$'", component.lineNumber, component.code)
                    continue
                }
            }

            markNode(start)
            if (kind != FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE && kind != FalstadPlacedKind.PROBE_DISPLAY) {
                markNode(end)
            }

            val cell = FalstadPoint((start.x + end.x) / 2, (start.y + end.y) / 2)
            placed += FalstadPlacedComponent(
                kind = kind,
                cell = cell,
                start = start,
                end = end,
                axis = if (horizontal) FalstadAxis.HORIZONTAL else FalstadAxis.VERTICAL,
                source = component,
                substitutions = substitutions
            )
            if (kind != FalstadPlacedKind.VOLTAGE_SOURCE &&
                kind != FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE &&
                kind != FalstadPlacedKind.CURRENT_SOURCE &&
                kind != FalstadPlacedKind.PROBE_DISPLAY) {
                fillComponentLeadPath(start, end, cell)
            }
        }

        val extraProbeX = placed
            .filter { it.kind == FalstadPlacedKind.PROBE_DISPLAY && it.axis == FalstadAxis.HORIZONTAL }
            .maxOfOrNull { max(it.start.x, it.end.x) + 1 } ?: (xMap.values.maxOrNull() ?: 0)
        val extraProbeY = placed
            .filter { it.kind == FalstadPlacedKind.PROBE_DISPLAY && it.axis == FalstadAxis.VERTICAL }
            .maxOfOrNull { max(it.start.y, it.end.y) + 1 } ?: (yMap.values.maxOrNull() ?: 0)

        return FalstadLayoutPlan(
            width = max((xMap.values.maxOrNull() ?: 0), extraProbeX) + 1,
            height = max((yMap.values.maxOrNull() ?: 0), extraProbeY) + 1,
            nodes = nodes,
            wires = wires,
            components = placed,
            warnings = warnings
        )
    }
}

data class FalstadXmlNode(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<FalstadXmlNode>
) {
    fun attribute(name: String): String? = attributes[name]

    fun intAttribute(name: String): Int? = attribute(name)?.toIntOrNull()

    fun doubleAttribute(name: String): Double? = attribute(name)?.toDoubleOrNull()

    fun pointListAttribute(name: String): List<Int>? {
        val raw = attribute(name) ?: return null
        val values = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { it.toIntOrNull() }
        return if (values.any { it == null }) null else values.filterNotNull()
    }
}

object FalstadXmlParser {
    fun parse(text: String): FalstadXmlNode {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringComments = true
        factory.isCoalescing = true
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(InputSource(StringReader(text)))
        val root = document.documentElement ?: error("Missing root element")
        return root.toNode()
    }

    private fun Element.toNode(): FalstadXmlNode {
        val attributes = buildMap {
            val attrs = this@toNode.attributes
            for (idx in 0 until attrs.length) {
                val attribute = attrs.item(idx)
                put(attribute.nodeName, attribute.nodeValue)
            }
        }

        val children = buildList {
            val nodeList = childNodes
            for (idx in 0 until nodeList.length) {
                val child = nodeList.item(idx)
                if (child is Element) {
                    add(child.toNode())
                }
            }
        }

        return FalstadXmlNode(
            name = tagName,
            attributes = attributes,
            children = children
        )
    }
}
