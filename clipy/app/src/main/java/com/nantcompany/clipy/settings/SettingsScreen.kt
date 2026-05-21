package com.nantcompany.clipy.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.ClipyConfirmationDialog
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun SettingsScreen(
    onNavigate: (AppRoute) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ClipyScaffold(
        title = "Settings",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = "Appearance") {
                Text(
                    "Dark theme is enabled by default to save battery and reduce eye strain.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = ClipyDesignTokens.secondaryText
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = uiState.enableHardwareAcceleration,
                        onCheckedChange = { viewModel.toggleHardwareAcceleration() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ClipyDesignTokens.primaryAccent,
                            uncheckedColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Text("Enable GPU acceleration", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            SettingsSection(title = "Export & Storage") {
                Text(
                    "Output path:",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClipyDesignTokens.secondaryText
                )
                Text(
                    uiState.outputHistoryPath,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ClipyDesignTokens.secondaryText
                )
                
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.05f))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = uiState.keepOriginalFiles,
                        onCheckedChange = { viewModel.toggleKeepOriginal() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ClipyDesignTokens.primaryAccent,
                            uncheckedColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Text("Keep original files after import", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            SettingsSection(title = "Maintenance") {
                ClipyPrimaryButton(
                    label = "Clear Cache Files", 
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.clearTempFiles() }
                )
                ClipySecondaryButton(
                    label = "Reset Export History", 
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.askClearHistory() }
                )
            }

            SettingsSection(title = "App Info") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text(uiState.appVersionLabel, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold)
                }
                Text("Privacy Policy", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.primaryAccent, modifier = Modifier.clickable { })
                Text("Terms of Service", style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.primaryAccent, modifier = Modifier.clickable { })
            }

            uiState.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.primaryAccent, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                LaunchedEffect(it) { viewModel.consumeMessage() }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (uiState.confirmClearHistory) {
        ClipyConfirmationDialog(
            title = "Clear history?",
            message = "This will remove all records from the history list. Your actual video files will not be deleted.",
            confirmLabel = "Clear All",
            dismissLabel = "Cancel",
            onConfirm = { viewModel.clearHistory() },
            onDismiss = { viewModel.cancelClearHistory() }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ClipySectionTitle(text = title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
            colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}
