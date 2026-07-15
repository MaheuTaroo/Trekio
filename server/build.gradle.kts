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
    implementation(libs.lettuce.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.postgres.jdbc)
    implementation(libs.spring.security)
    implementation(projects.shared)

    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
}

ktor {
    openApi {
        enabled = true
        codeInferenceEnabled = false
    }
}

/**
 * Docker related tasks
 */
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

tasks.register<Exec>("dockerUp") {
    description = "Raises local PostgreSQL and Redis container instances on Docker."
    if (project.hasProperty("useDb")) {
        println("Using DB")
        commandLine(
            dockerExe,
            "compose",
            "--profile",
            "db",
            "-f",
            dockerCompose,
            "up",
            "--force-recreate",
            "-d",
        )
    } else {
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
}

tasks.register<Exec>("waitForDatabase") {
    description = "Awaits for full PostgreSQL initialization."
    dependsOn("dockerUp")
    if (project.hasProperty("useDb")) {
        commandLine(dockerExe, "exec", "database", "/trekio-app/bin/wait-for-postgres.sh", "localhost")
    }
}

tasks.register<Exec>("dockerDown") {
    description = "Destroys the previously raised containers."
    if (project.hasProperty("useDb")) {
        commandLine(
            dockerExe,
            "compose",
            "--profile",
            "db",
            "-f",
            dockerCompose,
            "down",
            "--volumes",
            "--remove-orphans",
        )
    } else {
        commandLine(dockerExe, "compose", "-f", dockerCompose, "down", "--volumes", "--remove-orphans")
    }
}

tasks.named<JavaExec>("run") {
    if (project.hasProperty("useDb")) {
        dependsOn("waitForDatabase")
    } else {
        dependsOn("dockerUp")
    }
    finalizedBy("dockerDown")
    standardInput = System.`in`
}

tasks.register<Exec>("ensureDatabase") {
    description = "Ensures database in tests"
    commandLine("${project.projectDir.parent}\\gradlew.bat", ":server:waitForDatabase", "-PuseDb=1")
}

tasks.register<Exec>("stopDbAfterTests") {
    description = "Shuts down the aatabase after tests"
    commandLine("${project.projectDir.parent}\\gradlew.bat", ":server:dockerDown", "-PuseDb=1")
}

tasks.test {
    environment("TREKIO_ACCESS_TOKEN_LIFETIME", "20")
    dependsOn("ensureDatabase")
    finalizedBy("stopDbAfterTests")
}

tasks.register<Copy>("copyRuntimeDependencies") {
    description = "Copies the runtime dependencies to the build/libs directory."
    into("build/libs")
    from(configurations.runtimeClasspath)
}

tasks.startShadowScripts {
    mustRunAfter("copyRuntimeDependencies")
}

tasks.jar {
    dependsOn("copyRuntimeDependencies")
    manifest {
        attributes["Main-Class"] = "pt.trekio.server.ApplicationKt"
        attributes["Class-Path"] =
            "server-1.0.0 " + configurations.runtimeClasspath.get().joinToString(" ") { it.name }
    }
}
