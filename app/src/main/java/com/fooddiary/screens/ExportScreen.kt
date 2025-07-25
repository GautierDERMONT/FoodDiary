package com.fooddiary.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.fooddiary.R
import com.fooddiary.model.DayMeals
import com.fooddiary.viewmodel.MealViewModel
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
    viewModel: MealViewModel
) {
    val scope = rememberCoroutineScope()
    var exportFormat by remember { mutableStateOf("PDF") }
    val formats = listOf("PDF", "Image")
    var dieticianEmail by remember { mutableStateOf(viewModel.dieticianEmail.value) }
    var sendToDietician by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf("") }
    val weekMeals by viewModel.weekMeals.collectAsState()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(weekMeals, exportFormat) { // Retirez isDarkTheme des dépendances
        isLoading = true
        previewBitmap = generatePreviewBitmap(
            weekMeals = weekMeals,
            context = context,
            format = exportFormat,
            isDarkTheme = false, // Forcer false
            forPreview = true
        )
        isLoading = false
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exporter",  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> CircularProgressIndicator()
                            previewBitmap != null -> Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            else -> Text("Aucune donnée à afficher")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Format d'exportation:", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    formats.forEach { format ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.selectable(
                                selected = (format == exportFormat),
                                onClick = { exportFormat = format }
                            )
                        ) {
                            RadioButton(
                                selected = (format == exportFormat),
                                onClick = { exportFormat = format }
                            )
                            Text(text = format)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = sendToDietician,
                            onCheckedChange = { sendToDietician = it }
                        )
                        Text("Envoyer à ma diététicienne")
                    }

                    if (sendToDietician) {
                        OutlinedTextField(
                            value = dieticianEmail,
                            onValueChange = { dieticianEmail = it },
                            label = { Text("Email de la diététicienne") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val file = createExportFile(context, weekMeals, exportFormat)
                                file?.let {
                                    shareFile(context, it, exportFormat)
                                    if (sendToDietician && dieticianEmail.isNotBlank()) {
                                        sendToDietician(context, it, exportFormat, dieticianEmail)
                                    }
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Partager")
                    }

                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val file = createExportFile(context, weekMeals, exportFormat)
                                file?.let {
                                    saveSuccessMessage = saveFile(context, it, exportFormat)
                                        ?: "Erreur lors de l'enregistrement"
                                    showSaveSuccess = true
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Enregistrer")
                    }
                }
            }

            if (showSaveSuccess) {
                LaunchedEffect(Unit) {
                    delay(3000)
                    showSaveSuccess = false
                }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(saveSuccessMessage)
                }
            }
        }
    }
}

