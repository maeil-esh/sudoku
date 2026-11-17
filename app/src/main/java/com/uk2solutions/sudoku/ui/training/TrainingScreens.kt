package com.uk2solutions.sudoku.ui.training

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.premium.Entitlement
import com.uk2solutions.sudoku.domain.technique.Technique
import com.uk2solutions.sudoku.domain.training.Example
import com.uk2solutions.sudoku.domain.training.Lesson
import com.uk2solutions.sudoku.domain.training.LessonGroup
import com.uk2solutions.sudoku.domain.training.Lessons
import com.uk2solutions.sudoku.domain.training.QuizKind
import com.uk2solutions.sudoku.ui.game.NoteTone
import com.uk2solutions.sudoku.ui.game.SudokuBoard
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

// ---------------------------------------------------------------- 문장 매핑 (R8 리소스 축소가 지우지 않도록 명시적으로 참조)

private data class Txt(@StringRes val title: Int, @StringRes val intro: Int, @StringRes val s1: Int, @StringRes val s2: Int, @StringRes val ask: Int)

private fun txt(id: String) = when (id) {
    "rules" -> Txt(R.string.tr_rules_title, R.string.tr_rules_intro, R.string.tr_rules_s1, R.string.tr_rules_s2, R.string.tr_rules_ask)
    "naked_single" -> Txt(R.string.tr_naked_single_title, R.string.tr_naked_single_intro, R.string.tr_naked_single_s1, R.string.tr_naked_single_s2, R.string.tr_naked_single_ask)
    "hidden_single" -> Txt(R.string.tr_hidden_single_title, R.string.tr_hidden_single_intro, R.string.tr_hidden_single_s1, R.string.tr_hidden_single_s2, R.string.tr_hidden_single_ask)
    "pointing" -> Txt(R.string.tr_pointing_title, R.string.tr_pointing_intro, R.string.tr_pointing_s1, R.string.tr_pointing_s2, R.string.tr_pointing_ask)
    "claiming" -> Txt(R.string.tr_claiming_title, R.string.tr_claiming_intro, R.string.tr_claiming_s1, R.string.tr_claiming_s2, R.string.tr_claiming_ask)
    "naked_pair" -> Txt(R.string.tr_naked_pair_title, R.string.tr_naked_pair_intro, R.string.tr_naked_pair_s1, R.string.tr_naked_pair_s2, R.string.tr_naked_pair_ask)
    "hidden_pair" -> Txt(R.string.tr_hidden_pair_title, R.string.tr_hidden_pair_intro, R.string.tr_hidden_pair_s1, R.string.tr_hidden_pair_s2, R.string.tr_hidden_pair_ask)
    "naked_triple" -> Txt(R.string.tr_naked_triple_title, R.string.tr_naked_triple_intro, R.string.tr_naked_triple_s1, R.string.tr_naked_triple_s2, R.string.tr_naked_triple_ask)
    "hidden_triple" -> Txt(R.string.tr_hidden_triple_title, R.string.tr_hidden_triple_intro, R.string.tr_hidden_triple_s1, R.string.tr_hidden_triple_s2, R.string.tr_hidden_triple_ask)
    "x_wing" -> Txt(R.string.tr_x_wing_title, R.string.tr_x_wing_intro, R.string.tr_x_wing_s1, R.string.tr_x_wing_s2, R.string.tr_x_wing_ask)
    "y_wing" -> Txt(R.string.tr_y_wing_title, R.string.tr_y_wing_intro, R.string.tr_y_wing_s1, R.string.tr_y_wing_s2, R.string.tr_y_wing_ask)
    else -> Txt(R.string.tr_swordfish_title, R.string.tr_swordfish_intro, R.string.tr_swordfish_s1, R.string.tr_swordfish_s2, R.string.tr_swordfish_ask)
}

@StringRes private fun groupName(g: LessonGroup) = when (g) {
    LessonGroup.BASICS -> R.string.training_group_basics
    LessonGroup.BEGINNER -> R.string.training_group_beginner
    LessonGroup.INTERMEDIATE -> R.string.training_group_intermediate
    LessonGroup.EXPERT -> R.string.training_group_expert
}

@Composable fun lessonTitle(lesson: Lesson) = stringResource(txt(lesson.id).title)

private enum class Slot { S1, S2, ASK }

