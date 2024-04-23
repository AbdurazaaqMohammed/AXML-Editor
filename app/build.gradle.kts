plugins {
    id("com.android.application")
}

android {
    namespace = "com.abdurazaaqmohammed.androidmanifesteditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.abdurazaaqmohammed.androidmanifesteditor"
        minSdk = 16
        targetSdk = 34
        versionCode = 8
        versionName = "1.5.2"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = false
    }
}
dependencies {
    implementation("com.github.yukuku:ambilwarna:2.0.1")
}
