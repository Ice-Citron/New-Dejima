package com.dejima.recipefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

// ══════════════════════════════════════════════════════
//  API MODELS
// ══════════════════════════════════════════════════════

data class SearchResponse(val results: List<RecipeSummary>?)
data class RandomResponse(val recipes: List<RecipeSummary>?)

data class RecipeSummary(
    val id: Int,
    val title: String?,
    val image: String?,
    val readyInMinutes: Int?,
    val servings: Int?,
    val summary: String?
)

data class RecipeDetail(
    val id: Int,
    val title: String?,
    val image: String?,
    val readyInMinutes: Int?,
    val servings: Int?,
    val summary: String?,
    val extendedIngredients: List<Ingredient>?,
    val analyzedInstructions: List<InstructionGroup>?
)

data class Ingredient(val original: String?)

data class InstructionGroup(
    val name: String?,
    val steps: List<InstructionStep>?
)

data class InstructionStep(
    val number: Int?,
    val step: String?
)

// ══════════════════════════════════════════════════════
//  RETROFIT
// ══════════════════════════════════════════════════════

private const val API_KEY = "YOUR_SPOONACULAR_API_KEY" // Get free key at https://spoonacular.com/food-api

interface SpoonacularApi {
    @GET("recipes/complexSearch")
    suspend fun search(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addInfo: Boolean = true,
        @Query("apiKey") apiKey: String = API_KEY
    ): SearchResponse

    @GET("recipes/random")
    suspend fun random(
        @Query("number") number: Int = 5,
        @Query("apiKey") apiKey: String = API_KEY
    ): RandomResponse

    @GET("recipes/{id}/information")
    suspend fun detail(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String = API_KEY
    ): RecipeDetail
}

object ApiClient {
    val api: SpoonacularApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularApi::class.java)
    }
}

// ══════════════════════════════════════════════════════
//  HELPERS
// ══════════════════════════════════════════════════════

fun formatTime(minutes: Int?): String {
    if (minutes == null) return "? min"
    return when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}min"
    }
}

fun stripHtml(html: String?): String {
    if (html == null) return ""
    return html.replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
        .replace("\n\n", "\n").trim()
}

// ══════════════════════════════════════════════════════
//  VIEWMODEL
// ══════════════════════════════════════════════════════

class RecipeViewModel : ViewModel() {
    private val api = ApiClient.api

    // Discover
    private val _random = MutableStateFlow<List<RecipeSummary>>(emptyList())
    val random: StateFlow<List<RecipeSummary>> = _random.asStateFlow()
    private val _randomLoading = MutableStateFlow(false)
    val randomLoading: StateFlow<Boolean> = _randomLoading.asStateFlow()

    // Search
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _results = MutableStateFlow<List<RecipeSummary>>(emptyList())
    val results: StateFlow<List<RecipeSummary>> = _results.asStateFlow()
    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    // Detail
    private val _detail = MutableStateFlow<RecipeDetail?>(null)
    val detail: StateFlow<RecipeDetail?> = _detail.asStateFlow()
    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    init { loadRandom() }

    fun loadRandom() {
        viewModelScope.launch {
            _randomLoading.value = true
            try { _random.value = api.random().recipes ?: emptyList() }
            catch (_: Exception) { }
            _randomLoading.value = false
        }
    }

    fun updateQuery(q: String) { _query.value = q }

    fun search() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _searchLoading.value = true
            _hasSearched.value = true
            try { _results.value = api.search(q).results ?: emptyList() }
            catch (_: Exception) { }
            _searchLoading.value = false
        }
    }

    fun loadDetail(id: Int) {
        viewModelScope.launch {
            _detailLoading.value = true
            _detail.value = null
            try { _detail.value = api.detail(id) }
            catch (_: Exception) { }
            _detailLoading.value = false
        }
    }
}

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
                    val vm: RecipeViewModel = viewModel()
                    RecipeApp(vm)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
//  NAVIGATION
// ══════════════════════════════════════════════════════

sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Discover : Tab("discover", "Discover", Icons.Default.Fastfood)
    data object Search : Tab("search", "Search", Icons.Default.Search)
}

