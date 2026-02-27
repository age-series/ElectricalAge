package mods.eln.misc

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Obj3DTestResourceTest {
    @Test
    fun loadTestResourcesWiresPartsStringsAndFloats() {
        disableLog4jJmx()
        val obj = Obj3D()

        assertTrue(obj.loadFile("TestObj3D/TestObj3D.obj"))

        val panel = obj.getPart("panel")
        assertNotNull(panel)
        assertTrue(panel.faceGroup.isNotEmpty())
        assertNotNull(panel.faceGroup.first().textureResource)

        val hinge = obj.getPart("hinge")
        assertNotNull(hinge)
        assertEquals(0.25f, hinge.ox)
        assertEquals(0.5f, hinge.oy)
        assertEquals(0.75f, hinge.oz)
        assertEquals(45f, hinge.getFloat("openAngle"))
        assertEquals("testFixture", obj.getString("type"))
        assertEquals("enabled", obj.getString("mode"))
    }
}
