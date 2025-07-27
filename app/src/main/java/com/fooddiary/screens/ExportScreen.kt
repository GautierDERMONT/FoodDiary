package com.fooddiary.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import com.fooddiary.utils.getCurrentWeekInfo
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import com.fooddiary.model.Meal



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
    viewModel: MealViewModel
) {
    val scope = rememberCoroutineScope()
    val meals by viewModel.weekMeals.collectAsState()
    var exportFormat by remember { mutableStateOf("PDF") }
    val formats = listOf("PDF", "Image")
    val dieticianEmail by viewModel.dieticianEmail.collectAsState()
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
                            onValueChange = { viewModel.updateDieticianEmail(it) },
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
        val weekInfo = getCurrentWeekInfo()
        canvas.drawText(weekInfo, 20f * density, yPos, paint)
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
                        Paragraph(getCurrentWeekInfo())
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(12f)
                            .setMarginBottom(20f)
                    )

                    // Définir les largeurs des colonnes en pourcentage
                    val columnWidths = floatArrayOf(15f, 15f, 40f, 30f) // Jour, Repas, Description, Photo
                    val table = com.itextpdf.layout.element.Table(columnWidths)
                        .useAllAvailableWidth()
                        .setMarginTop(10f)

                    // Style pour les en-têtes
                    val headerStyle = com.itextpdf.layout.Style()
                        .setBold()
                        .setFontSize(12f)
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(5f)

                    // Style pour le contenu centré verticalement
                    val contentStyle = com.itextpdf.layout.Style()
                        .setTextAlignment(TextAlignment.LEFT) // Changé de CENTER à LEFT pour la description
                        .setPadding(5f)

                    // En-têtes
                    table.addHeaderCell(Paragraph("Jour").addStyle(headerStyle))
                    table.addHeaderCell(Paragraph("Repas").addStyle(headerStyle))
                    table.addHeaderCell(Paragraph("Description").addStyle(headerStyle))
                    table.addHeaderCell(Paragraph("Photo").addStyle(headerStyle))

                    weekMeals.forEach { dayMeals ->
                        dayMeals.meals.forEach { meal ->
                            if (!meal.isVierge()) {
                                // Cellule pour le jour (centrée verticalement)
                                val dayCell = com.itextpdf.layout.element.Cell()
                                    .add(Paragraph(dayMeals.day).addStyle(contentStyle))
                                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                                table.addCell(dayCell)

                                // Cellule pour le type de repas (centrée verticalement)
                                val typeCell = com.itextpdf.layout.element.Cell()
                                    .add(Paragraph(meal.type.toFrenchString()).addStyle(contentStyle))
                                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                                table.addCell(typeCell)

                                // Cellule pour la description avec retour à la ligne
                                val description = buildString {
                                    append(meal.description)
                                    if (!meal.notes.isNullOrBlank()) {
                                        append("\n\nNotes: ${meal.notes}")
                                    }
                                }
                                val descParagraph = Paragraph(description)
                                    .setFixedLeading(14f) // Espacement entre les lignes
                                val descCell = com.itextpdf.layout.element.Cell()
                                    .add(descParagraph.addStyle(contentStyle))
                                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                                table.addCell(descCell)

                                // Cellule pour la photo
                                val photoCell = com.itextpdf.layout.element.Cell()
                                    .addStyle(contentStyle)
                                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)

                                meal.photoUri?.let { uriString ->
                                    try {
                                        val filePath = uriString.removePrefix("file://")
                                        val imageData = ImageDataFactory.create(filePath)
                                        photoCell.add(
                                            com.itextpdf.layout.element.Image(imageData)
                                                .setWidth(100f)
                                                .setHeight(100f)
                                                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                                        )
                                    } catch (e: Exception) {
                                        photoCell.add(Paragraph("(image)").addStyle(contentStyle))
                                    }
                                } ?: photoCell.add(Paragraph("-").addStyle(contentStyle))

                                table.addCell(photoCell)
                            }
                        }
                    }

                    document.add(table)
                }
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private suspend fun createImage(file: File, weekMeals: List<DayMeals>, context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val density = context.resources.displayMetrics.density
        val pageWidth = (595 * density).toInt() // A4 width at 72dpi
        val pageHeight = (842 * density).toInt() // A4 height at 72dpi
        val margin = (20 * density).toInt()

        // Calculer le nombre de pages nécessaires
        val pagesInfo = calculatePages(weekMeals, density, pageHeight, margin)
        val outputFiles = mutableListOf<File>()

        pagesInfo.forEachIndexed { pageIndex, mealsForPage ->
            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            var yPos = margin

            // Dessiner l'en-tête (titre + date + pagination)
            drawHeader(canvas, pageIndex, pagesInfo.size, margin, yPos, density)
            yPos += (70 * density).toInt() // Espace pour l'en-tête

            // Dessiner le tableau
            yPos = drawTableHeader(canvas, margin, yPos, pageWidth, density)

            // Dessiner les repas de cette page
            yPos = drawMeals(canvas, mealsForPage, margin, yPos, pageWidth, density, context)

            // Enregistrer la page
            val pageFile = File(file.parent, "${file.nameWithoutExtension}_${pageIndex+1}.jpg")
            FileOutputStream(pageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
            outputFiles.add(pageFile)
        }

        // Pour la compatibilité, on garde le fichier original comme première page
        if (outputFiles.isNotEmpty()) {
            file.delete()
            outputFiles.first().copyTo(file, true)
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun calculatePages(
    weekMeals: List<DayMeals>,
    density: Float,
    pageHeight: Int,
    margin: Int
): List<List<Pair<DayMeals, Meal>>> {
    val mealsList = mutableListOf<Pair<DayMeals, Meal>>()
    weekMeals.forEach { dayMeals ->
        dayMeals.meals.filter { !it.isVierge() }.forEach { meal ->
            mealsList.add(dayMeals to meal)
        }
    }

    val pages = mutableListOf<List<Pair<DayMeals, Meal>>>()
    var currentPage = mutableListOf<Pair<DayMeals, Meal>>()
    var currentHeight = margin + (110 * density).toInt() // Header + table header

    mealsList.forEach { (dayMeals, meal) ->
        val description = if (!meal.notes.isNullOrBlank()) {
            "${meal.description}\nNotes: ${meal.notes}"
        } else {
            meal.description
        }

        val textHeight = calculateTextHeight(
            text = description,
            paint = Paint().apply {
                textSize = 12f * density
                typeface = Typeface.DEFAULT
            },
            maxWidth = (595 * density - 2 * margin) / 4f * 2 - (10 * density),
            lineHeight = 20f * density
        )

        val mealHeight = maxOf(
            (100 * density).toInt(), // Hauteur minimale
            (textHeight + 30 * density).toInt() // Hauteur réelle
        )

        if (currentHeight + mealHeight > pageHeight - (50 * density).toInt()) {
            pages.add(currentPage)
            currentPage = mutableListOf(dayMeals to meal)
            currentHeight = margin + (110 * density).toInt() + mealHeight
        } else {
            currentPage.add(dayMeals to meal)
            currentHeight += mealHeight
        }
    }

    if (currentPage.isNotEmpty()) {
        pages.add(currentPage)
    }

    return pages
}

private fun drawHeader(
    canvas: Canvas,
    pageIndex: Int,
    totalPages: Int,
    margin: Int,
    yPos: Int,
    density: Float
) {
    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f * density
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f * density
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    canvas.drawText("Récapitulatif des repas", margin.toFloat(), yPos.toFloat(), titlePaint)
    canvas.drawText(getCurrentWeekInfo(), margin.toFloat(), (yPos + 30 * density).toFloat(), textPaint)

    // Pagination
    canvas.drawText(
        "Page ${pageIndex + 1}/$totalPages",
        (595 * density - margin - 50 * density).toFloat(),
        (yPos + 30 * density).toFloat(),
        textPaint
    )
}

private fun drawTableHeader(
    canvas: Canvas,
    margin: Int,
    yPos: Int,
    pageWidth: Int,
    density: Float
): Int {
    val headerPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val linePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 1f * density
        isAntiAlias = true
    }

    val rowPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val columnWidth = (pageWidth - 2 * margin) / 4f
    val headerHeight = (30 * density).toInt()
    val centerY = yPos + headerHeight / 2 - (headerPaint.descent() + headerPaint.ascent()) / 2

    canvas.drawRect(
        margin.toFloat(),
        yPos.toFloat(),
        (pageWidth - margin).toFloat(),
        (yPos + headerHeight).toFloat(),
        rowPaint
    )

    canvas.drawText("Jour", margin + columnWidth / 2, centerY, headerPaint)
    canvas.drawText("Repas", margin + columnWidth + columnWidth / 2, centerY, headerPaint)
    canvas.drawText("Description", margin + columnWidth * 2 + columnWidth / 2, centerY, headerPaint)
    canvas.drawText("Photo", margin + columnWidth * 3 + columnWidth / 2, centerY, headerPaint)

    canvas.drawLine(
        margin.toFloat(),
        (yPos + headerHeight).toFloat(),
        (pageWidth - margin).toFloat(),
        (yPos + headerHeight).toFloat(),
        linePaint
    )

    return yPos + headerHeight + (10 * density).toInt()
}

private fun drawMeals(
    canvas: Canvas,
    meals: List<Pair<DayMeals, Meal>>,
    margin: Int,
    startY: Int,
    pageWidth: Int,
    density: Float,
    context: Context
): Int {
    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f * density
        typeface = Typeface.DEFAULT
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val linePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 0.5f * density
        isAntiAlias = true
    }

    val rowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val columnWidth = (pageWidth - 2 * margin) / 4f
    val imageSize = (80 * density).toInt()
    var yPos = startY

    meals.forEachIndexed { index, (dayMeals, meal) ->
        val description = if (!meal.notes.isNullOrBlank()) {
            "${meal.description}\nNotes: ${meal.notes}"
        } else {
            meal.description
        }

        val lineHeight = 20f * density
        val maxTextWidth = columnWidth * 2 - (15 * density)
        val textHeight = calculateTextHeight(description, textPaint, maxTextWidth, lineHeight)
        val rowHeight = maxOf(
            (100 * density).toInt(),
            (textHeight + 20 * density).toInt()
        )

        rowPaint.color = if (index % 2 == 0) {
            android.graphics.Color.parseColor("#F8F8F8")
        } else {
            android.graphics.Color.WHITE
        }

        canvas.drawRect(
            margin.toFloat(),
            yPos.toFloat(),
            (pageWidth - margin).toFloat(),
            (yPos + rowHeight).toFloat(),
            rowPaint
        )

        val centerY = yPos + rowHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(dayMeals.day, margin + columnWidth / 2, centerY, textPaint)
        canvas.drawText(meal.type.toFrenchString(), margin + columnWidth + columnWidth / 2, centerY, textPaint)

        val descPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.LEFT
        }

        drawMultilineText(
            canvas = canvas,
            text = description,
            x = margin + columnWidth * 2 + (5 * density),
            y = yPos + (20 * density).toInt(),
            paint = descPaint,
            maxWidth = maxTextWidth,
            lineHeight = lineHeight,
            maxLines = 5
        )

        meal.photoUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { originalBitmap ->
                        try {
                            val scaledBitmap = Bitmap.createScaledBitmap(
                                originalBitmap,
                                imageSize,
                                imageSize,
                                true
                            )
                            canvas.drawBitmap(
                                scaledBitmap,
                                margin + columnWidth * 3 + (columnWidth - imageSize) / 2,
                                yPos + (rowHeight - imageSize) / 2f,
                                null
                            )
                            scaledBitmap.recycle()
                        } finally {
                            originalBitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                canvas.drawText("-", margin + columnWidth * 3, centerY, textPaint)
            }
        } ?: run {
            canvas.drawText("-", margin + columnWidth * 3, centerY, textPaint)
        }

        yPos += rowHeight

        canvas.drawLine(
            margin.toFloat(),
            yPos.toFloat(),
            (pageWidth - margin).toFloat(),
            yPos.toFloat(),
            linePaint
        )

        yPos += (2 * density).toInt()
    }

    return yPos
}

