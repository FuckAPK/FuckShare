import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
}
android {
    namespace = "org.baiyu.fuckshare"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.baiyu.fuckshare"
        minSdk = 30
        targetSdk = 34
        versionCode = 68

        versionName = "9.5"
        resourceConfigurations += setOf("en", "zh-rCN")
        vectorDrawables.useSupportLibrary = true
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
    applicationVariants.all {
        outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach {
                it.outputFileName = "Fuck Share_${defaultConfig.versionName}.apk"
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
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")
    implementation("com.google.android.material:material:1.12.0")
    compileOnly("de.robv.android.xposed:api:82")
}
