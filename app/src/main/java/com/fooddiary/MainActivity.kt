package com.fooddiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fooddiary.screens.AddMealScreen
import com.fooddiary.screens.HomeScreen
import com.fooddiary.ui.theme.FoodDiaryTheme
import com.fooddiary.viewmodel.MealViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MealViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val application = requireNotNull(this@MainActivity.application) as FoodDiaryApplication
                return MealViewModel(application.database) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodDiaryTheme {
                FoodDiaryApp(viewModel)
            }
        }
    }
}

@Composable
fun FoodDiaryApp(viewModel: MealViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onMealClick = { day, mealIndex ->
                    navController.navigate("addMeal/$day/$mealIndex")
                }
            )
        }
        composable("addMeal/{day}/{mealIndex}") { backStackEntry ->
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
}