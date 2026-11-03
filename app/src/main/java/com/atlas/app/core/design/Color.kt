package com.atlas.app.core.design

import androidx.compose.ui.graphics.Color

/** ATLAS paleti. Parlak kırmızı, neon ve mor-AI gradyanı bilinçli olarak yok. */
object AtlasColors {
    val Burgundy = Color(0xFF421C30)
    val Charcoal = Color(0xFF29272B)
    val Surface2 = Color(0xFF211F23)
    val Ivory = Color(0xFFE7D8C4)
    val Brass = Color(0xFFB38A55)

    /** Küçük yazıda kontrast için biraz açık pirinç (koyu zeminde ≥ 4.5:1). */
    val BrassText = Color(0xFFC9A06A)

    /** Yalnızca çizgi ve dekorda; küçük metinde TaupeText kullanılır. */
    val Taupe = Color(0xFF8D8179)
    val TaupeText = Color(0xFFA89E95)

    /** Hata / başarısız durum için sönük kil rengi (parlak kırmızı değil). */
    val Rose = Color(0xFFC48A7A)

    val Line = Color(0x4DB38A55)
    val LineSoft = Color(0x598D8179)
}
