package com.atlas.app.feature.archive

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.data.db.FeedSourceEntity
import com.atlas.app.feature.common.SyncBar
import com.atlas.app.feature.common.relativeTime
import com.atlas.app.rememberAtlasApp

fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun ArchiveScreen() {
    val app = rememberAtlasApp()
    val context = LocalContext.current
    val db = app.database
    val feeds by db.feedDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val articleCount by db.articleDao().observeCount().collectAsStateWithLifecycle(0)
    val savedArticles by db.articleDao().observeSavedCount().collectAsStateWithLifecycle(0)
    val paperCount by db.paperDao().observeCount().collectAsStateWithLifecycle(0)
    val savedPapers by db.paperDao().observeSavedCount().collectAsStateWithLifecycle(0)
    val online = isOnline(context)

    ScreenFrame(Destination.OfflineArchive.sectionLabel, "Çevrimdışı arşiv") {
        SyncBar(app)
        AtlasPanel(Modifier.fillMaxWidth()) {
            SectionLabel("Durum")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AtlasChip(if (online) "İnternet var" else "İnternet yok · yerel veri okunuyor", highlight = online)
            }
            Text(
                "Cihazda $articleCount haber ($savedArticles kayıtlı) ve $paperCount makale ($savedPapers kayıtlı) var. " +
                    "Bunlar internet olmadan okunur. Eşitleme başarısız olursa mevcut veri silinmez; " +
                    "yalnızca kaydedilmemiş eski kayıtlar (haber 45, makale 120 günden eski) temizlenir.",
                style = MaterialTheme.typography.bodyMedium,
            )
            MetaText("Not: haberlerin tam metni indirilmez; yalnızca başlık, kaynağın özeti ve bağlantı saklanır. Tam metin için bağlantıyı açmak internet gerektirir.")
        }
        SectionLabel("Kaynak başına son durum · ${feeds.size}")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(feeds, key = { it.id }) { f -> FeedRow(f) }
        }
    }
}

@Composable
private fun FeedRow(f: FeedSourceEntity) {
    val (label, highlight) = when {
        !f.enabled -> "Kapalı" to false
        f.lastStatus == "OK" -> "Çevrimdışı hazır" to true
        f.lastStatus == "FAILED" -> "Başarısız" to false
        else -> "Eşitleme bekliyor" to false
    }
    AtlasPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(f.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = AtlasColors.Ivory)
            AtlasChip(label, highlight = highlight)
        }
        MetaText(
            when (f.lastStatus) {
                "OK" -> "Son başarılı: ${relativeTime(f.lastFetchedAt)} · ${f.lastCount} kayıt okundu"
                "FAILED" -> "Son deneme: ${relativeTime(f.lastFetchedAt)} · ${f.lastError ?: "bilinmeyen hata"}"
                else -> "Henüz denenmedi"
            },
        )
    }
}
