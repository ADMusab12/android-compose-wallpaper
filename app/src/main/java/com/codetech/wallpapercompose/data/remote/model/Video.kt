package com.codetech.wallpapercompose.data.remote.model

import com.google.gson.annotations.SerializedName

data class Video(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val image: String,
    val duration: Int,
    val user: User,
    @SerializedName("video_files") val videoFiles: List<VideoFile>
)
