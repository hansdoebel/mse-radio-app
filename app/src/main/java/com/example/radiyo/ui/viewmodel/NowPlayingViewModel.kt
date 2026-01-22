package com.example.radiyo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiyo.data.model.NowPlaying
import com.example.radiyo.data.model.RatingTargetType
import com.example.radiyo.data.repository.NowPlayingRepository
import com.example.radiyo.data.repository.RatingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "NowPlayingViewModel"

class NowPlayingViewModel : ViewModel() {
    private val nowPlayingRepository = NowPlayingRepository.getInstance()
    private val ratingsRepository = RatingsRepository.getInstance()

    val nowPlaying: StateFlow<NowPlaying?> = nowPlayingRepository.nowPlaying
    val isLoading: StateFlow<Boolean> = nowPlayingRepository.isLoading
    val error: StateFlow<String?> = nowPlayingRepository.error

    private val _playlistRating = MutableStateFlow(0)
    val playlistRating: StateFlow<Int> = _playlistRating.asStateFlow()

    private val _moderatorRating = MutableStateFlow(0)
    val moderatorRating: StateFlow<Int> = _moderatorRating.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var lastRatedPlaylistId: String? = null

    init {
        Log.d(TAG, "NowPlayingViewModel init")
        loadNowPlaying()
        viewModelScope.launch {
            nowPlaying.collect { np ->
                Log.d(TAG, "nowPlaying collected: ${np?.song?.title ?: "null"}")
                _isPlaying.value = np?.isPlaying ?: true

                val currentPlaylistId = np?.playlist?.id
                if (currentPlaylistId != null && currentPlaylistId != lastRatedPlaylistId) {
                    _playlistRating.value = 0
                    _moderatorRating.value = 0
                }
            }
        }
    }

    fun loadNowPlaying() {
        viewModelScope.launch {
            nowPlayingRepository.subscribeToNowPlaying()
        }
    }

    fun ratePlaylist(rating: Int) {
        val currentPlaylist = nowPlaying.value?.playlist ?: return
        _playlistRating.value = rating
        lastRatedPlaylistId = currentPlaylist.id

        viewModelScope.launch {
            ratingsRepository.submitRating(
                targetId = currentPlaylist.id,
                targetType = RatingTargetType.PLAYLIST,
                value = rating
            )
        }
    }

    fun rateModerator(rating: Int) {
        val currentModerator = nowPlaying.value?.moderator ?: return
        _moderatorRating.value = rating

        viewModelScope.launch {
            ratingsRepository.submitRating(
                targetId = currentModerator.id,
                targetType = RatingTargetType.MODERATOR,
                value = rating
            )
        }
    }

    fun submitRatings(playlistRating: Int, moderatorRating: Int) {
        viewModelScope.launch {
            if (playlistRating > 0) {
                ratePlaylist(playlistRating)
            }
            if (moderatorRating > 0) {
                rateModerator(moderatorRating)
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            if (_isPlaying.value) {
                nowPlayingRepository.pause()
            } else {
                nowPlayingRepository.resume()
            }
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            nowPlayingRepository.skipNext()
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            nowPlayingRepository.skipPrevious()
        }
    }
}
