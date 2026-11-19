package com.uk2solutions.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.domain.challenge.Challenge
import com.uk2solutions.sudoku.domain.challenge.ChallengeResult
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.ui.challenge.CompleteDialog
import com.uk2solutions.sudoku.ui.challenge.shareChallenge
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.game.Hint
import com.uk2solutions.sudoku.domain.game.HintPolicy
import com.uk2solutions.sudoku.domain.game.HintTechnique
import com.uk2solutions.sudoku.domain.game.InputResult
import com.uk2solutions.sudoku.domain.game.Scoring
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.ui.theme.SudokuTheme
import kotlinx.coroutines.launch

fun Difficulty.labelRes() = when (this) {
    Difficulty.EASY -> R.string.diff_easy
    Difficulty.MEDIUM -> R.string.diff_medium
    Difficulty.HARD -> R.string.diff_hard
    Difficulty.EXPERT -> R.string.diff_expert
    Difficulty.DEVIL -> R.string.diff_devil
}

fun formatTime(ms: Long): String {
    val t = ms / 1000
    val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** 게임 화면의 상태 비저장 본체. 미리보기·스크린샷에도 쓴다. */
@Composable
fun GameBody(
    ui: GameUiState, state: GameState, elapsedMs: Long,
    onBack: () -> Unit, onCell: (Int) -> Unit, onUndo: () -> Unit, onErase: () -> Unit, onNote: () -> Unit,
    onHint: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onDigit: (Int) -> Unit,
    onCancelWrong: () -> Unit = {},
    onHintNext: () -> Unit = {}, onHintApply: () -> Unit = {}, onHintClose: () -> Unit = {},
    onLearn: (String) -> Unit = {},
) {
    val c = SudokuTheme.colors
    val padDigit = if (ui.selectedDigit > 0) ui.selectedDigit else if (ui.selected >= 0) state.values[ui.selected] else 0
    val score = remember(state.values, state.mistakes, state.hintsUsed, ui.difficulty) { Scoring.score(state, ui.difficulty) }
    val wrong = remember(state.values) { GameRules.wrongCells(state) }

    Column(Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.game_back), tint = c.ink)
            }
            Text(
                stringResource(if (ui.daily) R.string.daily_title else R.string.app_name),
                color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
            )
        }
        ui.opponent?.let {
            Text(
                stringResource(R.string.challenge_vs_banner, it.score), color = c.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        InfoBlock(elapsedMs, ui, state, score, onPause = onPause)
        Spacer(Modifier.height(8.dp))
        if (ui.paused) PausedCover(onContinue = onResume, onExit = onBack)
        else SudokuBoard(
            state, ui.selected, padDigit, ui.conflicts, wrong, ui.settings,
            onCell = if (ui.hintPlan != null) ({ _ -> }) else onCell, effect = ui.effect, gain = ui.scoreGain, hintCell = ui.hint?.index ?: -1,
            spotlight = ui.hintPlan?.steps?.getOrNull(ui.hintStep)?.highlight,
            emphasis = ui.hintPlan?.steps?.getOrNull(ui.hintStep)?.emphasis.orEmpty(),
            previewDigit = if (ui.hintPlan?.steps?.getOrNull(ui.hintStep)?.type == com.uk2solutions.sudoku.domain.game.StepType.ANSWER) ui.hint?.digit ?: 0 else 0,
        )
        val plan = ui.hintPlan
        if (plan != null && !ui.paused) {
            Spacer(Modifier.height(12.dp))
            HintCard(plan, ui.hintStep, onNext = onHintNext, onApply = onHintApply, onClose = onHintClose, onLearn = onLearn)
        } else {
            if (wrong.isNotEmpty() && !ui.paused) WrongBanner(ui.blockedNudge, onCancelWrong)
            else HintBanner(ui.hint, ui.hintsExhausted)
            Spacer(Modifier.height(14.dp))
            ToolRow(
                canUndo = state.undo.isNotEmpty() && !ui.paused, noteMode = ui.noteMode,
                hintsLeft = if (ui.premium) null else (HintPolicy.FREE_PER_DAY - ui.settings.hintsUsedToday).coerceAtLeast(0),
                onUndo = onUndo, onErase = onErase, onNote = onNote, onHint = onHint,
            )
            Spacer(Modifier.height(14.dp))
            DigitRow(state, padDigit, ui.settings.showRemaining, enabled = !ui.paused && wrong.isEmpty(), effect = ui.effect, onDigit = onDigit)
        }
        Spacer(Modifier.height(16.dp))
    }

}

