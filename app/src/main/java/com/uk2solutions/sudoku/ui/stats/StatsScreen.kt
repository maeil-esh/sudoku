package com.uk2solutions.sudoku.ui.stats

import androidx.compose.foundation.BorderStroke
import java.time.format.TextStyle
import java.time.LocalDate
import com.uk2solutions.sudoku.domain.premium.Entitlement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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
fun StatsScreen(onPremium: () -> Unit, vm: StatsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    StatsContent(ui, onPremium)
}

@Composable
fun StatsContent(ui: StatsUi, onPremium: () -> Unit = {}) {
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
                    TableRow(
                        stringResource(d.labelRes()), (st?.cnt ?: 0).toString(),
                        st?.let { formatTime(it.best) } ?: none, st?.let { formatTime(it.avg.toLong()) } ?: none,
                    )
                }
            }
        }
        if (ui.premium) DetailStats(ui) else LockedDetail(onPremium)
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

@Composable
private fun LockedDetail(onPremium: () -> Unit) {
    val c = SudokuTheme.colors
    Surface(shape = RoundedCornerShape(20.dp), color = c.accentSoft, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_lock), contentDescription = stringResource(R.string.cd_locked), tint = c.accent, modifier = Modifier.size(18.dp))
                Text(" " + stringResource(R.string.stats_detail_title), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.ink)
            }
            Text("${stringResource(R.string.stats_weekly)} · ${stringResource(R.string.stats_distribution)}", color = c.muted, fontSize = 13.sp)
            Button(onClick = onPremium, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.stats_locked_cta))
            }
        }
    }
}

/** 상세 통계(프리미엄): 최근 7일 완료 수 막대, 난이도별 완료 비율. */
@Composable
private fun DetailStats(ui: StatsUi) {
    val c = SudokuTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    Surface(shape = RoundedCornerShape(20.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.stats_detail_title), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.ink)
            if (ui.totalDone == 0) {
                Text(stringResource(R.string.stats_detail_empty), color = c.muted)
                return@Column
            }
            Text(stringResource(R.string.stats_weekly), color = c.muted, fontSize = 13.sp)
            val max = (ui.weekly.maxOrNull() ?: 0).coerceAtLeast(1)
            val today = LocalDate.now()
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                ui.weekly.forEachIndexed { i, n ->
                    val day = today.minusDays((6 - i).toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(if (n > 0) n.toString() else "", fontSize = 12.sp, color = c.ink)
                        Box(Modifier.fillMaxWidth().height((70.dp * n / max).coerceAtLeast(if (n > 0) 4.dp else 2.dp)).background(if (n > 0) c.accent else c.line, RoundedCornerShape(4.dp)))
                        Text(day, fontSize = 12.sp, color = c.muted)
                    }
                }
            }
            Text(stringResource(R.string.stats_distribution), color = c.muted, fontSize = 13.sp)
            Difficulty.entries.filter { it in Entitlement.FREE_DIFFICULTIES }.forEach { d ->
                val n = ui.byDifficulty[d.name]?.cnt ?: 0
                val pct = n * 100 / ui.totalDone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(d.labelRes()), Modifier.width(88.dp), color = c.ink, fontSize = 14.sp)
                    Box(Modifier.weight(1f).height(10.dp).background(c.line, RoundedCornerShape(5.dp))) {
                        if (pct > 0) Box(Modifier.fillMaxWidth(pct / 100f).height(10.dp).background(c.accent, RoundedCornerShape(5.dp)))
                    }
                    Text("$pct%", Modifier.width(48.dp), color = c.muted, fontSize = 13.sp, textAlign = TextAlign.End)
                }
            }
        }
    }
}
