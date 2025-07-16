package com.fooddiary.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.fooddiary.model.Meal
import com.fooddiary.viewmodel.MealViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fooddiary.utils.getFormattedDate
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapScreen(
    navController: NavController,
    viewModel: MealViewModel
) {
    val weekMeals by viewModel.weekMeals.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Récapitulatif hebdomadaire",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Utilisation d'un LazyColumn pour un scroll fluide
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(weekMeals) { dayMeals ->
                Column {
                    DayHeader(day = dayMeals.day, onClick = {
                        navController.navigate("dayMeals/${dayMeals.day}")
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                    DayRecap(meals = dayMeals.meals)
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
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        onClick = onClick
    ) {
        Text(
            text = getFormattedDate(day),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
        )
    }
}

@Composable
private fun DayRecap(
    meals: List<Meal>
) {
    val context = LocalContext.current

    fun isUriValid(uriString: String?): Boolean {
        if (uriString == null) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri) != null
        } catch (e: Exception) {
            false
        }
    }

    val validMeals = meals.filter {
        !it.isVierge() && it.photoUri != null && isUriValid(it.photoUri)
    }

    val rows = (validMeals.size + 3) / 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        repeat(rows) { rowIndex ->
            val start = rowIndex * 4
            val end = minOf(start + 4, validMeals.size)
            val rowMeals = validMeals.subList(start, end)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(17.dp)
            ) {
                rowMeals.forEach { meal ->
                    Image(
                        painter = rememberAsyncImagePainter(meal.photoUri),
                        contentDescription = "Photo du repas",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                repeat(4 - rowMeals.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}