package com.fooddiary.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.fooddiary.FoodDiaryApplication
import com.fooddiary.R
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import com.fooddiary.viewmodel.MealViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalFocusManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    navController: NavController,
    day: String,
    mealIndex: Int,
    viewModel: MealViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val existingMeal by remember(day, mealIndex) {
        derivedStateOf {
            viewModel.weekMeals.value
                .find { it.day == day }
                ?.meals?.getOrNull(mealIndex)
                ?.takeIf { it.description.isNotBlank() || it.photoUri != null }
        }
    }

    val isEditMode = existingMeal != null
    var selectedMealType by remember {
        mutableStateOf<MealType?>(existingMeal?.type)
    }
    var description by remember { mutableStateOf(existingMeal?.description ?: "") }
    var notes by remember { mutableStateOf(existingMeal?.notes ?: "") }
    var photoUri by remember { mutableStateOf<Uri?>(existingMeal?.photoUri?.let { Uri.parse(it) }) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

    fun createNewImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir("images")
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).also { currentPhotoFile = it }
    }

    fun getPhotoUri(): Uri {
        currentPhotoFile?.delete()
        val newFile = createNewImageFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            newFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = currentPhotoFile?.let { Uri.fromFile(it) }
        } else {
            currentPhotoFile?.delete()
            currentPhotoFile = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = getPhotoUri()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission nécessaire pour utiliser la caméra", Toast.LENGTH_SHORT).show()
        }
    }

    fun takePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                val uri = getPhotoUri()
                cameraLauncher.launch(uri)
            }
            (context as Activity).shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showErrorDialog = true
                errorMessage = "L'application a besoin d'accéder à votre caméra"
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoUri = it }
    }

    fun saveImagePermanently(context: Context, tempUri: Uri): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_${timeStamp}_${System.currentTimeMillis()}.jpg"
        val storageDir = context.getExternalFilesDir(null) // Répertoire permanent

        return try {
            val inputStream = context.contentResolver.openInputStream(tempUri)
            val outputFile = File(storageDir, fileName)
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
            photoUri == null -> {
                errorMessage = "Veuillez ajouter une photo"
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
            title = { Text("Erreur") },
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
                title = { Text(if (isEditMode) "Modifier le repas" else "Ajouter un repas") },
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
                    .clickable { focusManager.clearFocus() }

            ) {
                Text(
                    text = "Type de repas",
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
                    if (photoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .size(800, 800)
                                    .build(),
                                imageLoader = (context.applicationContext as FoodDiaryApplication).imageLoader
                            ),
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
                            val permanentPhotoUri = photoUri?.let { uri ->
                                saveImagePermanently(context, uri)
                            }
                            viewModel.updateMeal(
                                day = day,
                                mealIndex = mealIndex,
                                meal = Meal(
                                    type = selectedMealType!!,
                                    description = description,
                                    photoUri = permanentPhotoUri?.toString(), // Utilisez l'URI permanent
                                    notes = notes.takeIf { it.isNotBlank() },
                                    mealIndex = mealIndex,
                                    day = day
                                )
                            )
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
                    Text(if (isEditMode) "Mettre à jour" else "Enregistrer")
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