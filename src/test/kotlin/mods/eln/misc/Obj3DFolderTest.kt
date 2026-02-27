package mods.eln.misc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class Obj3DFolderTest {
    @Test
    fun emptyFolderUsesFallbackModelForMissingObjects() {
        val folder = Obj3DFolder()
        assertNotNull(folder.getObj("missing"))
        assertNull(folder.getPart("missing", "part"))
        assertEquals(0, folder.objectList.size)
    }
}
