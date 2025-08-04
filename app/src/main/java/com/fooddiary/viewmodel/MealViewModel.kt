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
import java.util.Calendar


class MealViewModel(
    private val database: AppDatabase,
    private val application: Application
) : ViewModel() {
    private val mealDao = database.mealDao()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val _dieticianEmail = MutableStateFlow(prefs.getString("dietician_email", "") ?: "")
    val dieticianEmail: StateFlow<String> = _dieticianEmail.asStateFlow()

    val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    private val _currentWeekOffset = MutableStateFlow(0)
    val currentWeekOffset: StateFlow<Int> = _currentWeekOffset.asStateFlow()

    companion object {
        const val MAX_WEEK_OFFSET = 5

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

    private val currentWeekNumber: Int
        get() {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, _currentWeekOffset.value)
            }
            return calendar.get(Calendar.WEEK_OF_YEAR)
        }

    val weekMeals: StateFlow<List<DayMeals>> = currentWeekOffset.flatMapLatest { _ ->
        mealDao.getAllMeals()
            .map { allMeals ->
                weekDays.map { day ->
                    val dbMeals = allMeals
                        .filter { it.day == day && it.weekNumber == currentWeekNumber }
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
                            dbMeals.add(index, Meal(type, mealIndex = index, day = day))
                        }
                    }

                    DayMeals(day = day, meals = dbMeals)
                }
            }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, weekDays.map { DayMeals(day = it, meals = mutableListOf()) })

    fun updateDieticianEmail(email: String) {
        viewModelScope.launch {
            _dieticianEmail.value = email
            prefs.edit().putString("dietician_email", email).apply()
        }
    }

    fun changeWeekOffset(offset: Int) {
        val newOffset = offset.coerceIn(-MAX_WEEK_OFFSET, 0)
        _currentWeekOffset.value = newOffset
    }

    fun clearAllData() {
        viewModelScope.launch {
            mealDao.deleteAllMeals()
        }
    }

    // Dans MealViewModel.kt
    fun clearCurrentWeekData() {
        viewModelScope.launch {
            mealDao.deleteMealsByWeekNumber(currentWeekNumber)
        }
    }

    fun clearAllWeeksData() {
        viewModelScope.launch {
            mealDao.deleteAllMeals()
        }
    }

    fun hasPreviousWeekMeals(): Flow<Boolean> {
        if (currentWeekOffset.value <= -MAX_WEEK_OFFSET) {
            return flowOf(false)
        }

        val previousWeekNumber = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, currentWeekOffset.value - 1)
        }.get(Calendar.WEEK_OF_YEAR)

        return mealDao.getAllMeals()
            .map { meals ->
                meals.any { it.weekNumber == previousWeekNumber }
            }
    }

    fun addEmptyMeal(day: String) {
        viewModelScope.launch {
            val existingMeals = mealDao.getMealsByDayAndWeek(day, currentWeekNumber).first()
            if (existingMeals.none { it.description.isEmpty() && it.photoUri == null }) {
                val nextIndex = (existingMeals.maxOfOrNull { it.mealIndex } ?: 2) + 1
                if (nextIndex < 8) {
                    mealDao.insert(
                        MealEntity(
                            day = day,
                            mealIndex = nextIndex,
                            type = MealType.CUSTOM,
                            description = "",
                            photoUri = null,
                            notes = null,
                            weekNumber = currentWeekNumber
                        )
                    )
                }
            }
        }
    }

    fun removeLastViergeMeal(day: String) {
        viewModelScope.launch {
            mealDao.getMealsByDayAndWeek(day, currentWeekNumber).first()
                .filter { it.mealIndex >= 3 && it.description.isEmpty() && it.photoUri == null }
                .maxByOrNull { it.mealIndex }
                ?.let { mealDao.delete(it) }
        }
    }

    fun getMeal(day: String, mealIndex: Int): Meal? {
        return weekMeals.value
            .find { it.day == day }
            ?.meals?.getOrNull(mealIndex)
    }

    fun deleteMeal(day: String, mealIndex: Int) {
        viewModelScope.launch {
            mealDao.deleteMealAt(day, currentWeekNumber, mealIndex)
        }
    }

    fun updateMeal(day: String, mealIndex: Int, meal: Meal) {
        viewModelScope.launch {
            val existingMeal = mealDao.getMealsByDayAndWeek(day, currentWeekNumber).first()
                .find { it.mealIndex == mealIndex }

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
                        notes = meal.notes,
                        weekNumber = currentWeekNumber
                    )
                )
            }
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
}