package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Workout represents a single exercise set/entry in our database.
 * This is a Room Entity, which maps directly to a table in SQLite.
 */
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val exerciseName: String,
    
    val sets: Int,
    
    val reps: Int,
    
    // Weight in kg or lbs; null if bodyweight or not applicable
    val weight: Double? = null,
    
    // Duration of exercise in minutes; null if not timed
    val duration: Int? = null,
    
    // Date of the workout stored as a Unix timestamp (milliseconds since epoch)
    val date: Long,
    
    // Whether the exercise/set was successfully completed
    val isCompleted: Boolean = false
)
