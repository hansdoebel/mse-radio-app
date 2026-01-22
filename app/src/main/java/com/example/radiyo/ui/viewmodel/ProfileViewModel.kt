package com.example.radiyo.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiyo.data.model.User
import com.example.radiyo.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository.getInstance()

    val currentUser: StateFlow<User?> = userRepository.currentUser

    fun logout(context: Context) {
        viewModelScope.launch {
            userRepository.logout(context)
        }
    }
}
