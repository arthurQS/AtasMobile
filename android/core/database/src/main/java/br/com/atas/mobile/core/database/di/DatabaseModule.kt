package br.com.atas.mobile.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.atas.mobile.core.data.model.MeetingDetails
import br.com.atas.mobile.core.data.model.MeetingHymns
import br.com.atas.mobile.core.data.model.MeetingPrayers
import br.com.atas.mobile.core.data.model.MeetingSpeaker
import br.com.atas.mobile.core.data.repository.BackupRepository
import br.com.atas.mobile.core.data.repository.HymnRepository
import br.com.atas.mobile.core.data.repository.MeetingRepository
import br.com.atas.mobile.core.data.repository.SyncSettingsRepository
import br.com.atas.mobile.core.database.AppDatabase
import br.com.atas.mobile.core.database.dao.MeetingDao
import br.com.atas.mobile.core.database.datastore.BackupSettingsDataStore
import br.com.atas.mobile.core.database.datastore.SyncSettingsDataStore
import br.com.atas.mobile.core.database.model.MeetingEntity
import br.com.atas.mobile.core.database.repository.AssetHymnRepository
import br.com.atas.mobile.core.database.repository.DefaultBackupRepository
import br.com.atas.mobile.core.database.repository.OfflineMeetingRepository
import br.com.atas.mobile.core.database.util.MeetingJsonAdapter
import br.com.atas.mobile.core.drive.api.DriveBackupCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        adapter: MeetingJsonAdapter
    ): AppDatabase {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "atas.db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
        runBlocking {
            seedDatabaseIfNeeded(db, adapter)
        }
        return db
    }

    @Provides
    fun provideMeetingDao(db: AppDatabase): MeetingDao = db.meetingDao()

    @Provides
    @Singleton
    fun provideMeetingRepository(
        meetingDao: MeetingDao,
        adapter: MeetingJsonAdapter
    ): MeetingRepository = OfflineMeetingRepository(meetingDao, adapter)

    @Provides
    @Singleton
    fun provideBackupRepository(
        settingsDataStore: BackupSettingsDataStore,
        coordinator: DriveBackupCoordinator
    ): BackupRepository = DefaultBackupRepository(settingsDataStore, coordinator)

    @Provides
    @Singleton
    fun provideSyncSettingsRepository(
        settingsDataStore: SyncSettingsDataStore
    ): SyncSettingsRepository = settingsDataStore

    @Provides
    @Singleton
    fun provideHymnRepository(
        impl: AssetHymnRepository
    ): HymnRepository = impl
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meetings ADD COLUMN sync_version INTEGER NOT NULL DEFAULT 0")
    }
}

private suspend fun seedDatabaseIfNeeded(
    db: AppDatabase,
    adapter: MeetingJsonAdapter
) {
    val dao = db.meetingDao()
    if (dao.count() > 0) return
    dao.insertAll(createSampleMeetings(adapter))
}

