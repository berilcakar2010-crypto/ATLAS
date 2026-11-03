package com.atlas.app.feature.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.AtlasApp
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.util.Format
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Format.turkish)

/** "3 sa önce", "dün", "12 Eki 2026" biçiminde kısa tarih. */
fun relativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - epochMillis
    val minutes = diff / 60_000
    return when {
        diff < 0 -> dayFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
        minutes < 1 -> "az önce"
        minutes < 60 -> "$minutes dk önce"
        minutes < 24 * 60 -> "${minutes / 60} sa önce"
        minutes < 48 * 60 -> "dün"
        minutes < 14 * 24 * 60 -> "${minutes / (24 * 60)} gün önce"
        else -> dayFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
}

/** Adresi sistem tarayıcısında açar. Yalnızca http(s) adreslerine izin verilir. */
fun openUrl(context: Context, url: String) {
    val lower = url.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(context, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show()
    }
}

/** Yatay kaydırılan seçim şeridi. [options] = (anahtar, etiket). */
@Composable
fun FilterRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (key, label) ->
            AtlasChip(
                text = label,
                highlight = key == selected,
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clickable { onSelect(key) },
            )
        }
    }
}

@Composable
fun AtlasField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
    )
}

/** Senkronizasyon durumu + elle yenileme düğmesi. */
@Composable
fun SyncBar(app: AtlasApp, modifier: Modifier = Modifier) {
    val status by app.syncManager.status.collectAsStateWithLifecycle()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtlasOutlinedButton(
            text = if (status.running) "Eşitleniyor…" else "Şimdi eşitle",
            enabled = !status.running,
            onClick = { app.syncNow(forcePapers = false) },
        )
        val text = when {
            status.running -> "${status.phase} · ${status.done}/${status.total}"
            status.lastFinishedAt > 0 ->
                "Son eşitleme ${relativeTime(status.lastFinishedAt)} · +${status.lastNewArticles} haber, +${status.lastNewPapers} makale" +
                    if (status.lastFailedFeeds > 0) " · ${status.lastFailedFeeds} kaynak yanıt vermedi" else ""
            else -> "Henüz eşitleme yapılmadı. Yerel kayıtlar internet olmadan da açılır."
        }
        MetaText(text, Modifier.padding(vertical = 2.dp))
    }
}

/** Kullanıcının kategori listesi (sıralı). */
@Composable
fun rememberCategories(app: AtlasApp): List<com.atlas.app.data.db.CategoryEntity> {
    val list by app.database.categoryDao().observeAll().collectAsStateWithLifecycle(emptyList())
    return list
}
