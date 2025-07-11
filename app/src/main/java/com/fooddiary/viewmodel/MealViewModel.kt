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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class MealViewModel(private val database: AppDatabase) : ViewModel() {
    private val mealDao = database.mealDao()

    val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    val weekMeals: StateFlow<List<DayMeals>> = mealDao.getAllMeals()
        .map { allMeals ->
            weekDays.map { day ->
                val dbMeals = allMeals
                    .filter { it.day == day }
                    .sortedBy { it.mealIndex }
                    .map { it.toMeal() }
                    .toMutableList()

                // Ensure base meals exist
                (0..2).forEach { index ->
                    if (dbMeals.none { it.mealIndex == index }) {
                        val type = when(index) {
                            0 -> MealType.BREAKFAST
                            1 -> MealType.LUNCH
                            else -> MealType.DINNER
                        }
                        dbMeals.add(index, Meal(type, mealIndex = index))
                    }
                }

                DayMeals(day = day, meals = dbMeals)
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, initialWeekMeals())

    private fun initialWeekMeals(): List<DayMeals> {
        return weekDays.map { day ->
            DayMeals(day = day, meals = mutableListOf())
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

            // On détermine le prochain index disponible (minimum 3)
            val nextIndex = maxOf(
                existingMeals.maxOfOrNull { it.mealIndex }?.plus(1) ?: 3, // Commence à 3 si liste vide
                3 // Garantit qu'on ne descend pas en dessous de 3
            )

            if (nextIndex >= 8) return@launch // Limite à 8 repas max

            mealDao.insert(
                MealEntity(
                    day = day,
                    mealIndex = nextIndex,
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
            val existingMeals = mealDao.getMealsByDay(day).first()
            val lastVierge = existingMeals
                .filter { it.mealIndex >= 3 && it.description.isEmpty() && it.photoUri == null }
                .maxByOrNull { it.mealIndex }

            lastVierge?.let { mealDao.delete(it) }
        }
    }


    fun getMeal(day: String, mealIndex: Int): Meal? {
        return weekMeals.value
            .find { it.day == day }
            ?.meals?.getOrNull(mealIndex)
    }

    fun deleteMeal(day: String, mealIndex: Int) {
        viewModelScope.launch {
            mealDao.deleteMealAt(day, mealIndex)
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
            val existingMeal = mealDao.getMealsByDay(day).first().find { it.mealIndex == mealIndex }

            if (existingMeal != null) {
                mealDao.update(
                    existingMeal.copy(
                        type = meal.type,
                        description = meal.description,
                        photoUri = meal.photoUri,
                        notes = meal.notes
                    )
                )
            } else {
                mealDao.insert(
                    MealEntity(
                        day = day,
                        mealIndex = mealIndex,
                        type = meal.type,
                        description = meal.description,
                        photoUri = meal.photoUri,
                        notes = meal.notes
                    )
                )
            }

            // Force un refresh du Flow
            mealDao.getMealsByDay(day).first()
        }
    }

    private fun MealEntity.toMeal(): Meal {
        return Meal(
            type = type,
            description = description,
            photoUri = photoUri,
            notes = notes,
            mealIndex = mealIndex, // Ajoutez cette ligne
            day = day // Ajoutez cette ligne
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