package com.codetech.wallpapercompose.presentation.screens.video

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.codetech.wallpapercompose.SecretKeys.categoriesList
import com.codetech.wallpapercompose.data.remote.model.Video
import com.codetech.wallpapercompose.presentation.components.ShimmerGrid
import com.codetech.wallpapercompose.presentation.screens.wallpaper.Resource
import com.codetech.wallpapercompose.service.VideoWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Composable
fun VideosScreen(viewModel: VideoViewModel = viewModel()) {
    val videosState by viewModel.videos.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoriesList) { (displayName, queryName) ->
                FilterChip(
                    selected = selectedCategory == displayName,
                    onClick = {
                        viewModel.fetchVideosByCategory(displayName,queryName)
                    },
                    label = {
                        Text(
                            text = displayName,
                            fontWeight = if (selectedCategory == displayName) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Video Grid
        Box(modifier = Modifier.weight(1f)) {
            when (val state = videosState) {
                is Resource.Loading -> ShimmerGrid()
                is Resource.Success -> {
                    val videos = state.data
                    if (videos.isNotEmpty()) {
                        VideoGrid(
                            videos = videos,
                            onVideoClick = { video ->
                                scope.launch {
                                    setAsVideoWallpaper(
                                        context = context,
                                        video = video,
                                        onProgressUpdate = { progress, status ->
                                            isDownloading = true
                                            downloadProgress = progress
                                            downloadStatus = status
                                        },
                                        onComplete = {
                                            isDownloading = false
                                        }
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No videos found",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try selecting a different category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error loading videos",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }

            // Download Progress Dialog
            if (isDownloading) {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Downloading Video",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(80.dp),
                                strokeWidth = 6.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = downloadStatus,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchVideosByCategory(categoriesList.first().first,categoriesList.first().second)
    }
}

@Composable
fun VideoGrid(videos: List<Video>, onVideoClick: (Video) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(videos) { video ->
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(0.75f)
                    .clickable { onVideoClick(video) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box {
                    AsyncImage(
                        model = video.image,
                        contentDescription = "video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video duration overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

private suspend fun setAsVideoWallpaper(
    context: Context,
    video: Video,
    onProgressUpdate: (Float, String) -> Unit,
    onComplete: () -> Unit
) {
    val hdFile = video.videoFiles.firstOrNull { it.quality == "hd" } ?: run {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "HD video not available", Toast.LENGTH_SHORT).show()
            onComplete()
        }
        return
    }

    withContext(Dispatchers.IO) {
        try {
            onProgressUpdate(0f, "Preparing download...")
            delay(300)

            val client = OkHttpClient()
            val request = Request.Builder().url(hdFile.link).build()

            onProgressUpdate(0.1f, "Connecting to server...")

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val file = File(context.filesDir, "video_wallpaper.mp4")

                // Delete old file if exists
                if (file.exists()) {
                    onProgressUpdate(0.15f, "Removing old video...")
                    file.delete()
                    delay(200)
                }

                val totalBytes = response.body?.contentLength() ?: 0L
                var downloadedBytes = 0L

                onProgressUpdate(0.2f, "Downloading video...")

                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                // Progress from 20% to 80% during download
                                val downloadProgress = (downloadedBytes.toFloat() / totalBytes.toFloat())
                                val overallProgress = 0.2f + (downloadProgress * 0.6f)
                                val mbDownloaded = downloadedBytes / (1024f * 1024f)
                                val mbTotal = totalBytes / (1024f * 1024f)
                                onProgressUpdate(
                                    overallProgress,
                                    "Downloaded %.1f MB / %.1f MB".format(mbDownloaded, mbTotal)
                                )
                            }
                        }
                    }
                }

                onProgressUpdate(0.85f, "Saving video...")
                delay(300)

                // Save path
                val prefs = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("video_path", file.absolutePath)
                    putLong("video_timestamp", System.currentTimeMillis())
                }

                onProgressUpdate(0.9f, "Preparing wallpaper...")

                // Stop any existing wallpaper service
                try {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.clear()
                } catch (e: Exception) {
                    // Ignore if no wallpaper is set
                }

                delay(500)

                onProgressUpdate(1f, "Complete!")
                delay(300)

                // Launch live wallpaper chooser
                withContext(Dispatchers.Main) {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    intent.putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, VideoWallpaperService::class.java)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Select the live wallpaper in the chooser",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    onComplete()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to download video",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete()
            }
        }
    }
}