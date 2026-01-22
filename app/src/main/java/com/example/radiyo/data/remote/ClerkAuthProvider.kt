package com.example.radiyo.data.remote

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.clerk.api.user.User
import dev.convex.android.AuthProvider

class ClerkAuthProvider : AuthProvider<User> {

    override suspend fun login(context: Context): Result<User> {
        val user = Clerk.user
        return if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("Not logged in"))
        }
    }

    override suspend fun loginFromCache(): Result<User> {
        val user = Clerk.user
        return if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("No cached user"))
        }
    }

    override suspend fun logout(context: Context): Result<Void?> {
        return try {
            Clerk.signOut()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun extractIdToken(credentials: User): String {
        return runCatching {
            kotlinx.coroutines.runBlocking {
                var session = Clerk.session
                var retries = 0
                while (session == null && retries < 10) {
                    kotlinx.coroutines.delay(100)
                    session = Clerk.session
                    retries++
                }

                if (session == null) {
                    android.util.Log.e("ClerkAuthProvider", "No Clerk session available")
                    return@runBlocking ""
                }

                val tokenResult = session.fetchToken(
                    GetTokenOptions(template = "convex")
                )
                when (tokenResult) {
                    is ClerkResult.Success -> {
                        android.util.Log.d("ClerkAuthProvider", "Got JWT token for Convex")
                        tokenResult.value.jwt
                    }

                    is ClerkResult.Failure -> {
                        val errors = tokenResult.error?.errors
                        android.util.Log.e(
                            "ClerkAuthProvider",
                            "Failed to get token. Errors: $errors"
                        )
                        errors?.forEach { err ->
                            android.util.Log.e(
                                "ClerkAuthProvider",
                                "  - code: ${err.code}, message: ${err.message}, longMessage: ${err.longMessage}"
                            )
                        }
                        ""
                    }

                    else -> ""
                }
            }
        }.getOrDefault("")
    }
}
