import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

// Load keystore properties from external file
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.fixupxer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fixupxer"
        minSdk = 21
        targetSdk = 35
        versionCode = 26
        versionName = "1.4.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add build config fields
        buildConfigField("String", "VERSION_NAME", "\"1.4.8\"")
        buildConfigField("int", "VERSION_CODE", "26")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("app/keystore/fixupxer.keystore")
            storePassword = (keystoreProperties["storePassword"] as? String)?.trim() ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = (keystoreProperties["keyAlias"] as? String)?.trim() ?: System.getenv("KEY_ALIAS") ?: ""
            keyPassword = (keystoreProperties["keyPassword"] as? String)?.trim() ?: System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Include LICENSE files in APK/AAB for GPL compliance
            // Note: includes are handled via assets folder
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    
    // Google Play dependenciesInfo block for Play Store submissions
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }

    lint {
        abortOnError = true // still fail on real issues
        warningsAsErrors = false
        disable.addAll(listOf(
            "UnusedResources",
            "IconDuplicates",
            "VectorRaster",
            "IconDipSize",
            "IconLauncherShape",
            "IconLocation",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "UseTomlInstead",
            "ContentDescription",
            "LockedOrientationActivity",
            "DiscouragedApi",
            "UseAppTint"
        ))
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    
    // Material Design
    implementation(libs.material)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Logging
    implementation(libs.timber)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    
    // Memory leak detection (debug only)
    debugImplementation(libs.leakcanary)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1") // For RecyclerView actions
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1") // For Room testing
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.5.1") // For accessibility testing
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // For coroutines testing
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0") // For UI device testing
} 
