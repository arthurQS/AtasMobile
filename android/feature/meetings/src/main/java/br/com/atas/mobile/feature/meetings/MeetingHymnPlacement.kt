package br.com.atas.mobile.feature.meetings

import br.com.atas.mobile.core.data.model.MeetingSpeaker

data class HymnPlacement(
    val insertAfterIndex: Int,
    val message: String
)

/**
 * Computes where the hymn picker should appear between the list of speakers.
 *
 * Rules:
 * - 0 speakers: sacrament meeting of testimonies (no hymn picker).
 * - 1 speaker: hymn follows the single speaker.
 * - 2 speakers: hymn stays between both speakers.
 * - 3+ speakers: hymn stays between the 2nd and 3rd speakers.
 */
fun calculateHymnPlacement(speakers: List<MeetingSpeaker>): HymnPlacement? {
    if (speakers.isEmpty()) return null
    val insertAfterIndex = if (speakers.size >= 3) 1 else 0
    val message = when (speakers.size) {
        1 -> "O hino intermediário será cantado após o orador."
        2 -> "O hino intermediário ficará entre os dois oradores."
        else -> "O hino intermediário será cantado após o segundo orador."
    }
    return HymnPlacement(insertAfterIndex = insertAfterIndex, message = message)
}
