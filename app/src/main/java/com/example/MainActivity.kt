package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.FitTrackApp
import com.example.ui.WorkoutViewModel
import com.example.ui.WorkoutViewModelFactory
import com.example.ui.theme.FitTrackProTheme

/**
 * Main entrance activity for FitTrack Pro.
 * Setting up Full Egde-to-Edge composability and routing to FitTrackApp interface.
 */
class MainActivity : ComponentActivity() {

    // ViewModel instantiated cleanly via factory constructor standard architecture pattern
    private val viewModel: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mandatory edge-to-edge support configuration
        enableEdgeToEdge()
        
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                1 -> false // Light Mode
                2 -> true  // Dark Mode
                else -> isSystemInDarkTheme() // System Default Mode
            }

            FitTrackProTheme(darkTheme = darkTheme) {
                FitTrackApp(viewModel = viewModel)
            }
        }
    }
}
