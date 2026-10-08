plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dfuzer.birdnote"
    sourceSets["main"].assets.srcDir("../assets")

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dfuzer.birdnote"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Upload credentials stay outside version control. No debug-key fallback for release.
    val uploadStore = providers.environmentVariable("BIRDNOTE_UPLOAD_STORE_FILE").orNull
    val uploadPassword = providers.environmentVariable("BIRDNOTE_UPLOAD_STORE_PASSWORD").orNull
    val uploadAlias = providers.environmentVariable("BIRDNOTE_UPLOAD_KEY_ALIAS").orNull
    val uploadKeyPassword = providers.environmentVariable("BIRDNOTE_UPLOAD_KEY_PASSWORD").orNull
    val uploadValues = listOf(uploadStore, uploadPassword, uploadAlias, uploadKeyPassword)
    require(uploadValues.all { it == null } || uploadValues.all { !it.isNullOrBlank() }) {
        "Set all four BIRDNOTE_UPLOAD_* signing variables, or leave all unset for an unsigned release."
    }
    if (uploadStore != null) {
        signingConfigs.create("upload") {
            storeFile = file(uploadStore)
            storePassword = uploadPassword
            keyAlias = uploadAlias
            keyPassword = uploadKeyPassword
        }
    }
    bundle { language { enableSplit = false } }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-svg:3.3.0")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}