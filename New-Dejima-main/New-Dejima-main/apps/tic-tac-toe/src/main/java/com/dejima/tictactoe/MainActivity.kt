package com.dejima.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CellState { EMPTY, X, O }

data class GameState(
    val board: List<CellState> = List(9) { CellState.EMPTY },
    val currentPlayer: CellState = CellState.X,
    val winner: CellState? = null,
    val winningLine: List<Int>? = null,
    val isDraw: Boolean = false
)

val WIN_LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // columns
    listOf(0, 4, 8), listOf(2, 4, 6)                     // diagonals
)

fun checkWinner(board: List<CellState>): Pair<CellState?, List<Int>?> {
    for (line in WIN_LINES) {
        val (a, b, c) = line
        if (board[a] != CellState.EMPTY && board[a] == board[b] && board[b] == board[c]) {
            return board[a] to line
        }
    }
    return null to null
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
                    TicTacToeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen() {
    var game by remember { mutableStateOf(GameState()) }
    var scoreX by remember { mutableIntStateOf(0) }
    var scoreO by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }

    fun onCellClick(index: Int) {
        if (game.board[index] != CellState.EMPTY || game.winner != null || game.isDraw) return
        val newBoard = game.board.toMutableList()
        newBoard[index] = game.currentPlayer
        val (winner, winLine) = checkWinner(newBoard)
        val isDraw = winner == null && newBoard.none { it == CellState.EMPTY }

        if (winner == CellState.X) scoreX++
        else if (winner == CellState.O) scoreO++
        else if (isDraw) draws++

        game = GameState(
            board = newBoard,
            currentPlayer = if (game.currentPlayer == CellState.X) CellState.O else CellState.X,
            winner = winner,
            winningLine = winLine,
            isDraw = isDraw
        )
    }

    fun newGame() {
        game = GameState()
    }

    val gameOver = game.winner != null || game.isDraw
    val statusText = when {
        game.winner == CellState.X -> "🏆 X Wins!"
        game.winner == CellState.O -> "🏆 O Wins!"
        game.isDraw -> "🤝 It's a Draw!"
        game.currentPlayer == CellState.X -> "Player X's Turn"
        else -> "Player O's Turn"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tic Tac Toe") },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scoreboard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreItem(
                        label = "X",
                        score = scoreX,
                        color = MaterialTheme.colorScheme.primary,
                        isActive = !gameOver && game.currentPlayer == CellState.X
                    )
                    ScoreItem(
                        label = "Draw",
                        score = draws,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        isActive = false
                    )
                    ScoreItem(
                        label = "O",
                        score = scoreO,
                        color = MaterialTheme.colorScheme.tertiary,
                        isActive = !gameOver && game.currentPlayer == CellState.O
                    )
                }
            }

            // Status text
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    game.winner == CellState.X -> MaterialTheme.colorScheme.primary
                    game.winner == CellState.O -> MaterialTheme.colorScheme.tertiary
                    game.isDraw -> MaterialTheme.colorScheme.onSurfaceVariant
                    game.currentPlayer == CellState.X -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Game board
            GameBoard(
                board = game.board,
                winningLine = game.winningLine,
                onCellClick = { onCellClick(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // New Game button
            Button(
                onClick = { newGame() },
                modifier = Modifier.fillMaxWidth(0.6f),
                enabled = gameOver || game.board.any { it != CellState.EMPTY }
            ) {
                Text("🔄 New Game", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ScoreItem(label: String, score: Int, color: Color, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(
            "$score",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun GameBoard(
    board: List<CellState>,
    winningLine: List<Int>?,
    onCellClick: (Int) -> Unit
) {
    val gridSize = 3
    val cellSize = 100.dp
    val gap = 6.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until gridSize) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0 until gridSize) {
                    val index = row * gridSize + col
                    val isWinCell = winningLine?.contains(index) == true

                    GameCell(
                        state = board[index],
                        isWinCell = isWinCell,
                        size = cellSize,
                        onClick = { onCellClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun GameCell(
    state: CellState,
    isWinCell: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWinCell -> MaterialTheme.colorScheme.primaryContainer
            state != CellState.EMPTY -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(300),
        label = "cellBg"
    )

    val borderColor = when {
        isWinCell -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = if (isWinCell) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = state == CellState.EMPTY) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            CellState.X -> Text(
                "X",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWinCell) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            CellState.O -> Text(
                "O",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWinCell) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            CellState.EMPTY -> { /* empty cell */ }
        }
    }
}
