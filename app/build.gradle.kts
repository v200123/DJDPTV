plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val betaQrAppId = providers.gradleProperty("BETAQR_APP_ID").orElse("").get()
val betaQrApiToken = providers.gradleProperty("BETAQR_API_TOKEN").orElse("").get()

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("D:\\AiProject\\djdptv\\app\\djtv_putong")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
//        getByName("release") {
//            storeFile = file("D:\\AiProject\\djdptv\\app\\djtv_putong")
//            storePassword = "123456"
//            keyAlias = "key0"
//            keyPassword = "123456"
//        }
    }
    namespace = "com.fpa.dangjiandaping"
    compileSdk = 36
    lint {
        baseline = file("lint-baseline.xml")
    }
    defaultConfig {
        applicationId = "com.fpa.dangjiandaping"
        minSdk = 23
        targetSdk = 33
        versionCode = 205
        versionName = "2.0.5"
        // Configure these in the user Gradle properties; do not commit the API token.
        buildConfigField("String", "BETAQR_APP_ID", "\"6a841a5cf9454870f3a5a87a\"")
        buildConfigField("String", "BETAQR_API_TOKEN", "\"fc06ee35435bb4b74a7603bf2190a198\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            // 开发/测试包只能访问测试环境，避免调试时误操作正式数据。
            buildConfigField("String", "BASE_URL", "\"http://192.168.20.233:5173/xiaoyuTv/#/\"")
        }
        release {
            // 正式签名包固定访问正式环境。
            buildConfigField("String", "BASE_URL", "\"https://www.scycjy.gov.cn/xiaoyuTv/#/\"")
            signingConfig = signingConfigs.getByName("debug")
        }
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

    debugImplementation("androidx.compose.ui:ui-tooling")
}
