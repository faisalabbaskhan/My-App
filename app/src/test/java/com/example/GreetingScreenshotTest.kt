package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Workout
import com.example.ui.WorkoutItemRow
import com.example.ui.theme.FitTrackProTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual regression testing on FitTrack Pro custom M3 UI components.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun workout_row_screenshot() {
    composeTestRule.setContent { 
      FitTrackProTheme { 
        WorkoutItemRow(
          workout = Workout(
            id = 42,
            exerciseName = "Pro Bench Press Set",
            sets = 4,
            reps = 12,
            weight = 82.5,
            duration = 10,
            date = 1781282314288L, // static timestamp
            isCompleted = true
          ),
          onToggleCompletion = {},
          onDelete = {}
        )
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/workout_row.png")
  }
}
