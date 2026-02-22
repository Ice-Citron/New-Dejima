package com.dejima.countryexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.NumberFormat
import java.util.Locale

// ─── THEME ───

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF66D9EF),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5A),
    onPrimaryContainer = Color(0xFFB8EFFF),
    secondary = Color(0xFFF92672),
    onSecondary = Color(0xFF47001A),
    secondaryContainer = Color(0xFF650029),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFA6E22E),
    onTertiary = Color(0xFF223600),
    tertiaryContainer = Color(0xFF334F00),
    onTertiaryContainer = Color(0xFFC1FF83),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF282A2E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
)

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF00677D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EFFF),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFFB9004F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF3E0016),
    tertiary = Color(0xFF456900),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1FF83),
    onTertiaryContainer = Color(0xFF112000),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F)
)

@Composable
fun CountryExplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// ─── API Models ───

data class ApiCountryName(
    @SerializedName("common") val common: String,
    @SerializedName("official") val official: String
)

data class ApiCurrency(
    @SerializedName("name") val name: String?,
    @SerializedName("symbol") val symbol: String?
)

data class ApiCountry(
    @SerializedName("name") val name: ApiCountryName,
    @SerializedName("cca2") val cca2: String,
    @SerializedName("capital") val capital: List<String>?,
    @SerializedName("population") val population: Long,
    @SerializedName("area") val area: Double?,
    @SerializedName("region") val region: String,
    @SerializedName("subregion") val subregion: String?,
    @SerializedName("languages") val languages: Map<String, String>?,
    @SerializedName("currencies") val currencies: Map<String, ApiCurrency>?,
    @SerializedName("borders") val borders: List<String>?
)

// ─── UI Data Model ───

data class CurrencyInfo(val name: String, val symbol: String)

data class Country(
    val name: String,
    val officialName: String,
    val cca2: String,
    val capital: String,
    val population: Long,
    val area: Double,
    val region: String,
    val subregion: String,
    val languages: List<String>,
    val currencies: Map<String, CurrencyInfo>,
    val latlng: Pair<Double, Double>,
    val timezones: List<String>,
    val borders: List<String>,
    val flagUrl: String
)

fun ApiCountry.toCountry(): Country = Country(
    name = name.common,
    officialName = name.official,
    cca2 = cca2,
    capital = capital?.firstOrNull() ?: "N/A",
    population = population,
    area = area ?: 0.0,
    region = region,
    subregion = subregion ?: "",
    languages = languages?.values?.toList() ?: emptyList(),
    currencies = currencies?.mapValues {
        CurrencyInfo(it.value.name ?: "Unknown", it.value.symbol ?: "")
    } ?: emptyMap(),
    latlng = 0.0 to 0.0,
    timezones = emptyList(),
    borders = borders ?: emptyList(),
    flagUrl = "https://flagcdn.com/w320/${cca2.lowercase()}.png"
)

// ─── Retrofit API ───

interface CountriesApi {
    @GET("v3.1/all?fields=name,cca2,capital,population,area,region,subregion,languages,currencies,borders")
    suspend fun getAllCountries(): List<ApiCountry>

    @GET("v3.1/name/{name}?fields=name,cca2,capital,population,area,region,subregion,languages,currencies,borders")
    suspend fun searchByName(@Path("name") name: String): List<ApiCountry>
}

private val retrofit = Retrofit.Builder()
    .baseUrl("https://restcountries.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val countriesApi = retrofit.create(CountriesApi::class.java)

// ─── ViewModel ───

class CountryViewModel : ViewModel() {
    var allCountries by mutableStateOf<List<Country>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var countriesByCode by mutableStateOf<Map<String, Country>>(emptyMap())
        private set
    var regionMap by mutableStateOf<Map<String, List<Country>>>(emptyMap())
        private set
    var favoriteCodes by mutableStateOf<Set<String>>(emptySet())
        private set

    init { loadAllCountries() }

    fun loadAllCountries() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val apiList = countriesApi.getAllCountries()
                val countries = apiList.map { it.toCountry() }.sortedBy { it.name }
                allCountries = countries
                countriesByCode = countries.associateBy { it.cca2 }
                regionMap = countries.groupBy { it.region }.filterKeys { it.isNotBlank() }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load countries"
            }
            isLoading = false
        }
    }

    fun toggleFavorite(code: String) {
        favoriteCodes = if (code in favoriteCodes) favoriteCodes - code else favoriteCodes + code
    }

    fun removeFavorite(code: String) {
        favoriteCodes = favoriteCodes - code
    }
}

// ─── Utils ───

fun formatPopulation(population: Long): String =
    NumberFormat.getNumberInstance(Locale.US).format(population)

fun formatArea(area: Double): String =
    "${NumberFormat.getNumberInstance(Locale.US).format(area)} km²"

