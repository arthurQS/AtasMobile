package br.com.atas.mobile.core.database.repository

import android.content.Context
import android.util.Log
import br.com.atas.mobile.core.data.model.Hymn
import br.com.atas.mobile.core.data.repository.HymnRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class AssetHymnRepository @Inject constructor(
    @ApplicationContext context: Context
) : HymnRepository {

    private val tag = "AssetHymnRepository"
    private val hymns: List<Hymn> = loadFromAssets(context)

    override fun watchAll(): Flow<List<Hymn>> = flowOf(hymns)

    override suspend fun search(term: String): List<Hymn> {
        if (term.isBlank()) return hymns.take(50)
        val normalized = term.trim().lowercase()
        return hymns.filter { hymn ->
            hymn.title.lowercase().contains(normalized) ||
                hymn.number.toString().contains(normalized)
        }.take(50)
    }

    override suspend fun refreshFromAssets() {
        // no-op, assets são carregados no construtor
    }

    private fun loadFromAssets(context: Context): List<Hymn> {
        return runCatching {
            context.assets.open("hymns.json").use { input ->
                val jsonText = input.bufferedReader(Charsets.UTF_8).readText()
                HymnJsonParser.parse(jsonText)
            }
        }.onSuccess { list ->
            Log.d(tag, "Loaded ${list.size} hymns from assets")
        }.onFailure { err ->
            Log.e(tag, "Failed to load hymns.json", err)
        }.getOrElse { emptyList() }
    }
}
