package br.com.atas.mobile.di

import br.com.atas.mobile.core.data.repository.SyncRepository
import br.com.atas.mobile.core.sync.FirebaseSyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @Provides
    @Singleton
    fun provideSyncRepository(): SyncRepository {
        return FirebaseSyncRepository.createDefault()
    }
}
