package com.fooddiary.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fooddiary.model.MealType  // Vérifiez le chemin exact
import com.fooddiary.viewmodel.MealViewModel  // Vérifiez le chemin exact

@Composable
fun HomeScreen(viewModel: MealViewModel = viewModel()) {
    val jours = viewModel.weekDays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FoodDiary", style = MaterialTheme.typography.headlineMedium)
        Text("Aperçu hebdomadaire", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            jours.forEach { jour ->
                Text(jour, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        for (i in MealType.values().indices) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                jours.forEach { jour ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.LightGray, CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍽")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            jours.forEach { _ ->
                Button(
                    onClick = { },
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+")
                }
            }
        }
    }
}