private fun generatePreviewBitmap(
    weekMeals: List<DayMeals>,
    context: Context,
    format: String,
    isDarkTheme: Boolean,
    forPreview: Boolean = true
): Bitmap? {
    if (weekMeals.isEmpty() || weekMeals.all { it.meals.isEmpty() }) return null

    return try {
        val density = context.resources.displayMetrics.density
        val width = (300 * density).toInt()
        val height = (400 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }

        var yPos = 20f * density

        // Titre
        paint.textSize = 16f * density
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Prévisualisation", 20f * density, yPos, paint)
        yPos += 25f * density

        // Date
        paint.textSize = 12f * density
        paint.typeface = Typeface.DEFAULT
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(date, 20f * density, yPos, paint)
        yPos += 30f * density

        // Mini tableau
        val columnWidth = (width - 40 * density) / 3f

        // En-têtes
        paint.textSize = 12f * density
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Jour", 20f * density, yPos, paint)
        canvas.drawText("Repas", 20f * density + columnWidth, yPos, paint)
        canvas.drawText("Desc.", 20f * density + columnWidth * 2, yPos, paint)
        yPos += 20f * density

        // Ligne de séparation
        paint.strokeWidth = 1f * density
        canvas.drawLine(20f * density, yPos, width - 20f * density, yPos, paint)
        yPos += 10f * density

        // Contenu
        paint.textSize = 10f * density
        paint.typeface = Typeface.DEFAULT

        weekMeals.take(2).forEach { dayMeals ->
            dayMeals.meals.take(2).forEach { meal ->
                if (!meal.isVierge()) {
                    canvas.drawText(dayMeals.day, 20f * density, yPos, paint)
                    canvas.drawText(meal.type.toString(), 20f * density + columnWidth, yPos, paint)
                    canvas.drawText(meal.description.take(15) + if (meal.description.length > 15) "..." else "",
                        20f * density + columnWidth * 2, yPos, paint)
                    yPos += 20f * density
                }
            }
        }

        if (yPos >= height - 30f * density) {
            paint.textSize = 10f * density
            canvas.drawText("...", 20f * density, height - 20f * density, paint)
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private suspend fun createExportFile(
    context: Context,
    weekMeals: List<DayMeals>,
    format: String
): File? = withContext(Dispatchers.IO) {
    try {
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports").apply {
            mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "FoodDiary_$timeStamp.${format.lowercase()}"
        val file = File(exportDir, fileName)

        when (format) {
            "PDF" -> createPdf(file, weekMeals, context)
            "Image" -> createImage(file, weekMeals, context)
            else -> false
        }

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createSquareBitmap(context: Context, source: Bitmap, targetSize: Int): Bitmap {
    val size = minOf(source.width, source.height)
    val x = (source.width - size) / 2
    val y = (source.height - size) / 2

    val squareBitmap = Bitmap.createBitmap(source, x, y, size, size)
    return Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
}

private suspend fun createPdf(file: File, weekMeals: List<DayMeals>, context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        PdfWriter(file).use { writer ->
            PdfDocument(writer).use { pdfDocument ->
                Document(pdfDocument, PageSize.A4).use { document ->
                    document.setMargins(40f, 40f, 40f, 40f)

                    document.add(
                        Paragraph("Récapitulatif des repas")
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(18f)
                            .setBold()
                            .setMarginBottom(10f)
                    )

                    document.add(
                        Paragraph(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(12f)
                            .setMarginBottom(20f)
                    )

                    val table = com.itextpdf.layout.element.Table(4)
                        .useAllAvailableWidth()
                        .setMarginTop(10f)

                    val headerStyle = com.itextpdf.layout.Style()
                        .setBold()
                        .setFontSize(12f)
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)

                    table.addHeaderCell(Paragraph("Jour").addStyle(headerStyle))
                    table.addHeaderCell(Paragraph("Repas").addStyle(headerStyle))
                    table.addHeaderCell(Paragraph("Description").addStyle(headerStyle))
                    table.addHeaderCell(Paragraph("Photo").addStyle(headerStyle))

                    weekMeals.forEach { dayMeals ->
                        dayMeals.meals.forEach { meal ->
                            if (!meal.isVierge()) {
                                table.addCell(Paragraph(dayMeals.day).setPadding(5f))
                                table.addCell(Paragraph(meal.type.toString()).setPadding(5f))

                                val description = buildString {
                                    append(meal.description)
                                    if (!meal.notes.isNullOrBlank()) {
                                        append("\n\nNotes: ${meal.notes}")
                                    }
                                }
                                table.addCell(Paragraph(description).setPadding(5f))

                                val cell = com.itextpdf.layout.element.Cell()
                                meal.photoUri?.let { uriString ->
                                    try {
                                        val filePath = uriString.removePrefix("file://")
                                        val imageData = ImageDataFactory.create(filePath)
                                        cell.add(
                                            com.itextpdf.layout.element.Image(imageData)
                                                .setWidth(100f)
                                                .setHeight(100f)
                                        )
                                    } catch (e: Exception) {
                                        cell.add(Paragraph("(image)"))
                                    }
                                } ?: cell.add(Paragraph("-"))
                                table.addCell(cell.setPadding(5f))
                            }
                        }
                    }

                    document.add(table)
                }
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

private fun createSquareBitmap(source: Bitmap, targetSize: Int): Bitmap {
    val size = minOf(source.width, source.height)
    val x = (source.width - size) / 2
    val y = (source.height - size) / 2

    val squareBitmap = Bitmap.createBitmap(source, x, y, size, size)
    return Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
}

private suspend fun createImage(file: File, weekMeals: List<DayMeals>, context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val density = context.resources.displayMetrics.density
            val width = (800 * density).toInt()
            val height = calculateRequiredHeight(weekMeals, density) + 500 // Marge supplémentaire pour le tableau

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                isAntiAlias = true
            }

            var yPos = 50f * density

            // Titre
            paint.textSize = 24f * density
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Récapitulatif des repas", 40f * density, yPos, paint)
            yPos += 40f * density

            // Date
            paint.textSize = 16f * density
            paint.typeface = Typeface.DEFAULT
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            canvas.drawText(dateStr, 40f * density, yPos, paint)
            yPos += 40f * density

            // En-têtes du tableau
            paint.textSize = 14f * density
            paint.typeface = Typeface.DEFAULT_BOLD
            val columnWidth = (width - 80 * density) / 4f

            // Ligne de séparation
            paint.strokeWidth = 2f * density
            canvas.drawLine(40f * density, yPos, width - 40f * density, yPos, paint)
            yPos += 5f * density

            // Dessiner les en-têtes
            canvas.drawText("Jour", 40f * density, yPos, paint)
            canvas.drawText("Repas", 40f * density + columnWidth, yPos, paint)
            canvas.drawText("Description", 40f * density + columnWidth * 2, yPos, paint)
            canvas.drawText("Photo", 40f * density + columnWidth * 3, yPos, paint)
            yPos += 30f * density

            // Ligne de séparation
            canvas.drawLine(40f * density, yPos, width - 40f * density, yPos, paint)
            yPos += 15f * density

            // Contenu du tableau
            paint.textSize = 12f * density
            paint.typeface = Typeface.DEFAULT
            val rowPaint = Paint().apply {
                style = Paint.Style.FILL
            }

            weekMeals.forEach { dayMeals ->
                if (dayMeals.meals.any { !it.isVierge() }) {
                    dayMeals.meals.forEach { meal ->
                        if (!meal.isVierge()) {
                            // Couleur de fond alternée
                            rowPaint.color = if ((yPos / (30 * density)).toInt() % 2 == 0) {
                                android.graphics.Color.parseColor("#F5F5F5")
                            } else {
                                android.graphics.Color.WHITE
                            }

                            canvas.drawRect(
                                40f * density,
                                yPos - 15f * density,
                                width - 40f * density,
                                yPos + 60f * density,
                                rowPaint
                            )

                            // Jour
                            canvas.drawText(dayMeals.day, 40f * density, yPos, paint)

                            // Type de repas
                            canvas.drawText(meal.type.toString(), 40f * density + columnWidth, yPos, paint)

                            // Description + notes
                            val description = if (!meal.notes.isNullOrBlank()) {
                                "${meal.description}\nNotes: ${meal.notes}"
                            } else {
                                meal.description
                            }
                            drawMultilineText(canvas, description, 40f * density + columnWidth * 2, yPos, paint, columnWidth - 20 * density)

                            // Photo
                            meal.photoUri?.let { uriString ->
                                try {
                                    val uri = Uri.parse(uriString)
                                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                        val options = BitmapFactory.Options().apply {
                                            inSampleSize = 4
                                        }
                                        val mealBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                                        mealBitmap?.let {
                                            try {
                                                // Taille fixe pour les images dans l'export image (150x150)
                                                val squareBitmap = createSquareBitmap(it, (150 * density).toInt())

                                                canvas.drawBitmap(
                                                    squareBitmap,
                                                    null,
                                                    RectF(
                                                        40f * density + columnWidth * 3 + 10 * density,
                                                        yPos - 10f * density,
                                                        40f * density + columnWidth * 3 + 10 * density + 150 * density,
                                                        yPos - 10f * density + 150 * density
                                                    ),
                                                    null
                                                )
                                            } finally {
                                                it.recycle()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    canvas.drawText("-", 40f * density + columnWidth * 3, yPos, paint)
                                }
                            }
                            yPos += 70f * density // Hauteur de ligne
                        }
                    }
                }
            }

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

// Fonction helper pour dessiner du texte multiligne
private fun drawMultilineText(canvas: Canvas, text: String, x: Float, startY: Float, paint: Paint, maxWidth: Float) {
    var y = startY
    val lines = text.split("\n")
    val lineHeight = paint.textSize * 1.2f

    lines.forEach { line ->
        canvas.drawText(line, x, y, paint)
        y += lineHeight
    }
}

private fun calculateRequiredHeight(weekMeals: List<DayMeals>, density: Float): Int {
    var height = 150f * density

    weekMeals.forEach { dayMeals ->
        if (dayMeals.meals.any { !it.isVierge() }) {
            height += 50f * density

            dayMeals.meals.forEach { meal ->
                if (!meal.isVierge()) {
                    height += 25f * density

                    if (meal.photoUri != null) {
                        height += 200f * density
                    }

                    if (!meal.notes.isNullOrBlank()) {
                        height += 25f * density
                    }

                    height += 15f * density
                }
            }

            height += 20f * density
        }
    }

    return height.toInt()
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, when (format) {
                    "PDF" -> "application/pdf"
                    else -> "image/png"
                })
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/FoodDiary")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                "Fichier enregistré dans Documents/FoodDiary/${file.name}"
            } ?: run {
                null
            }
        } else {
            val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val foodDiaryDir = File(destinationDir, "FoodDiary").apply { mkdirs() }
            val destinationFile = File(foodDiaryDir, file.name)
            file.copyTo(destinationFile, overwrite = true)
            "Fichier enregistré dans Documents/FoodDiary/${file.name}"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}