package com.fooddiary.model

data class Meal(
    val type: MealType,
    var description: String = "",
    var photoUri: String? = null,
    var notes: String? = null
) {
    fun isVierge(): Boolean = description.isBlank() && photoUri == null
}