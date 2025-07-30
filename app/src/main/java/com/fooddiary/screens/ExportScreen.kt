package com.fooddiary.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Environment
import kotlin.math.min
import com.fooddiary.R
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import com.fooddiary.utils.getCurrentWeekInfo
import com.fooddiary.model.Meal
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(navController: NavController, viewModel: MealViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val weekMeals by viewModel.weekMeals.collectAsState()
    val dieticianEmail by viewModel.dieticianEmail.collectAsState()

    var exportFormat by remember { mutableStateOf("PDF") }
    var sendToDietician by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isSharing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var shareProgress by remember { mutableStateOf(0f) }
    var saveProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(weekMeals, exportFormat) {
        isLoading = true
        previewBitmap = generatePreviewBitmap(weekMeals, context, exportFormat)
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exporter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp)) {
                PreviewCard(previewBitmap, isLoading)
                Spacer(modifier = Modifier.height(16.dp)) // Ajout d'un espace ici
                FormatSelector(exportFormat) { exportFormat = it }
                Spacer(modifier = Modifier.height(16.dp)) // Ajout d'un espace ici
                DieticianOptions(sendToDietician, dieticianEmail,
                    { sendToDietician = it },
                    { viewModel.updateDieticianEmail(it) }
                )
                Spacer(modifier = Modifier.height(24.dp)) // Augmentation de l'espace ici
                ActionButtons(
                    isSharing = isSharing,
                    shareProgress = shareProgress,
                    isSaving = isSaving,
                    saveProgress = saveProgress,
                    onShare = {
                        scope.launch {
                            isSharing = true
                            shareProgress = 0f
                            createAndShare(context, weekMeals, exportFormat, sendToDietician, dieticianEmail) { p ->
                                shareProgress = p
                            }
                            isSharing = false
                        }
                    },
                    onSave = {
                        scope.launch {
                            isSaving = true
                            saveProgress = 0f
                            saveSuccessMessage = saveFile(context, weekMeals, exportFormat) { p ->
                                saveProgress = p
                            } ?: "Erreur lors de l'enregistrement"
                            showSaveSuccess = true
                            isSaving = false
                        }
                    }
                )
            }

            if (showSaveSuccess) {
                LaunchedEffect(Unit) { delay(3000); showSaveSuccess = false }
                Snackbar(Modifier.align(Alignment.BottomCenter)) { Text(saveSuccessMessage) }
            }
        }
    }
}

@Composable
private fun PreviewCard(previewBitmap: Bitmap?, isLoading: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(8.dp),
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
}

@Composable
private fun FormatSelector(currentFormat: String, onFormatChange: (String) -> Unit) {
    Column {
        Text("Format d'exportation:", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("PDF", "Image").forEach { format ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.selectable(selected = (format == currentFormat)) {
                        onFormatChange(format)
                    }
                ) {
                    RadioButton(selected = (format == currentFormat), onClick = { onFormatChange(format) })
                    Text(format)
                }
            }
        }
    }
}

@Composable
private fun DieticianOptions(
    sendToDietician: Boolean,
    email: String,
    onCheckedChange: (Boolean) -> Unit,
    onEmailChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sendToDietician, onCheckedChange = onCheckedChange)
            Text("Envoyer à ma diététicienne")
        }
        if (sendToDietician) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email de la diététicienne") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ActionButtons(
    isSharing: Boolean,
    shareProgress: Float,
    isSaving: Boolean,
    saveProgress: Float,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSharing && !isSaving, // Désactivé si sauvegarde en cours
            colors = ButtonDefaults.buttonColors()
        ) {
            if (isSharing) {
                LinearProgressIndicator(
                    progress = shareProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Partager",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Partager")
            }
        }

        OutlinedButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && !isSharing, // Désactivé si partage en cours
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            if (isSaving) {
                LinearProgressIndicator(
                    progress = saveProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Enregistrer",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Enregistrer")
            }
        }
    }
}

