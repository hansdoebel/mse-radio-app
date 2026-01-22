package com.example.radiyo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiyo.data.model.Playlist
import com.example.radiyo.data.model.RatingTargetType
import com.example.radiyo.data.model.Song
import com.example.radiyo.data.repository.LiveRatingEvent
import com.example.radiyo.data.repository.NowPlayingRepository
import com.example.radiyo.data.repository.PendingRequest
import com.example.radiyo.data.repository.PlaylistsRepository
import com.example.radiyo.data.repository.QueueManager
import com.example.radiyo.data.repository.RatingsRepository
import com.example.radiyo.data.repository.SongRequestsRepository
import com.example.radiyo.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardStats(
    val avgModeratorRating: Double = 0.0,
    val avgPlaylistRating: Double = 0.0,
    val totalRatingsToday: Int = 0
)

class ModeratorDashboardViewModel : ViewModel() {
    private val ratingsRepository = RatingsRepository.getInstance()
    private val requestsRepository = SongRequestsRepository.getInstance()
    private val playlistsRepository = PlaylistsRepository.getInstance()
    private val nowPlayingRepository = NowPlayingRepository.getInstance()
    private val queueManager = QueueManager.getInstance()

    val ratings: StateFlow<List<LiveRatingEvent>> = ratingsRepository.ratings
    val newRatingEvent: SharedFlow<LiveRatingEvent> = ratingsRepository.newRatingEvent

    val pendingRequests: StateFlow<List<PendingRequest>> = requestsRepository.pendingRequests

    val playlists: StateFlow<List<Playlist>> = playlistsRepository.playlists
    val songsInPlaylist: StateFlow<List<Song>> = playlistsRepository.songsInPlaylist

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val nowPlaying = queueManager.nowPlaying
    val queue = queueManager.queue

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        _isLoading.value = true
        viewModelScope.launch {
            nowPlayingRepository.clear()
            queueManager.clearQueue()
        }
        viewModelScope.launch {
            playlistsRepository.subscribeToPlaylists()
        }
        subscribeToUpdates()
        viewModelScope.launch {
            playlists.collect { playlistsList ->
                if (playlistsList.isNotEmpty()) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun subscribeToUpdates() {
        viewModelScope.launch {
            ratingsRepository.subscribeToRatings()
        }
        viewModelScope.launch {
            requestsRepository.subscribeToPendingRequests()
        }
        viewModelScope.launch {
            ratings.collect {
                calculateStats()
            }
        }
    }

    private fun calculateStats() {
        val ratings = ratings.value
        if (ratings.isEmpty()) {
            _stats.value = DashboardStats(
                avgModeratorRating = 0.0,
                avgPlaylistRating = 0.0,
                totalRatingsToday = 0
            )
            return
        }

        val moderatorRatings = ratings.filter { it.targetType == RatingTargetType.MODERATOR }
        val playlistRatings = ratings.filter { it.targetType == RatingTargetType.PLAYLIST }

        _stats.value = DashboardStats(
            avgModeratorRating = if (moderatorRatings.isNotEmpty())
                moderatorRatings.map { it.value }.average() else 0.0,
            avgPlaylistRating = if (playlistRatings.isNotEmpty())
                playlistRatings.map { it.value }.average() else 0.0,
            totalRatingsToday = ratings.size
        )
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        viewModelScope.launch {
            playlistsRepository.subscribeToSongsInPlaylist(playlist.id)
        }
    }

    fun clearPlaylistSelection() {
        _selectedPlaylist.value = null
        playlistsRepository.clearSongsInPlaylist()
    }

    fun stopPlaying() {
        viewModelScope.launch {
            queueManager.pausePlaying()
        }
    }

    fun resumePlaying() {
        viewModelScope.launch {
            queueManager.resumePlaying()
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            val request = pendingRequests.value.find { it.id == requestId }
            requestsRepository.approveRequest(requestId)

            if (request != null) {
                val currentPlaylist = nowPlaying.value.playlist
                if (currentPlaylist != null) {
                    val songs = playlistsRepository.fetchSongsInPlaylist(currentPlaylist.id)
                    val song = songs.find {
                        it.title.equals(request.songTitle, ignoreCase = true) &&
                                (request.artistName == null || it.artist.equals(request.artistName, ignoreCase = true))
                    }
                    if (song != null) {
                        val currentUser = UserRepository.getInstance().currentUser.value
                        queueManager.playOrQueueRequest(song, currentPlaylist, requestId, currentUser?.id)
                    }
                }
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            requestsRepository.rejectRequest(requestId)
        }
    }

    fun removeFromQueue(index: Int) {
        queueManager.removeFromQueue(index)
    }

    fun playNext() {
        viewModelScope.launch {
            val currentUser = UserRepository.getInstance().currentUser.value
            queueManager.playNext(currentUser?.id)
        }
    }
}
