package com.uk2solutions.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.game.HintPolicy
import com.uk2solutions.sudoku.domain.game.InputResult
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

@Composable
fun GameScreen(onExit: () -> Unit, onNewGame: (Difficulty) -> Unit, vm: GameViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val c = SudokuTheme.colors

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onStop() }
    LaunchedEffect(ui.missing) { if (ui.missing) onExit() }
    fun leave() = scope.launch { vm.saveNow(); onExit() }
    BackHandler { leave() }

    val state = ui.state
    if (ui.loading || state == null) {
        Box(Modifier.fillMaxSize().background(c.background))
        return
    }
    LaunchedEffect(ui.effect?.id) {
        if (ui.effect != null && ui.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    fun buzz(r: InputResult) {
        if (!ui.settings.vibration) return
        when (r) {
            InputResult.FILLED, InputResult.NOTE_TOGGLED, InputResult.ERASED -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            InputResult.MISTAKE -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            InputResult.NONE -> Unit
        }
    }

    Column(Modifier.fillMaxSize().background(c.background).padding(horizontal = 15.dp)) {
        TopBar(ui.difficulty, vm, onBack = { leave() }, onPause = vm::pause)
        InfoRow(state, ui.settings.mistakeLimitEnabled, ui.settings.mistakeLimit)
        Spacer(Modifier.height(10.dp))
        if (ui.paused) PausedCover(onContinue = vm::resume, onExit = { leave() })
        else SudokuBoard(state, ui.selected, ui.conflicts, ui.settings, vm::onCell, ui.effect, ui.hint?.index ?: -1)
        Spacer(Modifier.height(12.dp))
        Tools(
            canUndo = state.undo.isNotEmpty() && !ui.paused, noteMode = ui.noteMode,
            onUndo = vm::onUndo, onNote = vm::onToggleNote,
            onHint = vm::onHint, hintsLeft = (HintPolicy.FREE_PER_DAY - ui.settings.hintsUsedToday).coerceAtLeast(0),
        )
        HintBanner(ui.hint, ui.hintsExhausted)
        Spacer(Modifier.height(6.dp))
        NumberPad(state, enabled = !ui.paused, effect = ui.effect, onDigit = { buzz(vm.onDigit(it)) }, onErase = { buzz(vm.onErase()) })
    }

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
        val elapsed by vm.elapsedMs.collectAsState()
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.game_complete_title)) },
            text = { Text(stringResource(R.string.game_complete_desc, formatTime(elapsed), state.mistakes)) },
            confirmButton = { TextButton(onClick = { onNewGame(ui.difficulty) }) { Text(stringResource(R.string.game_complete_new)) } },
            dismissButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.game_complete_home)) } },
        )
    }
}

@Composable
private fun TopBar(difficulty: Difficulty, vm: GameViewModel, onBack: () -> Unit, onPause: () -> Unit) {
    val c = SudokuTheme.colors
    val elapsed by vm.elapsedMs.collectAsState()
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.game_back), tint = c.ink)
        }
        Text(stringResource(difficulty.labelRes()), color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(formatTime(elapsed), color = c.ink, fontSize = 17.sp)
        TextButton(onClick = onPause, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.game_pause)) }
    }
}

@Composable
private fun InfoRow(state: GameState, limitEnabled: Boolean, limit: Int) {
    val c = SudokuTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (limitEnabled) stringResource(R.string.game_mistakes, state.mistakes, limit)
            else stringResource(R.string.game_mistakes_unlimited, state.mistakes),
            color = c.muted, fontSize = 14.sp,
        )
        Text(stringResource(R.string.game_progress, GameRules.progressPercent(state.values)), color = c.muted, fontSize = 14.sp)
    }
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
private fun Tools(canUndo: Boolean, noteMode: Boolean, onUndo: () -> Unit, onNote: () -> Unit, onHint: () -> Unit, hintsLeft: Int) {
    val c = SudokuTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
            Text(stringResource(R.string.game_undo))
        }
        OutlinedButton(onClick = onHint, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
            Text(stringResource(R.string.game_hint, hintsLeft), fontSize = 13.sp)
        }
        Button(
            onClick = onNote, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (noteMode) c.accent else c.accentSoft,
                contentColor = if (noteMode) androidx.compose.ui.graphics.Color.White else c.accent,
            ),
        ) { Text(stringResource(if (noteMode) R.string.game_notes_on else R.string.game_notes_off)) }
    }
}

@Composable
private fun HintBanner(hint: com.uk2solutions.sudoku.domain.game.Hint?, exhausted: Boolean) {
    val c = SudokuTheme.colors
    val text = when {
        hint != null -> stringResource(
            R.string.game_hint_msg, hint.index / 9 + 1, hint.index % 9 + 1,
            stringResource(when (hint.technique) {
                com.uk2solutions.sudoku.domain.game.HintTechnique.NAKED_SINGLE -> R.string.hint_naked_single
                com.uk2solutions.sudoku.domain.game.HintTechnique.HIDDEN_SINGLE -> R.string.hint_hidden_single
                com.uk2solutions.sudoku.domain.game.HintTechnique.DEDUCTION -> R.string.hint_deduction
            }),
        )
        exhausted -> stringResource(R.string.game_hint_exhausted)
        else -> return
    }
    Text(text, color = c.accent, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
}

@Composable
private fun NumberPad(state: GameState, enabled: Boolean, effect: Effect?, onDigit: (Int) -> Unit, onErase: () -> Unit) {
    val rows = listOf(listOf(1, 2, 3, 4, 5), listOf(6, 7, 8, 9, 0))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    if (d == 0) PadKey(Modifier.weight(1f), enabled, onErase) {
                        Text(stringResource(R.string.game_erase), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    } else {
                        val left = GameRules.remaining(state, d)
                        PadKey(Modifier.weight(1f), enabled && left > 0, { onDigit(d) }, pulse = effect?.digit == d, pulseId = effect?.id) {
                            Text(d.toString(), fontSize = 24.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (left > 0) stringResource(R.string.game_remaining, left) else stringResource(R.string.game_done),
                                fontSize = 11.sp, color = SudokuTheme.colors.muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadKey(
    modifier: Modifier, enabled: Boolean, onClick: () -> Unit,
    pulse: Boolean = false, pulseId: Int? = null, content: @Composable () -> Unit,
) {
    val c = SudokuTheme.colors
    val flash = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(pulseId) {
        if (pulse) { flash.snapTo(1f); flash.animateTo(0f, androidx.compose.animation.core.tween(900)) }
    }
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp), color = androidx.compose.ui.graphics.lerp(c.surface, c.cellSelected, flash.value), contentColor = if (enabled) c.accent else c.muted,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
