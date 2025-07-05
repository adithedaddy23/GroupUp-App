package com.example.studygroupfinder.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studygroupfinder.googleSignIn.GoogleSignInClient

class AuthViewModelFactory (
    private val googleSignInClient: GoogleSignInClient
): ViewModelProvider.Factory {
    override fun<T: ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(googleSignInClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }
}