private fun createSampleMeetings(
    adapter: MeetingJsonAdapter
): List<MeetingEntity> {
    val baseDate = LocalDate.now()
    val now = Instant.now()
    val templates = listOf(
        MeetingSeed(
            title = "Reunião Sacramental de Novembro",
            details = MeetingDetails(
                orgao = "Ala Vila Nova",
                frequencia = "190",
                tipo = "Sacramental",
                preside = "Bispo Martins",
                dirige = "Bispo Martins",
                organista = "Maria Campos",
                regente = "Felipe Costa",
                hinos = MeetingHymns(
                    abertura = "5 - Vinde, Ó Santos",
                    sacramento = "71 - Silêncio Sagrado",
                    intermediario = "88 - A Verdade Irá Triunfar",
                    encerramento = "281 - Chamados a Servir"
                ),
                oracoes = MeetingPrayers(
                    abertura = "Cláudio Ribeiro",
                    encerramento = "Laura Santos"
                ),
                oradores = listOf(
                    MeetingSpeaker(nome = "Ana Costa", assunto = "O poder das escrituras"),
                    MeetingSpeaker(nome = "Ricardo Lopes", assunto = "Serviço cristão"),
                    MeetingSpeaker(nome = "Ir. Souza", assunto = "Testemunho pessoal")
                )
            )
        ),
        MeetingSeed(
            title = "Domingo de Testemunhos Dezembro",
            details = MeetingDetails(
                orgao = "Ala Horizonte",
                frequencia = "205",
                tipo = "Sacramental",
                preside = "Pres. Oliveira",
                dirige = "Bispo Peixoto",
                organista = "Juliana Gomes",
                regente = "Lucas Moraes",
                anuncios = "Mutirão de Natal na próxima semana.",
                hinos = MeetingHymns(
                    abertura = "2 - O Espírito de Deus",
                    sacramento = "173 - Ó Meu Pai",
                    intermediario = "193 - Graças Te Damos",
                    encerramento = "209 - Chamados a Servir"
                ),
                oracoes = MeetingPrayers(
                    abertura = "Mateus Soares",
                    encerramento = "Patrícia Reis"
                ),
                oradores = emptyList()
            )
        ),
        MeetingSeed(
            title = "Reunião Especial da Primária",
            details = MeetingDetails(
                orgao = "Ala Central",
                frequencia = "160",
                tipo = "Sacramental",
                preside = "Bispo Silva",
                dirige = "1º Conselheiro",
                organista = "Cecília Nunes",
                regente = "Tatiana Prado",
                chamados = "Sustentação da nova presidência da Primária.",
                hinos = MeetingHymns(
                    abertura = "301 - Eu Gosto de Ver o Templo",
                    sacramento = "69 - Jesus de Quem Dependo",
                    intermediario = "Primary Medley",
                    encerramento = "110 - Nos Passos do Mestre"
                ),
                oracoes = MeetingPrayers(
                    abertura = "Arthur Ferreira",
                    encerramento = "Lia Pereira"
                ),
                oradores = listOf(
                    MeetingSpeaker(nome = "Crianças da Primária", assunto = "Mensagens breves"),
                    MeetingSpeaker(nome = "Pres. da Primária", assunto = "A fé das crianças")
                )
            )
        ),
        MeetingSeed(
            title = "Domingo de Jovens",
            details = MeetingDetails(
                orgao = "Ala Jardim",
                frequencia = "178",
                tipo = "Sacramental",
                preside = "Presidência da Estaca",
                dirige = "Bispo Almeida",
                organista = "Rafael Prado",
                regente = "Beatriz Luna",
                anuncios = "Acampamento dos jovens em janeiro.",
                hinos = MeetingHymns(
                    abertura = "13 - Deus Vive",
                    sacramento = "195 - O Sacrifício Expiatório",
                    intermediario = "86 - Faze o Bem",
                    encerramento = "26 - Alegres Cantemos"
                ),
                oracoes = MeetingPrayers(
                    abertura = "Tiago Rocha",
                    encerramento = "Clara Ferreira"
                ),
                oradores = listOf(
                    MeetingSpeaker(nome = "Lucas Mattos", assunto = "Preparação para o templo"),
                    MeetingSpeaker(nome = "Camila Borges", assunto = "Esperança em Cristo"),
                    MeetingSpeaker(nome = "Pr. Jovens", assunto = "Servo de confiança")
                )
            )
        ),
        MeetingSeed(
            title = "Reunião de Natal",
            details = MeetingDetails(
                orgao = "Ala Planalto",
                frequencia = "230",
                tipo = "Sacramental",
                preside = "Bispo Azevedo",
                dirige = "Bispo Azevedo",
                organista = "João Clemente",
                regente = "Sara Farias",
                anuncios = "Devocional de Natal no dia 24.",
                observacoes = "Participação especial do coro da ala.",
                hinos = MeetingHymns(
                    abertura = "201 - Nasceu Jesus",
                    sacramento = "72 - Deixou a Corte Celestial",
                    intermediario = "202 - Canção de Natal",
                    encerramento = "204 - Noite Feliz"
                ),
                oracoes = MeetingPrayers(
                    abertura = "Helena Campos",
                    encerramento = "Marcos Rezende"
                ),
                oradores = listOf(
                    MeetingSpeaker(nome = "Coro da Ala", assunto = "Apresentação"),
                    MeetingSpeaker(nome = "Bispo Azevedo", assunto = "Mensagem de Natal")
                )
            )
        )
    )
    return templates.mapIndexed { index, seed ->
        val createdAt = now.minusSeconds(index * 86_400L).toString()
        MeetingEntity(
            date = baseDate.minusWeeks(index.toLong()).toString(),
            title = seed.title,
            detailsJson = adapter.encode(seed.details),
            createdAt = createdAt,
            updatedAt = createdAt,
            syncVersion = 0L
        )
    }
}

private data class MeetingSeed(
    val title: String,
    val details: MeetingDetails
)
