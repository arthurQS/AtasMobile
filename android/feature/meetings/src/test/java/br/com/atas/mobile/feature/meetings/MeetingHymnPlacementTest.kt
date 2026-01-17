package br.com.atas.mobile.feature.meetings

import br.com.atas.mobile.core.data.model.MeetingSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeetingHymnPlacementTest {

    @Test
    fun `returns null when there are no speakers`() {
        assertNull(calculateHymnPlacement(emptyList()))
    }

    @Test
    fun `single speaker keeps hymn after the only speaker`() {
        val placement = calculateHymnPlacement(listOf(MeetingSpeaker()))

        assertEquals(0, placement?.insertAfterIndex)
        assertEquals("O hino intermediario sera cantado apos o orador.", placement?.message)
    }

    @Test
    fun `two speakers keep hymn between them`() {
        val placement = calculateHymnPlacement(
            listOf(MeetingSpeaker(nome = "A"), MeetingSpeaker(nome = "B"))
        )

        assertEquals(0, placement?.insertAfterIndex)
        assertEquals("O hino intermediario ficara entre os dois oradores.", placement?.message)
    }

    @Test
    fun `three or more speakers keep hymn after the second`() {
        val placement = calculateHymnPlacement(
            listOf(MeetingSpeaker(), MeetingSpeaker(), MeetingSpeaker(), MeetingSpeaker())
        )

        assertEquals(1, placement?.insertAfterIndex)
        assertEquals("O hino intermediario sera cantado apos o segundo orador.", placement?.message)
    }
}
