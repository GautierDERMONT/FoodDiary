package com.fooddiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fooddiary.screens.HomeScreen  // Importez votre écran
import com.fooddiary.ui.theme.FoodDiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodDiaryTheme {
                HomeScreen()  // Affichez directement HomeScreen
            }
        }
    }
}