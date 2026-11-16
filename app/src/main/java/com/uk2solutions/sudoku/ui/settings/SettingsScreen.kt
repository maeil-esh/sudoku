package com.uk2solutions.sudoku.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

/** 설정 목록 본체(상태 비저장). 스크린샷에도 쓴다. */
@Composable
fun SettingsBody(
    s: AppSettings, langTag: String, permDenied: Boolean, themeLabels: List<Int>,
    onTheme: () -> Unit, onLang: () -> Unit, onHour: () -> Unit, onNotify: (Boolean) -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    val c = SudokuTheme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.settings_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.ink)

        Card {
            ChoiceRow(stringResource(R.string.set_theme), stringResource(themeLabels[s.themeMode.coerceIn(0, 2)]), onTheme)
            Toggle(R.string.set_sound, R.string.set_sound_desc, s.sound) { v -> onUpdate { it.copy(sound = v) } }
            Toggle(R.string.set_vibration, R.string.set_vibration_desc, s.vibration) { v -> onUpdate { it.copy(vibration = v) } }
            ChoiceRow(stringResource(R.string.set_language), stringResource(langLabel(langTag)), onLang)
        }
        Card {
            Toggle(R.string.set_notify, R.string.set_notify_desc, s.notifyEnabled, onNotify)
            if (permDenied) Text(
                stringResource(R.string.notify_permission_needed), color = c.error, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (s.notifyEnabled) ChoiceRow(stringResource(R.string.set_notify_time), stringResource(R.string.hour_label, s.notifyHour), onHour)
        }
        Card {
            Toggle(R.string.set_mistake_limit, R.string.set_mistake_limit_desc, s.mistakeLimitEnabled) { v -> onUpdate { it.copy(mistakeLimitEnabled = v) } }
            Toggle(R.string.set_number_first, R.string.set_number_first_desc, s.numberFirst) { v -> onUpdate { it.copy(numberFirst = v) } }
            Toggle(R.string.set_auto_next, R.string.set_auto_next_desc, s.autoNextNumber) { v -> onUpdate { it.copy(autoNextNumber = v) } }
            Toggle(R.string.set_auto_remove, R.string.set_auto_remove_desc, s.autoRemoveNotes) { v -> onUpdate { it.copy(autoRemoveNotes = v) } }
            Toggle(R.string.set_show_errors, R.string.set_show_errors_desc, s.showErrors) { v -> onUpdate { it.copy(showErrors = v) } }
        }
        Card {
            Toggle(R.string.set_highlight_area, R.string.set_highlight_area_desc, s.highlightArea) { v -> onUpdate { it.copy(highlightArea = v) } }
            Toggle(R.string.set_highlight_same, R.string.set_highlight_same_desc, s.highlightSame) { v -> onUpdate { it.copy(highlightSame = v) } }
            Toggle(R.string.set_timer, R.string.set_timer_desc, s.showTimer) { v -> onUpdate { it.copy(showTimer = v) } }
            Toggle(R.string.set_progress, R.string.set_progress_desc, s.showProgress) { v -> onUpdate { it.copy(showProgress = v) } }
            Toggle(R.string.set_score, R.string.set_score_desc, s.showScore) { v -> onUpdate { it.copy(showScore = v) } }
            Toggle(R.string.set_score_anim, R.string.set_score_anim_desc, s.scoreAnimation) { v -> onUpdate { it.copy(scoreAnimation = v) } }
            Toggle(R.string.set_complete_anim, R.string.set_complete_anim_desc, s.completeAnimation) { v -> onUpdate { it.copy(completeAnimation = v) } }
            Toggle(R.string.set_remaining, R.string.set_remaining_desc, s.showRemaining) { v -> onUpdate { it.copy(showRemaining = v) } }
        }
        Card {
            val ctx = LocalContext.current
            val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.set_version), color = c.ink, modifier = Modifier.weight(1f))
                Text(version, color = c.muted)
            }
        }
    }

}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = SudokuTheme.colors
    val ctx = LocalContext.current
    var themeDialog by remember { mutableStateOf(false) }
    var langDialog by remember { mutableStateOf(false) }
    var hourDialog by remember { mutableStateOf(false) }
    var permDenied by remember { mutableStateOf(false) }
    var langTag by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permDenied = !granted
        if (granted) vm.update { it.copy(notifyEnabled = true) }
    }
    val themeLabels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)

    SettingsBody(
        s, langTag, permDenied, themeLabels,
        onTheme = { themeDialog = true }, onLang = { langDialog = true }, onHour = { hourDialog = true },
        onNotify = { v ->
            if (!v) { vm.update { it.copy(notifyEnabled = false) }; permDenied = false }
            else if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else vm.update { it.copy(notifyEnabled = true) }
        },
        onUpdate = vm::update,
    )

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text(stringResource(R.string.theme_pick_title)) },
            text = {
                Column {
                    themeLabels.forEachIndexed { i, res ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected = s.themeMode == i, role = Role.RadioButton) { vm.update { it.copy(themeMode = i) }; themeDialog = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.themeMode == i, onClick = null)
                            Text(stringResource(res), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (langDialog) {
        val tags = listOf("", "ko", "en")
        AlertDialog(
            onDismissRequest = { langDialog = false },
            title = { Text(stringResource(R.string.lang_pick_title)) },
            text = {
                Column {
                    tags.forEach { tag ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = langTag == tag, role = Role.RadioButton) {
                                langDialog = false; langTag = tag
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) // 화면이 새 언어로 다시 만들어진다
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = langTag == tag, onClick = null)
                            Text(stringResource(langLabel(tag)), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { langDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (hourDialog) {
        AlertDialog(
            onDismissRequest = { hourDialog = false },
            title = { Text(stringResource(R.string.set_notify_time)) },
            text = {
                Column {
                    listOf(9, 12, 18, 20, 22).forEach { h ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = s.notifyHour == h, role = Role.RadioButton) {
                                vm.update { it.copy(notifyHour = h) }; hourDialog = false
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.notifyHour == h, onClick = null)
                            Text(stringResource(R.string.hour_label, h), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { hourDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

fun langLabel(tag: String) = when {
    tag.startsWith("ko") -> R.string.lang_ko
    tag.startsWith("en") -> R.string.lang_en
    else -> R.string.lang_system
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val c = SudokuTheme.colors
    Surface(shape = RoundedCornerShape(20.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun Toggle(title: Int, desc: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = SudokuTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(desc), color = c.muted, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    val c = SudokuTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = c.muted)
    }
}
