package com.example.petmeds.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.petmeds.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val PlusJakartaSans = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Normal),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Bold),
)

private fun base(weight: FontWeight, size: Int, lineHeight: Int): TextStyle = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val PetMedsTypography = Typography(
    displaySmall   = base(FontWeight.Bold,     32, 40),
    headlineLarge  = base(FontWeight.Bold,     28, 36),
    headlineMedium = base(FontWeight.Bold,     24, 32),
    headlineSmall  = base(FontWeight.SemiBold, 20, 28),
    titleLarge     = base(FontWeight.SemiBold, 20, 28),
    titleMedium    = base(FontWeight.SemiBold, 17, 24),
    titleSmall     = base(FontWeight.SemiBold, 15, 20),
    bodyLarge      = base(FontWeight.Medium,   17, 26),
    bodyMedium     = base(FontWeight.Normal,   15, 22),
    bodySmall      = base(FontWeight.Normal,   13, 18),
    labelLarge     = base(FontWeight.SemiBold, 15, 20),
    labelMedium    = base(FontWeight.SemiBold, 13, 18),
    labelSmall     = base(FontWeight.SemiBold, 11, 16),
)
