package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.NotificationRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to export sender notification history to TXT and PDF documents and share them.
 */
object HistoryExportUtils {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

  /**
   * Generates a plain text transcript of the sender's notifications and triggers a system share intent.
   */
  fun exportAsTxt(
    context: Context,
    appName: String,
    packageName: String,
    records: List<NotificationRecord>,
  ): Boolean {
    return try {
      val fileName = "Notification_History_${appName.replace(" ", "_")}_${System.currentTimeMillis()}.txt"
      val file = File(context.cacheDir, fileName)

      val sb = StringBuilder()
      sb.append("=========================================\n")
      sb.append("NOTIFICATION HISTORY TRANSCRIPT\n")
      sb.append("Application: $appName ($packageName)\n")
      sb.append("Total Alerts: ${records.size}\n")
      sb.append("Exported At: ${dateFormat.format(Date())}\n")
      sb.append("=========================================\n\n")

      records.forEachIndexed { index, record ->
        sb.append("[#${index + 1}] Date: ${dateFormat.format(Date(record.postTime))}\n")
        if (record.isDeleted) {
          sb.append("Status: [DELETED / RECALLED BY SENDER]\n")
        }
        if (record.title.isNotBlank()) {
          sb.append("Title: ${record.title}\n")
        }
        if (record.content.isNotBlank()) {
          sb.append("Content: ${record.content}\n")
        }
        sb.append("-----------------------------------------\n\n")
      }

      file.writeText(sb.toString())
      shareFile(context, file, "text/plain", "Share $appName Notification History (TXT)")
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Generates a PDF document containing the sender's notification history and triggers a system share intent.
   */
  fun exportAsPdf(
    context: Context,
    appName: String,
    packageName: String,
    records: List<NotificationRecord>,
  ): Boolean {
    return try {
      val pdfDocument = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (in points)
      var currentPageNumber = 1
      var page = pdfDocument.startPage(pageInfo)
      var canvas = page.canvas

      val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        isAntiAlias = true
      }
      val paintHeader = Paint().apply {
        color = Color.rgb(30, 41, 59)
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val paintSubtitle = Paint().apply {
        color = Color.rgb(100, 116, 139)
        textSize = 9f
        isAntiAlias = true
      }
      val paintDivider = Paint().apply {
        color = Color.rgb(226, 232, 240)
        strokeWidth = 1f
      }
      val paintDeleted = Paint().apply {
        color = Color.rgb(220, 38, 38)
        textSize = 9f
        isFakeBoldText = true
        isAntiAlias = true
      }

      var yPos = 40f
      val margin = 40f
      val maxY = 800f

      fun drawHeader(c: Canvas) {
        c.drawText("Notification History: $appName", margin, 40f, paintHeader)
        c.drawText("Package: $packageName | Total Alerts: ${records.size} | Exported: ${dateFormat.format(Date())}", margin, 55f, paintSubtitle)
        c.drawLine(margin, 65f, 595f - margin, 65f, paintDivider)
      }

      drawHeader(canvas)
      yPos = 85f

      records.forEachIndexed { index, record ->
        // Estimate height needed for this record: ~50-60 points
        if (yPos > maxY - 70f) {
          pdfDocument.finishPage(page)
          currentPageNumber++
          val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
          page = pdfDocument.startPage(newPageInfo)
          canvas = page.canvas
          drawHeader(canvas)
          yPos = 85f
        }

        // Draw date and index
        val dateStr = "[#${index + 1}] ${dateFormat.format(Date(record.postTime))}"
        canvas.drawText(dateStr, margin, yPos, paintSubtitle)
        if (record.isDeleted) {
          canvas.drawText(" [RECALLED / DELETED]", margin + 180f, yPos, paintDeleted)
        }
        yPos += 14f

        // Draw title
        if (record.title.isNotBlank()) {
          val cleanTitle = if (record.title.length > 80) record.title.take(77) + "..." else record.title
          paintText.isFakeBoldText = true
          canvas.drawText(cleanTitle, margin, yPos, paintText)
          paintText.isFakeBoldText = false
          yPos += 14f
        }

        // Draw content
        if (record.content.isNotBlank()) {
          val cleanContent = if (record.content.length > 95) record.content.take(92) + "..." else record.content
          canvas.drawText(cleanContent, margin, yPos, paintText)
          yPos += 14f
        }

        canvas.drawLine(margin, yPos, 595f - margin, yPos, paintDivider)
        yPos += 14f
      }

      pdfDocument.finishPage(page)

      val fileName = "Notification_History_${appName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
      val file = File(context.cacheDir, fileName)
      val fos = FileOutputStream(file)
      pdfDocument.writeTo(fos)
      fos.close()
      pdfDocument.close()

      shareFile(context, file, "application/pdf", "Share $appName Notification History (PDF)")
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(shareIntent, title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
  }
}
