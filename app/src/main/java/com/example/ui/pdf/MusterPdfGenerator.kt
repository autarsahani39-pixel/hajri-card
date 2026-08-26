package com.example.ui.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayType
import com.example.data.model.SignatureRecord
import com.example.data.model.Worker
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object MusterPdfGenerator {

    private const val TAG = "MusterPdfGenerator"

    fun generateAndShareMusterPdf(
        context: Context,
        worker: Worker,
        yearMonth: YearMonth,
        attendances: List<Attendance>,
        signatureRecord: SignatureRecord?,
        calculatedSalary: Double,
        totalAdvance: Double,
        balancePayable: Double
    ): File? {
        var pdfDoc: PdfDocument? = null
        var fos: FileOutputStream? = null
        try {
            pdfDoc = PdfDocument()
            // Standard A4 dimensions in points: 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.DKGRAY
                strokeWidth = 1f
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            // Draw outer border
            val margin = 24f
            val contentWidth = pageWidth - (margin * 2)
            strokePaint.strokeWidth = 1.5f
            strokePaint.color = Color.BLACK
            canvas.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, strokePaint)

            var currentY = margin + 20f

            // --- HEADER TITLE ---
            fillPaint.color = Color.rgb(30, 58, 138) // Deep Blue
            canvas.drawRect(margin, margin, pageWidth - margin, margin + 42f, fillPaint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            paint.textSize = 17f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("MUSTER CARD / ATTENDANCE REGISTER", pageWidth / 2f, margin + 28f, paint)

            currentY = margin + 56f

            // Company info
            val companyName = if (worker.companyName.isNotBlank()) worker.companyName else "Hajri Card - Attendance Register"
            paint.color = Color.BLACK
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(companyName, pageWidth / 2f, currentY, paint)
            currentY += 16f

            val address = if (worker.address.isNotBlank()) worker.address else ""
            if (address.isNotBlank()) {
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Address: $address", pageWidth / 2f, currentY, paint)
                currentY += 20f
            } else {
                currentY += 8f
            }

            // Horizontal separator
            strokePaint.strokeWidth = 1f
            strokePaint.color = Color.LTGRAY
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, strokePaint)
            currentY += 8f

            // --- WORKER DETAILS INFO BOX ---
            paint.textAlign = Paint.Align.LEFT
            val col1X = margin + 12f
            val col2X = margin + (contentWidth / 2f) + 12f

            paint.textSize = 9.5f
            // Row 1
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Worker Name: ", col1X, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val workerNameSafe = worker.name.ifBlank { "Worker" }
            canvas.drawText(workerNameSafe, col1X + 100f, currentY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Month & Year: ", col2X, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val monthLabel = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()} ${yearMonth.year}"
            canvas.drawText(monthLabel, col2X + 100f, currentY, paint)
            currentY += 14f

            // Row 2
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Father's Name: ", col1X, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (worker.fatherName.isNotBlank()) worker.fatherName else "—", col1X + 100f, currentY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Worker ID: ", col2X, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (worker.workerId.isNotBlank()) worker.workerId else "WRK-${worker.id}", col2X + 100f, currentY, paint)
            currentY += 14f

            // Row 3
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Job / Role: ", col1X, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (worker.job.isNotBlank()) worker.job else "Worker", col1X + 100f, currentY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Pay Rate: ", col2X, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val payRateText = if (worker.payType == PayType.DAILY) {
                "₹${String.format(Locale.ENGLISH, "%.0f", worker.dailyWage)} / Day"
            } else {
                "₹${String.format(Locale.ENGLISH, "%.0f", worker.monthlyPay)} / Month"
            }
            canvas.drawText(payRateText, col2X + 100f, currentY, paint)
            currentY += 16f

            // --- TWO COLUMN 1..31 TABLE ---
            val daysInMonth = yearMonth.lengthOfMonth()
            val colTableWidth = (contentWidth - 10f) / 2f
            val tableTopY = currentY
            val rowHeight = 16.5f

            val attendanceMap = attendances.associateBy { it.date }

            fun drawTableColumn(startX: Float, startDay: Int, endDay: Int) {
                val endX = startX + colTableWidth
                var rY = tableTopY

                // Header Row
                fillPaint.color = Color.rgb(241, 245, 249)
                canvas.drawRect(startX, rY, endX, rY + rowHeight, fillPaint)
                strokePaint.color = Color.BLACK
                strokePaint.strokeWidth = 1f
                canvas.drawRect(startX, rY, endX, rY + rowHeight, strokePaint)

                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                paint.color = Color.BLACK
                paint.textAlign = Paint.Align.CENTER

                val dateColW = 48f
                val statusColW = 42f
                val advColW = 54f
                val signColW = colTableWidth - dateColW - statusColW - advColW

                canvas.drawText("Date", startX + (dateColW / 2f), rY + 11.5f, paint)
                canvas.drawText("Status", startX + dateColW + (statusColW / 2f), rY + 11.5f, paint)
                canvas.drawText("Advance", startX + dateColW + statusColW + (advColW / 2f), rY + 11.5f, paint)
                canvas.drawText("Sign/Note", startX + dateColW + statusColW + advColW + (signColW / 2f), rY + 11.5f, paint)

                // Vertical column lines for header
                canvas.drawLine(startX + dateColW, rY, startX + dateColW, rY + rowHeight, strokePaint)
                canvas.drawLine(startX + dateColW + statusColW, rY, startX + dateColW + statusColW, rY + rowHeight, strokePaint)
                canvas.drawLine(startX + dateColW + statusColW + advColW, rY, startX + dateColW + statusColW + advColW, rY + rowHeight, strokePaint)

                rY += rowHeight

                for (day in startDay..endDay) {
                    val isPastOrCurrentMonthDay = day <= daysInMonth
                    val dateStr = if (isPastOrCurrentMonthDay) {
                        String.format(Locale.ENGLISH, "%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, day)
                    } else ""
                    val att = if (isPastOrCurrentMonthDay) attendanceMap[dateStr] else null

                    // Background zebra striping or weekend highlight
                    val dayOfWeek = if (isPastOrCurrentMonthDay) {
                        try {
                            LocalDate.of(yearMonth.year, yearMonth.month, day).dayOfWeek.value
                        } catch (e: Exception) {
                            0
                        }
                    } else 0
                    val isSunday = dayOfWeek == 7

                    if (isSunday) {
                        fillPaint.color = Color.rgb(254, 242, 242) // Light red for Sunday
                        canvas.drawRect(startX, rY, endX, rY + rowHeight, fillPaint)
                    } else if (day % 2 == 0) {
                        fillPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(startX, rY, endX, rY + rowHeight, fillPaint)
                    }

                    // Row border
                    canvas.drawRect(startX, rY, endX, rY + rowHeight, strokePaint)

                    // Vertical lines
                    canvas.drawLine(startX + dateColW, rY, startX + dateColW, rY + rowHeight, strokePaint)
                    canvas.drawLine(startX + dateColW + statusColW, rY, startX + dateColW + statusColW, rY + rowHeight, strokePaint)
                    canvas.drawLine(startX + dateColW + statusColW + advColW, rY, startX + dateColW + statusColW + advColW, rY + rowHeight, strokePaint)

                    if (isPastOrCurrentMonthDay) {
                        // Date column
                        paint.textSize = 8f
                        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                        paint.color = if (isSunday) Color.rgb(185, 28, 28) else Color.BLACK
                        paint.textAlign = Paint.Align.CENTER
                        val dayShort = try {
                            LocalDate.of(yearMonth.year, yearMonth.month, day).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        } catch (e: Exception) {
                            ""
                        }
                        canvas.drawText(String.format(Locale.ENGLISH, "%02d %s", day, dayShort), startX + (dateColW / 2f), rY + 11.5f, paint)

                        // Status column
                        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                        val statusCode = att?.status?.code ?: "—"
                        val statusColor = when (att?.status) {
                            AttendanceStatus.PRESENT -> Color.rgb(22, 163, 74)
                            AttendanceStatus.ABSENT -> Color.rgb(220, 38, 38)
                            AttendanceStatus.HALF_DAY -> Color.rgb(37, 99, 235)
                            AttendanceStatus.LEAVE -> Color.rgb(217, 119, 6)
                            AttendanceStatus.DOUBLE_PRESENT -> Color.rgb(124, 58, 237)
                            null -> Color.GRAY
                        }
                        paint.color = statusColor
                        canvas.drawText(statusCode, startX + dateColW + (statusColW / 2f), rY + 11.5f, paint)

                        // Advance column
                        val advVal = att?.advance ?: 0.0
                        paint.color = if (advVal > 0) Color.rgb(234, 88, 12) else Color.GRAY
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        val advText = if (advVal > 0) "₹${String.format(Locale.ENGLISH, "%.0f", advVal)}" else "—"
                        canvas.drawText(advText, startX + dateColW + statusColW + (advColW / 2f), rY + 11.5f, paint)

                        // Sign / remark column
                        paint.color = Color.DKGRAY
                        paint.textSize = 7f
                        val remarkText = if (att?.remark?.isNotBlank() == true) att.remark else if (statusCode == "P") "✓" else "—"
                        canvas.drawText(remarkText, startX + dateColW + statusColW + advColW + (signColW / 2f), rY + 11.5f, paint)
                    }

                    rY += rowHeight
                }
            }

            // Draw left column (Days 1 to 16)
            drawTableColumn(margin, 1, 16)
            // Draw right column (Days 17 to 31)
            drawTableColumn(margin + colTableWidth + 10f, 17, 31)

            currentY = tableTopY + (17 * rowHeight) + 16f

            // --- BOTTOM SUMMARY CALCULATIONS BOX ---
            val summaryBoxHeight = 58f
            fillPaint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + summaryBoxHeight, fillPaint)
            strokePaint.strokeWidth = 1.2f
            strokePaint.color = Color.BLACK
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + summaryBoxHeight, strokePaint)

            val pDays = attendances.count { it.status == AttendanceStatus.PRESENT }
            val hdDays = attendances.count { it.status == AttendanceStatus.HALF_DAY }
            val lDays = attendances.count { it.status == AttendanceStatus.LEAVE }
            val ppDays = attendances.count { it.status == AttendanceStatus.DOUBLE_PRESENT }
            val aDays = attendances.count { it.status == AttendanceStatus.ABSENT }
            val totalWorkedDays = (pDays.toDouble() * 1.0) + (hdDays.toDouble() * 0.5) + (lDays.toDouble() * 1.5) + (ppDays.toDouble() * 2.0)

            val sumColW = contentWidth / 3f
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 8.5f

            // Column 1: Days breakdown
            var sY = currentY + 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            paint.color = Color.rgb(22, 163, 74)
            canvas.drawText("P: $pDays", margin + 8f, sY, paint)
            paint.color = Color.rgb(37, 99, 235)
            canvas.drawText("½: $hdDays", margin + 8f + 40f, sY, paint)
            paint.color = Color.rgb(217, 119, 6)
            canvas.drawText("P½: $lDays", margin + 8f + 78f, sY, paint)
            paint.color = Color.rgb(124, 58, 237)
            canvas.drawText("PP: $ppDays", margin + 8f + 120f, sY, paint)
            sY += 18f
            paint.color = Color.rgb(220, 38, 38)
            canvas.drawText("A: $aDays", margin + 8f, sY, paint)
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            val workedFmt = if (totalWorkedDays % 1.0 == 0.0) "${totalWorkedDays.toInt()}" else "$totalWorkedDays"
            canvas.drawText("Total Worked: $workedFmt Days", margin + 8f + 40f, sY, paint)

            // Column 2: Total Pay & Advance
            sY = currentY + 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            paint.color = Color.BLACK
            canvas.drawText("Total Pay: ", margin + sumColW + 10f, sY, paint)
            paint.color = Color.rgb(30, 58, 138)
            canvas.drawText("₹ ${String.format(Locale.ENGLISH, "%,.0f", calculatedSalary)}", margin + sumColW + 70f, sY, paint)
            sY += 18f
            paint.color = Color.BLACK
            canvas.drawText("Advance: ", margin + sumColW + 10f, sY, paint)
            paint.color = Color.rgb(234, 88, 12)
            canvas.drawText("₹ ${String.format(Locale.ENGLISH, "%,.0f", totalAdvance)}", margin + sumColW + 70f, sY, paint)

            // Column 3: Net Balance
            sY = currentY + 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            paint.color = Color.BLACK
            canvas.drawText("Balance Payable:", margin + (sumColW * 2) + 10f, sY, paint)
            sY += 22f
            paint.textSize = 13f
            paint.color = if (balancePayable >= 0) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            canvas.drawText("₹ ${String.format(Locale.ENGLISH, "%,.0f", balancePayable)}", margin + (sumColW * 2) + 10f, sY, paint)

            currentY += summaryBoxHeight + 20f

            // --- SIGNATURE SECTION ---
            val sigBoxWidth = (contentWidth - 20f) / 2f
            val sigBoxHeight = 85f

            // Box 1: Receiver's Signature / Thumb Impression
            strokePaint.strokeWidth = 1f
            strokePaint.color = Color.GRAY
            canvas.drawRect(margin, currentY, margin + sigBoxWidth, currentY + sigBoxHeight, strokePaint)

            paint.textSize = 8.5f
            paint.color = Color.DKGRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "RECEIVER'S SIGNATURE / THUMB IMPRESSION",
                margin + (sigBoxWidth / 2f),
                currentY + 14f,
                paint
            )
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(
                "(Worker's Signature / Thumb Impression)",
                margin + (sigBoxWidth / 2f),
                currentY + 24f,
                paint
            )

            // Draw signature if available
            if (signatureRecord != null && signatureRecord.signatureSvg.isNotBlank()) {
                drawSignatureOnCanvas(
                    canvas,
                    signatureRecord.signatureSvg,
                    margin + 10f,
                    currentY + 28f,
                    sigBoxWidth - 20f,
                    sigBoxHeight - 32f
                )
            } else {
                paint.color = Color.LTGRAY
                paint.textSize = 8f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Signature Not Available", margin + (sigBoxWidth / 2f), currentY + 54f, paint)
            }

            // Box 2: Manager / Employer Signature
            val box2X = margin + sigBoxWidth + 20f
            canvas.drawRect(box2X, currentY, box2X + sigBoxWidth, currentY + sigBoxHeight, strokePaint)

            paint.color = Color.DKGRAY
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText(
                "EMPLOYER / MANAGER SIGNATURE",
                box2X + (sigBoxWidth / 2f),
                currentY + 14f,
                paint
            )
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(
                "(Employer / Manager Signature)",
                box2X + (sigBoxWidth / 2f),
                currentY + 24f,
                paint
            )

            // Line for manager signature
            canvas.drawLine(box2X + 24f, currentY + 65f, box2X + sigBoxWidth - 24f, currentY + 65f, strokePaint)
            paint.textSize = 7.5f
            paint.color = Color.GRAY
            canvas.drawText("Authorized Signatory", box2X + (sigBoxWidth / 2f), currentY + 76f, paint)

            // Footer note
            paint.textSize = 7.5f
            paint.color = Color.GRAY
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "Generated by Hajri Card App • Digital Record of Attendance & Wages",
                pageWidth / 2f,
                pageHeight - margin - 8f,
                paint
            )

            pdfDoc.finishPage(page)

            // Prepare safe folder and safe file name
            val safeName = worker.name.trim()
                .replace("[^a-zA-Z0-9\\u0900-\\u097F_-]".toRegex(), "_")
                .take(30)
                .ifBlank { "Worker_${worker.id}" }
            val fileName = "Muster_Card_${safeName}_${yearMonth.year}_${yearMonth.monthValue}.pdf"
            
            val targetDir = File(context.cacheDir, "pdf_documents").apply { mkdirs() }
            val pdfFile = File(targetDir, fileName)
            
            fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.flush()
            
            Log.d(TAG, "Muster Card PDF generated successfully at: ${pdfFile.absolutePath}")
            return pdfFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Muster Card PDF", e)
            return null
        } finally {
            try { fos?.close() } catch (_: Exception) {}
            try { pdfDoc?.close() } catch (_: Exception) {}
        }
    }

    private fun drawSignatureOnCanvas(
        canvas: Canvas,
        pointsJson: String,
        boxX: Float,
        boxY: Float,
        boxW: Float,
        boxH: Float
    ) {
        try {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLUE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val jsonArray = JSONArray(pointsJson)
            if (jsonArray.length() == 0) return

            // Calculate bounding box of raw points
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (i in 0 until jsonArray.length()) {
                val stroke = jsonArray.getJSONArray(i)
                for (j in 0 until stroke.length()) {
                    val pt = stroke.getJSONObject(j)
                    val px = pt.getDouble("x").toFloat()
                    val py = pt.getDouble("y").toFloat()
                    if (px < minX) minX = px
                    if (py < minY) minY = py
                    if (px > maxX) maxX = px
                    if (py > maxY) maxY = py
                }
            }

            val rawW = (maxX - minX).coerceAtLeast(10f)
            val rawH = (maxY - minY).coerceAtLeast(10f)

            var scale = (boxW / rawW).coerceAtMost(boxH / rawH) * 0.85f
            if (scale.isNaN() || scale.isInfinite() || scale <= 0f) {
                scale = 1f
            }
            val offsetX = boxX + (boxW - (rawW * scale)) / 2f
            val offsetY = boxY + (boxH - (rawH * scale)) / 2f

            for (i in 0 until jsonArray.length()) {
                val stroke = jsonArray.getJSONArray(i)
                if (stroke.length() < 2) continue
                val path = Path()
                for (j in 0 until stroke.length()) {
                    val pt = stroke.getJSONObject(j)
                    val px = (pt.getDouble("x").toFloat() - minX) * scale + offsetX
                    val py = (pt.getDouble("y").toFloat() - minY) * scale + offsetY
                    if (px.isFinite() && py.isFinite()) {
                        if (j == 0) {
                            path.moveTo(px, py)
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                }
                canvas.drawPath(path, strokePaint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing signature on canvas", e)
        }
    }

    /**
     * Share PDF file using standard Android Intent chooser.
     * Guaranteed to never throw unhandled crash exceptions.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Muster Card PDF Attendance")
                putExtra(Intent.EXTRA_TEXT, "Muster Card Attendance Sheet for ${pdfFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Share Muster Card PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing PDF via chooser", e)
            openPdf(context, pdfFile)
        }
    }

    /**
     * Directly opens the PDF in any installed PDF viewer on device.
     */
    fun openPdf(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF directly", e)
            Toast.makeText(context, "PDF saved successfully: ${pdfFile.name}", Toast.LENGTH_LONG).show()
        }
    }
}

