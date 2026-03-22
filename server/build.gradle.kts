plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application

    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "pt.trekio"
version = "1.0.0"
application {
    mainClass.set("pt.trekio.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.logback)
    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.kotlinxSerialization)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
