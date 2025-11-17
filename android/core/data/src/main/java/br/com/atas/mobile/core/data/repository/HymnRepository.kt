package br.com.atas.mobile.core.data.repository

import br.com.atas.mobile.core.data.model.Hymn
import kotlinx.coroutines.flow.Flow

interface HymnRepository {
    fun watchAll(): Flow<List<Hymn>>
    suspend fun search(term: String): List<Hymn>
    suspend fun refreshFromAssets()
}
