plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application

    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
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
    implementation(libs.lettuce.core)
    testImplementation(libs.ktor.client.contentNegotiation)
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
val redisImage = "trekio-redis"

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
    description = "Builds a local PostgreSQL container on Docker as an execution dependency."
    println("Building postgres...")
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

tasks.register<Exec>("buildRedis") {
    description = "Builds a local Redis container on Docker as an execution dependency."
    println("Building redis...")
    commandLine(
        dockerExe,
        "build",
        "-t",
        redisImage,
        "-f",
        "docker/Dockerfile-redis",
        "docker",
    )
}

tasks.register<Exec>("dockerUp") {
    description = "Raises local PostgreSQL and Redis container instances on Docker."
    dependsOn("buildRedis")
    if (project.hasProperty("useDb")) {
        println("Using DB")
        dependsOn("buildPostgres")
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
    }
    else {
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

tasks.register<Exec>("waitForPostgres") {
    description = "Awaits for full PostgreSQL initialization."
    commandLine(dockerExe, "exec", postgresImage, "/trekio-app/bin/wait-for-postgres.sh", "localhost")
}

tasks.register<Exec>("dockerDown") {
    description = "Destroys the previously raised containers."
    commandLine(dockerExe, "compose", "-f", dockerCompose, "down", "--volumes", "--remove-orphans")
}

tasks.named<JavaExec>("run") {
    dependsOn("dockerUp")
    if (project.hasProperty("useDb")) {
        println("Waiting for postgres...")
        dependsOn("awaitForPostgres")
    }
    finalizedBy("dockerDown")
    standardInput = System.`in`
}

tasks.test {
    environment("TREKIO_ACCESS_TOKEN_LIFETIME", "1")
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
            "2526-2-common.jar " + configurations.runtimeClasspath.get().joinToString(" ") { it.name }
    }
}
