package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class ReportTableData(
    val title: String,
    val subtitle: String,
    val kpis: List<Pair<String, String>> = emptyList(),
    val headers: List<String>,
    val rows: List<List<String>>,
    val footerRow: List<String>? = null,
    val showSignature: Boolean = false
)

object ReportExporter {

    fun renderReportBitmap(data: ReportTableData): Bitmap {
        val width = 1240
        val padding = 64f
        val rowHeight = 52f
        val headerHeight = 160f
        val kpiHeight = if (data.kpis.isNotEmpty()) 110f else 0f
        val tableHeaderHeight = 56f
        val footerHeight = if (data.footerRow != null) 56f else 0f
        val signatureHeight = if (data.showSignature) 140f else 40f

        val totalHeight = (padding * 2 +
            headerHeight +
            kpiHeight +
            tableHeaderHeight +
            (data.rows.size.coerceAtLeast(1) * rowHeight) +
            footerHeight +
            signatureHeight).toInt().coerceAtLeast(900)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6E56")
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#555550")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B6B66")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val kpiValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F6E56")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1D1B")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1D1B")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#F2F6F4")
            style = Paint.Style.FILL
        }

        var y = padding + 44f
        canvas.drawText("Maison La Grance divine", width / 2f, y, brandPaint)
        y += 40f
        canvas.drawText(data.subtitle, width / 2f, y, subtitlePaint)
        y += 36f
        canvas.drawLine(padding, y, width - padding, y, borderPaint)
        y += 36f

        if (data.kpis.isNotEmpty()) {
            val colW = (width - padding * 2) / data.kpis.size
            data.kpis.forEachIndexed { idx, (label, value) ->
                val x = padding + idx * colW
                canvas.drawText(label, x, y + 24f, kpiLabelPaint)
                canvas.drawText(value, x, y + 66f, kpiValuePaint)
            }
            y += kpiHeight
        }

        val tableWidth = width - padding * 2
        val colCount = data.headers.size.coerceAtLeast(1)
        val colWidth = tableWidth / colCount

        // Table Header
        canvas.drawRect(padding, y, width - padding, y + tableHeaderHeight, headerBgPaint)
        canvas.drawRect(padding, y, width - padding, y + tableHeaderHeight, borderPaint)
        data.headers.forEachIndexed { idx, h ->
            canvas.drawText(h, padding + idx * colWidth + 12f, y + 36f, thPaint)
        }
        y += tableHeaderHeight

        // Table Rows
        if (data.rows.isEmpty()) {
            canvas.drawRect(padding, y, width - padding, y + rowHeight, borderPaint)
            canvas.drawText("Aucune donnée", padding + 12f, y + 34f, tdPaint)
            y += rowHeight
        } else {
            data.rows.forEach { row ->
                canvas.drawRect(padding, y, width - padding, y + rowHeight, borderPaint)
                row.forEachIndexed { idx, cell ->
                    if (idx < colCount) {
                        val clipped = if (cell.length > 28) cell.take(25) + "..." else cell
                        canvas.drawText(clipped, padding + idx * colWidth + 12f, y + 34f, tdPaint)
                    }
                }
                y += rowHeight
            }
        }

        // Footer Row
        if (data.footerRow != null) {
            canvas.drawRect(padding, y, width - padding, y + footerHeight, headerBgPaint)
            canvas.drawRect(padding, y, width - padding, y + footerHeight, borderPaint)
            data.footerRow.forEachIndexed { idx, cell ->
                if (idx < colCount) {
                    canvas.drawText(cell, padding + idx * colWidth + 12f, y + 36f, thPaint)
                }
            }
            y += footerHeight
        }

        // Signature
        if (data.showSignature) {
            y += 80f
            val sigStart = width - padding - 260f
            val sigEnd = width - padding
            canvas.drawLine(sigStart, y, sigEnd, y, borderPaint)
            val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#555550")
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Signature", (sigStart + sigEnd) / 2f, y + 30f, sigPaint)
        }

        return bitmap
    }

    fun saveJpegToGalleryAndCache(context: Context, title: String, bitmap: Bitmap): Pair<Boolean, Uri?> {
        val safeName = title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-') + "-" + todayDateKey() + ".jpg"

        var savedToGallery = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GestionBoutique")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    savedToGallery = true
                }
            } catch (_: Exception) {
                savedToGallery = false
            }
        }

        val shareUri = try {
            val cacheFile = File(context.cacheDir, safeName)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        } catch (_: Exception) {
            null
        }

        return savedToGallery to shareUri
    }

    fun shareImageUri(context: Context, uri: Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager $title"))
    }

    fun printBitmap(context: Context, jobName: String, bitmap: Bitmap) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdf = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdf.finishPage(page)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdf.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }
}
