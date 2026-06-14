package com.ahsas.pixqr

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val rawContent: String,
    val cleanedContent: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)