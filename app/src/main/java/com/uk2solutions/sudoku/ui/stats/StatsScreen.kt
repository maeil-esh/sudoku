package com.uk2solutions.sudoku.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.ui.game.formatTime
import com.uk2solutions.sudoku.ui.game.labelRes
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

@Composable
fun StatsScreen(vm: StatsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    StatsContent(ui)
}

@Composable
fun StatsContent(ui: StatsUi) {
    val c = SudokuTheme.colors
    val none = stringResource(R.string.stats_none)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.stats_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f), stringResource(R.string.stats_tile_done), stringResource(R.string.stats_unit_games, ui.totalDone))
            Tile(Modifier.weight(1f), stringResource(R.string.stats_tile_streak), stringResource(R.string.stats_unit_days, ui.streak))
            Tile(Modifier.weight(1f), stringResource(R.string.stats_tile_best), ui.bestMs?.let { formatTime(it) } ?: none)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TableRow(stringResource(R.string.stats_col_level), stringResource(R.string.stats_col_done), stringResource(R.string.stats_col_best), stringResource(R.string.stats_col_avg), header = true)
                Difficulty.entries.forEach { d ->
                    val st = ui.byDifficulty[d.name]
                    val supported = d in listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)
                    if (!supported) TableRow(stringResource(d.labelRes()), stringResource(R.string.stats_locked), "", "", muted = true)
                    else TableRow(
                        stringResource(d.labelRes()), (st?.cnt ?: 0).toString(),
                        st?.let { formatTime(it.best) } ?: none, st?.let { formatTime(it.avg.toLong()) } ?: none,
                    )
                }
            }
        }
        Text(stringResource(R.string.stats_premium_note), color = c.muted, fontSize = 13.sp)
    }
}

@Composable
private fun Tile(modifier: Modifier, label: String, value: String) {
    val c = SudokuTheme.colors
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = c.surface, border = BorderStroke(1.dp, c.line)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = c.muted, fontSize = 12.sp)
            Text(value, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TableRow(a: String, b: String, cc: String, d: String, header: Boolean = false, muted: Boolean = false) {
    val c = SudokuTheme.colors
    val color = if (header || muted) c.muted else c.ink
    val w = if (header) FontWeight.Medium else FontWeight.Normal
    Row(Modifier.fillMaxWidth()) {
        Text(a, Modifier.weight(1.2f), color = color, fontWeight = w)
        Text(b, Modifier.weight(1f), color = color, fontWeight = w, textAlign = TextAlign.End)
        Text(cc, Modifier.weight(1f), color = color, fontWeight = w, textAlign = TextAlign.End)
        Text(d, Modifier.weight(1f), color = color, fontWeight = w, textAlign = TextAlign.End)
    }
}
