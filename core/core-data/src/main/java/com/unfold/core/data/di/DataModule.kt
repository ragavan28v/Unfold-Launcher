package com.unfold.core.data.di

import android.content.Context
import com.unfold.core.data.local.AppDatabase
import com.unfold.core.data.local.dao.*
import com.unfold.core.data.repositoryimpl.AppRepositoryImpl
import com.unfold.core.data.repositoryimpl.FolderRepositoryImpl
import com.unfold.core.data.repositoryimpl.GestureRepositoryImpl
import com.unfold.core.data.repositoryimpl.SystemStatsRepositoryImpl
import com.unfold.core.data.repositoryimpl.ThemeRepositoryImpl
import com.unfold.core.domain.repository.AppRepository
import com.unfold.core.domain.repository.FolderRepository
import com.unfold.core.domain.repository.GestureRepository
import com.unfold.core.domain.repository.SystemStatsRepository
import com.unfold.core.domain.repository.ThemeRepository
import com.unfold.core.data.repositoryimpl.TimelineRepositoryImpl
import com.unfold.core.domain.repository.TimelineRepository
import com.unfold.core.data.repositoryimpl.NoteRepositoryImpl
import com.unfold.core.domain.repository.NoteRepository
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
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindGestureRepository(impl: GestureRepositoryImpl): GestureRepository

    @Binds
    @Singleton
    abstract fun bindSystemStatsRepository(impl: SystemStatsRepositoryImpl): SystemStatsRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindTimelineRepository(impl: TimelineRepositoryImpl): TimelineRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

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

        @Provides
        fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()
    }
}

