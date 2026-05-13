package com.nantcompany.clipy.processing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.EditorSessionViewModel
import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.navigation.AppRoute

// Design Tokens
private val BackgroundMidnight = Color(0xFF020617)
private val SurfaceGlass = Color(0xFF1E293B).copy(alpha = 0.5f)
private val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.15f)
private val PrimaryContainer = Color(0xFFB76DFF)
private val NeonCyan = Color(0xFF67E8F9)
private val TextMuted = Color(0xFF94A3B8)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun ProcessingScreen(
    sessionViewModel: EditorSessionViewModel,
    onNavigate: (AppRoute) -> Unit,
    viewModel: ProcessingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val pendingRequest = sessionState.pendingRequest
    val outputRepository = remember { LocalOutputRepository() }

    LaunchedEffect(pendingRequest, uiState.activeRequest, uiState.isRunning) {
        if (pendingRequest != null && uiState.activeRequest == null && !uiState.isRunning) {
            val request = sessionViewModel.consumePendingRequest() ?: return@LaunchedEffect
            viewModel.start(request)
        }
    }

    LaunchedEffect(uiState.isCompleted, uiState.output) {
        if (uiState.isCompleted) {
            uiState.output?.let {
                outputRepository.save(it)
                sessionViewModel.setLastOutput(it)
            }
            sessionViewModel.clearPendingRequest()
            viewModel.consumeCompletion()
            onNavigate(AppRoute.RESULT)
        }
    }

    BackHandler(enabled = uiState.isRunning) { }

    val outputName = pendingRequest?.outputPath?.substringAfterLast('/')?.substringAfterLast('\\') ?: "VIDEO_EXPORT.MP4"
    val progress = uiState.progressPercent.coerceIn(0, 100) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "Progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMidnight)
    ) {
        // Cinematic Background Glows
        Box(modifier = Modifier.offset(x = (-100).dp, y = (-100).dp).size(600.dp).blur(120.dp).background(PrimaryContainer.copy(alpha = 0.1f), CircleShape))
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).size(500.dp).blur(100.dp).background(NeonCyan.copy(alpha = 0.1f), CircleShape))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Central Progress Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceGlass)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Circular Progress
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val strokeWidthTrack = 8.dp.toPx()
                            val strokeWidthProgress = 12.dp.toPx()
                            val radius = (size.minDimension - strokeWidthProgress) / 2

                            // Track
                            drawCircle(
                                color = Color.White.copy(0.05f),
                                radius = radius,
                                style = Stroke(width = strokeWidthTrack)
                            )
                            // Progress
                            drawArc(
                                brush = Brush.linearGradient(
                                    colors = listOf(PrimaryContainer, NeonCyan)
                                ),
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidthProgress, cap = StrokeCap.Round)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-2).sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Exporting Master",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Estimated time: ${calculateRemaining(uiState.progressPercent)} remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Steps
                    ProcessingStepItem(
                        icon = Icons.Default.Check,
                        label = "Preparing audio & video tracks",
                        state = if (uiState.phase == ProcessingPhase.Preparing) StepState.Active else if (uiState.progressPercent > 10) StepState.Completed else StepState.Pending
                    )
                    ProcessingStepItem(
                        icon = Icons.Default.Refresh,
                        label = "Processing visual effects",
                        state = if (uiState.phase == ProcessingPhase.Processing && uiState.progressPercent < 90) StepState.Active else if (uiState.progressPercent >= 90) StepState.Completed else StepState.Pending
                    )
                    ProcessingStepItem(
                        icon = Icons.Default.Build,
                        label = "Writing final MP4 container",
                        state = if (uiState.progressPercent >= 90 && uiState.isRunning) StepState.Active else StepState.Pending
                    )
                }
            }

            // Output Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.1f))
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(0.8f))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "OUTPUT FILE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = PrimaryContainer
                    )
                    Text(
                        text = outputName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Please keep Clipy open in the foreground to ensure maximum rendering performance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            Button(
                onClick = { 
                    if (uiState.isRunning) viewModel.cancel() else onNavigate(AppRoute.HOME)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.1f),
                    contentColor = ErrorRed
                ),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.2f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Cancel Export", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Error Overlay
        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Export Failed", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(uiState.errorMessage ?: "Unknown error", color = TextMuted, textAlign = TextAlign.Center)
                    Button(
                        onClick = {
                            viewModel.clearFailure()
                            pendingRequest?.let { viewModel.start(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry Export")
                    }
                    TextButton(onClick = { onNavigate(AppRoute.HOME) }) {
                        Text("Back Home", color = Color.White)
                    }
                }
            }
        }
    }
}

enum class StepState { Pending, Active, Completed }

@Composable
fun ProcessingStepItem(
    icon: ImageVector,
    label: String,
    state: StepState
) {
    val opacity = when (state) {
        StepState.Completed -> 0.6f
        StepState.Active -> 1.0f
        StepState.Pending -> 0.4f
    }
    
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "Rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (state == StepState.Active) NeonCyan.copy(alpha = 0.2f)
                    else if (state == StepState.Completed) PrimaryContainer.copy(alpha = 0.2f)
                    else Color.White.copy(0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (state == StepState.Active) NeonCyan else if (state == StepState.Completed) PrimaryContainer else TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .run { if (state == StepState.Active && icon == Icons.Default.Refresh) rotate(rotation) else this }
            )
        }
        Text(
            text = label,
            style = if (state == StepState.Active) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyLarge,
            color = if (state == StepState.Active) Color.White else if (state == StepState.Completed) Color.White.copy(0.8f) else TextMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun calculateRemaining(progress: Int): String {
    if (progress <= 0) return "Calculating..."
    val remaining = (100 - progress) * 2 // Simulate 2 seconds per 1%
    val mins = remaining / 60
    val secs = remaining % 60
    return "%02d:%02d".format(mins, secs)
}
