package br.com.atas.mobile.core.database.util

import br.com.atas.mobile.core.data.model.MeetingDetails
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class MeetingJsonAdapter @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val serializer = MeetingDetails.serializer()

    fun encode(details: MeetingDetails): String =
        json.encodeToString(serializer, details)

    fun decode(raw: String): MeetingDetails =
        runCatching { json.decodeFromString(serializer, raw) }
            .getOrDefault(MeetingDetails())
}
