package com.klaustracker.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.klaustracker.app.data.local.dao.BackupManifestDao
import com.klaustracker.app.data.local.dao.CapturePointDao
import com.klaustracker.app.data.local.dao.EnrichmentDao
import com.klaustracker.app.data.local.dao.PlaceDao
import com.klaustracker.app.data.local.dao.PlaceSuggestionDao
import com.klaustracker.app.data.local.dao.StaySegmentDao
import com.klaustracker.app.data.local.dao.VisitDao
import com.klaustracker.app.data.local.entity.BackupManifestEntity
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.EnrichmentEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.entity.PlaceSuggestionEntity
import com.klaustracker.app.data.local.entity.StaySegmentEntity
import com.klaustracker.app.data.local.entity.VisitEntity

@Database(
    entities = [
        CapturePointEntity::class,
        EnrichmentEntity::class,
        StaySegmentEntity::class,
        PlaceEntity::class,
        PlaceSuggestionEntity::class,
        VisitEntity::class,
        BackupManifestEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun capturePointDao(): CapturePointDao
    abstract fun enrichmentDao(): EnrichmentDao
    abstract fun staySegmentDao(): StaySegmentDao
    abstract fun placeDao(): PlaceDao
    abstract fun placeSuggestionDao(): PlaceSuggestionDao
    abstract fun visitDao(): VisitDao
    abstract fun backupManifestDao(): BackupManifestDao

    companion object {
        const val DB_NAME = "klaus_tracker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // V2 introduces motion_state; default unknown for legacy rows.
                db.execSQL(
                    "ALTER TABLE capture_points ADD COLUMN motion_state TEXT NOT NULL DEFAULT 'unknown'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS place_suggestions (
                        id TEXT NOT NULL PRIMARY KEY,
                        place_id TEXT NOT NULL,
                        suggested_label_type TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        status TEXT NOT NULL,
                        created_utc TEXT NOT NULL,
                        updated_utc TEXT NOT NULL,
                        FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_place_suggestions_place_id ON place_suggestions(place_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_place_suggestions_status ON place_suggestions(status)")
            }
        }

        @Volatile
        private var instance: TrackerDatabase? = null

        fun getInstance(context: Context): TrackerDatabase {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val initial = buildDatabase(appContext)
                    val verified = try {
                        initial.openHelper.writableDatabase
                        initial
                    } catch (_: SQLiteException) {
                        initial.close()
                        appContext.deleteDatabase(DB_NAME)
                        val recreated = buildDatabase(appContext)
                        recreated.openHelper.writableDatabase
                        recreated
                    }
                    verified.also { instance = it }
                }
            }
        }

        private fun buildDatabase(context: Context): TrackerDatabase {
            return Room.databaseBuilder(
                context,
                TrackerDatabase::class.java,
                DB_NAME,
            )
                .openHelperFactory(DatabaseEncryption.supportFactory(context))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
