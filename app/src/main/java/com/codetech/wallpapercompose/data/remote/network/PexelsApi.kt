package com.codetech.wallpapercompose.data.remote.network

import com.codetech.wallpapercompose.SecretKeys
import com.codetech.wallpapercompose.data.remote.model.Photo
import com.codetech.wallpapercompose.data.remote.model.PhotoResponse
import com.codetech.wallpapercompose.data.remote.model.VideoResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface PexelsApi {
    @Headers("Authorization:${SecretKeys.PEXELS_API_KEY}")
    @GET("v1/search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 80
    ): PhotoResponse

    @Headers("Authorization:${SecretKeys.PEXELS_API_KEY}")
    @GET("v1/photos/{id}")
    suspend fun getPhotoById(@Path("id") id: Int): Photo

    @Headers("Authorization:${SecretKeys.PEXELS_API_KEY}")
    @GET("videos/search")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 80
    ): VideoResponse

    companion object{
        private const val BASE_URL = "https://api.pexels.com/"

        fun create(): PexelsApi{
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PexelsApi::class.java)
        }
    }
}