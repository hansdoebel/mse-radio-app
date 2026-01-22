package com.example.radiyo.data.repository

import android.util.Log
import com.example.radiyo.data.model.Playlist
import com.example.radiyo.data.model.Song
import com.example.radiyo.data.remote.ConvexConfig
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

private const val TAG = "PlaylistsRepository"

@Serializable
data class PlaylistWithId(
    @kotlinx.serialization.SerialName("_id") val id: String,
    val name: String,
    val description: String? = null
)

class PlaylistsRepository private constructor() {
    private val client = ConvexConfig.client

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _songsInPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val songsInPlaylist: StateFlow<List<Song>> = _songsInPlaylist.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun subscribeToPlaylists() {
        _isLoading.value = true
        _error.value = null
        Log.d(TAG, "Starting playlists subscription")

        try {
            client.subscribe<List<PlaylistWithId>>("playlists:list").collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "Received ${data.size} playlists")
                    _playlists.value = data.map { Playlist(id = it.id, name = it.name) }
                    _isLoading.value = false
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch playlists: ${e.message}", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToPlaylists: ${e.message}", e)
            _error.value = e.message
            _isLoading.value = false
        }
    }

    suspend fun subscribeToSongsInPlaylist(playlistId: String) {
        _isLoading.value = true
        _error.value = null
        Log.d(TAG, "Subscribing to songs for playlist: $playlistId")

        try {
            client.subscribe<List<Song>>(
                "playlists:getSongsInPlaylist",
                args = mapOf("playlistId" to playlistId)
            ).collect { result ->
                result.onSuccess { data ->
                    Log.d(TAG, "Received ${data.size} songs in playlist")
                    _songsInPlaylist.value = data
                    _isLoading.value = false
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch songs in playlist: ${e.message}", e)
                    _error.value = e.message
                    _songsInPlaylist.value = emptyList()
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToSongsInPlaylist: ${e.message}", e)
            _error.value = e.message
            _songsInPlaylist.value = emptyList()
            _isLoading.value = false
        }
    }

    fun clearSongsInPlaylist() {
        _songsInPlaylist.value = emptyList()
    }

    suspend fun fetchSongsInPlaylist(playlistId: String): List<Song> {
        Log.d(TAG, "Fetching songs for playlist: $playlistId")
        return try {
            val queryResult = client.subscribe<List<Song>>(
                "playlists:getSongsInPlaylist",
                args = mapOf("playlistId" to playlistId)
            ).first()
            queryResult.getOrElse { e ->
                Log.e(TAG, "Failed to fetch songs in playlist: ${e.message}", e)
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching songs in playlist: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        @Volatile
        private var instance: PlaylistsRepository? = null

        fun getInstance(): PlaylistsRepository {
            return instance ?: synchronized(this) {
                instance ?: PlaylistsRepository().also { instance = it }
            }
        }
    }
}
