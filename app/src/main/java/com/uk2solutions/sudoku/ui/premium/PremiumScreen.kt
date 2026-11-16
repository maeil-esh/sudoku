package com.uk2solutions.sudoku.ui.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PremiumScreen(onBack: () -> Unit, vm: PremiumViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val premium by vm.premium.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResumed() }
    PremiumContent(
        premium = premium, pending = pending, price = ui.price, priceLoaded = ui.priceLoaded, message = ui.message,
        onBack = onBack, onBuy = { ctx.findActivity()?.let(vm::buy) }, onRestore = vm::restore, onApplyCode = { vm.applyCode(it, ctx) },
    )
}

/** 프리미엄·초청코드 화면(S3)의 상태 비저장 본체. */
@Composable
fun PremiumContent(
    premium: Boolean, pending: Boolean, price: String?, priceLoaded: Boolean, message: PremiumMessage?,
    onBack: () -> Unit, onBuy: () -> Unit, onRestore: () -> Unit, onApplyCode: (String) -> Unit,
) {
    val c = SudokuTheme.colors
    var code by remember { mutableStateOf("") } // 저장·복원하지 않는다(rememberSaveable 아님)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.game_back), tint = c.ink) }
            Text(stringResource(R.string.premium_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.ink)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = c.accentSoft, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.premium_tagline), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = c.ink)
                listOf(R.string.premium_benefit_levels, R.string.premium_benefit_hints, R.string.premium_benefit_stats, R.string.premium_benefit_themes).forEach {
                    Row { Text("✓  ", color = c.accent, fontWeight = FontWeight.Bold); Text(stringResource(it), color = c.ink) }
                }
            }
        }
        if (premium) {
            Text(stringResource(R.string.premium_active), color = c.accent, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        } else {
            Button(
                onClick = onBuy, enabled = price != null, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(if (price != null) stringResource(R.string.premium_buy, price) else stringResource(R.string.premium_price_loading), fontWeight = FontWeight.SemiBold)
            }
            if (priceLoaded && price == null) Text(stringResource(R.string.premium_unavailable), color = c.muted, fontSize = 13.sp)
            if (pending) Text(stringResource(R.string.premium_msg_pending), color = c.accent, fontSize = 13.sp)
        }
        OutlinedButton(onClick = onRestore, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.premium_restore))
        }
        if (!premium) Surface(shape = RoundedCornerShape(20.dp), color = c.surface, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.premium_promo_title), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = c.ink)
                Text(stringResource(R.string.premium_promo_desc), color = c.muted, fontSize = 13.sp)
                OutlinedTextField(
                    value = code, onValueChange = { code = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.premium_promo_hint)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                Button(onClick = { onApplyCode(code) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.premium_promo_apply))
                }
            }
        }
        message?.let {
            Text(
                stringResource(
                    when (it) {
                        PremiumMessage.RESTORED -> R.string.premium_msg_restored
                        PremiumMessage.RESTORE_NONE -> R.string.premium_msg_restore_none
                        PremiumMessage.ERROR -> R.string.premium_msg_error
                        PremiumMessage.PROMO_EMPTY -> R.string.premium_msg_promo_empty
                        PremiumMessage.PROMO_APPLIED -> R.string.premium_msg_promo_applied
                        PremiumMessage.PROMO_NOT_CONFIRMED -> R.string.premium_msg_promo_not_confirmed
                        PremiumMessage.OPEN_FAILED -> R.string.premium_msg_open_failed
                    },
                ),
                color = if (it == PremiumMessage.PROMO_APPLIED || it == PremiumMessage.RESTORED) c.accent else c.error,
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
            )
        }
    }
}
