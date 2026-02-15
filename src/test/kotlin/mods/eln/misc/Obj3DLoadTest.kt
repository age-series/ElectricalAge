package mods.eln.misc

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Obj3DLoadTest {
    @Test
    fun loadStreetLightReadsMtlAndTxtProperties() {
        disableLog4jJmx()
        val obj = Obj3D()

        assertTrue(obj.loadFile("StreetLight/StreetLight.obj"))

        val socket = obj.getPart("socket")
        assertNotNull(socket)
        assertTrue(socket.faceGroup.isNotEmpty())
        val fg = socket.faceGroup.first()
        assertNotNull(fg.textureResource)
        assertTrue(fg.textureResource.toString().contains("eln:model/StreetLight/StreetLight_on.png"))
        assertEquals("noLampDraw", obj.getString("type"))
        assertEquals("enable", obj.getString("cable"))
        assertTrue(obj.xDim > 0f)
        assertTrue(obj.dimMaxInv > 0f)
    }

    @Test
    fun loadMqttSignalControllerAppliesTxtOriginsAndFloats() {
        disableLog4jJmx()
        val obj = Obj3D()

        assertTrue(obj.loadFile("MqttSignalController/MqttSignalController.obj"))

        val door = obj.getPart("door")
        assertNotNull(door)
        assertEquals(0.3125f, door.ox)
        assertEquals(0.0f, door.oy)
        assertEquals(0.3125f, door.oz)
        assertEquals(90f, door.getFloat("alphaOff"))
    }
}
