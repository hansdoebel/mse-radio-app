package com.example.radiyo.data.repository

import android.util.Log
import com.example.radiyo.data.model.NowPlaying
import com.example.radiyo.data.remote.ConvexConfig
import kotlinx.coroutines.flow.*

private const val TAG = "NowPlayingRepository"

class NowPlayingRepository private constructor() {
    private val client = ConvexConfig.client

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun subscribeToNowPlaying() {
        _isLoading.value = true
        _error.value = null
        Log.d(TAG, "Starting nowPlaying subscription")

        try {
            client.subscribe<NowPlaying?>("nowPlaying:get").collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "Received nowPlaying: ${data?.song?.title ?: "null"}")
                    _nowPlaying.value = data
                    _isLoading.value = false
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch nowPlaying: ${e.message}", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToNowPlaying: ${e.message}", e)
            _error.value = e.message
            _isLoading.value = false
        }
    }

    suspend fun setNowPlaying(songId: String, playlistId: String, moderatorId: String? = null) {
        try {
            Log.d(TAG, "setNowPlaying called: songId=$songId, playlistId=$playlistId, moderatorId=$moderatorId")
            val args = mutableMapOf<String, Any?>(
                "songId" to songId,
                "playlistId" to playlistId
            )
            if (moderatorId != null) {
                args["moderatorId"] = moderatorId
            }
            client.mutation<Unit>("nowPlaying:update", args = args)
            Log.d(TAG, "setNowPlaying mutation completed")
        } catch (e: Exception) {
            Log.e(TAG, "setNowPlaying failed: ${e.message}", e)
            _error.value = e.message
        }
    }

    suspend fun skipNext() {
        try {
            client.mutation<Unit>("nowPlaying:skipNext", args = emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to skip next: ${e.message}", e)
            _error.value = e.message
        }
    }

    suspend fun skipPrevious() {
        try {
            client.mutation<Unit>("nowPlaying:skipPrevious", args = emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to skip previous: ${e.message}", e)
            _error.value = e.message
        }
    }

    suspend fun pause() {
        try {
            client.mutation<Unit>("nowPlaying:pause", args = emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause: ${e.message}", e)
            _error.value = e.message
        }
    }

    suspend fun resume() {
        try {
            client.mutation<Unit>("nowPlaying:resume", args = emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume: ${e.message}", e)
            _error.value = e.message
        }
    }

    suspend fun clear() {
        try {
            client.mutation<Unit>("nowPlaying:clear", args = emptyMap())
            Log.d(TAG, "Cleared nowPlaying")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear nowPlaying: ${e.message}", e)
            _error.value = e.message
        }
    }

    companion object {
        @Volatile
        private var instance: NowPlayingRepository? = null

        fun getInstance(): NowPlayingRepository {
            return instance ?: synchronized(this) {
                instance ?: NowPlayingRepository().also { instance = it }
            }
        }
    }
}
