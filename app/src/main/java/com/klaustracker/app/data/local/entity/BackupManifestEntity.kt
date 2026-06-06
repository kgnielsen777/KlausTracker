package com.klaustracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_manifests")
data class BackupManifestEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "backup_version") val backupVersion: Int,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "created_utc") val createdUtc: String,
    @ColumnInfo(name = "snapshot_blob_path") val snapshotBlobPath: String,
    @ColumnInfo(name = "checksum_sha256") val checksumSha256: String,
    val encrypted: Boolean,
    @ColumnInfo(name = "app_version") val appVersion: String,
)