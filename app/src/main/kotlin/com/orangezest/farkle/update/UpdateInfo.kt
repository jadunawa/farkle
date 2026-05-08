package com.orangezest.farkle.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val body: String,
    val assets: List<GitHubAsset>,
)

@Serializable
data class GitHubAsset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val name: String,
)

data class UpdateAvailable(
    val remoteVersionCode: Int,
    val downloadUrl: String,
)
