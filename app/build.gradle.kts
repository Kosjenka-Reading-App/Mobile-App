plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.dsd.kosjenka"
    compileSdk = 34

    buildFeatures {
        dataBinding = true
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.dsd.kosjenka"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://dev-kosj-api.fly.dev/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://dev-kosj-api.fly.dev/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlinOptions {
        jvmTarget = "18"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    //Material Design
    implementation("com.google.android.material:material:1.10.0")
    //Constraint Layout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    //Retrofit2
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    //Gson
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    //OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.3")

    //Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.paging:paging-common-android:3.3.0-alpha02")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")

    //Paging 3
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    //CardView
    implementation("androidx.cardview:cardview:1.0.0")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.14.2")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    //Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    //Timber Logs
    implementation("com.jakewharton.timber:timber:5.0.1")

    //Material 3
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")


    implementation(platform("com.google.firebase:firebase-bom:32.5.0"))
    implementation("com.google.firebase:firebase-analytics")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}
// Allow references to generated code
kapt {
    correctErrorTypes = true
}