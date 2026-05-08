package com.orangezest.farkle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.orangezest.farkle.data.proto.SavedGameState
import com.orangezest.farkle.engine.GameUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameStateRepository(
    private val dataStore: DataStore<SavedGameState>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val savedGame: Flow<GameUiState?> = dataStore.data.map { saved ->
        if (saved.jsonState.isEmpty()) null
        else try {
            json.decodeFromString<GameUiState>(saved.jsonState)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveGame(state: GameUiState) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setSchemaVersion(1)
                .setJsonState(json.encodeToString(state))
                .build()
        }
    }

    suspend fun clearSavedGame() {
        dataStore.updateData { SavedGameState.getDefaultInstance() }
    }

    companion object {
        private val Context.gameStateDataStore: DataStore<SavedGameState> by dataStore(
            fileName = "game_state.pb",
            serializer = GameStateSerializer,
        )

        fun create(context: Context): GameStateRepository =
            GameStateRepository(context.gameStateDataStore)
    }
}
