package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository pattern implementation to abstract access to the Database.
 * Allows easy swapping of data sources in the future.
 */
class WorkoutRepository(private val workoutDao: WorkoutDao) {

    // Expose all workouts as a reactive Flow
    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts()

    // Retrieve workouts recorded specifically today
    fun getWorkoutsForToday(): Flow<List<Workout>> {
        val calendar = Calendar.getInstance()
        
        // Start of today (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        // End of today (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        return workoutDao.getWorkoutsInDateRange(startOfToday, endOfToday)
    }

    suspend fun insertWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout)
    }

    suspend fun deleteWorkout(workout: Workout) {
        workoutDao.deleteWorkout(workout)
    }

    suspend fun deleteWorkoutById(id: Int) {
        workoutDao.deleteWorkoutById(id)
    }

    suspend fun updateWorkoutCompletion(id: Int, isCompleted: Boolean) {
        workoutDao.updateWorkoutCompletion(id, isCompleted)
    }

    // --- Body Weight Records ---
    val allBodyWeights: Flow<List<BodyWeight>> = workoutDao.getAllBodyWeights()

    suspend fun insertBodyWeight(bodyWeight: BodyWeight): Long {
        return workoutDao.insertBodyWeight(bodyWeight)
    }

    suspend fun deleteBodyWeight(bodyWeight: BodyWeight) {
        workoutDao.deleteBodyWeight(bodyWeight)
    }

    suspend fun deleteBodyWeightById(id: Int) {
        workoutDao.deleteBodyWeightById(id)
    }
}
