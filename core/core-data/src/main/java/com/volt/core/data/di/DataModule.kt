package com.volt.core.data.di

import android.content.Context
import com.volt.core.data.local.AppDatabase
import com.volt.core.data.local.dao.*
import com.volt.core.data.repositoryimpl.AppRepositoryImpl
import com.volt.core.data.repositoryimpl.GestureRepositoryImpl
import com.volt.core.data.repositoryimpl.SystemStatsRepositoryImpl
import com.volt.core.data.repositoryimpl.ThemeRepositoryImpl
import com.volt.core.domain.repository.AppRepository
import com.volt.core.domain.repository.GestureRepository
import com.volt.core.domain.repository.SystemStatsRepository
import com.volt.core.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindGestureRepository(impl: GestureRepositoryImpl): GestureRepository

    @Binds
    @Singleton
    abstract fun bindSystemStatsRepository(impl: SystemStatsRepositoryImpl): SystemStatsRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.getInstance(context)
        }

        @Provides
        fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

        @Provides
        fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

        @Provides
        fun provideGestureDao(database: AppDatabase): GestureDao = database.gestureDao()

        @Provides
        fun provideHiddenFileDao(database: AppDatabase): HiddenFileDao = database.hiddenFileDao()

        @Provides
        fun provideLayoutSnapshotDao(database: AppDatabase): LayoutSnapshotDao = database.layoutSnapshotDao()
    }
}
