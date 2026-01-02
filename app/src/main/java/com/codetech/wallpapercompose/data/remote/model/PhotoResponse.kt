package com.codetech.wallpapercompose.data.remote.model

data class PhotoResponse(
    val photos: List<Photo>,
    val next_page: String ?= null
)
