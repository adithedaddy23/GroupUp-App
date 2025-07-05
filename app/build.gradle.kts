plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt") // Only keep one kapt
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.studygroupfinder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.studygroupfinder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val room_version = "2.7.1"
    //Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.8")
    //HiltViewModel
    implementation("com.google.dagger:hilt-android:2.51.1") // Use the same version as the plugin
    kapt("com.google.dagger:hilt-compiler:2.51.1")  // Optional, for Compose


    // Hilt ViewModel
    implementation ("androidx.hilt:hilt-navigation-compose:1.1.0")
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation ("com.google.firebase:firebase-firestore-ktx:25.1.4")
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.1")

    //Koil Images
    implementation("io.coil-kt:coil-compose:2.4.0")

    //Google Maps
    implementation ("com.google.maps.android:maps-compose:2.11.4")
    implementation ("com.google.android.gms:play-services-maps:19.2.0")

    //FreeOpenMap
    implementation ("org.osmdroid:osmdroid-android:6.1.16")
    implementation ("androidx.preference:preference-ktx:1.2.1")
    implementation ("org.maplibre.gl:android-sdk:9.5.2")
    implementation ("org.maplibre.gl:android-plugin-annotation-v9:1.0.0")

    implementation("com.jakewharton.timber:timber:5.0.1")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.googleid)
    implementation(libs.play.services.location)
    implementation(libs.androidx.ui.text.google.fonts)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}