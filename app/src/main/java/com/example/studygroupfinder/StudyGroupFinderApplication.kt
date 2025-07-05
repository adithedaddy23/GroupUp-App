package com.example.studygroupfinder

import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import timber.log.Timber

//class StudyGroupFinderApplication : Application() {
//    override fun onCreate() {
//        super.onCreate()
//
//        // Initialize Firebase
//        try {
//            FirebaseApp.initializeApp(this)
//            Timber.d("Firebase initialized successfully")
//        } catch (e: Exception) {
//            Timber.e(e, "Firebase initialization failed")
//        }
//
//        // Initialize Timber for logging (optional)
//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
//        }
//    }
//}