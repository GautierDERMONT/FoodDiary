package com.fooddiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fooddiary.model.MealType

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val mealIndex: Int,
    val type: MealType,
    val description: String,
    val photoUri: String?,
    val notes: String?,
    val weekNumber: Int

)