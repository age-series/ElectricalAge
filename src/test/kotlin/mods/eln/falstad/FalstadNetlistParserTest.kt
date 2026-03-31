package mods.eln.falstad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FalstadNetlistParserTest {
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
}
