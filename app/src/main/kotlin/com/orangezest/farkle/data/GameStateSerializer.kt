package com.orangezest.farkle.data

import androidx.datastore.core.Serializer
import com.orangezest.farkle.data.proto.SavedGameState
import java.io.InputStream
import java.io.OutputStream

object GameStateSerializer : Serializer<SavedGameState> {
    override val defaultValue: SavedGameState = SavedGameState.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SavedGameState =
        SavedGameState.parseFrom(input)

    override suspend fun writeTo(t: SavedGameState, output: OutputStream) =
        t.writeTo(output)
}
