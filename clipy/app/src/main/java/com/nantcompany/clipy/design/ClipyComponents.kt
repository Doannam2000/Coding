package com.nantcompany.clipy.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun ClipySectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun ClipyPrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled) { Text(label) }
}

@Composable
fun ClipySecondaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled) { Text(label) }
}

@Composable
fun ClipyToolCard(
    title: String,
    description: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(ClipyDesignTokens.toolCardCorner),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(icon, contentDescription = title, tint = ClipyDesignTokens.primaryAccent)
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.linkText)
        }
    }
}

@Composable
fun ClipyEmptyState(title: String, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface), shape = RoundedCornerShape(ClipyDesignTokens.cardCorner)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(message, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
            if (actionLabel != null && onAction != null) ClipySecondaryButton(actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ClipyErrorState(message: String, retryLabel: String = "Retry", onRetry: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface), shape = RoundedCornerShape(ClipyDesignTokens.cardCorner)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            if (onRetry != null) ClipySecondaryButton(retryLabel, onClick = onRetry)
        }
    }
}

@Composable
fun ClipyLoadingState(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator()
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.secondaryText)
    }
}

@Composable
fun ClipyBottomActionBar(primaryLabel: String, onPrimary: () -> Unit, secondaryLabel: String? = null, onSecondary: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (secondaryLabel != null && onSecondary != null) {
            ClipySecondaryButton(label = secondaryLabel, onClick = onSecondary)
        }
        ClipyPrimaryButton(label = primaryLabel, onClick = onPrimary)
    }
}

@Composable
fun ClipyConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } }
    )
}

@Composable
fun ClipyTopBarHero(title: String, subtitle: String, accent: Brush = ClipyDesignTokens.heroBrush, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(ClipyDesignTokens.heroCorner), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.background(accent).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = ClipyDesignTokens.heroTitle)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.heroSubtitle)
                content()
            }
        }
    }
}
