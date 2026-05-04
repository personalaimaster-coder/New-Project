package com.example.petmeds.data.repo

import android.content.Context
import com.example.petmeds.domain.model.MedicineReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MedicineReferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val medicines: List<MedicineReference> by lazy { loadDatabase() }

    fun getAll(): List<MedicineReference> = medicines

    /**
     * Returns medicines whose brand name, generic name, or search terms match the
     * query via case-insensitive prefix or contains match. Used for autocomplete.
     */
    fun search(query: String, limit: Int = 10): List<MedicineReference> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()

        return medicines
            .map { med -> med to scoreForAutocomplete(med, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Best fuzzy match against OCR-extracted text. Returns the highest-scoring
     * medicine if the score exceeds the confidence threshold.
     */
    fun fuzzyMatch(text: String, threshold: Double = 0.5): FuzzyMatchResult? {
        if (text.isBlank()) return null
        val query = text.trim().lowercase()

        var bestMed: MedicineReference? = null
        var bestScore = 0.0

        for (med in medicines) {
            val score = fuzzyScore(med, query)
            if (score > bestScore) {
                bestScore = score
                bestMed = med
            }
        }

        return if (bestMed != null && bestScore >= threshold) {
            FuzzyMatchResult(bestMed, bestScore)
        } else null
    }

    /**
     * Exact lookup by brand name (case-insensitive).
     */
    fun findByBrandName(name: String): MedicineReference? {
        val lower = name.trim().lowercase()
        return medicines.firstOrNull { it.brandName.lowercase() == lower }
    }

    /**
     * Lookup by any name match (brand or generic, case-insensitive).
     */
    fun findByName(name: String): MedicineReference? {
        val lower = name.trim().lowercase()
        return medicines.firstOrNull { med ->
            med.brandName.lowercase() == lower ||
                med.genericName.lowercase() == lower ||
                med.searchTerms.any { it == lower }
        }
    }

    // ── Scoring ─────────────────────────────────────────────────────────────────

    private fun scoreForAutocomplete(med: MedicineReference, query: String): Int {
        val brand = med.brandName.lowercase()
        val generic = med.genericName.lowercase()

        if (brand == query || generic == query) return 100
        if (brand.startsWith(query)) return 90
        if (generic.startsWith(query)) return 85
        if (brand.contains(query)) return 70
        if (generic.contains(query)) return 65
        if (med.searchTerms.any { it.startsWith(query) }) return 60
        if (med.searchTerms.any { it.contains(query) }) return 50

        // Token-level prefix match
        val queryTokens = query.split(Regex("\\s+"))
        val allTerms = (med.searchTerms + brand + generic).joinToString(" ")
        val matchedTokens = queryTokens.count { token -> allTerms.contains(token) }
        if (matchedTokens == queryTokens.size) return 40

        return 0
    }

    private fun fuzzyScore(med: MedicineReference, query: String): Double {
        val brand = med.brandName.lowercase()
        val generic = med.genericName.lowercase()
        val terms = med.searchTerms

        // Exact match
        if (brand == query || generic == query) return 1.0
        if (terms.any { it == query }) return 1.0

        // Prefix match
        if (brand.startsWith(query) || query.startsWith(brand)) return 0.9
        if (generic.startsWith(query) || query.startsWith(generic)) return 0.9
        if (terms.any { it.startsWith(query) || query.startsWith(it) }) return 0.85

        // Levenshtein distance on tokens
        val queryTokens = query.split(Regex("\\s+"))
        val medTokens = (listOf(brand, generic) + terms)
            .flatMap { it.split(Regex("\\s+")) }
            .distinct()

        var maxTokenScore = 0.0
        for (qt in queryTokens) {
            for (mt in medTokens) {
                val dist = levenshteinDistance(qt, mt)
                val maxLen = maxOf(qt.length, mt.length)
                if (maxLen == 0) continue
                val similarity = 1.0 - (dist.toDouble() / maxLen)
                if (similarity > maxTokenScore) maxTokenScore = similarity
            }
        }

        if (maxTokenScore >= 0.8) return 0.7 + (maxTokenScore - 0.8)

        // Token overlap ratio
        val matchedCount = queryTokens.count { qt ->
            medTokens.any { mt -> mt.contains(qt) || qt.contains(mt) }
        }
        val overlapRatio = matchedCount.toDouble() / queryTokens.size
        if (overlapRatio >= 0.5) return 0.5 + (overlapRatio * 0.2)

        return 0.0
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost,
                )
            }
        }
        return dp[m][n]
    }

    private fun loadDatabase(): List<MedicineReference> {
        return try {
            val raw = context.assets.open("medicine_database.json")
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<List<MedicineReference>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class FuzzyMatchResult(
    val medicine: MedicineReference,
    val score: Double,
)
