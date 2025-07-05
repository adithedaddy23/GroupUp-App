package com.example.studygroupfinder.viewmodel

sealed class AuthState {
    object Authenticated : AuthState()
    object NotAuthenticated : AuthState()
}