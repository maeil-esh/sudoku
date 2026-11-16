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
import com.uk2solutions.sudoku.domain.premium.Entitlement
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.uk2solutions.sudoku.ui.challenge.ChallengeReceiveDialog
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
fun HomeScreen(onResume: () -> Unit, onDaily: () -> Unit, onNewGame: (Difficulty) -> Unit, onPremium: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val resume by vm.resume.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val premium by vm.premium.collectAsStateWithLifecycle()
    val pending by vm.pendingChallenge.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    HomeContent(resume, daily, onResume, onDaily, onNewGame, premium, onPremium, onReceiveChallenge = { error = false; dialog = true })
    if (dialog || pending != null) {
        ChallengeReceiveDialog(
            initialText = pending.orEmpty(), error = error,
            onStart = { text -> vm.acceptChallenge(text, onStarted = { dialog = false; onResume() }, onInvalid = { error = true }) },
            onDismiss = { dialog = false; error = false; vm.clearPendingChallenge() },
        )
    }
}

@Composable
fun HomeContent(
    resume: ResumeInfo?, daily: DailyInfo, onResume: () -> Unit, onDaily: () -> Unit, onNewGame: (Difficulty) -> Unit,
    premium: Boolean = false, onPremium: () -> Unit = {}, onReceiveChallenge: () -> Unit = {},
) {
    val c = SudokuTheme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
            if (premium) Text(stringResource(R.string.premium_member), color = c.accent, fontWeight = FontWeight.SemiBold)
            else Button(
                onClick = onPremium, shape = RoundedCornerShape(14.dp), modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accentSoft, contentColor = c.accent),
            ) { Text(stringResource(R.string.premium_title), fontWeight = FontWeight.SemiBold) }
        }
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
                    Text(stringResource(R.string.daily_title), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.ink, modifier = Modifier.weight(1f).padding(end = 8.dp))
                    Text(stringResource(R.string.daily_streak, daily.streak), color = c.accent, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
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
        OutlinedButton(onClick = onReceiveChallenge, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.challenge_receive))
        }
        // 전문가·악마: 무료는 잠금 표시(탭하면 프리미엄 화면), 프리미엄은 기법 확장 전까지 "준비 중"
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Difficulty.EXPERT, Difficulty.DEVIL).forEach { d ->
                val locked = !Entitlement(premium).canPlay(d)
                val lockDesc = stringResource(R.string.cd_locked)
                val levelName = stringResource(d.labelRes())
                OutlinedButton(
                    onClick = { if (locked) onPremium() }, enabled = locked, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { if (locked) contentDescription = "$levelName, $lockDesc" },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(d.labelRes()), fontWeight = FontWeight.SemiBold)
                        if (locked) Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(" " + stringResource(R.string.premium_title), fontSize = 12.sp)
                        } else Text(stringResource(R.string.home_coming_soon), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
