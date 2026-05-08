package com.orangezest.farkle.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.data.PreferencesSettingsStore
import com.orangezest.farkle.data.SettingsRepository
import com.orangezest.farkle.data.SettingsStore
import android.util.Log
import com.orangezest.farkle.BuildConfig
import com.orangezest.farkle.engine.DiceRoller
import com.orangezest.farkle.engine.GameConfig
import com.orangezest.farkle.engine.GameReducer
import com.orangezest.farkle.engine.ScoringEngine
import com.orangezest.farkle.engine.SecureRandomDiceRoller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    @Provides
    @Singleton
    fun provideDiceRoller(): DiceRoller {
        val roller: DiceRoller = SecureRandomDiceRoller()
        if (!BuildConfig.DEBUG) return roller
        return DiceRoller { count ->
            roller.roll(count).also { result ->
                Log.d("DiceRoller", "Rolled $count dice: $result")
            }
        }
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.preferencesDataStore

    @Provides
    @Singleton
    fun provideSettingsStore(
        dataStore: DataStore<Preferences>,
    ): SettingsStore = PreferencesSettingsStore(dataStore)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        store: SettingsStore,
    ): SettingsRepository = SettingsRepository(store)

    @Provides
    @Singleton
    fun provideGameStateRepository(
        @ApplicationContext context: Context,
    ): GameStateRepository = GameStateRepository.create(context)

    @Provides
    @Singleton
    fun provideScoringEngine(): ScoringEngine = ScoringEngine(GameConfig.DEFAULT)

    @Provides
    @Singleton
    fun provideGameReducer(
        scoringEngine: ScoringEngine,
        diceRoller: DiceRoller,
    ): GameReducer = GameReducer(scoringEngine, diceRoller)
}
