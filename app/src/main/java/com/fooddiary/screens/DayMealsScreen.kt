package com.fooddiary.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.fooddiary.R
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import com.fooddiary.utils.getFormattedDate
import com.fooddiary.viewmodel.MealViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayMealsScreen(
    navController: NavController,
    day: String,
    viewModel: MealViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Récupérer le décalage de semaine actuel
    val currentWeekOffset by viewModel.currentWeekOffset.collectAsStateWithLifecycle()
    val weekMeals by viewModel.weekMeals.collectAsStateWithLifecycle()

    val filteredMeals = weekMeals
        .find { it.day == day }
        ?.meals
        ?.filterNot { it.description.isBlank() && it.photoUri == null }
        ?: emptyList()

    // Utiliser le décalage de semaine pour obtenir la bonne date
    val fullDate = getFormattedDate(day, currentWeekOffset)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Repas du $fullDate") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            if (filteredMeals.size < 8) {
                FloatingActionButton(
                    onClick = {
                        val newIndex = (filteredMeals.maxByOrNull { it.mealIndex }?.mealIndex ?: -1) + 1
                        navController.navigate("addMeal/$day/$newIndex")
                    },
                    modifier = Modifier.size(64.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    content = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ajouter un repas",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            }
        },
        content = { innerPadding ->
            if (filteredMeals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun repas enregistré pour ce jour")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMeals) { meal ->
                        MealCard(
                            meal = meal,
                            onClick = { navController.navigate("mealDetail/$day/${meal.mealIndex}") },
                            onEdit = { navController.navigate("addMeal/$day/${meal.mealIndex}") },
                            onDelete = { viewModel.deleteMeal(day, meal.mealIndex) }
                        )
                        if (meal != filteredMeals.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MealCard(meal: Meal, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmation") },
            text = { Text("Êtes-vous sûr de vouloir supprimer ce repas ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    meal.photoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = "Photo du repas",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (meal.type) {
                                        MealType.BREAKFAST -> R.drawable.croissant
                                        MealType.LUNCH -> R.drawable.dish
                                        MealType.DINNER -> R.drawable.moon
                                        MealType.SNACK -> R.drawable.apple
                                        MealType.CUSTOM -> R.drawable.star
                                    }
                                ),
                                contentDescription = "Icône du repas",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (meal.type) {
                            MealType.BREAKFAST -> "Petit-déjeuner"
                            MealType.LUNCH -> "Déjeuner"
                            MealType.DINNER -> "Dîner"
                            MealType.SNACK -> "Collation"
                            MealType.CUSTOM -> "Autres"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (meal.description.isNotBlank()) {
                        Text(
                            text = meal.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    meal.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Column {
                            Text(
                                text = "Remarques:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { onEdit() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Modifier",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}