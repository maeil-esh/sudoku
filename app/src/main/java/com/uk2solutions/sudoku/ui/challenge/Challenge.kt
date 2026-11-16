package com.uk2solutions.sudoku.ui.challenge

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.domain.challenge.Challenge
import com.uk2solutions.sudoku.domain.challenge.ChallengeCode
import com.uk2solutions.sudoku.domain.challenge.ChallengeCompare
import com.uk2solutions.sudoku.domain.challenge.ChallengeResult
import com.uk2solutions.sudoku.domain.challenge.Outcome
import com.uk2solutions.sudoku.ui.game.formatTime
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

/** 도전장 공유: 코드와 내 기록이 든 문구를 메신저 등으로 보낸다. 서버를 거치지 않는다. */
fun shareChallenge(context: Context, challenge: Challenge, mine: ChallengeResult, opponent: ChallengeResult?) {
    val code = ChallengeCode.encode(challenge)
    val text = context.getString(R.string.challenge_share_text, formatTime(mine.timeSec * 1000L), mine.score, code)
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(
        Intent.createChooser(send, context.getString(if (opponent != null) R.string.challenge_send_back else R.string.challenge_send))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** 완료 대화상자: 내 기록, (도전 중이면) 상대와 비교, 도전장 보내기. */
@Composable
fun CompleteDialog(
    elapsedMs: Long, mistakes: Int, score: Int, opponent: ChallengeResult?,
    onSend: () -> Unit, onNewGame: () -> Unit, onHome: () -> Unit,
) {
    Dialog(onDismissRequest = {}) { CompleteCard(elapsedMs, mistakes, score, opponent, onSend, onNewGame, onHome) }
}

@Composable
fun CompleteCard(
    elapsedMs: Long, mistakes: Int, score: Int, opponent: ChallengeResult?,
    onSend: () -> Unit, onNewGame: () -> Unit, onHome: () -> Unit,
) {
    val c = SudokuTheme.colors
    val mine = ChallengeResult((elapsedMs / 1000).toInt(), mistakes, 0, score)
    Surface(shape = RoundedCornerShape(24.dp), color = c.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val outcome = opponent?.let { ChallengeCompare.outcome(mine, it) }
            Text(
                stringResource(
                    when (outcome) {
                        Outcome.WIN -> R.string.challenge_result_win
                        Outcome.LOSE -> R.string.challenge_result_lose
                        Outcome.DRAW -> R.string.challenge_result_draw
                        null -> R.string.game_complete_title
                    },
                ),
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (outcome == Outcome.LOSE) c.ink else c.accent,
            )
            ResultRow(stringResource(R.string.challenge_you), formatTime(elapsedMs), mistakes, score, highlight = true)
            opponent?.let { ResultRow(stringResource(R.string.challenge_opponent), formatTime(it.timeSec * 1000L), it.mistakes, it.score, highlight = false) }
            Button(onClick = onSend, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(if (opponent != null) R.string.challenge_send_back else R.string.challenge_send))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNewGame, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.game_complete_new))
                }
                OutlinedButton(onClick = onHome, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.game_complete_home))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(who: String, time: String, mistakes: Int, score: Int, highlight: Boolean) {
    val c = SudokuTheme.colors
    Surface(shape = RoundedCornerShape(12.dp), color = if (highlight) c.accentSoft else c.cellPeer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(who, fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.weight(0.8f))
            Text(stringResource(R.string.challenge_compare_line, time, mistakes, score), color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(2f))
        }
    }
}

/** 도전장 받기: 코드나 공유 문구를 붙여 넣고 시작. 클립보드는 "붙여넣기"를 눌렀을 때만 읽는다. */
@Composable
fun ChallengeReceiveDialog(initialText: String, error: Boolean, onStart: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { ReceiveCard(initialText, error, onStart, onDismiss) }
}

@Composable
fun ReceiveCard(initialText: String, error: Boolean, onStart: (String) -> Unit, onDismiss: () -> Unit, pasted: String? = null) {
    val c = SudokuTheme.colors
    var text by androidx.compose.runtime.remember(initialText) { androidx.compose.runtime.mutableStateOf(initialText) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Surface(shape = RoundedCornerShape(24.dp), color = c.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.challenge_receive_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(stringResource(R.string.challenge_receive_desc), color = c.muted, fontSize = 13.sp)
            androidx.compose.material3.OutlinedTextField(
                value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                label = { Text(stringResource(R.string.challenge_code_hint)) }, isError = error,
                supportingText = if (error) ({ Text(stringResource(R.string.challenge_invalid)) }) else null,
            )
            androidx.compose.material3.TextButton(onClick = { (pasted ?: clipboard.getText()?.text)?.let { text = it } }) {
                Text(stringResource(R.string.challenge_paste))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_close))
                }
                Button(onClick = { onStart(text) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.challenge_start))
                }
            }
        }
    }
}
