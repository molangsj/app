plugins {
    id("com.android.application")
//    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.project1"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.project1"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
//    packagingOptions {
//        exclude("com/example/project1/ClaendarManager.class")
//        exclude("com/example/project1/DataManager.class")
//        exclude("com/example/project1/MainActivity.class")
//    }

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
    viewBinding{
        enable=true;
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation ("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation (libs.play.services.vision)
    implementation("com.google.firebase:firebase-ml-vision:24.1.0") {
        exclude(group = "com.google.android.gms", module = "play-services-vision-common")
    }
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1")
    implementation(libs.mlkit.text.recognition.korean)
//    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
//    implementation ("com.google.android.gms:play-services-auth:23.1.0")
    implementation ("com.google.android.gms:play-services-auth:20.5.0")

    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.firebase:firebase-messaging:24.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database:20.0.3")

    // Glide for image loading
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase Storage (아마 이미 있을 수 있음)
    implementation ("com.google.firebase:firebase-storage:20.3.0")

    implementation ("com.kakao.sdk:v2-all:2.20.6")
    implementation ("com.kakao.sdk:v2-user:2.20.6")
    implementation ("com.kakao.sdk:v2-share:2.20.6")
    implementation ("com.kakao.sdk:v2-talk:2.20.6")
    implementation ("com.kakao.sdk:v2-friend:2.20.6")
    implementation ("com.kakao.sdk:v2-navi:2.20.6")
    implementation ("com.kakao.sdk:v2-cert:2.20.6")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.camera.core)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.messaging)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}

