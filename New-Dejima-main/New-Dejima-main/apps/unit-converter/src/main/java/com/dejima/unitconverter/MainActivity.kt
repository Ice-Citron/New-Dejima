package com.dejima.unitconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

enum class ConversionType(
    val label: String,
    val icon: String,
    val fromUnit: String,
    val toUnit: String,
    val convert: (Double) -> Double,
    val reverseConvert: (Double) -> Double
) {
    KM_MILES(
        label = "Distance",
        icon = "📏",
        fromUnit = "Kilometers",
        toUnit = "Miles",
        convert = { it * 0.621371 },
        reverseConvert = { it / 0.621371 }
    ),
    KG_LBS(
        label = "Weight",
        icon = "⚖️",
        fromUnit = "Kilograms",
        toUnit = "Pounds",
        convert = { it * 2.20462 },
        reverseConvert = { it / 2.20462 }
    ),
    CELSIUS_FAHRENHEIT(
        label = "Temperature",
        icon = "🌡️",
        fromUnit = "Celsius",
        toUnit = "Fahrenheit",
        convert = { it * 9.0 / 5.0 + 32.0 },
        reverseConvert = { (it - 32.0) * 5.0 / 9.0 }
    )
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
                    UnitConverterScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen() {
    val focusManager = LocalFocusManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var isReversed by remember { mutableStateOf(false) }

    val conversionType = ConversionType.entries[selectedTab]
    val inputValue = inputText.toDoubleOrNull()
    val resultValue = inputValue?.let {
        if (isReversed) conversionType.reverseConvert(it)
        else conversionType.convert(it)
    }
    val formatter = remember { DecimalFormat("#,##0.####") }

    val fromUnit = if (isReversed) conversionType.toUnit else conversionType.fromUnit
    val toUnit = if (isReversed) conversionType.fromUnit else conversionType.toUnit

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Unit Converter") },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                ConversionType.entries.forEachIndexed { index, type ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            inputText = ""
                            isReversed = false
                        },
                        text = {
                            Text(
                                "${type.icon} ${type.label}",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            // Conversion direction header
            AnimatedContent(
                targetState = conversionType to isReversed,
                label = "header"
            ) { (type, reversed) ->
                val from = if (reversed) type.toUnit else type.fromUnit
                val to = if (reversed) type.fromUnit else type.toUnit
                Text(
                    "$from → $to",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Swap button
            FilledTonalButton(
                onClick = {
                    isReversed = !isReversed
                    inputText = ""
                }
            ) {
                Text("🔄  Swap Direction")
            }

            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal == "-" || newVal == "." || newVal == "-." ||
                        newVal.matches(Regex("^-?\\d*\\.?\\d{0,4}$"))
                    ) {
                        inputText = newVal
                    }
                },
                label = { Text(fromUnit) },
                suffix = { Text(unitAbbreviation(fromUnit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            // Result card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        toUnit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (resultValue != null) formatter.format(resultValue)
                               else "—",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        unitAbbreviation(toUnit),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Quick reference card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Quick Reference",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val references = getQuickReferences(conversionType, isReversed, formatter)
                    references.forEach { ref ->
                        Text(
                            ref,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun unitAbbreviation(unit: String): String = when (unit) {
    "Kilometers" -> "km"
    "Miles" -> "mi"
    "Kilograms" -> "kg"
    "Pounds" -> "lbs"
    "Celsius" -> "°C"
    "Fahrenheit" -> "°F"
    else -> ""
}

private fun getQuickReferences(
    type: ConversionType,
    reversed: Boolean,
    formatter: DecimalFormat
): List<String> {
    val convert = if (reversed) type.reverseConvert else type.convert
    val fromAbbr = unitAbbreviation(if (reversed) type.toUnit else type.fromUnit)
    val toAbbr = unitAbbreviation(if (reversed) type.fromUnit else type.toUnit)
    val samples = when (type) {
        ConversionType.KM_MILES -> if (reversed) listOf(1.0, 5.0, 10.0, 26.2) else listOf(1.0, 5.0, 10.0, 42.195)
        ConversionType.KG_LBS -> if (reversed) listOf(1.0, 10.0, 100.0, 150.0) else listOf(1.0, 10.0, 50.0, 100.0)
        ConversionType.CELSIUS_FAHRENHEIT -> if (reversed) listOf(32.0, 72.0, 98.6, 212.0) else listOf(0.0, 20.0, 37.0, 100.0)
    }
    return samples.map { v ->
        "${formatter.format(v)} $fromAbbr = ${formatter.format(convert(v))} $toAbbr"
    }
}
