package com.uk2solutions.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.uk2solutions.sudoku.billing.PremiumStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore에 premiumCached / lastVerifiedAt 저장(SPEC §8). 구매 토큰·프로모 코드는 저장하지 않는다. */
@Singleton
class PremiumStoreImpl @Inject constructor(@ApplicationContext private val context: Context) : PremiumStore {
    private val keyPremium = booleanPreferencesKey("premiumCached")
    private val keyVerified = longPreferencesKey("lastVerifiedAt")

    override val premiumCached: Flow<Boolean> = context.dataStore.data.map { it[keyPremium] ?: false }

    override suspend fun save(premium: Boolean, verifiedAt: Long) {
        context.dataStore.edit { it[keyPremium] = premium; it[keyVerified] = verifiedAt }
    }
}
