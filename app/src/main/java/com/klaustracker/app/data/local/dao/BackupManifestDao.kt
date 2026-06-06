package com.klaustracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klaustracker.app.data.local.entity.BackupManifestEntity

@Dao
interface BackupManifestDao {
    @Upsert
    suspend fun upsert(manifest: BackupManifestEntity)

    @Query("SELECT * FROM backup_manifests ORDER BY created_utc DESC LIMIT 1")
    suspend fun latest(): BackupManifestEntity?
}
