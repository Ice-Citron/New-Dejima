package com.dejima.workouttimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = dynamicLightColorScheme(this)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkoutTimerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTimerScreen() {
    val presets = listOf(30, 60, 90)
    var totalSeconds by remember { mutableIntStateOf(30) }
    var remainingSeconds by remember { mutableIntStateOf(30) }
    var isRunning by remember { mutableStateOf(false) }

    // Countdown effect
    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0) {
                isRunning = false
            }
        }
    }

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%d:%02d".format(minutes, seconds)
    val finished = remainingSeconds == 0 && !isRunning && totalSeconds > 0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Workout Timer") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preset selector
            Text("Select Duration", style = MaterialTheme.typography.titleMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                presets.forEachIndexed { idx, secs ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = presets.size),
                        onClick = {
                            if (!isRunning) {
                                totalSeconds = secs
                                remainingSeconds = secs
                            }
                        },
                        selected = totalSeconds == secs && !isRunning,
                        enabled = !isRunning
                    ) {
                        Text("${secs}s")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Circular progress indicator with countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val activeColor = when {
                    finished -> MaterialTheme.colorScheme.error
                    remainingSeconds <= 10 && isRunning -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }

                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val diameter = size.minDimension
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Time text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (finished) "Done!" else timeText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (finished) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (!finished) {
                        Text(
                            text = "remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset button
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        remainingSeconds = totalSeconds
                    },
                    modifier = Modifier.weight(1f),
                    enabled = remainingSeconds != totalSeconds || finished
                ) {
                    Text("Reset")
                }

                // Start/Pause button
                Button(
                    onClick = {
                        if (finished) {
                            remainingSeconds = totalSeconds
                            isRunning = true
                        } else {
                            isRunning = !isRunning
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary
                                         else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        when {
                            finished -> "Restart"
                            isRunning -> "Pause"
                            else -> "Start"
                        }
                    )
                }
            }
        }
    }
}
