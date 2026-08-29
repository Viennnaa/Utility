package com.viennnaa.utilities.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

val UtilitiesTypography = Typography()

/**
 * The oversized style shared by mini apps that show a single result — the rolled
 * number, the yes/no answer. Kept here so every result reads at the same weight.
 */
val ResultTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 84.sp,
    lineHeight = 92.sp,
    textAlign = TextAlign.Center,
)
