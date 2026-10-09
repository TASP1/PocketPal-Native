plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.tasp1.pocketpal"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tasp1.pocketpal"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.10.1-claude"
        buildConfigField("String", "BRIDGE_URL", "\"https://scrubbed-calcium-subscript.ngrok-free.dev\"")
        buildConfigField("String", "BRIDGE_KEY", "\"28f53fa45b3c222cc2b1cac6795b6ae1\"")
        // Render-hosted agent / gh-cli MCP (shell). Free tier may cold-start.
        buildConfigField("String", "RENDER_AGENT_URL", "\"https://gh-cli-for-ai-bots.onrender.com\"")
        buildConfigField("String", "RENDER_API_BASE", "\"https://api.render.com\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Images + large attachments
    implementation("io.coil-kt:coil-compose:2.7.0")
    // On-device OCR (high accuracy Latin)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Optional: Devanagari / Chinese later via extra modules
    implementation("androidx.documentfile:documentfile:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
