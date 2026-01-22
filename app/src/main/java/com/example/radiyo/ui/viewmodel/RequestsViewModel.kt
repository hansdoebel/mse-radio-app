package com.example.radiyo.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiyo.data.model.Song
import com.example.radiyo.data.model.SongRequest
import com.example.radiyo.data.repository.NowPlayingRepository
import com.example.radiyo.data.repository.PlaylistsRepository
import com.example.radiyo.data.repository.SongRequestsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "RequestsViewModel"

class RequestsViewModel : ViewModel() {
    private val repository = SongRequestsRepository.getInstance()
    private val playlistsRepository = PlaylistsRepository.getInstance()
    private val nowPlayingRepository = NowPlayingRepository.getInstance()

    val myRequests: StateFlow<List<SongRequest>> = repository.myRequests
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    private val _availableSongs = MutableStateFlow<List<Song>>(emptyList())
    val availableSongs: StateFlow<List<Song>> = _availableSongs.asStateFlow()

    private val _songsLoading = MutableStateFlow(false)
    val songsLoading: StateFlow<Boolean> = _songsLoading.asStateFlow()

    private val _submitSuccess = MutableStateFlow<Boolean?>(null)
    val submitSuccess: StateFlow<Boolean?> = _submitSuccess.asStateFlow()

    val currentPlaylistName: StateFlow<String?> = MutableStateFlow(null)

    init {
        Log.d(TAG, "RequestsViewModel init")
        loadRequests()
        loadSongsFromCurrentPlaylist()
        viewModelScope.launch {
            nowPlayingRepository.nowPlaying.collect { nowPlaying ->
                val playlistId = nowPlaying?.playlist?.id
                if (playlistId != null) {
                    Log.d(TAG, "Playlist changed to: ${nowPlaying.playlist?.name}, reloading songs")
                    val songs = playlistsRepository.fetchSongsInPlaylist(playlistId)
                    _availableSongs.value = songs
                }
            }
        }
    }

    fun loadRequests() {
        Log.d(TAG, "loadRequests called")
        viewModelScope.launch {
            repository.subscribeToMyRequests()
        }
    }

    fun loadSongsFromCurrentPlaylist() {
        viewModelScope.launch {
            _songsLoading.value = true
            val nowPlaying = nowPlayingRepository.nowPlaying.value
            val playlistId = nowPlaying?.playlist?.id
            if (playlistId != null) {
                val songs = playlistsRepository.fetchSongsInPlaylist(playlistId)
                _availableSongs.value = songs
            } else {
                _availableSongs.value = emptyList()
            }
            _songsLoading.value = false
        }
    }

    fun submitRequest(songTitle: String, artistName: String?) {
        viewModelScope.launch {
            val success = repository.submitRequest(songTitle, artistName)
            _submitSuccess.value = success
        }
    }

    fun submitSongRequest(song: Song) {
        submitRequest(song.title, song.artist)
    }

    fun clearSubmitStatus() {
        _submitSuccess.value = null
    }
}
