package com.codetech.wallpapercompose.service

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

class VideoWallpaperService: WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null
        private var videoPath: String? = null
        private var surfaceWidth: Int = 0
        private var surfaceHeight: Int = 0
        private var isPrepared: Boolean = false
        private var shouldPlay: Boolean = false
        private var lastTimestamp: Long = 0

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "video_timestamp") {
                val newTimestamp = prefs.getLong("video_timestamp", 0)
                if (newTimestamp != lastTimestamp) {
                    Log.d("VideoWallpaper", "New video detected, reloading...")
                    lastTimestamp = newTimestamp
                    videoPath = prefs.getString("video_path", null)

                    // Reinitialize with new video
                    surfaceHolder?.let { holder ->
                        initializeMediaPlayer(holder)
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d("VideoWallpaper", "onCreate called")

            // Get the video path and timestamp
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            videoPath = prefs.getString("video_path", null)
            lastTimestamp = prefs.getLong("video_timestamp", 0)

            // Register preference change listener
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)

            Log.d("VideoWallpaper", "Video path: $videoPath")
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            Log.d("VideoWallpaper", "onSurfaceCreated called")
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d("VideoWallpaper", "onSurfaceChanged: $width x $height")

            surfaceWidth = width
            surfaceHeight = height

            // Initialize MediaPlayer when surface is ready with dimensions
            if (holder != null && videoPath != null) {
                initializeMediaPlayer(holder)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Log.d("VideoWallpaper", "onSurfaceDestroyed called")
            releaseMediaPlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d("VideoWallpaper", "onVisibilityChanged: $visible, isPrepared: $isPrepared")

            shouldPlay = visible

            if (visible) {
                if (isPrepared) {
                    mediaPlayer?.let {
                        if (!it.isPlaying) {
                            it.start()
                            Log.d("VideoWallpaper", "Started playback")
                        }
                    }
                } else {
                    Log.d("VideoWallpaper", "Video not prepared yet, will start when ready")
                }
            } else {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        Log.d("VideoWallpaper", "Paused playback")
                    }
                }
            }
        }

        override fun onDestroy() {
            Log.d("VideoWallpaper", "onDestroy called")

            // Unregister preference listener
            getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(prefsListener)

            releaseMediaPlayer()
            super.onDestroy()
        }

        private fun initializeMediaPlayer(holder: SurfaceHolder) {
            if (videoPath == null) {
                Log.e("VideoWallpaper", "Video path is null")
                return
            }

            try {
                Log.d("VideoWallpaper", "Initializing MediaPlayer with: $videoPath")

                // Release any existing MediaPlayer
                releaseMediaPlayer()

                mediaPlayer = MediaPlayer().apply {
                    // Reset preparation flag
                    isPrepared = false

                    setDataSource(videoPath!!)

                    // Set the surface
                    setSurface(holder.surface)

                    // Configure playback
                    isLooping = true
                    setVolume(0f, 0f)

                    setOnPreparedListener { mp ->
                        Log.d("VideoWallpaper", "MediaPlayer prepared")

                        // Get video dimensions
                        val videoWidth = mp.videoWidth
                        val videoHeight = mp.videoHeight

                        Log.d("VideoWallpaper", "Video size: $videoWidth x $videoHeight")
                        Log.d("VideoWallpaper", "Surface size: $surfaceWidth x $surfaceHeight")

                        // Mark as prepared
                        isPrepared = true

                        // Start playback only if visible
                        if (shouldPlay && isVisible) {
                            mp.start()
                            Log.d("VideoWallpaper", "Started playback after prepare")
                        } else {
                            Log.d("VideoWallpaper", "Not starting - shouldPlay: $shouldPlay, isVisible: $isVisible")
                        }
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.e("VideoWallpaper", "MediaPlayer error: what=$what, extra=$extra")
                        isPrepared = false
                        releaseMediaPlayer()
                        true
                    }

                    setOnVideoSizeChangedListener { mp, width, height ->
                        Log.d("VideoWallpaper", "Video size changed: $width x $height")
                    }

                    setOnInfoListener { mp, what, extra ->
                        Log.d("VideoWallpaper", "MediaPlayer info: what=$what, extra=$extra")
                        false
                    }

                    setOnCompletionListener {
                        Log.d("VideoWallpaper", "Video completed")
                    }

                    // Prepare asynchronously
                    prepareAsync()
                    Log.d("VideoWallpaper", "prepareAsync called")
                }
            } catch (e: Exception) {
                Log.e("VideoWallpaper", "Error initializing MediaPlayer", e)
                isPrepared = false
                releaseMediaPlayer()
            }
        }

        private fun releaseMediaPlayer() {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                    reset()
                    release()
                    Log.d("VideoWallpaper", "MediaPlayer released")
                } catch (e: Exception) {
                    Log.e("VideoWallpaper", "Error releasing MediaPlayer", e)
                }
            }
            mediaPlayer = null
            isPrepared = false
        }
    }
}