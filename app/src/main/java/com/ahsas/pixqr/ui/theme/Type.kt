package com.ahsas.pixqr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ahsas.pixqr.R

val playfairFontFamily = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = playfairFontFamily, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = playfairFontFamily, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = playfairFontFamily, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = playfairFontFamily, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = playfairFontFamily, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = playfairFontFamily, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = playfairFontFamily, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = playfairFontFamily, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = playfairFontFamily, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = playfairFontFamily, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = playfairFontFamily, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = playfairFontFamily, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = playfairFontFamily, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = playfairFontFamily, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = playfairFontFamily, fontSize = 11.sp),
)