plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "org.feup.apm.cmeb_login"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.feup.apm.cmeb_login"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.runner)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.hbb20:ccp:2.5.0")
    implementation ("at.favre.lib:bcrypt:0.9.0")
    implementation ("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation ("com.github.dhaval2404:imagepicker:2.1")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.15.0")
    implementation("com.google.firebase:firebase-bom:32.1.1") // Ajuste a versão conforme necessário
    implementation ("com.google.firebase:firebase-messaging")

    implementation(libs.ccp)
    implementation (libs.bcrypt)
    implementation (libs.firebase.ui.firestore)
    implementation (libs.mobile.ffmpeg.full)
    implementation (libs.media3.transformer)
    implementation (libs.androidx.media3.effect)
    implementation (libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    implementation (libs.ucrop)
    implementation (libs.github.glide)
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
}