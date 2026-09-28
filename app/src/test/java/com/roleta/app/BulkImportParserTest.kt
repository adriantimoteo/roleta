package com.roleta.app

import com.roleta.app.data.repository.BulkImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkImportParserTest {

    @Test
    fun firstLineIsTitle_restAreItems() {
        val parsed = BulkImportParser.parse("Weekend Trips\nBaguio\nLa Union\nBatangas")!!
        assertEquals("Weekend Trips", parsed.title)
        assertEquals(listOf("Baguio", "La Union", "Batangas"), parsed.items)
        assertTrue(parsed.duplicates.isEmpty())
    }

    @Test
    fun trimsLines_ignoresBlankLines_handlesCrlf() {
        val parsed = BulkImportParser.parse("\n\n  Movies  \r\n\r\n Dune \r\n\t\r\nArrival\n")!!
        assertEquals("Movies", parsed.title)
        assertEquals(listOf("Dune", "Arrival"), parsed.items)
    }

    @Test
    fun stripsBulletMarkers() {
        val parsed = BulkImportParser.parse("- Food\n- Pizza\n* Tacos\n• Ramen\n7-Eleven")!!
        assertEquals("Food", parsed.title)
        assertEquals(listOf("Pizza", "Tacos", "Ramen", "7-Eleven"), parsed.items)
    }

    @Test
    fun repeatedItems_areReportedAsDuplicates_caseInsensitive() {
        val parsed = BulkImportParser.parse("Food\nPizza\npizza\nTacos\nPIZZA")!!
        assertEquals(listOf("Pizza", "Tacos"), parsed.items)
        assertEquals(listOf("pizza", "PIZZA"), parsed.duplicates)
    }

    @Test
    fun titleOnly_hasNoItems() {
        val parsed = BulkImportParser.parse("Just a title\n\n")!!
        assertEquals("Just a title", parsed.title)
        assertTrue(parsed.items.isEmpty())
    }

    @Test
    fun blankText_returnsNull() {
        assertNull(BulkImportParser.parse(""))
        assertNull(BulkImportParser.parse("  \n \n"))
    }
}
