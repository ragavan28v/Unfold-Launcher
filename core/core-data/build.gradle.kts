plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.volt.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.incremental", "false")
    }
}

dependencies {
    api(project(":core:core-domain"))
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // Datastore
    implementation(libs.datastore.proto)
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation("com.google.dagger:dagger:2.52")
    kapt("com.google.dagger:dagger-compiler:2.52")
    api("javax.inject:javax.inject:1")
    
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core.ktx)
}
