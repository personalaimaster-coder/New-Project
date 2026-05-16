package com.example.petmeds.ui.meds.report

import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.NoteCategory
import com.example.petmeds.domain.model.Species
import com.example.petmeds.domain.report.AdherenceStats
import com.example.petmeds.domain.report.CourseReport
import com.example.petmeds.domain.report.MedicationReportRow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

/** Encodes a file path as a `data:image/jpeg;base64,...` URI for the report. */
typealias DataUriEncoder = (String) -> String?

/**
 * Renders a [CourseReport] into a self-contained HTML string. CSS is inlined,
 * images are embedded as data URIs (via [encodeImage]) so the result has zero
 * external dependencies — the WebView print pipeline can rasterize it directly.
 *
 * Pure, deterministic (apart from the locale-independent date formatting),
 * trivially unit-testable.
 */
fun renderCourseReportHtml(
    report: CourseReport,
    encodeImage: DataUriEncoder,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val sb = StringBuilder(8192)
    sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
    // Lock layout to A4 width in CSS px so margins/columns render identically
    // regardless of the device pixel density of the rendering WebView.
    sb.append("<meta name=\"viewport\" content=\"width=595\">")
    sb.append("<title>")
    sb.appendEscaped("Medication course report — ${report.course.name}")
    sb.append("</title>")
    sb.append("<style>").append(STYLES).append("</style>")
    sb.append("</head><body>")

    appendHeader(sb, report, zone)
    appendPatientAndCourseSummary(sb, report, zone)
    appendMedicationsSection(sb, report.medications)
    appendNotesSection(sb, report.notes, encodeImage, zone)
    appendFooter(sb, report.generatedAt, zone)

    sb.append("</body></html>")
    return sb.toString()
}

// ── Sections ─────────────────────────────────────────────────────────────────

private fun appendHeader(sb: StringBuilder, report: CourseReport, zone: TimeZone) {
    sb.append("<header class=\"report-header\">")
    sb.append("<div class=\"brand\">PawPill — Medication course report</div>")
    sb.append("<div class=\"course-title\">")
    sb.appendEscaped(report.course.name)
    sb.append("</div>")
    sb.append("<div class=\"sub\">")
    sb.appendEscaped("For ${report.pet.name} — generated ${formatDateTime(report.generatedAt, zone)}")
    sb.append("</div></header>")
}

private fun appendPatientAndCourseSummary(
    sb: StringBuilder,
    report: CourseReport,
    zone: TimeZone,
) {
    val pet = report.pet
    val course = report.course
    sb.append("<section class=\"two-col\">")
    sb.append("<div class=\"card\">")
    sb.append("<h2>Patient</h2><dl>")
    appendDl(sb, "Name", pet.name)
    appendDl(sb, "Species", speciesLabel(pet.species))
    pet.breed?.takeIf { it.isNotBlank() }?.let { appendDl(sb, "Breed", it) }
    pet.weightKg?.let { appendDl(sb, "Weight", "%.1f kg".format(it)) }
    pet.birthDate?.let {
        val ageYears = ageInYears(it, report.generatedAt, zone)
        appendDl(sb, "Date of birth", "${formatDate(it)} ($ageYears yr)")
    }
    sb.append("</dl></div>")

    sb.append("<div class=\"card\">")
    sb.append("<h2>Course</h2><dl>")
    appendDl(sb, "Course", course.name)
    appendDl(sb, "Status", course.status.name.lowercase().replaceFirstChar { it.titlecase() })
    appendDl(sb, "Start date", formatDate(course.startDate))
    if (course.endDate != null) {
        appendDl(sb, "End date", formatDate(course.endDate))
        val duration = course.startDate.daysUntil(course.endDate) + 1
        appendDl(sb, "Duration", "$duration days")
    } else {
        appendDl(sb, "End date", "—")
    }
    course.notes?.takeIf { it.isNotBlank() }?.let { appendDl(sb, "Vet notes", it) }
    sb.append("</dl></div>")
    sb.append("</section>")
}

