package com.whereami.locus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackPointEntity::class, PlaceLabelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocusDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun placeLabelDao(): PlaceLabelDao

    companion object {
        @Volatile
        private var instance: LocusDatabase? = null

        fun get(context: Context): LocusDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocusDatabase::class.java,
                    "locus.db"
                ).build().also { instance = it }
            }
        }
    }
}
