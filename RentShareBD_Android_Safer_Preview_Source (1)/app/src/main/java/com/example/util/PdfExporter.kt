package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.ExpenseEntity
import com.example.data.local.PropertyEntity
import com.example.data.local.RentalAgreementEntity
import com.example.data.local.UserEntity
import com.example.domain.DomainLogic
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportAgreementPdf(
        context: Context,
        agreement: RentalAgreementEntity,
        property: PropertyEntity?,
        tenant: UserEntity?,
        landlord: UserEntity?
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(79, 70, 229)
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, 595f, 100f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("RENTSHARE BD — DIGITAL RENTAL AGREEMENT", 36f, 48f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = Color.rgb(224, 231, 255)
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText(
            "Agreement #AG-${agreement.id}  |  Status: ${agreement.status.uppercase()}",
            36f,
            74f,
            subtitlePaint
        )

        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 11.5f
            isAntiAlias = true
        }

        var y = 138f
        fun drawLineItem(label: String, value: String) {
            canvas.drawText("$label: $value", 36f, y, bodyPaint)
            y += 22f
        }

        canvas.drawText("1. PARTIES & PROPERTY", 36f, y, sectionPaint)
        y += 24f
        drawLineItem("Landlord", "${landlord?.fullName ?: "Landlord"} (${landlord?.phone ?: ""})")
        drawLineItem("Tenant", "${tenant?.fullName ?: "Tenant"} (${tenant?.phone ?: ""})")
        drawLineItem("Property", property?.title ?: "Property #${agreement.propertyId}")
        drawLineItem("Address", property?.address ?: "Dhaka, Bangladesh")

        y += 12f
        canvas.drawText("2. FINANCIAL & LEASE TERMS", 36f, y, sectionPaint)
        y += 24f
        drawLineItem("Monthly Rent", DomainLogic.formatBdt(agreement.monthlyRent))
        drawLineItem("Advance Deposit", DomainLogic.formatBdt(agreement.advanceAmount))
        drawLineItem("Duration", "${agreement.durationMonths} months")
        drawLineItem("Lease Period", "${agreement.startDate} to ${agreement.endDate}")
        drawLineItem("Notice Period", "${agreement.noticePeriodDays} days prior notice")

        y += 12f
        canvas.drawText("3. UTILITY TERMS", 36f, y, sectionPaint)
        y += 22f
        drawWrappedText(canvas, agreement.utilityTerms, 36f, y, 520f, bodyPaint).also { y = it + 16f }

        canvas.drawText("4. HOUSE RULES", 36f, y, sectionPaint)
        y += 22f
        drawWrappedText(canvas, agreement.houseRules, 36f, y, 520f, bodyPaint).also { y = it + 24f }

        canvas.drawText("5. DIGITAL ACCEPTANCE", 36f, y, sectionPaint)
        y += 24f
        drawLineItem("Landlord Accepted", if (agreement.landlordAccepted) "YES (Signed)" else "PENDING")
        drawLineItem(
            "Tenant Accepted",
            if (agreement.tenantAccepted) "YES (${agreement.tenantAcceptedAt ?: "Verified"})" else "PENDING"
        )

        doc.finishPage(page)

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(outDir, "RentShareBD_Agreement_${agreement.id}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun exportExpensesCsv(
        context: Context,
        groupName: String,
        expenses: List<ExpenseEntity>,
        usersById: Map<Long, UserEntity>
    ): File {
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(outDir, "RentShareBD_Expenses.csv")
        val content = buildString {
            appendLine("ID,Group,Date,Category,Title,Amount_BDT,PaidBy,SplitType,Notes")
            expenses.forEach { exp ->
                val payer = usersById[exp.paidByUserId]?.fullName ?: "User ${exp.paidByUserId}"
                val safeTitle = exp.title.replace("\"", "\"\"")
                val safeNotes = exp.notes.replace("\"", "\"\"")
                appendLine("${exp.id},\"$groupName\",${exp.expenseDate},${exp.category},\"$safeTitle\",${exp.amount},\"$payer\",${exp.splitType},\"$safeNotes\"")
            }
        }
        file.writeText(content)
        return file
    }

    fun exportAdminReportJson(context: Context, reportJson: String): File {
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(outDir, "RentShareBD_Admin_Report.json")
        file.writeText(reportJson)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var y = startY
        val words = text.split(" ")
        var line = StringBuilder()
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "${line} $word"
            if (paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += 18f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += 18f
        }
        return y
    }
}
