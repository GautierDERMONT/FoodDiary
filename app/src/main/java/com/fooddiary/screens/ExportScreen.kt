package com.fooddiary.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fooddiary.model.Meal
import com.fooddiary.viewmodel.MealViewModel
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.ContentValues
import android.provider.MediaStore
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream

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
    val weekMeals by viewModel.weekMeals.collectAsState()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(weekMeals, exportFormat) {
        previewBitmap = generatePreviewBitmap(weekMeals, context, exportFormat)
    }

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
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Prévisualisation",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
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

private fun generatePreviewBitmap(
    weekMeals: List<DayMeals>,
    context: Context,
    format: String
): Bitmap? {
    return try {
        val density = context.resources.displayMetrics.density
        val width = (210 * density).toInt()
        val height = (297 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            isAntiAlias = true
        }
        val cornerRadius = 0f * density
        val halfStroke = borderPaint.strokeWidth / 2
        val borderRect = RectF(
            halfStroke,
            halfStroke,
            width.toFloat() - halfStroke,
            height.toFloat() - halfStroke
        )
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f * density
        }

        var yPos = 30f * density
        weekMeals.forEach { dayMeals ->
            textPaint.textSize = 14f * density
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(dayMeals.day, 20f * density, yPos, textPaint)
            yPos += 20f * density

            textPaint.textSize = 12f * density
            textPaint.typeface = Typeface.DEFAULT
            dayMeals.meals.forEach { meal ->
                if (!meal.isVierge()) {
                    canvas.drawText("${meal.type}: ${meal.description}", 30f * density, yPos, textPaint)
                    yPos += 18f * density

                    meal.photoUri?.let { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val mealBitmap = BitmapFactory.decodeStream(inputStream)
                                val imageWidth = 100f * density
                                val imageHeight = (mealBitmap.height * (imageWidth / mealBitmap.width))

                                canvas.drawBitmap(
                                    mealBitmap,
                                    null,
                                    RectF(30f * density, yPos, 30f * density + imageWidth, yPos + imageHeight),
                                    null
                                )
                                yPos += imageHeight + 10f * density
                                mealBitmap.recycle()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    meal.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        textPaint.textSize = 10f * density
                        canvas.drawText("Notes: $notes", 40f * density, yPos, textPaint)
                        yPos += 15f * density
                    }
                }
            }
            yPos += 15f * density
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createExportFile(context: Context, viewModel: MealViewModel, format: String): File? {
    return try {
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports").apply {
            mkdirs()
            listFiles()?.takeIf { it.size > 5 }?.sortedBy { it.lastModified() }?.dropLast(5)?.forEach { it.delete() }
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "FoodDiary_$timeStamp.${format.lowercase()}"
        val file = File(exportDir, fileName)

        when (format) {
            "PDF" -> createPdf(file, viewModel, context)
            "Image" -> createImage(file, viewModel, context)
            else -> false
        }

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createPdf(file: File, viewModel: MealViewModel, context: Context): Boolean {
    val weekMeals = viewModel.weekMeals.value
    val pdfWriter = PdfWriter(file)
    val pdfDocument = PdfDocument(pdfWriter)
    val document = Document(pdfDocument, PageSize.A4).apply {
        setMargins(40f, 40f, 40f, 40f)
    }

    return try {
        document.add(Paragraph("Récapitulatif des repas")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold())

        document.add(Paragraph(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12f)
            .setMarginBottom(20f))

        weekMeals.forEach { dayMeals ->
            document.add(Paragraph(dayMeals.day)
                .setBold()
                .setFontSize(14f)
                .setMarginTop(15f)
                .setMarginBottom(5f))

            dayMeals.meals.forEach { meal ->
                if (!meal.isVierge()) {
                    document.add(Paragraph("${meal.type}: ${meal.description}")
                        .setFontSize(12f)
                        .setMarginBottom(5f))

                    meal.photoUri?.let { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                try {
                                    val maxWidth = 300f
                                    val scaleFactor = maxWidth / bitmap.width
                                    val scaledBitmap = Bitmap.createScaledBitmap(
                                        bitmap,
                                        maxWidth.toInt(),
                                        (bitmap.height * scaleFactor).toInt(),
                                        true
                                    )
                                    try {
                                        ByteArrayOutputStream().use { stream ->
                                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                                            val imageData = ImageDataFactory.create(stream.toByteArray())
                                            document.add(
                                                com.itextpdf.layout.element.Image(imageData)
                                                    .setWidth(maxWidth)
                                                    .setAutoScaleHeight(true)
                                                    .setMarginBottom(10f)
                                            )
                                        }
                                    } finally {
                                        scaledBitmap.recycle()
                                    }
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    meal.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        document.add(Paragraph("Notes: $note")
                            .setItalic()
                            .setFontSize(10f)
                            .setMarginBottom(10f))
                    }

                    document.add(Paragraph("\n"))
                }
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        document.close()
        pdfDocument.close()
        pdfWriter.close()
    }
}

private fun createImage(file: File, viewModel: MealViewModel, context: Context): Boolean {
    return try {
        val weekMeals = viewModel.weekMeals.value
        val density = context.resources.displayMetrics.density
        val width = (600 * density).toInt()
        val height = calculateRequiredHeight(weekMeals, density)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        paint.textSize = 20f * density
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Récapitulatif des repas", 30f * density, 50f * density, paint)

        paint.textSize = 14f * density
        paint.typeface = Typeface.DEFAULT
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateStr, 30f * density, 80f * density, paint)

        var yPos = 120f * density
        weekMeals.forEach { dayMeals ->
            paint.textSize = 18f * density
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(dayMeals.day, 30f * density, yPos, paint)
            yPos += 30f * density

            paint.textSize = 14f * density
            paint.typeface = Typeface.DEFAULT
            dayMeals.meals.forEach { meal ->
                if (!meal.isVierge()) {
                    canvas.drawText("${meal.type}: ${meal.description}", 50f * density, yPos, paint)
                    yPos += 25f * density

                    meal.photoUri?.let { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = 2
                                }
                                val mealBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                                mealBitmap?.let {
                                    val maxWidth = width - 100 * density
                                    val scale = maxWidth / it.width.toFloat()
                                    val scaledHeight = it.height * scale

                                    canvas.drawBitmap(
                                        it,
                                        null,
                                        RectF(50f * density, yPos, 50f * density + maxWidth, yPos + scaledHeight),
                                        null
                                    )
                                    yPos += scaledHeight + 15f * density
                                    it.recycle()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    meal.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        paint.textSize = 12f * density
                        canvas.drawText("Notes: $note", 70f * density, yPos, paint)
                        yPos += 20f * density
                    }
                    yPos += 10f * density
                }
            }
            yPos += 15f * density
        }

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun calculateRequiredHeight(weekMeals: List<DayMeals>, density: Float): Int {
    var height = 150f * density

    weekMeals.forEach { dayMeals ->
        height += 45f * density

        dayMeals.meals.forEach { meal ->
            if (!meal.isVierge()) {
                height += 25f * density

                if (meal.photoUri != null) {
                    height += 150f * density
                }

                if (!meal.notes.isNullOrBlank()) {
                    height += 20f * density
                }

                height += 10f * density
            }
        }

        height += 15f * density
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