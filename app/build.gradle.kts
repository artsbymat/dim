import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseKeystore = rootProject.file("keystore.properties")
val releaseProperties = providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
    .asText.orNull?.let { contents -> Properties().apply { contents.reader().use(::load) } }

fun Properties.requireSigningProperty(name: String): String =
    getProperty(name)?.takeIf { it.isNotBlank() }
        ?: error("Missing '$name' in ${releaseKeystore.name}")

android {
    namespace = "com.example.dim"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.dim"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    val releaseSigning = releaseProperties?.let { properties ->
        signingConfigs.create("release") {
            storeFile = rootProject.file(properties.requireSigningProperty("storeFile"))
            storePassword = properties.requireSigningProperty("storePassword")
            keyAlias = properties.requireSigningProperty("keyAlias")
            keyPassword = properties.requireSigningProperty("keyPassword")
            properties.getProperty("storeType")?.takeIf { it.isNotBlank() }?.let { storeType = it }
        }
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
