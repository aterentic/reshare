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

// Termux binary download configuration
val termuxBaseUrl = "https://packages.termux.dev/apt/termux-main/pool/main"
val termuxArchitectures = mapOf(
    "aarch64" to "arm64-v8a",
    "x86_64" to "x86_64"
)

// Pandoc packages
val pandocPackages = mapOf(
    "pandoc" to "p/pandoc/pandoc_3.7.0.2-2",
    "zlib" to "z/zlib/zlib_1.3.1-1",
    "libiconv" to "libi/libiconv/libiconv_1.18-1",
    "libgmp" to "libg/libgmp/libgmp_6.3.0-2",
    "lua54" to "l/lua54/lua54_5.4.8-6",
    "libffi" to "libf/libffi/libffi_3.4.7-1"
)

// Poppler packages (pdftohtml + dependencies not already in pandocPackages)
val popplerPackages = mapOf(
    "poppler" to "p/poppler/poppler_24.05.0-5",
    "fontconfig" to "f/fontconfig/fontconfig_2.17.1-1",
    "freetype" to "f/freetype/freetype_2.14.1",
    "libpng" to "libp/libpng/libpng_1.6.54",
    "libjpeg-turbo" to "libj/libjpeg-turbo/libjpeg-turbo_3.1.3",
    "libtiff" to "libt/libtiff/libtiff_4.7.1",
    "littlecms" to "l/littlecms/littlecms_2.18",
    "openjpeg" to "o/openjpeg/openjpeg_2.5.4",
    "libcurl" to "libc/libcurl/libcurl_8.18.0-1",
    "openssl" to "o/openssl/openssl_1%3A3.6.1",
    "libnghttp2" to "libn/libnghttp2/libnghttp2_1.68.0-1",
    "libnghttp3" to "libn/libnghttp3/libnghttp3_1.15.0",
    "libngtcp2" to "libn/libngtcp2/libngtcp2_1.20.0",
    "libssh2" to "libs/libssh2/libssh2_1.11.1-1",
    "libnspr" to "libn/libnspr/libnspr_4.38.2",
    "libnss" to "libn/libnss/libnss_3.120",
    "gpgme" to "g/gpgme/gpgme_2.0.1",
    "gpgmepp" to "g/gpgmepp/gpgmepp_2.0.0",
    "libassuan" to "liba/libassuan/libassuan_3.0.2-1",
    "libgpg-error" to "libg/libgpg-error/libgpg-error_1.58",
    "libexpat" to "libe/libexpat/libexpat_2.7.4",
    "brotli" to "b/brotli/brotli_1.2.0",
    "libbz2" to "libb/libbz2/libbz2_1.0.8-8",
    "liblzma" to "libl/liblzma/liblzma_5.8.2",
    "zstd" to "z/zstd/zstd_1.5.7-1",
    "libc++" to "libc/libc++/libc++_29"
)

val allPackages = pandocPackages + popplerPackages

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

