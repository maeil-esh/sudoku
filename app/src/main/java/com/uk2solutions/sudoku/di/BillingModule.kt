package com.uk2solutions.sudoku.di

import com.uk2solutions.sudoku.billing.BillingGateway
import com.uk2solutions.sudoku.billing.PlayBillingGateway
import com.uk2solutions.sudoku.billing.PremiumManager
import com.uk2solutions.sudoku.billing.PremiumStore
import com.uk2solutions.sudoku.data.PremiumStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingBindings {
    @Binds abstract fun gateway(impl: PlayBillingGateway): BillingGateway
    @Binds abstract fun store(impl: PremiumStoreImpl): PremiumStore
}

@Module
@InstallIn(SingletonComponent::class)
object BillingProvides {
    @Provides @Singleton
    fun premiumManager(gateway: BillingGateway, store: PremiumStore): PremiumManager =
        PremiumManager(gateway, store).also { it.start(CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