/** 기법마다 문장이 쓰는 자리표시자가 다르다(숫자 하나 / 숫자 목록 / 숫자 셋). */
@Composable
private fun lessonText(slot: Slot, lesson: Lesson, ex: Example): String {
    val t = txt(lesson.id)
    val res = when (slot) { Slot.S1 -> t.s1; Slot.S2 -> t.s2; Slot.ASK -> t.ask }
    val dg = ex.deduction.digits
    return when (lesson.technique) {
        Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE ->
            if (slot == Slot.ASK) stringResource(res, ex.quizDigit) else stringResource(res, dg.joinToString(", "))
        Technique.Y_WING -> if (slot == Slot.ASK) stringResource(res, ex.quizDigit) else stringResource(res, dg[0], dg[1], dg[2])
        else -> stringResource(res, ex.quizDigit)
    }
}

// ---------------------------------------------------------------- 목록

@Composable
fun TrainingHubScreen(onOpen: (String) -> Unit, onPremium: () -> Unit, vm: TrainingHubViewModel = hiltViewModel()) {
    val scores by vm.scores.collectAsStateWithLifecycle()
    val premium by vm.premium.collectAsStateWithLifecycle()
    TrainingContent(scores, premium, onOpen, onPremium)
}

@Composable
fun TrainingContent(scores: Map<String, Int>, premium: Boolean, onOpen: (String) -> Unit, onPremium: () -> Unit) {
    val c = SudokuTheme.colors
    val ent = Entitlement(premium)
    val next = Lessons.all.firstOrNull { (scores[it.id] ?: -1) < 2 && (!it.premium || ent.premium) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.training_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.ink)
        Text(stringResource(R.string.training_subtitle), color = c.muted, fontSize = 14.sp)
        if (next != null) Surface(shape = RoundedCornerShape(20.dp), color = c.accentSoft, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.training_continue), color = c.accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(lessonTitle(next), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Button(onClick = { onOpen(next.id) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.training_start))
                }
            }
        }
        LessonGroup.entries.forEach { g ->
            Text(stringResource(groupName(g)), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.muted, modifier = Modifier.padding(top = 6.dp))
            Lessons.all.filter { it.group == g }.forEach { l ->
                val locked = l.premium && !ent.premium
                LessonRow(Lessons.all.indexOf(l) + 1, l, scores[l.id], locked) { if (locked) onPremium() else onOpen(l.id) }
            }
        }
    }
}

@Composable
private fun LessonRow(n: Int, l: Lesson, best: Int?, locked: Boolean, onClick: () -> Unit) {
    val c = SudokuTheme.colors
    val lockDesc = stringResource(R.string.cd_locked)
    val scoreDesc = if (best != null) stringResource(R.string.training_mastered_cd, best) else ""
    val title = lessonTitle(l)
    Surface(
        shape = RoundedCornerShape(16.dp), color = c.surface, border = BorderStroke(1.dp, c.line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).semantics { contentDescription = listOf(title, if (locked) lockDesc else "", scoreDesc).filter { it.isNotEmpty() }.joinToString(", ") },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = c.accentSoft, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(n.toString(), color = c.accent, fontWeight = FontWeight.Bold) }
            }
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = c.ink, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            if (locked) Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
            else if (best != null) Row {
                repeat(3) { i -> Icon(Icons.Filled.Star, contentDescription = null, tint = if (i < best) c.accent else c.line, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

// ---------------------------------------------------------------- 레슨

@Composable
fun LessonScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onPremium: () -> Unit, vm: LessonViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val premium by vm.premium.collectAsStateWithLifecycle()
    val locked = ui.lesson?.let { it.premium && !premium } == true
    LessonBody(
        ui.copy(locked = locked), onBack = onBack, onStart = vm::start, onDemoNext = vm::demoNext, onDemoBack = vm::demoBack,
        onPickDigit = vm::pickDigit, onTapCell = vm::tapCell, onCheck = vm::check, onNextQuestion = vm::nextQuestion,
        onRetry = vm::retry, onNextLesson = { ui.nextLessonId?.let(onOpen) }, onPremium = onPremium,
    )
}

/** 레슨 화면의 상태 비저장 본체(스크린샷 테스트에서도 쓴다). */
@Composable
fun LessonBody(
    ui: LessonUi, onBack: () -> Unit, onStart: () -> Unit, onDemoNext: () -> Unit, onDemoBack: () -> Unit,
    onPickDigit: (Int) -> Unit, onTapCell: (Int) -> Unit, onCheck: () -> Unit, onNextQuestion: () -> Unit,
    onRetry: () -> Unit, onNextLesson: () -> Unit, onPremium: () -> Unit = {},
) {
    val c = SudokuTheme.colors
    val lesson = ui.lesson
    Column(Modifier.fillMaxSize().background().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.game_back), tint = c.ink) }
            Text(lesson?.let { lessonTitle(it) } ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink)
        }
        when {
            ui.loading -> Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = c.accent); Text(stringResource(R.string.training_loading), color = c.muted)
            }
            ui.locked -> Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(painterResource(R.drawable.ic_lock), contentDescription = stringResource(R.string.cd_locked), tint = c.accent, modifier = Modifier.size(40.dp))
                Button(onClick = onPremium, shape = RoundedCornerShape(14.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.premium_title)) }
            }
            ui.failed || lesson == null -> Text(stringResource(R.string.training_failed), color = c.error, modifier = Modifier.padding(top = 40.dp))
            ui.phase == LessonPhase.INTRO -> IntroCard(lesson, onStart)
            ui.phase == LessonPhase.DEMO -> DemoPhase(ui, lesson, onDemoNext, onDemoBack)
            ui.phase == LessonPhase.QUIZ -> QuizPhase(ui, lesson, onPickDigit, onTapCell, onCheck, onNextQuestion)
            else -> ResultCard(ui, onRetry, onNextLesson, onBack)
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun Modifier.background(): Modifier = this.then(Modifier)

