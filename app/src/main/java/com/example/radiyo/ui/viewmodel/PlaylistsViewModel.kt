package com.example.radiyo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiyo.data.model.Playlist
import com.example.radiyo.data.model.Song
import com.example.radiyo.data.repository.PlaylistsRepository
import com.example.radiyo.data.repository.QueueManager
import com.example.radiyo.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel : ViewModel() {
    private val playlistsRepository = PlaylistsRepository.getInstance()
    private val queueManager = QueueManager.getInstance()

    val playlists: StateFlow<List<Playlist>> = playlistsRepository.playlists
    val isLoading: StateFlow<Boolean> = playlistsRepository.isLoading

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _songsInPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val songsInPlaylist: StateFlow<List<Song>> = _songsInPlaylist.asStateFlow()

    init {
        viewModelScope.launch {
            playlistsRepository.subscribeToPlaylists()
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        viewModelScope.launch {
            val songs = playlistsRepository.fetchSongsInPlaylist(playlist.id)
            _songsInPlaylist.value = songs
        }
    }

    fun clearSelection() {
        _selectedPlaylist.value = null
        _songsInPlaylist.value = emptyList()
    }

    fun playSong(song: Song) {
        val playlist = _selectedPlaylist.value ?: return
        viewModelScope.launch {
            val currentUser = UserRepository.getInstance().currentUser.value
            queueManager.playSongNow(song, playlist, currentUser?.id)
        }
    }

    fun addToQueue(song: Song) {
        val playlist = _selectedPlaylist.value ?: return
        queueManager.addToQueueBottom(song, playlist)
    }
}
