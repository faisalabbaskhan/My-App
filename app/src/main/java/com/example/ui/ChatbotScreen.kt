package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ui.theme.AthleticBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Models for Gemini Direct REST API ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<ContentJson>,
    val systemInstruction: ContentJson? = null,
    val generationConfig: GenerationConfigJson? = null
)

@JsonClass(generateAdapter = true)
data class ContentJson(
    val role: String? = null,
    val parts: List<PartJson>
)

@JsonClass(generateAdapter = true)
data class PartJson(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfigJson(
    val temperature: Float? = 0.7f,
    val maxOutputTokens: Int? = 800
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<CandidateJson>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateJson(
    val content: ContentJson? = null,
    val finishReason: String? = null
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Local Chat Message Representation ---

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// --- ViewModel for Chat ---

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("gymora_chat_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    private val listAdapter = moshi.adapter<List<ChatMessage>>(listType)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        try {
            val json = sharedPrefs.getString("chat_history", null)
            if (!json.isNullOrEmpty()) {
                val decoded = listAdapter.fromJson(json)
                if (decoded != null) {
                    _messages.value = decoded
                }
            }
        } catch (e: Exception) {
            _error.value = "Failed to load chat history"
        }
    }

    private fun saveMessages(list: List<ChatMessage>) {
        try {
            val json = listAdapter.toJson(list)
            sharedPrefs.edit().putString("chat_history", json).apply()
        } catch (e: Exception) {
            // Silently fallback if serialization fails
        }
    }

    fun sendMessage(text: String, userContextPrompt: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true
        )

        val updatedList = _messages.value + userMessage
        _messages.value = updatedList
        saveMessages(updatedList)

        _isGenerating.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Compile contents array from current conversation flow
                val apiContents = mutableListOf<ContentJson>()
                
                // Add conversational history limit to avoid token overflow and keep response fast
                val historyLimit = updatedList.takeLast(6)
                for (msg in historyLimit) {
                    apiContents.add(
                        ContentJson(
                            role = if (msg.isUser) "user" else "model",
                            parts = listOf(PartJson(text = msg.text))
                        )
                    )
                }

                val systemPrompt = ContentJson(
                    parts = listOf(PartJson(text = userContextPrompt))
                )

                val requestBody = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = systemPrompt,
                    generationConfig = GenerationConfigJson(
                        temperature = 0.4f,
                        maxOutputTokens = 500
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("Gemini API Key is not set in Secrets. Please configure GEMINI_API_KEY.")
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent("gemini-3.1-flash-lite-preview", apiKey, requestBody)
                }

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I'm sorry, I couldn't formulate a response. Please try again."

                val coachMessage = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = replyText,
                    isUser = false
                )

                val finalNewList = _messages.value + coachMessage
                _messages.value = finalNewList
                saveMessages(finalNewList)

            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                val errorMsg = ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = "⚠️ Network Connection Error: Could not connect to Gymora Coach services. Please ensure your API secrets are fully configured or try again later.",
                    isUser = false
                )
                _messages.value = _messages.value + errorMsg
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearHistory() {
        _messages.value = emptyList()
        sharedPrefs.edit().remove("chat_history").apply()
        _error.value = null
    }
}

// Factory provider
class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Main UI screen for AI Chatbot ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    workoutViewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as Application
    val chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ChatViewModelFactory(context)
    )

    val messages by chatViewModel.messages.collectAsState()
    val isGenerating by chatViewModel.isGenerating.collectAsState()
    val error by chatViewModel.error.collectAsState()

    // Grab user details to pass as context
    val userName by workoutViewModel.userName.collectAsState()
    val stepsToday by workoutViewModel.stepsToday.collectAsState()
    val stepGoal by workoutViewModel.stepGoal.collectAsState()
    val waterIntakeToday by workoutViewModel.waterIntakeToday.collectAsState()
    val waterGoal by workoutViewModel.waterGoal.collectAsState()

    // Dynamic Context details for high quality custom AI answers
    val systemInstructionText = remember(userName, stepsToday, stepGoal, waterIntakeToday, waterGoal) {
        """
        You are "Gymora AI Coach", a premium, clever, and high-intensity motivational fitness coach and nutritional advisor.
        Your personality is: energetic, motivating, precise, scientific, warm, yet demanding! You refer to the user as a Champion.
        
        The user's direct progress today:
        - Name: $userName
        - Today's steps: $stepsToday / $stepGoal steps
        - Today's hydration: ${waterIntakeToday}ml / ${waterGoal}ml
        
        Guidelines:
        1. Keep responses structured, concise, and highly dynamic.
        2. Use bullet points and line breaks for workout routines, menu structures, and health tips.
        3. Keep text optimized for mobile readability (avoid massive walls of text).
        4. If the user asks about their parameters, refer to their real telemetry provided above. Celebrate if they are close to targets!
        5. Motivate the user to complete their water target and step goals today if they are behind.
        6. Do not generate code, only respond about athletic training, aerobics, strength, cardio, recovery, sleep, and sports science.
        """.trimIndent()
    }

    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to bottom when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("chatbot_screen_view")
    ) {
        // Chatbot Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SmartToy,
                            contentDescription = "AI Coach Icon",
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GYMORA AI COACH",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = ElectricBlue
                        )
                        Text(
                            text = "Personalized Athletic Intelligence",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = { chatViewModel.clearHistory() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Clear Chat History",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Messages Thread or Empty Welcome State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                // Empty Welcome Vibe
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "Gymora Fitness Icon",
                        tint = ElectricBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unleash Your Potential!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "I am Gymora, your expert personal trainer. I am plugged into your active dashboard to help you plan workouts, check custom exercises, or build diet strategies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Tap a prompt to begin:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = ElectricBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Suggestion Chip Pile
                    val suggestions = listOf(
                        "Build a fast, full-body workout",
                        "Evaluate my water & steps today",
                        "Best stretching protocols for legs",
                        "Give me a motivation check!"
                    )

                    suggestions.forEach { prompt ->
                        Card(
                            onClick = {
                                chatViewModel.sendMessage(prompt, systemInstructionText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Dynamic Message Bubbles
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatBubbleRow(message = message)
                    }

                    // Floating Typing Loader inside list
                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SmartToy,
                                        contentDescription = "Thinking",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = ElectricBlue
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Gymora is designing a plan...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Input suggestions when thread is active
        if (messages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val contextSuggestions = listOf(
                    "Evaluate today's goals",
                    "Add a training routine",
                    "Nutrition ideas"
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    contextSuggestions.forEach { label ->
                        Card(
                            onClick = {
                                if (!isGenerating) {
                                    chatViewModel.sendMessage(label, systemInstructionText)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Sending Input Controls Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.ime)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = "Message your Gymora AI Coach...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text")
                            .heightIn(max = 120.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.trim().isNotEmpty() && !isGenerating) {
                                    chatViewModel.sendMessage(textInput, systemInstructionText)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty() && !isGenerating) {
                                chatViewModel.sendMessage(textInput, systemInstructionText)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = textInput.trim().isNotEmpty() && !isGenerating,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.trim().isNotEmpty() && !isGenerating) {
                                    ElectricBlue
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                }
                            )
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (textInput.trim().isNotEmpty() && !isGenerating) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Component row representing a single Chat Bubble ---

@Composable
fun ChatBubbleRow(
    message: ChatMessage
) {
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val containerColor = if (message.isUser) {
        AthleticBlue
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (message.isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val rowAlignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = rowAlignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.1f))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = "Coach Avatar",
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (message.isUser) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            if (message.isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AthleticBlue.copy(alpha = 0.2f))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User Avatar",
                        tint = AthleticBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
