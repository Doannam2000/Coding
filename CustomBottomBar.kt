package com.nextplay.DIY.LiveWallpaper.gify.ui.screen.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.nextplay.DIY.LiveWallpaper.gify.R

// ── Liquid Glass Config ─────────────────────────────────────────────────────
private val PillActiveColor = Color(0xFFFF008B)
private val PillActiveBg = Color(0x22FF008B)
private val LabelActiveColor = Color(0xFFFF008B)

private val BarHeight = 68.dp
private val BarHPad = 20.dp
private val BarRadius = 34.dp
private val IndicatorLineHeight = 3.dp
private val IndicatorLineRadius = 2.dp

/**
 * Liquid‑Glass bottom navigation that sees through to the content behind it.
 *
 * @param backdrop  The [Backdrop] captured at the HomeScreen level so the bar
 *                  can refract / blur the actual wallpaper‑grid content beneath.
 */
@Composable
fun CustomBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSettingClick: () -> Unit = {},
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val tabPositions = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

    val indicatorX by animateFloatAsState(
        targetValue = tabPositions[selectedTab]?.first ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillX"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = tabPositions[selectedTab]?.second ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillW"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BarHPad)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        // ── Glass frost layer (draws blurred content behind) ────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(BarRadius) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.45f))
                    }
                )
        )

        // ── Tab items + animated pill (drawn on top of the glass) ───────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .drawBehind {
                    if (indicatorWidth > 0f) {
                        val lineH = with(density) { IndicatorLineHeight.toPx() }
                        val r = with(density) { IndicatorLineRadius.toPx() }
                        val yOff = size.height - lineH - with(density) { 6.dp.toPx() }
                        val lineW = indicatorWidth * 0.5f
                        val lineX = indicatorX + (indicatorWidth - lineW) / 2f

                        // Glow mờ phía dưới
                        drawRoundRect(
                            color = PillActiveColor.copy(alpha = 0.25f),
                            topLeft = Offset(lineX - 4f, yOff - 2f),
                            size = Size(lineW + 8f, lineH + 4f),
                            cornerRadius = CornerRadius(r + 2f)
                        )
                        // Gạch chân chính
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    PillActiveColor.copy(alpha = 0.6f),
                                    PillActiveColor,
                                    PillActiveColor.copy(alpha = 0.6f)
                                )
                            ),
                            topLeft = Offset(lineX, yOff),
                            size = Size(lineW, lineH),
                            cornerRadius = CornerRadius(r)
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidTab(0, selectedTab == 0, R.drawable.ic_star_hone_selected,
                    "DIY", { onTabSelected(0) }) { x, w -> tabPositions[0] = x to w }

                LiquidTab(1, selectedTab == 1, R.drawable.ic_live_wallpaper_selected ,
                    stringResource(R.string.live), { onTabSelected(1) }) { x, w -> tabPositions[1] = x to w }

                LiquidTab(2, false,
                    R.drawable.ic_setting,
                    stringResource(R.string.settings), { onSettingClick() }) { _, _ -> }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun LiquidTab(
    index: Int,
    selected: Boolean,
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
    onPositioned: (Float, Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "ic"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(250), label = "la"
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(interactionSource, null, onClick = onClick)
            .padding(horizontal = 22.dp)
            .onGloballyPositioned {
                onPositioned(it.positionInParent().x, it.size.width.toFloat())
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = label,
            modifier = Modifier.size(26.dp).scale(iconScale)
        )

        if (selected) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = LabelActiveColor,
                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}
