package mods.eln.misc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Obj3DTest {
    @Test
    fun partTracksBoundsAndFloats() {
        val obj = Obj3D()
        val part = obj.Obj3DPart(obj.vertex, obj.uv)

        val v1 = obj.Vertex(1f, 2f, 3f)
        val v2 = obj.Vertex(-1f, -2f, -3f)
        part.addVertex(v1)
        part.addVertex(v2)

        assertEquals(-1f, part.xMin)
        assertEquals(3f, part.zMax)
        assertEquals(0f, part.getFloat("missing"))
    }

    @Test
    fun boundingBoxUsesFaceGroups() {
        val obj = Obj3D()
        val part = obj.Obj3DPart(obj.vertex, obj.uv)
        val fg = Obj3D.FaceGroup()

        val a = obj.Vertex(0f, 0f, 0f)
        val b = obj.Vertex(1f, 0f, 0f)
        val c = obj.Vertex(0f, 1f, 0f)
        val normal = obj.Normal(a, b, c)
        val face = obj.Face(arrayOf(a, b, c), arrayOf(obj.Uv(0f, 0f), obj.Uv(0f, 0f), obj.Uv(0f, 0f)), normal)
        fg.face.add(face)
        part.faceGroup.add(fg)

        val box = part.boundingBox()
        assertNotNull(box)
        assertTrue(box.max.xCoord >= 1.0)
        assertTrue(box.max.yCoord >= 1.0)
    }

    @Test
    fun faceGroupDrawNoBindUsesGlShim() {
        val obj = Obj3D()
        val fg = Obj3D.FaceGroup()
        val a = obj.Vertex(0f, 0f, 0f)
        val b = obj.Vertex(1f, 0f, 0f)
        val c = obj.Vertex(0f, 1f, 0f)
        val normal = obj.Normal(a, b, c)
        val face = obj.Face(arrayOf(a, b, c), arrayOf(obj.Uv(0f, 0f), obj.Uv(0f, 0f), obj.Uv(0f, 0f)), normal)
        fg.face.add(face)

        fg.drawNoBind()
        fg.drawNoBind()
    }
}