private fun appendMedicationsSection(sb: StringBuilder, rows: List<MedicationReportRow>) {
    sb.append("<section><h2>Medications &amp; adherence</h2>")
    if (rows.isEmpty()) {
        sb.append("<p class=\"empty\">No medications logged for this course.</p>")
        sb.append("</section>")
        return
    }
    sb.append("<table class=\"meds\">")
    sb.append("<colgroup>")
    sb.append("<col class=\"col-med\"><col class=\"col-dose\"><col class=\"col-sched\">")
    sb.append("<col class=\"col-period\"><col class=\"col-adh\">")
    sb.append("</colgroup>")
    sb.append("<thead><tr>")
    sb.append("<th>Medication</th>")
    sb.append("<th>Dose</th>")
    sb.append("<th>Schedule</th>")
    sb.append("<th>Period</th>")
    sb.append("<th>Adherence</th>")
    sb.append("</tr></thead><tbody>")
    rows.forEach { row ->
        val m = row.medication
        sb.append("<tr>")
        sb.append("<td><strong>")
        sb.appendEscaped(m.name)
        sb.append("</strong><div class=\"muted small\">")
        sb.appendEscaped(formLabel(m.form.name))
        m.notes?.takeIf { it.isNotBlank() }?.let {
            sb.append(" — ")
            sb.appendEscaped(it)
        }
        sb.append("</div></td>")

        sb.append("<td>")
        sb.appendEscaped(formatDosage(m.dosageAmount, m.dosageUnit))
        sb.append("</td>")

        sb.append("<td>")
        sb.appendEscaped(row.scheduleSummary)
        sb.append("</td>")

        sb.append("<td>")
        sb.append(formatDate(m.startDate))
        sb.append(" — ")
        sb.append(m.endDate?.let { formatDate(it) } ?: "ongoing")
        sb.append("</td>")

        sb.append("<td>")
        appendAdherenceCell(sb, row.adherence)
        sb.append("</td>")
        sb.append("</tr>")
    }
    sb.append("</tbody></table>")
    sb.append("</section>")
}

private fun appendAdherenceCell(sb: StringBuilder, stats: AdherenceStats) {
    if (stats.isEmpty) {
        sb.append("<span class=\"muted small\">No doses logged</span>")
        return
    }
    val pct = stats.taken100
    sb.append("<div class=\"bar\"><div class=\"fill\" style=\"width:")
        .append(pct.toString())
        .append("%\"></div></div>")
    sb.append("<div class=\"small\">")
        .append("<strong>").append(pct.toString()).append("%</strong>")
        .append(" taken · ")
        .append(stats.taken.toString()).append(" taken / ")
        .append(stats.missed.toString()).append(" missed / ")
        .append(stats.skipped.toString()).append(" skipped")
    if (stats.pending > 0) sb.append(" · ").append(stats.pending.toString()).append(" pending")
    sb.append("</div>")
}

private fun appendNotesSection(
    sb: StringBuilder,
    notes: List<CourseNote>,
    encodeImage: DataUriEncoder,
    zone: TimeZone,
) {
    sb.append("<section class=\"notes\"><h2>Progress notes</h2>")
    if (notes.isEmpty()) {
        sb.append("<p class=\"empty\">No progress notes recorded.</p>")
        sb.append("</section>")
        return
    }
    notes.forEach { note ->
        sb.append("<article class=\"note\">")
        sb.append("<div class=\"note-head\">")
        sb.append("<span class=\"chip chip-")
            .append(noteCategoryClass(note.category))
            .append("\">")
            .appendEscaped(noteCategoryLabel(note.category))
            .append("</span>")
        sb.append("<span class=\"timestamp\">")
            .appendEscaped(formatDateTime(note.occurredAt, zone))
            .append("</span>")
        sb.append("</div>")
        sb.append("<div class=\"note-body\">").appendEscaped(note.body).append("</div>")
        note.photoPath?.let { path ->
            encodeImage(path)?.let { dataUri ->
                sb.append("<img class=\"note-photo\" src=\"").append(dataUri).append("\"/>")
            }
        }
        sb.append("</article>")
    }
    sb.append("</section>")
}

private fun appendFooter(sb: StringBuilder, generatedAt: Instant, zone: TimeZone) {
    sb.append("<footer>")
    sb.append("Generated by PawPill on ")
    sb.appendEscaped(formatDateTime(generatedAt, zone))
    sb.append(". This report is a personal medication log and is not a substitute for veterinary advice.")
    sb.append("</footer>")
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun appendDl(sb: StringBuilder, label: String, value: String) {
    sb.append("<dt>").appendEscaped(label).append("</dt>")
    sb.append("<dd>").appendEscaped(value).append("</dd>")
}

private fun StringBuilder.appendEscaped(value: String): StringBuilder {
    for (c in value) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            '\n' -> append("<br>")
            else -> append(c)
        }
    }
    return this
}

private fun formatDate(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)
    return "${date.dayOfMonth} $month ${date.year}"
}

private fun formatDateTime(instant: Instant, zone: TimeZone): String {
    val ldt: LocalDateTime = instant.toLocalDateTime(zone)
    return "%s %s %d · %02d:%02d".format(
        ldt.dayOfMonth.toString().padStart(2, '0'),
        ldt.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3),
        ldt.year,
        ldt.hour,
        ldt.minute,
    )
}

private fun ageInYears(birth: LocalDate, now: Instant, zone: TimeZone): Int {
    val today = now.toLocalDateTime(zone).date
    val days = birth.daysUntil(today)
    return (days / 365).coerceAtLeast(0)
}

