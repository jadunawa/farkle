package com.orangezest.farkle.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orangezest.farkle.viewmodel.UpdateUiState
import com.orangezest.farkle.viewmodel.UpdateViewModel
import java.io.File

@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val current = state) {
        is UpdateUiState.Idle -> {}

        is UpdateUiState.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismiss() },
                title = { Text("Update Available") },
                text = { Text("A newer debug build (#${current.info.remoteVersionCode}) is available.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.onUserAcceptsUpdate(current.info) }) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismiss() }) {
                        Text("Later")
                    }
                },
            )
        }

        is UpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Downloading...") },
                text = { CircularProgressIndicator() },
                confirmButton = {},
            )
        }

        is UpdateUiState.ReadyToInstall -> {
            LaunchedEffect(current.apkFile) {
                launchInstaller(context, current.apkFile)
                viewModel.dismiss()
            }
        }

        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismiss() },
                title = { Text("Update Failed") },
                text = { Text(current.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismiss() }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

private fun launchInstaller(context: Context, apkFile: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
