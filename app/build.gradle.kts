import java.net.HttpURLConnection
import java.net.URI
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val versionFromEnv: String = System.getenv("VERSION_NAME") ?: "1.0"
val versionCodeFromEnv: Int = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.reshare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reshare"
        minSdk = 26
        targetSdk = 34
        versionCode = versionCodeFromEnv
        versionName = versionFromEnv

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (keystorePropertiesFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
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
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
            jniLibs.srcDirs("src/main/jniLibs")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/kotlin")
            assets.srcDirs("src/androidTest/assets")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.google.truth:truth:1.1.5")
}

// Pandoc binary download configuration
val pandocBaseUrl = "https://packages.termux.dev/apt/termux-main/pool/main"
val pandocArchitectures = mapOf(
    "aarch64" to "arm64-v8a",
    "x86_64" to "x86_64"
)
val pandocPackages = mapOf(
    "pandoc" to "p/pandoc/pandoc_3.7.0.2-2",
    "zlib" to "z/zlib/zlib_1.3.1-1",
    "libiconv" to "libi/libiconv/libiconv_1.18-1",
    "libgmp" to "libg/libgmp/libgmp_6.3.0-2",
    "lua54" to "l/lua54/lua54_5.4.8-6",
    "libffi" to "libf/libffi/libffi_3.4.7-1"
)

// Files to extract from each package and their target names
// Format: "path/in/tar" to "target_name"
val pandocFileMapping = mapOf(
    "pandoc" to mapOf("data/data/com.termux/files/usr/bin/pandoc" to "libpandoc.so"),
    "zlib" to mapOf("data/data/com.termux/files/usr/lib/libz.so.1.3.1" to "libz.so"),
    "libiconv" to mapOf("data/data/com.termux/files/usr/lib/libiconv.so" to "libiconv.so"),
    "libgmp" to mapOf("data/data/com.termux/files/usr/lib/libgmp.so" to "libgmp.so"),
    "lua54" to mapOf("data/data/com.termux/files/usr/lib/liblua5.4.so.5.4.8" to "liblua5.4.so"),
    "libffi" to mapOf("data/data/com.termux/files/usr/lib/libffi.so" to "libffi.so")
)

tasks.register("downloadPandoc") {
    group = "pandoc"
    description = "Downloads and extracts Pandoc binaries from Termux packages"

    val jniLibsDir = file("src/main/jniLibs")

    outputs.dir(jniLibsDir)

    doLast {
        // Check if all files already exist
        val allFilesExist = pandocArchitectures.values.all { jniDir ->
            val archDir = jniLibsDir.resolve(jniDir)
            pandocFileMapping.values.flatMap { it.values }.all { targetName ->
                archDir.resolve(targetName).exists()
            }
        }

        if (allFilesExist) {
            println("All Pandoc binaries already exist, skipping download")
            return@doLast
        }

        val tempDir = file("${layout.buildDirectory.get()}/pandoc-temp")
        tempDir.mkdirs()

        try {
            for ((termuxArch, jniDir) in pandocArchitectures) {
                val archOutputDir = jniLibsDir.resolve(jniDir)
                archOutputDir.mkdirs()

                println("Processing architecture: $termuxArch -> $jniDir")

                for ((packageName, packagePath) in pandocPackages) {
                    val debUrl = "$pandocBaseUrl/$packagePath" + "_$termuxArch.deb"
                    val debFile = tempDir.resolve("${packageName}_$termuxArch.deb")

                    // Download .deb file
                    if (!debFile.exists()) {
                        println("  Downloading $packageName...")
                        downloadFile(debUrl, debFile)
                        println("    Downloaded: ${debFile.length() / 1024} KB")
                    }

                    // Extract files from .deb using ar and tar commands
                    val fileMapping = pandocFileMapping[packageName] ?: continue
                    for ((sourcePath, targetName) in fileMapping) {
                        val targetFile = archOutputDir.resolve(targetName)
                        if (targetFile.exists()) {
                            println("  $targetName already exists, skipping")
                            continue
                        }

                        println("  Extracting $targetName from $packageName...")
                        extractFromDeb(debFile, sourcePath, targetFile, tempDir)
                        targetFile.setExecutable(true)
                        println("    Extracted: ${targetFile.length() / 1024} KB")
                    }
                }
            }
        } finally {
            // Clean up temp directory
            tempDir.deleteRecursively()
        }

        println("Pandoc binaries downloaded successfully to $jniLibsDir")
    }
}

fun downloadFile(url: String, target: File) {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = 30000
    connection.readTimeout = 300000
    connection.instanceFollowRedirects = true

    if (connection.responseCode != 200) {
        throw GradleException("Failed to download $url: HTTP ${connection.responseCode}")
    }

    connection.inputStream.use { input ->
        target.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

fun extractFromDeb(debFile: File, sourcePath: String, targetFile: File, tempDir: File) {
    // Create a working directory for extraction
    val extractDir = tempDir.resolve("extract_${System.currentTimeMillis()}")
    extractDir.mkdirs()

    try {
        // Extract .deb (ar archive): use "ar" on Linux, fall back to "tar" (bsdtar on macOS)
        val arAvailable = try {
            ProcessBuilder("ar", "--version").start().waitFor() == 0
        } catch (_: Exception) { false }

        if (arAvailable) {
            val arProcess = ProcessBuilder("ar", "x", debFile.absolutePath)
                .directory(extractDir)
                .redirectErrorStream(true)
                .start()
            val arResult = arProcess.waitFor()
            if (arResult != 0) {
                throw GradleException("Failed to extract .deb file with ar: ${arProcess.inputStream.bufferedReader().readText()}")
            }
        } else {
            val debExtractProcess = ProcessBuilder("tar", "-xf", debFile.absolutePath)
                .directory(extractDir)
                .redirectErrorStream(true)
                .start()
            val debResult = debExtractProcess.waitFor()
            if (debResult != 0) {
                throw GradleException("Failed to extract .deb file with tar: ${debExtractProcess.inputStream.bufferedReader().readText()}")
            }
        }

        // Find the data archive (could be data.tar.xz, data.tar.gz, or data.tar.zst)
        val dataArchive = extractDir.listFiles()?.find { it.name.startsWith("data.tar") }
            ?: throw GradleException("No data.tar.* found in ${debFile.name}")

        // Extract the specific file from the data archive
        // Paths may have "./" prefix (GNU tar) or not (bsdtar), try both
        val tarProcess = ProcessBuilder("tar", "-xf", dataArchive.absolutePath, "./$sourcePath", sourcePath)
            .directory(extractDir)
            .redirectErrorStream(true)
            .start()
        tarProcess.waitFor()

        // Copy the extracted file to the target location (check both path variants)
        val extractedFile = listOf(extractDir.resolve(sourcePath), extractDir.resolve("./$sourcePath"))
            .firstOrNull { it.exists() }
            ?: throw GradleException("Extracted file not found: $sourcePath")

        targetFile.parentFile?.mkdirs()
        extractedFile.copyTo(targetFile, overwrite = true)
    } finally {
        extractDir.deleteRecursively()
    }
}
