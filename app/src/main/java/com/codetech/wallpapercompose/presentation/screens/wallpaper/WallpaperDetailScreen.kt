package com.codetech.wallpapercompose.presentation.screens.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codetech.wallpapercompose.data.remote.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperDetailScreen(
    photoId: Int,
    navController: NavHostController,
    onBack: () -> Unit,
    viewModel: WallpaperViewModel = viewModel()
) {
    val photoState by viewModel.selectedPhoto.collectAsState()

    LaunchedEffect(photoId) {
        if (photoState !is Resource.Success) {
            viewModel.fetchPhotoById(photoId)
        }
    }

    when (val state = photoState) {
        is Resource.Success -> {
            WallpaperDetailContent(
                photo = state.data,
                onBack = onBack
            )
        }
        is Resource.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is Resource.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error loading photo\n${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun WallpaperDetailContent(photo: Photo, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSetting by remember { mutableStateOf(false) }
    var settingFor by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blur background
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.src.large)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
        )
    }

    // Dark gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.4f),
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Set Wallpaper",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(45.dp))
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Main preview
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 15.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.src.large)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.alt,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Attribution
        Text(
            text = "Photo by ${photo.photographer} on Pexels",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Action Buttons
        AnimatedVisibility(
            visible = !isSetting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Set on home screen
                Button(
                    onClick = {
                        isSetting = true
                        settingFor = "Home Screen"
                        coroutineScope.launch {
                            setWallpaper(
                                context = context,
                                photo = photo,
                                which = WallpaperManager.FLAG_SYSTEM
                            )
                            isSetting = false
                            settingFor = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Set On Home Screen",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Set on lock screen
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            isSetting = true
                            settingFor = "Lock Screen"
                            coroutineScope.launch {
                                setWallpaper(
                                    context = context,
                                    photo = photo,
                                    which = WallpaperManager.FLAG_LOCK
                                )
                                isSetting = false
                                settingFor = ""
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Lock screen not supported on this device",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Set On Lock Screen",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Set on both
                Button(
                    onClick = {
                        isSetting = true
                        settingFor = "Both Screens"
                        coroutineScope.launch {
                            setWallpaper(
                                context = context,
                                photo = photo,
                                which = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            )
                            isSetting = false
                            settingFor = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Set On Both",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Loading indicator
        AnimatedVisibility(visible = isSetting) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Setting wallpaper for $settingFor...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private suspend fun setWallpaper(context: Context, photo: Photo, which: Int) {
    val wallpaperManager = WallpaperManager.getInstance(context)
    withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(photo.src.portrait)
                .allowHardware(false)
                .build()
            val result = ImageLoader(context).execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap

            if (bitmap != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, which)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }

                withContext(Dispatchers.Main) {
                    val msg = when (which) {
                        WallpaperManager.FLAG_SYSTEM -> "Home screen updated!"
                        WallpaperManager.FLAG_LOCK -> "Lock screen updated!"
                        else -> "Wallpaper set on both screens!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Failed to set wallpaper: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}