import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Hugging Face access token for gated models (e.g. Gemma). Read from local.properties (gitignored)
// or the HF_TOKEN env var — never committed. Empty when not configured.
val hfAccessToken: String = run {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
    props.getProperty("HF_TOKEN") ?: System.getenv("HF_TOKEN") ?: ""
}

android {
    namespace = "com.smallllm"
    // compileSdk 37: the Compose foundation/ui/animation 1.12.0-alpha03 artifacts pulled in
    // transitively by material3 1.5.0-alpha (Expressive) require compiling against API 37+.
    // targetSdk stays 36 — this only raises the compile-time API, not runtime behavior.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.smallllm"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "HF_TOKEN", "\"$hfAccessToken\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":llamacpp"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.litertlm)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
