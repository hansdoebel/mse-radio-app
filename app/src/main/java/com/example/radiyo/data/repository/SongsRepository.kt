package com.example.radiyo.data.repository

import android.util.Log
import com.example.radiyo.data.model.Song
import com.example.radiyo.data.remote.ConvexConfig
import kotlinx.coroutines.flow.*

private const val TAG = "SongsRepository"

class SongsRepository private constructor() {
    private val client = ConvexConfig.client

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun subscribeToSongs() {
        _isLoading.value = true
        _error.value = null

        try {
            Log.d(TAG, "Starting songs subscription")
            client.subscribe<List<Song>>("songs:list").collect { result ->
                result.onSuccess { songList ->
                    Log.d(TAG, "Received ${songList.size} songs")
                    _songs.value = songList
                    _isLoading.value = false
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch songs: ${e.message}", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToSongs: ${e.message}", e)
            _error.value = e.message
            _isLoading.value = false
        }
    }

    companion object {
        @Volatile
        private var instance: SongsRepository? = null

        fun getInstance(): SongsRepository {
            return instance ?: synchronized(this) {
                instance ?: SongsRepository().also { instance = it }
            }
        }
    }
}
