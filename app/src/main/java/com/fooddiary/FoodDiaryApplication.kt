// FoodDiaryApplication.kt
package com.fooddiary

import android.app.Application
import com.fooddiary.data.AppDatabase

class FoodDiaryApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}