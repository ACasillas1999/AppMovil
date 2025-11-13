plugins {
    id("com.android.application")
}



android {
    namespace = "com.example.app_pedidos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app_pedidos"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Configuración para controlar HTTP en claro por tipo de build
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        // Usar Java 17 para código fuente y bytecode
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.android.volley:volley:1.2.1")
     implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    implementation ("com.squareup.picasso:picasso:2.71828")

    implementation ("androidx.cardview:cardview:1.0.0")

    implementation ("com.google.android.gms:play-services-location:21.0.1")

    // SweetAlert removed to avoid external dependency during compile


    // implementation("com.mapbox.mapboxsdk:")
}
