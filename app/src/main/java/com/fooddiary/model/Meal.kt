package com.fooddiary.model

data class Meal(
    val type: MealType,
    var description: String = "",
    var photoUri: String? = null,
    var notes: String? = null, // Bien séparé de la description
    val mealIndex: Int = 0,
    val day: String = ""
) {
    fun isVierge(): Boolean = description.isBlank() && photoUri == null
}