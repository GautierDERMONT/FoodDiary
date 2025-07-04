package com.fooddiary.model

data class DayMeals(
    val day: String, // "Lun", "Mar", ...
    val meals: MutableList<Meal> = mutableListOf()
)