@Composable
fun GameScreen(onExit: () -> Unit, onNewGame: (Difficulty) -> Unit, onLearn: (String) -> Unit = {}, vm: GameViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = SudokuTheme.colors
    val fx = remember { SoundFx() }
    DisposableEffect(Unit) { onDispose { fx.release() } }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onStop() }
    LaunchedEffect(ui.missing) { if (ui.missing) onExit() }
    fun leave() = scope.launch { vm.saveNow(); onExit() }
    BackHandler { leave() }

    val state = ui.state
    if (ui.loading || state == null) {
        Column(Modifier.fillMaxSize().background(c.background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = c.accent)
            Text(stringResource(R.string.game_generating), color = c.muted, modifier = Modifier.padding(top = 16.dp))
        }
        return
    }
    LaunchedEffect(ui.blockedNudge) {
        if (ui.blockedNudge > 0 && ui.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (ui.blockedNudge > 0 && ui.settings.sound) fx.mistake()
    }
    LaunchedEffect(ui.effect?.id) {
        if (ui.effect != null && ui.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (ui.effect != null && ui.settings.sound) fx.cheer()
    }
    fun feedback(r: InputResult) {
        if (r == InputResult.NONE) return
        if (ui.settings.vibration) {
            haptic.performHapticFeedback(if (r == InputResult.MISTAKE) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
        if (ui.settings.sound && r == InputResult.MISTAKE) fx.mistake()
        else if (ui.settings.sound) fx.tap()
    }

    val elapsedNow by vm.elapsedMs.collectAsState()
    val score = remember(state.values, state.mistakes, state.hintsUsed, ui.difficulty) { Scoring.score(state, ui.difficulty) }
    GameBody(
        ui, state, elapsedNow, onBack = { leave() }, onCell = { feedback(vm.onCell(it)) }, onUndo = vm::onUndo,
        onErase = { feedback(vm.onErase()) }, onNote = vm::onToggleNote, onHint = vm::onHint, onPause = vm::pause,
        onResume = vm::resume, onDigit = { feedback(vm.onDigit(it)) }, onCancelWrong = { feedback(vm.onCancelWrong()) },
        onHintNext = vm::onHintNext, onHintApply = { feedback(InputResult.FILLED); vm.onHintApply() }, onHintClose = vm::onHintClose, onLearn = onLearn,
    )

    if (ui.mistakeLimitReached) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.game_limit_title)) },
            text = { Text(stringResource(R.string.game_limit_desc, ui.settings.mistakeLimit)) },
            confirmButton = { TextButton(onClick = vm::continueWithoutLimit) { Text(stringResource(R.string.game_limit_continue)) } },
            dismissButton = { TextButton(onClick = { onNewGame(ui.difficulty) }) { Text(stringResource(R.string.game_limit_new)) } },
        )
    }
    if (ui.completed) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val mine = ChallengeResult(elapsedNow.toInt() / 1000, state.mistakes, state.hintsUsed, score)
        CompleteDialog(
            elapsedMs = elapsedNow, mistakes = state.mistakes, score = score, opponent = ui.opponent,
            onSend = { shareChallenge(ctx, Challenge(ui.difficulty, Board.format(state.givens), mine), mine, opponent = ui.opponent) },
            onNewGame = { onNewGame(ui.difficulty) }, onHome = onExit,
        )
    }
}

