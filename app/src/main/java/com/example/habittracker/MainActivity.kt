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
import com.example.habittracker.ui.theme.HabitTrackerTheme
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
    const val LOADING = "loading"
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
            HabitTrackerTheme {
                HabitTrackerNavGraph()
            }
        }
    }
}

@Composable
fun HabitTrackerNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (navController.currentDestination?.route != Routes.DASHBOARD) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOADING) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated, is AuthState.Error -> {
                if (navController.currentDestination?.route != Routes.AUTH) {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Loading -> {
                if (navController.currentDestination?.route != Routes.LOADING) {
                    navController.navigate(Routes.LOADING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOADING,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.LOADING) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

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
    HabitTrackerTheme {
        DashboardScreen(
            onStartRoutine = {},
            onCreateRoutine = {},
            onEditRoutine = {},
            onNavigateToTodos = {},
            onProfile = {}
        )
    }
}