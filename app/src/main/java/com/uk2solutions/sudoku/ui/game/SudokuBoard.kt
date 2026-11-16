package com.uk2solutions.sudoku.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

/**
 * SPEC §6.1: 박스 경계 2dp(ink), 칸 경계 1dp(line), 바깥 2dp(ink).
 * [sameDigit]: 강조할 숫자(선택 칸의 값, 숫자 우선 모드에서는 고른 숫자). 0이면 없음.
 */
@Composable
fun SudokuBoard(
    state: GameState,
    selected: Int,
    sameDigit: Int,
    conflicts: Set<Int>,
    settings: AppSettings,
    onCell: (Int) -> Unit,
    effect: Effect? = null,
    gain: ScoreGain? = null,
    hintCell: Int = -1,
    modifier: Modifier = Modifier,
) {
    val c = SudokuTheme.colors
    val flash = remember { Animatable(0f) }
    LaunchedEffect(effect?.id) {
        if (effect != null && effect.cells.isNotEmpty()) { flash.snapTo(1f); flash.animateTo(0f, tween(900)) }
    }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(c.surface)
            .drawWithContent {
                drawContent()
                val step = size.width / 9f
                for (k in 1..8) {
                    val thick = k % 3 == 0
                    val w = (if (thick) 2.dp else 1.dp).toPx()
                    val color = if (thick) c.ink else c.line
                    drawLine(color, Offset(k * step, 0f), Offset(k * step, size.height), w)
                    drawLine(color, Offset(0f, k * step), Offset(size.width, k * step), w)
                }
            }
            .border(2.dp, c.ink),
    ) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 9) {
                        val i = r * 9 + col
                        val isSel = i == selected
                        val isPeer = settings.highlightArea && selected >= 0 && !isSel &&
                            (Board.row(i) == Board.row(selected) || Board.col(i) == Board.col(selected) || Board.box(i) == Board.box(selected))
                        val isErr = settings.showErrors && i in conflicts
                        val isSame = settings.highlightSame && sameDigit != 0 && state.values[i] == sameDigit
                        val bg = when {
                            isErr -> c.errorBg
                            isSame -> c.cellSame
                            isSel -> c.cellSelected
                            isPeer -> c.cellPeer
                            else -> Color.Transparent
                        }
                        val f = if (effect != null && i in effect.cells) flash.value else 0f
                        Cell(
                            flash = f, hinted = i == hintCell, index = i, value = state.values[i], notes = state.notes[i],
                            given = state.isGiven(i), error = isErr, selected = isSel, background = bg,
                            onClick = { onCell(i) }, modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
        if (gain != null) key(gain.id) { ScoreFloat(gain, maxWidth / 9) }
    }
}

/** 채운 칸 위로 떠올랐다 사라지는 점수. */
@Composable
private fun ScoreFloat(gain: ScoreGain, cell: Dp) {
    val c = SudokuTheme.colors
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(1000)) }
    if (p.value >= 1f) return
    val desc = stringResource(R.string.cd_score_gain, gain.delta)
    Box(
        Modifier
            .offset(cell * (gain.index % 9), cell * (gain.index / 9) - cell * 0.9f * p.value)
            .size(cell)
            .graphicsLayer { alpha = 1f - p.value * p.value }
            .semantics { contentDescription = desc; liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = c.surface.copy(alpha = 0.9f)) {
            Text(
                if (gain.delta > 0) "+${gain.delta}" else "${gain.delta}",
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false,
                color = if (gain.delta > 0) c.accent else c.error,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun Cell(
    flash: Float, hinted: Boolean, index: Int, value: Int, notes: Int, given: Boolean, error: Boolean, selected: Boolean,
    background: Color, onClick: () -> Unit, modifier: Modifier,
) {
    val c = SudokuTheme.colors
    val row = Board.row(index) + 1
    val col = Board.col(index) + 1
    val noteList = (1..9).filter { notes and (1 shl (it - 1)) != 0 }.joinToString(" ")
    val desc = when {
        value != 0 && error -> stringResource(R.string.cell_desc_error, row, col, value)
        value != 0 && given -> stringResource(R.string.cell_desc_given, row, col, value)
        value != 0 -> stringResource(R.string.cell_desc_filled, row, col, value)
        notes != 0 -> stringResource(R.string.cell_desc_notes, row, col, noteList)
        else -> stringResource(R.string.cell_desc_empty, row, col)
    }
    val bg = lerp(background, c.accent.copy(alpha = 0.55f).compositeOver(c.surface), flash)
    Box(
        modifier
            .background(bg)
            .then(if (selected || hinted) Modifier.border(if (hinted) 3.dp else 2.dp, c.accent) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc; role = Role.Button; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            // 오류는 색 + 굵기로 함께 표현(색만으로 구분하지 않음)
            Text(
                value.toString(), fontSize = 24.sp,
                fontWeight = when { error -> FontWeight.ExtraBold; given -> FontWeight.SemiBold; else -> FontWeight.Medium },
                color = when { error -> c.error; given -> c.ink; else -> c.accent },
            )
        } else if (notes != 0) {
            Column(Modifier.fillMaxSize()) {
                for (r in 0 until 3) Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (k in 0 until 3) {
                        val d = r * 3 + k + 1
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (notes and (1 shl (d - 1)) != 0) Text(d.toString(), fontSize = 10.sp, lineHeight = 10.sp, color = c.muted)
                        }
                    }
                }
            }
        }
    }
}
