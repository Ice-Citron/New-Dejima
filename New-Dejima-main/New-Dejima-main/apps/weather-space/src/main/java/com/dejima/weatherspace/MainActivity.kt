package com.dejima.weatherspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ══════════════════════════════════════════════════════════
//  WEATHER API MODELS  (Open-Meteo)
// ══════════════════════════════════════════════════════════

data class WeatherResponse(
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("relative_humidity_2m") val humidity: Int?,
    @SerializedName("wind_speed_10m") val windSpeed: Double?,
    @SerializedName("weather_code") val weatherCode: Int?
)

data class HourlyWeather(
    val time: List<String>?,
    @SerializedName("temperature_2m") val temperature: List<Double>?,
    @SerializedName("weather_code") val weatherCode: List<Int>?
)

data class DailyWeather(
    val time: List<String>?,
    @SerializedName("weather_code") val weatherCode: List<Int>?,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>?,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>?,
    val sunrise: List<String>?,
    val sunset: List<String>?
)

// ══════════════════════════════════════════════════════════
//  NASA APOD MODELS
// ══════════════════════════════════════════════════════════

data class ApodResponse(
    val title: String?,
    val date: String?,
    val explanation: String?,
    val url: String?,
    val hdurl: String?,
    @SerializedName("media_type") val mediaType: String?
)

// ══════════════════════════════════════════════════════════
//  RETROFIT SERVICES
// ══════════════════════════════════════════════════════════

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double = 48.8566,
        @Query("longitude") lon: Double = 2.3522,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): WeatherResponse
}

interface NasaApodApi {
    @GET("planetary/apod")
    suspend fun getToday(
        @Query("api_key") apiKey: String = "YOUR_NASA_API_KEY" // Get free key at https://api.nasa.gov
    ): ApodResponse

    @GET("planetary/apod")
    suspend fun getRandom(
        @Query("api_key") apiKey: String = "YOUR_NASA_API_KEY", // Get free key at https://api.nasa.gov
        @Query("count") count: Int = 7
    ): List<ApodResponse>
}

object ApiClients {
    val weather: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }
    val nasa: NasaApodApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.nasa.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NasaApodApi::class.java)
    }
}

// ══════════════════════════════════════════════════════════
//  HELPERS
// ══════════════════════════════════════════════════════════

fun weatherEmoji(code: Int?): String = when (code) {
    null -> "❓"
    0 -> "☀️"
    in 1..3 -> "⛅"
    in 45..48 -> "🌫️"
    in 51..55 -> "🌧️"
    in 61..65 -> "🌧️"
    in 71..77 -> "❄️"
    in 80..82 -> "🌦️"
    in 95..99 -> "⛈️"
    else -> "🌤️"
}

fun weatherLabel(code: Int?): String = when (code) {
    null -> "Unknown"
    0 -> "Clear sky"
    in 1..3 -> "Partly cloudy"
    in 45..48 -> "Fog"
    in 51..55 -> "Drizzle"
    in 61..65 -> "Rain"
    in 71..77 -> "Snow"
    in 80..82 -> "Rain showers"
    in 95..99 -> "Thunderstorm"
    else -> "Cloudy"
}

fun formatTemp(temp: Double?): String {
    if (temp == null) return "--°"
    return "${temp.toInt()}°"
}

fun extractTime(isoString: String?): String {
    if (isoString == null) return "--:--"
    // "2026-02-21T07:30" → "07:30"
    return isoString.substringAfter("T", "--:--")
}

// ══════════════════════════════════════════════════════════
//  VIEWMODEL
// ══════════════════════════════════════════════════════════

class AppViewModel : ViewModel() {
    private val weatherApi = ApiClients.weather
    private val nasaApi = ApiClients.nasa

    // Weather
    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather.asStateFlow()
    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()
    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    // APOD today
    private val _apodToday = MutableStateFlow<ApodResponse?>(null)
    val apodToday: StateFlow<ApodResponse?> = _apodToday.asStateFlow()
    private val _apodLoading = MutableStateFlow(false)
    val apodLoading: StateFlow<Boolean> = _apodLoading.asStateFlow()

