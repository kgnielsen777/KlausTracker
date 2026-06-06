package com.klaustracker.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerDatabaseMigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_addsMotionStateColumn() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS capture_points (
                    id TEXT NOT NULL PRIMARY KEY,
                    timestamp_utc TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    accuracy_meters REAL NOT NULL,
                    speed_kmh REAL,
                    source TEXT NOT NULL,
                    enrichment_status TEXT NOT NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            TrackerDatabase.MIGRATION_1_2,
        )

        migratedDb.query("PRAGMA table_info(capture_points)").use { cursor ->
            var hasMotionState = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName == "motion_state") {
                    hasMotionState = true
                    break
                }
            }
            check(hasMotionState) { "Expected motion_state column after migration" }
        }
        migratedDb.close()
    }

    @Test
    fun migrate2To3_addsPlaceSuggestionsTable() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS places (
                    id TEXT NOT NULL PRIMARY KEY,
                    canonical_name TEXT NOT NULL,
                    label_type TEXT NOT NULL,
                    custom_label TEXT,
                    default_address TEXT,
                    centroid_lat REAL NOT NULL,
                    centroid_lng REAL NOT NULL,
                    active INTEGER NOT NULL,
                    created_utc TEXT NOT NULL,
                    updated_utc TEXT NOT NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            3,
            true,
            TrackerDatabase.MIGRATION_2_3,
        )

        migratedDb.query("PRAGMA table_info(place_suggestions)").use { cursor ->
            var hasPlaceId = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName == "place_id") {
                    hasPlaceId = true
                    break
                }
            }
            check(hasPlaceId) { "Expected place_suggestions table with place_id column after migration" }
        }
        migratedDb.close()
    }
}
