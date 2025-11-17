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
        state.drawSection("Observações") { drawParagraph(meeting.details.observacoes) }

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
            textSize = 20f
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
        private var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create())
        private var canvas = page.canvas
        private var cursorY = TOP_MARGIN

        fun drawHeader(meeting: Meeting) {
            drawLargeTitle(meeting.title.ifBlank { "Ata da Reunião" })
            drawParagraph(formatDate(meeting.date))
            drawParagraph("Gerada em ${formatDate(LocalDate.now().toString())}")
            cursorY += 12f
            canvas.drawLine(MARGIN_START, cursorY, PAGE_WIDTH - MARGIN_START, cursorY, dividerPaint)
            cursorY += 20f
        }

        fun drawGeneralSection(meeting: Meeting) {
            drawSection("Informações gerais") {
                drawPair("Órgão", meeting.details.orgao)
                drawPair("Ala", meeting.details.ala)
                drawPair("Tipo", meeting.details.tipo)
                drawPair("Frequência", meeting.details.frequencia)
                drawPair("Preside", meeting.details.preside)
                drawPair("Dirige", meeting.details.dirige)
                drawPair("Organista", meeting.details.organista)
                drawPair("Regente", meeting.details.regente)
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
                if (value.isNullOrBlank()) return
                ensureSpace(32f)
                canvas.drawText(label, MARGIN_START, cursorY, labelPaint)
                cursorY += 16f
                wrapText(valuePaint, value).forEach { line ->
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

            fun drawSpeakers(details: MeetingDetails) {
                if (details.oradores.isEmpty()) {
                    drawParagraph("Reunião de Testemunhos")
                    return
                }
                details.oradores.forEachIndexed { index, speaker ->
                    drawPair("Orador ${index + 1}", formatSpeakerLine(speaker))
                }
            }

            fun drawAbertura(details: MeetingDetails) {
                drawPair("Hino de abertura", details.hinos.abertura)
                drawPair("Oração de abertura", details.oracoes.abertura)
                drawPair("Anúncios", details.anuncios)
            }

            fun drawChamados(details: MeetingDetails) {
                drawPair("Desobrigações", details.desobrigacoes)
                drawPair("Chamados, apoios e boas-vindas", details.chamados)
            }

            fun drawSacramento(details: MeetingDetails) {
                drawPair("Hino do sacramento", details.hinos.sacramento)
                drawPair("Oficiantes do sacramento", details.oficiantesSacramento)
            }

            fun drawDiscursos(details: MeetingDetails) {
                drawSpeakers(details)
                drawPair("Hino intermediário", details.hinos.intermediario)
            }

            fun drawEncerramento(details: MeetingDetails) {
                drawPair("Hino de encerramento", details.hinos.encerramento)
                drawPair("Oração de encerramento", details.oracoes.encerramento)
            }
        }

        private fun formatSpeakerLine(speaker: MeetingSpeaker): String {
            val nome = speaker.nome.ifBlank { "Nome não informado" }
            val assunto = speaker.assunto.takeUnless { it.isBlank() }?.let { " • $it" } ?: ""
            return "$nome$assunto"
        }

        private fun drawLargeTitle(text: String) {
            canvas.drawText(text, MARGIN_START, cursorY, titlePaint)
            cursorY += 26f
        }

        private fun drawParagraph(text: String) {
            wrapText(valuePaint, text).forEach { line ->
                canvas.drawText(line, MARGIN_START, cursorY, valuePaint)
                cursorY += 16f
                ensureSpace(0f)
            }
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

    private fun wrapText(paint: Paint, text: String): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(candidate) > (PAGE_WIDTH - MARGIN_START * 2)) {
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

        fun formatDate(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return runCatching {
                LocalDate.parse(raw)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
            }.getOrDefault(raw)
        }
    }
}
