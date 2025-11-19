package br.com.atas.mobile.feature.meetings

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.model.MeetingDetails
import br.com.atas.mobile.core.data.model.MeetingSpeaker
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class MeetingPdfGenerator {

    suspend fun generate(context: Context, meeting: Meeting): Uri = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val state = PdfState(document)

        state.drawHeader(meeting)
        state.drawGeneralSection(meeting)
        state.drawSection("Abertura") { drawAbertura(meeting.details) }
        state.drawSection("Chamados e apoios") { drawChamados(meeting.details) }
        state.drawSection("Sacramento") { drawSacramento(meeting.details) }
        state.drawSection("Discursos") { drawDiscursos(meeting.details) }
        state.drawSection("Encerramento") { drawEncerramento(meeting.details) }
        state.drawSection("Observacoes") { drawParagraph(meeting.details.observacoes) }

        state.finish()

        val file = createOutputFile(context, meeting)
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private inner class PdfState(private val document: PdfDocument) {
        private val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
        }
        private val sectionPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        private val labelPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        private val valuePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
        }
        private val dividerPaint = Paint().apply {
            strokeWidth = 1f
        }

        private var pageIndex = 1
        private var page =
            document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create())
        private var canvas = page.canvas
        private var cursorY = TOP_MARGIN

        fun drawHeader(meeting: Meeting) {
            val title = meeting.title.ifBlank { "Ata da Reuniao" }
            drawCenteredLine(title, titlePaint, spacingAfter = 26f)
            formatDate(meeting.date)
                .takeIf { it.isNotBlank() }
                ?.let { drawCenteredLine(it, valuePaint, spacingAfter = 18f) }
            canvas.drawLine(MARGIN_START, cursorY, PAGE_WIDTH - MARGIN_START, cursorY, dividerPaint)
            cursorY += 20f
        }

        fun drawGeneralSection(meeting: Meeting) {
            drawSection("Informacoes gerais") {
                drawInfoGrid(
                    listOf(
                        "Data" to formatDate(meeting.date),
                        "Orgao" to meeting.details.orgao,
                        "Ala" to meeting.details.ala,
                        "Tipo" to meeting.details.tipo,
                        "Frequencia" to meeting.details.frequencia,
                        "Preside" to meeting.details.preside,
                        "Dirige" to meeting.details.dirige,
                        "Organista" to meeting.details.organista,
                        "Regente" to meeting.details.regente
                    )
                )
            }
        }

        fun drawSection(title: String, content: SectionScope.() -> Unit) {
            ensureSpace(80f)
            canvas.drawText(title.uppercase(Locale.getDefault()), MARGIN_START, cursorY, sectionPaint)
            cursorY += 8f
            canvas.drawLine(MARGIN_START, cursorY, PAGE_WIDTH - MARGIN_START, cursorY, dividerPaint)
            cursorY += 12f
            SectionScope().content()
            cursorY += 12f
        }

        inner class SectionScope {

            fun drawPair(label: String, value: String?) {
                val safeValue = value?.trim().takeUnless { it.isNullOrEmpty() } ?: return
                ensureSpace(32f)
                canvas.drawText(label, MARGIN_START, cursorY, labelPaint)
                cursorY += 16f
                wrapText(valuePaint, safeValue).forEach { line ->
                    ensureSpace(0f)
                    canvas.drawText(line, MARGIN_START, cursorY, valuePaint)
                    cursorY += 16f
                }
                cursorY += 4f
            }

            fun drawParagraph(text: String?) {
                val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return
                ensureSpace(32f)
                wrapText(valuePaint, value).forEach { line ->
                    ensureSpace(0f)
                    canvas.drawText(line, MARGIN_START, cursorY, valuePaint)
                    cursorY += 16f
                }
                cursorY += 4f
            }

            fun drawAbertura(details: MeetingDetails) {
                drawPair("Anuncios", details.anuncios)
                drawPair("Hino de abertura", details.hinos.abertura)
                drawPair("Oracao de abertura", details.oracoes.abertura)
            }

            fun drawChamados(details: MeetingDetails) {
                drawPair("Desobrigacoes", details.desobrigacoes)
                drawPair("Chamados, apoios e boas-vindas", details.chamados)
            }

            fun drawSacramento(details: MeetingDetails) {
                drawPair("Hino do sacramento", details.hinos.sacramento)
                drawPair("Oficiantes do sacramento", details.oficiantesSacramento)
            }

            fun drawDiscursos(details: MeetingDetails) {
                val speakers = details.oradores
                if (speakers.isEmpty()) {
                    drawParagraph("Reuniao de Testemunhos")
                    drawPair("Hino intermediario", details.hinos.intermediario)
                    return
                }

                val hymnPlacement = calculateHymnPlacement(speakers)
                speakers.forEachIndexed { index, speaker ->
                    drawPair("Orador ${index + 1}", formatSpeakerLine(speaker))
                    if (hymnPlacement?.insertAfterIndex == index) {
                        drawPair("Hino intermediario", details.hinos.intermediario)
                    }
                }
            }

            fun drawEncerramento(details: MeetingDetails) {
                drawPair("Hino de encerramento", details.hinos.encerramento)
                drawPair("Oracao de encerramento", details.oracoes.encerramento)
            }

            fun drawInfoGrid(items: List<Pair<String, String?>>) {
                val displayItems = items.mapNotNull { (label, value) ->
                    value?.takeIf { it.isNotBlank() }?.let { label to it.trim() }
                }
                if (displayItems.isEmpty()) return

                val columns = INFO_GRID_COLUMNS
                val maxRows = GRID_MAX_ROWS
                val limitedItems = displayItems.take(columns * maxRows)
                val rows = limitedItems.chunked(columns)
                val columnSpacing = 24f
                val availableWidth = PAGE_WIDTH - MARGIN_START * 2
                val columnWidth = (availableWidth - columnSpacing * (columns - 1)) / columns

                rows.forEach { row ->
                    val cells = row.map { (label, value) ->
                        val labelLines = wrapText(labelPaint, label, columnWidth)
                        val valueLines = wrapText(valuePaint, value, columnWidth)
                        val contentHeight =
                            (labelLines.size * LABEL_LINE_HEIGHT) +
                                (valueLines.size * VALUE_LINE_HEIGHT) +
                                CELL_PADDING
                        PdfGridCell(labelLines, valueLines, max(contentHeight, MIN_GRID_ROW_HEIGHT))
                    }
                    val rowHeight = cells.maxOfOrNull { it.height } ?: MIN_GRID_ROW_HEIGHT
                    ensureSpace(rowHeight + GRID_ROW_SPACING)

                    cells.forEachIndexed { columnIndex, cell ->
                        val startX = MARGIN_START + columnIndex * (columnWidth + columnSpacing)
                        var textBaseline = cursorY
                        cell.labelLines.forEach { line ->
                            canvas.drawText(line, startX, textBaseline, labelPaint)
                            textBaseline += LABEL_LINE_HEIGHT
                        }
                        textBaseline += 2f
                        cell.valueLines.forEach { line ->
                            canvas.drawText(line, startX, textBaseline, valuePaint)
                            textBaseline += VALUE_LINE_HEIGHT
                        }
                    }
                    cursorY += rowHeight + GRID_ROW_SPACING
                }
            }
        }

        private fun formatSpeakerLine(speaker: MeetingSpeaker): String {
            val name = speaker.nome.ifBlank { "Nome nao informado" }
            val topic = speaker.assunto.takeUnless { it.isBlank() }?.let { " - $it" } ?: ""
            return "$name$topic"
        }

        private fun drawCenteredLine(text: String, paint: Paint, spacingAfter: Float) {
            ensureSpace(paint.textSize + spacingAfter)
            val x = (PAGE_WIDTH - paint.measureText(text)) / 2f
            canvas.drawText(text, x, cursorY, paint)
            cursorY += spacingAfter
        }

        private fun ensureSpace(extra: Float) {
            if (cursorY + extra >= PAGE_HEIGHT - TOP_MARGIN) {
                finishPage()
                startNewPage()
            }
        }

        private fun finishPage() {
            document.finishPage(page)
        }

        private fun startNewPage() {
            pageIndex += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create())
            canvas = page.canvas
            cursorY = TOP_MARGIN
        }

        fun finish() {
            finishPage()
        }
    }

    private fun wrapText(paint: Paint, text: String, maxWidth: Float = PAGE_WIDTH - MARGIN_START * 2): List<String> {
        if (text.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(candidate) > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(candidate)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun createOutputFile(context: Context, meeting: Meeting): File {
        val safeTitle = meeting.title.ifBlank { "Ata" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "${safeTitle}-${meeting.date}.pdf"
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        return File(dir, fileName)
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val TOP_MARGIN = 48f
        private const val MARGIN_START = 40f
        private const val LABEL_LINE_HEIGHT = 14f
        private const val VALUE_LINE_HEIGHT = 16f
        private const val GRID_ROW_SPACING = 8f
        private const val CELL_PADDING = 6f
        private const val MIN_GRID_ROW_HEIGHT = 28f
        private const val INFO_GRID_COLUMNS = 3
        private const val GRID_MAX_ROWS = 4

        fun formatDate(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return runCatching {
                LocalDate.parse(raw)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
            }.getOrDefault(raw)
        }
    }
}

private data class PdfGridCell(
    val labelLines: List<String>,
    val valueLines: List<String>,
    val height: Float
)
