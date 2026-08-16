package com.unfold.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unfold.core.data.local.dao.*
import com.unfold.core.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppEntity::class,
        FolderEntity::class,
        GestureEntity::class,
        HiddenFileEntity::class,
        LayoutSnapshotEntity::class,
        NoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun folderDao(): FolderDao
    abstract fun gestureDao(): GestureDao
    abstract fun hiddenFileDao(): HiddenFileDao
    abstract fun layoutSnapshotDao(): LayoutSnapshotDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance: AppDatabase? = null
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unfold_launcher_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        AppDatabaseSeedData.seedDatabase(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance!!
            }
        }
    }
}


