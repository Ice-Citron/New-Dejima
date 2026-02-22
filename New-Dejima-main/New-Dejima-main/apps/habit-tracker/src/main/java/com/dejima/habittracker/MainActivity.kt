package com.dejima.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class Habit(
    val id: Long,
    val name: String,
    val emoji: String,
    val completedDays: Set<LocalDate> = emptySet()
)

fun Habit.streakEndingOn(date: LocalDate): Int {
    var count = 0
    var d = date
    while (d in completedDays) {
        count++
        d = d.minusDays(1)
    }
    return count
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
                    HabitTrackerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen() {
    val today = remember { LocalDate.now() }
    val weekStart = remember { today.with(DayOfWeek.MONDAY) }
    val weekDays = remember { (0L..6L).map { weekStart.plusDays(it) } }
    val dayLabels = remember {
        weekDays.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    var nextId by remember { mutableLongStateOf(4L) }
    val habits = remember {
        mutableStateListOf(
            Habit(1, "Exercise", "🏋️"),
            Habit(2, "Read", "📚"),
            Habit(3, "Meditate", "🧘")
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Int?>(null) }

    val todayDone by remember {
        derivedStateOf { habits.count { today in it.completedDays } }
    }
    val todayTotal by remember { derivedStateOf { habits.size } }
    val todayProgress by remember {
        derivedStateOf {
            if (todayTotal > 0) todayDone.toFloat() / todayTotal else 0f
        }
    }
    val allDoneToday by remember {
        derivedStateOf { todayTotal > 0 && todayDone == todayTotal }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Habit Tracker") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today's progress card
            item(key = "progress") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$todayDone / $todayTotal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { todayProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            color = if (allDoneToday) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Motivational banner
            item(key = "banner") {
                AnimatedVisibility(
                    visible = allDoneToday,
                    enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎉", fontSize = 36.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "All habits done!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "You're crushing it! Keep the streak alive! 🔥",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Week day header row
            item(key = "header") {
                WeekHeader(dayLabels = dayLabels, weekDays = weekDays, today = today)
            }

            // Habit rows
            itemsIndexed(habits, key = { _, h -> h.id }) { index, habit ->
                HabitRow(
                    habit = habit,
                    weekDays = weekDays,
                    today = today,
                    onToggleDay = { date ->
                        val current = habits[index]
                        val newDays = current.completedDays.toMutableSet()
                        if (date in newDays) newDays.remove(date) else newDays.add(date)
                        habits[index] = current.copy(completedDays = newDays.toSet())
                    },
                    onDelete = { deleteTarget = index }
                )
            }

            // Empty state
            if (habits.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No habits yet!\nTap + to create one.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, emoji ->
                habits.add(Habit(nextId++, name, emoji))
                showAddDialog = false
            }
        )
    }

    deleteTarget?.let { idx ->
        val habit = habits.getOrNull(idx)
        if (habit != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete Habit?") },
                text = { Text("Remove \"${habit.emoji} ${habit.name}\"? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            habits.removeAt(idx)
                            deleteTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun WeekHeader(dayLabels: List<String>, weekDays: List<LocalDate>, today: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(100.dp))
        weekDays.forEachIndexed { i, date ->
            val isToday = date == today
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    dayLabels[i].take(2),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${date.dayOfMonth}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(44.dp))
    }
}

@Composable
fun HabitRow(
    habit: Habit,
    weekDays: List<LocalDate>,
    today: LocalDate,
    onToggleDay: (LocalDate) -> Unit,
    onDelete: () -> Unit
) {
    val streak = habit.streakEndingOn(today)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit name + emoji
            Column(
                modifier = Modifier
                    .width(92.dp)
                    .clickable { onDelete() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(habit.emoji, fontSize = 22.sp)
                Text(
                    habit.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Day circles
            weekDays.forEach { date ->
                val done = date in habit.completedDays
                val isFuture = date.isAfter(today)
                val bgColor = when {
                    done -> MaterialTheme.colorScheme.primary
                    isFuture -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(enabled = !isFuture) { onToggleDay(date) },
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Text(
                            "✓",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Streak badge
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (streak > 0) {
                    Text("🔥", fontSize = 14.sp)
                    Text(
                        "$streak",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("") }
    val emojiOptions = listOf(
        "💪", "📚", "🧘", "🏃", "💧", "🎨",
        "🎵", "✍️", "🥗", "😴", "🧹", "💰"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Pick an icon:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in emojiOptions.chunked(6)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { emoji ->
                                val isSelected = emoji == selectedEmoji
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name.trim(), selectedEmoji) },
                enabled = name.isNotBlank() && selectedEmoji.isNotEmpty()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
