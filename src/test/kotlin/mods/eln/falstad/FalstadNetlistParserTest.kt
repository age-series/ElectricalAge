package mods.eln.falstad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FalstadNetlistParserTest {
    private data class TerminalPoint(
        val label: String,
        val source: FalstadPoint,
        val placed: FalstadPoint
    )

    private class PointUnionFind {
        private val parent = linkedMapOf<FalstadPoint, FalstadPoint>()

        fun add(point: FalstadPoint) {
            parent.putIfAbsent(point, point)
        }

        fun union(a: FalstadPoint, b: FalstadPoint) {
            val rootA = find(a)
            val rootB = find(b)
            if (rootA != rootB) parent[rootA] = rootB
        }

        fun connected(a: FalstadPoint, b: FalstadPoint): Boolean = find(a) == find(b)

        private fun find(point: FalstadPoint): FalstadPoint {
            val currentParent = parent.getOrPut(point) { point }
            if (currentParent == point) return point
            val root = find(currentParent)
            parent[point] = root
            return root
        }
    }

    private fun FalstadParseResult.findNearestConnectionOnAxis(excluding: FalstadComponent, x: Int, y: Int, above: Boolean): FalstadPoint? {
        val candidates = components
            .asSequence()
            .filter { it !== excluding }
            .flatMap { sequenceOf(it.start, it.end) }
            .filter { it.x == x && if (above) it.y < y else it.y > y }
            .distinct()
        return if (above) candidates.maxByOrNull { it.y } else candidates.minByOrNull { it.y }
    }

    private fun xorTerminalPoints(parseResult: FalstadParseResult, plan: FalstadLayoutPlan): List<TerminalPoint> {
        val placedBySource = plan.components.groupBy { it.source }
        return buildList {
            for (component in parseResult.components) {
                when (component.code) {
                    "151" -> {
                        val placed = placedBySource.getValue(component).single()
                        val upper = parseResult.findNearestConnectionOnAxis(component, component.start.x, component.start.y, above = true)
                        val lower = parseResult.findNearestConnectionOnAxis(component, component.start.x, component.start.y, above = false)
                        if (upper != null) add(TerminalPoint("151-upper-${component.lineNumber}", upper, placed.start))
                        if (lower != null) add(TerminalPoint("151-lower-${component.lineNumber}", lower, placed.end))
                        add(TerminalPoint("151-output-${component.lineNumber}", component.end, placed.extraPoints.first()))
                    }
                    "L" -> {
                        val placed = placedBySource.getValue(component).single()
                        add(TerminalPoint("L-${component.lineNumber}", component.start, placed.start))
                    }
                    "M" -> {
                        val placed = placedBySource.getValue(component).single()
                        add(TerminalPoint("M-${component.lineNumber}", component.start, placed.start))
                    }
                }
            }
        }
    }

    private fun xorFalstadNetGroups(parseResult: FalstadParseResult, terminals: List<TerminalPoint>): PointUnionFind {
        val nets = PointUnionFind()
        terminals.forEach { nets.add(it.source) }
        for (component in parseResult.components) {
            when (component.code) {
                "w" -> nets.union(component.start, component.end)
                "g" -> nets.add(component.start)
                "L", "M", "151" -> {
                    nets.add(component.start)
                    nets.add(component.end)
                }
            }
        }
        return nets
    }

    private fun generatedConnectivity(plan: FalstadLayoutPlan): Set<Pair<FalstadPoint, FalstadPoint>> {
        val occupied = (plan.nodes.keys + plan.wires).toSet()
        val visited = mutableSetOf<FalstadPoint>()
        val connections = mutableSetOf<Pair<FalstadPoint, FalstadPoint>>()

        for (start in occupied) {
            if (!visited.add(start)) continue
            val queue = ArrayDeque<FalstadPoint>()
            val component = linkedSetOf<FalstadPoint>()
            queue += start
            component += start

            while (queue.isNotEmpty()) {
                val point = queue.removeFirst()
                val neighbors = listOf(
                    FalstadPoint(point.x - 1, point.y),
                    FalstadPoint(point.x + 1, point.y),
                    FalstadPoint(point.x, point.y - 1),
                    FalstadPoint(point.x, point.y + 1)
                )
                for (neighbor in neighbors) {
                    if (neighbor !in occupied || !visited.add(neighbor)) continue
                    queue += neighbor
                    component += neighbor
                }
            }

            for (a in component) {
                for (b in component) {
                    connections += a to b
                }
            }
        }

        return connections
    }

    @Test
    fun parsesLegacyBasicNetlist() {
        val text = """
            ${'$'} 1 5.0E-6 10 50 5
            v 0 0 64 0 0 5 0 0 0
            r 64 0 128 0 0 1000
            w 128 0 128 64 0
            g 128 64 128 64 0
        """.trimIndent()

        val result = FalstadNetlistParser.parse(text)
        assertEquals(4, result.components.size)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun plansDoubledGrid() {
        val text = """
            v 0 0 64 0 0 5 0 0 0
            r 64 0 128 0 0 1000
            w 128 0 128 64 0
            g 128 64 128 64 0
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadNetlistParser.parse(text))
        assertEquals(5, plan.width)
        assertEquals(3, plan.height)
        assertEquals(FalstadNodeKind.GROUND, plan.nodes[FalstadPoint(4, 2)])
        assertTrue(plan.wires.contains(FalstadPoint(4, 1)))
        assertEquals(FalstadPlacedKind.VOLTAGE_SOURCE, plan.components[0].kind)
        assertEquals(FalstadPlacedKind.RESISTOR, plan.components[1].kind)
    }

    @Test
    fun recordsSubstitutions() {
        val text = """
            z 0 0 64 0 0 5.1
            p 64 0 128 0 0
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadNetlistParser.parse(text))
        assertTrue(plan.components[0].substitutions.any { it.contains("Zener") })
        assertTrue(plan.components[1].substitutions.any { it.contains("Push switch") })
    }

    @Test
    fun parsesXmlCircuitFormat() {
        val text = """
            <cir f="1" ts="0.000005" ic="10.20027730826997" cb="50" pb="43" vr="5" mts="5e-11">
              <r x="176 80 384 80" f="0" r="10"/>
              <s x="384 80 448 80" f="0" p="1"/>
              <w x="176 80 176 352" f="0"/>
              <c x="384 352 176 352" f="0" c="0.000015" iv="-10" sr="0" vd="2.888220517043701"/>
              <o en="4" sp="64" f="x3" p="0">
                <p v="0" sc="10"/>
                <p v="3" sc="0.05"/>
              </o>
            </cir>
        """.trimIndent()

        val root = FalstadXmlParser.parse(text)
        assertEquals("cir", root.name)
        assertEquals("1", root.attribute("f"))
        assertEquals(5, root.children.size)

        val resistor = root.children[0]
        assertEquals("r", resistor.name)
        assertEquals(listOf(176, 80, 384, 80), resistor.pointListAttribute("x"))
        assertEquals(10.0, resistor.doubleAttribute("r"))

        val scope = root.children[4]
        assertEquals("o", scope.name)
        assertEquals(2, scope.children.size)
        assertEquals("p", scope.children[0].name)
        assertEquals(0, scope.children[0].intAttribute("v"))
    }

    @Test
    fun deviceParserAcceptsXmlCircuitFormat() {
        val text = """
            <cir f="1" ts="0.000005" ic="10.2" cb="50" pb="43" vr="5" mts="5e-11">
              <r x="176 80 384 80" f="0" r="10"/>
              <s x="384 80 448 80" f="0" p="1"/>
              <w x="176 80 176 352" f="0"/>
              <c x="384 352 176 352" f="0" c="0.000015" iv="-10"/>
              <l x="384 80 384 352" f="0" l="1" ic="0"/>
              <v x="448 352 448 80" f="0" wf="0" maxv="5"/>
            </cir>
        """.trimIndent()

        val result = FalstadDeviceParser.parse(text)
        assertEquals(6, result.components.size)
        assertEquals("r", result.components[0].code)
        assertEquals(listOf("0", "10"), result.components[0].params)
        assertEquals("s", result.components[1].code)
        assertEquals(listOf("0", "1"), result.components[1].params)
        assertEquals("v", result.components[5].code)
        assertEquals("5", result.components[5].params[1])
        assertEquals("0", result.components[5].params[4])
    }

    @Test
    fun parsesLegacyAdjustableVoltageSourceAsFixedDcSource() {
        val text = """
            ${'$'} 1 5.0E-6 10.391409633455755 50 5.0 50
            r 256 176 256 304 0 100.0
            172 304 176 304 128 0 6 5.0 5.0 0.0 0.0 0.5 Voltage
            g 256 336 256 352 0
            w 256 304 256 336 1
            r 352 176 352 304 0 1000.0
            w 352 304 352 336 1
            g 352 336 352 352 0
            w 304 176 352 176 0
            w 256 176 304 176 0
        """.trimIndent()

        val result = FalstadDeviceParser.parse(text)
        assertTrue(result.warnings.any { it.contains("adjustable voltage source") })
        val source = result.components.first { it.code == "vs" }
        assertEquals(listOf("0", "0", "0", "5.0", "0"), source.params)

        val plan = FalstadLayoutPlanner.plan(result)
        assertTrue(plan.components.any { it.kind == FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE })
    }

    @Test
    fun plansLegacyProbeDisplayWithExtraDotSourceCell() {
        val text = """
            v 112 368 112 48 0 0 40.0 10.0 0.0
            w 112 48 240 48 0
            r 240 48 240 208 0 10000
            r 240 208 240 368 0 10000
            w 112 368 240 368 0
            O 240 208 304 208 1
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadDeviceParser.parse(text))
        val probeDisplay = plan.components.single { it.kind == FalstadPlacedKind.PROBE_DISPLAY }

        assertEquals(FalstadPoint(2, 2), probeDisplay.start)
        assertEquals(FalstadPoint(4, 2), probeDisplay.end)
        assertEquals(FalstadPoint(3, 2), probeDisplay.cell)
        assertEquals(6, plan.width)
        assertTrue(FalstadPoint(4, 2) !in plan.nodes)
        assertTrue(FalstadPoint(5, 2) !in plan.nodes)
    }

    @Test
    fun rotatesLayoutClockwise() {
        val text = """
            v 0 0 64 0 0 5 0 0 0
            r 64 0 128 0 0 1000
            w 128 0 128 64 0
            g 128 64 128 64 0
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadNetlistParser.parse(text))
        val rotated = plan.rotatedClockwise()

        assertEquals(3, rotated.width)
        assertEquals(5, rotated.height)
        assertEquals(FalstadNodeKind.GROUND, rotated.nodes[FalstadPoint(0, 4)])
        assertTrue(rotated.wires.contains(FalstadPoint(1, 4)))
        assertEquals(FalstadAxis.VERTICAL, rotated.components[0].axis)
        assertEquals(FalstadPoint(2, 0), rotated.components[0].start)
        assertEquals(FalstadPoint(2, 2), rotated.components[0].end)
    }

    @Test
    fun plansFalstadSpdtSwitchAsTwoComplementarySwitches() {
        val text = """
            ${'$'} 1 0.000005 16.13108636308289 50 5 50
            v 96 336 96 64 0 0 40 5 0 0 0.5
            S 256 144 256 64 0 0 false 0 2
            w 96 64 240 64 0
            r 96 336 256 336 0 140
            r 256 336 400 336 0 140
            w 272 64 400 64 0
            w 400 64 400 336 0
            l 256 144 256 336 0 3 0
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadDeviceParser.parse(text))
        val switches = plan.components.filter { it.kind == FalstadPlacedKind.SWITCH }

        assertEquals(2, switches.size)
        assertEquals(setOf(true, false), switches.mapNotNull { it.forcedSwitchState }.toSet())
        assertTrue(switches.any { it.start == FalstadPoint(2, 2) && it.end == FalstadPoint(2, 0) })
        assertTrue(switches.any { it.start == FalstadPoint(6, 2) && it.end == FalstadPoint(6, 0) })
        assertTrue(plan.components.any { it.substitutions.any { msg -> msg.contains("SPDT switch") } })
    }

    @Test
    fun plansCurrentSourceWithoutStretchedLeadWires() {
        val text = """
            i 0 0 128 0 0 0.5
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadNetlistParser.parse(text))

        assertEquals(1, plan.components.size)
        assertEquals(FalstadPlacedKind.CURRENT_SOURCE, plan.components.single().kind)
        assertTrue(plan.wires.isEmpty())
        assertEquals(FalstadNodeKind.NORMAL, plan.nodes[FalstadPoint(0, 0)])
        assertEquals(FalstadNodeKind.NORMAL, plan.nodes[FalstadPoint(2, 0)])
    }

    @Test
    fun defaultsFalstadCurrentSourceToTenMilliampsWhenValueMissing() {
        val text = """
            i 112 352 112 32 0
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadNetlistParser.parse(text))
        val placement = FalstadImporter.run {
            val component = plan.components.single { it.kind == FalstadPlacedKind.CURRENT_SOURCE }
            val method = javaClass.getDeclaredMethod("currentSourcePlacement", FalstadPlacedComponent::class.java)
            method.isAccessible = true
            method.invoke(this, component)
        }

        val currentField = placement.javaClass.getDeclaredField("current")
        currentField.isAccessible = true
        assertEquals(0.01, currentField.getDouble(placement))
    }

    @Test
    fun plansFalstadLogicSymbolsFromXorNetlist() {
        val text = """
            ${'$'} 1 5.0E-6 1.5 50 5.0
            151 96 240 208 240 0 2 0
            151 208 192 320 192 0 2 0
            151 208 288 320 288 0 2 0
            w 208 240 208 272 0
            w 208 240 208 208 0
            151 320 240 432 240 0 2 0
            w 320 192 320 224 0
            w 320 256 320 288 0
            w 96 176 96 224 0
            w 96 176 208 176 0
            w 96 256 96 304 0
            w 96 304 208 304 0
            M 432 240 480 240 0
            L 96 176 48 176 0 true false
            L 96 304 48 304 0 true false
        """.trimIndent()

        val plan = FalstadLayoutPlanner.plan(FalstadDeviceParser.parse(text))

        assertEquals(4, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_NAND_GATE })
        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_INPUT })
        assertEquals(1, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_OUTPUT })
        assertTrue(plan.warnings.isEmpty())
    }

    @Test
    fun generatedPlacementPlanDoesNotShortFalstadXorNets() {
        val text = """
            ${'$'} 1 5.0E-6 1.5 50 5.0
            151 96 240 208 240 0 2 0
            151 208 192 320 192 0 2 0
            151 208 288 320 288 0 2 0
            w 208 240 208 272 0
            w 208 240 208 208 0
            151 320 240 432 240 0 2 0
            w 320 192 320 224 0
            w 320 256 320 288 0
            w 96 176 96 224 0
            w 96 176 208 176 0
            w 96 256 96 304 0
            w 96 304 208 304 0
            M 432 240 480 240 0
            L 96 176 48 176 0 true false
            L 96 304 48 304 0 true false
        """.trimIndent()

        val parseResult = FalstadDeviceParser.parse(text)
        val plan = FalstadLayoutPlanner.plan(parseResult)
        val terminals = xorTerminalPoints(parseResult, plan)
        val sourceNets = xorFalstadNetGroups(parseResult, terminals)
        val placedConnectivity = generatedConnectivity(plan)

        for (i in terminals.indices) {
            for (j in i + 1 until terminals.size) {
                val left = terminals[i]
                val right = terminals[j]
                val sourceConnected = sourceNets.connected(left.source, right.source)
                val placedConnected = (left.placed to right.placed) in placedConnectivity
                assertEquals(
                    sourceConnected,
                    placedConnected,
                    "${left.label} (${left.source} -> ${left.placed}) vs ${right.label} (${right.source} -> ${right.placed})"
                )
            }
        }
    }

//    @Test
//    fun plansHalfAdderLogicSymbols() {
//        val plan = FalstadLayoutPlanner.plan(FalstadDeviceParser.parse(java.io.File("halfadder.txt").readText()))
//
//        assertEquals(1, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_XOR_GATE })
//        assertEquals(1, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_AND_GATE })
//        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_INPUT })
//        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_OUTPUT })
//        assertTrue(plan.warnings.isEmpty(), plan.warnings.joinToString())
//    }
//
//    @Test
//    fun plansFullAdderLogicSymbols() {
//        val plan = FalstadLayoutPlanner.plan(FalstadDeviceParser.parse(java.io.File("fulladd.txt").readText()))
//
//        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_XOR_GATE })
//        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_AND_GATE })
//        assertEquals(1, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_OR_GATE })
//        assertEquals(3, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_INPUT })
//        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_OUTPUT })
//        assertTrue(plan.warnings.isEmpty(), plan.warnings.joinToString())
//    }
//
//    @Test
//    fun plansDigitalComparatorLogicSymbols() {
//        val plan = FalstadLayoutPlanner.plan(FalstadDeviceParser.parse(java.io.File("digicompare.txt").readText()))
//
//        assertEquals(2, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_AND_GATE })
//        assertEquals(4, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_NAND_GATE })
//        assertEquals(1, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_OR_GATE })
//        assertEquals(1, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_NOR_GATE })
//        assertEquals(3, plan.components.count { it.kind == FalstadPlacedKind.FALSTAD_NOT_GATE })
//        assertEquals(4, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_INPUT })
//        assertEquals(3, plan.components.count { it.kind == FalstadPlacedKind.SIGNAL_OUTPUT })
//        assertTrue(plan.warnings.isEmpty(), plan.warnings.joinToString())
//    }
}
