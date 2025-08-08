plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.tie.vibein"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tie.vibein"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
    }

    buildFeatures {
        viewBinding =true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.animation.core.android)
    implementation(libs.androidx.constraintlayout.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)



    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation ("com.intuit.ssp:ssp-android:1.1.1")
    implementation ("com.intuit.sdp:sdp-android:1.1.1")
    implementation ("com.hbb20:ccp:2.7.3")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.9.2")
    implementation ("com.squareup.retrofit2:retrofit:3.0.0")
    implementation ("com.squareup.okhttp3:okhttp:5.1.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:5.1.0")
    implementation ("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation ("androidx.navigation:navigation-fragment-ktx:2.9.2")
    implementation ("androidx.navigation:navigation-ui-ktx:2.9.2")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.mikhaellopez:circularimageview:4.3.1")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")


    // 1. LocalBroadcastManager (for real-time in-app communication)
    implementation ("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // 2. Image Compression Library
    implementation ("id.zelory:compressor:3.0.1")


    // For pinch-to-zoom in the full-screen viewer
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")

    // For easily converting a list of image URLs to a JSON string and back
    implementation ("com.google.code.gson:gson:2.10.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.razorpay:checkout:1.6.41")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    //BlurTransformation
    implementation("jp.wasabeef:glide-transformations:4.3.0")








}