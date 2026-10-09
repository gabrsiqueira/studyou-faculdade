import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Lido de /mobile/.env (nao versionado — veja .env.example) para nao deixar o endereco
// do backend fixo no codigo. Veja app/src/main/java/.../data/remote/FileAnalysisApiClient.kt.
val envProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}
val backendBaseUrl: String = envProperties.getProperty("BACKEND_BASE_URL", "http://localhost:8000")

/**
 * Generates res/xml/network_security_config.xml allowing plain HTTP only to the backend host
 * configured in .env (plus the usual local-development addresses), so changing the IP in
 * .env never requires touching this file by hand. Everything else still requires HTTPS.
 */
abstract class GenerateNetworkSecurityConfig : DefaultTask() {
    @get:Input
    abstract val backendHost: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val hosts = linkedSetOf("localhost", "127.0.0.1", "10.0.2.2", backendHost.get()).filter { it.isNotBlank() }
        val domains = hosts.joinToString("\n") { "        <domain includeSubdomains=\"false\">$it</domain>" }
        val file = outputDir.get().file("xml/network_security_config.xml").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<!-- Generated from BACKEND_BASE_URL in /mobile/.env by app/build.gradle.kts — do not edit. -->
            |<network-security-config>
            |    <domain-config cleartextTrafficPermitted="true">
            |$domains
            |    </domain-config>
            |</network-security-config>
            |""".trimMargin()
        )
    }
}

val generateNetworkSecurityConfig = tasks.register<GenerateNetworkSecurityConfig>("generateNetworkSecurityConfig") {
    backendHost.set(URI(backendBaseUrl).host.orEmpty())
    outputDir.set(layout.buildDirectory.dir("generated/res/networkSecurityConfig"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(
            generateNetworkSecurityConfig,
            GenerateNetworkSecurityConfig::outputDir
        )
    }
}

android {
    namespace = "br.com.studyou"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.studyou"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