// ─── Navigation ───

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Browse : Screen("browse", "Browse", Icons.Outlined.Public, Icons.Filled.Public)
    data object Search : Screen("search", "Search", Icons.Outlined.Search, Icons.Filled.Search)
    data object Regions : Screen("regions", "Regions", Icons.Outlined.Category, Icons.Filled.Category)
    data object Favorites : Screen("favorites", "Favorites", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
}

val bottomNavItems = listOf(Screen.Browse, Screen.Search, Screen.Regions, Screen.Favorites)

// ─── Activity ───

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CountryExplorerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppScaffold()
                }
            }
        }
    }
}

@Composable
fun AppScaffold(vm: CountryViewModel = viewModel()) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(if (selected) screen.selectedIcon else screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Browse.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Browse.route) {
                BrowseScreen(vm) { code -> navController.navigate("detail/$code") }
            }
            composable(Screen.Search.route) {
                SearchScreen(vm) { code -> navController.navigate("detail/$code") }
            }
            composable(Screen.Regions.route) {
                RegionsScreen(vm) { code -> navController.navigate("detail/$code") }
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(vm) { code -> navController.navigate("detail/$code") }
            }
            composable(
                route = "detail/{countryCode}",
                arguments = listOf(navArgument("countryCode") { type = NavType.StringType })
            ) { backStackEntry ->
                val countryCode = backStackEntry.arguments?.getString("countryCode")
                val country = vm.countriesByCode[countryCode]
                if (country != null) {
                    CountryDetailScreen(
                        country = country,
                        isFavorite = country.cca2 in vm.favoriteCodes,
                        allCountries = vm.countriesByCode,
                        onToggleFavorite = { vm.toggleFavorite(country.cca2) },
                        onNavigateBack = { navController.popBackStack() },
                        onBorderClick = { code -> navController.navigate("detail/$code") }
                    )
                } else {
                    ErrorState(message = "Country not found.") { navController.popBackStack() }
                }
            }
        }
    }
}

// ─── Browse Screen ───

@Composable
fun BrowseScreen(vm: CountryViewModel, onCountryClick: (String) -> Unit) {
    Crossfade(targetState = vm.isLoading to vm.error, label = "browse-state") { (loading, err) ->
        when {
            loading -> LoadingState()
            err != null -> ErrorState(message = err) { vm.loadAllCountries() }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(span = { GridItemSpan(2) }) { BrowseHeader() }
                    items(vm.allCountries) { country ->
                        CountryGridCard(country = country, onClick = { onCountryClick(country.cca2) })
                    }
                }
            }
        }
    }
}

// ─── Search Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: CountryViewModel, onCountryClick: (String) -> Unit) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val recentSearches = remember { listOf("Brazil", "Germany", "Japan") }

    val filteredCountries = remember(searchQuery, vm.allCountries) {
        if (searchQuery.isBlank()) emptyList()
        else vm.allCountries.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.capital.contains(searchQuery, ignoreCase = true) ||
            it.region.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            label = { Text("Search by name, capital, or region") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        Crossfade(targetState = searchQuery.isBlank(), label = "search-state") { isBlank ->
            if (isBlank) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Search, contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Search for a country",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text("Recent Searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentSearches.forEach { term ->
                            FilterChip(selected = false, onClick = { searchQuery = term }, label = { Text(term) })
                        }
                    }
                }
            } else {
                if (filteredCountries.isEmpty()) {
                    EmptyState(Icons.Default.Search, "No results", "Try a different search term.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredCountries) { country ->
                            CountryListCard(country = country, onClick = { onCountryClick(country.cca2) })
                        }
                    }
                }
            }
        }
    }
}

// ─── Regions Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionsScreen(vm: CountryViewModel, onCountryClick: (String) -> Unit) {
    var selectedRegion by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Regions of the World", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (vm.isLoading) {
            LoadingState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(all = 24.dp),
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                vm.regionMap.entries.forEach { (regionName, countries) ->
                    item {
                        RegionCard(
                            regionName = regionName,
                            countryCount = countries.size,
                            isSelected = selectedRegion == regionName,
                            onClick = { selectedRegion = if (selectedRegion == regionName) null else regionName }
                        )
                    }
                    if (selectedRegion == regionName) {
                        items(countries) { country ->
                            CountryListCard(
                                country = country,
                                onClick = { onCountryClick(country.cca2) },
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Favorites Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: CountryViewModel, onCountryClick: (String) -> Unit) {
    val favoriteCountries = remember(vm.favoriteCodes, vm.allCountries) {
        vm.allCountries.filter { it.cca2 in vm.favoriteCodes }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Favorite Countries", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Crossfade(targetState = favoriteCountries.isEmpty(), label = "favorites-state") { isEmpty ->
            if (isEmpty) {
                EmptyState(
                    icon = Icons.Outlined.FavoriteBorder,
                    message = "No favorites yet",
                    subMessage = "Tap the star on any country to save it here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    modifier = Modifier.padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteCountries, key = { it.cca2 }) { country ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    vm.removeFavorite(country.cca2)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val bgColor = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(bgColor, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        ) {
                            CountryListCard(country = country, onClick = { onCountryClick(country.cca2) })
                        }
                    }
                }
            }
        }
    }
}

