package com.example.radiyo.data.repository

import android.util.Log
import com.example.radiyo.data.model.Playlist
import com.example.radiyo.data.model.Song
import kotlinx.coroutines.flow.*

private const val TAG = "QueueManager"

data class QueueItem(
    val song: Song,
    val playlist: Playlist,
    val isFromRequest: Boolean = false,
    val requestId: String? = null
)

data class NowPlayingState(
    val song: Song? = null,
    val playlist: Playlist? = null,
    val isPlaying: Boolean = false,
    val startedAt: Double = 0.0
)

class QueueManager private constructor() {
    private val nowPlayingRepository = NowPlayingRepository.getInstance()
    private val playlistsRepository = PlaylistsRepository.getInstance()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlayingState())
    val nowPlaying: StateFlow<NowPlayingState> = _nowPlaying.asStateFlow()

    fun addToQueueBottom(song: Song, playlist: Playlist, isFromRequest: Boolean = false, requestId: String? = null) {
        Log.d(TAG, "Adding to queue bottom: ${song.title}")
        val newItem = QueueItem(song, playlist, isFromRequest, requestId)
        _queue.value = _queue.value + newItem
    }

    fun addToQueueTop(song: Song, playlist: Playlist, isFromRequest: Boolean = true, requestId: String? = null) {
        Log.d(TAG, "Adding to queue top: ${song.title}")
        val newItem = QueueItem(song, playlist, isFromRequest, requestId)
        _queue.value = listOf(newItem) + _queue.value
    }

    fun removeFromQueue(index: Int) {
        val current = _queue.value.toMutableList()
        if (index in current.indices) {
            Log.d(TAG, "Removing from queue at index: $index")
            current.removeAt(index)
            _queue.value = current
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _nowPlaying.value = NowPlayingState()
    }

    suspend fun playSongNow(song: Song, playlist: Playlist, moderatorId: String?) {
        Log.d(TAG, "Playing song now: ${song.title}")
        nowPlayingRepository.setNowPlaying(song.id, playlist.id, moderatorId)
        _nowPlaying.value = NowPlayingState(
            song = song,
            playlist = playlist,
            isPlaying = true,
            startedAt = System.currentTimeMillis().toDouble()
        )
        populateQueueFromPlaylist(song, playlist)
    }

    suspend fun playNext(moderatorId: String?) {
        val currentQueue = _queue.value
        if (currentQueue.isNotEmpty()) {
            val nextItem = currentQueue.first()
            _queue.value = currentQueue.drop(1)
            Log.d(TAG, "Playing next: ${nextItem.song.title}")

            nowPlayingRepository.setNowPlaying(nextItem.song.id, nextItem.playlist.id, moderatorId)
            _nowPlaying.value = NowPlayingState(
                song = nextItem.song,
                playlist = nextItem.playlist,
                isPlaying = true,
                startedAt = System.currentTimeMillis().toDouble()
            )
        }
    }

    suspend fun playOrQueueRequest(song: Song, playlist: Playlist, requestId: String, moderatorId: String?) {
        if (_nowPlaying.value.song == null) {
            Log.d(TAG, "No song playing, playing request immediately: ${song.title}")
            playSongNow(song, playlist, moderatorId)
        } else {
            Log.d(TAG, "Song playing, adding request to top of queue: ${song.title}")
            addToQueueTop(song, playlist, isFromRequest = true, requestId = requestId)
        }
    }

    suspend fun pausePlaying() {
        nowPlayingRepository.pause()
        _nowPlaying.value = _nowPlaying.value.copy(isPlaying = false)
    }

    suspend fun resumePlaying() {
        nowPlayingRepository.resume()
        _nowPlaying.value = _nowPlaying.value.copy(isPlaying = true)
    }

    private suspend fun populateQueueFromPlaylist(currentSong: Song, playlist: Playlist) {
        val songs = playlistsRepository.fetchSongsInPlaylist(playlist.id)
        val currentIndex = songs.indexOfFirst { it.id == currentSong.id }
        if (currentIndex >= 0) {
            val upcomingSongs = songs.drop(currentIndex + 1)
            val existingRequestItems = _queue.value.filter { it.isFromRequest }
            val newQueueItems = upcomingSongs.map { QueueItem(it, playlist) }
            _queue.value = existingRequestItems + newQueueItems
            Log.d(
                TAG,
                "Populated queue with ${newQueueItems.size} songs from playlist, keeping ${existingRequestItems.size} request items"
            )
        }
    }

    companion object {
        @Volatile
        private var instance: QueueManager? = null

        fun getInstance(): QueueManager {
            return instance ?: synchronized(this) {
                instance ?: QueueManager().also { instance = it }
            }
        }
    }
}
