package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.domain.model.AuthState
import com.example.habittracker.presentation.AuthViewModel
import com.example.habittracker.presentation.DashboardViewModel
import com.example.habittracker.presentation.TodoViewModel
import com.example.habittracker.ui.analytics.AnalyticsScreen
import com.example.habittracker.ui.auth.AuthScreen
import com.example.habittracker.ui.ai.AiGeneratorScreen
import com.example.habittracker.ui.dashboard.DashboardScreen
import com.example.habittracker.ui.dashboard.RoutineDetailScreen
import com.example.habittracker.ui.dashboard.TemplateSelectionScreen
import com.example.habittracker.ui.profile.ProfileScreen
import com.example.habittracker.ui.theme.AppTheme
import com.example.habittracker.ui.timer.ActiveTimerScreen
import com.example.habittracker.ui.todo.TodoDetailScreen
import com.example.habittracker.ui.todo.TodoListScreen
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val AUTH = "auth"
    const val DASHBOARD = "dashboard"
    const val ACTIVE_TIMER = "active_timer/{routineId}"
    const val CREATE_ROUTINE = "create_routine"
    const val EDIT_ROUTINE = "edit_routine/{routineId}"
    const val ANALYTICS = "analytics"
    const val PROFILE = "profile"
    const val TEMPLATE_SELECTION = "template_selection"
    // LOADING route removed — auth page is now the first screen
    const val TODO_LIST = "todo_list"
    const val TODO_DETAIL = "todo_detail/{todoId}"
    const val AI_GENERATOR = "ai_generator"
    fun activeTimer(routineId: String) = "active_timer/$routineId"
    fun editRoutine(routineId: String) = "edit_routine/$routineId"
    fun todoDetail(todoId: String) = "todo_detail/$todoId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                HabitTrackerNavGraph()
            }
        }
    }
}

/**
 * Safe navigation helper: guards against IllegalStateException from
 * overlapping navigation actions, which is the #1 crash cause.
 */
private fun safeNavigate(
    navController: androidx.navigation.NavController,
    route: String,
    popUpToRoute: String? = null,
    inclusive: Boolean = false
) {
    try {
        if (popUpToRoute != null) {
            navController.navigate(route) {
                popUpTo(popUpToRoute) { this.inclusive = inclusive }
                launchSingleTop = true
            }
        } else {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    } catch (e: IllegalArgumentException) {
        // Navigation already in progress or back stack altered - safe to ignore
        android.util.Log.w("Navigation", "Safe navigation failed: ${e.message}")
    } catch (e: IllegalStateException) {
        android.util.Log.w("Navigation", "Safe navigation failed: ${e.message}")
    }
}

@Composable
fun HabitTrackerNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    val lastNavRequest = remember { mutableStateOf<AuthState?>(null) }

    LaunchedEffect(authState) {
        if (lastNavRequest.value === authState) return@LaunchedEffect
        lastNavRequest.value = authState

        val currentRoute = navController.currentDestination?.route
        when {
            authState is AuthState.Authenticated && currentRoute != Routes.DASHBOARD -> {
                safeNavigate(navController, Routes.DASHBOARD, Routes.AUTH, true)
            }
            (authState is AuthState.Unauthenticated || authState is AuthState.Error) && currentRoute != Routes.AUTH -> {
                // Sign-out or error: go back to login page
                safeNavigate(navController, Routes.AUTH, Routes.DASHBOARD, true)
            }
            // Loading: stay on current screen — AuthScreen handles its own UI
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                viewModel = authViewModel
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onStartRoutine = { routineId ->
                    navController.navigate(Routes.activeTimer(routineId))
                },
                onCreateRoutine = {
                    navController.navigate(Routes.CREATE_ROUTINE)
                },
                onEditRoutine = { routineId ->
                    navController.navigate(Routes.editRoutine(routineId))
                },
                onUseTemplate = {
                    navController.navigate(Routes.TEMPLATE_SELECTION)
                },
                onAnalytics = {
                    navController.navigate(Routes.ANALYTICS)
                },
                onNavigateToTodos = {
                    navController.navigate(Routes.TODO_LIST)
                },
                onProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNavigateToAiGenerator = {
                    navController.navigate(Routes.AI_GENERATOR)
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
                onBack = { navController.popBackStack() },
                onRoutineCompleted = {
                    dashboardViewModel.onRoutineCompleted()
                }
            )
        }

        composable(Routes.CREATE_ROUTINE) {
            RoutineDetailScreen(
                routineId = null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_ROUTINE,
            arguments = listOf(
                navArgument("routineId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: return@composable
            RoutineDetailScreen(
                routineId = routineId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ANALYTICS) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() },
                onProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TEMPLATE_SELECTION) {
            TemplateSelectionScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { routineId ->
                    navController.navigate(Routes.editRoutine(routineId)) {
                        popUpTo(Routes.DASHBOARD)
                    }
                },
                onNavigateToCreate = {
                    navController.navigate(Routes.CREATE_ROUTINE) {
                        popUpTo(Routes.DASHBOARD)
                    }
                }
            )
        }

        // === AI GENERATOR ROUTE ===
        composable(Routes.AI_GENERATOR) {
            AiGeneratorScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // === TODO ROUTES ===
        composable(Routes.TODO_LIST) {
            val todoViewModel: TodoViewModel = hiltViewModel()
            TodoListScreen(
                onBack = { navController.popBackStack() },
                onCreateTodo = { },
                onEditTodo = { todoId ->
                    navController.navigate(Routes.todoDetail(todoId))
                }
            )
        }

        composable(
            route = Routes.TODO_DETAIL,
            arguments = listOf(
                navArgument("todoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId") ?: return@composable
            val todoDetailViewModel: TodoViewModel = hiltViewModel()
            val todoState by todoDetailViewModel.allTodos.collectAsState()
            val todo = todoState.find { it.id == todoId }

            if (todo != null) {
                TodoDetailScreen(
                    todo = todo,
                    onBack = { navController.popBackStack() },
                    onSave = { updatedTodo ->
                        todoDetailViewModel.updateTodoFull(updatedTodo)
                    },
                    onDelete = { deletedTodo ->
                        todoDetailViewModel.deleteTodo(deletedTodo)
                        navController.popBackStack()
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    AppTheme {
        DashboardScreen(
            onStartRoutine = {},
            onCreateRoutine = {},
            onEditRoutine = {},
            onNavigateToTodos = {},
            onProfile = {}
        )
    }
}