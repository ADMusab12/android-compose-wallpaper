package com.codetech.wallpapercompose.presentation.screens.video

import android.R.attr.category
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codetech.wallpapercompose.data.remote.model.Video
import com.codetech.wallpapercompose.data.remote.network.PexelsApi
import com.codetech.wallpapercompose.presentation.screens.wallpaper.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoViewModel(private val api: PexelsApi = PexelsApi.create()): ViewModel() {
    private val _videos = MutableStateFlow<Resource<List<Video>>>(Resource.Loading)
    val videos = _videos.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Nature")
    val selectedCategory = _selectedCategory.asStateFlow()

    fun fetchVideosByCategory(displayName: String, query: String) {
        viewModelScope.launch {
            _videos.value = Resource.Loading
            try {
                val response = api.searchVideos(query = query)
                _videos.value = Resource.Success(response.videos)
                _selectedCategory.value = displayName
            } catch (e: Exception) {
                _videos.value = Resource.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}