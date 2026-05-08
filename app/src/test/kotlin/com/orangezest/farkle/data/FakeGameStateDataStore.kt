package com.orangezest.farkle.data

import androidx.datastore.core.DataStore
import com.orangezest.farkle.data.proto.SavedGameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeGameStateDataStore : DataStore<SavedGameState> {
    private val _data = MutableStateFlow(SavedGameState.getDefaultInstance())
    override val data: Flow<SavedGameState> = _data

    override suspend fun updateData(transform: suspend (t: SavedGameState) -> SavedGameState): SavedGameState {
        _data.update { transform(it) }
        return _data.value
    }
}
