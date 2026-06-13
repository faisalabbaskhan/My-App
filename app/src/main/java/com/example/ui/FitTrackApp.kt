package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Workout
import com.example.data.BodyWeight
import com.example.ui.theme.AthleticBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom Tabs/Screens navigation configuration.
 */
enum class AppScreen(val route: String, val title: String, val iconSelected: androidx.compose.ui.graphics.vector.ImageVector, val iconUnselected: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    AI_CHATBOT("ai_chatbot", "AI Chatbot", Icons.Filled.Forum, Icons.Outlined.Forum),
    PROGRESS("progress", "Progress", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTrackApp(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    // Keep track of which screen tab is currently active
    var currentTab by remember { mutableStateOf(AppScreen.DASHBOARD) }
    
    // Dialog visibility for logging a new workout
    var showAddWorkoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("app_navigation_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp
            ) {
                AppScreen.values().forEach { screen ->
                    val isSelected = currentTab == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.iconSelected else screen.iconUnselected,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == AppScreen.DASHBOARD || currentTab == AppScreen.HISTORY) {
                FloatingActionButton(
                    onClick = { showAddWorkoutDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("add_workout_fab")
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Log Exercise",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            // Screen switching with slide animations
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onAddWorkoutClick = { showAddWorkoutDialog = true }
                    )
                    AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppScreen.AI_CHATBOT -> ChatbotScreen(workoutViewModel = viewModel)
                    AppScreen.PROGRESS -> ProgressScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Workout Log Dialog Overlay
        if (showAddWorkoutDialog) {
            AddWorkoutDialog(
                onDismiss = { showAddWorkoutDialog = false },
                onSave = { name, sets, reps, weight, duration, date, completed ->
                    viewModel.addWorkout(
                        exerciseName = name,
                        sets = sets,
                        reps = reps,
                        weight = weight,
                        duration = duration,
                        date = date,
                        isCompleted = completed
                    )
                    showAddWorkoutDialog = false
                }
            )
        }
    }
}

/**
 * DASHBOARD SCREEN - Overview of stats, quotes, and today's exercises.
 */
@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    onAddWorkoutClick: () -> Unit
) {
    val quote by viewModel.currentQuote.collectAsStateWithLifecycle()
    val todaysProgress by viewModel.todaysCompletionPercentage.collectAsStateWithLifecycle()
    val todaysWorkouts by viewModel.todaysWorkouts.collectAsStateWithLifecycle()
    val allWorkouts by viewModel.allWorkouts.collectAsStateWithLifecycle()
    val totalWorkoutsCount = allWorkouts.size
    val userNameState by viewModel.userName.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        ProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HELLO, ${userNameState.uppercase()}!",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = ElectricBlue,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.gymora_logo),
                        contentDescription = "Gymora Logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.Black)
                            .border(1.5.dp, ElectricBlue, CircleShape)
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gymora",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = (-0.5).sp,
                        color = ElectricBlue
                    )
                }
            }
            
            // Decorative sporty profile/tag icon
            val userProfilePic by viewModel.userProfilePicture.collectAsStateWithLifecycle()
            ProfileImageBubble(
                profilePic = userProfilePic,
                userName = userNameState,
                size = 42.dp,
                onClick = { showProfileDialog = true }
            )
        }

        // Motivational Quote Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("motivation_quote_card")
                .clickable { viewModel.rotateQuote() }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(20.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ElectricBlue.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                // Vertical Cobalt side bar indicator (border-l-2 border-blue-500)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(color = ElectricBlue)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatQuote,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "MOTIVATION",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = ElectricBlue
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "New Quote",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Tap block to rotate quote",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Daily Stat Highlights Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Block 1: Overview
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = AthleticBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "$totalWorkoutsCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Stats Block 2: Interactive Completion percentage ring
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            val percentText = (todaysProgress * 100).toInt()
                            Text(
                                text = "$percentText%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Today's Goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Circular Progress Rings using Canvas
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progressColor = if (todaysProgress >= 1f) NeonGreen else MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.size(50.dp)) {
                            // Background Ring
                            drawCircle(
                                color = progressColor.copy(alpha = 0.15f),
                                style = Stroke(width = 6.dp.toPx())
                            )
                            // Foreground Active Progress
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = todaysProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = if (todaysProgress >= 1f) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Today's Checklist header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TODAY'S WORKOUTS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            val countCompleted = todaysWorkouts.count { it.isCompleted }
            Text(
                text = "$countCompleted/${todaysWorkouts.size} Completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Today's Workouts List Space
        if (todaysWorkouts.isEmpty()) {
            EmptyStatePlaceholder(
                title = "No exercises logged today",
                subtitle = "Consistency is key. Log your first exercise of the day to visualizes progress!",
                icon = Icons.Filled.AddReaction,
                actionString = "Log Warmup",
                onClick = onAddWorkoutClick
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                todaysWorkouts.forEach { workout ->
                    WorkoutItemRow(
                        workout = workout,
                        onToggleCompletion = { viewModel.toggleWorkoutCompletion(workout) },
                        onDelete = { viewModel.deleteWorkout(workout) },
                        modifier = Modifier.testTag("workout_item_today_${workout.id}")
                    )
                }
            }
        }
    }
}

/**
 * HISTORY SCREEN - Full workout audit, search fields, and clear management tools.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredWorkouts by viewModel.filteredWorkouts.collectAsStateWithLifecycle()
    val controller = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WORKOUT HISTORY",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )

        // Search Text Box with leading search and trailing clear icon
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search exercise name...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.updateSearchQuery("") },
                        modifier = Modifier.testTag("search_clear_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_workout_text_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        // Workouts History List
        if (filteredWorkouts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "No workouts recorded yet" else "No matching exercises",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "Log a workout down below to start building your visual calendar map." else "Check spelling or input a wider keyword query.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_workouts_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredWorkouts,
                    key = { it.id }
                ) { workout ->
                    WorkoutItemRow(
                        workout = workout,
                        onToggleCompletion = { viewModel.toggleWorkoutCompletion(workout) },
                        onDelete = { viewModel.deleteWorkout(workout) },
                        showDate = true,
                        modifier = Modifier.testTag("workout_item_history_${workout.id}")
                    )
                }
            }
        }
    }
}

/**
 * PROGRESS SCREEN - Elegant customizable charts and statistics overview.
 */
@Composable
fun ProgressScreen(
    viewModel: WorkoutViewModel
) {
    val context = LocalContext.current
    val totalDone by viewModel.totalCompletedExercises.collectAsStateWithLifecycle()
    val weeklyChartData by viewModel.weeklyChartData.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.currentMonthStats.collectAsStateWithLifecycle()
    val allWorkouts by viewModel.allWorkouts.collectAsStateWithLifecycle()

    var showDaySummaryDialog by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val selectedWorkoutsForDay = remember(selectedDateMillis, allWorkouts) {
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
        }
        val targetCal = Calendar.getInstance()
        allWorkouts.filter {
            targetCal.timeInMillis = it.date
            targetCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
        }
    }

    if (showDaySummaryDialog) {
        DaySummaryDialog(
            dateMillis = selectedDateMillis,
            workouts = selectedWorkoutsForDay,
            onDismiss = { showDaySummaryDialog = false },
            onToggleCompletion = { viewModel.toggleWorkoutCompletion(it) },
            onDelete = { viewModel.deleteWorkout(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PROGRESS TRACKING",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )

        // General highlights
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column {
                    Text(
                        text = "Lifetime Completed Sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$totalDone exercises finished",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Your body reacts to everyday habits. Lock in success!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // WEEKLY PROGRESS CHART CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weekly_chart_card")
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly completed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed sets per day for last 7 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(
                        onClick = {
                            val activeCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                            }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    selectedDateMillis = selectedCal.timeInMillis
                                    showDaySummaryDialog = true
                                },
                                activeCal.get(Calendar.YEAR),
                                activeCal.get(Calendar.MONTH),
                                activeCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("progress_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Inspect Calendar Day Workouts",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Custom Canvas-drawn chart
                WeeklyChartDisplay(
                    chartData = weeklyChartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 10.dp)
                )
            }
        }

        // MONTHLY STATISTICS BLOCK
        Text(
            text = "MONTHLY STAT PACK",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        )

        // Statistics Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMicroCard(
                    title = "Monthly Logs",
                    value = "${monthlyStats.workoutsCount}",
                    desc = "Total items logged",
                    icon = Icons.Outlined.AddAlert,
                    iconColor = AthleticBlue,
                    modifier = Modifier.weight(1f)
                )
                StatMicroCard(
                    title = "Completion rate",
                    value = "${(monthlyStats.completionRate * 100).toInt()}%",
                    desc = "Completed vs logged",
                    icon = Icons.Outlined.CheckCircle,
                    iconColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val formattedWeight = if (monthlyStats.totalWeightLifted > 1000) {
                    String.format("%.1f k", monthlyStats.totalWeightLifted / 1000.0)
                } else {
                    String.format("%.0f", monthlyStats.totalWeightLifted)
                }
                StatMicroCard(
                    title = "Estimated Vol",
                    value = "$formattedWeight units",
                    desc = "Sets × Reps × Weight",
                    icon = Icons.Outlined.MonitorWeight,
                    iconColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                StatMicroCard(
                    title = "Active time",
                    value = "${monthlyStats.totalActiveDurationMins}m",
                    desc = "Total workout mins",
                    icon = Icons.Outlined.Timer,
                    iconColor = Color.Yellow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Custom bar chart drawing element using rich Canvas API.
 */
@Composable
fun WeeklyChartDisplay(
    chartData: List<WorkoutViewModel.DayProgress>,
    modifier: Modifier = Modifier
) {
    if (chartData.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Colors derived from local package theme variables
    val activeGradient = Brush.verticalGradient(
        colors = listOf(ElectricBlue, AthleticBlue)
    )
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val textValueColor = MaterialTheme.colorScheme.onSurface

    // Find local max to scale items safely
    val maxProgressValue = maxOf(4, chartData.maxOfOrNull { it.completedCount } ?: 0)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val bottomPadding = 30.dp.toPx()
        val topPadding = 20.dp.toPx()
        val chartHeight = height - bottomPadding - topPadding
        val barCount = chartData.size
        val spaceBetweenBarsBlock = width / barCount

        // 1. Draw horizontal metric guide background markers
        val horizontalLines = 4
        for (i in 0..horizontalLines) {
            val yPos = topPadding + (chartHeight / horizontalLines) * i
            drawLine(
                color = gridLineColor,
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Draw bars and coordinate elements
        chartData.forEachIndexed { index, progress ->
            val blockCenterX = (spaceBetweenBarsBlock * index) + (spaceBetweenBarsBlock / 2f)
            val barWidth = 16.dp.toPx()

            // Safe calculation for bar height
            val valuePercent = progress.completedCount.toFloat() / maxProgressValue.toFloat()
            val finalBarHeight = chartHeight * valuePercent

            val barTopY = topPadding + chartHeight - finalBarHeight
            val barBottomY = topPadding + chartHeight

            if (progress.completedCount > 0) {
                // Draw rounded athletic pill bar
                drawRoundRect(
                    brush = activeGradient,
                    topLeft = Offset(blockCenterX - (barWidth / 2f), barTopY),
                    size = Size(barWidth, finalBarHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            } else {
                // Draw decorative empty baseline stub
                drawRoundRect(
                    color = textLabelColor.copy(alpha = 0.15f),
                    topLeft = Offset(blockCenterX - (barWidth / 2f), barBottomY - 4.dp.toPx()),
                    size = Size(barWidth, 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // Draw Day text label labels underneath
            // Note: Standard native text drawing is easiest via nativeCanvas interface
            drawContext.canvas.nativeCanvas.apply {
                val textPaintLabel = android.graphics.Paint().apply {
                    color = textLabelColor.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val textPaintFreq = android.graphics.Paint().apply {
                    color = textValueColor.toArgb()
                    textSize = 10.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Day Label name
                drawText(
                    progress.dayLabel,
                    blockCenterX,
                    height - 8.dp.toPx(),
                    textPaintLabel
                )

                // Draw completed target indicators on top of individual bar if greater than 0
                if (progress.completedCount > 0) {
                    drawText(
                        "${progress.completedCount}",
                        blockCenterX,
                        barTopY - 6.dp.toPx(),
                        textPaintFreq
                    )
                }
            }
        }
    }
}

/**
 * Micro stats rendering card
 */
@Composable
fun StatMicroCard(
    title: String,
    value: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * A beautiful list item card row representing individual exercises logged.
 */
@Composable
fun WorkoutItemRow(
    workout: Workout,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    showDate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedBackgroundCardColor by animateColorAsState(
        targetValue = if (workout.isCompleted) {
            ElectricBlue.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "WorkoutRowBackground"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBackgroundCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Completion checklist checkmark target
            IconButton(
                onClick = onToggleCompletion,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toggle_complete_${workout.id}")
            ) {
                Icon(
                    imageVector = if (workout.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle completion status",
                    tint = if (workout.isCompleted) NeonGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(26.dp)
                )
            }

            // 2. Exercise details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = workout.exerciseName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (workout.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = if (workout.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set and reps badge
                    Text(
                        text = "${workout.sets} sets × ${workout.reps} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Weight Indicator (Optional)
                    workout.weight?.let { kg ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$kg kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Duration Indicator (Optional)
                    workout.duration?.let { min ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${min}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Row-Date details (e.g. historical context)
                if (showDate) {
                    val formattedDate = remember(workout.date) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        sdf.format(Date(workout.date))
                    }
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 3. Simple delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("delete_workout_${workout.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Workout Instance",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Empty States placeholder
 */
@Composable
fun EmptyStatePlaceholder(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionString: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .testTag("empty_state_action_button")
                    .padding(top = 8.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = actionString, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * ProfileImageBubble - Custom profile picture rendering supporting initials, presets, or gallery pictures
 */
@Composable
fun ProfileImageBubble(
    profilePic: String?,
    userName: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bitmap = remember(profilePic) {
        if (profilePic != null && (profilePic.startsWith("content://") || profilePic.startsWith("file://"))) {
            try {
                val uri = Uri.parse(profilePic)
                val inputStream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(ElectricBlue.copy(alpha = 0.15f))
        .border(1.5.dp, ElectricBlue, CircleShape)
        .run {
            if (onClick != null) clickable(onClick = onClick) else this
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            when (profilePic) {
                "avatar_runner" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsRun,
                            contentDescription = "Runner Avatar",
                            tint = NeonGreen,
                            modifier = Modifier.size((size.value * 0.55f).dp)
                        )
                    }
                }
                "avatar_captain" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AthleticBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Captain Avatar",
                            tint = AthleticBlue,
                            modifier = Modifier.size((size.value * 0.55f).dp)
                        )
                    }
                }
                "avatar_yoga" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE040FB).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SelfImprovement,
                            contentDescription = "Yoga Avatar",
                            tint = Color(0xFFE040FB),
                            modifier = Modifier.size((size.value * 0.55f).dp)
                        )
                    }
                }
                "avatar_iron" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF9100).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Iron Avatar",
                            tint = Color(0xFFFF9100),
                            modifier = Modifier.size((size.value * 0.55f).dp)
                        )
                    }
                }
                else -> {
                    val initial = if (userName.isNotEmpty()) userName.take(1).uppercase() else "C"
                    Text(
                        text = initial,
                        fontSize = (size.value * 0.45f).sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricBlue
                    )
                }
            }
        }
    }
}

/**
 * Interactive Profile Config dialogue containing customizable Name, Contact, and Email.
 */
@Composable
fun ProfileDialog(
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val nameState by viewModel.userName.collectAsStateWithLifecycle()
    val contactState by viewModel.userContact.collectAsStateWithLifecycle()
    val usernameState by viewModel.userUsername.collectAsStateWithLifecycle()
    val profilePicState by viewModel.userProfilePicture.collectAsStateWithLifecycle()

    var nameInput by remember { mutableStateOf(nameState) }
    var contactInput by remember { mutableStateOf(contactState) }
    var usernameInput by remember { mutableStateOf(usernameState) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "USER PROFILE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = ElectricBlue
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close Profile")
                    }
                }

                // Profile summary and Avatar decorative element
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElectricBlue.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileImageBubble(
                        profilePic = profilePicState,
                        userName = nameInput,
                        size = 56.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = nameInput.ifEmpty { "Champion" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Level 8 Active Trainer",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlue
                        )
                    }
                }

                // Avatar Select Row Header
                Text(
                    text = "CHOOSE AVATAR OR PHOTO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = ElectricBlue
                )

                // Carousel of presets + gallery trigger
                val pLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        viewModel.updateProfilePicture(uri.toString())
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(
                        "avatar_runner",
                        "avatar_captain",
                        "avatar_yoga",
                        "avatar_iron"
                    )
                    
                    presets.forEach { id ->
                        val isSelected = profilePicState == id
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ElectricBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateProfilePicture(id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (id) {
                                "avatar_runner" -> Icons.Filled.DirectionsRun
                                "avatar_captain" -> Icons.Filled.EmojiEvents
                                "avatar_yoga" -> Icons.Filled.SelfImprovement
                                else -> Icons.Filled.FitnessCenter
                            }
                            val color = when (id) {
                                "avatar_runner" -> NeonGreen
                                "avatar_captain" -> AthleticBlue
                                "avatar_yoga" -> Color(0xFFE040FB)
                                else -> Color(0xFFFF9100)
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Gallery option button
                    val isCustom = profilePicState != null && profilePicState !in presets
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isCustom) ElectricBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = if (isCustom) 2.dp else 1.dp,
                                color = if (isCustom) ElectricBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable {
                                pLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Upload Custom Photo",
                            tint = if (isCustom) ElectricBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Field with customizable name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Custom Name") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Field with customizable Contact
                OutlinedTextField(
                    value = contactInput,
                    onValueChange = { contactInput = it },
                    label = { Text("Contact Phone") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Field with customizable Username
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Actions Cancel / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.updateProfile(nameInput, contactInput, usernameInput)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Save Details", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * SETTINGS SCREEN - Manage profile, theme selection, custom workout targets, goals, and more.
 */
@Composable
fun SettingsScreen(viewModel: WorkoutViewModel) {
    val context = LocalContext.current
    val nameState by viewModel.userName.collectAsStateWithLifecycle()
    val contactState by viewModel.userContact.collectAsStateWithLifecycle()
    val usernameState by viewModel.userUsername.collectAsStateWithLifecycle()
    val themeModeState by viewModel.themeMode.collectAsStateWithLifecycle()
    val profilePicState by viewModel.userProfilePicture.collectAsStateWithLifecycle()

    val waterGoal by viewModel.waterGoal.collectAsStateWithLifecycle()
    val waterIntakeToday by viewModel.waterIntakeToday.collectAsStateWithLifecycle()
    val stepGoal by viewModel.stepGoal.collectAsStateWithLifecycle()
    val stepsToday by viewModel.stepsToday.collectAsStateWithLifecycle()
    val restTimerDefault by viewModel.restTimerDefault.collectAsStateWithLifecycle()
    val measurementUnit by viewModel.measurementUnit.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (showEditProfileDialog) {
        ProfileDialog(viewModel = viewModel, onDismiss = { showEditProfileDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title
        Column {
            Text(
                text = "CONFIG",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = ElectricBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. Profile Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileImageBubble(
                    profilePic = profilePicState,
                    userName = nameState,
                    size = 72.dp,
                    onClick = { showEditProfileDialog = true }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameState,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (usernameState.startsWith("@")) usernameState else "@$usernameState",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = contactState,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(ElectricBlue.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile Info",
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Theme Selection Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "APP THEME",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ElectricBlue
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf("System", "Light", "Dark")
                    themes.forEachIndexed { index, label ->
                        val isSelected = themeModeState == index
                        Button(
                            onClick = { viewModel.updateThemeMode(index) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Hydration Water Tracker Interactivity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalDrink,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "HYDRATION ASSISTANT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = ElectricBlue,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$waterIntakeToday/${waterGoal}ml",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Hydration Progress indicator
                val hydrationProgress = if (waterGoal > 0) (waterIntakeToday.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { hydrationProgress },
                    modifier = Modifier
                         .fillMaxWidth()
                         .height(8.dp)
                         .clip(CircleShape),
                    color = ElectricBlue,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.addWaterIntake(250) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.15f), contentColor = ElectricBlue),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+250ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.addWaterIntake(500) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.15f), contentColor = ElectricBlue),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+500ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetWaterIntake() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                // Slider to adjust water goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Water Target:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${waterGoal} ml",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        maxLines = 1
                    )
                }
                Slider(
                    value = waterGoal.toFloat(),
                    onValueChange = { viewModel.updateWaterGoal(it.toInt()) },
                    valueRange = 1000f..5000f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                )
            }
        }

        // 4. Activity Step Counter Interactivity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "STEP TRACKER LOG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = ElectricBlue,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$stepsToday/${stepGoal}st",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Step Progress indicator
                val stepsProgress = if (stepGoal > 0) (stepsToday.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { stepsProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = ElectricBlue,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.addSteps(1000) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.15f), contentColor = ElectricBlue),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+1,000", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.addSteps(5000) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.15f), contentColor = ElectricBlue),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+5,000", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetSteps() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                // Slider to adjust steps goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Step Goal Target:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stepGoal} steps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        maxLines = 1
                    )
                }
                Slider(
                    value = stepGoal.toFloat(),
                    onValueChange = { viewModel.updateStepGoal(it.toInt()) },
                    valueRange = 3000f..20000f,
                    steps = 17,
                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                )
            }
        }

        // 5. Config Utilities Card (Rest Timer and Units)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "UTILITY PREFERENCES",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ElectricBlue
                    )
                }

                // Default resting timer customization
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Default Rest Interval Between Sets:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val intervals = listOf(30, 60, 90, 120, 180)
                        intervals.forEach { sec ->
                            val isSelected = restTimerDefault == sec
                            Button(
                                onClick = { viewModel.updateRestTimer(sec) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "${sec}s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Measurement Units
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Measurement System", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Weight & Distance unit metrics", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val units = listOf("Metric (kg/km)", "Imperial (lbs/mi)")
                        units.forEachIndexed { index, label ->
                            val isSelected = measurementUnit == index
                            Button(
                                onClick = { viewModel.updateMeasurementUnit(index) },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(text = label.substringBefore(" "), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Body Weight Tracker Panel with Line Graph Visualization
        BodyWeightSection(viewModel = viewModel)

        // 6. Professional Utilities: Export History and Information Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DATA MANAGEMENT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ElectricBlue
                    )
                }

                Text(
                    text = "Export your records or share workout details to clipboard for personal review on external trackers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                val allWorkouts by viewModel.allWorkouts.collectAsStateWithLifecycle()
                Button(
                    onClick = {
                        val csv = StringBuilder("Gymora Workout Record History\nDate,Exercise,Sets,Reps,Weight,Duration\n")
                        allWorkouts.forEach {
                            val wStr = if (it.weight != null) "${it.weight}kg" else "N/A"
                            val dStr = if (it.duration != null) "${it.duration}m" else "N/A"
                            csv.append("${it.date},\"${it.exerciseName}\",${it.sets},${it.reps},$wStr,$dStr\n")
                        }
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Gymora Log History Export", csv.toString())
                        clipboardManager.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Workout history successfully copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Detailed History Export", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Modal Alert adding workouts records safely.
 */
@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, sets: Int, reps: Int, weight: Double?, duration: Int?, date: Long, completed: Boolean) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var setsString by remember { mutableStateOf("3") }
    var repsString by remember { mutableStateOf("10") }
    var weightString by remember { mutableStateOf("") }
    var durationString by remember { mutableStateOf("") }
    var markCompleted by remember { mutableStateOf(true) }

    // Validation
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .testTag("add_workout_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOG NEW EXERCISE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close Dialog")
                    }
                }

                // Exercise Field with Suggestions Dropdown
                var dropdownExpanded by remember { mutableStateOf(false) }
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
                    "Lunges",
                    "Crunch",
                    "Running",
                    "Cycling"
                )
                val filteredSuggestions = remember(exerciseName) {
                    if (exerciseName.isBlank()) {
                        presetExercises
                    } else {
                        presetExercises.filter { it.contains(exerciseName, ignoreCase = true) }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { 
                            exerciseName = it
                            showError = false
                            dropdownExpanded = true
                        },
                        label = { Text("Exercise Name (e.g., Pushups)") },
                        shape = RoundedCornerShape(12.dp),
                        isError = showError && exerciseName.isBlank(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                Icon(
                                    imageVector = if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = "Show exercise presets"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_exercise_name_input")
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { dropdownExpanded = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(text = suggestion) },
                                onClick = {
                                    exerciseName = suggestion
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reps and Sets on a nice Row grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = setsString,
                        onValueChange = { setsString = it },
                        label = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_sets_input")
                    )

                    OutlinedTextField(
                        value = repsString,
                        onValueChange = { repsString = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_reps_input")
                    )
                }

                // Support metrics weight and durations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = weightString,
                        onValueChange = { weightString = it },
                        label = { Text("Weight (kg, optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_weight_input")
                    )

                    OutlinedTextField(
                        value = durationString,
                        onValueChange = { durationString = it },
                        label = { Text("Duration (mins, opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_duration_input")
                    )
                }

                // Completion status options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { markCompleted = !markCompleted }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = markCompleted,
                        onCheckedChange = { markCompleted = it },
                        modifier = Modifier.testTag("dialog_completed_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Mark immediately completed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Uncheck to log as a target to do later today.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                if (showError) {
                    Text(
                        text = "Exercise name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Action controls Save / Dismissal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (exerciseName.isBlank()) {
                                showError = true
                            } else {
                                val s = setsString.toIntOrNull() ?: 3
                                val r = repsString.toIntOrNull() ?: 10
                                val w = weightString.toDoubleOrNull()
                                val d = durationString.toIntOrNull()
                                onSave(exerciseName, s, r, w, d, System.currentTimeMillis(), markCompleted)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("dialog_save_button")
                    ) {
                        Text("Save Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * BODY WEIGHT TRACKER - Display weight history over time with an elegant line graph and quick entry logger.
 */
@Composable
fun BodyWeightSection(viewModel: WorkoutViewModel) {
    val bodyWeights by viewModel.allBodyWeights.collectAsStateWithLifecycle()
    val measurementUnit by viewModel.measurementUnit.collectAsStateWithLifecycle()
    val unitLabel = if (measurementUnit == 0) "kg" else "lbs"

    var newWeightInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonitorWeight,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "BODY WEIGHT TRACKER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ElectricBlue
                    )
                }

                if (bodyWeights.isNotEmpty()) {
                    val latest = bodyWeights.last()
                    Text(
                        text = "Current: ${latest.weight} $unitLabel",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Chart area
            Text(
                text = "Progress Chart Time-series (Metric)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Canvas-based Line Graph (acting like Recharts line plot but standard native Compose)
            WeightLineChart(
                entries = bodyWeights,
                unitLabel = unitLabel
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Quick add section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Insert Daily Log Weight",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newWeightInput,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                newWeightInput = it
                            }
                            inputError = null
                        },
                        label = { Text("Log Weight ($unitLabel)") },
                        placeholder = { Text("e.g. 78.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        isError = inputError != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val w = newWeightInput.toDoubleOrNull()
                            if (w == null || w <= 0.0) {
                                inputError = "Enter valid weight"
                            } else {
                                viewModel.addBodyWeight(w)
                                newWeightInput = ""
                                inputError = null
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("submit_weight_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log")
                    }
                }
                
                if (inputError != null) {
                    Text(
                        text = inputError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Log entries list to delete if needed
            if (bodyWeights.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Text(
                    text = "Recent Log Details",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                // List of latest 4 weight records
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bodyWeights.takeLast(4).reversed().forEach { item ->
                        val dateStr = remember(item.date) {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.date))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(text = dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${item.weight} $unitLabel",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue
                                )
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete weight log",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { viewModel.deleteBodyWeightById(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Customized Canvas-based Line Graph behaving exactly like Recharts but optimized natively
 */
@Composable
fun WeightLineChart(
    entries: List<com.example.data.BodyWeight>,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) {
        // Aesthetic empty state inside the chart container
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "No Progress Graph Available",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "Log at least 2 weight logs to automatically generate custom time-series trend visuals.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.widthIn(max = 250.dp)
                )
            }
        }
        return
    }

    // Chart has entries! Let's plot beautifully!
    // Sort to be extra sure they are clean, and remember metrics to prevent garbage collection churn
    val sorted = remember(entries) { entries.sortedBy { it.date } }
    
    val metrics = remember(sorted) {
        val weightsList = sorted.map { it.weight }
        val minW = (weightsList.minOrNull() ?: 0.0) - 2.0
        val maxW = (weightsList.maxOrNull() ?: 100.0) + 2.0
        val rangeW = maxW - minW
        
        val firstTarget = sorted.firstOrNull()?.date ?: 0L
        val lastTarget = sorted.lastOrNull()?.date ?: 0L
        val rangeD = (lastTarget - firstTarget).coerceAtLeast(1L)
        
        val formatShortDate = SimpleDateFormat("MM/dd", Locale.getDefault())
        val firstStr = formatShortDate.format(Date(firstTarget))
        val lastStr = formatShortDate.format(Date(lastTarget))
        val midStr = if (sorted.size > 2) {
            val midIndex = sorted.size / 2
            formatShortDate.format(Date(sorted[midIndex].date))
        } else null
        
        android.util.Pair(
            arrayOf(minW, maxW, rangeW, firstTarget.toDouble(), lastTarget.toDouble(), rangeD.toDouble()),
            arrayOf(firstStr, lastStr, midStr)
        )
    }

    val minWeight = metrics.first[0]
    val maxWeight = metrics.first[1]
    val weightRange = metrics.first[2]
    val firstDate = metrics.first[3].toLong()
    val lastDate = metrics.first[4].toLong()
    val dateRange = metrics.first[5].toLong()
    
    val firstDateStr = metrics.second[0] as String
    val lastDateStr = metrics.second[1] as String
    val midDateStr = metrics.second[2] as? String

    val gridLinesCount = 3
    val primaryColor = ElectricBlue
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(bottom = 8.dp)
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw horizontal grid lines and Y labels
            for (i in 0..gridLinesCount) {
                val y = i * (height / gridLinesCount)
                drawLine(
                    color = gridColor,
                    start = Offset(45.dp.toPx(), y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Left offset to leave room for Y labels: 45.dp
            val startX = 45.dp.toPx()
            val drawWidth = width - startX
            
            // Draw gradient shaded area path (calculate offsets on-the-fly to avoid allocations inside drawing block)
            if (sorted.isNotEmpty()) {
                val gradientPath = androidx.compose.ui.graphics.Path().apply {
                    val firstItem = sorted.first()
                    val firstX = if (sorted.size > 1) {
                        startX + drawWidth * ((firstItem.date - firstDate).toFloat() / dateRange.toFloat())
                    } else {
                        startX + drawWidth / 2f
                    }
                    val firstY = height - (height * ((firstItem.weight - minWeight).toFloat() / weightRange.toFloat()))
                    
                    moveTo(firstX, height)
                    
                    sorted.forEach { item ->
                        val x = if (sorted.size > 1) {
                            startX + drawWidth * ((item.date - firstDate).toFloat() / dateRange.toFloat())
                        } else {
                            startX + drawWidth / 2f
                        }
                        val y = height - (height * ((item.weight - minWeight).toFloat() / weightRange.toFloat()))
                        lineTo(x, y)
                    }
                    
                    val lastItem = sorted.last()
                    val lastX = if (sorted.size > 1) {
                        startX + drawWidth * ((lastItem.date - firstDate).toFloat() / dateRange.toFloat())
                    } else {
                        startX + drawWidth / 2f
                    }
                    lineTo(lastX, height)
                    close()
                }

                drawPath(
                    path = gradientPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )
            }

            // Draw line path
            if (sorted.isNotEmpty()) {
                val linePath = androidx.compose.ui.graphics.Path().apply {
                    sorted.forEachIndexed { i, item ->
                        val x = if (sorted.size > 1) {
                            startX + drawWidth * ((item.date - firstDate).toFloat() / dateRange.toFloat())
                        } else {
                            startX + drawWidth / 2f
                        }
                        val y = height - (height * ((item.weight - minWeight).toFloat() / weightRange.toFloat()))
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            // Draw interactive markers
            sorted.forEach { item ->
                val x = if (sorted.size > 1) {
                    startX + drawWidth * ((item.date - firstDate).toFloat() / dateRange.toFloat())
                } else {
                    startX + drawWidth / 2f
                }
                val y = height - (height * ((item.weight - minWeight).toFloat() / weightRange.toFloat()))
                val pt = Offset(x, y)
                
                // Outer glow
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 8.dp.toPx(),
                    center = pt
                )
                // Solid border circle
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                // White inner dot
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }

        // Draw X Axis dates labels beautifully
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 45.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = firstDateStr,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = labelColor
            )

            // Show middle date indicator if there are enough entries
            if (sorted.size > 2 && midDateStr != null) {
                Text(
                    text = midDateStr,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = labelColor
                )
            }

            Text(
                text = lastDateStr,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = labelColor
            )
        }
    }
}

/**
 * Dialog displaying historic workouts and activities completed on the selected calendar date.
 */
@Composable
fun DaySummaryDialog(
    dateMillis: Long,
    workouts: List<Workout>,
    onDismiss: () -> Unit,
    onToggleCompletion: (Workout) -> Unit,
    onDelete: (Workout) -> Unit
) {
    val dateStr = remember(dateMillis) {
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(dateMillis))
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("day_summary_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAY INSPECTOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = ElectricBlue
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (workouts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No workouts recorded",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "There are no workout logs filed for this selected calendar date.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${workouts.size} Workouts Found",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        workouts.forEach { w ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = w.isCompleted,
                                        onCheckedChange = { onToggleCompletion(w) }
                                    )
                                    Column {
                                        Text(
                                            text = w.exerciseName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            style = if (w.isCompleted) {
                                                androidx.compose.ui.text.TextStyle(
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                )
                                            } else {
                                                androidx.compose.ui.text.TextStyle.Default
                                            },
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${w.sets} sets × ${w.reps} reps" + 
                                                   (if (w.weight != null) " @ ${w.weight}kg" else "") +
                                                   (if (w.duration != null) " (${w.duration}m)" else ""),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDelete(w) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
