package com.example.birdnote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.birdnote.R

val Amaranth = FontFamily(
    Font(R.font.amaranth_regular, FontWeight.Normal),
    Font(R.font.amaranth_bold, FontWeight.Bold),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Amaranth,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Amaranth,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Amaranth,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Amaranth,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Amaranth,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Amaranth,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 0.72.sp,
    ),
)