package com.example.petmeds.data.ocr

import android.content.Context
import com.google.mlkit.vision.text.Text
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts raw ML Kit [Text] output into structured [OcrMedResult] objects.
 *
 * ## Strategy: dosage-anchored parsing
 *
 * A line is only treated as a medication entry when it contains an unambiguous
 * dosage pattern (a number followed by a medical unit, e.g. "500mg", "10ml",
 * "2 tablets"). This single constraint eliminates virtually all false positives
 * such as clinic names, doctor names, and instruction lines.
 *
 * For each dosage-containing line:
 *   - The text **before** the dosage on the same line is the name candidate.
 *   - If the name is blank (dosage starts the line), the previous line is used.
 *   - The text **after** the dosage (plus the next line) provides frequency/form context.
 *
 * The extracted name is then validated against the bundled India pet drug
 * dictionary ([drug_names.txt]) to set [OcrMedResult.confidenceHigh].
 *
 * The dictionary is a *confidence signal only* — unrecognised names are still
 * returned so the user can verify them in the editable review screen.
 */
@Singleton
class PrescriptionTextParser @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Set of lowercase drug names loaded once from assets/drug_names.txt. */
    private val drugDictionary: Set<String> by lazy { loadDictionary() }

    fun parse(visionText: Text): List<OcrMedResult> {
        val lines = visionText.textBlocks
            .flatMap { block -> block.lines }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()

        val results = mutableListOf<OcrMedResult>()

        lines.forEachIndexed { idx, line ->
            val dosageMatch = DOSAGE_PATTERN.find(line) ?: return@forEachIndexed

            // ── Extract name ─────────────────────────────────────────────────
            val beforeDosage = line.substring(0, dosageMatch.range.first).trim()
            val nameCandidate = when {
                beforeDosage.isNotBlank() -> beforeDosage
                // Dosage is at the start of this line — look at the previous line
                idx > 0 -> lines[idx - 1].trim()
                else -> ""
            }

            val cleanName = cleanName(nameCandidate)
            if (cleanName.isBlank()) return@forEachIndexed

            // ── Build context for frequency / form / notes ───────────────────
            val afterDosage = line.substring(dosageMatch.range.last + 1).trim()
            val nextLine = if (idx + 1 < lines.size) lines[idx + 1] else ""
            val context = "$line $afterDosage $nextLine"

            // ── Extract fields ────────────────────────────────────────────────
            val amount = dosageMatch.groupValues[1]
            val unit = normaliseUnit(dosageMatch.groupValues[2])
            val freqInfo = extractFrequency(context)
            val form = inferForm(line + " " + afterDosage)
            val notes = extractNotes(context)

            // ── Dictionary lookup ────────────────────────────────────────────
            val nameLower = cleanName.lowercase()
            val confident = drugDictionary.any { entry ->
                nameLower == entry || nameLower.startsWith(entry) || entry.startsWith(nameLower)
            }

            results += OcrMedResult(
                name = cleanName,
                dosageAmount = amount,
                dosageUnit = unit,
                form = form,
                frequencyType = freqInfo.type,
                timesPerDay = freqInfo.timesPerDay,
                intervalHours = freqInfo.intervalHours ?: 8,
                notes = notes,
                rawText = line,
                confidenceHigh = confident,
            )
        }

        return results.distinctBy { it.name.lowercase() }
    }

    // ── Name cleaning ──────────────────────────────────────────────────────────

    /**
     * Strips common prescription prefixes (Tab., Cap., Inj., Syr., etc.) and
     * noise suffixes, then returns the clean drug name portion.
     */
    private fun cleanName(raw: String): String {
        var s = raw.trim()

        // Strip leading form prefixes: "Tab.", "Cap.", "Inj.", "Syr.", "Oint.", etc.
        s = FORM_PREFIX_PATTERN.replace(s, "").trim()

        // Strip trailing punctuation and noise
        s = s.trimEnd('.', ',', ':', '-', '/', '(', ')')

        // Take only leading alphabetic tokens (stop at digits or purely symbolic chars)
        val nameTokens = mutableListOf<String>()
        for (token in s.split(Regex("\\s+"))) {
            if (token.isBlank()) continue
            // Allow tokens that start with a letter (handles "co-Amoxiclav", "N-Acetyl")
            if (token[0].isLetter() || (token.length > 1 && token[0] == '-' && token[1].isLetter())) {
                nameTokens += token
            } else {
                break // stop at first non-alpha token (e.g. a dosage number)
            }
        }

        val candidate = nameTokens.joinToString(" ").trim()

        // Reject if too short, all digits, or matches a known noise phrase
        if (candidate.length < 3) return ""
        if (candidate.all { it.isDigit() }) return ""
        if (NOISE_PATTERN.containsMatchIn(candidate)) return ""
        if (SKIP_WORDS.contains(candidate.lowercase())) return ""

        return candidate
    }

    // ── Frequency extraction ───────────────────────────────────────────────────

    private fun extractFrequency(text: String): FreqInfo {
        val upper = text.uppercase()

        val intervalMatch = INTERVAL_PATTERN.find(upper)
        if (intervalMatch != null) {
            val hours = intervalMatch.groupValues[1].toIntOrNull()
                ?: INTERVAL_ABBREV[intervalMatch.groupValues[2]] ?: 8
            return FreqInfo("interval", 1, hours)
        }

        if (PRN_TERMS.any { upper.contains(it) }) {
            return FreqInfo("prn", 0, null)
        }

        for ((terms, times) in NAMED_FREQUENCIES) {
            if (terms.any { upper.contains(it) }) {
                return FreqInfo("daily_times", times, null)
            }
        }

        return FreqInfo("daily_times", 1, null)
    }

    // ── Form inference ─────────────────────────────────────────────────────────

    private fun inferForm(text: String): String {
        val lower = text.lowercase()
        return when {
            PILL_TERMS.any { lower.contains(it) }     -> "pill"
            LIQUID_TERMS.any { lower.contains(it) }   -> "liquid"
            EYE_DROP_TERMS.any { lower.contains(it) } -> "eye_drop"
            EAR_DROP_TERMS.any { lower.contains(it) } -> "ear_drop"
            TOPICAL_TERMS.any { lower.contains(it) }  -> "topical"
            else -> ""
        }
    }

    // ── Notes extraction ───────────────────────────────────────────────────────

    private fun extractNotes(text: String): String {
        val lower = text.lowercase()
        val notes = mutableListOf<String>()
        if (FOOD_TERMS.any { lower.contains(it) }) notes += "Take with food"
        if (lower.contains("empty stomach") || lower.contains(" ac ")) notes += "Take on empty stomach"
        if (lower.contains("plenty") && lower.contains("water")) notes += "Take with plenty of water"
        return notes.joinToString("; ")
    }

    // ── Dictionary loader ──────────────────────────────────────────────────────

    private fun loadDictionary(): Set<String> {
        return try {
            context.assets.open("drug_names.txt").bufferedReader().useLines { lines ->
                lines
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith('#') }
                    .map { it.lowercase() }
                    .toHashSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ── Unit normalisation ─────────────────────────────────────────────────────

    private fun normaliseUnit(raw: String): String {
        val lower = raw.trim().lowercase()
        return UNIT_ALIASES[lower] ?: lower
    }

    // ── Internal data ──────────────────────────────────────────────────────────

    private data class FreqInfo(
        val type: String,
        val timesPerDay: Int,
        val intervalHours: Int?,
    )

    companion object {

        /**
         * Primary anchor: matches a quantity + unit.
         * Group 1 = numeric amount, Group 2 = unit string.
         */
        val DOSAGE_PATTERN: Regex = Regex(
            """(\d+\.?\d*)\s*(mg|mcg|µg|μg|g\b|ml|mL|IU|iu|unit|units|tablet|tablets|tab\b|tabs|capsule|capsules|cap\b|caps|drop|drops|gtt|puff|puffs|sachet|sachets|patch|patches|application|ml\/kg|mg\/kg)""",
            RegexOption.IGNORE_CASE,
        )

        private val FORM_PREFIX_PATTERN = Regex(
            """^(tab\.?|cap\.?|inj\.?|syr\.?|oint\.?|cr\.?|sol\.?|susp\.?|pwd\.?|liq\.?|gel\.?|ear\.?|eye\.?|top\.?|oral\s+tab\.?)\s+""",
            RegexOption.IGNORE_CASE,
        )

        private val INTERVAL_PATTERN = Regex(
            """(?:every|q|Q)\s*(\d+)\s*h(?:ours?)?|Q(4H|6H|8H|12H)""",
            RegexOption.IGNORE_CASE,
        )

        private val INTERVAL_ABBREV = mapOf("Q4H" to 4, "Q6H" to 6, "Q8H" to 8, "Q12H" to 12)

        private val PRN_TERMS = listOf("PRN", "P.R.N", "AS NEEDED", "AS REQUIRED", "WHEN NEEDED", "SOS")

        private val NAMED_FREQUENCIES = listOf(
            listOf("QID", "Q.I.D", "4 TIMES", "FOUR TIMES", "4X", "FOUR X") to 4,
            listOf("TID", "T.I.D", "3 TIMES", "THREE TIMES", "TDS", "3X") to 3,
            listOf("BID", "B.I.D", "BD", "TWICE DAILY", "TWICE A DAY", "2 TIMES", "2X") to 2,
            listOf("QHS", " HS ", "AT BEDTIME", "NOCTE", "BEDTIME", "HS\n") to 1,
            listOf("QD", " OD ", " OD\n", "ONCE DAILY", "ONCE A DAY", "DAILY", "1X", "SID") to 1,
        )

        private val UNIT_ALIASES = mapOf(
            "tab" to "tablet", "tabs" to "tablet", "tablets" to "tablet",
            "cap" to "capsule", "caps" to "capsule", "capsules" to "capsule",
            "gtt" to "drops", "drop" to "drops",
            "mcg" to "mcg", "µg" to "mcg", "μg" to "mcg",
            "ml" to "ml", "ml/kg" to "ml/kg", "mg/kg" to "mg/kg",
        )

        private val NOISE_PATTERN = Regex(
            """^(date|patient|name|doctor|dr|hospital|clinic|rx|rp|sig|dispense|refill|phone|tel|fax|dob|address|age|weight|wt|ref|diagnosis|diag|impression|advice|follow)""",
            RegexOption.IGNORE_CASE,
        )

        private val SKIP_WORDS = setOf(
            "take", "apply", "use", "instil", "instill", "give", "administer",
            "the", "and", "for", "with", "after", "before", "at", "in", "on", "by",
            "sig", "rx", "rp", "dispense", "quantity", "each", "once", "twice",
            "morning", "evening", "night", "daily", "times",
        )

        private val PILL_TERMS = listOf("tablet", " tab ", "tab.", "capsule", " cap ", "cap.", "pill", "lozenge")
        private val LIQUID_TERMS = listOf("syrup", "suspension", "solution", "elixir", "liquid", " ml ", "/ml")
        private val EYE_DROP_TERMS = listOf("eye drop", "ophthalmic", "eye gtt", "ocular", "eye oint")
        private val EAR_DROP_TERMS = listOf("ear drop", "otic", "ear gtt", "ear oint")
        private val TOPICAL_TERMS = listOf("cream", "ointment", " gel ", "lotion", "topical", "patch", "transdermal", "spray")

        private val FOOD_TERMS = listOf("with food", "after meal", "after food", "with meal", " pc ", "pc\n")
    }
}
