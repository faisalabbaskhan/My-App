package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Workout entity.
 * Defines the SQL operations used to interact with the Room database.
 */
@Dao
interface WorkoutDao {

    // Retrieve all workouts, newest first
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    // Retrieve workouts for a specific day/timestamp range
    @Query("SELECT * FROM workouts WHERE date >= :startOfDay AND date <= :endOfDays ORDER BY date DESC")
    fun getWorkoutsInDateRange(startOfDay: Long, endOfDays: Long): Flow<List<Workout>>

    // Insert or update a workout. If the ID exists, it gets replaced (updated)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    // Delete a workout record
    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Int)

    // Quick toggle for completed status
    @Query("UPDATE workouts SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateWorkoutCompletion(id: Int, isCompleted: Boolean)

    // --- Body Weight Database Operations ---
    @Query("SELECT * FROM body_weights ORDER BY date ASC")
    fun getAllBodyWeights(): Flow<List<BodyWeight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(bodyWeight: BodyWeight): Long

    @Delete
    suspend fun deleteBodyWeight(bodyWeight: BodyWeight)

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun deleteBodyWeightById(id: Int)
}
