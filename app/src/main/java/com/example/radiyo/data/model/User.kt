package com.example.radiyo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("_id") val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val tokenIdentifier: String
)

@Serializable
enum class UserRole {
    @SerialName("listener") LISTENER,
    @SerialName("moderator") MODERATOR
}
