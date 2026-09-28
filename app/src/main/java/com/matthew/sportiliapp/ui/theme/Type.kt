package com.matthew.sportiliapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.matthew.sportiliapp.R

val montserratFontFamily = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

private fun montserratStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f
) = TextStyle(
    fontFamily = montserratFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val CustomTypography = Typography(
    displayLarge = montserratStyle(FontWeight.Bold, 32, 38),
    displayMedium = montserratStyle(FontWeight.Bold, 30, 36),
    displaySmall = montserratStyle(FontWeight.Bold, 28, 34),
    headlineLarge = montserratStyle(FontWeight.Bold, 26, 32),
    headlineMedium = montserratStyle(FontWeight.Bold, 24, 30),
    headlineSmall = montserratStyle(FontWeight.Bold, 22, 28),
    titleLarge = montserratStyle(FontWeight.Bold, 22, 28),
    titleMedium = montserratStyle(FontWeight.SemiBold, 18, 24),
    titleSmall = montserratStyle(FontWeight.SemiBold, 16, 22),
    bodyLarge = montserratStyle(FontWeight.Normal, 17, 25, 0.1f),
    bodyMedium = montserratStyle(FontWeight.Normal, 16, 24, 0.1f),
    bodySmall = montserratStyle(FontWeight.Normal, 14, 20, 0.1f),
    labelLarge = montserratStyle(FontWeight.SemiBold, 14, 20, 0.1f),
    labelMedium = montserratStyle(FontWeight.SemiBold, 13, 18, 0.1f),
    labelSmall = montserratStyle(FontWeight.Medium, 12, 16, 0.1f)
)
