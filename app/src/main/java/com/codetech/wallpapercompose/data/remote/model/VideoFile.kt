package com.codetech.wallpapercompose.data.remote.model

import com.google.gson.annotations.SerializedName

data class VideoFile(
    val id: Int,
    val quality: String,
    @SerializedName("file_type") val fileType: String,
    val width: Int?,
    val height: Int?,
    val link: String
)