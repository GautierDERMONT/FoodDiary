package com.fooddiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fooddiary.screens.AddMealScreen
import com.fooddiary.screens.HomeScreen
import com.fooddiary.screens.MealDetailScreen
import com.fooddiary.screens.DayMealsScreen
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
    val weekMealsState = viewModel.weekMeals.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onMealClick = { day, mealIndex ->
                    val dayMeals = weekMealsState.value.find { it.day == day }
                    val meal = dayMeals?.meals?.getOrNull(mealIndex)

                    if (meal != null) {
                        if (meal.description.isBlank() && meal.photoUri == null) {
                            navController.navigate("addMeal/$day/$mealIndex")
                        } else {
                            navController.navigate("mealDetail/$day/$mealIndex")
                        }
                    }
                },
                        onDayClick = { day ->
                    navController.navigate("dayMeals/$day")
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


        composable("dayMeals/{day}") { backStackEntry ->
            val day = backStackEntry.arguments?.getString("day") ?: ""
            DayMealsScreen(
                navController = navController,
                day = day,
                viewModel = viewModel
            )
        }

        composable("mealDetail/{day}/{mealIndex}") { backStackEntry ->
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
}