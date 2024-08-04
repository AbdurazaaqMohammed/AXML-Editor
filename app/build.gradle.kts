plugins {
    id("com.android.application")
}

android {
    namespace = "com.abdurazaaqmohammed.androidmanifesteditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abdurazaaqmohammed.androidmanifesteditor"
        minSdk = 1
        targetSdk = 35
        versionCode = 11
        versionName = "1.6.2"

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
}