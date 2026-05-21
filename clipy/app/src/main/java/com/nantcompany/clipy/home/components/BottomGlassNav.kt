package com.nantcompany.clipy.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.home.HomeTab
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun BottomGlassNav(
    currentIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(ClipyDesignTokens.bgNav.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(36.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEachIndexed { index, tab ->
                val active = currentIndex == index
                NavItem(
                    label = tab.label,
                    icon = tab.icon,
                    active = active,
                    onClick = { onTabClick(index) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.secondaryText
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(interactionSource = null, indication = null) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (active) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .blur(16.dp)
                        .background(ClipyDesignTokens.primaryAccent.copy(alpha = 0.25f), CircleShape)
                )
            }
            Icon(
                icon, 
                contentDescription = label, 
                tint = tint, 
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label, 
            color = tint, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
