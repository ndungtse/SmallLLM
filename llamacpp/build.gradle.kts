plugins {
    alias(libs.plugins.android.library)
}

// The native build is wired only once the llama.cpp submodule is present. Until then this module
// builds Kotlin-only (the SmolLM `native` methods are just declarations), so the app keeps building
// without the NDK. See llamacpp/README.md for the one-time setup (NDK + CMake + submodule).
val nativeReady = file("src/main/cpp/llama.cpp/CMakeLists.txt").exists()

android {
    namespace = "com.smallllm.llamacpp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24

        if (nativeReady) {
            ndk {
                // GGUF inference is CPU-only; ship the common Android ABIs.
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
            externalNativeBuild {
                cmake {
                    cppFlags += "-O3"
                    arguments += "-DANDROID_STL=c++_shared"
                }
            }
        }
    }

    if (nativeReady) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
