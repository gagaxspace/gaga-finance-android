plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.gagafinance.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.gagafinance.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 7
        versionName = "0.7.0"
        manifestPlaceholders["gagaAppLabel"] = "GaGa Finance"
    }

    // A Play upload key is supplied only through private GitHub Actions secrets.
    // It is never committed to Git or generated afresh by CI on each release.
    signingConfigs {
        create("playUpload") {
            val keyFile = System.getenv("GAGA_FINANCE_UPLOAD_KEY_FILE")
            if (!keyFile.isNullOrBlank()) {
                storeFile = file(keyFile)
                storePassword = System.getenv("GAGA_FINANCE_UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("GAGA_FINANCE_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("GAGA_FINANCE_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".preview07"
            versionNameSuffix = "-preview"
            manifestPlaceholders["gagaAppLabel"] = "GaGa Finance Preview"
        }
        release {
            if (!System.getenv("GAGA_FINANCE_UPLOAD_KEY_FILE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("playUpload")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
