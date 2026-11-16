package com.uk2solutions.sudoku.di

import android.content.Context
import androidx.room.Room
import com.uk2solutions.sudoku.data.AppDatabase
import com.uk2solutions.sudoku.data.GameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase = Room.databaseBuilder(c, AppDatabase::class.java, "sudoku.db").build()

    @Provides fun gameDao(db: AppDatabase): GameDao = db.gameDao()
}
