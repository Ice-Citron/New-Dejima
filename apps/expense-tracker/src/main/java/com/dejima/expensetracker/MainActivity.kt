package com.dejima.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Data ──────────────────────────────────────────────

enum class Category(val label: String, val emoji: String, val color: Long) {
    FOOD("Food", "🍔", 0xFFE57373),
    TRANSPORT("Transport", "🚗", 0xFF64B5F6),
    ENTERTAINMENT("Entertainment", "🎬", 0xFFBA68C8),
    SHOPPING("Shopping", "🛍️", 0xFFFFB74D),
    BILLS("Bills", "📄", 0xFF4DB6AC),
    OTHER("Other", "📦", 0xFF90A4AE)
}

data class Expense(
    val id: Long,
    val category: Category,
    val amount: Double,
    val note: String,
    val dateTime: LocalDateTime
)

// ── Activity ──────────────────────────────────────────

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
                    ExpenseTrackerScreen()
                }
            }
        }
    }
}

// ── Main Screen ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen() {
    var nextId by remember { mutableLongStateOf(1L) }
    val expenses = remember { mutableStateListOf<Expense>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Int?>(null) }

    val totalSpending by remember {
        derivedStateOf { expenses.sumOf { it.amount } }
    }
    val categoryTotals by remember {
        derivedStateOf {
            Category.entries.associateWith { cat ->
                expenses.filter { it.category == cat }.sumOf { it.amount }
            }.filterValues { it > 0.0 }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Expense Tracker") },
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
                Icon(Icons.Default.Add, "Add expense")
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
            // ── Total spending card ──
            item(key = "total") {
                TotalSpendingCard(total = totalSpending)
            }

            // ── Category breakdown ──
            if (categoryTotals.isNotEmpty()) {
                item(key = "breakdown") {
                    CategoryBreakdown(
                        categoryTotals = categoryTotals,
                        totalSpending = totalSpending
                    )
                }

                item(key = "chips") {
                    CategoryChips(categoryTotals = categoryTotals)
                }
            }

            // ── Recent expenses header ──
            item(key = "header") {
                Text(
                    if (expenses.isEmpty()) "No expenses yet" else "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ── Expense list (newest first) ──
            val sorted = expenses.sortedByDescending { it.dateTime }
            itemsIndexed(sorted, key = { _, e -> e.id }) { _, expense ->
                ExpenseCard(
                    expense = expense,
                    onDelete = {
                        val idx = expenses.indexOfFirst { it.id == expense.id }
                        if (idx >= 0) deleteTarget = idx
                    }
                )
            }

            // ── Empty state ──
            if (expenses.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tap + to add your first expense",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // ── Add dialog ──
    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { category, amount, note ->
                expenses.add(
                    Expense(
                        id = nextId++,
                        category = category,
                        amount = amount,
                        note = note,
                        dateTime = LocalDateTime.now()
                    )
                )
                showAddDialog = false
            }
        )
    }

    // ── Delete confirmation ──
    deleteTarget?.let { idx ->
        val expense = expenses.getOrNull(idx)
        if (expense != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete Expense?") },
                text = {
                    Text(
                        "Remove ${expense.category.emoji} " +
                        "€${"%.2f".format(expense.amount)} " +
                        "${expense.category.label}?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            expenses.removeAt(idx)
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

// ── Total Spending Card ───────────────────────────────

@Composable
fun TotalSpendingCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Spending",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "€${"%.2f".format(total)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── Category Breakdown (progress bars) ────────────────

@Composable
fun CategoryBreakdown(
    categoryTotals: Map<Category, Double>,
    totalSpending: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val sorted = categoryTotals.entries.sortedByDescending { it.value }
            sorted.forEach { (category, amount) ->
                val fraction = if (totalSpending > 0) (amount / totalSpending).toFloat() else 0f
                val animFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(600),
                    label = "bar"
                )
                val pct = (fraction * 100).toInt()
                val catColor = Color(category.color)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${category.emoji} ${category.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "€${"%.2f".format(amount)} ($pct%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { animFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = catColor,
                        trackColor = catColor.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

// ── Category Chips (scrollable row) ───────────────────

@Composable
fun CategoryChips(categoryTotals: Map<Category, Double>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categoryTotals.entries.sortedByDescending { it.value }.toList()) { (category, amount) ->
            val catColor = Color(category.color)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = catColor.copy(alpha = 0.15f),
                border = null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )
                    Text(
                        "${category.label} €${"%.2f".format(amount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = catColor.copy(alpha = 1f)
                    )
                }
            }
        }
    }
}

// ── Expense Card (long-press to delete) ───────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCard(expense: Expense, onDelete: () -> Unit) {
    val catColor = Color(expense.category.color)
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(expense.category.emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.category.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (expense.note.isNotBlank()) {
                    Text(
                        expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    expense.dateTime.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Amount
            Text(
                "€${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Delete icon
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Add Expense Dialog ────────────────────────────────

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (Category, Double, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull()
    val isValid = selectedCategory != null && amount != null && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category picker
                Text("Category:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Category.entries.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                val catColor = Color(cat.color)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = cat },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) catColor.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = if (isSelected)
                                        androidx.compose.foundation.BorderStroke(2.dp, catColor)
                                    else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(cat.emoji, fontSize = 20.sp)
                                        Text(
                                            cat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Amount field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) onAdd(selectedCategory!!, amount!!, note.trim())
                },
                enabled = isValid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
