package com.klaustracker.app.data.local

import androidx.room.Room
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

        helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            TrackerDatabase.MIGRATION_1_2,
        )

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TrackerDatabase::class.java,
            dbName,
        )
            .addMigrations(TrackerDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        db.openHelper.writableDatabase.query("PRAGMA table_info(capture_points)").use { cursor ->
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
        db.close()
    }
}
