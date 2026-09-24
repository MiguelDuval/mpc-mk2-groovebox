plugins {
    id("com.android.application")
}

android {
    namespace = "com.miguelduval.mpcmk2groovebox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miguelduval.mpcmk2groovebox"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-foundation"

        ndkVersion = "28.2.13676358"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../../CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