private suspend fun createAndShare(
    context: Context,
    weekMeals: List<DayMeals>,
    format: String,
    sendToDietician: Boolean,
    email: String,
    onProgress: (Float) -> Unit = {}
) {
    val file = createExportFile(context, weekMeals, format, onProgress) ?: return
    shareFile(context, file, format)
    if (sendToDietician && email.isNotBlank()) {
        sendToDietician(context, file, format, email)
    }
}

private fun generatePreviewBitmap(
    weekMeals: List<DayMeals>,
    context: Context,
    format: String
): Bitmap? {
    val nonEmptyMeals = weekMeals.flatMap { dayMeals ->
        dayMeals.meals.filterNot { it.isVierge() }.map { dayMeals.day to it }
    }

    if (nonEmptyMeals.isEmpty()) {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(
            (300 * density).toInt(),
            (200 * density).toInt(),
            Bitmap.Config.ARGB_8888
        )
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            Paint().apply {
                color = Color.BLACK
                textSize = 14f * density
                textAlign = Paint.Align.CENTER
            }.let { paint ->
                drawText(
                    "Aucun repas enregistré",
                    bitmap.width / 2f,
                    bitmap.height / 2f,
                    paint
                )
            }
        }
        return bitmap
    }

    val density = context.resources.displayMetrics.density
    val width = (300 * density).toInt()
    val height = (400 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }
    val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
    }

    var yPos = 15f * density
    paint.textSize = 16f * density
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("Prévisualisation", 15f * density, yPos, paint)
    yPos += 25f * density

    paint.textSize = 12f * density
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(getCurrentWeekInfo(), 15f * density, yPos, paint)
    yPos += 25f * density

    paint.strokeWidth = 1f * density
    canvas.drawLine(15f * density, yPos, width - 15f * density, yPos, paint)
    yPos += 15f * density

    val columnConfig = listOf(
        "Jour" to 0.15f,
        "Repas" to 0.25f,
        "Description" to 0.45f,
        "Photo" to 0.15f
    )
    val totalWidth = width - 30f * density
    val headerHeight = 25f * density
    val iconSize = (20 * density).toInt()
    val photoIcon = ContextCompat.getDrawable(context, R.drawable.ic_photo_frame)?.apply {
        setBounds(0, 0, iconSize, iconSize)
    }

    canvas.drawRect(
        15f * density, yPos,
        width - 15f * density, yPos + headerHeight,
        Paint().apply { color = Color.LTGRAY }
    )

    var currentX = 15f * density
    columnConfig.forEach { (title, widthRatio) ->
        val colWidth = totalWidth * widthRatio
        paint.textSize = 10f * density
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            title,
            currentX + 5f * density,
            yPos + headerHeight / 2 + (paint.textSize / 3),
            paint
        )
        currentX += colWidth
    }
    yPos += headerHeight + 5f * density

    val maxRows = min(8, nonEmptyMeals.size)
    val lineHeight = 25f * density

    for (i in 0 until maxRows) {
        val (day, meal) = nonEmptyMeals[i]

        canvas.drawRect(
            15f * density, yPos,
            width - 15f * density, yPos + lineHeight,
            Paint().apply {
                color = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F5F5F5")
            }
        )

        currentX = 15f * density
        var colWidth = totalWidth * columnConfig[0].second
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            day,
            currentX + 5f * density,
            yPos + lineHeight / 2 + (paint.textSize / 3),
            paint
        )
        currentX += colWidth

        colWidth = totalWidth * columnConfig[1].second
        canvas.drawText(
            meal.type.toFrenchString(),
            currentX + 5f * density,
            yPos + lineHeight / 2 + (paint.textSize / 3),
            paint
        )
        currentX += colWidth

        colWidth = totalWidth * columnConfig[2].second
        val desc = if (meal.description.length > 20) {
            "${meal.description.take(17)}..."
        } else {
            meal.description
        }
        canvas.drawText(
            desc,
            currentX + 5f * density,
            yPos + lineHeight / 2 + (paint.textSize / 3),
            paint
        )
        currentX += colWidth

        colWidth = totalWidth * columnConfig[3].second
        if (meal.photoUri != null) {
            photoIcon?.let { icon ->
                canvas.save()
                canvas.translate(
                    currentX + (colWidth - iconSize) / 2,
                    yPos + (lineHeight - iconSize) / 2
                )
                icon.draw(canvas)
                canvas.restore()
            }
        } else {
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "-",
                currentX + colWidth / 2,
                yPos + lineHeight / 2 + (paint.textSize / 3),
                paint
            )
            paint.textAlign = Paint.Align.LEFT
        }

        yPos += lineHeight

        if (i < maxRows - 1) {
            canvas.drawLine(
                15f * density, yPos,
                width - 15f * density, yPos,
                Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 0.5f * density
                }
            )
            yPos += 2f * density
        }
    }

    if (nonEmptyMeals.size > maxRows) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "... +${nonEmptyMeals.size - maxRows} repas",
            width / 2f,
            height - 20f * density,
            paint
        )
    }

    return bitmap
}

