package com.fooddiary.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fooddiary.data.AppDatabase
import com.fooddiary.data.MealEntity
import com.fooddiary.model.DayMeals
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MealViewModel(private val database: AppDatabase) : ViewModel() {
    private val mealDao = database.mealDao()

    val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    val weekMeals: StateFlow<List<DayMeals>> = mealDao.getAllMeals()
        .map { allMeals ->
            weekDays.map { day ->
                val dayMeals = allMeals
                    .filter { it.day == day }
                    .sortedBy { it.mealIndex }
                    .map { it.toMeal() }
                    .toMutableList()

                // Garantit les 3 repas de base
                while (dayMeals.size < 3) {
                    dayMeals.add(Meal(MealType.values()[dayMeals.size]))
                }

                DayMeals(day = day, meals = dayMeals)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialWeekMeals())

    private fun initialWeekMeals(): List<DayMeals> {
        return weekDays.map { day ->
            DayMeals(
                day = day,
                meals = mutableListOf(
                    Meal(MealType.BREAKFAST),
                    Meal(MealType.LUNCH),
                    Meal(MealType.DINNER)
                )
            )
        }
    }

    fun removeViergeMeal(day: String, mealIndex: Int) {
        viewModelScope.launch {
            val meals = mealDao.getMealsByDay(day).first()
            if (meals.size <= 3) return@launch // Ne pas supprimer les 3 repas de base

            // Supprime seulement le repas vierge à l'index spécifié
            meals.firstOrNull { it.mealIndex == mealIndex && it.description.isEmpty() && it.photoUri == null }
                ?.let { mealDao.delete(it) }

            // Réindexation
            val remainingMeals = mealDao.getMealsByDay(day).first()
            remainingMeals.forEachIndexed { newIndex, meal ->
                if (meal.mealIndex != newIndex) {
                    mealDao.insert(meal.copy(mealIndex = newIndex))
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            mealDao.deleteAllMeals()
        }
    }


    fun addEmptyMeal(day: String) {
        viewModelScope.launch {
            val existingMeals = mealDao.getMealsByDay(day).first()

            // Ajoute toujours après les 3 repas de base
            val newIndex = maxOf(3, existingMeals.size)

            mealDao.insert(
                MealEntity(
                    day = day,
                    mealIndex = newIndex,
                    type = MealType.CUSTOM,
                    description = "",
                    photoUri = null,
                    notes = null
                )
            )
        }
    }

    fun removeLastViergeMeal(day: String) {
        viewModelScope.launch {
            val lastVierge = mealDao.getLastViergeMeal(day)
            lastVierge?.let { mealDao.delete(it) }
        }
    }

    val canRemoveVierge: StateFlow<Map<String, Boolean>> = mealDao.getAllMeals()
        .map { allMeals ->
            weekDays.associate { day ->
                day to allMeals.any {
                    it.day == day &&
                            it.mealIndex >= 3 &&
                            it.description.isEmpty() &&
                            it.photoUri == null
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, weekDays.associate { it to false })

    fun updateMeal(day: String, mealIndex: Int, meal: Meal) {
        viewModelScope.launch {
            // D'abord supprimer l'ancien repas s'il existe
            mealDao.deleteMealAt(day, mealIndex)

            // Puis insérer le nouveau avec le même index
            mealDao.insert(
                MealEntity(
                    day = day,
                    mealIndex = mealIndex, // Conserve le même index
                    type = meal.type,
                    description = meal.description,
                    photoUri = meal.photoUri,
                    notes = meal.notes
                )
            )
        }
    }

    private fun MealEntity.toMeal(): Meal {
        return Meal(
            type = type,
            description = description,
            photoUri = photoUri,
            notes = notes
        )
    }

    companion object {
        class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
                    return MealViewModel(database) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}