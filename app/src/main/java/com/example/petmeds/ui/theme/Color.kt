package com.example.petmeds.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Brand {
    // Core brand
    val Canvas = Color(0xFFFFFFFF)
    val CanvasDark = Color(0xFF0F172A)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF111A2E)
    val DarkBlue = Color(0xFF1A365D)
    val DarkBlueOn = Color(0xFFFFFFFF)
    val GrassGreen = Color(0xFF4ADE80)
    val GrassGreenOn = Color(0xFF052E16)

    // Extended warm palette (Headspace-inspired)
    val Cream = Color(0xFFFFF8EE)          // warm canvas for onboarding / empty states
    val CreamDark = Color(0xFF1C1610)
    val Coral = Color(0xFFFF8A65)          // friendly CTA accent
    val CoralOn = Color(0xFF3B0900)
    val Sage = Color(0xFFA7D7C5)           // soft success / "all done" celebration
    val SageOn = Color(0xFF003829)
    val SageDark = Color(0xFF1B4D3E)
    val Lavender = Color(0xFFE8E5FF)       // muted section backgrounds
    val LavenderOn = Color(0xFF1A1250)
    val Apricot = Color(0xFFFFCC80)        // medication color slot 4
    val SkyBlue = Color(0xFFB3E5FC)        // medication color slot 5
    val Mint = Color(0xFFB2DFDB)           // medication color slot 6

    // Neutrals
    val Slate900 = Color(0xFF0F172A)
    val Slate700 = Color(0xFF334155)
    val Slate500 = Color(0xFF64748B)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate50 = Color(0xFFF8FAFC)

    // Semantic
    val DangerRed = Color(0xFFDC2626)
    val DangerRedLight = Color(0xFFFEE2E2)
    val DangerRedOn = Color(0xFF7F1D1D)
    val WarningAmber = Color(0xFFD97706)
    val WarningAmberLight = Color(0xFFFEF3C7)
}

/** Six per-medication accent colours cycled by medication index. */
val MedicationAccents = listOf(
    Brand.Coral,
    Brand.Sage,
    Brand.Lavender,
    Brand.SkyBlue,
    Brand.Apricot,
    Brand.Mint,
)

/**
 * Reusable warm gradient brushes for hero areas, badges, and celebration
 * surfaces. Kept here so screens stay consistent and we can re-tune the
 * brand feel from one place.
 */
object BrandGradients {
    /** Soft top-down wash for the Today header / hero zones. */
    val Hero: Brush = Brush.verticalGradient(
        0.0f to Brand.Cream,
        0.7f to Brand.Cream.copy(alpha = 0.55f),
        1.0f to Brand.Canvas,
    )

    /** Wordmark badge — coral to apricot, sunny and warm. */
    val Badge: Brush = Brush.linearGradient(listOf(Brand.Coral, Brand.Apricot))

    /** All-done celebration. */
    val Celebrate: Brush = Brush.linearGradient(listOf(Brand.Sage, Brand.Mint))

    /** Empty state illustration — playful lavender to sky. */
    val EmptyTile: Brush = Brush.linearGradient(listOf(Brand.Lavender, Brand.SkyBlue))

    /** Onboarding page 1 — coral to apricot. */
    val OnboardingWarm: Brush = Brush.linearGradient(listOf(Brand.Coral.copy(alpha = 0.65f), Brand.Apricot))

    /** Onboarding page 2 — lavender to sky. */
    val OnboardingCool: Brush = Brush.linearGradient(listOf(Brand.Lavender, Brand.SkyBlue))

    /** Onboarding page 3 — sage to mint. */
    val OnboardingFresh: Brush = Brush.linearGradient(listOf(Brand.Sage, Brand.Mint))

    /** Pet profile hero backdrop — gentle radial wash. */
    fun petHero(): Brush = Brush.radialGradient(
        0.0f to Brand.Lavender.copy(alpha = 0.55f),
        1.0f to Brand.Canvas,
    )
}
