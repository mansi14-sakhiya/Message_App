plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.app.messageapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.messageapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        allWarningsAsErrors = true
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.room.run.time)
    implementation (libs.gson)

    //Manage View Size
    implementation (libs.ssp.android)
    implementation (libs.managesize)

    implementation (libs.tab.indicator)

    /* Glide - Image Loading */
    implementation (libs.glide.bumptech)
    implementation (libs.glide.bumptech.compiler)
    implementation (libs.glide.jp.transformation)
}

tasks.register<DefaultTask>("checkKotlinFileLength") {
    doLast {
        val maxLines = 1000 // Set the maximum number of lines
        val exceededFiles = mutableListOf<String>()
        val srcDir = file("src")

        // Check all Kotlin files
        fileTree(srcDir).matching {
            include("**/*.kt")
        }.forEach { file ->
            val lineCount = file.readLines().size
            if (lineCount > maxLines) {
                val relativePath = file.toPath().normalize().toAbsolutePath().toString()
                    .replace(srcDir.toPath().toAbsolutePath().toString(), "")
                    .replace("\\", "/")
                println("File $relativePath has $lineCount lines, which exceeds the maximum allowed of $maxLines lines.")
                exceededFiles.add(relativePath)
            }
        }

        if (exceededFiles.isNotEmpty()) {
            println("The following Kotlin files exceed the maximum allowed length of $maxLines lines:")
            exceededFiles.forEach { file ->
                println(" - $file")
            }
            throw GradleException("File length check :\n" + exceededFiles.joinToString("\n") + "\n The following Kotlin files exceed the maximum allowed length of 500 lines")
        } else {
            println("All Kotlin files are within the allowed length.")
        }
    }
}

// Ensure the checkKotlinFileLength task runs before the build
tasks.named("preBuild") {
    dependsOn(tasks.named("checkKotlinFileLength"))
}