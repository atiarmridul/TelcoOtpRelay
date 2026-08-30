plugins {
    id("com.android.application")
}

android {
    namespace = "com.mridul.telcootprelay"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.mridul.telcootprelay"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation("junit:junit:4.13.2")
}

val packageSmsRelayDebugApk by tasks.registering(Copy::class) {
    dependsOn("packageDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(layout.buildDirectory.dir("outputs/distribution"))
    rename { "SMS Relay.apk" }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(packageSmsRelayDebugApk)
}