private suspend fun createExportFile(
    context: Context,
    weekMeals: List<DayMeals>,
    format: String,
    onProgress: (Float) -> Unit = {}
): File? = withContext(Dispatchers.IO) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseName = "FoodDiary_$timeStamp"
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports").apply { mkdirs() }

        clearPreviousExports(context, baseName)

        // Correction ici : forcer l'extension .jpg pour les images
        val fileExtension = if (format == "Image") "jpg" else format.lowercase()
        val file = File(exportDir, "$baseName.$fileExtension")

        when (format) {
            "PDF" -> if (!createPdf(file, weekMeals, context, onProgress)) return@withContext null
            "Image" -> if (!createImage(file, weekMeals, context, onProgress)) return@withContext null
            else -> return@withContext null
        }

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun clearPreviousExports(context: Context, baseName: String) {
    val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
    exportDir.listFiles()?.forEach { file ->
        if (file.name.startsWith(baseName) && (file.name.endsWith(".jpg") || file.name.endsWith(".pdf") || file.name.endsWith(".image"))) {
            file.delete()
        }
    }
}

private suspend fun createPdf(
    file: File,
    weekMeals: List<DayMeals>,
    context: Context,
    onProgress: (Float) -> Unit = {}
): Boolean = withContext(Dispatchers.IO) {
    try {
        PdfWriter(file).use { writer ->
            PdfDocument(writer).use { pdfDocument ->
                Document(pdfDocument, PageSize.A4).use { document ->
                    document.setMargins(40f, 40f, 40f, 40f)
                    document.add(Paragraph("Récapitulatif des repas").apply {
                        setTextAlignment(TextAlignment.CENTER)
                        setFontSize(18f).setBold().setMarginBottom(10f)
                    })
                    document.add(Paragraph(getCurrentWeekInfo()).apply {
                        setTextAlignment(TextAlignment.CENTER)
                        setFontSize(12f).setMarginBottom(20f)
                    })

                    val allMeals = weekMeals.flatMap { dayMeals ->
                        dayMeals.meals.filterNot { it.isVierge() }.map { dayMeals to it }
                    }
                    val totalMeals = allMeals.size
                    var processed = 0

                    val table = com.itextpdf.layout.element.Table(floatArrayOf(15f, 15f, 40f, 30f))
                        .useAllAvailableWidth().setMarginTop(10f)

                    val headerStyle = com.itextpdf.layout.Style()
                        .setBold().setFontSize(12f)
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER).setPadding(5f)

                    listOf("Jour", "Repas", "Description", "Photo").forEach {
                        table.addHeaderCell(Paragraph(it).addStyle(headerStyle))
                    }

                    allMeals.forEach { (dayMeals, meal) ->
                        table.addCell(createCell(dayMeals.day))
                        table.addCell(createCell(meal.type.toFrenchString()))
                        table.addCell(createDescriptionCell(meal))
                        table.addCell(createPhotoCell(meal, context))

                        processed++
                        onProgress(processed.toFloat() / totalMeals)
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

private fun createCell(text: String): com.itextpdf.layout.element.Cell {
    return com.itextpdf.layout.element.Cell()
        .add(Paragraph(text).setTextAlignment(TextAlignment.LEFT))
        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
}

private fun createDescriptionCell(meal: Meal): com.itextpdf.layout.element.Cell {
    val description = buildString {
        append(meal.description)
        if (!meal.notes.isNullOrBlank()) append("\n\nNotes: ${meal.notes}")
    }
    return com.itextpdf.layout.element.Cell()
        .add(Paragraph(description).setFixedLeading(14f))
}

private fun createPhotoCell(meal: Meal, context: Context): com.itextpdf.layout.element.Cell {
    val cell = com.itextpdf.layout.element.Cell()
        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)

    meal.photoUri?.let { uriString ->
        try {
            // Ajoutez cette partie pour redimensionner l'image avant l'ajout au PDF
            val originalBitmap = BitmapFactory.decodeFile(uriString.removePrefix("file://"))
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap,
                originalBitmap.width / 2,  // Réduire la largeur de moitié
                originalBitmap.height / 2, // Réduire la hauteur de moitié
                true)

            // Sauvegarder l'image redimensionnée temporairement
            val tempFile = File.createTempFile("temp_img", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) // 70% de qualité
            }

            val imageData = ImageDataFactory.create(tempFile.absolutePath)
            cell.add(com.itextpdf.layout.element.Image(imageData)
                .setWidth(80f)  // Réduire encore la taille d'affichage
                .setHeight(80f))

            // Nettoyer
            originalBitmap.recycle()
            scaledBitmap.recycle()
            tempFile.delete()
        } catch (e: Exception) {
            cell.add(Paragraph("(image)"))
        }
    } ?: cell.add(Paragraph("-"))
    return cell
}

private suspend fun createImage(
    file: File,
    weekMeals: List<DayMeals>,
    context: Context,
    onProgress: (Float) -> Unit = {}
): Boolean = withContext(Dispatchers.IO) {
    try {
        val density = context.resources.displayMetrics.density
        val pageWidth = (595 * density).toInt()
        val pageHeight = (842 * density).toInt()
        val margin = (20 * density).toInt()

        val allMeals = weekMeals.flatMap { dayMeals ->
            dayMeals.meals.filterNot { it.isVierge() }.map { dayMeals to it }
        }

        if (allMeals.isEmpty()) return@withContext false

        val pages = mutableListOf<List<Pair<DayMeals, Meal>>>()
        var currentPage = mutableListOf<Pair<DayMeals, Meal>>()
        var currentHeight = margin + (80 * density).toInt()
        val totalMeals = allMeals.size
        var processed = 0

        // Calcul des pages avec mise à jour de la progression
        allMeals.forEach { (dayMeals, meal) ->
            val rowHeight = calculateRowHeight(meal, density, pageWidth, margin)

            if (currentHeight + rowHeight > pageHeight - margin) {
                pages.add(currentPage)
                currentPage = mutableListOf(dayMeals to meal)
                currentHeight = margin + (80 * density).toInt() + rowHeight
            } else {
                currentPage.add(dayMeals to meal)
                currentHeight += rowHeight
            }

            processed++
            onProgress(processed.toFloat() / totalMeals * 0.5f) // Première moitié pour le calcul des pages
        }

        if (currentPage.isNotEmpty()) pages.add(currentPage)

        // Génération des images avec mise à jour de la progression
        pages.forEachIndexed { index, meals ->
            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }

            var yPos = margin
            drawHeader(canvas, index, pages.size, margin, yPos, density)
            yPos += (70 * density).toInt()

            yPos = drawTableHeader(canvas, margin, yPos, pageWidth, density)
            drawMeals(canvas, meals, margin, yPos, pageWidth, density, context)

            val outputFile = if (index == 0) {
                File(file.parent, "${file.nameWithoutExtension}.jpg")
            } else {
                File(file.parent, "${file.nameWithoutExtension}_${index+1}.jpg")
            }

            FileOutputStream(outputFile).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                    throw Exception("Failed to compress bitmap")
                }
            }
            bitmap.recycle()

            // Mise à jour de la progression pour la génération des images
            onProgress(0.5f + (index + 1).toFloat() / pages.size.toFloat() * 0.5f)
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun shareFile(context: Context, file: File, format: String) {
    try {
        val uris = ArrayList<Uri>()
        val baseName = file.nameWithoutExtension

        // Vérifier d'abord le fichier principal en fonction du format
        val mainFile = when (format) {
            "PDF" -> File(file.parent, "$baseName.pdf")
            else -> File(file.parent, "$baseName.jpg")
        }

        if (mainFile.exists()) {
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mainFile))
        } else {
            Toast.makeText(context, "Fichier principal introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        // Pour les images multi-pages
        if (format == "Image") {
            var pageNum = 2
            while (true) {
                val pageFile = File(file.parent, "${baseName}_$pageNum.jpg")
                if (!pageFile.exists()) break
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pageFile))
                pageNum++
            }
        }

        val shareIntent = Intent().apply {
            action = if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
            type = when (format) {
                "PDF" -> "application/pdf"
                else -> "image/jpeg"
            }

            if (uris.size > 1) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            } else {
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur lors du partage: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}
private fun calculateRowHeight(meal: Meal, density: Float, pageWidth: Int, margin: Int): Int {
    val columnWidth = (pageWidth - 2 * margin) / 4f
    val description = if (!meal.notes.isNullOrBlank()) "${meal.description}\nNotes: ${meal.notes}" else meal.description
    val textHeight = calculateTextHeight(
        text = description,
        paint = Paint().apply { textSize = 12f * density },
        maxWidth = columnWidth * 2 - (15 * density),
        lineHeight = 20f * density
    )
    return maxOf((100 * density).toInt(), (textHeight + 20 * density).toInt())
}

private fun calculateTextHeight(text: String, paint: Paint, maxWidth: Float, lineHeight: Float): Float {
    return text.split("\n").sumOf { line ->
        if (paint.measureText(line) > maxWidth) {
            var start = 0
            var lines = 0
            while (start < line.length) {
                val count = paint.breakText(line, start, line.length, true, maxWidth, null)
                lines++
                start += count
            }
            lines
        } else 1
    } * lineHeight
}

private fun drawHeader(canvas: Canvas, pageIndex: Int, totalPages: Int, margin: Int, yPos: Int, density: Float) {
    val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f * density
        typeface = Typeface.DEFAULT_BOLD
    }
    val textPaint = Paint(titlePaint).apply { textSize = 12f * density }

    canvas.drawText("Récapitulatif des repas", margin.toFloat(), yPos.toFloat(), titlePaint)
    canvas.drawText(getCurrentWeekInfo(), margin.toFloat(), (yPos + 30 * density), textPaint)
    canvas.drawText("Page ${pageIndex + 1}/$totalPages", (595 * density - margin - 50 * density), (yPos + 30 * density), textPaint)
}

private fun drawTableHeader(canvas: Canvas, margin: Int, yPos: Int, pageWidth: Int, density: Float): Int {
    val headerPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val linePaint = Paint().apply { strokeWidth = 1f * density }
    // Modifier ici les largeurs des colonnes (Jour: 10%, Repas: 15%, Description: 55%, Photo: 20%)
    val columnWidths = listOf(0.1f, 0.15f, 0.55f, 0.2f)
    val totalWidth = pageWidth - 2 * margin
    val headerHeight = (30 * density).toInt()
    val centerY = yPos + headerHeight / 2 - (headerPaint.descent() + headerPaint.ascent()) / 2

    canvas.drawRect(margin.toFloat(), yPos.toFloat(), (pageWidth - margin).toFloat(), (yPos + headerHeight).toFloat(),
        Paint().apply { color = Color.LTGRAY })

    var currentX = margin.toFloat()
    listOf("Jour", "Repas", "Description", "Photo").forEachIndexed { index, text ->
        val colWidth = totalWidth * columnWidths[index]
        canvas.drawText(text, currentX + colWidth / 2, centerY, headerPaint)
        currentX += colWidth
    }

    canvas.drawLine(margin.toFloat(), (yPos + headerHeight).toFloat(), (pageWidth - margin).toFloat(), (yPos + headerHeight).toFloat(), linePaint)
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
        color = Color.BLACK
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
    }
    val linePaint = Paint().apply { strokeWidth = 0.5f * density }
    // Mêmes proportions que pour l'en-tête
    val columnWidths = listOf(0.1f, 0.15f, 0.55f, 0.2f)
    val totalWidth = pageWidth - 2 * margin
    var yPos = startY

    meals.forEachIndexed { index, (dayMeals, meal) ->
        val rowHeight = calculateRowHeight(meal, density, pageWidth, margin)
        val rowPaint = Paint().apply {
            color = if (index % 2 == 0) Color.parseColor("#F8F8F8") else Color.WHITE
        }

        canvas.drawRect(margin.toFloat(), yPos.toFloat(), (pageWidth - margin).toFloat(), (yPos + rowHeight).toFloat(), rowPaint)
        val centerY = yPos + rowHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2

        // Dessiner les cellules avec les nouvelles largeurs
        var currentX = margin.toFloat()

        // Cellule Jour
        canvas.drawText(dayMeals.day, currentX + (totalWidth * columnWidths[0]) / 2, centerY, textPaint)
        currentX += totalWidth * columnWidths[0]

        // Cellule Repas
        canvas.drawText(meal.type.toFrenchString(), currentX + (totalWidth * columnWidths[1]) / 2, centerY, textPaint)
        currentX += totalWidth * columnWidths[1]

        // Cellule Description
        val descPaint = Paint(textPaint).apply { textAlign = Paint.Align.LEFT }
        drawMultilineText(
            canvas = canvas,
            text = if (!meal.notes.isNullOrBlank()) "${meal.description}\nNotes: ${meal.notes}" else meal.description,
            x = currentX + (5 * density),
            y = yPos + (20 * density).toInt(),
            paint = descPaint,
            maxWidth = totalWidth * columnWidths[2] - (10 * density),
            lineHeight = 20f * density
        )
        currentX += totalWidth * columnWidths[2]

        // Cellule Photo
        meal.photoUri?.let { uriString ->
            try {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { originalBitmap ->
                        val imageSize = (80 * density).toInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, true)
                        canvas.drawBitmap(scaledBitmap,
                            currentX + (totalWidth * columnWidths[3] - imageSize) / 2,
                            yPos + (rowHeight - imageSize) / 2f, null)
                        scaledBitmap.recycle()
                        originalBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                canvas.drawText("-", currentX + (totalWidth * columnWidths[3]) / 2, centerY, textPaint)
            }
        } ?: canvas.drawText("-", currentX + (totalWidth * columnWidths[3]) / 2, centerY, textPaint)

        yPos += rowHeight
        canvas.drawLine(margin.toFloat(), yPos.toFloat(), (pageWidth - margin).toFloat(), yPos.toFloat(), linePaint)
        yPos += (2 * density).toInt()
    }

    return yPos
}

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
    var linesDrawn = 0

    text.split("\n").forEach { line ->
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
        } else if (linesDrawn < maxLines) {
            canvas.drawText(line, x, currentY, paint)
            currentY += lineHeight
            linesDrawn++
        }
    }
}

