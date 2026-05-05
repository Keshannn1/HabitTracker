package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.ui.dashboard.DashboardScreen
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.ui.timer.ActiveTimerScreen
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val DASHBOARD = "dashboard"
    const val ACTIVE_TIMER = "active_timer/{routineId}"
    fun activeTimer(routineId: String) = "active_timer/$routineId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitTrackerTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            onStartRoutine = { routineId ->
                                navController.navigate(Routes.activeTimer(routineId))
                            }
                        )
                    }
                    composable(
                        route = Routes.ACTIVE_TIMER,
                        arguments = listOf(
                            navArgument("routineId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val routineId = backStackEntry.arguments?.getString("routineId") ?: return@composable
                        ActiveTimerScreen(
                            routineId = routineId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    HabitTrackerTheme {
        DashboardScreen(
            onStartRoutine = {},
            viewModel = hiltViewModel()
        )
    }
}