package com.drummer.speed.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drummer.speed.data.model.SessionResult

@Database(
    entities = [SessionResult::class], 
    version = 1, 
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drum_counter_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = false) // Safely handle structural changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
