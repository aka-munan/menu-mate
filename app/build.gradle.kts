plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("/home/aka-munan/Android/keystore/key.jks")
            storePassword = "aka@munan9103"
            keyAlias = "haris"
            keyPassword = "aka@munan9103"
        }
        create("release") {
            storeFile = file("/home/aka-munan/Android/keystore/key.jks")
            storePassword = "aka@munan9103"
            keyAlias = "haris"
            keyPassword = "aka@munan9103"
        }
    }
    namespace = "com.devoid.menumate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.devoid.menumate"
        minSdk = 28
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
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.googleid)
    implementation(libs.firebase.database.ktx)
    implementation(files("libs/okhttp-4.12.0.jar"))
    implementation(libs.glide)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.lottie)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.core)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
