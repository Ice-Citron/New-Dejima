package com.dejima.cryptodashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.NumberFormat
import java.util.Locale

// ══════════════════════════════════════════════════════
// API MODELS
// ══════════════════════════════════════════════════════

data class CoinMarket(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("market_cap") val marketCap: Long?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("total_volume") val totalVolume: Long?,
    @SerializedName("price_change_percentage_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_1h_in_currency") val priceChange1h: Double?,
    @SerializedName("price_change_percentage_7d_in_currency") val priceChange7d: Double?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    @SerializedName("total_supply") val totalSupply: Double?,
    @SerializedName("ath") val ath: Double?,
    @SerializedName("ath_date") val athDate: String?
)

data class CoinDetail(
    val id: String,
    val symbol: String,
    val name: String,
    val image: CoinImage?,
    val description: CoinDescription?,
    @SerializedName("market_data") val marketData: MarketData?
)

data class CoinImage(val large: String?, val small: String?)

data class CoinDescription(val en: String?)

data class MarketData(
    @SerializedName("current_price") val currentPrice: Map<String, Double>?,
    @SerializedName("market_cap") val marketCap: Map<String, Double>?,
    @SerializedName("total_volume") val totalVolume: Map<String, Double>?,
    @SerializedName("price_change_percentage_1h_in_currency") val priceChange1h: Map<String, Double>?,
    @SerializedName("price_change_percentage_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_7d") val priceChange7d: Double?,
    @SerializedName("circulating_supply") val circulatingSupply: Double?,
    @SerializedName("total_supply") val totalSupply: Double?,
    @SerializedName("ath") val ath: Map<String, Double>?,
    @SerializedName("ath_date") val athDate: Map<String, String>?
)

data class TrendingResponse(val coins: List<TrendingCoinItem>?)
data class TrendingCoinItem(val item: TrendingCoin?)
data class TrendingCoin(
    val id: String,
    val name: String,
    val symbol: String,
    @SerializedName("large") val image: String?,
    @SerializedName("market_cap_rank") val marketCapRank: Int?,
    @SerializedName("price_btc") val priceBtc: Double?,
    val score: Int?
)

// ══════════════════════════════════════════════════════
// RETROFIT API
// ══════════════════════════════════════════════════════

interface CoinGeckoApi {
    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "1h,24h,7d"
    ): List<CoinMarket>

    @GET("coins/{id}")
    suspend fun getCoinDetail(
        @Path("id") id: String,
        @Query("localization") localization: Boolean = false,
        @Query("tickers") tickers: Boolean = false,
        @Query("community_data") communityData: Boolean = false,
        @Query("developer_data") developerData: Boolean = false
    ): CoinDetail

    @GET("search/trending")
    suspend fun getTrending(): TrendingResponse
}

object RetrofitClient {
    val api: CoinGeckoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }
}

// ══════════════════════════════════════════════════════
// VIEWMODEL
// ══════════════════════════════════════════════════════

class CryptoViewModel : ViewModel() {
    private val api = RetrofitClient.api

    private val _markets = MutableStateFlow<List<CoinMarket>>(emptyList())
    val markets: StateFlow<List<CoinMarket>> = _markets.asStateFlow()

    private val _trending = MutableStateFlow<List<TrendingCoin>>(emptyList())
    val trending: StateFlow<List<TrendingCoin>> = _trending.asStateFlow()

    private val _coinDetail = MutableStateFlow<CoinDetail?>(null)
    val coinDetail: StateFlow<CoinDetail?> = _coinDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        loadMarkets()
        loadTrending()
    }

    fun loadMarkets() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _markets.value = api.getMarkets()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load market data"
            }
            _isLoading.value = false
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            try {
                val response = api.getTrending()
                _trending.value = response.coins?.mapNotNull { it.item } ?: emptyList()
            } catch (_: Exception) { }
        }
    }

    fun loadCoinDetail(coinId: String) {
        viewModelScope.launch {
            _isDetailLoading.value = true
            _coinDetail.value = null
            try {
                _coinDetail.value = api.getCoinDetail(coinId)
            } catch (_: Exception) { }
            _isDetailLoading.value = false
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }
}

