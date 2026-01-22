package com.example.radiyo.data.remote

import android.content.Context
import com.clerk.api.user.User
import com.example.radiyo.BuildConfig
import dev.convex.android.ConvexClientWithAuth

object ConvexConfig {
    private val authProvider = ClerkAuthProvider()

    val client: ConvexClientWithAuth<User> by lazy {
        ConvexClientWithAuth(BuildConfig.CONVEX_URL, authProvider)
    }

    suspend fun loginWithClerk(context: Context): Result<User> {
        return client.login(context)
    }

    suspend fun loginFromCache(): Result<User> {
        return client.loginFromCache()
    }

    suspend fun logout(context: Context): Result<Void?> {
        return client.logout(context)
    }
}
