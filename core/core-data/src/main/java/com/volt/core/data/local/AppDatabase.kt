package com.volt.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.volt.core.data.local.dao.*
import com.volt.core.data.local.entity.*
import com.volt.core.domain.navigation.VoltRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppEntity::class,
        FolderEntity::class,
        GestureEntity::class,
        HiddenFileEntity::class,
        LayoutSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun folderDao(): FolderDao
    abstract fun gestureDao(): GestureDao
    abstract fun hiddenFileDao(): HiddenFileDao
    abstract fun layoutSnapshotDao(): LayoutSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "volt_launcher_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                             val defaultGestures = listOf(
                                 GestureEntity("SWIPE_LEFT_1F", "OPEN_INTENT", targetIntentUri = "tel:"),
                                 GestureEntity("SWIPE_RIGHT_1F", "LAUNCH_APP", targetPackage = "com.whatsapp"),
                                 GestureEntity("SWIPE_LEFT_2F", "OPEN_SCREEN", targetScreenRoute = VoltRoute.HiddenSpace.route),
                                 GestureEntity("SWIPE_RIGHT_2F", "OPEN_INTENT", targetIntentUri = "market://details?id="),
                                 GestureEntity("SWIPE_DOWN_1F", "OPEN_SCREEN", targetScreenRoute = VoltRoute.UniversalSearch.route),
                                 GestureEntity("SWIPE_UP_1F", "OPEN_SCREEN", targetScreenRoute = VoltRoute.AppDrawer.route)
                             )
                             getInstance(context).gestureDao().insertAll(defaultGestures)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
