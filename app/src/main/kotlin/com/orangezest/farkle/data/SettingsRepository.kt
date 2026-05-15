package com.orangezest.farkle.data

import com.orangezest.farkle.engine.GameConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface SettingsStore {
    val settings: Flow<Map<String, Any>>
    suspend fun update(key: String, value: Any)
}

class InMemorySettingsStore : SettingsStore {
    private val _settings = MutableStateFlow<Map<String, Any>>(emptyMap())
    override val settings: Flow<Map<String, Any>> = _settings

    override suspend fun update(key: String, value: Any) {
        _settings.update { it + (key to value) }
    }
}

class SettingsRepository(private val store: SettingsStore) {

    val gameConfig: Flow<GameConfig> = store.settings.map { prefs ->
        GameConfig(
            targetScore = prefs["target_score"] as? Int ?: GameConfig.DEFAULT.targetScore,
            minimumToBoard = prefs["minimum_to_board"] as? Int ?: GameConfig.DEFAULT.minimumToBoard,
            hotDiceEnabled = prefs["hot_dice"] as? Boolean ?: GameConfig.DEFAULT.hotDiceEnabled,
            piggybackingEnabled = prefs["piggybacking"] as? Boolean ?: GameConfig.DEFAULT.piggybackingEnabled,
            single1Points = prefs["single_1_points"] as? Int ?: GameConfig.DEFAULT.single1Points,
            single5Points = prefs["single_5_points"] as? Int ?: GameConfig.DEFAULT.single5Points,
            three1sPoints = prefs["three_1s_points"] as? Int ?: GameConfig.DEFAULT.three1sPoints,
            three2sPoints = prefs["three_2s_points"] as? Int ?: GameConfig.DEFAULT.three2sPoints,
            three3sPoints = prefs["three_3s_points"] as? Int ?: GameConfig.DEFAULT.three3sPoints,
            three4sPoints = prefs["three_4s_points"] as? Int ?: GameConfig.DEFAULT.three4sPoints,
            three5sPoints = prefs["three_5s_points"] as? Int ?: GameConfig.DEFAULT.three5sPoints,
            three6sPoints = prefs["three_6s_points"] as? Int ?: GameConfig.DEFAULT.three6sPoints,
            fourOfAKindPoints = prefs["four_of_a_kind_points"] as? Int ?: GameConfig.DEFAULT.fourOfAKindPoints,
            fiveOfAKindPoints = prefs["five_of_a_kind_points"] as? Int ?: GameConfig.DEFAULT.fiveOfAKindPoints,
            sixOfAKindPoints = prefs["six_of_a_kind_points"] as? Int ?: GameConfig.DEFAULT.sixOfAKindPoints,
            straightPoints = prefs["straight_points"] as? Int ?: GameConfig.DEFAULT.straightPoints,
            threePairsPoints = prefs["three_pairs_points"] as? Int ?: GameConfig.DEFAULT.threePairsPoints,
            fourPlusAPairPoints = prefs["four_plus_a_pair_points"] as? Int ?: GameConfig.DEFAULT.fourPlusAPairPoints,
            twoTripletsPoints = prefs["two_triplets_points"] as? Int ?: GameConfig.DEFAULT.twoTripletsPoints,
        )
    }

    val soundEnabled: Flow<Boolean> = store.settings.map { prefs ->
        prefs["sound_enabled"] as? Boolean ?: true
    }

    val hapticEnabled: Flow<Boolean> = store.settings.map { prefs ->
        prefs["haptic_enabled"] as? Boolean ?: true
    }

    val themeMode: Flow<String> = store.settings.map { prefs ->
        prefs["theme_mode"] as? String ?: "system"
    }

    val diceStayInPlace: Flow<Boolean> = store.settings.map { prefs ->
        prefs["dice_stay_in_place"] as? Boolean ?: true
    }

    suspend fun updateTargetScore(value: Int) = store.update("target_score", value)
    suspend fun updateMinimumToBoard(value: Int) = store.update("minimum_to_board", value)
    suspend fun updateHotDice(enabled: Boolean) = store.update("hot_dice", enabled)
    suspend fun updatePiggybacking(enabled: Boolean) = store.update("piggybacking", enabled)
    suspend fun updateSoundEnabled(enabled: Boolean) = store.update("sound_enabled", enabled)
    suspend fun updateHapticEnabled(enabled: Boolean) = store.update("haptic_enabled", enabled)
    suspend fun updateThemeMode(mode: String) = store.update("theme_mode", mode)
    suspend fun updateDiceStayInPlace(enabled: Boolean) = store.update("dice_stay_in_place", enabled)
}
