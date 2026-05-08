import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

// App version: env-var override (CI uses this) → version catalog. Read
// once here so both the Android manifest and the BuildKonfig block see
// the same numbers without diverging.
val appVersionName: String = System.getenv("VERSION_NAME")
    ?: libs.versions.app.versionName.get()
val appVersionCode: Int = System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: libs.versions.app.versionCode.get().toInt()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // play-services-location + coroutines-play-services were the
            // dependency stack for the hand-rolled FusedLocationProvider
            // wrapper. Compass pulls them in transitively now (its own
            // FLP impl). Keeping these as explicit deps would double-pin
            // the version and silently shadow Compass's resolution.
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.datetime.names)
            implementation(libs.datastore.preferences)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.navigation.compose)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
            implementation(libs.kmp.maps.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.compass.geolocation)
            implementation(libs.compass.geolocation.mobile)
        }
    }
}

android {
    namespace = "com.novawerk.berlinfoodmap"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.novawerk.berlinfoodmap"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        manifestPlaceholders["MAPS_API_KEY"] = secrets.getProperty("MAPS_API_KEY", "")
    }
    signingConfigs {
        create("release") {
            storeFile = file(
                System.getenv("KEYSTORE_PATH") ?: "${rootProject.projectDir}/berlinfoodmap-release.jks"
            )
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Exposes `BuildKonfig.VERSION_NAME` and `BuildKonfig.VERSION_CODE` to
// commonMain — used by the Settings screen so the displayed version
// always matches whatever Android+iOS were built with, no risk of the
// hardcoded UI string drifting from the actual build.
buildkonfig {
    packageName = "com.novawerk.berlinfoodmap"
    defaultConfigs {
        buildConfigField(STRING, "VERSION_NAME", appVersionName)
        buildConfigField(INT, "VERSION_CODE", appVersionCode.toString())
    }
}

// Pushes the canonical version (libs.versions.toml) into the iOS
// xcconfig so xcodebuild picks it up. Run from CI after a version bump
// (see .github/workflows/bump-version.yml). No-op when the xcconfig is
// missing — first-time iOS contributors copy the template.
tasks.register("syncVersionToIos") {
    description = "Sync app version from version catalog into iosApp Config.xcconfig."
    val versionCode = libs.versions.app.versionCode.get()
    val versionName = libs.versions.app.versionName.get()
    val xcconfigFile = rootProject.file("iosApp/Configuration/Config.xcconfig")
    doLast {
        if (!xcconfigFile.exists()) {
            println("Skipped: ${xcconfigFile.relativeTo(rootProject.projectDir)} not found.")
            return@doLast
        }
        val content = xcconfigFile.readText()
            .replace(Regex("CURRENT_PROJECT_VERSION=.*"), "CURRENT_PROJECT_VERSION=$versionCode")
            .replace(Regex("MARKETING_VERSION=.*"), "MARKETING_VERSION=$versionName")
        xcconfigFile.writeText(content)
        println("Synced iOS version: $versionName ($versionCode)")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    debugImplementation(compose.uiTooling)
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}
