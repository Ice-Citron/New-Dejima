package com.dejima.moodtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MoodEntry(
    val emoji: String,
    val label: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class Mood(val emoji: String, val label: String) {
    HAPPY("😀", "Happy"),
    SAD("😢", "Sad"),
    NEUTRAL("😐", "Neutral"),
    EXCITED("🤩", "Excited"),
    TIRED("😴", "Tired")
}

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
                    MoodTrackerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTrackerScreen() {
    val entries = remember { mutableStateListOf<MoodEntry>() }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a") }
    var lastTapped by remember { mutableStateOf<Mood?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mood Tracker") },
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
        ) {
            // Mood selector card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "How are you feeling?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Mood.entries.forEach { mood ->
                            MoodButton(
                                mood = mood,
                                isSelected = lastTapped == mood,
                                onClick = {
                                    lastTapped = mood
                                    entries.add(
                                        0,
                                        MoodEntry(mood.emoji, mood.label)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // History header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entries.isNotEmpty()) {
                    Text(
                        "${entries.size} ${if (entries.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // History list or empty state
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No entries yet\nTap a mood to start tracking!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        entries,
                        key = { index, entry -> "$index-${entry.timestamp}" }
                    ) { index, entry ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn()
                        ) {
                            MoodEntryCard(entry, formatter, index == 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodButton(mood: Mood, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.size(60.dp),
            contentPadding = PaddingValues(0.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Text(mood.emoji, fontSize = 28.sp, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            mood.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MoodEntryCard(entry: MoodEntry, formatter: DateTimeFormatter, isLatest: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(entry.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    entry.timestamp.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLatest) {
                AssistChip(
                    onClick = {},
                    label = { Text("Latest") },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}
