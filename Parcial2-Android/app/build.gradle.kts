plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.parcial2_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.parcial2_android"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ─── Core ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Desugaring (java.time en minSdk 24)
    coreLibraryDesugaring("com.android.tools.desugar_jdk_libs:2.1.4")

    // ─── Material Design ───────────────────────────────────────────────────
    implementation("com.google.android.material:material:1.12.0")

    // ─── Room ──────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ─── ViewModel + LiveData + Lifecycle ──────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // ─── Coroutines ────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ─── RecyclerView ──────────────────────────────────────────────────────
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ─── Mapa GRATIS — OpenStreetMap (sin API Key, sin billing) ───────────
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // ─── Ubicación GPS (sin Maps, solo FusedLocation) ─────────────────────
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ─── Carga de imágenes ─────────────────────────────────────────────────
    implementation("io.coil-kt:coil:2.7.0")

    // ─── Tests ─────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
