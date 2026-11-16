package com.uk2solutions.sudoku.data

import com.uk2solutions.sudoku.domain.challenge.ChallengeCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 다른 앱(메신저)에서 "공유"로 넘어온 도전장 문구를 홈이 받을 때까지 보관한다. 저장소에는 쓰지 않는다. */
@Singleton
class ChallengeInbox @Inject constructor() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(text: String?) { if (ChallengeCode.containsCode(text)) _pending.value = text }
    fun clear() { _pending.value = null }
}