@Composable
fun RecipeApp(vm: RecipeViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "discover"
    val tabs = listOf(Tab.Discover, Tab.Search)
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
                                        popUpTo("discover") { saveState = true }
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
        NavHost(nav, startDestination = "discover", modifier = Modifier.padding(padding)) {
            composable("discover") { DiscoverScreen(vm, nav) }
            composable("search") { SearchScreen(vm, nav) }
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
                DetailScreen(vm, id, nav)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
//  DISCOVER SCREEN
// ══════════════════════════════════════════════════════

val WarmOrange = Color(0xFFFF7043)
val WarmYellow = Color(0xFFFFCA28)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: RecipeViewModel, nav: NavHostController) {
    val recipes by vm.random.collectAsState()
    val loading by vm.randomLoading.collectAsState()

    Column(Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(WarmOrange, WarmYellow)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text("🍳 Recipe Finder", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Discover delicious meals", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            }
        }

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { vm.loadRandom() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (recipes.isEmpty() && !loading) {
                ErrorBox("No recipes loaded") { vm.loadRandom() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        DiscoverCard(recipe) { nav.navigate("detail/${recipe.id}") }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverCard(recipe: RecipeSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (recipe.image != null) {
                    AsyncImage(
                        model = recipe.image,
                        contentDescription = recipe.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Text("🍽️", fontSize = 48.sp) }
                }
                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
            }

            Column(Modifier.padding(14.dp)) {
                Text(
                    recipe.title ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime, "Time",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatTime(recipe.readyInMinutes),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People, "Servings",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${recipe.servings ?: "?"} servings",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
//  SEARCH SCREEN
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: RecipeViewModel, nav: NavHostController) {
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val loading by vm.searchLoading.collectAsState()
    val hasSearched by vm.hasSearched.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("🔍 Search Recipes") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { vm.updateQuery(it) },
            placeholder = { Text("Search pasta, chicken, cake...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                vm.search()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Search button
        Button(
            onClick = {
                focusManager.clearFocus()
                vm.search()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = query.isNotBlank()
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Find Recipes")
        }

        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            !hasSearched -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍕", fontSize = 64.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Search for any recipe!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No recipes found. Try a different search!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp))
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.id }) { recipe ->
                        SearchResultCard(recipe) { nav.navigate("detail/${recipe.id}") }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(recipe: RecipeSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (recipe.image != null) {
                AsyncImage(
                    model = recipe.image,
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) { Text("🍽️", fontSize = 28.sp) }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    recipe.title ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "⏱ ${formatTime(recipe.readyInMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "👥 ${recipe.servings ?: "?"} servings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
fun DetailScreen(vm: RecipeViewModel, id: Int, nav: NavHostController) {
    val recipe by vm.detail.collectAsState()
    val loading by vm.detailLoading.collectAsState()

    LaunchedEffect(id) { vm.loadDetail(id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        recipe?.title ?: "Recipe",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
        } else if (recipe != null) {
            val r = recipe!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Hero image
                item(key = "img") {
                    if (r.image != null) {
                        AsyncImage(
                            model = r.image,
                            contentDescription = r.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Title + info chips
                item(key = "info") {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            r.title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoChip("⏱", formatTime(r.readyInMinutes))
                            InfoChip("👥", "${r.servings ?: "?"} servings")
                        }
                    }
                }

                // Ingredients
                val ingredients = r.extendedIngredients?.mapNotNull { it.original } ?: emptyList()
                if (ingredients.isNotEmpty()) {
                    item(key = "ing_header") {
                        SectionHeader("🥕 Ingredients")
                    }
                    item(key = "ing_list") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                ingredients.forEach { ing ->
                                    Row(Modifier.padding(vertical = 3.dp)) {
                                        Text("•  ", fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                        Text(ing, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Instructions
                val steps = r.analyzedInstructions?.firstOrNull()?.steps ?: emptyList()
                if (steps.isNotEmpty()) {
                    item(key = "steps_header") {
                        SectionHeader("📝 Instructions")
                    }
                    item(key = "steps_list") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                steps.forEach { step ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "${step.number ?: ""}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            step.step ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary
                val summary = stripHtml(r.summary)
                if (summary.isNotBlank()) {
                    item(key = "summary_header") {
                        SectionHeader("📖 About")
                    }
                    item(key = "summary_text") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Text(
                                summary.take(800) + if (summary.length > 800) "…" else "",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            ErrorBox("Could not load recipe") { vm.loadDetail(id) }
        }
    }
}

@Composable
fun InfoChip(emoji: String, text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
