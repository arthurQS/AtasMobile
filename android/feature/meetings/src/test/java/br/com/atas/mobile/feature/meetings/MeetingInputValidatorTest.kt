package br.com.atas.mobile.feature.meetings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeetingInputValidatorTest {

    @Test
    fun returnsErrorWhenDateOrTitleMissing() {
        assertEquals("Preencha data e titulo", MeetingInputValidator.validate("", "Agenda"))
        assertEquals("Preencha data e titulo", MeetingInputValidator.validate("2026-01-16", ""))
    }

    @Test
    fun returnsErrorWhenDateInvalid() {
        assertEquals(
            "Data invalida. Use AAAA-MM-DD.",
            MeetingInputValidator.validate("16/01/2026", "Agenda")
        )
    }

    @Test
    fun returnsNullWhenValid() {
        assertNull(MeetingInputValidator.validate("2026-01-16", "Agenda"))
    }
}
