package com.orangezest.farkle.update

import com.orangezest.farkle.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateAvailable? = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) return@withContext null

        try {
            val connection = (URL(RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 5_000
                readTimeout = 5_000
            }

            try {
                if (connection.responseCode != 200) return@withContext null

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val release = json.decodeFromString<GitHubRelease>(responseBody)

                val remoteVersionCode = release.body.trim().toIntOrNull()
                    ?: return@withContext null

                if (remoteVersionCode <= BuildConfig.VERSION_CODE) return@withContext null

                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@withContext null

                UpdateAvailable(
                    remoteVersionCode = remoteVersionCode,
                    downloadUrl = apkAsset.browserDownloadUrl,
                )
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val RELEASE_URL =
            "https://api.github.com/repos/jadunawa/farkle/releases/tags/debug-latest"
    }
}
