package com.fooddiary.screens  // Package cohérent

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.fooddiary.ui.theme.FoodDiaryTheme  // Import du thème

@Composable
fun HomeScreen() {
    Text("Bienvenue dans FoodDiary !")  // Contenu simplifié
}

@Preview
@Composable
fun Preview() {
    FoodDiaryTheme {
        HomeScreen()
    }
}