// ══════════════════════════════════════════════════════
// FORMATTERS
// ══════════════════════════════════════════════════════

fun formatPrice(price: Double?): String {
    if (price == null) return "N/A"
    return if (price >= 1.0) {
        val fmt = NumberFormat.getCurrencyInstance(Locale.US)
        fmt.maximumFractionDigits = 2
        fmt.minimumFractionDigits = 2
        fmt.format(price)
    } else {
        "$${String.format(Locale.US, "%.8f", price).trimEnd('0').trimEnd('.')}"
    }
}

fun formatLargeNumber(value: Double?): String {
    if (value == null) return "N/A"
    return when {
        value >= 1_000_000_000_000 -> String.format(Locale.US, "$%.2fT", value / 1_000_000_000_000)
        value >= 1_000_000_000 -> String.format(Locale.US, "$%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format(Locale.US, "$%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "$%.2fK", value / 1_000)
        else -> String.format(Locale.US, "$%.2f", value)
    }
}

fun formatPercent(value: Double?): String {
    if (value == null) return "N/A"
    val sign = if (value >= 0) "+" else ""
    return "$sign${String.format(Locale.US, "%.2f", value)}%"
}

fun formatSupply(value: Double?): String {
    if (value == null) return "N/A"
    return when {
        value >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "%.2fK", value / 1_000)
        else -> String.format(Locale.US, "%.0f", value)
    }
}

fun stripHtml(html: String?): String {
    if (html == null) return ""
    return html.replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace("\r\n", "\n")
        .trim()
}

val GainGreen = Color(0xFF4CAF50)
val LossRed = Color(0xFFE53935)

fun changeColor(value: Double?): Color {
    if (value == null) return Color.Gray
    return if (value >= 0) GainGreen else LossRed
}

