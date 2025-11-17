package br.com.atas.mobile.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Hymn(
    val number: Int,
    val title: String,
    val category: String? = null
)
