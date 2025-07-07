package com.fooddiary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddiary.model.DayMeals
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MealViewModel : ViewModel() {
    val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    private val _weekMeals = MutableStateFlow(
        weekDays.map { day ->
            DayMeals(
                day = day,
                meals = mutableListOf(
                    Meal(MealType.BREAKFAST),
                    Meal(MealType.LUNCH),
                    Meal(MealType.DINNER)
                )
            )
        }
    )
    val weekMeals: StateFlow<List<DayMeals>> = _weekMeals

    fun addEmptyMeal(day: String) {
        viewModelScope.launch {
            _weekMeals.value = _weekMeals.value.map { dayMeals ->
                if (dayMeals.day == day && dayMeals.meals.size < 8) { // Vérifier qu'on n'a pas déjà 8 repas
                    dayMeals.copy(
                        meals = dayMeals.meals.toMutableList().apply {
                            add(Meal(MealType.CUSTOM, "Nouveau repas"))
                        }
                    )
                } else {
                    dayMeals
                }
            }
        }
    }

    fun updateMeal(day: String, mealIndex: Int, updatedMeal: Meal) {
        viewModelScope.launch {
            _weekMeals.value = _weekMeals.value.map { dayMeals ->
                if (dayMeals.day == day && dayMeals.meals.size > mealIndex) {
                    dayMeals.copy(
                        meals = dayMeals.meals.toMutableList().apply {
                            set(mealIndex, updatedMeal)
                        }
                    )
                } else {
                    dayMeals
                }
            }
        }
    }
    fun removeMeal(day: String, index: Int) {
        viewModelScope.launch {
            _weekMeals.value = _weekMeals.value.map { dayMeals ->
                if (dayMeals.day == day && dayMeals.meals.size > 1) { // Garder au moins 1 repas
                    dayMeals.copy(
                        meals = dayMeals.meals.toMutableList().apply {
                            removeAt(index)
                        }
                    )
                } else {
                    dayMeals
                }
            }
        }
    }
}