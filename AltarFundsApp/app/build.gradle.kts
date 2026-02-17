plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

android {
    namespace = "com.altarfunds.member"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.altarfunds.member"
        minSdk = 24
        targetSdk = 36
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
    buildFeatures{
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.13.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Lifecycle (MVVM)
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v2100)
    implementation(libs.androidx.lifecycle.livedata.ktx.v2100)
    implementation(libs.androidx.lifecycle.runtime.ktx.v2100)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx.v297)
    implementation(libs.androidx.navigation.ui.ktx.v297)

    // UI
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.androidx.swiperefreshlayout.v120)

    // Images & Effects
    implementation(libs.glide.v505)
    implementation(libs.shimmer)

    // Data & Background
    implementation(libs.androidx.datastore.preferences.v120)
    implementation(libs.androidx.work.runtime.ktx.v2111)
    
    // Room Database for offline caching
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)

}