package com.codetech.wallpapercompose.data.remote.model

import com.google.gson.annotations.SerializedName

data class VideoResponse(
    @SerializedName("videos")
    val videos: List<Video>,
    @SerializedName("next_page")
    val nextPage: String? = null
)
