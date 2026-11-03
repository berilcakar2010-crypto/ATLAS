package com.atlas.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.atlas.app.core.design.AtlasColors
import com.atlas.app.core.design.HairlineDivider
import com.atlas.app.core.design.MetaText
import com.atlas.app.core.design.SectionLabel
import com.atlas.app.core.design.SourceSerif
import com.atlas.app.core.navigation.Destination
import com.atlas.app.feature.command.CommandCenterScreen
import com.atlas.app.feature.library.LibraryScreen
import com.atlas.app.feature.archive.ArchiveScreen
import com.atlas.app.feature.deepdive.DeepDiveScreen
import com.atlas.app.feature.intelligence.IntelligenceScreen
import com.atlas.app.feature.notebook.NotebookScreen
import com.atlas.app.feature.radar.RadarScreen
import com.atlas.app.feature.sustainability.SustainabilityScreen
import com.atlas.app.feature.questions.QuestionsScreen
import com.atlas.app.feature.settings.SettingsScreen

private val ExpandedSidebarWidth = 232.dp
private val RailWidth = 76.dp
private val ExpandedBreakpoint = 900.dp

@Composable
fun AtlasShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStackEntry?.destination?.route) ?: Destination.CommandCenter

    fun navigateTo(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(Destination.CommandCenter.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val expanded = maxWidth >= ExpandedBreakpoint
        Row(Modifier.fillMaxSize()) {
            AtlasSidebar(current = current, expanded = expanded, onSelect = ::navigateTo)
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(AtlasColors.Line),
            )
            NavHost(
                navController = navController,
                startDestination = Destination.CommandCenter.route,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                composable(Destination.CommandCenter.route) {
                    CommandCenterScreen(onNavigate = ::navigateTo)
                }
                composable(Destination.DailyIntelligence.route) { IntelligenceScreen() }
                composable(Destination.ResearchRadar.route) { RadarScreen() }
                composable(Destination.DeepDive.route) { DeepDiveScreen() }
                composable(Destination.Library.route) { LibraryScreen() }
                composable(Destination.Questions.route) { QuestionsScreen() }
                composable(Destination.Notebook.route) { NotebookScreen() }
                composable(Destination.Sustainability.route) { SustainabilityScreen() }
                composable(Destination.OfflineArchive.route) { ArchiveScreen() }
                composable(Destination.Settings.route) { SettingsScreen() }
            }
        }
    }
}

@Composable
private fun AtlasSidebar(
    current: Destination,
    expanded: Boolean,
    onSelect: (Destination) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(if (expanded) ExpandedSidebarWidth else RailWidth)
            .fillMaxHeight()
            .background(AtlasColors.Surface2)
            .padding(vertical = 22.dp),
    ) {
        SidebarBrand(expanded)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp),
        ) {
            Destination.entries.forEach { destination ->
                SidebarItem(
                    destination = destination,
                    selected = destination == current,
                    expanded = expanded,
                    onSelect = onSelect,
                )
            }
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                HairlineDivider()
                SectionLabel("Veri", color = AtlasColors.TaupeText)
                MetaText("Yalnızca bu cihazda saklanır. Çevrimdışı çalışır.")
            }
        }
    }
}

@Composable
private fun SidebarBrand(expanded: Boolean) {
    if (expanded) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "ATLAS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = SourceSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    letterSpacing = 8.sp,
                ),
                color = AtlasColors.Ivory,
            )
            MetaText("PERSONAL INTELLIGENCE SYSTEM")
        }
    } else {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "A",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AtlasColors.Ivory,
            )
        }
    }
}

@Composable
private fun SidebarItem(
    destination: Destination,
    selected: Boolean,
    expanded: Boolean,
    onSelect: (Destination) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (selected) AtlasColors.Burgundy else Color.Transparent)
            .semantics { contentDescription = destination.title }
            .selectable(selected = selected, onClick = { onSelect(destination) }, role = Role.Tab),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(if (selected) AtlasColors.Brass else Color.Transparent),
        )
        if (expanded) {
            Spacer(Modifier.width(20.dp))
            Text(
                text = destination.index,
                modifier = Modifier.width(30.dp),
                style = MaterialTheme.typography.labelMedium,
                color = AtlasColors.Brass,
            )
            Text(
                text = destination.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AtlasColors.Ivory else AtlasColors.TaupeText,
            )
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = destination.index,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) AtlasColors.Ivory else AtlasColors.TaupeText,
                )
            }
        }
    }
}
