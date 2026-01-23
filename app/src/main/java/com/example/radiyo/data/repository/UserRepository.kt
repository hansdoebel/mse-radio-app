package com.example.radiyo.data.repository

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.example.radiyo.data.model.User
import com.example.radiyo.data.model.UserRole
import com.example.radiyo.data.remote.ConvexConfig
import kotlinx.coroutines.flow.*

class UserRepository private constructor() {
    private val client = ConvexConfig.client

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun signInWithPassword(email: String, password: String): Result<Unit> {
        _isLoading.value = true
        _error.value = null

        if (Clerk.session != null && Clerk.user != null) {
            android.util.Log.d("UserRepository", "Already signed in, syncing user")
            syncUserFromClerk()
            _isLoading.value = false
            return Result.success(Unit)
        }

        return try {
            val result = SignIn.create(
                SignIn.CreateParams.Strategy.Password(
                    identifier = email,
                    password = password
                )
            )
            when (result) {
                is ClerkResult.Success -> {
                    val signIn = result.value
                    if (signIn.status == SignIn.Status.COMPLETE) {
                        signIn.createdSessionId?.let { sessionId ->
                            Clerk.setActive(sessionId = sessionId)
                        }
                        syncUserFromClerk()
                        Result.success(Unit)
                    } else {
                        _error.value = "Sign-in incomplete: ${signIn.status}"
                        Result.failure(Exception("Sign-in incomplete"))
                    }
                }

                is ClerkResult.Failure -> {
                    val errorMsg = result.error?.errors?.firstOrNull()?.message ?: "Sign-in failed"
                    _error.value = errorMsg
                    Result.failure(Exception(errorMsg))
                }

                else -> {
                    _error.value = "Unknown error"
                    Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<Unit> {
        _isLoading.value = true
        _error.value = null
        android.util.Log.d("UserRepository", "signUpWithEmail called for: $email")
        return try {
            val result = SignUp.create(
                SignUp.CreateParams.Standard(
                    emailAddress = email,
                    password = password,
                    firstName = name
                )
            )
            android.util.Log.d("UserRepository", "SignUp.create result: $result")
            when (result) {
                is ClerkResult.Success -> {
                    val signUp = result.value
                    android.util.Log.d(
                        "UserRepository",
                        "SignUp status: ${signUp.status}, sessionId: ${signUp.createdSessionId}"
                    )
                    if (signUp.status == SignUp.Status.COMPLETE) {
                        signUp.createdSessionId?.let { sessionId ->
                            android.util.Log.d("UserRepository", "Setting active session: $sessionId")
                            Clerk.setActive(sessionId = sessionId)
                        }
                        android.util.Log.d("UserRepository", "Calling syncUserFromClerk")
                        syncUserFromClerk(name)
                        Result.success(Unit)
                    } else {
                        android.util.Log.w("UserRepository", "Sign-up not complete, status: ${signUp.status}")
                        _error.value = "Sign-up incomplete"
                        Result.failure(Exception("Sign-up incomplete"))
                    }
                }

                is ClerkResult.Failure -> {
                    val errorMsg = result.error?.errors?.firstOrNull()?.message ?: "Sign-up failed"
                    _error.value = errorMsg
                    Result.failure(Exception(errorMsg))
                }

                else -> {
                    _error.value = "Unknown error"
                    Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun syncUserFromClerk(name: String? = null) {
        android.util.Log.d("UserRepository", "syncUserFromClerk called with name: $name")
        val clerkUser = Clerk.user
        android.util.Log.d("UserRepository", "Clerk.user: $clerkUser")
        val displayName = name ?: clerkUser?.firstName ?: clerkUser?.primaryEmailAddress?.emailAddress ?: "User"
        android.util.Log.d("UserRepository", "Using displayName: $displayName")

        val authResult = ConvexConfig.loginFromCache()
        android.util.Log.d("UserRepository", "loginFromCache result: $authResult")
        if (authResult.isFailure) {
            android.util.Log.e("UserRepository", "Failed to set Convex auth: ${authResult.exceptionOrNull()?.message}")
        }

        kotlinx.coroutines.delay(100)

        try {
            android.util.Log.d("UserRepository", "Calling users:upsert mutation")
            client.mutation<String>(
                "users:upsert",
                args = mapOf("name" to displayName)
            )
            android.util.Log.d("UserRepository", "users:upsert completed")
            val result = client.subscribe<User?>("users:whoami", emptyMap()).first()
            val fetchedUser = result.getOrNull()
            if (fetchedUser != null) {
                android.util.Log.d("UserRepository", "Fetched user from Convex: role=${fetchedUser.role}")
                _currentUser.value = fetchedUser
            } else {
                android.util.Log.w("UserRepository", "whoami returned null, using fallback")
                val clerkEmail = clerkUser?.primaryEmailAddress?.emailAddress ?: ""
                _currentUser.value = User(
                    id = clerkUser?.id ?: "",
                    email = clerkEmail,
                    name = displayName,
                    role = UserRole.LISTENER,
                    tokenIdentifier = clerkUser?.id ?: ""
                )
            }
            _isAuthenticated.value = true
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to sync user to Convex: ${e.message}")
            val clerkEmail = clerkUser?.primaryEmailAddress?.emailAddress ?: ""
            _currentUser.value = User(
                id = clerkUser?.id ?: "",
                email = clerkEmail,
                name = displayName,
                role = UserRole.LISTENER,
                tokenIdentifier = clerkUser?.id ?: ""
            )
            _isAuthenticated.value = true
        }
    }

    suspend fun logout(context: android.content.Context) {
        _isLoading.value = true
        try {
            val result = Clerk.signOut()
            android.util.Log.d("UserRepository", "Clerk signOut result: $result")

            ConvexConfig.logout(context)

            kotlinx.coroutines.delay(100)

            android.util.Log.d("UserRepository", "After logout - session: ${Clerk.session}, user: ${Clerk.user}")
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Logout error: ${e.message}")
        } finally {
            _currentUser.value = null
            _isAuthenticated.value = false
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository().also { instance = it }
            }
        }
    }
}

internal fun normalizeEmail(raw: String): String = raw.trim()

internal fun isValidEmail(raw: String): Boolean {
    val email = normalizeEmail(raw)
    if (email.isEmpty()) return false
    if (email.contains(" ")) return false

    val at = email.indexOf('@')
    if (at <= 0) return false
    if (at != email.lastIndexOf('@')) return false

    val domain = email.substring(at + 1)
    if (domain.isBlank()) return false
    if (!domain.contains('.')) return false
    if (domain.startsWith('.') || domain.endsWith('.')) return false

    return true
}

internal fun isPasswordValid(raw: String, minLength: Int = 8): Boolean {
    return raw.isNotBlank() && raw.length >= minLength
}