/** 보드 위 정보: [진행 n/m] [점수] / [실수 x/3] [난이도] [타이머 ⏸]. 각 항목은 설정으로 끌 수 있다. */
@Composable
private fun InfoBlock(elapsed: Long, ui: GameUiState, state: GameState, score: Int, onPause: () -> Unit) {
    val c = SudokuTheme.colors
    val (filled, total) = Scoring.progress(state)
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            // 글자가 아주 크면(접근성) 가운데 정렬 대신 양 끝으로 벌려 진행 표시와 점수가 겹치지 않게 한다.
            val bigFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
            val progress = @Composable {
                if (ui.settings.showProgress) Text(stringResource(R.string.game_progress_count, filled, total), color = c.muted, fontSize = 14.sp, maxLines = 1, softWrap = false)
            }
            if (bigFont) { progress(); androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            else Box(Modifier.weight(1f)) { progress() }
            if (ui.settings.showScore) {
                val pop = remember { Animatable(1f) }
                var prev by remember { mutableIntStateOf(score) }
                LaunchedEffect(score) {
                    if (score > prev && ui.settings.scoreAnimation) { pop.snapTo(1.3f); pop.animateTo(1f, tween(350)) }
                    prev = score
                }
                Text(
                    stringResource(R.string.game_score, score), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                )
            }
            if (!bigFont) Box(Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (ui.settings.mistakeLimitEnabled) stringResource(R.string.game_mistakes, state.mistakes, ui.settings.mistakeLimit)
                else stringResource(R.string.game_mistakes_unlimited, state.mistakes),
                color = c.muted, fontSize = 14.sp, modifier = Modifier.weight(1f),
            )
            Text(stringResource(ui.difficulty.labelRes()), color = c.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (ui.settings.showTimer) Text(formatTime(elapsed), color = c.muted, fontSize = 15.sp)
                PauseButton(onPause)
            }
        }
    }
}

@Composable
private fun PauseButton(onClick: () -> Unit) {
    val c = SudokuTheme.colors
    val desc = stringResource(R.string.game_pause)
    Box(
        Modifier.size(48.dp).clickable(onClick = onClick).semantics { contentDescription = desc; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val w = 2.dp.toPx()
            drawCircle(c.muted, radius = size.minDimension / 2 - w / 2, style = Stroke(w))
            val bar = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(c.muted, Offset(size.width * 0.4f, size.height * 0.33f), Offset(size.width * 0.4f, size.height * 0.67f), bar.width, StrokeCap.Round)
            drawLine(c.muted, Offset(size.width * 0.6f, size.height * 0.33f), Offset(size.width * 0.6f, size.height * 0.67f), bar.width, StrokeCap.Round)
        }
    }
}

@Composable
private fun HintBanner(hint: Hint?, exhausted: Boolean) {
    val c = SudokuTheme.colors
    val text = when {
        exhausted -> stringResource(R.string.game_hint_exhausted)
        else -> return
    }
    Text(text, color = c.accent, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp))
}

@Composable
private fun PausedCover(onContinue: () -> Unit, onExit: () -> Unit) {
    val c = SudokuTheme.colors
    Surface(
        Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(4.dp),
        color = c.surface, border = BorderStroke(2.dp, c.ink),
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.game_paused_title), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Text(stringResource(R.string.game_paused_desc), color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.game_continue)) }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)) { Text(stringResource(R.string.game_exit)) }
        }
    }
}

@Composable
private fun ToolRow(
    canUndo: Boolean, noteMode: Boolean, hintsLeft: Int?,
    onUndo: () -> Unit, onErase: () -> Unit, onNote: () -> Unit, onHint: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ToolButton(R.drawable.ic_undo, stringResource(R.string.game_undo), canUndo, null, false, onUndo)
        ToolButton(R.drawable.ic_eraser, stringResource(R.string.game_erase), true, null, false, onErase)
        ToolButton(R.drawable.ic_pencil, stringResource(R.string.game_notes), true, stringResource(if (noteMode) R.string.badge_on else R.string.badge_off), noteMode, onNote)
        ToolButton(R.drawable.ic_bulb, stringResource(R.string.game_hint), true, hintsLeft?.toString() ?: "∞", hintsLeft == null || hintsLeft > 0, onHint)
    }
}

