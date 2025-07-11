package com.fooddiary.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE day = :day ORDER BY mealIndex")
    fun getMealsByDay(day: String): Flow<List<MealEntity>>

    @Query("DELETE FROM meals WHERE mealIndex >= 3")
    suspend fun deleteAllExtraMeals()

    @Query("DELETE FROM meals WHERE day = :day AND mealIndex > 2 AND description = '' AND photoUri IS NULL")
    suspend fun cleanExtraViergeMeals(day: String)

    @Query("DELETE FROM meals WHERE day = :day AND mealIndex = :mealIndex AND description = '' AND photoUri IS NULL")
    suspend fun deleteViergeMeal(day: String, mealIndex: Int)

    @Query("SELECT * FROM meals WHERE day = :day AND mealIndex >= 3 AND description = '' AND photoUri IS NULL ORDER BY mealIndex DESC LIMIT 1")
    suspend fun getLastViergeMeal(day: String): MealEntity?

    @Query("DELETE FROM meals WHERE day = :day AND mealIndex = :mealIndex")
    suspend fun deleteMealAt(day: String, mealIndex: Int)

    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()

    @Query("SELECT * FROM meals")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Update
    suspend fun update(meal: MealEntity)

    @Delete
    suspend fun delete(meal: MealEntity)
}