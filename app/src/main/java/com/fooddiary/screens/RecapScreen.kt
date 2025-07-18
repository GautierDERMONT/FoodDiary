package com.fooddiary.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.fooddiary.R
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import com.fooddiary.viewmodel.MealViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fooddiary.utils.getFormattedDate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import kotlin.math.min
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapScreen(
    navController: NavController,
    viewModel: MealViewModel
) {
    val weekMeals by viewModel.weekMeals.collectAsStateWithLifecycle()
    var selectedMealType by remember { mutableStateOf<MealType?>(null) }
    val hasFilteredMeals = remember(weekMeals, selectedMealType) {
        weekMeals.any { dayMeals ->
            dayMeals.meals.any { meal ->
                !meal.isVierge() &&
                        meal.photoUri != null &&
                        (selectedMealType == null || meal.type == selectedMealType)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Récapitulatif hebdomadaire",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Filtres:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MealType.entries.forEach { type ->
                        MealTypeFilterOption(
                            type = type,
                            isSelected = selectedMealType == type,
                            onSelect = { selectedMealType = if (selectedMealType == type) null else type }
                        )
                    }
                }
            }

            if (!hasFilteredMeals) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedMealType != null) {
                        Text(
                            text = buildAnnotatedString {
                                append("Aucun repas de type ")
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(selectedMealType?.name?.lowercase() ?: "")
                                }
                                append(" cette semaine")
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = "Aucun repas enregistré cette semaine",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(weekMeals) { dayMeals ->
                        val filteredMeals = dayMeals.meals.filter {
                            !it.isVierge() &&
                                    it.photoUri != null &&
                                    (selectedMealType == null || it.type == selectedMealType)
                        }

                        if (filteredMeals.isNotEmpty()) {
                            Column {
                                DayHeader(
                                    day = dayMeals.day,
                                    onClick = { navController.navigate("dayMeals/${dayMeals.day}") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StableDayRecap(
                                    meals = filteredMeals,
                                    day = dayMeals.day,
                                    onMealClick = { meal ->
                                        val originalIndex = dayMeals.meals.indexOf(meal)
                                        navController.navigate("mealDetail/${dayMeals.day}/$originalIndex")
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

@Composable
private fun StableDayRecap(
    meals: List<Meal>,
    day: String,
    onMealClick: (Meal) -> Unit // Passer l'objet Meal directement
) {
    val context = LocalContext.current
    val validMeals = remember(meals) {
        meals.filter { !it.isVierge() && it.photoUri != null }
    }
    val rowHeight = 96.dp
    val imageSpacing = 8.dp
    val rows = remember(validMeals.size) { (validMeals.size + 3) / 4 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = (rows * rowHeight.value.toInt()).dp),
        verticalArrangement = Arrangement.spacedBy(imageSpacing)
    ) {
        for (i in 0 until rows) {
            val start = i * 4
            val end = min(start + 4, validMeals.size)
            val rowItems = validMeals.subList(start, end)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(imageSpacing)
            ) {
                for (meal in rowItems) {
                    Image(
                        painter = rememberAsyncImagePainter(meal.photoUri),
                        contentDescription = "Photo du repas",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMealClick(meal) }, // Passer l'objet Meal
                        contentScale = ContentScale.Crop
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxSize())
                }
            }
        }
    }
}
@Composable
private fun DayHeader(
    day: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp) // Hauteur fixe
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = getFormattedDate(day),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                painter = painterResource(R.drawable.ic_eye),
                contentDescription = "Voir les repas du jour",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MealTypeFilterOption(
    type: MealType,
    isSelected: Boolean,
    onSelect: (MealType) -> Unit
) {
    val (iconRes, label) = when (type) {
        MealType.BREAKFAST -> Pair(R.drawable.croissant, "Petit-déj")
        MealType.SNACK -> Pair(R.drawable.apple, "Collation")
        MealType.LUNCH -> Pair(R.drawable.dish, "Déjeuner")
        MealType.DINNER -> Pair(R.drawable.moon, "Dîner")
        MealType.CUSTOM -> Pair(R.drawable.star, "Autre")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onSelect(type) }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}