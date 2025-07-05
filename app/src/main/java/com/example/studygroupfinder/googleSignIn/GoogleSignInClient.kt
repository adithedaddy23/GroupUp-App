package com.example.studygroupfinder.googleSignIn

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.studygroupfinder.firestore.FirestoreRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleSignInClient (
    private var context: Context,
) {
    private val tag = "GoogleSignInClient: "

    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun isSignedIn(): Boolean {
        val isSignedIn = firebaseAuth.currentUser != null
        println(tag + "User signed in status: $isSignedIn")
        return isSignedIn
    }

    suspend fun signIn(): Boolean {
        println(tag + "signIn() called")

        if(isSignedIn()) {
            println(tag + "User already signed in")
            return true
        }

        return try {
            println(tag + "Building credential request...")
            val result = buildCredentialRequest()
            println(tag + "Credential request completed, handling result...")
            val success = handleSignInResult(result)
            println(tag + "Sign in result: $success")
            success
        } catch (e: Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
            println(tag + "Sign in Error: ${e.message}")
            false
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Boolean {
        println(tag + "handleSignInResult called")
        val credential = result.credential

        if(
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                println(tag + "Token credential created successfully")
                println(tag + "name: ${tokenCredential.displayName}")
                println(tag + "email: ${tokenCredential.id}")

                val authCredential = GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
                println(tag + "Auth credential created, signing in with Firebase...")

                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                println(tag + "Firebase sign in completed")

                if(authResult.user != null) {
                    println(tag + "User authenticated successfully, creating profile...")

                    // Create user profile in Firestore (run in background, don't wait for it)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val repository = FirestoreRepository()
                            repository.createUserProfile(
                                userId = authResult.user!!.uid,
                                name = authResult.user!!.displayName ?: "",
                                email = authResult.user!!.email ?: "",
                                profilePic = authResult.user!!.photoUrl?.toString()
                            )
                            println(tag + "User profile created successfully")
                        } catch (e: Exception) {
                            println(tag + "Error creating user profile: ${e.message}")
                            // Don't fail the sign-in process if profile creation fails
                        }
                    }

                    println(tag + "Sign in successful")
                    true
                } else {
                    println(tag + "Auth result user is null")
                    false
                }
            } catch (e: GoogleIdTokenParsingException) {
                println(tag + "GoogleIdTokenParsingException: ${e.message}")
                false
            } catch (e: Exception) {
                println(tag + "Exception in handleSignInResult: ${e.message}")
                e.printStackTrace()
                false
            }
        } else {
            println(tag + "credential is not of type GoogleIdTokenCredential")
            false
        }
        return true
    }

    private suspend fun buildCredentialRequest(): GetCredentialResponse {
        println(tag + "buildCredentialRequest called")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(
                        "251239509884-2ko4q4futfc32m55hrpcgmkb3qfmltag.apps.googleusercontent.com"
                    )
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        println(tag + "Getting credential from CredentialManager...")
        return credentialManager.getCredential(
            request = request,
            context = context
        )
    }

    suspend fun signOut() {
        println(tag + "signOut called")
        try {
            credentialManager.clearCredentialState(
                ClearCredentialStateRequest()
            )
            firebaseAuth.signOut()
            println(tag + "Sign out completed")
        } catch (e: Exception) {
            println(tag + "Error during sign out: ${e.message}")
        }
    }
}