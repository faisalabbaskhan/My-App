package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BodyWeight represents a single recorded body weight measurement.
 */
@Entity(tableName = "body_weights")
data class BodyWeight(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val weight: Double,
    
    // Unix timestamp in milliseconds
    val date: Long
)
