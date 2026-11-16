package com.uk2solutions.sudoku.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
fun HomeScreen(onResume: () -> Unit, onDaily: () -> Unit, onNewGame: (Difficulty) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val resume by vm.resume.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val c = SudokuTheme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.home_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.ink)
        resume?.let { r ->
            Surface(shape = RoundedCornerShape(20.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_resume_title), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.ink)
                    Text(
                        stringResource(R.string.home_resume_info, stringResource(r.difficulty.labelRes()), formatTime(r.elapsedMs), r.percent),
                        color = c.muted,
                    )
                    Button(onClick = onResume, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.game_continue))
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = c.accentSoft, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.daily_title), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.ink)
                    Text(stringResource(R.string.daily_streak, daily.streak), color = c.accent, fontWeight = FontWeight.SemiBold)
                }
                Text(stringResource(R.string.daily_desc), color = c.muted, fontSize = 13.sp)
                Button(
                    onClick = onDaily, enabled = daily.status != DailyStatus.DONE,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(when (daily.status) {
                        DailyStatus.NOT_STARTED -> stringResource(R.string.daily_start)
                        DailyStatus.IN_PROGRESS -> stringResource(R.string.daily_continue, daily.percent)
                        DailyStatus.DONE -> stringResource(R.string.daily_done)
                    })
                }
            }
        }
        Text(stringResource(R.string.home_new_game), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD).forEach { d ->
                Button(onClick = { onNewGame(d) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(d.labelRes()))
                }
            }
        }
        // 어려움~악마는 M3(기법 평가)·M4(잠금 UI)에서 활성화
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Difficulty.EXPERT, Difficulty.DEVIL).forEach { d ->
                OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("${stringResource(d.labelRes())}\n${stringResource(R.string.home_coming_soon)}", fontSize = 12.sp)
                }
            }
        }
    }
}