// ══════════════════════════════════════════════════════
// ACTIVITY
// ══════════════════════════════════════════════════════

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: CryptoViewModel = viewModel()
            val isDark by vm.isDarkTheme.collectAsState()
            val colorScheme = if (isDark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CryptoApp(vm)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// NAVIGATION
// ══════════════════════════════════════════════════════

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Market : Screen("market", "Market", Icons.Default.TrendingUp)
    data object Trending : Screen("trending", "Trending", Icons.Default.Star)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun CryptoApp(vm: CryptoViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "market"
    val bottomTabs = listOf(Screen.Market, Screen.Trending, Screen.Settings)
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo("market") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "market",
            modifier = Modifier.padding(padding)
        ) {
            composable("market") { MarketScreen(vm, navController) }
            composable("trending") { TrendingScreen(vm, navController) }
            composable("settings") { SettingsScreen(vm) }
            composable("detail/{coinId}") { entry ->
                val coinId = entry.arguments?.getString("coinId") ?: ""
                DetailScreen(vm, coinId, navController)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// MARKET SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(vm: CryptoViewModel, nav: NavHostController) {
    val coins by vm.markets.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val query by vm.searchQuery.collectAsState()

    val filtered = if (query.isBlank()) coins else coins.filter {
        it.name.contains(query, ignoreCase = true) ||
        it.symbol.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Crypto Market") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { vm.updateSearch(it) },
            placeholder = { Text("Search coins...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        when {
            error != null && coins.isEmpty() -> {
                ErrorState(message = error!!, onRetry = { vm.loadMarkets() })
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { vm.loadMarkets() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filtered.isEmpty() && !isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No coins found", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered, key = { it.id }) { coin ->
                                CoinRow(coin = coin, onClick = { nav.navigate("detail/${coin.id}") })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// COIN ROW
// ══════════════════════════════════════════════════════

@Composable
fun CoinRow(coin: CoinMarket, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                "${coin.marketCapRank ?: "-"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )

            // Icon
            AsyncImage(
                model = coin.image,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(10.dp))

            // Name & Symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    coin.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    coin.symbol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Price & Change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatPrice(coin.currentPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val change = coin.priceChange24h
                Text(
                    formatPercent(change),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor(change)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// DETAIL SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: CryptoViewModel, coinId: String, nav: NavHostController) {
    val detail by vm.coinDetail.collectAsState()
    val isLoading by vm.isDetailLoading.collectAsState()
    // Also find this coin in market list for 1h/7d data
    val markets by vm.markets.collectAsState()
    val marketCoin = markets.find { it.id == coinId }

    LaunchedEffect(coinId) {
        vm.loadCoinDetail(coinId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: coinId.replaceFirstChar { it.uppercase() }) },
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
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (detail != null) {
            val d = detail!!
            val md = d.marketData
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: icon + name + price
                item(key = "header") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = d.image?.large ?: d.image?.small,
                            contentDescription = d.name,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${d.name} (${d.symbol.uppercase()})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatPrice(md?.currentPrice?.get("usd")),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Price change chips
                item(key = "changes") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val change1h = md?.priceChange1h?.get("usd") ?: marketCoin?.priceChange1h
                        val change24h = md?.priceChange24h ?: marketCoin?.priceChange24h
                        val change7d = md?.priceChange7d ?: marketCoin?.priceChange7d
                        ChangeChip("1h", change1h)
                        ChangeChip("24h", change24h)
                        ChangeChip("7d", change7d)
                    }
                }

                // Market stats
                item(key = "stats") {
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
                                "Market Stats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatRow("Market Cap", formatLargeNumber(md?.marketCap?.get("usd")))
                            StatRow("24h Volume", formatLargeNumber(md?.totalVolume?.get("usd")))
                            StatRow("Circulating Supply", formatSupply(md?.circulatingSupply))
                            StatRow("Total Supply", formatSupply(md?.totalSupply))
                            HorizontalDivider()
                            StatRow("All-Time High", formatPrice(md?.ath?.get("usd")))
                            val athDateRaw = md?.athDate?.get("usd")
                            val athDateClean = athDateRaw?.take(10) ?: "N/A"
                            StatRow("ATH Date", athDateClean)
                        }
                    }
                }

                // Description
                val desc = stripHtml(d.description?.en)
                if (desc.isNotBlank()) {
                    item(key = "desc") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "About",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    desc.take(1000) + if (desc.length > 1000) "…" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            ErrorState(
                message = "Could not load coin details",
                onRetry = { vm.loadCoinDetail(coinId) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun ChangeChip(label: String, value: Double?) {
    val color = changeColor(value)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                formatPercent(value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ══════════════════════════════════════════════════════
// TRENDING SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(vm: CryptoViewModel, nav: NavHostController) {
    val trending by vm.trending.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("🔥 Trending") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { vm.loadTrending() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (trending.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No trending data", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { vm.loadTrending() }) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(trending, key = { it.id }) { coin ->
                        TrendingRow(coin = coin, onClick = { nav.navigate("detail/${coin.id}") })
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingRow(coin: TrendingCoin, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score rank
            Text(
                "#${(coin.score ?: 0) + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

            // Icon
            AsyncImage(
                model = coin.image,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(10.dp))

            // Name & Symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    coin.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    coin.symbol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rank badge
            if (coin.marketCapRank != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "Rank #${coin.marketCapRank}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// SETTINGS SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: CryptoViewModel) {
    val isDark by vm.isDarkTheme.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Settings") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme toggle
            item(key = "theme") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Dark Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isDark) "Dark mode enabled" else "Light mode enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { vm.toggleTheme() }
                        )
                    }
                }
            }

            // App info
            item(key = "info") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "App Info",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        StatRow("App Name", "Crypto Dashboard")
                        Spacer(Modifier.height(6.dp))
                        StatRow("Version", "1.0.0")
                        Spacer(Modifier.height(6.dp))
                        StatRow("Build", "Dejima Labs")
                    }
                }
            }

            // Attribution
            item(key = "attribution") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🦎", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Powered by CoinGecko API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Free cryptocurrency data API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// SHARED COMPONENTS
// ══════════════════════════════════════════════════════

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "⚠️",
                fontSize = 48.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
