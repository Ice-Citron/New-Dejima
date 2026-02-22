package com.dejima.stocktracker

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
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale

// ══════════════════════════════════════════════════════
//  API MODELS
// ══════════════════════════════════════════════════════

data class StockQuote(
    val c: Double?,   // current
    val d: Double?,   // change
    val dp: Double?,  // percent change
    val h: Double?,   // high
    val l: Double?,   // low
    val o: Double?,   // open
    val pc: Double?   // prev close
)

data class CompanyProfile(
    val name: String?,
    val ticker: String?,
    val logo: String?,
    val finnhubIndustry: String?,
    val marketCapitalization: Double?,
    val country: String?,
    val exchange: String?
)

data class NewsArticle(
    val headline: String?,
    val summary: String?,
    val source: String?,
    val url: String?,
    val image: String?,
    val datetime: Long?
)

// Combined data for a watchlist entry
data class StockData(
    val symbol: String,
    val quote: StockQuote? = null,
    val profile: CompanyProfile? = null,
    val isLoading: Boolean = true
)

// ══════════════════════════════════════════════════════
//  RETROFIT
// ══════════════════════════════════════════════════════

private const val API_KEY = "YOUR_FINNHUB_API_KEY" // Get free key at https://finnhub.io

interface FinnhubApi {
    @GET("api/v1/quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String = API_KEY
    ): StockQuote

    @GET("api/v1/stock/profile2")
    suspend fun getProfile(
        @Query("symbol") symbol: String,
        @Query("token") token: String = API_KEY
    ): CompanyProfile

    @GET("api/v1/news")
    suspend fun getNews(
        @Query("category") category: String = "general",
        @Query("token") token: String = API_KEY
    ): List<NewsArticle>
}

object RetrofitClient {
    val api: FinnhubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://finnhub.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApi::class.java)
    }
}

// ══════════════════════════════════════════════════════
//  VIEWMODEL
// ══════════════════════════════════════════════════════

val WATCHLIST_SYMBOLS = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "NVDA", "META", "NFLX")

class StockViewModel : ViewModel() {
    private val api = RetrofitClient.api

    private val _stocks = MutableStateFlow<List<StockData>>(
        WATCHLIST_SYMBOLS.map { StockData(symbol = it) }
    )
    val stocks: StateFlow<List<StockData>> = _stocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _news = MutableStateFlow<List<NewsArticle>>(emptyList())
    val news: StateFlow<List<NewsArticle>> = _news.asStateFlow()

    private val _newsLoading = MutableStateFlow(false)
    val newsLoading: StateFlow<Boolean> = _newsLoading.asStateFlow()

    // Detail
    private val _detailQuote = MutableStateFlow<StockQuote?>(null)
    val detailQuote: StateFlow<StockQuote?> = _detailQuote.asStateFlow()
    private val _detailProfile = MutableStateFlow<CompanyProfile?>(null)
    val detailProfile: StateFlow<CompanyProfile?> = _detailProfile.asStateFlow()
    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    init {
        loadAllStocks()
        loadNews()
    }

    fun loadAllStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            val updated = mutableListOf<StockData>()
            for (symbol in WATCHLIST_SYMBOLS) {
                try {
                    val quote = api.getQuote(symbol)
                    val profile = try { api.getProfile(symbol) } catch (_: Exception) { null }
                    updated.add(StockData(symbol, quote, profile, isLoading = false))
                } catch (_: Exception) {
                    updated.add(StockData(symbol, isLoading = false))
                }
                _stocks.value = buildFullList(updated)
                delay(250) // Rate-limit: ~4 req/sec to stay under 60/min
            }
            _isLoading.value = false
        }
    }

    private fun buildFullList(loaded: List<StockData>): List<StockData> {
        val loadedMap = loaded.associateBy { it.symbol }
        return WATCHLIST_SYMBOLS.map { sym ->
            loadedMap[sym] ?: StockData(sym)
        }
    }

    fun loadDetail(symbol: String) {
        viewModelScope.launch {
            _detailLoading.value = true
            _detailQuote.value = null
            _detailProfile.value = null
            try {
                _detailQuote.value = api.getQuote(symbol)
                _detailProfile.value = api.getProfile(symbol)
            } catch (_: Exception) { }
            _detailLoading.value = false
        }
    }

    fun loadNews() {
        viewModelScope.launch {
            _newsLoading.value = true
            try {
                _news.value = api.getNews()
            } catch (_: Exception) { }
            _newsLoading.value = false
        }
    }
}

// ══════════════════════════════════════════════════════
//  FORMATTERS
// ══════════════════════════════════════════════════════

fun fmtPrice(value: Double?): String {
    if (value == null || value == 0.0) return "--"
    return String.format(Locale.US, "$%,.2f", value)
}

fun fmtChange(value: Double?): String {
    if (value == null) return "--"
    val sign = if (value >= 0) "+" else ""
    return String.format(Locale.US, "%s%.2f", sign, value)
}

fun fmtPct(value: Double?): String {
    if (value == null) return "--"
    val sign = if (value >= 0) "+" else ""
    return String.format(Locale.US, "%s%.2f%%", sign, value)
}

