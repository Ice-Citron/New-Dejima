package com.dejima.flashcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Flashcard(
    val question: String,
    val answer: String,
    var mastered: Boolean = false
)

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
                    FlashcardApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardApp() {
    val cards = remember {
        mutableStateListOf(
            Flashcard("What is the capital of France?", "Paris"),
            Flashcard("What is the capital of Japan?", "Tokyo"),
            Flashcard("What is the capital of Brazil?", "Brasília"),
            Flashcard("What is the capital of Australia?", "Canberra"),
            Flashcard("What is the capital of Egypt?", "Cairo")
        )
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }

    val masteredCount = cards.count { it.mastered }
    val totalCount = cards.size
    val progress = if (totalCount > 0) masteredCount.toFloat() / totalCount.toFloat() else 0f
    val unmasteredIndices = cards.indices.filter { !cards[it].mastered }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Flashcard Study") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$masteredCount / $totalCount mastered",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showComplete || unmasteredIndices.isEmpty() && totalCount > 0 && masteredCount == totalCount) {
                // All mastered celebration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "All cards mastered!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(onClick = {
                                cards.forEach { it.mastered = false }
                                // Force recomposition
                                val temp = cards.toList()
                                cards.clear()
                                cards.addAll(temp.map { it.copy(mastered = false) })
                                currentIndex = 0
                                isFlipped = false
                                showComplete = false
                            }) {
                                Text("Study Again")
                            }
                        }
                    }
                }
            } else if (totalCount == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No cards yet!\nTap + to add one.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Ensure currentIndex is valid and points to unmastered card
                val safeIndex = if (unmasteredIndices.isNotEmpty()) {
                    if (currentIndex in unmasteredIndices) currentIndex
                    else unmasteredIndices.first()
                } else 0
                val card = cards[safeIndex]

                // Card counter
                val unmasteredPosition = unmasteredIndices.indexOf(safeIndex) + 1
                Text(
                    "Card $unmasteredPosition of ${unmasteredIndices.size} remaining",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Flashcard with flip animation
                FlipCard(
                    card = card,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped }
                )

                Text(
                    if (isFlipped) "Showing: Answer" else "Tap card to reveal answer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Still learning — move to next unmastered
                            isFlipped = false
                            val nextUnmastered = unmasteredIndices
                                .filter { it != safeIndex }
                            if (nextUnmastered.isNotEmpty()) {
                                currentIndex = nextUnmastered.first()
                            }
                            // If only one unmastered left, stay on it
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📖 Still Learning")
                    }
                    Button(
                        onClick = {
                            // Mark as mastered
                            val idx = safeIndex
                            cards[idx] = cards[idx].copy(mastered = true)
                            isFlipped = false
                            val remaining = cards.indices.filter { !cards[it].mastered }
                            if (remaining.isEmpty()) {
                                showComplete = true
                            } else {
                                currentIndex = remaining.first()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("✅ Know It")
                    }
                }
            }
        }
    }

    // Add card dialog
    if (showAddDialog) {
        AddCardDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { question, answer ->
                cards.add(Flashcard(question, answer))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun FlipCard(card: Flashcard, isFlipped: Boolean, onFlip: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flip"
    )
    val isFrontVisible = rotation <= 90f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlip
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrontVisible)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Counter-rotate text so it's not mirrored
                    if (!isFrontVisible) rotationY = 180f
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    if (isFrontVisible) "QUESTION" else "ANSWER",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFrontVisible)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (isFrontVisible) card.question else card.answer,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = if (isFrontVisible)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(question.trim(), answer.trim()) },
                enabled = question.isNotBlank() && answer.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
