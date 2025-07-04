package com.fooddiary.model

data class Meal(
    val type: MealType,
    var description: String = "",
    var photoUri: String? = null
)