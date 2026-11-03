package com.atlas.app.feature.dictionary

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlas.app.core.design.AtlasButton
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasOutlinedButton
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.data.db.VocabEntity
import com.atlas.app.feature.common.AtlasField
import com.atlas.app.feature.common.FilterRow
import com.atlas.app.rememberAtlasApp
import java.net.URLEncoder
import kotlinx.coroutines.launch

private fun turengUrl(word: String) =
    "https://tureng.com/tr/turkce-ingilizce/" + URLEncoder.encode(word.trim(), "UTF-8").replace("+", "%20")

@Composable
fun DictionaryScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var loadedUrl by rememberSaveable { mutableStateOf("") }
    var draftWord by rememberSaveable { mutableStateOf("") }

    ScreenFrame(Destination.Dictionary.sectionLabel, "Sözlük") {
        MetaText(
            "Tureng'e bağlanır (tureng.com). ATLAS sayfayı kazımaz; Tureng uygulama içinde normal bir web sayfası olarak açılır, " +
                "bu yüzden anlamı kendin seçip aşağıya kaydedersin. Tureng'in kendi koşulları geçerlidir; açılmazsa 'Tarayıcıda aç'ı dene.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AtlasField(query, { query = it }, "Sözcük veya ifade (İngilizce ya da Türkçe)", Modifier.weight(1f), singleLine = true)
            AtlasButton("Ara", {
                if (query.isNotBlank()) {
                    loadedUrl = turengUrl(query)
                    draftWord = query.trim()
                }
            }, enabled = query.isNotBlank())
            val context = LocalContext.current
            AtlasOutlinedButton("Tarayıcıda aç", {
                if (query.isNotBlank()) context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(turengUrl(query))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }, enabled = query.isNotBlank())
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 900.dp
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TurengView(loadedUrl, Modifier.weight(1.2f).fillMaxHeight())
                    WordsColumn(draftWord, { draftWord = it }, { w -> query = w; loadedUrl = turengUrl(w) }, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TurengView(loadedUrl, Modifier.fillMaxWidth().height(320.dp))
                    WordsColumn(draftWord, { draftWord = it }, { w -> query = w; loadedUrl = turengUrl(w) }, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TurengView(url: String, modifier: Modifier) {
    val context = LocalContext.current
    if (url.isBlank()) {
        AtlasPanel(modifier) {
            SectionLabel("Tureng")
            EmptyHint("Bir sözcük yazıp 'Ara'ya bas; sonuç burada açılır.")
        }
        return
    }
    AndroidView(
        modifier = modifier.border(1.dp, AtlasColors.Line),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val host = request.url.host.orEmpty()
                        if (host == "tureng.com" || host.endsWith(".tureng.com")) return false
                        // Başka siteler (reklam vb.) uygulama içinde açılmaz.
                        return true
                    }
                }
                loadUrl(url)
                tag = url
            }
        },
        update = { view ->
            if (view.tag != url) {
                view.tag = url
                view.loadUrl(url)
            }
        },
    )
}

@Composable
private fun WordsColumn(draftWord: String, onDraft: (String) -> Unit, onLookup: (String) -> Unit, modifier: Modifier) {
    val app = rememberAtlasApp()
    val dao = app.database.vocabDao()
    val scope = rememberCoroutineScope()
    val words by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    var meaning by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    var context by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var search by rememberSaveable { mutableStateOf("") }
    var hide by rememberSaveable { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }

    val shown = words.filter {
        (filter == "all" || (filter == "learning" && !it.known) || (filter == "known" && it.known)) &&
            (search.isBlank() || it.word.contains(search, true) || it.meaning.contains(search, true))
    }

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AtlasPanel(Modifier.fillMaxWidth()) {
                SectionLabel("Sözcük kaydet")
                AtlasField(draftWord, onDraft, "Sözcük / ifade", Modifier.fillMaxWidth(), singleLine = true)
                AtlasField(meaning, { meaning = it }, "Anlamı (Tureng'den seçtiğin)", Modifier.fillMaxWidth(), singleLine = true)
                AtlasField(example, { example = it }, "Örnek cümle (isteğe bağlı)", Modifier.fillMaxWidth(), minLines = 2)
                AtlasField(context, { context = it }, "Nerede gördün? (haber, kitap…)", Modifier.fillMaxWidth(), singleLine = true)
                AtlasButton("Sözlüğüme ekle", {
                    scope.launch {
                        if (dao.countWord(draftWord.trim()) > 0) {
                            info = "Bu sözcük zaten kayıtlı."
                        } else {
                            dao.insert(VocabEntity(word = draftWord.trim(), meaning = meaning.trim(), example = example.trim(), context = context.trim()))
                            onDraft(""); meaning = ""; example = ""; context = ""
                            info = "Kaydedildi."
                        }
                    }
                }, enabled = draftWord.isNotBlank() && meaning.isNotBlank())
                info?.let { MetaText(it) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Sözlüğüm · ${words.size}")
                FilterRow(listOf("all" to "Hepsi", "learning" to "Öğreniyorum", "known" to "Biliyorum"), filter, { filter = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AtlasField(search, { search = it }, "Kayıtlı sözcüklerde ara", Modifier.weight(1f), singleLine = true)
                    AtlasChip(if (hide) "Tekrar modu: anlamlar gizli" else "Tekrar modu: kapalı", highlight = hide, modifier = Modifier.clickable { hide = !hide })
                }
            }
        }
        if (shown.isEmpty()) item { EmptyHint("Burada gösterilecek sözcük yok.") }
        items(shown, key = { it.id }) { w -> WordCard(w, hide, onLookup, dao, scope) }
    }
}

@Composable
private fun WordCard(
    w: VocabEntity,
    hide: Boolean,
    onLookup: (String) -> Unit,
    dao: com.atlas.app.data.db.VocabDao,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var revealed by remember(w.id, hide) { mutableStateOf(false) }
    AtlasPanel(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(w.word, style = MaterialTheme.typography.titleMedium, color = AtlasColors.Ivory, modifier = Modifier.weight(1f))
            if (w.known) AtlasChip("Biliyorum", highlight = true)
        }
        if (hide && !revealed) {
            AtlasOutlinedButton("Anlamı göster", { revealed = true })
        } else {
            Text(w.meaning, style = MaterialTheme.typography.bodyMedium)
            if (w.example.isNotBlank()) MetaText("“${w.example}”")
            if (w.context.isNotBlank()) MetaText("Kaynak: ${w.context}")
        }
        MetaText("${w.reviewCount} tekrar")
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AtlasTextButton("Biliyorum", { scope.launch { dao.markReviewed(w.id, true) } })
            AtlasTextButton("Tekrar", { scope.launch { dao.markReviewed(w.id, false) } })
            AtlasTextButton("Tureng'de aç", { onLookup(w.word) })
            AtlasTextButton("Sil", { scope.launch { dao.delete(w.id) } })
        }
    }
}
