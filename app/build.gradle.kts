plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

private fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
private val firebaseProperties = java.util.Properties().apply {
    val file = rootProject.file("firebase.properties")
    if (file.isFile) file.inputStream().use(::load)
}
private val firebaseConfig: (String) -> String = { name ->
    (providers.gradleProperty(name).orNull
        ?: System.getenv(name)
        ?: firebaseProperties.getProperty(name)
        ?: "").trim()
}

android {
    namespace = "com.iumrah.beta"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iumrah.beta"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.8.2-stage8-stable36"

        // FCM credentials are supplied as Gradle properties or environment variables.
        // Keeping them optional lets the repository build before Firebase provisioning,
        // while the runtime enables remote push immediately once all four values exist.
        buildConfigField("String", "IUMRAH_FIREBASE_API_KEY", firebaseConfig("IUMRAH_FIREBASE_API_KEY").asBuildConfigString())
        buildConfigField("String", "IUMRAH_FIREBASE_APP_ID", firebaseConfig("IUMRAH_FIREBASE_APP_ID").asBuildConfigString())
        buildConfigField("String", "IUMRAH_FIREBASE_PROJECT_ID", firebaseConfig("IUMRAH_FIREBASE_PROJECT_ID").asBuildConfigString())
        buildConfigField("String", "IUMRAH_FIREBASE_SENDER_ID", firebaseConfig("IUMRAH_FIREBASE_SENDER_ID").asBuildConfigString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.0"))
    implementation("com.squareup.okhttp3:okhttp")

    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    implementation("com.google.zxing:core:3.5.3")

    // Remote push parity with iOS APNs. Firebase is initialized manually from
    // BuildConfig so builds stay valid even before the Android Firebase app is provisioned.
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Real interactive maps for Ziyarats and airport selection. OpenGL is used for widest device compatibility.
    implementation("org.maplibre.gl:android-sdk-opengl:13.6.1")

    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
