import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}


val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""


android {
    namespace = "com.abhishek.cellularlab"
    compileSdk = 36
    ndkVersion = "28.1.13356709"

    defaultConfig {
        applicationId = "com.abhishek.cellularlab"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.2"
        buildToolsVersion = "36.0.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Required for native builds
        externalNativeBuild {
            cmake {
                cFlags += listOf("-std=c11", "-D__STDC_NO_ATOMICS__=0")

            }
        }

        // ✅ Specify ABI(s) if you're targeting specific architectures
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    // region Signing Config (optional if keystore.properties present)
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }
    signingConfigs {
        maybeCreate("release").apply {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] ?: "")
                storePassword = keystoreProperties["storePassword"] as String?
                keyAlias = keystoreProperties["keyAlias"] as String?
                keyPassword = keystoreProperties["keyPassword"] as String?
            }
        }
    }
    // endregion

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.0.2"
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.taptargetview)
    implementation(libs.okhttp)

    implementation(libs.markwon)
    implementation(libs.markwon.html)
    implementation(libs.markwon.table)
    implementation(libs.markwon.strikethrough)
    implementation(libs.markwon.tasklist)
    implementation(libs.markwon.linkify)
}