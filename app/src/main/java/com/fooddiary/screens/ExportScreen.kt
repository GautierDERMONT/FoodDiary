package com.fooddiary.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.fooddiary.R
import com.fooddiary.viewmodel.MealViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
    viewModel: MealViewModel
) {
    var exportFormat by remember { mutableStateOf("PDF") }
    val formats = listOf("PDF", "Image")
    var dieticianEmail by remember { mutableStateOf(viewModel.dieticianEmail.value) }
    var sendToDietician by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf("") }

    LaunchedEffect(dieticianEmail) {
        viewModel.updateDieticianEmail(dieticianEmail)
    }

    if (showSaveSuccess) {
        LaunchedEffect(Unit) {
            delay(3000)
            showSaveSuccess = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Exporter",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },                navigationIcon = {
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
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Prévisualisation du récapitulatif", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Format d'exportation:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                formats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (format == exportFormat),
                                onClick = { exportFormat = format }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (format == exportFormat),
                            onClick = { exportFormat = format }
                        )
                        Text(
                            text = format,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sendToDietician,
                        onCheckedChange = { sendToDietician = it },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Envoyer à ma diététicienne", style = MaterialTheme.typography.bodyLarge)
                }

                if (sendToDietician) {
                    OutlinedTextField(
                        value = dieticianEmail,
                        onValueChange = { dieticianEmail = it },
                        label = { Text("Email de la diététicienne") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        val file = createExportFile(context, viewModel, exportFormat)
                        file?.let {
                            shareFile(context, it, exportFormat)
                            if (sendToDietician) {
                                sendToDietician(context, it, exportFormat, dieticianEmail)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "Partager",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Partager via...")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val file = createExportFile(context, viewModel, exportFormat)
                        file?.let {
                            saveFile(context, it, exportFormat)?.let { message ->
                                saveSuccessMessage = message
                                showSaveSuccess = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_save),
                            contentDescription = "Enregistrer",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Enregistrer une copie")
                    }
                }
            }

            if (showSaveSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = saveSuccessMessage)
                    }
                }
            }
        }
    }
}

private fun createExportFile(context: Context, viewModel: MealViewModel, format: String): File? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "FoodDiary_$timeStamp.${format.lowercase()}"

        val storageDir = File(context.cacheDir, "exports")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        File(storageDir, fileName).apply {
            createNewFile()
            when (format) {
                "PDF" -> createPdf(this, viewModel)
                "Image" -> createImage(this, viewModel, context)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createPdf(file: File, viewModel: MealViewModel): Boolean {
    return try {
        file.writeText("PDF Content")
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun createImage(file: File, viewModel: MealViewModel, context: Context): Boolean {
    return try {
        file.writeText("Image Content")
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun shareFile(context: Context, file: File, format: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = when (format) {
                "PDF" -> "application/pdf"
                else -> "image/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur lors du partage: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun sendToDietician(context: Context, file: File, format: String, email: String) {
    if (email.isBlank()) {
        Toast.makeText(context, "Veuillez entrer un email valide", Toast.LENGTH_SHORT).show()
        return
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, "Mon carnet alimentaire")
        putExtra(Intent.EXTRA_TEXT, "Voici mon carnet alimentaire comme convenu.")
        putExtra(Intent.EXTRA_STREAM, uri)
        type = when (format) {
            "PDF" -> "application/pdf"
            else -> "image/png"
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(Intent.createChooser(emailIntent, "Envoyer à la diététicienne"))
    } catch (e: Exception) {
        Toast.makeText(context, "Aucune application email trouvée", Toast.LENGTH_SHORT).show()
    }
}

private fun saveFile(context: Context, file: File, format: String): String? {
    return try {
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        destinationDir.mkdirs()

        val destinationFile = File(destinationDir, file.name)
        file.copyTo(destinationFile, overwrite = true)

        "Fichier enregistré dans Documents/${file.name}"
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}