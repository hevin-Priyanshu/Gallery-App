plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.demo.newgalleryapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.demo.newgalleryapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")



    // images loading libraries
    implementation ("com.squareup.picasso:picasso:2.8")

    //glide
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    implementation ("com.github.chrisbanes:PhotoView:2.3.0")

    val lifecycle_version = "2.6.2"

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")

    //ROOM DATABASE LIBRARY
    implementation ("androidx.room:room-runtime:2.4.0")
    kapt ("androidx.room:room-compiler:2.4.0")

    implementation(kotlin("reflect"))

    // images edit library
    api ("com.theartofdev.edmodo:android-image-cropper:2.8.0")

//    implementation ("com.github.yalantis:ucrop:2.2.6")

    implementation ("com.github.yalantis:ucrop:2.2.8-native")
    // images filters libray
    implementation ("net.alhazmy13.ImageFilters:library:0.1.2-beta")

    implementation ("commons-io:commons-io:2.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")

    val media3_version = "1.2.0"
    // For media playback using ExoPlayer
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation ("androidx.media3:media3-ui:$media3_version")
    implementation ("androidx.media3:media3-exoplayer-dash:$media3_version")

    implementation ("com.intuit.sdp:sdp-android:1.1.0")
    implementation ("com.intuit.ssp:ssp-android:1.1.0")

}