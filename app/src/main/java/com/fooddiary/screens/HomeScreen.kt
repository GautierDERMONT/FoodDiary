package com.fooddiary.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fooddiary.R
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import com.fooddiary.utils.getCurrentDayShort
import com.fooddiary.viewmodel.MealViewModel

@Composable
fun HomeScreen(
    viewModel: MealViewModel = viewModel(),
    onMealClick: (String, Int) -> Unit = { _, _ -> },
    onAddMealClick: (String, Int) -> Unit = { _, _ -> },
    onDayClick: (String) -> Unit = { _ -> },
    onRecapClick: () -> Unit = {}
) {
    val currentDay = remember { getCurrentDayShort() }
    val jours = viewModel.weekDays
    val weekMeals by viewModel.weekMeals.collectAsState()

    val mealsCount by remember(weekMeals) {
        derivedStateOf {

            weekMeals.associate { dayMeals ->
                dayMeals.day to dayMeals.meals.count { !it.isVierge() }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo), // Remplacez par le nom de votre fichier
            contentDescription = "FoodDiary Logo",
            modifier = Modifier
                .width(300.dp) // Ajustez la taille selon vos besoins
                .height(200.dp)
                .padding(bottom = 8.dp), // Réduire l'espace en dessous de la rangée

        contentScale = ContentScale.Fit
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable { }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_export),
                        contentDescription = "Export",
                        modifier = Modifier.size(30.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Export", style = MaterialTheme.typography.labelSmall)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable { onRecapClick() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_recap),
                        contentDescription = "Recap",
                        modifier = Modifier.size(30.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Recap", style = MaterialTheme.typography.labelSmall)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var showResetDialog by remember { mutableStateOf(false) }

                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = { Text("Confirmation") },
                        text = { Text("Êtes-vous sûr de vouloir supprimer toutes les données ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.clearAllData()
                                    showResetDialog = false
                                }
                            ) {
                                Text("Confirmer", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showResetDialog = false }
                            ) {
                                Text("Annuler", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable { showResetDialog = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_reset),
                        contentDescription = "Reset",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Reset", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .padding(horizontal = 32.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    jours.forEach { jour ->
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .background(
                                    if (jour == currentDay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    2.dp,
                                    if (jour == currentDay) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp)
                                .clickable { onDayClick(jour) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                jour,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (jour == currentDay) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekMeals.forEach { dayMeals ->
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .background(
                                    if (dayMeals.day == currentDay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (dayMeals.day == currentDay) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                dayMeals.meals
                                    .filter { !it.isVierge() }
                                    .sortedBy { it.mealIndex }
                                    .forEachIndexed { index, meal ->
                                        if (index > 0) Spacer(Modifier.height(12.dp))
                                        ClickableMealBox(
                                            meal = meal,
                                            onClick = { onMealClick(dayMeals.day, meal.mealIndex) }
                                        )
                                    }

                                if ((mealsCount[dayMeals.day] ?: 0) < 8) {
                                    if (dayMeals.meals.any { !it.isVierge() }) {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    AddMealButton(
                                        onClick = {
                                            val nextIndex = dayMeals.meals
                                                .maxOfOrNull { it.mealIndex }
                                                ?.plus(1) ?: 0
                                            onAddMealClick(dayMeals.day, nextIndex)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableMealBox(
    meal: Meal,
    onClick: () -> Unit
) {
    val iconRes by remember(meal.type) {
        derivedStateOf {
            when (meal.type) {
                MealType.BREAKFAST -> R.drawable.croissant
                MealType.LUNCH -> R.drawable.dish
                MealType.DINNER -> R.drawable.moon
                MealType.SNACK -> R.drawable.apple
                MealType.CUSTOM -> R.drawable.star
            }
        }
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = meal.type.name,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AddMealButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Ajouter un repas",
            tint = Color.LightGray
        )
    }
}