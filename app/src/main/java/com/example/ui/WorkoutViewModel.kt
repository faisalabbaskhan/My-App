package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Workout
import com.example.data.BodyWeight
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern ViewModel holding and preparing UI states for our FitTrack Pro tracker.
 * It manages quotes, searches, daily records, and historical statistics.
 */
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository
    
    init {
        val database = WorkoutDatabase.getDatabase(application)
        repository = WorkoutRepository(database.workoutDao())
    }

    // Persistent storage for user profile details
    private val sharedPrefs = application.getSharedPreferences("fit_track_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", "Champion") ?: "Champion")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userContact = MutableStateFlow(sharedPrefs.getString("user_contact", "+1 (555) 019-2834") ?: "+1 (555) 019-2834")
    val userContact: StateFlow<String> = _userContact.asStateFlow()

    private val _userUsername = MutableStateFlow(sharedPrefs.getString("user_username", "champion_active") ?: "champion_active")
    val userUsername: StateFlow<String> = _userUsername.asStateFlow()

    // Theme options: 0 = System, 1 = Light, 2 = Dark
    private val _themeMode = MutableStateFlow(sharedPrefs.getInt("theme_mode", 0))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    // Profile picture: null (uses Initials), or "avatar_runner", "avatar_captain", "avatar_yoga", "avatar_iron" or custom Uri string
    private val _userProfilePicture = MutableStateFlow(sharedPrefs.getString("user_profile_picture", null))
    val userProfilePicture: StateFlow<String?> = _userProfilePicture.asStateFlow()

    // Additional Settings & Goals
    private val _waterGoal = MutableStateFlow(sharedPrefs.getInt("water_goal", 2500))
    val waterGoal: StateFlow<Int> = _waterGoal.asStateFlow()

    private val _waterIntakeToday = MutableStateFlow(sharedPrefs.getInt("water_intake_today", 0))
    val waterIntakeToday: StateFlow<Int> = _waterIntakeToday.asStateFlow()

    private val _stepGoal = MutableStateFlow(sharedPrefs.getInt("step_goal", 10000))
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _stepsToday = MutableStateFlow(sharedPrefs.getInt("steps_today", 0))
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    private val _restTimerDefault = MutableStateFlow(sharedPrefs.getInt("rest_timer_default", 90))
    val restTimerDefault: StateFlow<Int> = _restTimerDefault.asStateFlow()

    private val _measurementUnit = MutableStateFlow(sharedPrefs.getInt("measurement_unit", 0)) // 0: Metric, 1: Imperial
    val measurementUnit: StateFlow<Int> = _measurementUnit.asStateFlow()

    fun updateProfile(name: String, contact: String, username: String) {
        sharedPrefs.edit().apply {
            putString("user_name", name.trim())
            putString("user_contact", contact.trim())
            putString("user_username", username.trim())
            apply()
        }
        _userName.value = name.trim().ifEmpty { "Champion" }
        _userContact.value = contact.trim().ifEmpty { "+1 (555) 019-2834" }
        _userUsername.value = username.trim().ifEmpty { "champion_active" }
    }

    fun updateThemeMode(mode: Int) {
        sharedPrefs.edit().putInt("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun updateProfilePicture(uriOrId: String?) {
        sharedPrefs.edit().putString("user_profile_picture", uriOrId).apply()
        _userProfilePicture.value = uriOrId
    }

    fun updateWaterGoal(goal: Int) {
        sharedPrefs.edit().putInt("water_goal", goal).apply()
        _waterGoal.value = goal
    }

    fun addWaterIntake(amountMl: Int) {
        val newVal = _waterIntakeToday.value + amountMl
        sharedPrefs.edit().putInt("water_intake_today", newVal).apply()
        _waterIntakeToday.value = newVal
    }

    fun resetWaterIntake() {
        sharedPrefs.edit().putInt("water_intake_today", 0).apply()
        _waterIntakeToday.value = 0
    }

    fun updateStepGoal(goal: Int) {
        sharedPrefs.edit().putInt("step_goal", goal).apply()
        _stepGoal.value = goal
    }

    fun addSteps(steps: Int) {
        val newVal = _stepsToday.value + steps
        sharedPrefs.edit().putInt("steps_today", newVal).apply()
        _stepsToday.value = newVal
    }

    fun resetSteps() {
        sharedPrefs.edit().putInt("steps_today", 0).apply()
        _stepsToday.value = 0
    }

    fun updateRestTimer(seconds: Int) {
        sharedPrefs.edit().putInt("rest_timer_default", seconds).apply()
        _restTimerDefault.value = seconds
    }

    fun updateMeasurementUnit(unit: Int) {
        sharedPrefs.edit().putInt("measurement_unit", unit).apply()
        _measurementUnit.value = unit
    }

    // Already feeded preset list of exercises
    val presetExercises = listOf(
        "Bench Press",
        "Squat",
        "Deadlift",
        "Overhead Press",
        "Barbell Row",
        "Dumbbell Bicep Curl",
        "Incline Dumbbell Press",
        "Leg Press",
        "Pull-ups",
        "Push-ups",
        "Lateral Raise",
        "Tricep Pushdown",
        "Lying Leg Curl",
        "Plank",
        "Lunges"
    )

    // List of motivational quotes for our dashboard
    private val motivationalQuotes = listOf(
        "The only bad workout is the one that didn't happen.",
        "Success isn't always about greatness. It's about consistency.",
        "Your body can stand almost anything. It's your mind that you have to convince.",
        "What hurts today makes you stronger tomorrow.",
        "Action is the foundational key to all fitness success.",
        "You don't have to be extreme, just consistent.",
        "Don't limit your challenges. Challenge your limits.",
        "The pain you feel today will be the strength you feel tomorrow.",
        "Believe you can and you're halfway there.",
        "Energy flows where attention goes. Focus on your strength today!",
        "Definition of a great workout: when you hated starting, but loved finishing!"
    )

    // Current motivational quote
    private val _currentQuote = MutableStateFlow(motivationalQuotes.first())
    val currentQuote: StateFlow<String> = _currentQuote.asStateFlow()

    fun rotateQuote() {
        val nextQuote = motivationalQuotes.filter { it != _currentQuote.value }.random()
        _currentQuote.value = nextQuote
    }

    // Reactively observe all workouts from database
    val allWorkouts: StateFlow<List<Workout>> = repository.allWorkouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactively observe all body weight records from database
    val allBodyWeights: StateFlow<List<BodyWeight>> = repository.allBodyWeights
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addBodyWeight(weightValue: Double, dateMs: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertBodyWeight(BodyWeight(weight = weightValue, date = dateMs))
        }
    }

    fun deleteBodyWeightById(id: Int) {
        viewModelScope.launch {
            repository.deleteBodyWeightById(id)
        }
    }

    // Reactively observe today's workouts
    val todaysWorkouts: StateFlow<List<Workout>> = repository.getWorkoutsForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search queries for Workout History
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered historical workouts matching search query
    val filteredWorkouts: StateFlow<List<Workout>> = combine(allWorkouts, _searchQuery) { workouts, query ->
        if (query.isBlank()) {
            workouts
        } else {
            workouts.filter {
                it.exerciseName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Completion percentage for today's workouts
    val todaysCompletionPercentage: StateFlow<Float> = todaysWorkouts
        .map { workouts ->
            if (workouts.isEmpty()) 0f
            else {
                val completed = workouts.count { it.isCompleted }
                completed.toFloat() / workouts.size.toFloat()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    // Total Completed Exercises historically
    val totalCompletedExercises: StateFlow<Int> = allWorkouts
        .map { workouts ->
            workouts.count { it.isCompleted }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Structure representing progress on a specific day for chart rendering.
     */
    data class DayProgress(
        val dayLabel: String,
        val completedCount: Int,
        val dateMillis: Long
    )

    // Weekly progress chart metrics (last 7 days completed exercises)
    val weeklyChartData: StateFlow<List<DayProgress>> = allWorkouts
        .map { workouts ->
            val calendar = Calendar.getInstance()
            val result = mutableListOf<DayProgress>()
            
            // Format for day labels (e.g. "Mon", "Tue")
            val dayFormat = SimpleDateFormat("E", Locale.getDefault())

            // Go back 7 days
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                
                // Get bounds for this calendar day
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfDay = cal.timeInMillis

                // Count completed workouts matching this specific scope
                val count = workouts.count { workout ->
                    workout.isCompleted && workout.date in startOfDay..endOfDay
                }

                val label = dayFormat.format(cal.time)
                result.add(DayProgress(label, count, startOfDay))
            }
            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Monthly analytics metrics block
     */
    data class MonthlyStats(
        val workoutsCount: Int,
        val completionRate: Float,
        val totalWeightLifted: Double,
        val totalActiveDurationMins: Int
    )

    val currentMonthStats: StateFlow<MonthlyStats> = allWorkouts
        .map { workouts ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            val monthlyWorkouts = workouts.filter { it.date >= startOfMonth }
            
            val total = monthlyWorkouts.size
            val completed = monthlyWorkouts.count { it.isCompleted }
            val rate = if (total == 0) 0f else (completed.toFloat() / total.toFloat())
            
            val totalWeight = monthlyWorkouts
                .filter { it.isCompleted }
                .sumOf { (it.weight ?: 0.0) * it.sets * it.reps } // Total Volume (Sets * Reps * Weight)
                
            val totalDuration = monthlyWorkouts
                .filter { it.isCompleted }
                .sumOf { it.duration ?: 0 }

            MonthlyStats(
                workoutsCount = total,
                completionRate = rate,
                totalWeightLifted = totalWeight,
                totalActiveDurationMins = totalDuration
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyStats(0, 0f, 0.0, 0)
        )

    /**
     * Database Operations (Executed on Dispatcher IO under-the-hood via Room implementation)
     */
    fun addWorkout(
        exerciseName: String,
        sets: Int,
        reps: Int,
        weight: Double?,
        duration: Int?,
        date: Long = System.currentTimeMillis(),
        isCompleted: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertWorkout(
                Workout(
                    exerciseName = exerciseName.trim(),
                    sets = sets,
                    reps = reps,
                    weight = weight,
                    duration = duration,
                    date = date,
                    isCompleted = isCompleted
                )
            )
        }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            repository.deleteWorkout(workout)
        }
    }

    fun deleteWorkoutById(id: Int) {
        viewModelScope.launch {
            repository.deleteWorkoutById(id)
        }
    }

    fun toggleWorkoutCompletion(workout: Workout) {
        viewModelScope.launch {
            repository.updateWorkoutCompletion(workout.id, !workout.isCompleted)
        }
    }
}

/**
 * Standard factory creator for clean ViewModel injection.
 */
class WorkoutViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
