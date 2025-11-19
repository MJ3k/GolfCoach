package com.example.golfcoach

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.TextButton
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.golfcoach.ui.theme.LoginScreen



class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. 记录当前登录的 userId（Null 表示还没登录）
            var currentUserId by remember { mutableStateOf<Int?>(null) }

            // 2. 根据是否登录展示不同界面
            if (currentUserId == null) {
                LoginScreen { userId ->
                    currentUserId = userId
                }
            } else {
                GolfCoachUploadPreview(
                    onBack = {
                        currentUserId = null
                    }
                )
            }
        }
    }
}

data class FileInfo(val name: String, val sizeMB: Double)

@OptIn(UnstableApi::class)
@Composable
fun GolfCoachUploadPreview(onBack:() -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var fileInfo by remember { mutableStateOf<FileInfo?>(null) }

    // Android 13+ Photo Picker(Videos only)
    val picker13 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            videoUri = uri
            fileInfo = uri?.let { queryFileInfo(context.contentResolver, it) }
        }
    )

    // File picker choose file
    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            videoUri = uri
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                } catch (_: Exception) { }
            }
            fileInfo = uri?.let { queryFileInfo(context.contentResolver, it) }
        }
    )

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            ) {
                TextButton(onClick = { onBack() }) {
                    Text("← Back")
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "GolfCoach · Upload & Preview",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))

                Text(
                    "Select a swing video for preview; AI analysis will be integrated subsequently.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            picker13.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        } else {
                            openDoc.launch(arrayOf("video/*"))
                        }
                    }) { Text(if (videoUri == null) "Select Video" else "Select again") }

                    if (videoUri != null) {
                        OutlinedButton(onClick = {
                            videoUri = null
                            fileInfo = null
                        }) { Text("Delete") }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoUri == null) {
                        Text("Video is not selected", color = Color.White)
                    } else {
                        VideoPlayer(uri = videoUri!!)
                    }
                }

                if (fileInfo != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${fileInfo!!.name}  •  ${"%.2f".format(fileInfo!!.sizeMB)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(12.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Analysis(Occupied)", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        if (videoUri == null) {
                            Text("Please Select Video First.", color = Color.Gray)
                        } else {
                            Text("Waiting for AI analysis…(Milestone 2 with backend)",
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Button(enabled = false, onClick = {}) { Text("Analyze(Coming Soon)") }
                        }
                    }
                }
            }
        }
    }
}

// get file name and size
private fun queryFileInfo(cr: ContentResolver, uri: Uri): FileInfo {
    var name = "video"
    var size = 0L
    val cursor: Cursor? = cr.query(uri, null, null, null, null)
    cursor?.use {
        val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
        if (it.moveToFirst()) {
            if (nameIdx >= 0) name = it.getString(nameIdx) ?: name
            if (sizeIdx >= 0) size = it.getLong(sizeIdx)
        }
    }
    return FileInfo(name, size / 1024.0 / 1024.0)
}

@OptIn(UnstableApi::class)
@Composable

// ExoPlayer player to play video
fun VideoPlayer(uri: Uri) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        }
    )
}
