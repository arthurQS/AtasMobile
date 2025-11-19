package br.com.atas.mobile.core.drive.di

import br.com.atas.mobile.core.drive.api.DriveBackupCoordinator
import br.com.atas.mobile.core.drive.impl.DriveDocumentBackupCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveModule {
    @Binds
    @Singleton
    abstract fun bindDriveCoordinator(
        impl: DriveDocumentBackupCoordinator
    ): DriveBackupCoordinator
}
