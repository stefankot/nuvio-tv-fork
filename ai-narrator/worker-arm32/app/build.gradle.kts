plugins { id("com.android.application") }

val keyFile = System.getenv("NUVIO_RELEASE_STORE_FILE").orEmpty()
val releaseAlias = System.getenv("NUVIO_RELEASE_KEY_ALIAS").orEmpty()
val releaseKeyPassword = System.getenv("NUVIO_RELEASE_KEY_PASSWORD").orEmpty()
val releaseStorePassword = System.getenv("NUVIO_RELEASE_STORE_PASSWORD").orEmpty()

android {
    namespace = "com.nuvio.tv.zonos32"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.nuvio.tv.zonos32"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += "armeabi-v7a" }
    }
    signingConfigs {
        create("nuvio") {
            if (keyFile.isNotBlank()) storeFile = file(keyFile)
            keyAlias = releaseAlias
            keyPassword = releaseKeyPassword
            storePassword = releaseStorePassword
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (keyFile.isNotBlank() && releaseAlias.isNotBlank() &&
                releaseKeyPassword.isNotBlank() && releaseStorePassword.isNotBlank()) {
                signingConfig = signingConfigs.getByName("nuvio")
            }
        }
    }
    packaging { jniLibs { useLegacyPackaging = true } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register("verifyNativeWorker") {
    doLast {
        val worker = file("src/main/jniLibs/armeabi-v7a/libzonos3_server.so")
        check(worker.isFile && worker.length() > 100_000L) {
            "Missing cross-compiled ARM32 executable: $worker"
        }
        check(keyFile.isNotBlank() && keyAlias.isNotBlank() &&
            keyPassword.isNotBlank() && storePassword.isNotBlank()) {
            "A signed companion requires the SAME NUVIO_RELEASE_* keystore as NuvioTV"
        }
    }
}
tasks.named("preBuild") { dependsOn("verifyNativeWorker") }