private fun noteCategoryLabel(category: NoteCategory): String = when (category) {
    NoteCategory.OBSERVATION -> "Observation"
    NoteCategory.SYMPTOM -> "Symptom"
    NoteCategory.SIDE_EFFECT -> "Side effect"
    NoteCategory.VET_VISIT -> "Vet visit"
}

private fun noteCategoryClass(category: NoteCategory): String = when (category) {
    NoteCategory.OBSERVATION -> "observation"
    NoteCategory.SYMPTOM -> "symptom"
    NoteCategory.SIDE_EFFECT -> "side-effect"
    NoteCategory.VET_VISIT -> "vet-visit"
}

private fun speciesLabel(species: Species): String = when (species) {
    Species.DOG -> "Dog"
    Species.CAT -> "Cat"
    Species.RABBIT -> "Rabbit"
    Species.BIRD -> "Bird"
    Species.OTHER -> "Other"
}

private fun formLabel(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

private fun formatDosage(amount: Double, unit: String): String {
    val a = if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else amount.toString()
    return "$a $unit"
}

// ── Inline stylesheet (A4) ───────────────────────────────────────────────────

// Layout assumes the document is rendered into a 595 CSS-px wide viewport
// (locked via <meta name="viewport" content="width=595"> + WebView's
// useWideViewPort=true). All paddings/columns below are tuned for that width.
private const val STYLES = """
@page { size: A4; margin: 0; }
* { box-sizing: border-box; }
html, body { margin: 0; color: #111827; font-family: 'Helvetica', 'Arial', sans-serif; font-size: 11pt; line-height: 1.45; word-wrap: break-word; overflow-wrap: anywhere; }
body { width: 595px; padding: 36px 36px 36px 36px; background: #ffffff; }
.report-header { border-bottom: 2px solid #1B4D3E; padding: 0 0 10px; margin: 0 0 16px; }
.report-header .brand { color: #1B4D3E; font-weight: 700; letter-spacing: .2px; font-size: 9.5pt; text-transform: uppercase; }
.report-header .course-title { font-size: 20pt; font-weight: 700; margin-top: 2px; line-height: 1.15; }
.report-header .sub { color: #4b5563; font-size: 9.5pt; margin-top: 4px; }
section { margin: 0 0 16px; }
section > h2 { font-size: 12pt; color: #1B4D3E; margin: 0 0 8px; padding-bottom: 4px; border-bottom: 1px solid #e5e7eb; }
.two-col { display: flex; gap: 10px; }
.two-col .card { flex: 1 1 0; min-width: 0; border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px 12px; background: #fafafa; }
.card h2 { margin-top: 0; margin-bottom: 8px; font-size: 12pt; }
dl { margin: 0; display: grid; grid-template-columns: 80px 1fr; row-gap: 4px; column-gap: 8px; }
dt { color: #6b7280; font-size: 9pt; min-width: 0; }
dd { margin: 0; font-size: 10pt; min-width: 0; word-break: break-word; overflow-wrap: anywhere; }
table.meds { width: 100%; border-collapse: collapse; table-layout: fixed; }
table.meds th, table.meds td { text-align: left; vertical-align: top; padding: 6px 8px; border-bottom: 1px solid #e5e7eb; word-break: break-word; overflow-wrap: anywhere; }
table.meds th { background: #f3f4f6; font-weight: 600; font-size: 9pt; color: #374151; }
table.meds col.col-med { width: 26%; }
table.meds col.col-dose { width: 14%; }
table.meds col.col-sched { width: 18%; }
table.meds col.col-period { width: 18%; }
table.meds col.col-adh { width: 24%; }
.muted { color: #6b7280; }
.small { font-size: 9pt; }
.bar { width: 100%; height: 6px; background: #e5e7eb; border-radius: 999px; margin-bottom: 4px; overflow: hidden; }
.bar .fill { height: 100%; background: #4ADE80; }
.notes .note { border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px 12px; margin-bottom: 8px; page-break-inside: avoid; background: #fff; }
.note .note-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; gap: 8px; }
.note .timestamp { color: #6b7280; font-size: 9pt; }
.note .note-body { white-space: pre-wrap; word-break: break-word; }
.note .note-photo { display: block; max-width: 100%; max-height: 280px; margin-top: 8px; border-radius: 4px; border: 1px solid #e5e7eb; }
.chip { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 8.5pt; font-weight: 600; letter-spacing: .2px; }
.chip-observation { background: #E0E7FF; color: #1A1250; }
.chip-symptom { background: #FEE2E2; color: #7F1D1D; }
.chip-side-effect { background: #FEF3C7; color: #7c2d12; }
.chip-vet-visit { background: #DCFCE7; color: #052E16; }
.empty { color: #6b7280; font-style: italic; }
footer { margin-top: 18px; padding-top: 8px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 8.5pt; }
"""
