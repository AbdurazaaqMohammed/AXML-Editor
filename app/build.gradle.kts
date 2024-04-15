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
        versionCode = 1
        versionName = "1.0"

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
    testImplementation("junit:junit:4.13.2")
}
