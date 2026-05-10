package com.nantcompany.clipy.home.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.home.components.HeaderRow
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.settings.SettingsViewModel
import com.nantcompany.clipy.theme.ClipyDesignTokens

private val CardBg = Color(0x661E293B)
private val CardBorder = Color(0x1AFFFFFF)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val NeonPurple = Color(0xFFD8B4FE)
private val NeonCyan = Color(0xFF67E8F9)

@Composable
fun SettingsPage(onNavigate: (AppRoute) -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClipyDesignTokens.screenPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HeaderRow() }

        item {
            Text(
                "Settings",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            SettingsSectionCard(title = "Appearance", icon = Icons.Default.Star) {
                SettingRow(
                    title = "Theme",
                    subtitle = "Currently enforced to Dark Mode",
                    trailingText = "Dark"
                )
            }
        }

        item {
            SettingsSectionCard(title = "Export Defaults", icon = Icons.Default.Share) {
                SettingRow(
                    title = "Default Resolution",
                    subtitle = "Standard quality for quick renders",
                    trailingText = "1080p HD"
                )

                DividerLine()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hardware Acceleration", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text("Use GPU for faster processing", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = uiState.enableHardwareAcceleration,
                        onCheckedChange = { viewModel.toggleHardwareAcceleration() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple,
                            uncheckedThumbColor = Color(0xFFCBD5E1),
                            uncheckedTrackColor = Color(0x33475569)
                        )
                    )
                }

                DividerLine()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep Original Files", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (uiState.keepOriginalFiles) "Enabled" else "Disabled",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = uiState.keepOriginalFiles,
                        onCheckedChange = { viewModel.toggleKeepOriginal() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple,
                            uncheckedThumbColor = Color(0xFFCBD5E1),
                            uncheckedTrackColor = Color(0x33475569)
                        )
                    )
                }
            }
        }

        item {
            SettingsSectionCard(title = "Storage", icon = Icons.Default.Build) {
                Text("App Cache Usage", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2.4 GB / 10 GB", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "History: ${uiState.outputHistoryPath}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF020617))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.24f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(NeonPurple)
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.clearTempFiles() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Cache")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x14EF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33EF4444))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Danger Zone", color = Color(0xFFEF4444), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Irreversible actions for your account", color = Color(0xCCEF4444), style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { viewModel.askClearHistory() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x1AEF4444),
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Render History")
                    }
                }
            }
        }

        item {
            SettingsSectionCard(title = "About Clipy", icon = Icons.Default.Settings) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("About Clipy", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Text(uiState.appVersionLabel, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                DividerLine()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppRoute.SETTINGS) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Privacy Policy", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            uiState.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                LaunchedEffect(it) { viewModel.consumeMessage() }
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }

    if (uiState.confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearHistory() },
            title = { Text("Clear export history?") },
            text = { Text("This removes history records only and does not delete actual files.") },
            confirmButton = { Button(onClick = { viewModel.clearHistory() }) { Text("Clear") } },
            dismissButton = { OutlinedButton(onClick = { viewModel.cancelClearHistory() }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            DividerLine()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailingText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trailingText, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x14FFFFFF))
    )
}
