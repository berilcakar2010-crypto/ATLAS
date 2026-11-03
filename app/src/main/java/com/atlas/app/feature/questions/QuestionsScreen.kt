package com.atlas.app.feature.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.atlas.app.data.db.QuestionDao
import com.atlas.app.data.db.QuestionEntity
import com.atlas.app.data.db.QuestionStatus
import com.atlas.app.rememberAtlasApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuestionsViewModel(private val dao: QuestionDao) : ViewModel() {
    val questions: StateFlow<List<QuestionEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, subject: String, description: String) {
        viewModelScope.launch {
            dao.insert(
                QuestionEntity(
                    title = title.trim(),
                    subject = subject.trim(),
                    description = description.trim(),
                ),
            )
        }
    }

    fun setStatus(id: Long, status: QuestionStatus) {
        viewModelScope.launch { dao.setStatus(id, status.name, System.currentTimeMillis()) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}

@Composable
fun QuestionsScreen() {
    val app = rememberAtlasApp()
    val vm: QuestionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { QuestionsViewModel(app.database.questionDao()) }
        },
    )
    val questions by vm.questions.collectAsStateWithLifecycle()

    ScreenFrame(section = Destination.Questions.sectionLabel, title = "Soru Motoru") {
        Text(
            text = "Okurken ve düşünürken aklına gelen soruları kaydet. Amaç soruyu kapatmak değil, derinleştirmek.",
            style = MaterialTheme.typography.bodyMedium,
            color = AtlasColors.TaupeText,
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= 820.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    QuestionList(questions, vm, Modifier.weight(1.5f).fillMaxHeight())
                    QuestionForm(vm, Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    QuestionForm(vm, Modifier.fillMaxWidth())
                    QuestionList(questions, vm, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun QuestionForm(vm: QuestionsViewModel, modifier: Modifier) {
    var title by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    AtlasPanel(modifier) {
        SectionLabel("Yeni soru")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Soru") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Konu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Ayrıntı veya hipotezin") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        AtlasButton(
            text = "Soruyu kaydet",
            enabled = title.isNotBlank(),
            onClick = {
                vm.add(title, subject, description)
                title = ""
                subject = ""
                description = ""
            },
        )
    }
}

@Composable
private fun QuestionList(
    questions: List<QuestionEntity>,
    vm: QuestionsViewModel,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (questions.isEmpty()) {
            item(key = "empty") {
                EmptyHint("Henüz soru yok. İlk sorunu yukarıdaki veya yandaki formdan kaydedebilirsin.")
            }
        }
        items(questions, key = { it.id }) { question ->
            QuestionCard(question, vm)
        }
    }
}

@Composable
private fun QuestionCard(question: QuestionEntity, vm: QuestionsViewModel) {
    val status = QuestionStatus.fromName(question.status)
    AtlasPanel(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasChip(status.label, highlight = status.isOpen)
            StatusSelector(status) { vm.setStatus(question.id, it) }
        }
        Text(
            text = question.title,
            style = MaterialTheme.typography.titleMedium,
            color = AtlasColors.Ivory,
        )
        if (question.subject.isNotBlank()) {
            MetaText("Konu: ${question.subject}")
        }
        if (question.description.isNotBlank()) {
            Text(
                text = question.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AtlasColors.TaupeText,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaText(Format.timestamp(question.updatedAt))
            AtlasTextButton("Sil", onClick = { vm.delete(question.id) })
        }
    }
}

@Composable
private fun StatusSelector(current: QuestionStatus, onSelect: (QuestionStatus) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AtlasTextButton("Durumu değiştir", onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            QuestionStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (option == current) "● ${option.label}" else option.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}