fun fmtMarketCap(value: Double?): String {
    if (value == null) return "N/A"
    // Finnhub returns market cap in millions
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "$%.2fT", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "$%.2fB", value / 1_000)
        value >= 1 -> String.format(Locale.US, "$%.2fM", value)
        else -> String.format(Locale.US, "$%.2f", value)
    }
}

fun timeAgo(epochSec: Long?): String {
    if (epochSec == null) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - epochSec
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        else -> "${diff / 86400}d ago"
    }
}

val GainGreen = Color(0xFF4CAF50)
val LossRed = Color(0xFFE53935)
fun changeColor(v: Double?): Color = if (v != null && v >= 0) GainGreen else LossRed

// ══════════════════════════════════════════════════════
//  ACTIVITY
// ══════════════════════════════════════════════════════

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = dynamicLightColorScheme(this)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: StockViewModel = viewModel()
                    StockApp(vm)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
//  NAVIGATION
// ══════════════════════════════════════════════════════

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Watchlist : Tab("watchlist", "Watchlist", Icons.Default.ShowChart)
    data object News : Tab("news", "News", Icons.Default.Newspaper)
}

@Composable
fun StockApp(vm: StockViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "watchlist"
    val tabs = listOf(Tab.Watchlist, Tab.News)
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
                                        popUpTo("watchlist") { saveState = true }
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
        NavHost(nav, startDestination = "watchlist", modifier = Modifier.padding(padding)) {
            composable("watchlist") { WatchlistScreen(vm, nav) }
            composable("news") { NewsScreen(vm) }
            composable("detail/{symbol}") { entry ->
                val symbol = entry.arguments?.getString("symbol") ?: ""
                DetailScreen(vm, symbol, nav)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
//  WATCHLIST SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(vm: StockViewModel, nav: NavHostController) {
    val stocks by vm.stocks.collectAsState()
    val loading by vm.isLoading.collectAsState()

    val totalValue by remember {
        derivedStateOf {
            stocks.mapNotNull { it.quote?.c }.sum()
        }
    }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("📈 Stock Tracker") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { vm.loadAllStocks() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Portfolio summary
                item(key = "summary") {
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
                                "Watchlist Total",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                fmtPrice(totalValue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${stocks.size} stocks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Stock rows
                items(stocks, key = { it.symbol }) { stock ->
                    StockRow(stock) {
                        nav.navigate("detail/${stock.symbol}")
                    }
                }
            }
        }
    }
}

@Composable
fun StockRow(stock: StockData, onClick: () -> Unit) {
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stock.symbol.take(2),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    stock.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stock.profile?.name ?: stock.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (stock.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        fmtPrice(stock.quote?.c),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fmtChange(stock.quote?.d),
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor(stock.quote?.d)
                        )
                        Text(" ", style = MaterialTheme.typography.labelSmall)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = changeColor(stock.quote?.dp).copy(alpha = 0.12f)
                        ) {
                            Text(
                                fmtPct(stock.quote?.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = changeColor(stock.quote?.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
//  DETAIL SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: StockViewModel, symbol: String, nav: NavHostController) {
    val quote by vm.detailQuote.collectAsState()
    val profile by vm.detailProfile.collectAsState()
    val loading by vm.detailLoading.collectAsState()

    LaunchedEffect(symbol) { vm.loadDetail(symbol) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(symbol) },
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
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: logo + name + price
                item(key = "header") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val logoUrl = profile?.logo
                        if (!logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = profile?.name,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📊", fontSize = 32.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            profile?.name ?: symbol,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            fmtPrice(quote?.c),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                fmtChange(quote?.d),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = changeColor(quote?.d)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = changeColor(quote?.dp).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    fmtPct(quote?.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = changeColor(quote?.dp)
                                )
                            }
                        }
                    }
                }

                // Price grid: Open, High, Low, Prev Close
                item(key = "grid") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Today's Trading",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth()) {
                                GridCell("Open", fmtPrice(quote?.o), Modifier.weight(1f))
                                GridCell("High", fmtPrice(quote?.h), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth()) {
                                GridCell("Low", fmtPrice(quote?.l), Modifier.weight(1f))
                                GridCell("Prev Close", fmtPrice(quote?.pc), Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Company info
                if (profile != null) {
                    item(key = "info") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Company Info",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                InfoRow("Market Cap", fmtMarketCap(profile?.marketCapitalization))
                                InfoRow("Industry", profile?.finnhubIndustry ?: "N/A")
                                InfoRow("Country", profile?.country ?: "N/A")
                                InfoRow("Exchange", profile?.exchange ?: "N/A")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
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
//  NEWS SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(vm: StockViewModel) {
    val news by vm.news.collectAsState()
    val loading by vm.newsLoading.collectAsState()

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("📰 Market News") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { vm.loadNews() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (news.isEmpty() && !loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No news available")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.loadNews() }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(news.take(20), key = { it.hashCode() }) { article ->
                        NewsCard(article)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            // Headline
            Text(
                article.headline ?: "Untitled",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            // Source + time
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    article.source ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    timeAgo(article.datetime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Summary
            if (!article.summary.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
