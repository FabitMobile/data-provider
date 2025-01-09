plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "ru.fabit.dataprovider.remoteservice"
    compileSdk = 34

    defaultConfig {
        minSdk = 19

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core"))

    implementation(libs.coroutines)
    implementation(libs.retrofit2)
    implementation(libs.loggingInterceptor)
    implementation(libs.exception)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junit.ktx)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}

apply(from = "../android_publish.gradle")