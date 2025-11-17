package br.com.atas.mobile.core.database.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HymnJsonParserTest {

    @Test
    fun `parse returns entries from modern schema`() {
        val json = """
            [
                {"number": 1, "title": "A Alva Rompe", "category": "Abertura"},
                {"number": 2, "title": "Vinde Ó Santos"}
            ]
        """.trimIndent()

        val hymns = HymnJsonParser.parse(json)

        assertEquals(2, hymns.size)
        assertEquals(1, hymns.first().number)
        assertEquals("A Alva Rompe", hymns.first().title)
        assertEquals("Abertura", hymns.first().category)
    }

    @Test
    fun `parse understands portuguese legacy keys`() {
        val json = """
            {
                "hinos": [
                    {"numero": "5", "titulo": "Vinde, Ó Santos", "grupo": "Sacramental"}
                ]
            }
        """.trimIndent()

        val hymns = HymnJsonParser.parse(json)

        assertEquals(1, hymns.size)
        assertEquals(5, hymns.first().number)
        assertEquals("Vinde, Ó Santos", hymns.first().title)
        assertEquals("Sacramental", hymns.first().category)
    }

    @Test
    fun `parse skips invalid rows`() {
        val json = """
            [
                {"number": "NaN", "title": "Inválido"},
                {"numero": 10},
                {"number": 20, "title": ""}
            ]
        """.trimIndent()

        val hymns = HymnJsonParser.parse(json)

        assertTrue(hymns.isEmpty())
    }
}
