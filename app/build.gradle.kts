plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.fpa.dangjiandaping"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.fpa.dangjiandaping"
        minSdk = 23
        targetSdk = 33
        versionCode = 205
        versionName = "2.0.5"
        buildConfigField(
            "String",
            "BIG_SCREEN_DEVICE_CODE",
            "\"Tv-device-ganzi001\"",
        )
        buildConfigField(
            "String",
            "BIG_SCREEN_LOGIN_SECRET",
            "\"scgbwlxyBigScreen2026\"",
        )
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BIG_SCREEN_API_BASE_URL",
                "\"http://192.168.99.174:7030/\"",
            )
            buildConfigField(
                "String",
                "BASE_URL",
                "\"http://192.168.20.233:5173/ganziTv/#/\""
            )
        }
        release {
            buildConfigField(
                "String",
                "BIG_SCREEN_API_BASE_URL",
                "\"https://www.scycjy.gov.cn/\"",
            )
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://www.scycjy.gov.cn/ganziTv/#/\""
            )
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.navigation3:navigation3-runtime:1.1.4")
    implementation("androidx.navigation3:navigation3-ui:1.1.4")
    implementation("io.github.carguo:gsyvideoplayer-compose:13.1.0")
    implementation("io.github.carguo:gsyvideoplayer-exo2:13.1.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
