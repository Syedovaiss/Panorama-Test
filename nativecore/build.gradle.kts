plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ovais.nativecore"
    ndkVersion = "28.2.13676358"
    compileSdk = 35 // Keep at 35, we'll fix the dependency instead if possible or increase if required.
    // Actually, the error says it requires 36. Let's update to 36.

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-O3")
                // Pass the ABI to CMake to help it find the right libraries
                arguments += "-DANDROID_ABI=arm64-v8a"
            }
        }
        ndk {
            // Only build for arm64-v8a since OpenCV libs for other ABIs are missing
            abiFilters += listOf("arm64-v8a")
        }
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
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Override compileSdk to 36 because androidx.core:core-ktx:1.17.0 requires it
android.compileSdk = 36

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}