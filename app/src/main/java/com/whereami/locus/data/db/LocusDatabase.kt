package com.whereami.locus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whereami.locus.data.security.DatabaseKeyStore
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SupportFactory
import java.io.File

@Database(
    entities = [TrackPointEntity::class, PlaceLabelEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LocusDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun placeLabelDao(): PlaceLabelDao

    companion object {
        private const val ENCRYPTED_DB_NAME = "locus_secure.db"
        private const val LEGACY_DB_NAME = "locus.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE track_points ADD COLUMN uploadedAt INTEGER")
            }
        }

        @Volatile
        private var instance: LocusDatabase? = null

        fun get(context: Context): LocusDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): LocusDatabase {
            migrateLegacyIfNeeded(context)
            val passphrase = DatabaseKeyStore(context).getOrCreatePassphrase()
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                LocusDatabase::class.java,
                ENCRYPTED_DB_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private fun migrateLegacyIfNeeded(context: Context) {
            val legacyFile = context.getDatabasePath(LEGACY_DB_NAME)
            val encryptedFile = context.getDatabasePath(ENCRYPTED_DB_NAME)
            if (!legacyFile.exists() || encryptedFile.exists()) return

            val passphrase = DatabaseKeyStore(context).getOrCreatePassphrase()
            val encrypted = Room.databaseBuilder(
                context,
                LocusDatabase::class.java,
                ENCRYPTED_DB_NAME
            )
                .openHelperFactory(SupportFactory(passphrase.copyOf()))
                .addMigrations(MIGRATION_1_2)
                .build()

            val legacy = Room.databaseBuilder(
                context,
                LocusDatabase::class.java,
                LEGACY_DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()

            var migrated = false
            try {
                runBlocking {
                    val points = legacy.trackPointDao().getAll()
                    val labels = legacy.placeLabelDao().getAll()
                    points.forEach { point ->
                        encrypted.trackPointDao().insert(point.copy(id = 0L))
                    }
                    labels.forEach { label ->
                        encrypted.placeLabelDao().upsert(label.copy(id = 0L))
                    }
                    val copiedPoints = encrypted.trackPointDao().getAll().size
                    val copiedLabels = encrypted.placeLabelDao().getAll().size
                    check(copiedPoints == points.size && copiedLabels == labels.size) {
                        "Trail migration count mismatch"
                    }
                    migrated = true
                }
            } finally {
                legacy.close()
                encrypted.close()
            }

            if (migrated) {
                deleteDbFiles(legacyFile)
            } else {
                deleteDbFiles(encryptedFile)
                error("Failed to migrate trail database; plaintext DB kept for retry")
            }
        }

        private fun deleteDbFiles(primary: File) {
            primary.delete()
            File(primary.path + "-shm").delete()
            File(primary.path + "-wal").delete()
            File(primary.path + "-journal").delete()
        }
    }
}
