package com.fooddiary.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.fooddiary.R
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import com.fooddiary.viewmodel.MealViewModel
import java.io.File
import java.io.FileOutputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    navController: NavController,
    day: String,
    mealIndex: Int,
    viewModel: MealViewModel
) {
    val existingMeal by remember(day, mealIndex) {
        derivedStateOf {
            viewModel.weekMeals.value
                .find { it.day == day }
                ?.meals?.getOrNull(mealIndex)
        }
    }

    val isEditMode = existingMeal != null
    var selectedMealType by remember {
        mutableStateOf(
            if (isEditMode) existingMeal?.type
            else null
        )
    }
    var description by remember { mutableStateOf(existingMeal?.description ?: "") }
    var notes by remember { mutableStateOf(existingMeal?.notes ?: "") }
    var photoUri by remember { mutableStateOf<Uri?>(existingMeal?.photoUri?.let { Uri.parse(it) }) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    val title = if (isEditMode) "Modifier le repas" else "Ajouter un repas"
    val buttonText = if (isEditMode) "Mettre à jour" else "Enregistrer"

    fun isUriValid(context: Context, uriString: String?): Boolean {
        if (uriString == null) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri) != null
        } catch (e: Exception) {
            false
        }
    }

    fun copyImageToAppStorage(context: Context, uri: Uri): Uri {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return uri
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            val outputFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            uri // Retourne l'URI original en cas d'erreur
        }
    }



    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val copiedUri = copyImageToAppStorage(context, it)
                    photoUri = copiedUri
                } catch (e: Exception) {
                    e.printStackTrace()
                    photoUri = uri // Utilise l'URI original si la copie échoue
                }
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri?.let { }
    }



    fun takePhoto() {
        val uri = createImageUri(context)
        photoUri = uri
        cameraLauncher.launch(uri)
    }

    fun validateFields(): Boolean {
        return when {
            selectedMealType == null -> {
                errorMessage = "Veuillez sélectionner un type de repas"
                false
            }
            description.isBlank() -> {
                errorMessage = "Veuillez saisir une description"
                false
            }
            photoUri == null || !isUriValid(context, photoUri.toString()) -> {
                errorMessage = "Veuillez ajouter une photo valide"
                false
            }
            else -> true
        }
    }

    if (showPhotoPicker) {
        AlertDialog(
            onDismissRequest = { showPhotoPicker = false },
            title = { Text("Choisir une photo") },
            text = { Text("Sélectionnez la source de la photo") },
            confirmButton = {
                TextButton(onClick = {
                    galleryLauncher.launch("image/*")
                    showPhotoPicker = false
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddPhotoAlternate, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Galerie")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    takePhoto()
                    showPhotoPicker = false
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Caméra")
                    }
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Champs obligatoires") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Type de repas*",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MealType.entries.forEach { type ->
                        MealTypeOption(
                            type = type,
                            isSelected = selectedMealType == type,
                            onSelect = { selectedMealType = it }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Photo du repas*",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { showPhotoPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null && isUriValid(context, photoUri.toString())) {
                        Image(
                            painter = rememberAsyncImagePainter(photoUri),
                            contentDescription = "Photo du repas",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Ajouter photo",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ajouter une photo",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarques (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "* Champs obligatoires",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        if (validateFields()) {
                            val meal = Meal(
                                type = selectedMealType!!,
                                description = description,
                                photoUri = photoUri?.toString(),
                                notes = notes.takeIf { it.isNotBlank() }
                            )
                            viewModel.updateMeal(day, mealIndex, meal)
                            navController.popBackStack()
                        } else {
                            showErrorDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    )
}

@Composable
fun MealTypeOption(
    type: MealType,
    isSelected: Boolean,
    onSelect: (MealType) -> Unit
) {
    val (iconRes, label) = when (type) {
        MealType.BREAKFAST -> Pair(R.drawable.croissant, "Petit-déj")
        MealType.SNACK -> Pair(R.drawable.apple, "Collation")
        MealType.LUNCH -> Pair(R.drawable.dish, "Déjeuner")
        MealType.DINNER -> Pair(R.drawable.moon, "Dîner")
        else -> Pair(R.drawable.star, "Autre")
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
                .size(64.dp)
                .background(
                    color = Color.Transparent,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.LightGray,
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "meal_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}