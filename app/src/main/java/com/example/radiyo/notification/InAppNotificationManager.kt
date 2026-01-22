package com.example.radiyo.notification

import android.util.Log
import com.example.radiyo.data.model.InAppNotification
import com.example.radiyo.data.model.UserRole
import com.example.radiyo.data.repository.RatingsRepository
import com.example.radiyo.data.repository.SongRequestsRepository
import com.example.radiyo.data.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val TAG = "InAppNotificationManager"
private const val AUTO_DISMISS_DELAY_MS = 4000L

class InAppNotificationManager private constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val ratingsRepository = RatingsRepository.getInstance()
    private val requestsRepository = SongRequestsRepository.getInstance()
    private val userRepository = UserRepository.getInstance()

    private val _currentNotification = MutableStateFlow<InAppNotification?>(null)
    val currentNotification: StateFlow<InAppNotification?> = _currentNotification.asStateFlow()

    private val notificationQueue = mutableListOf<InAppNotification>()
    private var isShowingNotification = false

    private var knownRequestIds: MutableSet<String> = mutableSetOf()
    private var hasInitializedRequests = false
    private var isListening = false

    fun startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening, skipping")
            return
        }
        isListening = true
        Log.d(TAG, "Starting notification listeners")

        scope.launch {
            ratingsRepository.newRatingEvent.collect { rating ->
                val currentUser = userRepository.currentUser.value
                if (currentUser?.role == UserRole.MODERATOR) {
                    Log.d(TAG, "New rating received: ${rating.id}")
                    val notification = InAppNotification.NewRating(
                        id = rating.id,
                        userName = rating.userName,
                        targetType = rating.targetType,
                        targetName = rating.targetName,
                        value = rating.value
                    )
                    enqueueNotification(notification)
                }
            }
        }

        scope.launch {
            requestsRepository.pendingRequests.collect { requests ->
                val currentUser = userRepository.currentUser.value
                if (currentUser?.role != UserRole.MODERATOR) return@collect

                val currentIds = requests.map { it.id }.toSet()

                if (!hasInitializedRequests) {
                    knownRequestIds.addAll(currentIds)
                    hasInitializedRequests = true
                    Log.d(TAG, "Initialized with ${currentIds.size} existing requests")
                    return@collect
                }

                val newRequests = requests.filter { it.id !in knownRequestIds }

                newRequests.forEach { request ->
                    Log.d(TAG, "New request detected: ${request.id}")
                    val notification = InAppNotification.NewSongRequest(
                        id = request.id,
                        userName = request.userName,
                        songTitle = request.songTitle,
                        artistName = request.artistName
                    )
                    enqueueNotification(notification)
                    knownRequestIds.add(request.id)
                }
            }
        }
    }

    private fun enqueueNotification(notification: InAppNotification) {
        notificationQueue.add(notification)
        if (!isShowingNotification) {
            showNextNotification()
        }
    }

    private fun showNextNotification() {
        if (notificationQueue.isEmpty()) {
            isShowingNotification = false
            return
        }

        isShowingNotification = true
        val notification = notificationQueue.removeAt(0)
        _currentNotification.value = notification

        scope.launch {
            delay(AUTO_DISMISS_DELAY_MS)
            if (_currentNotification.value?.id == notification.id) {
                _currentNotification.value = null
            }
            showNextNotification()
        }
    }

    fun dismissNotification() {
        _currentNotification.value = null
        showNextNotification()
    }

    fun reset() {
        knownRequestIds.clear()
        hasInitializedRequests = false
        notificationQueue.clear()
        _currentNotification.value = null
        isShowingNotification = false
        isListening = false
    }

    companion object {
        @Volatile
        private var instance: InAppNotificationManager? = null

        fun getInstance(): InAppNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: InAppNotificationManager().also { instance = it }
            }
        }
    }
}
