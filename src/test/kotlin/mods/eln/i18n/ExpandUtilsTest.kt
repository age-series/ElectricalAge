package mods.eln.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartesianProductTest {

    @Test
    fun emptyAxesReturnsSingleEmptyCombination() {
        val result = cartesianProduct(emptyList())
        assertEquals(listOf(emptyList<String>()), result)
    }

    @Test
    fun singleAxisExpandsToIndividualElements() {
        val result = cartesianProduct(listOf(listOf("A", "B", "C")))
        assertEquals(3, result.size)
        assertTrue(result.contains(listOf("A")))
        assertTrue(result.contains(listOf("B")))
        assertTrue(result.contains(listOf("C")))
    }

    @Test
    fun twoAxesProduceFullProduct() {
        val result = cartesianProduct(listOf(listOf("X", "Y"), listOf("1", "2")))
        assertEquals(4, result.size)
        assertTrue(result.contains(listOf("X", "1")))
        assertTrue(result.contains(listOf("X", "2")))
        assertTrue(result.contains(listOf("Y", "1")))
        assertTrue(result.contains(listOf("Y", "2")))
    }

    @Test
    fun threeAxesProduceFullProduct() {
        val result = cartesianProduct(
            listOf(listOf("A", "B"), listOf("X"), listOf("1", "2", "3"))
        )
        assertEquals(6, result.size)
        assertTrue(result.contains(listOf("A", "X", "1")))
        assertTrue(result.contains(listOf("B", "X", "3")))
    }

    @Test
    fun singleElementAxesProduceOneCombination() {
        val result = cartesianProduct(listOf(listOf("A"), listOf("B"), listOf("C")))
        assertEquals(1, result.size)
        assertEquals(listOf("A", "B", "C"), result[0])
    }
}

class SubstituteFormatTest {

    @Test
    fun basicSubstitutionWithMatchingCount() {
        assertEquals("Copper 26 AWG Cable Bare", substituteFormat("%s %s Cable %s", listOf("Copper", "26 AWG", "Bare")))
    }

    @Test
    fun singlePlaceholder() {
        assertEquals("Copper Molten Metal Pile", substituteFormat("%s Molten Metal Pile", listOf("Copper")))
    }

    @Test
    fun noPlaceholdersReturnsFormatUnchanged() {
        assertEquals("Hello World", substituteFormat("Hello World", listOf("ignored")))
    }

    @Test
    fun extraValuesAreIgnored() {
        assertEquals("A B", substituteFormat("%s %s", listOf("A", "B", "extra")))
    }

    @Test
    fun fewerValuesThanPlaceholdersProducesTruncatedOutput() {
        assertEquals("A ", substituteFormat("%s %s", listOf("A")))
    }

    @Test
    fun literalPercentNotFollowedBySIsPreserved() {
        assertEquals("100%% done", substituteFormat("100%% done", listOf()))
    }

    @Test
    fun emptyFormatWithValues() {
        assertEquals("", substituteFormat("", listOf("A")))
    }

    @Test
    fun formatWithSlashes() {
        assertEquals("Copper 1/0 AWG Cable 1000V", substituteFormat("%s %s Cable %s", listOf("Copper", "1/0 AWG", "1000V")))
    }
}
