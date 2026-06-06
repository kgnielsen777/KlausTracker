package com.klaustracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.klaustracker.app.data.local.dao.BackupManifestDao
import com.klaustracker.app.data.local.dao.CapturePointDao
import com.klaustracker.app.data.local.dao.EnrichmentDao
import com.klaustracker.app.data.local.dao.PlaceDao
import com.klaustracker.app.data.local.dao.StaySegmentDao
import com.klaustracker.app.data.local.dao.VisitDao
import com.klaustracker.app.data.local.entity.BackupManifestEntity
import com.klaustracker.app.data.local.entity.CapturePointEntity
import com.klaustracker.app.data.local.entity.EnrichmentEntity
import com.klaustracker.app.data.local.entity.PlaceEntity
import com.klaustracker.app.data.local.entity.StaySegmentEntity
import com.klaustracker.app.data.local.entity.VisitEntity

@Database(
    entities = [
        CapturePointEntity::class,
        EnrichmentEntity::class,
        StaySegmentEntity::class,
        PlaceEntity::class,
        VisitEntity::class,
        BackupManifestEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun capturePointDao(): CapturePointDao
    abstract fun enrichmentDao(): EnrichmentDao
    abstract fun staySegmentDao(): StaySegmentDao
    abstract fun placeDao(): PlaceDao
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

        @Volatile
        private var instance: TrackerDatabase? = null

        fun getInstance(context: Context): TrackerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
