import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    return providers.exec {
        isIgnoreExitValue = true
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
    }.standardOutput.asText.get().trim()
}

android {
    namespace = "org.lyaaz.fuckshare"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.lyaaz.fuckshare"
        minSdk = 30
        targetSdk = 35
        versionCode = "git rev-list HEAD --count".execute().toInt()
        versionName = "git describe --tag --always".execute().removePrefix("v")
        resourceConfigurations += setOf("en", "zh-rCN")
        vectorDrawables.useSupportLibrary = true
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    signingConfigs {
        create("release") {
            val properties = Properties().apply {
                load(File("signing.properties").reader())
            }
            storeFile = File(properties.getProperty("storeFilePath"))
            storePassword = properties.getProperty("storePassword")
            keyPassword = properties.getProperty("keyPassword")
            keyAlias = properties.getProperty("keyAlias")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "FS debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
    }
}
dependencies {
    implementation(project(":ui"))

    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("androidx.work:work-runtime-ktx:2.10.4")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.github.bumptech.glide:gifencoder-integration:5.0.5")

    // compose
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // QR code
    implementation("com.google.zxing:core:3.5.3")
}