    // Gallery
    private val _gallery = MutableStateFlow<List<ApodResponse>>(emptyList())
    val gallery: StateFlow<List<ApodResponse>> = _gallery.asStateFlow()
    private val _galleryLoading = MutableStateFlow(false)
    val galleryLoading: StateFlow<Boolean> = _galleryLoading.asStateFlow()

    init {
        loadWeather()
        loadApodToday()
        loadGallery()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _weatherLoading.value = true
            _weatherError.value = null
            try {
                _weather.value = weatherApi.getForecast()
            } catch (e: Exception) {
                _weatherError.value = e.message ?: "Weather load failed"
            }
            _weatherLoading.value = false
        }
    }

    fun loadApodToday() {
        viewModelScope.launch {
            _apodLoading.value = true
            try {
                _apodToday.value = nasaApi.getToday()
            } catch (_: Exception) { }
            _apodLoading.value = false
        }
    }

    fun loadGallery() {
        viewModelScope.launch {
            _galleryLoading.value = true
            try {
                _gallery.value = nasaApi.getRandom()
            } catch (_: Exception) { }
            _galleryLoading.value = false
        }
    }
}

// ══════════════════════════════════════════════════════════
//  ACTIVITY
// ══════════════════════════════════════════════════════════

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
                    val vm: AppViewModel = viewModel()
                    WeatherSpaceApp(vm)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  NAVIGATION
// ══════════════════════════════════════════════════════════

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Weather : Tab("weather", "Weather", Icons.Default.Cloud)
    data object Space : Tab("space", "Space", Icons.Default.RocketLaunch)
    data object Gallery : Tab("gallery", "Gallery", Icons.Default.Collections)
}

