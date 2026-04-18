package com.example.mynote.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "notes")
@Parcelize
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String, // Added missing comma
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val timestamp: Long,
    var isCompleted: Boolean = false,
    val location: String? = null,
    val reminderAt: Long? = null,
    val reminderPhone: String? = null
) : Parcelable