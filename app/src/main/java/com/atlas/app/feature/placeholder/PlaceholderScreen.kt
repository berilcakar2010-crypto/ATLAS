package com.atlas.app.feature.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.HairlineDivider
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination

/**
 * Henüz uygulanmamış modüller için DÜRÜST yer tutucu: ekran, ne zaman ve ne olarak
 * geleceğini açıkça söyler. Çalışıyormuş gibi görünen sahte içerik göstermez.
 */
@Composable
fun PlaceholderScreen(
    destination: Destination,
    phase: String,
    planned: List<String>,
) {
    ScreenFrame(section = destination.sectionLabel, title = destination.title) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasChip("Uygulanmadı", highlight = true)
                AtlasChip(phase)
            }
            AtlasPanel(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                SectionLabel("Planlanan kapsam")
                HairlineDivider()
                planned.forEach { line ->
                    Text(
                        text = "— $line",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtlasColors.Ivory,
                    )
                }
            }
            Text(
                text = "Bu ekran şimdilik yalnızca yer tutucudur; listelenen özelliklerin hiçbiri henüz çalışmıyor.",
                style = MaterialTheme.typography.bodyMedium,
                color = AtlasColors.TaupeText,
            )
        }
    }
}
