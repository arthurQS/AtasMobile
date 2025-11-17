package br.com.atas.mobile.core.database.repository

import br.com.atas.mobile.core.data.model.Hymn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Lightweight parser that understands both the legacy web JSON (numero/titulo)
 * and the new mobile JSON schema (number/title/category).
 */
object HymnJsonParser {

    private val parser = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): List<Hymn> {
        if (jsonText.isBlank()) return emptyList()
        val root = runCatching { parser.parseToJsonElement(jsonText) }.getOrElse { return emptyList() }
        val array = extractArray(root) ?: return emptyList()
        return array.mapNotNull { it.asHymn() }
    }

    private fun extractArray(element: JsonElement): JsonArray? {
        return when (element) {
            is JsonArray -> element
            is JsonObject -> {
                element["hinos"] as? JsonArray
                    ?: element["hymns"] as? JsonArray
                    ?: element.values.firstOrNull { it is JsonArray } as? JsonArray
            }
            else -> null
        }
    }

    private fun JsonElement.asHymn(): Hymn? {
        val json = this.jsonObject
        val numberField = json["number"] ?: json["numero"]
        val titleField = json["title"] ?: json["titulo"]
        if (numberField == null || titleField == null) return null
        val number = numberField.jsonPrimitive.content.trim().toIntOrNull() ?: return null
        val title = titleField.jsonPrimitive.content.trim().ifEmpty { return null }
        val categoryField = json["category"] ?: json["categoria"] ?: json["grupo"]
        val category = categoryField?.jsonPrimitive?.content?.trim()?.ifEmpty { null }
        return Hymn(number = number, title = title, category = category)
    }
}
