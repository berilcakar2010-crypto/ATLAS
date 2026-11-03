package com.atlas.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlas.app.core.design.AtlasButton
import com.atlas.app.core.design.AtlasChip
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.AtlasPanel
import com.atlas.app.core.design.AtlasTextButton
import com.atlas.app.core.design.EmptyHint
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.ScreenFrame
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.navigation.Destination
import com.atlas.app.core.util.Format
import com.atlas.app.data.db.LibraryDao
import com.atlas.app.data.db.LibraryItemEntity
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val dao: LibraryDao) : ViewModel() {
    val items: StateFlow<List<LibraryItemEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addNote(title: String, body: String) {
        viewModelScope.launch {
            dao.insert(LibraryItemEntity(type = "NOTE", title = title.trim(), body = body.trim()))
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { dao.toggleFavorite(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}

@Composable
fun LibraryScreen() {
    val app = rememberAtlasApp()
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LibraryViewModel(app.database.libraryDao()) }
        },
    )
    val items by vm.items.collectAsStateWithLifecycle()

    ScreenFrame(section = Destination.Library.sectionLabel, title = "Kütüphane") {
        var tab by rememberSaveable { mutableStateOf("notes") }
        com.atlas.app.feature.common.FilterRow(
            listOf("notes" to "Notlarım", "saved" to "Tüm kayıtlılar"),
            tab,
            { tab = it },
        )
        if (tab == "saved") {
            Box(Modifier.weight(1f).fillMaxWidth()) { SavedPanel(app.database) }
            return@ScreenFrame
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= 820.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ItemList(items, vm, Modifier.weight(1.5f).fillMaxHeight())
                    NoteForm(vm, Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    NoteForm(vm, Modifier.fillMaxWidth())
                    ItemList(items, vm, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun NoteForm(vm: LibraryViewModel, modifier: Modifier) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }

    AtlasPanel(modifier) {
        SectionLabel("Yeni not")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Başlık") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Not") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        AtlasButton(
            text = "Notu kaydet",
            enabled = title.isNotBlank(),
            onClick = {
                vm.addNote(title, body)
                title = ""
                body = ""
            },
        )
    }
}

@Composable
private fun ItemList(items: List<LibraryItemEntity>, vm: LibraryViewModel, modifier: Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) {
            item(key = "empty") {
                EmptyHint("Kütüphane boş. İlk notunu formdan ekleyebilirsin.")
            }
        }
        items(items, key = { it.id }) { item ->
            AtlasPanel(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AtlasChip(item.type)
                    if (item.favorite) AtlasChip("Favori", highlight = true)
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AtlasColors.Ivory,
                )
                if (item.body.isNotBlank()) {
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtlasColors.TaupeText,
                        maxLines = 6,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetaText(Format.timestamp(item.createdAt))
                    Row {
                        AtlasTextButton(
                            text = if (item.favorite) "Favoriden çıkar" else "Favori",
                            onClick = { vm.toggleFavorite(item.id) },
                        )
                        AtlasTextButton("Sil", onClick = { vm.delete(item.id) })
                    }
                }
            }
        }
    }
}
