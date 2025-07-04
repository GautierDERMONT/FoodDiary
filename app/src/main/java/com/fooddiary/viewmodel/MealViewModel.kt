package com.fooddiary.viewmodel

import androidx.lifecycle.ViewModel
import com.fooddiary.model.*

class MealViewModel : ViewModel() {
    val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    var weekMeals = mutableListOf<DayMeals>().apply {
        weekDays.forEach { day ->
            add(
                DayMeals(
                    day = day,
                    meals = mutableListOf(
                        Meal(MealType.BREAKFAST),
                        Meal(MealType.LUNCH),
                        Meal(MealType.DINNER),
                        Meal(MealType.SNACK)
                    )
                )
            )
        }
    }

    fun getMealsForDay(day: String): List<Meal> {
        return weekMeals.find { it.day == day }?.meals ?: emptyList()
    }

    fun updateMeal(day: String, mealType: MealType, newMeal: Meal) {
        weekMeals.find { it.day == day }?.meals?.let { meals ->
            val index = meals.indexOfFirst { it.type == mealType }
            if (index != -1) meals[index] = newMeal
        }
    }
}