val popplerFileMapping = mapOf(
    "poppler" to mapOf(
        "data/data/com.termux/files/usr/bin/pdftohtml" to "libpdftohtml.so",
        "data/data/com.termux/files/usr/lib/libpoppler.so" to "libpoppler.so"
    ),
    "fontconfig" to mapOf("data/data/com.termux/files/usr/lib/libfontconfig.so" to "libfontconfig.so"),
    "freetype" to mapOf("data/data/com.termux/files/usr/lib/libfreetype.so" to "libfreetype.so"),
    "libpng" to mapOf("data/data/com.termux/files/usr/lib/libpng16.so" to "libpng16.so"),
    "libjpeg-turbo" to mapOf("data/data/com.termux/files/usr/lib/libjpeg.so.8.3.2" to "libjpeg.so"),
    "libtiff" to mapOf("data/data/com.termux/files/usr/lib/libtiff.so" to "libtiff.so"),
    "littlecms" to mapOf("data/data/com.termux/files/usr/lib/liblcms2.so" to "liblcms2.so"),
    "openjpeg" to mapOf("data/data/com.termux/files/usr/lib/libopenjp2.so" to "libopenjp2.so"),
    "libcurl" to mapOf("data/data/com.termux/files/usr/lib/libcurl.so" to "libcurl.so"),
    "openssl" to mapOf(
        "data/data/com.termux/files/usr/lib/libcrypto.so.3" to "libcrypto.so",
        "data/data/com.termux/files/usr/lib/libssl.so.3" to "libssl.so"
    ),
    "libnghttp2" to mapOf("data/data/com.termux/files/usr/lib/libnghttp2.so" to "libnghttp2.so"),
    "libnghttp3" to mapOf("data/data/com.termux/files/usr/lib/libnghttp3.so" to "libnghttp3.so"),
    "libngtcp2" to mapOf(
        "data/data/com.termux/files/usr/lib/libngtcp2.so" to "libngtcp2.so",
        "data/data/com.termux/files/usr/lib/libngtcp2_crypto_ossl.so" to "libngtcp2_crypto_ossl.so"
    ),
    "libssh2" to mapOf("data/data/com.termux/files/usr/lib/libssh2.so" to "libssh2.so"),
    "libnspr" to mapOf(
        "data/data/com.termux/files/usr/lib/libnspr4.so" to "libnspr4.so",
        "data/data/com.termux/files/usr/lib/libplc4.so" to "libplc4.so",
        "data/data/com.termux/files/usr/lib/libplds4.so" to "libplds4.so"
    ),
    "libnss" to mapOf(
        "data/data/com.termux/files/usr/lib/libnss3.so" to "libnss3.so",
        "data/data/com.termux/files/usr/lib/libnssutil3.so" to "libnssutil3.so",
        "data/data/com.termux/files/usr/lib/libsmime3.so" to "libsmime3.so",
        "data/data/com.termux/files/usr/lib/libnssckbi.so" to "libnssckbi.so",
        "data/data/com.termux/files/usr/lib/libsoftokn3.so" to "libsoftokn3.so",
        "data/data/com.termux/files/usr/lib/libfreebl3.so" to "libfreebl3.so",
        "data/data/com.termux/files/usr/lib/libfreeblpriv3.so" to "libfreeblpriv3.so"
    ),
    "gpgme" to mapOf("data/data/com.termux/files/usr/lib/libgpgme.so" to "libgpgme.so"),
    "gpgmepp" to mapOf("data/data/com.termux/files/usr/lib/libgpgmepp.so" to "libgpgmepp.so"),
    "libassuan" to mapOf("data/data/com.termux/files/usr/lib/libassuan.so" to "libassuan.so"),
    "libgpg-error" to mapOf("data/data/com.termux/files/usr/lib/libgpg-error.so" to "libgpg-error.so"),
    "libexpat" to mapOf("data/data/com.termux/files/usr/lib/libexpat.so.1.11.2" to "libexpat.so"),
    "brotli" to mapOf(
        "data/data/com.termux/files/usr/lib/libbrotlicommon.so" to "libbrotlicommon.so",
        "data/data/com.termux/files/usr/lib/libbrotlidec.so" to "libbrotlidec.so"
    ),
    "libbz2" to mapOf("data/data/com.termux/files/usr/lib/libbz2.so.1.0.8" to "libbz2.so"),
    "liblzma" to mapOf("data/data/com.termux/files/usr/lib/liblzma.so.5.8.2" to "liblzma.so"),
    "zstd" to mapOf("data/data/com.termux/files/usr/lib/libzstd.so.1.5.7" to "libzstd.so"),
    "libc++" to mapOf("data/data/com.termux/files/usr/lib/libc++_shared.so" to "libc++_shared.so")
)

val allFileMapping = pandocFileMapping + popplerFileMapping

tasks.register("downloadPandoc") {
    group = "pandoc"
    description = "Downloads and extracts Pandoc and Poppler binaries from Termux packages"

    val jniLibsDir = file("src/main/jniLibs")

    outputs.dir(jniLibsDir)

    doLast {
        // Check if all files already exist
        val allFilesExist = termuxArchitectures.values.all { jniDir ->
            val archDir = jniLibsDir.resolve(jniDir)
            allFileMapping.values.flatMap { it.values }.all { targetName ->
                archDir.resolve(targetName).exists()
            }
        }

        if (allFilesExist) {
            println("All binaries already exist, skipping download")
            return@doLast
        }

        val tempDir = file("${layout.buildDirectory.get()}/pandoc-temp")
        tempDir.mkdirs()

        try {
            for ((termuxArch, jniDir) in termuxArchitectures) {
                val archOutputDir = jniLibsDir.resolve(jniDir)
                archOutputDir.mkdirs()

                println("Processing architecture: $termuxArch -> $jniDir")

                for ((packageName, packagePath) in allPackages) {
                    val debUrl = "$termuxBaseUrl/$packagePath" + "_$termuxArch.deb"
                    val debFile = tempDir.resolve("${packageName}_$termuxArch.deb")

                    // Download .deb file
                    if (!debFile.exists()) {
                        println("  Downloading $packageName...")
                        downloadFile(debUrl, debFile)
                        println("    Downloaded: ${debFile.length() / 1024} KB")
                    }

                    // Extract files from .deb using ar and tar commands
                    val fileMapping = allFileMapping[packageName] ?: continue
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

        println("Binaries downloaded successfully to $jniLibsDir")
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
