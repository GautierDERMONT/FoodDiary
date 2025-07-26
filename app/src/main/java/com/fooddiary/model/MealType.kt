package com.fooddiary.model

enum class MealType {
    BREAKFAST, SNACK, LUNCH, DINNER, CUSTOM;

    fun toFrenchString(): String {
        return when (this) {
            BREAKFAST -> "Petit-déj"
            SNACK -> "Collation"
            LUNCH -> "Déjeuner"
            DINNER -> "Dîner"
            CUSTOM -> "Autre"
        }
    }
}