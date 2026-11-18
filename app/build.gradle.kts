import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pintafacil.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pintafacil.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.8"
        vectorDrawables { useSupportLibrary = true }

        // Claves de API: leídas de apikeys.properties (no está en git).
        // Si falta el archivo, la app compila pero las funciones que
        // requieren clave mostrarán un aviso.
        val apiKeysFile = rootProject.file("apikeys.properties")
        val apiProps = Properties()
        if (apiKeysFile.exists()) {
            apiKeysFile.inputStream().use { apiProps.load(it) }
        }
        val pixabayKey = apiProps.getProperty("Pixabay", "").trim()
        buildConfigField("String", "PIXABAY_API_KEY", "\"$pixabayKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
