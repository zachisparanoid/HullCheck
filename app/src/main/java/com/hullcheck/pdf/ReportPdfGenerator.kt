package com.hullcheck.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.hullcheck.R
import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.model.Severity
import com.hullcheck.data.model.SurveyType
import com.hullcheck.util.DateFormatters
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Surveyor letterhead / certification details applied to the report. */
data class ReportBranding(
    val firmName: String? = null,
    val licenseNumber: String? = null,
    val logoPath: String? = null,
    val signaturePath: String? = null,
    val disclaimer: String? = null,
    val surveyorName: String? = null,
)

/**
 * Lays out a professional A4 inspection report with android.graphics.pdf.PdfDocument and a
 * Canvas. Handles automatic pagination, per-page header/footer, a defect summary, the full
 * checklist by category, and a 2-up photo appendix. All bitmaps are downsampled before drawing
 * to guard against OutOfMemory on large photo sets.
 */
@Singleton
class ReportPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        const val PAGE_WIDTH = 595   // A4 @ 72dpi
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val HEADER_HEIGHT = 54f
        const val FOOTER_HEIGHT = 28f
        val CONTENT_LEFT = MARGIN
        val CONTENT_RIGHT = PAGE_WIDTH - MARGIN
        val CONTENT_WIDTH = CONTENT_RIGHT - CONTENT_LEFT
        val CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT

        const val INK = 0xFF11212B.toInt()
        const val MUTED = 0xFF5C6B78.toInt()
        const val TEAL = 0xFF0E8F8F.toInt()
        const val HAIR = 0xFFD7DEE4.toInt()
        const val PANEL = 0xFFF1F5F8.toInt()
    }

    private val regular: Typeface = loadFont()
    private val bold: Typeface = Typeface.create(regular, Typeface.BOLD)

    private fun loadFont(): Typeface =
        runCatching { ResourcesCompat.getFont(context, R.font.inter_variable) }.getOrNull()
            ?: Typeface.SANS_SERIF

    fun generate(
        project: Project,
        progress: ProjectProgress,
        branding: ReportBranding,
        outputFile: File,
    ): File {
        val doc = PdfDocument()
        val state = PageState(doc, project.boatName, branding)
        state.startPage()

        drawCover(state, project, branding)
        drawSummary(state, project, progress)
        drawDefectSummary(state, project)
        drawChecklist(state, project)
        drawCertification(state, project, branding)
        drawPhotoAppendix(state, project)

        state.finishPage()
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { doc.writeTo(it) }
        doc.close()
        return outputFile
    }

    // ---------- Page / pagination state ----------

    private inner class PageState(val doc: PdfDocument, val boatName: String, val branding: ReportBranding) {
        private var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y: Float = 0f
        private var pageNumber = 0

        fun startPage() {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = doc.startPage(info)
            canvas = page!!.canvas
            y = MARGIN + HEADER_HEIGHT
            drawHeader()
            drawFooter()
        }

        fun finishPage() {
            page?.let { doc.finishPage(it) }
            page = null
            canvas = null
        }

        /** Ensure [needed] vertical points are available, else break to a new page. */
        fun ensure(needed: Float) {
            if (y + needed > CONTENT_BOTTOM) {
                finishPage()
                startPage()
            }
        }

        private fun drawHeader() {
            val c = canvas ?: return
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = bold; textSize = 9f; color = TEAL; letterSpacing = 0.14f
            }
            val name = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 12f; color = INK }
            val headerLabel = branding.firmName?.takeIf { it.isNotBlank() }?.uppercase() ?: "HULLCHECK SURVEY"
            c.drawText(ellipsize(headerLabel, label, 280f), CONTENT_LEFT, MARGIN + 6f, label)
            val nm = ellipsize(boatName.uppercase(), name, 300f)
            c.drawText(nm, CONTENT_RIGHT - name.measureText(nm), MARGIN + 6f, name)
            val line = Paint().apply { color = HAIR; strokeWidth = 1f }
            c.drawLine(CONTENT_LEFT, MARGIN + 16f, CONTENT_RIGHT, MARGIN + 16f, line)
        }

        private fun drawFooter() {
            val c = canvas ?: return
            val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular; textSize = 8f; color = MUTED }
            val line = Paint().apply { color = HAIR; strokeWidth = 1f }
            val fy = PAGE_HEIGHT - MARGIN - 10f
            c.drawLine(CONTENT_LEFT, fy - 10f, CONTENT_RIGHT, fy - 10f, line)
            val generated = context.getString(
                R.string.report_generated_by,
                DateFormatters.formatDate(Instant.now()),
            )
            c.drawText(generated, CONTENT_LEFT, fy, foot)
            val pageText = context.getString(R.string.report_page, pageNumber)
            c.drawText(pageText, CONTENT_RIGHT - foot.measureText(pageText), fy, foot)
        }
    }

    // ---------- Sections ----------

    private fun drawCover(s: PageState, project: Project, branding: ReportBranding) {
        branding.logoPath?.let { path ->
            loadBitmap(path, 600)?.let { bmp ->
                val h = 44f
                val w = (bmp.width * (h / bmp.height)).coerceAtMost(220f)
                s.canvas?.drawBitmap(bmp, null, RectF(CONTENT_LEFT, s.y, CONTENT_LEFT + w, s.y + h), null)
                bmp.recycle()
                s.y += h + 12f
            }
        }
        val label = paint(bold, 9f, TEAL).apply { letterSpacing = 0.16f }
        s.canvas?.drawText("INSPECTION REPORT", CONTENT_LEFT, s.y, label)
        s.y += 26f

        val title = paint(bold, 24f, INK)
        for (line in wrap(project.boatName, title, CONTENT_WIDTH)) {
            s.ensure(30f); s.canvas?.drawText(line, CONTENT_LEFT, s.y, title); s.y += 28f
        }
        s.y += 4f

        project.coverPhotoPath?.let { path ->
            loadBitmap(path, 1200)?.let { bmp ->
                val maxH = 230f
                val scale = minOf(CONTENT_WIDTH / bmp.width, maxH / bmp.height)
                val w = bmp.width * scale
                val h = bmp.height * scale
                s.ensure(h + 12f)
                s.canvas?.drawBitmap(bmp, null, RectF(CONTENT_LEFT, s.y, CONTENT_LEFT + w, s.y + h), null)
                bmp.recycle()
                s.y += h + 14f
            }
        }

        val rows = buildList {
            add("Survey type" to surveyTypeLabel(project.surveyType))
            add("Survey date" to DateFormatters.formatDate(project.surveyDate))
            add("Surveyor" to (project.surveyorName.orDash()))
            add("Client" to (project.clientName.orDash()))
            add("Vessel" to (project.makeModelYear.ifBlank { "—" }))
            add("Boat type" to boatTypeLabel(project.boatType))
            add("Location" to (project.location.orDash()))
            project.coordinatesText?.let { add("Coordinates" to it) }
            add("HIN" to (project.hullId.orDash()))
            add("LOA" to (project.lengthFeet?.let { "%.1f ft".format(it) } ?: "—"))
            add("Registration" to (project.registrationNumber.orDash()))
        }
        drawMetaGrid(s, rows)
    }

    private fun drawMetaGrid(s: PageState, rows: List<Pair<String, String>>) {
        val labelPaint = paint(regular, 8.5f, MUTED)
        val valuePaint = paint(bold, 10.5f, INK)
        val colWidth = CONTENT_WIDTH / 2f
        var i = 0
        while (i < rows.size) {
            s.ensure(34f)
            val rowY = s.y
            for (col in 0..1) {
                val idx = i + col
                if (idx >= rows.size) break
                val x = CONTENT_LEFT + col * colWidth
                val (k, v) = rows[idx]
                s.canvas?.drawText(k.uppercase(), x, rowY, labelPaint)
                val value = ellipsize(v, valuePaint, colWidth - 12f)
                s.canvas?.drawText(value, x, rowY + 14f, valuePaint)
            }
            s.y = rowY + 32f
            i += 2
        }
    }

    private fun drawSummary(s: PageState, project: Project, progress: ProjectProgress) {
        sectionHeading(s, context.getString(R.string.report_section_summary))
        val statLine = "${progress.completed} of ${progress.total} items checked  ·  ${progress.percentInt}% complete"
        body(s, statLine, bold)
        s.y += 2f
        val counts = "OK ${progress.ok}   ·   Defects ${progress.defects}   ·   Needs service ${progress.needsService}   ·   N/A ${progress.na}   ·   Pending ${progress.pending}"
        body(s, counts, regular, MUTED)
        s.y += 6f

        val summaryText = project.reportSummary?.takeIf { it.isNotBlank() }
            ?: project.generalNotes?.takeIf { it.isNotBlank() }
        if (summaryText != null) {
            body(s, "Overall condition", bold)
            paragraph(s, summaryText)
        }
        s.y += 6f
    }

    private fun drawDefectSummary(s: PageState, project: Project) {
        sectionHeading(s, context.getString(R.string.report_section_defects))
        val defects = project.allItems.filter { it.status.isDefectLike }
        if (defects.isEmpty()) {
            body(s, context.getString(R.string.report_no_defects), regular, MUTED)
            s.y += 8f
            return
        }
        val categoryNames = project.categories.associate { it.id to it.name }
        Severity.entries.sortedByDescending { it.rank }.forEach { severity ->
            val group = defects.filter { (it.severity ?: Severity.MODERATE) == severity }
            if (group.isEmpty()) return@forEach
            s.ensure(22f)
            val sevPaint = paint(bold, 10f, severityColor(severity))
            s.canvas?.drawText("${severityLabel(severity).uppercase()}  (${group.size})", CONTENT_LEFT, s.y, sevPaint)
            s.y += 16f
            group.forEach { item -> drawDefectRow(s, item, categoryNames[item.categoryId] ?: "") }
            s.y += 4f
        }
    }

    private fun drawDefectRow(s: PageState, item: ChecklistItem, category: String) {
        s.ensure(20f)
        val stripe = Paint().apply { color = severityColor(item.severity ?: Severity.MODERATE) }
        val titlePaint = paint(bold, 10f, INK)
        val metaPaint = paint(regular, 8.5f, MUTED)
        val top = s.y - 9f
        s.canvas?.drawRect(CONTENT_LEFT, top, CONTENT_LEFT + 3f, top + 16f, stripe)
        val textX = CONTENT_LEFT + 10f
        val title = ellipsize(item.title, titlePaint, CONTENT_WIDTH - 10f)
        s.canvas?.drawText(title, textX, s.y, titlePaint)
        s.y += 12f
        val meta = listOfNotNull(
            category.ifBlank { null },
            item.condition?.let { conditionLabel(it) },
        ).joinToString("  ·  ")
        if (meta.isNotBlank()) {
            s.canvas?.drawText(meta, textX, s.y, metaPaint)
            s.y += 11f
        }
        item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            paragraphAt(s, notes, textX, CONTENT_WIDTH - 10f, paint(regular, 9f, INK))
        }
        s.y += 4f
    }

    private fun drawChecklist(s: PageState, project: Project) {
        sectionHeading(s, context.getString(R.string.report_section_checklist))
        project.categories.sortedBy { it.sortOrder }.forEach { category ->
            s.ensure(26f)
            val catPaint = paint(bold, 12f, INK)
            s.canvas?.drawText(category.name, CONTENT_LEFT, s.y, catPaint)
            val cp = category.progress
            val frac = paint(regular, 9f, MUTED)
            val fracText = "${cp.completed}/${cp.total}"
            s.canvas?.drawText(fracText, CONTENT_RIGHT - frac.measureText(fracText), s.y, frac)
            s.y += 6f
            val divider = Paint().apply { color = HAIR; strokeWidth = 1f }
            s.canvas?.drawLine(CONTENT_LEFT, s.y, CONTENT_RIGHT, s.y, divider)
            s.y += 12f
            category.items.sortedBy { it.sortOrder }.forEach { item -> drawChecklistItem(s, item) }
            s.y += 6f
        }
    }

    private fun drawChecklistItem(s: PageState, item: ChecklistItem) {
        s.ensure(18f)
        val statusColor = statusColor(item.status)
        val chip = paint(bold, 8f, Color.WHITE)
        val statusText = statusLabel(item.status)
        val chipW = chip.measureText(statusText) + 12f
        val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor }
        val chipTop = s.y - 9f
        s.canvas?.drawRoundRect(RectF(CONTENT_LEFT, chipTop, CONTENT_LEFT + chipW, chipTop + 13f), 3f, 3f, chipBg)
        s.canvas?.drawText(statusText, CONTENT_LEFT + 6f, chipTop + 9.5f, chip)

        val titleX = CONTENT_LEFT + chipW + 8f
        val titlePaint = paint(if (item.status.isDefectLike) bold else regular, 10f, INK)
        val titleMax = CONTENT_RIGHT - titleX
        val title = ellipsize(item.title, titlePaint, titleMax)
        s.canvas?.drawText(title, titleX, s.y, titlePaint)
        s.y += 13f

        val extras = buildList {
            item.condition?.let { add(conditionLabel(it)) }
            if (item.status.isDefectLike) item.severity?.let { add(severityLabel(it)) }
            item.measurementText?.let { add(it) }
            item.expiryDate?.let { add("Expires ${DateFormatters.formatDate(it)}") }
            if (item.photoCount > 0) add("${item.photoCount} photo${if (item.photoCount > 1) "s" else ""}")
        }
        if (extras.isNotEmpty()) {
            s.canvas?.drawText(extras.joinToString("  ·  "), titleX, s.y, paint(regular, 8.5f, MUTED))
            s.y += 11f
        }
        item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            paragraphAt(s, notes, titleX, titleMax, paint(regular, 9f, INK))
        }
        s.y += 5f
    }

    private fun drawCertification(s: PageState, project: Project, branding: ReportBranding) {
        val surveyor = project.surveyorName?.takeIf { it.isNotBlank() }
            ?: branding.surveyorName?.takeIf { it.isNotBlank() }
        val hasContent = !branding.disclaimer.isNullOrBlank() || branding.signaturePath != null ||
            surveyor != null || !branding.licenseNumber.isNullOrBlank()
        if (!hasContent) return

        sectionHeading(s, context.getString(R.string.report_certification))
        branding.disclaimer?.takeIf { it.isNotBlank() }?.let {
            paragraph(s, it)
            s.y += 8f
        }
        branding.signaturePath?.let { path ->
            loadBitmap(path, 600)?.let { bmp ->
                val h = 46f
                val w = (bmp.width * (h / bmp.height)).coerceAtMost(200f)
                s.ensure(h + 6f)
                s.canvas?.drawBitmap(bmp, null, RectF(CONTENT_LEFT, s.y, CONTENT_LEFT + w, s.y + h), null)
                bmp.recycle()
                s.y += h + 4f
            }
        }
        s.ensure(40f)
        val line = Paint().apply { color = INK; strokeWidth = 1f }
        s.canvas?.drawLine(CONTENT_LEFT, s.y, CONTENT_LEFT + 220f, s.y, line)
        s.y += 14f
        surveyor?.let { body(s, "Signed: $it", bold) }
        branding.licenseNumber?.takeIf { it.isNotBlank() }?.let { body(s, "License $it", regular, MUTED) }
        body(s, DateFormatters.formatDate(Instant.now()), regular, MUTED)
        s.y += 6f
    }

    private fun drawPhotoAppendix(s: PageState, project: Project) {
        val photos = project.allItems.flatMap { item -> item.photos.map { item to it } }
        if (photos.isEmpty()) return
        sectionHeading(s, context.getString(R.string.report_section_photos))

        val gutter = 14f
        val cellW = (CONTENT_WIDTH - gutter) / 2f
        val imgH = cellW * 0.72f
        var col = 0
        var rowTop = s.y
        photos.forEach { (item, photo) ->
            if (col == 0) {
                s.ensure(imgH + 34f)
                rowTop = s.y
            }
            val x = CONTENT_LEFT + col * (cellW + gutter)
            val frame = Paint().apply { color = PANEL }
            s.canvas?.drawRect(RectF(x, rowTop, x + cellW, rowTop + imgH), frame)
            loadBitmap(photo.filePath, 900)?.let { bmp ->
                val scale = minOf(cellW / bmp.width, imgH / bmp.height)
                val w = bmp.width * scale
                val h = bmp.height * scale
                val ox = x + (cellW - w) / 2f
                val oy = rowTop + (imgH - h) / 2f
                s.canvas?.drawBitmap(bmp, null, RectF(ox, oy, ox + w, oy + h), null)
                bmp.recycle()
            }
            val capPaint = paint(regular, 8f, MUTED)
            val cap = ellipsize(photo.caption?.ifBlank { null } ?: item.title, capPaint, cellW)
            s.canvas?.drawText(cap, x, rowTop + imgH + 12f, capPaint)

            col++
            if (col == 2) {
                col = 0
                s.y = rowTop + imgH + 26f
            }
        }
        if (col == 1) s.y = rowTop + imgH + 26f
    }

    // ---------- Drawing helpers ----------

    private fun sectionHeading(s: PageState, text: String) {
        s.ensure(30f)
        s.y += 6f
        val p = paint(bold, 13f, INK)
        s.canvas?.drawText(text, CONTENT_LEFT, s.y, p)
        s.y += 6f
        val accent = Paint().apply { color = TEAL; strokeWidth = 2f }
        s.canvas?.drawLine(CONTENT_LEFT, s.y, CONTENT_LEFT + 34f, s.y, accent)
        s.y += 12f
    }

    private fun body(s: PageState, text: String, tf: Typeface, color: Int = INK) {
        s.ensure(14f)
        s.canvas?.drawText(text, CONTENT_LEFT, s.y, paint(tf, 10f, color))
        s.y += 14f
    }

    private fun paragraph(s: PageState, text: String) =
        paragraphAt(s, text, CONTENT_LEFT, CONTENT_WIDTH, paint(regular, 10f, INK))

    private fun paragraphAt(s: PageState, text: String, x: Float, maxWidth: Float, p: Paint) {
        text.split("\n").forEach { rawLine ->
            wrap(rawLine, p, maxWidth).forEach { line ->
                s.ensure(13f)
                s.canvas?.drawText(line, x, s.y, p)
                s.y += 13f
            }
        }
    }

    private fun paint(tf: Typeface, size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = tf; textSize = size; this.color = color
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = ArrayList<String>()
        var line = StringBuilder()
        for (w in words) {
            val test = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(test) <= maxWidth) {
                line = StringBuilder(test)
            } else {
                if (line.isNotEmpty()) lines.add(line.toString())
                if (paint.measureText(w) <= maxWidth) {
                    line = StringBuilder(w)
                } else {
                    var rem = w
                    while (paint.measureText(rem) > maxWidth && rem.length > 1) {
                        var cut = rem.length
                        while (cut > 1 && paint.measureText(rem.substring(0, cut)) > maxWidth) cut--
                        lines.add(rem.substring(0, cut))
                        rem = rem.substring(cut)
                    }
                    line = StringBuilder(rem)
                }
            }
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        return lines.ifEmpty { listOf("") }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }

    private fun loadBitmap(path: String, maxDim: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        }.getOrNull()
    }

    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "—"

    // ---------- Enum labels (report is English) ----------

    private fun statusLabel(s: ItemStatus) = when (s) {
        ItemStatus.PENDING -> "PENDING"
        ItemStatus.OK -> "OK"
        ItemStatus.DEFECT -> "DEFECT"
        ItemStatus.NOT_APPLICABLE -> "N/A"
        ItemStatus.NEEDS_SERVICE -> "SERVICE"
    }

    private fun statusColor(s: ItemStatus) = when (s) {
        ItemStatus.PENDING -> 0xFF7A8896.toInt()
        ItemStatus.OK -> 0xFF2FBF71.toInt()
        ItemStatus.DEFECT -> 0xFFE5484D.toInt()
        ItemStatus.NOT_APPLICABLE -> 0xFF8A98A5.toInt()
        ItemStatus.NEEDS_SERVICE -> 0xFFE08A1E.toInt()
    }

    private fun conditionLabel(c: ConditionRating) = when (c) {
        ConditionRating.EXCELLENT -> "Excellent"
        ConditionRating.GOOD -> "Good"
        ConditionRating.FAIR -> "Fair"
        ConditionRating.POOR -> "Poor"
        ConditionRating.FAILED -> "Failed"
    }

    private fun severityLabel(s: Severity) = when (s) {
        Severity.MINOR -> "Minor"
        Severity.MODERATE -> "Moderate"
        Severity.MAJOR -> "Major"
        Severity.SAFETY -> "Safety"
    }

    private fun severityColor(s: Severity) = when (s) {
        Severity.MINOR -> 0xFFCB9A1E.toInt()
        Severity.MODERATE -> 0xFFD2772B.toInt()
        Severity.MAJOR -> 0xFFE5484D.toInt()
        Severity.SAFETY -> 0xFFD61F69.toInt()
    }

    private fun surveyTypeLabel(t: SurveyType) = when (t) {
        SurveyType.PRE_PURCHASE -> "Pre-Purchase"
        SurveyType.INSURANCE -> "Insurance"
        SurveyType.DAMAGE -> "Damage"
        SurveyType.APPRAISAL -> "Appraisal"
        SurveyType.COMPLIANCE -> "Compliance"
    }

    private fun boatTypeLabel(t: BoatType) = when (t) {
        BoatType.SAIL_MONOHULL -> "Sail — Monohull"
        BoatType.SAIL_CATAMARAN -> "Sail — Catamaran"
        BoatType.POWER_INBOARD -> "Power — Inboard"
        BoatType.POWER_OUTBOARD -> "Power — Outboard"
        BoatType.POWER_STERNDRIVE -> "Power — Sterndrive"
        BoatType.TRAWLER -> "Trawler"
        BoatType.PWC -> "Personal Watercraft"
        BoatType.DINGHY -> "Dinghy / Tender"
        BoatType.OTHER -> "Other"
    }
}
