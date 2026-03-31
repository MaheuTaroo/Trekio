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
    implementation(libs.ktor.kotlinxSerialization)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.openApi)
    implementation(libs.ktor.server.routingOpenApi)
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.ktor.client.contentNegotiation)
}

ktor {
    openApi {
        enabled = true
        codeInferenceEnabled = false
        onlyCommented = true
    }
}
