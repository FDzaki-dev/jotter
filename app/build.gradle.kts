import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.jotter.notes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jotter.notes"
        minSdk = 31
        targetSdk = 35
        // Versioning Lock: DILARANG bump manual di sini. versionCode & versionName datang dari
        // -PappVersionCode/-PappVersionName yang di-inject CI (release.yml) dari GITHUB_RUN_NUMBER.
        // Fallback di bawah HANYA aktif utk build lokal (gradle :app:assembleRelease tanpa CI) -
        // sengaja dibedakan ("-dev") supaya gak pernah ketuker sama build asli hasil CI.
        versionCode = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("appVersionName") as String?)
            ?: "${findProperty("jotterBaseVersion") as String? ?: "2.0.0"}-dev"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (keystorePropertiesFile.exists()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    // WAJIB eksplisit: MainActivity extends FragmentActivity (syarat BiometricPrompt), tapi
    // androidx.fragment TIDAK PERNAH dideklarasikan langsung di sini - sebelumnya cuma ke-tarik
    // transitif dari androidx.biometric:1.2.0-alpha05 (alpha lama, ~2021) di versi fragment yang
    // JAUH lebih tua dari activity-compose 1.9.3 di atas. Ketidakcocokan itu penyebab crash nyata
    // "Can only use lower 16 bits for requestCode" (lihat PROJECT_STATE.md v2_Batch43) - fragment
    // versi lama itu masih enforce cek 16-bit warisan lama, sedangkan ActivityResultRegistry
    // modern (dipakai rememberLauncherForActivityResult) generate request code RANDOM tanpa
    // dibatasi 16-bit. Pin eksplisit ke versi modern ini (bukan pin biometric) - fragment TIDAK
    // dipakai langsung di kode manapun di project ini, jadi 0 resiko breaking API change.
    implementation("androidx.fragment:fragment:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // In-app updater: cek & download rilis dari GitHub Releases API.
    // WAJIB streaming chunk-by-chunk (bukan readBytes() penuh ke RAM) - lihat ReleaseDownloader.kt.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