@Composable
private fun IntroCard(lesson: Lesson, onStart: () -> Unit) {
    val c = SudokuTheme.colors
    Surface(shape = RoundedCornerShape(24.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.training_lesson_n, Lessons.all.indexOf(lesson) + 1), color = c.accent, fontWeight = FontWeight.SemiBold)
            Text(lessonTitle(lesson), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(stringResource(txt(lesson.id).intro), color = c.muted, fontSize = 16.sp, lineHeight = 24.sp)
            Button(onClick = onStart, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.training_start), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}

/** 보드에 어떻게 보여 줄지(스포트라이트·강조·후보 톤). 답이 드러나는 시점은 [reveal]. */
private data class View(
    val spotlight: Set<Int>, val emphasis: Set<Int>, val target: Int, val preview: Int, val tones: Map<Int, Map<Int, NoteTone>>,
)

private fun viewOf(ex: Example, lesson: Lesson, reveal: Boolean): View {
    val d = ex.deduction
    return when (lesson.quiz) {
        QuizKind.PICK_DIGIT -> View(d.houses, emptySet(), d.place!!.first, if (reveal) ex.quizDigit else 0, emptyMap())
        QuizKind.PICK_CELL -> View(d.houses, emptySet(), if (reveal) d.place!!.first else -1, if (reveal) ex.quizDigit else 0, emptyMap())
        QuizKind.PICK_ELIMINATIONS -> {
            val tones = HashMap<Int, MutableMap<Int, NoteTone>>()
            for (cell in d.pattern) for (dg in d.digits) if (ex.grid.has(cell, dg)) tones.getOrPut(cell) { HashMap() }[dg] = NoteTone.PATTERN
            if (reveal) for ((cell, dg) in d.eliminations) tones.getOrPut(cell) { HashMap() }[dg] = NoteTone.REMOVE
            View(d.houses + d.pattern, d.pattern, -1, 0, tones)
        }
    }
}

@Composable
private fun LessonBoard(ex: Example, lesson: Lesson, reveal: Boolean, picked: Set<Int>, onCell: (Int) -> Unit) {
    val v = viewOf(ex, lesson, reveal)
    val state = GameState(ex.grid.v.copyOf(), ex.solution, ex.grid.v.copyOf(), if (lesson.showCandidates) ex.grid.c.copyOf() else IntArray(81))
    SudokuBoard(
        state, -1, 0, emptySet(), emptySet(), AppSettings(highlightArea = false, highlightSame = false, showErrors = false), onCell,
        hintCell = v.target, spotlight = v.spotlight, emphasis = v.emphasis, previewDigit = v.preview, noteTones = v.tones, picked = picked,
    )
}

@Composable
private fun StepCard(title: String, text: String, step: Int, total: Int, primary: String, onPrimary: () -> Unit, onBack: (() -> Unit)?) {
    val c = SudokuTheme.colors
    Surface(shape = RoundedCornerShape(24.dp), color = c.surface, border = BorderStroke(1.dp, c.line), shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(text, color = c.muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.heightIn(min = 66.dp))
            if (total > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(total) { i ->
                    Surface(shape = CircleShape, color = if (i == step) c.accent else c.line, modifier = Modifier.padding(horizontal = 3.dp).size(width = if (i == step) 22.dp else 8.dp, height = 6.dp)) {}
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onBack != null) OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text(stringResource(R.string.training_back)) }
                Button(onClick = onPrimary, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(if (onBack != null) 2f else 1f).heightIn(min = 52.dp)) {
                    Text(primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DemoPhase(ui: LessonUi, lesson: Lesson, onNext: () -> Unit, onBack: () -> Unit) {
    val ex = ui.demo ?: return
    LessonBoard(ex, lesson, reveal = ui.demoStep >= 1, picked = emptySet(), onCell = {})
    Spacer(Modifier.height(12.dp))
    val last = ui.demoStep >= 1
    StepCard(
        title = lessonTitle(lesson), text = lessonText(if (last) Slot.S2 else Slot.S1, lesson, ex), step = ui.demoStep, total = 2,
        primary = stringResource(if (last) R.string.training_practice else R.string.training_next), onPrimary = onNext, onBack = onBack,
    )
}

@Composable
private fun QuizPhase(ui: LessonUi, lesson: Lesson, onPickDigit: (Int) -> Unit, onTapCell: (Int) -> Unit, onCheck: () -> Unit, onNext: () -> Unit) {
    val c = SudokuTheme.colors
    val ex = ui.quiz.getOrNull(ui.qIndex) ?: return
    Text(stringResource(R.string.training_question, ui.qIndex + 1, ui.quiz.size), color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    LessonBoard(ex, lesson, reveal = ui.answered, picked = ui.picked, onCell = onTapCell)
    Spacer(Modifier.height(12.dp))
    if (!ui.answered) {
        Surface(shape = RoundedCornerShape(24.dp), color = c.surface, border = BorderStroke(1.dp, c.line), shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(lessonText(Slot.ASK, lesson, ex), color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
                when (lesson.quiz) {
                    QuizKind.PICK_DIGIT -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 1..9) Surface(
                            onClick = { onPickDigit(d) }, shape = RoundedCornerShape(10.dp), color = c.accentSoft,
                            modifier = Modifier.weight(1f).height(56.dp).semantics { contentDescription = d.toString() },
                        ) { Box(contentAlignment = Alignment.Center) { Text(d.toString(), fontSize = 26.sp, fontWeight = FontWeight.Medium, color = c.accent) } }
                    }
                    QuizKind.PICK_ELIMINATIONS -> {
                        Text(stringResource(R.string.training_pick_hint), color = c.muted, fontSize = 13.sp)
                        Button(onClick = onCheck, enabled = ui.picked.isNotEmpty(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(stringResource(R.string.training_check), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    QuizKind.PICK_CELL -> {}
                }
            }
        }
    } else {
        val last = ui.qIndex + 1 >= ui.quiz.size
        StepCard(
            title = stringResource(if (ui.correct) R.string.training_correct else R.string.training_wrong),
            text = lessonText(Slot.S2, lesson, ex) + if (!ui.correct && lesson.quiz == QuizKind.PICK_ELIMINATIONS) "\n" + stringResource(R.string.training_explain_wrong) else "",
            step = 0, total = 0, primary = stringResource(if (last) R.string.training_see_result else R.string.training_next_q), onPrimary = onNext, onBack = null,
        )
    }
}

@Composable
private fun ResultCard(ui: LessonUi, onRetry: () -> Unit, onNextLesson: () -> Unit, onList: () -> Unit) {
    val c = SudokuTheme.colors
    Surface(shape = RoundedCornerShape(24.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.training_result_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Row { repeat(3) { i -> Icon(Icons.Filled.Star, contentDescription = null, tint = if (i < ui.score) c.accent else c.line, modifier = Modifier.size(40.dp)) } }
            Text(stringResource(R.string.training_result_score, ui.score, ui.quiz.size), color = c.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (ui.nextLessonId != null) Button(onClick = onNextLesson, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.training_next_lesson), fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.training_retry)) }
            OutlinedButton(onClick = onList, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.training_all_lessons)) }
        }
    }
}
