package com.codetech.wallpapercompose.presentation.screens.wallpaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codetech.wallpapercompose.data.remote.model.Photo
import com.codetech.wallpapercompose.data.remote.network.PexelsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Resource<out T>{
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String?): Resource<Nothing>()
}

class WallpaperViewModel(private val api: PexelsApi = PexelsApi.create()): ViewModel() {
    private val _photos = MutableStateFlow<Resource<List<Photo>>>(Resource.Loading)
    val photos = _photos.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Nature")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedPhoto = MutableStateFlow<Resource<Photo>>(Resource.Loading)
    val selectedPhoto = _selectedPhoto.asStateFlow()

    fun fetchPhotosByCategory(displayName: String, query: String) {
        viewModelScope.launch {
            _photos.value = Resource.Loading
            try {
                val response = api.searchPhotos(query = query, perPage = 80)
                _photos.value = Resource.Success(response.photos)
                _selectedCategory.value = displayName
            } catch (e: Exception) {
                _photos.value = Resource.Error(e.message ?: "Failed to load photos")
            }
        }
    }

    fun fetchPhotoById(photoId: Int) {
        viewModelScope.launch {
            _selectedPhoto.value = Resource.Loading
            try {
                val photo = api.getPhotoById(photoId)
                _selectedPhoto.value = Resource.Success(photo)
            } catch (e: Exception) {
                _selectedPhoto.value = Resource.Error(e.message ?: "Failed to load photo details")
            }
        }
    }
}