// ─── Country Detail Screen ───

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CountryDetailScreen(
    country: Country,
    isFavorite: Boolean,
    allCountries: Map<String, Country>,
    onToggleFavorite: () -> Unit,
    onNavigateBack: () -> Unit,
    onBorderClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(country.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero flag image with gradient overlay
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomStart
                ) {
                    AsyncImage(
                        model = country.flagUrl,
                        contentDescription = "Flag of ${country.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 400f
                            )
                        )
                    )
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(country.name, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(country.officialName, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Info grid
            item {
                Spacer(Modifier.height(24.dp))
                InfoGrid(country)
            }

            // Map coordinates
            item {
                DetailSection(title = "Location") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Lat: ${"%.4f".format(country.latlng.first)}, Lng: ${"%.4f".format(country.latlng.second)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Languages
            if (country.languages.isNotEmpty()) {
                item {
                    DetailSection(title = "Languages") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            country.languages.forEach { lang ->
                                FilterChip(selected = false, onClick = {}, label = { Text(lang) })
                            }
                        }
                    }
                }
            }

            // Currencies
            if (country.currencies.isNotEmpty()) {
                item {
                    DetailSection(title = "Currencies") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            country.currencies.forEach { (code, info) ->
                                FilterChip(selected = false, onClick = {}, label = { Text("${info.name} (${info.symbol} $code)") })
                            }
                        }
                    }
                }
            }

            // Timezones
            if (country.timezones.isNotEmpty()) {
                item {
                    DetailSection(title = "Timezones") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            country.timezones.forEach { tz ->
                                FilterChip(selected = false, onClick = {}, label = { Text(tz) })
                            }
                        }
                    }
                }
            }

            // Border countries
            if (country.borders.isNotEmpty()) {
                item {
                    DetailSection(title = "Border Countries", usePadding = false) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            items(country.borders) { borderCode ->
                                val borderCountry = allCountries[borderCode]
                                if (borderCountry != null) {
                                    BorderCountryChip(country = borderCountry, onClick = { onBorderClick(borderCountry.cca2) })
                                } else {
                                    FilterChip(selected = false, onClick = {}, label = { Text(borderCode) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Reusable Components ───

@Composable
fun BrowseHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Explore the World", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Text("Discover countries, flags, and cultures.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryGridCard(country: Country, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.animateContentSize()
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(country.flagUrl).crossfade(true).build(),
                contentDescription = "Flag of ${country.name}",
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp)) {
                Text(country.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                InfoRow(icon = Icons.Filled.AccountBalance, text = country.capital)
                Spacer(Modifier.height(4.dp))
                InfoRow(icon = Icons.Filled.Groups, text = formatPopulation(country.population))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListCard(country: Country, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = country.flagUrl,
                contentDescription = "Flag of ${country.name}",
                modifier = Modifier.size(60.dp, 45.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(country.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(country.capital, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                country.region,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionCard(regionName: String, countryCount: Int, isSelected: Boolean, onClick: () -> Unit) {
    val regionData = remember(regionName) {
        when (regionName) {
            "Africa" -> Triple("🌍", Color(0xFFF9A825), Color(0xFFF57F17))
            "Americas" -> Triple("🌎", Color(0xFF29B6F6), Color(0xFF0277BD))
            "Asia" -> Triple("🌏", Color(0xFFEF5350), Color(0xFFC62828))
            "Europe" -> Triple("🌍", Color(0xFF66BB6A), Color(0xFF2E7D32))
            "Oceania" -> Triple("🌊", Color(0xFFAB47BC), Color(0xFF6A1B9A))
            "Antarctic" -> Triple("❄️", Color(0xFF78909C), Color(0xFF37474F))
            else -> Triple("🌐", Color(0xFF78909C), Color(0xFF37474F))
        }
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(300)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(colors = listOf(regionData.second, regionData.third))).padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(regionData.first, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(regionName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("$countryCount countries", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
fun InfoGrid(country: Country) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(icon = Icons.Filled.Groups, label = "Population", value = formatPopulation(country.population), modifier = Modifier.weight(1f))
        InfoCard(icon = Icons.Filled.Landscape, label = "Area", value = formatArea(country.area), modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(icon = Icons.Filled.Public, label = "Region", value = country.region, modifier = Modifier.weight(1f))
        InfoCard(icon = Icons.Filled.Map, label = "Subregion", value = country.subregion.ifEmpty { "N/A" }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun InfoCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorderCountryChip(country: Country, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            AsyncImage(
                model = country.flagUrl,
                contentDescription = "Flag of ${country.name}",
                modifier = Modifier.size(32.dp, 24.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(country.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String, subMessage: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        Text(subMessage, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DetailSection(title: String, usePadding: Boolean = true, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (usePadding) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) { content() }
        } else {
            content()
        }
    }
}