@Composable
fun WeatherSpaceApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "weather"
    val tabs = listOf(Tab.Weather, Tab.Space, Tab.Gallery)
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo("weather") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "weather", modifier = Modifier.padding(padding)) {
            composable("weather") { WeatherScreen(vm) }
            composable("space") { SpaceScreen(vm, nav) }
            composable("gallery") { GalleryScreen(vm, nav) }
            composable("fullimage/{url}") { entry ->
                val url = entry.arguments?.getString("url") ?: ""
                FullImageScreen(url, nav)
            }
            composable("apoddetail/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                ApodDetailScreen(vm, index, nav)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  WEATHER SCREEN
// ══════════════════════════════════════════════════════════

val SkyBlueTop = Color(0xFF4FC3F7)
val SkyBlueBot = Color(0xFF0288D1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(vm: AppViewModel) {
    val data by vm.weather.collectAsState()
    val loading by vm.weatherLoading.collectAsState()
    val error by vm.weatherError.collectAsState()

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { vm.loadWeather() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            error != null && data == null -> ErrorBox(error!!) { vm.loadWeather() }
            data == null && loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> {
                val w = data ?: return@PullToRefreshBox
                val cur = w.current
                val daily = w.daily

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Hero card
                    item(key = "hero") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(SkyBlueTop, SkyBlueBot)))
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📍 Paris", color = Color.White, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    weatherEmoji(cur?.weatherCode),
                                    fontSize = 72.sp
                                )
                                Text(
                                    formatTemp(cur?.temperature),
                                    color = Color.White,
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    weatherLabel(cur?.weatherCode),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    // Info chips row
                    item(key = "info") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoChip(
                                "💧", "Humidity",
                                "${cur?.humidity ?: "--"}%",
                                Modifier.weight(1f)
                            )
                            InfoChip(
                                "💨", "Wind",
                                "${cur?.windSpeed?.toInt() ?: "--"} km/h",
                                Modifier.weight(1f)
                            )
                        }
                    }

                    // Sunrise / sunset
                    item(key = "sun") {
                        val sunrise = extractTime(daily?.sunrise?.firstOrNull())
                        val sunset = extractTime(daily?.sunset?.firstOrNull())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoChip("🌅", "Sunrise", sunrise, Modifier.weight(1f))
                            InfoChip("🌇", "Sunset", sunset, Modifier.weight(1f))
                        }
                    }

                    // 7-day header
                    item(key = "fheader") {
                        Text(
                            "7-Day Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    // 7-day horizontal scroll
                    item(key = "forecast") {
                        val days = daily?.time ?: emptyList()
                        val codes = daily?.weatherCode ?: emptyList()
                        val highs = daily?.tempMax ?: emptyList()
                        val lows = daily?.tempMin ?: emptyList()
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(days) { i, dateStr ->
                                val date = try { LocalDate.parse(dateStr) } catch (_: Exception) { null }
                                val dayName = if (i == 0) "Today" else
                                    date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "?"
                                ForecastCard(
                                    day = dayName,
                                    emoji = weatherEmoji(codes.getOrNull(i)),
                                    high = formatTemp(highs.getOrNull(i)),
                                    low = formatTemp(lows.getOrNull(i))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ForecastCard(day: String, emoji: String, high: String, low: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(day, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(high, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(low, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ══════════════════════════════════════════════════════════
//  SPACE SCREEN  (Today's APOD)
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(vm: AppViewModel, nav: NavHostController) {
    val apod by vm.apodToday.collectAsState()
    val loading by vm.apodLoading.collectAsState()

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { vm.loadApodToday() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            apod == null && loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            apod == null -> ErrorBox("Could not load NASA APOD") { vm.loadApodToday() }
            else -> {
                val a = apod!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Top bar
                    item(key = "bar") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🚀 Space Picture of the Day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Image
                    if (a.mediaType == "image" && a.url != null) {
                        item(key = "img") {
                            AsyncImage(
                                model = a.url,
                                contentDescription = a.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 220.dp, max = 360.dp)
                                    .clickable {
                                        val encoded = java.net.URLEncoder.encode(
                                            a.hdurl ?: a.url, "UTF-8"
                                        )
                                        nav.navigate("fullimage/$encoded")
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Title + date
                    item(key = "title") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                a.title ?: "Untitled",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                a.date ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Explanation
                    if (!a.explanation.isNullOrBlank()) {
                        item(key = "explain") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Text(
                                    a.explanation,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    // Attribution
                    item(key = "attr") {
                        Text(
                            "Powered by NASA Open APIs",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  FULL IMAGE SCREEN
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullImageScreen(encodedUrl: String, nav: NavHostController) {
    val url = try { java.net.URLDecoder.decode(encodedUrl, "UTF-8") } catch (_: Exception) { encodedUrl }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full Image") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Full image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
//  GALLERY SCREEN
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(vm: AppViewModel, nav: NavHostController) {
    val items by vm.gallery.collectAsState()
    val loading by vm.galleryLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🌌 Space Gallery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { vm.loadGallery() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (items.isEmpty() && !loading) {
                ErrorBox("No gallery images") { vm.loadGallery() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(items) { index, apod ->
                        GalleryCard(apod = apod, onClick = {
                            nav.navigate("apoddetail/$index")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryCard(apod: ApodResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            if (apod.mediaType == "image" && apod.url != null) {
                AsyncImage(
                    model = apod.url,
                    contentDescription = apod.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    apod.title ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    apod.date ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  APOD DETAIL SCREEN  (from gallery)
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApodDetailScreen(vm: AppViewModel, index: Int, nav: NavHostController) {
    val items by vm.gallery.collectAsState()
    val apod = items.getOrNull(index)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(apod?.title ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (apod == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Image not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (apod.mediaType == "image" && apod.url != null) {
                    item(key = "img") {
                        AsyncImage(
                            model = apod.url,
                            contentDescription = apod.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 360.dp)
                                .clickable {
                                    val enc = java.net.URLEncoder.encode(
                                        apod.hdurl ?: apod.url, "UTF-8"
                                    )
                                    nav.navigate("fullimage/$enc")
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item(key = "title") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            apod.title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            apod.date ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!apod.explanation.isNullOrBlank()) {
                    item(key = "explain") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Text(
                                apod.explanation,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  SHARED ERROR
// ══════════════════════════════════════════════════════════

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
