package com.atlas.app.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.atlas.app.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weightValue: Int, weight: FontWeight): Font =
    Font(
        resId = resId,
        weight = weight,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(weightValue)),
    )

/** Başlıklar: Source Serif 4 (OFL). */
val SourceSerif = FontFamily(
    variableFont(R.font.source_serif_4, 400, FontWeight.Normal),
    variableFont(R.font.source_serif_4, 600, FontWeight.SemiBold),
)

/** Gövde: Inter (OFL). */
val InterFamily = FontFamily(
    variableFont(R.font.inter, 400, FontWeight.Normal),
    variableFont(R.font.inter, 500, FontWeight.Medium),
    variableFont(R.font.inter, 600, FontWeight.SemiBold),
)

/** Teknik etiketler ve meta veri: JetBrains Mono (OFL). */
val MonoFamily = FontFamily(
    variableFont(R.font.jetbrains_mono, 400, FontWeight.Normal),
    variableFont(R.font.jetbrains_mono, 500, FontWeight.Medium),
)

val AtlasTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SourceSerif, fontWeight = FontWeight.Normal,
        fontSize = 30.sp, lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SourceSerif, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SourceSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SourceSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp,
    ),
)
