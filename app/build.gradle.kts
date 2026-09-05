import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Load keystore properties from external file
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.fixupxer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fixupxer"
        minSdk = 21
        targetSdk = 36
        versionCode = 46
        versionName = "2.6.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add build config fields
        buildConfigField("String", "VERSION_NAME", "\"2.6.4\"")
        buildConfigField("int", "VERSION_CODE", "46")
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

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Include LICENSE files in APK/AAB for GPL compliance
            // Note: includes are handled via assets folder
        }
    }
    
    testOptions {
        animationsDisabled = true
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    
    // Material Design
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    
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
    implementation(libs.re2j)
    
    // Memory leak detection (debug only)
    debugImplementation(libs.leakcanary)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.gson) // JsonBasedCleanerTest čita test-cases.json
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib) // RecyclerView actions
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.espresso.accessibility)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.uiautomator)
} 
