package com.example.radiyo.data.repository

import android.util.Log
import com.example.radiyo.data.model.RatingStats
import com.example.radiyo.data.model.RatingTargetType
import com.example.radiyo.data.remote.ConvexConfig
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "RatingsRepository"

@Serializable
data class LiveRatingEvent(
    @SerialName("_id") val id: String,
    val userName: String,
    val targetType: RatingTargetType,
    val targetId: String,
    val targetName: String = "Unbekannt",
    val value: Double,
    @SerialName("_creationTime") val timestamp: Double = 0.0
)

class RatingsRepository private constructor() {
    private val client = ConvexConfig.client

    private val _ratings = MutableStateFlow<List<LiveRatingEvent>>(emptyList())
    val ratings: StateFlow<List<LiveRatingEvent>> = _ratings.asStateFlow()

    private val _newRatingEvent = MutableSharedFlow<LiveRatingEvent>()
    val newRatingEvent: SharedFlow<LiveRatingEvent> = _newRatingEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun submitRating(
        targetId: String,
        targetType: RatingTargetType,
        value: Int,
        comment: String? = null
    ): Boolean {
        return try {
            val currentUser = UserRepository.getInstance().currentUser.value
            val args = mutableMapOf<String, Any?>(
                "targetId" to targetId,
                "targetType" to if (targetType == RatingTargetType.PLAYLIST) "playlist" else "moderator",
                "value" to value
            )
            comment?.let { args["comment"] = it }
            currentUser?.let { args["userId"] = it.id }

            client.mutation<String>("ratings:submitRating", args = args)
            true
        } catch (e: Exception) {
            _error.value = e.message
            false
        }
    }

    suspend fun getStats(targetId: String, targetType: RatingTargetType): RatingStats? {
        return try {
            var stats: RatingStats? = null
            client.subscribe<RatingStats>(
                "ratings:getStats",
                args = mapOf(
                    "targetId" to targetId,
                    "targetType" to if (targetType == RatingTargetType.PLAYLIST) "playlist" else "moderator"
                )
            ).collect { result ->
                result.onSuccess { data ->
                    stats = data.copy(targetId = targetId)
                    return@collect
                }
            }
            stats
        } catch (e: Exception) {
            _error.value = e.message
            null
        }
    }

    suspend fun subscribeToRatings(limit: Int = 20) {
        _isLoading.value = true
        _error.value = null
        Log.d(TAG, "Starting Ratings subscription")

        try {
            client.subscribe<List<LiveRatingEvent>>(
                "ratings:getRatings",
                args = mapOf("limit" to limit)
            ).collect { result ->
                result.onSuccess { ratings ->
                    Log.d(TAG, "Received ${ratings.size} ratings")
                    val previousIds = _ratings.value.map { it.id }.toSet()
                    _ratings.value = ratings
                    _isLoading.value = false

                    ratings.filter { it.id !in previousIds }.forEach { rating ->
                        _newRatingEvent.emit(rating)
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch ratings: ${e.message}", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in subscribeToRatings: ${e.message}", e)
            _error.value = e.message
            _isLoading.value = false
        }
    }

    companion object {
        @Volatile
        private var instance: RatingsRepository? = null

        fun getInstance(): RatingsRepository {
            return instance ?: synchronized(this) {
                instance ?: RatingsRepository().also { instance = it }
            }
        }
    }
}

internal fun isRatingValid(value: Int, min: Int = 1, max: Int = 5): Boolean {
    return value in min..max
}

internal fun normalizeComment(raw: String?): String? {
    val t = raw?.trim()
    return if (t.isNullOrEmpty()) null else t
}


