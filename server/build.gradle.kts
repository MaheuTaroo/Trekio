plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.ktlint)
    application

    alias(libs.plugins.kotlinSerialization)
}

group = "pt.trekio"
version = "1.0.0"
application {
    mainClass.set("pt.trekio.server.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.logback)
    implementation(libs.kotlinx.serialization)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.kotlinxSerialization)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.openApi)
    implementation(libs.ktor.server.routingOpenApi)
    implementation(libs.postgres.jdbc)
    implementation(libs.spring.security)
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
}

ktor {
    openApi {
        enabled = true
        codeInferenceEnabled = false
        onlyCommented = true
    }
}

/**
 * Docker related tasks
 */
val postgresImage = "trekio-postgres"

val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker" // Linux and others
    }
val dockerCompose = "docker/docker-compose.yml"

tasks.register<Exec>("buildPostgres") {
    commandLine(
        dockerExe,
        "build",
        "-t",
        postgresImage,
        "-f",
        "docker/Dockerfile-postgres",
        "docker",
    )
}

tasks.register<Exec>("dockerUp") {
    dependsOn("buildPostgres")
    commandLine(
        dockerExe,
        "compose",
        "-f",
        dockerCompose,
        "up",
        "--force-recreate",
        "-d",
    )
}

tasks.register<Exec>("dockerStart") {
    dependsOn("dockerUp")
    commandLine(dockerExe, "exec", postgresImage, "/app/bin/wait-for-postgres.sh", "localhost")
}

tasks.register<Exec>("dockerDown") {
    commandLine(dockerExe, "compose", "-f", dockerCompose, "down", "--volumes", "--remove-orphans")
}

tasks.named<JavaExec>("run") {
    if (project.hasProperty("useDb")) {
        dependsOn("dockerStart")
        finalizedBy("dockerDown")
    }
    standardInput = System.`in`
}

tasks.test {
    environment("TREKIO_ACCESS_TOKEN_LIFETIME", "20")
}
