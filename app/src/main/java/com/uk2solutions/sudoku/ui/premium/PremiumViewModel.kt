package com.uk2solutions.sudoku.ui.premium

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.billing.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PremiumMessage { RESTORED, RESTORE_NONE, ERROR, PROMO_EMPTY, PROMO_APPLIED, PROMO_NOT_CONFIRMED, OPEN_FAILED }

data class PremiumUi(val price: String? = null, val priceLoaded: Boolean = false, val message: PremiumMessage? = null)

@HiltViewModel
class PremiumViewModel @Inject constructor(private val manager: PremiumManager) : ViewModel() {
    val premium: StateFlow<Boolean> = manager.isPremium.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val pending: StateFlow<Boolean> = manager.pending

    private val _ui = MutableStateFlow(PremiumUi())
    val ui: StateFlow<PremiumUi> = _ui.asStateFlow()

    /** 앱 밖(Play 스토어)에서 코드 상환을 시작했는지. 복귀 시 결과 안내에 쓴다. 코드 자체는 보관하지 않는다. */
    private var awaitingRedeem = false

    init { loadPrice() }

    private fun loadPrice() {
        viewModelScope.launch {
            val p = manager.priceText()
            _ui.update { it.copy(price = p, priceLoaded = true) }
        }
    }

    fun buy(activity: android.app.Activity) {
        viewModelScope.launch {
            if (!manager.launchPurchase(activity)) _ui.update { it.copy(message = PremiumMessage.ERROR) }
        }
    }

    fun restore() {
        viewModelScope.launch {
            val ok = manager.refresh()
            val prem = manager.isPremium.first()
            _ui.update { it.copy(message = when { !ok -> PremiumMessage.ERROR; prem -> PremiumMessage.RESTORED; else -> PremiumMessage.RESTORE_NONE }) }
        }
    }

    /**
     * 초청코드 적용(SPEC §7.3). 코드는 공백만 제거해 Play 상환 링크(https://play.google.com/redeem?code=…)로 넘긴다.
     * 유효성은 Play가 판단하고, 코드는 로그·저장소에 남기지 않는다.
     */
    fun applyCode(raw: String, context: Context) {
        val code = raw.filter { !it.isWhitespace() }
        if (code.isEmpty()) { _ui.update { it.copy(message = PremiumMessage.PROMO_EMPTY) }; return }
        val uri = Uri.parse("https://play.google.com/redeem?code=" + Uri.encode(code))
        val viaPlay = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = try { context.startActivity(viaPlay); true } catch (_: ActivityNotFoundException) {
            try { context.startActivity(fallback); true } catch (_: ActivityNotFoundException) { false }
        }
        if (opened) { awaitingRedeem = true; _ui.update { it.copy(message = null) } }
        else _ui.update { it.copy(message = PremiumMessage.OPEN_FAILED) }
    }

    /** 화면 복귀(ON_RESUME): 보유 구매를 조회하고, 코드 상환 직후라면 결과를 안내한다. */
    fun onResumed() {
        viewModelScope.launch {
            manager.refresh()
            if (_ui.value.price == null) loadPrice()
            if (awaitingRedeem) {
                awaitingRedeem = false
                val prem = manager.isPremium.first()
                _ui.update { it.copy(message = if (prem) PremiumMessage.PROMO_APPLIED else PremiumMessage.PROMO_NOT_CONFIRMED) }
            }
        }
    }
}