private fun calculateTextHeight(text: String, paint: Paint, maxWidth: Float, lineHeight: Float): Float {
    var height = 0f
    val lines = text.split("\n")

    lines.forEach { line ->
        if (paint.measureText(line) > maxWidth) {
            var start = 0
            while (start < line.length) {
                val count = paint.breakText(line, start, line.length, true, maxWidth, null)
                height += lineHeight
                start += count
            }
        } else {
            height += lineHeight
        }
    }

    return height
}

// Fonction existante améliorée pour le texte multiligne
private fun drawMultilineText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Int,
    paint: Paint,
    maxWidth: Float,
    lineHeight: Float,
    maxLines: Int = Int.MAX_VALUE
) {
    var currentY = y.toFloat()
    val lines = text.split("\n")
    var linesDrawn = 0

    lines.forEach { line ->
        if (linesDrawn >= maxLines) return@forEach

        if (paint.measureText(line) > maxWidth) {
            var start = 0
            while (start < line.length && linesDrawn < maxLines) {
                val count = paint.breakText(line, start, line.length, true, maxWidth, null)
                canvas.drawText(line.substring(start, start + count), x, currentY, paint)
                currentY += lineHeight
                start += count
                linesDrawn++
            }
        } else {
            if (linesDrawn < maxLines) {
                canvas.drawText(line, x, currentY, paint)
                currentY += lineHeight
                linesDrawn++
            }
        }
    }

    if (linesDrawn >= maxLines && text.lines().sumOf { it.length } > 0) {
        canvas.drawText("...", x, currentY, paint)
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
        putExtra(Intent.EXTRA_TEXT, "Voici mon carnet alimentaire comme convenu. Cordialement.")
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