private fun sendToDietician(context: Context, file: File, format: String, email: String) {
    try {
        val uris = ArrayList<Uri>()
        val baseName = file.nameWithoutExtension

        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))

        if (format == "Image") {
            var pageNum = 2
            while (true) {
                val pageFile = File(file.parent, "${baseName}_$pageNum.jpg")
                if (!pageFile.exists()) break
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pageFile))
                pageNum++
            }
        }

        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Mon carnet alimentaire")
            putExtra(Intent.EXTRA_TEXT, "Voici mon carnet alimentaire comme convenu. Cordialement.")
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(emailIntent, "Envoyer à la diététicienne"))
    } catch (e: Exception) {
        Toast.makeText(context, "Aucune application email trouvée", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun saveFile(
    context: Context,
    weekMeals: List<DayMeals>,
    format: String,
    onProgress: (Float) -> Unit = {}
): String? = withContext(Dispatchers.IO) {
    try {
        // Créer le fichier temporaire
        val tempFile = createExportFile(context, weekMeals, format, onProgress) ?: return@withContext null
        val baseName = tempFile.nameWithoutExtension
        val mimeType = when (format) {
            "PDF" -> "application/pdf"
            else -> "image/jpeg"
        }

        // Pour Android 10+ (Q)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$baseName.${if (format == "PDF") "pdf" else "jpg"}")
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    if (format == "PDF")
                        Environment.DIRECTORY_DOCUMENTS + "/FoodDiary"
                    else
                        Environment.DIRECTORY_PICTURES + "/FoodDiary")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                if (format == "PDF")
                    MediaStore.Files.getContentUri("external")
                else
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext null

            // Copier le fichier temporaire
            resolver.openOutputStream(uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            // Pour les images multi-pages
            if (format == "Image") {
                var pageNum = 2
                while (true) {
                    val pageFile = File(tempFile.parent, "${baseName}_$pageNum.jpg")
                    if (!pageFile.exists()) break

                    val pageContentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "${baseName}_$pageNum.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FoodDiary")
                    }

                    val pageUri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        pageContentValues
                    ) ?: continue

                    resolver.openOutputStream(pageUri)?.use { output ->
                        pageFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    pageNum++
                }
            }

            return@withContext if (format == "PDF") {
                "PDF enregistré dans Documents/FoodDiary"
            } else {
                "Image(s) enregistrée(s) dans Photos/FoodDiary"
            }
        }
        // Pour Android <10
        else {
            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(
                    if (format == "PDF")
                        Environment.DIRECTORY_DOCUMENTS
                    else
                        Environment.DIRECTORY_PICTURES
                ),
                "FoodDiary"
            ).apply { mkdirs() }

            // Copier le fichier principal
            val destFile = File(targetDir, tempFile.name)
            tempFile.copyTo(destFile, true)

            // Pour les images multi-pages
            if (format == "Image") {
                var pageNum = 2
                while (true) {
                    val pageFile = File(tempFile.parent, "${baseName}_$pageNum.jpg")
                    if (!pageFile.exists()) break

                    File(targetDir, "${baseName}_$pageNum.jpg").let { destPageFile ->
                        pageFile.copyTo(destPageFile, true)
                    }
                    pageNum++
                }
            }

            // Scanner le fichier pour qu'il apparaisse dans la galerie (pour les images)
            if (format == "Image") {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(targetDir)
                context.sendBroadcast(mediaScanIntent)
            }

            return@withContext if (format == "PDF") {
                "PDF enregistré dans Documents/FoodDiary"
            } else {
                "Image(s) enregistrée(s) dans Photos/FoodDiary"
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}