@Composable
private fun ToolButton(@androidx.annotation.DrawableRes icon: Int, label: String, enabled: Boolean, badge: String?, badgeOn: Boolean, onClick: () -> Unit) {
    val c = SudokuTheme.colors
    Column(
        Modifier.heightIn(min = 64.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(
                painterResource(icon), contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp).size(30.dp),
                tint = if (enabled) c.ink else c.muted.copy(alpha = 0.5f),
            )
            if (badge != null) {
                Surface(
                    shape = CircleShape, color = if (badgeOn) c.accent else c.line,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-4).dp),
                ) {
                    Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (badgeOn) c.onAccent else c.muted, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
        }
        Text(label, fontSize = 12.sp, color = c.ink)
    }
}

/** 한 줄 숫자 패드. 선택된 숫자는 흰 카드로 올라오고, 아래에 남은 개수를 표시한다. */
@Composable
private fun DigitRow(state: GameState, selectedDigit: Int, showRemaining: Boolean, enabled: Boolean, effect: Effect?, onDigit: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (d in 1..9) {
            val left = GameRules.remaining(state, d)
            DigitKey(
                d, left, selected = d == selectedDigit, showRemaining = showRemaining, enabled = enabled && left > 0,
                pulse = effect?.digit == d, pulseId = effect?.id, onClick = { onDigit(d) }, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: Int, left: Int, selected: Boolean, showRemaining: Boolean, enabled: Boolean,
    pulse: Boolean, pulseId: Int?, onClick: () -> Unit, modifier: Modifier,
) {
    val c = SudokuTheme.colors
    val flash = remember { Animatable(0f) }
    LaunchedEffect(pulseId) { if (pulse) { flash.snapTo(1f); flash.animateTo(0f, tween(900)) } }
    val base = if (selected) c.surface else c.accentSoft
    val desc = stringResource(R.string.game_key_desc, digit, left)
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(64.dp).semantics(mergeDescendants = true) { contentDescription = desc; role = Role.Button },
        shape = RoundedCornerShape(10.dp), color = lerp(base, c.cellSame, flash.value),
        shadowElevation = if (selected) 4.dp else 0.dp,
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            // 읽기는 부모의 contentDescription("7, 남은 개수 4")으로 하고, 보이는 글자는 읽지 않게 해 중복을 막는다.
            // (clearAndSetSemantics를 부모에 쓰면 "누르기" 동작까지 지워져 TalkBack으로 누를 수 없다.)
            Text(
                digit.toString(), fontSize = 28.sp, fontWeight = FontWeight.Medium,
                color = if (enabled) c.accent else c.muted.copy(alpha = 0.4f),
                modifier = Modifier.clearAndSetSemantics {},
            )
            if (showRemaining) Text(if (left > 0) left.toString() else "", fontSize = 13.sp, color = if (selected) c.ink else c.muted, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

/** 틀린 숫자 안내: 지울 때까지 다음 입력이 막힌다. 막힌 채 입력하려 하면(nudge) 잠깐 강조된다. */
@Composable
private fun WrongBanner(nudge: Int, onCancel: () -> Unit) {
    val c = SudokuTheme.colors
    val flash = remember { Animatable(0f) }
    LaunchedEffect(nudge) { if (nudge > 0) { flash.snapTo(1f); flash.animateTo(0f, tween(600)) } }
    Surface(
        shape = RoundedCornerShape(12.dp), color = lerp(c.errorBg, c.error.copy(alpha = 0.35f).compositeOver(c.errorBg), flash.value),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wrong_banner), color = c.error, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.wrong_cancel), color = c.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 힌트 설명 카드: 제목(기법 이름), 단계별 설명, 단계 점, 다음/채우기 버튼. 보드는 관련 칸만 밝게 남는다. */
@Composable
private fun HintCard(plan: com.uk2solutions.sudoku.domain.game.HintPlan, step: Int, onNext: () -> Unit, onApply: () -> Unit, onClose: () -> Unit, onLearn: (String) -> Unit) {
    val c = SudokuTheme.colors
    val st = plan.steps[step.coerceIn(0, plan.steps.lastIndex)]
    val last = step >= plan.steps.lastIndex
    val title = stringResource(when (plan.hint.technique) {
        HintTechnique.NAKED_SINGLE -> R.string.hint_naked_single
        HintTechnique.HIDDEN_SINGLE -> R.string.hint_hidden_single
        HintTechnique.DEDUCTION -> R.string.hint_deduction
    })
    val unitName = stringResource(when (st.unit ?: plan.hint.unit) {
        com.uk2solutions.sudoku.domain.game.UnitKind.ROW -> R.string.hint_unit_row
        com.uk2solutions.sudoku.domain.game.UnitKind.COL -> R.string.hint_unit_col
        else -> R.string.hint_unit_box
    })
    val text = when (st.type) {
        com.uk2solutions.sudoku.domain.game.StepType.INTRO -> when (plan.hint.technique) {
            HintTechnique.NAKED_SINGLE -> stringResource(R.string.hint_intro_naked)
            HintTechnique.HIDDEN_SINGLE -> stringResource(R.string.hint_intro_hidden, unitName, plan.hint.digit)
            HintTechnique.DEDUCTION -> stringResource(if (plan.hint.learn != null) R.string.hint_intro_technique else R.string.hint_intro_deduction)
        }
        com.uk2solutions.sudoku.domain.game.StepType.ELIMINATION -> when (plan.hint.technique) {
            HintTechnique.HIDDEN_SINGLE -> stringResource(R.string.hint_elim_hidden, unitName, plan.hint.digit)
            else -> stringResource(R.string.hint_elim_naked, st.digits.joinToString(", "))
        }
        com.uk2solutions.sudoku.domain.game.StepType.ANSWER -> stringResource(R.string.hint_answer, plan.hint.digit)
    }
    Surface(shape = RoundedCornerShape(24.dp), color = c.surface, border = BorderStroke(1.dp, c.line), shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = c.ink)
                }
            }
            Text(text, color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.heightIn(min = 66.dp))
            if (plan.hint.technique == HintTechnique.DEDUCTION) LearnRow(plan.hint.learn, onLearn)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                plan.steps.indices.forEach { i ->
                    Surface(
                        shape = CircleShape, color = if (i == step) c.accent else c.line,
                        modifier = Modifier.padding(horizontal = 3.dp).size(width = if (i == step) 22.dp else 8.dp, height = 6.dp),
                    ) {}
                }
            }
            Button(
                onClick = if (last) onApply else onNext, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(if (last) R.string.hint_fill else R.string.hint_next), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        }
    }
}

/** 추론이 필요한 자리: 어떤 기술인지 알려 주고 트레이닝 레슨으로 이어 준다(잠긴 레슨은 맛보기→프리미엄). */
@Composable
private fun LearnRow(learn: com.uk2solutions.sudoku.domain.technique.Technique?, onLearn: (String) -> Unit) {
    val c = SudokuTheme.colors
    val lesson = learn?.let { t -> com.uk2solutions.sudoku.domain.training.Lessons.all.firstOrNull { it.technique == t && !it.rules } }
    Surface(shape = RoundedCornerShape(14.dp), color = c.accentSoft, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (lesson != null) stringResource(R.string.hint_learn_body, com.uk2solutions.sudoku.ui.training.lessonTitle(lesson)) else stringResource(R.string.hint_learn_beyond),
                color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
            )
            if (lesson != null) TextButton(onClick = { onLearn(lesson.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.hint_learn_cta), color = c.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
