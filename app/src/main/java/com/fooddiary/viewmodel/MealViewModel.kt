package com.fooddiary.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.fooddiary.data.AppDatabase
import com.fooddiary.data.MealEntity
import com.fooddiary.model.DayMeals
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MealViewModel(
    private val database: AppDatabase,
    private val application: Application
) : ViewModel() {
    private val mealDao = database.mealDao()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val _dieticianEmail = MutableStateFlow(prefs.getString("dietician_email", "") ?: "")
    val dieticianEmail: StateFlow<String> = _dieticianEmail.asStateFlow()

    val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    val weekMeals: StateFlow<List<DayMeals>> = mealDao.getAllMeals()
        .map { allMeals ->
            weekDays.map { day ->
                val dbMeals = allMeals
                    .filter { it.day == day }
                    .sortedBy { it.mealIndex }
                    .map { it.toMeal() }
                    .toMutableList()

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

    fun updateDieticianEmail(email: String) {
        viewModelScope.launch {
            _dieticianEmail.value = email
            prefs.edit().putString("dietician_email", email).apply()
        }
    }

    private fun initialWeekMeals(): List<DayMeals> {
        return weekDays.map { day ->
            DayMeals(day = day, meals = mutableListOf())
        }
    }

    fun removeViergeMeal(day: String, mealIndex: Int) {
        viewModelScope.launch {
            val meals = mealDao.getMealsByDay(day).first()
            if (meals.size <= 3) return@launch

            meals.firstOrNull { it.mealIndex == mealIndex && it.description.isEmpty() && it.photoUri == null }
                ?.let { mealDao.delete(it) }

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
            if (existingMeals.none { it.description.isEmpty() && it.photoUri == null }) {
                val usedIndices = existingMeals.map { it.mealIndex }.toSet()
                var nextIndex = 3
                while (nextIndex in usedIndices && nextIndex < 8) {
                    nextIndex++
                }

                if (nextIndex < 8) {
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

    fun canAddMoreMeals(day: String): Flow<Boolean> = mealDao.getMealsByDay(day)
        .map { it.size < 8 }
        .flowOn(Dispatchers.Default)

    fun getMeal(day: String, mealIndex: Int): Meal? {
        return weekMeals.value
            .find { it.day == day }
            ?.meals?.getOrNull(mealIndex)
    }

    fun deleteMeal(day: String, mealIndex: Int) {
        viewModelScope.launch {
            mealDao.deleteMealAt(day, mealIndex)
            weekMeals.value
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
            mealDao.getMealsByDay(day).first()
        }
    }

    private fun MealEntity.toMeal(): Meal {
        return Meal(
            type = type,
            description = description,
            photoUri = photoUri,
            notes = notes,
            mealIndex = mealIndex,
            day = day
        )
    }

    companion object {
        class Factory(
            private val database: AppDatabase,
            private val application: Application
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
                    return MealViewModel(database, application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}