package mods.eln.falstad

import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
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
    PROBE_DISPLAY
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
    val substitutions: List<String> = emptyList()
)

data class FalstadLayoutPlan(
    val width: Int,
    val height: Int,
    val nodes: Map<FalstadPoint, FalstadNodeKind>,
    val wires: Set<FalstadPoint>,
    val components: List<FalstadPlacedComponent>,
    val warnings: List<String>
)

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
                    warnings += "Line $lineNumber: malformed ground coordinates"
                    return@forEachIndexed
                }
                components += FalstadComponent(code, FalstadPoint(x, y), FalstadPoint(x, y), parts.drop(3), lineNumber, line)
                return@forEachIndexed
            }

            if (parts.size < 5) {
                warnings += "Line $lineNumber: too few fields for component '$code'"
                return@forEachIndexed
            }

            val x1 = parts[1].toIntOrNull()
            val y1 = parts[2].toIntOrNull()
            val x2 = parts[3].toIntOrNull()
            val y2 = parts[4].toIntOrNull()
            if (x1 == null || y1 == null || x2 == null || y2 == null) {
                warnings += "Line $lineNumber: malformed coordinates"
                return@forEachIndexed
            }

            val rawParams = parts.drop(5)
            val normalized = normalizeLegacyComponent(code, rawParams)
            normalized.warning?.let { warnings += "Line $lineNumber: $it" }

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

    private data class NormalizedLegacyComponent(
        val code: String,
        val params: List<String>,
        val warning: String? = null
    )

    private fun normalizeLegacyComponent(code: String, params: List<String>): NormalizedLegacyComponent {
        if (code != "172") {
            return NormalizedLegacyComponent(code, params)
        }

        val initialValue = params.getOrNull(2)?.toDoubleOrNull()
            ?: params.getOrNull(3)?.toDoubleOrNull()
            ?: 0.0

        return NormalizedLegacyComponent(
            code = "vs",
            params = listOf("0", "0", "0", initialValue.toString(), "0"),
            warning = "legacy adjustable voltage source imported as a fixed DC source"
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
                warnings += "Element ${index + 1}: missing or invalid x attribute for <$code>"
                continue
            }

            val start = if (code == "g") {
                val x = child.intAttribute("x") ?: child.pointListAttribute("x")?.getOrNull(0)
                val y = child.intAttribute("y") ?: child.pointListAttribute("x")?.getOrNull(1)
                if (x == null || y == null) {
                    warnings += "Element ${index + 1}: malformed ground element"
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
                    warnings += "Element ${index + 1}: unsupported <$code> element"
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

        val xMap = components.flatMap { listOf(it.start.x, it.end.x) }
            .distinct()
            .sorted()
            .withIndex()
            .associate { it.value to it.index * 2 }
        val yMap = components.flatMap { listOf(it.start.y, it.end.y) }
            .distinct()
            .sorted()
            .withIndex()
            .associate { it.value to it.index * 2 }

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
                    if (y % 2 == 0) markNode(point) else wires += point
                }
            } else {
                for (x in min(start.x, end.x)..max(start.x, end.x)) {
                    val point = FalstadPoint(x, start.y)
                    if (x % 2 == 0) markNode(point) else wires += point
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

        for (component in components) {
            val code = component.code.lowercase()
            val start = mapped(component.start)
            val end = mapped(component.end)

            if (code == "g") {
                markNode(start, FalstadNodeKind.GROUND)
                continue
            }

            val horizontal = start.y == end.y
            val vertical = start.x == end.x
            if (!horizontal && !vertical) {
                warnings += "Line ${component.lineNumber}: diagonal component '${component.code}' is not supported"
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
                    substitutions += "Zener diode substituted with a standard diode"
                    FalstadPlacedKind.DIODE
                }
                "v" -> FalstadPlacedKind.VOLTAGE_SOURCE
                "vs" -> FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE
                "i" -> FalstadPlacedKind.CURRENT_SOURCE
                "s" -> FalstadPlacedKind.SWITCH
                "o" -> FalstadPlacedKind.PROBE_DISPLAY
                "p" -> {
                    substitutions += "Push switch substituted with a maintained switch"
                    FalstadPlacedKind.SWITCH
                }
                else -> {
                    warnings += "Line ${component.lineNumber}: unsupported component '${component.code}'"
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
