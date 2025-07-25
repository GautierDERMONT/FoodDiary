package com.fooddiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.fooddiary.screens.AddMealScreen
import com.fooddiary.screens.HomeScreen
import com.fooddiary.screens.MealDetailScreen
import com.fooddiary.screens.DayMealsScreen
import com.fooddiary.ui.theme.FoodDiaryTheme
import com.fooddiary.screens.RecapScreen
import com.fooddiary.screens.ExportScreen
import com.fooddiary.viewmodel.MealViewModel
import com.fooddiary.screens.SplashScreen
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.systemBarsPadding


class MainActivity : ComponentActivity() {
    private val viewModel: MealViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val application = requireNotNull(this@MainActivity.application) as FoodDiaryApplication
                return MealViewModel(application.database, application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            FoodDiaryTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    FoodDiaryApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun FoodDiaryApp(viewModel: MealViewModel) {
    val navController = rememberNavController()
    val weekMealsState = viewModel.weekMeals.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("splash") {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(2000) // Affiche le splash screen pendant 2 secondes
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                viewModel = viewModel,
                onMealClick = { day, mealIndex ->
                    navController.navigate("mealDetail/$day/$mealIndex")
                },
                onAddMealClick = { day, mealIndex ->
                    navController.navigate("addMeal/$day/$mealIndex")
                },
                onDayClick = { day ->
                    navController.navigate("dayMeals/$day")
                },
                onRecapClick = {
                    navController.navigate("recap")
                }
            )
        }


        composable("recap") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                RecapScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }

        composable("addMeal/{day}/{mealIndex}") { backStackEntry ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val day = backStackEntry.arguments?.getString("day") ?: ""
                val mealIndex = backStackEntry.arguments?.getString("mealIndex")?.toIntOrNull() ?: 0
                AddMealScreen(
                    navController = navController,
                    day = day,
                    mealIndex = mealIndex,
                    viewModel = viewModel
                )
            }
        }

        composable("dayMeals/{day}") { backStackEntry ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val day = backStackEntry.arguments?.getString("day") ?: ""
                DayMealsScreen(
                    navController = navController,
                    day = day,
                    viewModel = viewModel
                )
            }
        }

        composable("mealDetail/{day}/{mealIndex}") { backStackEntry ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val day = backStackEntry.arguments?.getString("day") ?: ""
                val mealIndex = backStackEntry.arguments?.getString("mealIndex")?.toIntOrNull() ?: 0
                val dayMeals = weekMealsState.value.find { it.day == day }
                val meal = dayMeals?.meals?.getOrNull(mealIndex)

                MealDetailScreen(
                    navController = navController,
                    day = day,
                    mealIndex = mealIndex,
                    viewModel = viewModel
                )
            }
        }

        composable("export") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ExportScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}