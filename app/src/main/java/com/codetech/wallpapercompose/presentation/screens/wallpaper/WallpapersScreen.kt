package com.codetech.wallpapercompose.presentation.screens.wallpaper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codetech.wallpapercompose.SecretKeys.categoriesList
import com.codetech.wallpapercompose.data.remote.model.Photo
import com.codetech.wallpapercompose.presentation.components.ShimmerGrid

@Composable
fun WallpapersScreen(navController: NavHostController,viewModel: WallpaperViewModel = viewModel()){
    val photosState by viewModel.photos.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()


    Column(modifier = Modifier.fillMaxSize()) {
        // Category chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoriesList) { (displayName, query) ->
                FilterChip(
                    selected = selectedCategory == displayName,
                    onClick = {
                        viewModel.fetchPhotosByCategory(displayName,query)

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

        // Main content
        Box(modifier = Modifier.weight(1f)) {
            when (val state = photosState) {
                is Resource.Loading -> ShimmerGrid()
                is Resource.Success -> {
                    val photos = state.data
                    if (photos.isNotEmpty()) {
                        PhotoGrid(photos, navController)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No photos found in this category",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${state.message ?: "Unknown error"}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchPhotosByCategory(categoriesList.first().first,categoriesList.first().second)
    }
}

@Composable
fun PhotoGrid(photos: List<Photo>,navController: NavHostController){
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(photos){ photo->
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(0.75f)
                    .clickable{
                        navController.navigate("wallpaper_detail/${photo.id}")
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.src.medium)
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.alt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

