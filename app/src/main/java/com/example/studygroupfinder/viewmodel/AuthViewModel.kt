package com.example.studygroupfinder.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studygroupfinder.googleSignIn.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel(private val googleSignInClient: GoogleSignInClient) : ViewModel() {
    private val _authState = MutableLiveData<AuthState?>()
    val authState: LiveData<AuthState?> = _authState

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Add current user properties
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    // Helper property to get current user ID
    val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        checkAuthState()
    }

    fun isAuthenticated(): Boolean {
        return googleSignInClient.isSignedIn()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Small delay to ensure UI shows loading state
                delay(100)

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                _currentUser.value = firebaseUser

                _authState.value = if (googleSignInClient.isSignedIn() && firebaseUser != null) {
                    AuthState.Authenticated
                } else {
                    AuthState.NotAuthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.NotAuthenticated
                _currentUser.value = null
                _errorMessage.value = "Error checking auth state: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Call the suspend function properly
                val success = googleSignInClient.signIn()

                if (success) {
                    // Update current user after successful sign-in
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    _currentUser.value = firebaseUser

                    // Double-check the auth state after sign-in
                    _authState.value = if (googleSignInClient.isSignedIn() && firebaseUser != null) {
                        AuthState.Authenticated
                    } else {
                        AuthState.NotAuthenticated
                    }
                } else {
                    _authState.value = AuthState.NotAuthenticated
                    _currentUser.value = null
                    _errorMessage.value = "Sign in failed"
                }
            } catch (e: Exception) {
                _authState.value = AuthState.NotAuthenticated
                _currentUser.value = null
                _errorMessage.value = "Sign in error: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                googleSignInClient.signOut()
                _authState.value = AuthState.NotAuthenticated
                _currentUser.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Sign out error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refreshAuthState() {
        checkAuthState()
    }
}