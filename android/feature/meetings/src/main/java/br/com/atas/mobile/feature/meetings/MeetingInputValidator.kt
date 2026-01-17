package br.com.atas.mobile.feature.meetings

import java.time.LocalDate

object MeetingInputValidator {

    fun validate(date: String, title: String): String? {
        if (date.isBlank() || title.trim().isBlank()) {
            return "Preencha data e titulo"
        }
        return runCatching { LocalDate.parse(date) }
            .fold(
                onSuccess = { null },
                onFailure = { "Data invalida. Use AAAA-MM-DD." }
            )
    }
}
