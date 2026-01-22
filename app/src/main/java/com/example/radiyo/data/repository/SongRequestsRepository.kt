package com.example.radiyo.data.repository

import android.util.Log
import com.example.radiyo.data.model.SongRequest
import com.example.radiyo.data.remote.ConvexConfig
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "SongRequestsRepository"

@Serializable
data class PendingRequest(
    @SerialName("_id") val id: String,
    val userId: String,
    val userName: String,
    val songTitle: String,
    val artistName: String?,
    @SerialName("_creationTime") val createdAt: Double = 0.0
)

class SongRequestsRepository private constructor() {
    private val client = ConvexConfig.client

    private val _myRequests = MutableStateFlow<List<SongRequest>>(emptyList())
    val myRequests: StateFlow<List<SongRequest>> = _myRequests.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<PendingRequest>>(emptyList())
    val pendingRequests: StateFlow<List<PendingRequest>> = _pendingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var isSubscribedToMyRequests = false
    private var isSubscribedToPending = false

    suspend fun submitRequest(songTitle: String, artistName: String?): Boolean {
        return try {
            val currentUser = UserRepository.getInstance().currentUser.value
            Log.d(TAG, "Submitting request for '$songTitle' by user: ${currentUser?.id ?: "null"}")
            val args = mutableMapOf<String, Any?>("songTitle" to songTitle)
            artistName?.let { args["artistName"] = it }
            currentUser?.let { args["userId"] = it.id }

            client.mutation<String>("songRequests:submit", args = args)
            Log.d(TAG, "Request submitted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit request: ${e.message}", e)
            _error.value = e.message
            false
        }
    }

    suspend fun subscribeToMyRequests() {
        if (isSubscribedToMyRequests) {
            Log.d(TAG, "Already subscribed to myRequests, skipping")
            return
        }
        isSubscribedToMyRequests = true
        _isLoading.value = true
        _error.value = null
        Log.d(TAG, "Starting myRequests subscription")

        try {
            val currentUser = UserRepository.getInstance().currentUser.value
            Log.d(TAG, "Current user: ${currentUser?.id ?: "null"}")
            val args = mutableMapOf<String, Any?>()
            currentUser?.let { args["userId"] = it.id }

            client.subscribe<List<SongRequest>>(
                "songRequests:getMyRequests",
                args = args
            ).collect { result ->
                result.onSuccess { requests ->
                    Log.d(TAG, "Received ${requests.size} my requests")
                    _myRequests.value = requests
                    _isLoading.value = false
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch my requests: ${e.message}", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToMyRequests: ${e.message}", e)
            _error.value = e.message
            _isLoading.value = false
            isSubscribedToMyRequests = false
        }
    }

    suspend fun subscribeToPendingRequests() {
        if (isSubscribedToPending) {
            Log.d(TAG, "Already subscribed to pendingRequests, skipping")
            return
        }
        isSubscribedToPending = true
        _isLoading.value = true
        _error.value = null
        Log.d(TAG, "Starting pendingRequests subscription")

        try {
            client.subscribe<List<PendingRequest>>("songRequests:getPendingRequests").collect { result ->
                result.onSuccess { requests ->
                    Log.d(TAG, "Received ${requests.size} pending requests")
                    _pendingRequests.value = requests
                    _isLoading.value = false
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch pending requests: ${e.message}", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToPendingRequests: ${e.message}", e)
            _error.value = e.message
            _isLoading.value = false
            isSubscribedToPending = false
        }
    }

    suspend fun approveRequest(requestId: String): Boolean {
        return updateRequestStatus(requestId, "approved")
    }

    suspend fun rejectRequest(requestId: String): Boolean {
        return updateRequestStatus(requestId, "rejected")
    }

    private suspend fun updateRequestStatus(requestId: String, status: String): Boolean {
        return try {
            client.mutation<Unit>(
                "songRequests:updateStatus",
                args = mapOf("requestId" to requestId, "status" to status)
            )
            true
        } catch (e: Exception) {
            _error.value = e.message
            false
        }
    }

    companion object {
        @Volatile
        private var instance: SongRequestsRepository? = null

        fun getInstance(): SongRequestsRepository {
            return instance ?: synchronized(this) {
                instance ?: SongRequestsRepository().also { instance = it }
            }
        }
    }
}
