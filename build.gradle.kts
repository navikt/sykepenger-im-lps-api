val kotlin_version: String by project
val logback_version: String by project
val tokenSupportVersion: String by project
val hagDomeneInntektsmeldingVersion: String by project
val utilsVersion: String by project
val mockOauth2ServerVersion: String by project
val kotestVersion: String by project
val mockkVersion: String by project
val ktorVersion: String by project
val exposedVersion: String by project
val flywayCoreVersion: String by project
val hikariVersion: String by project
val postgresqlVersion: String by project
val h2_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization")
    id("io.ktor.plugin") version "2.3.12"
    id("org.jmailen.kotlinter")
}

group = "no.nav.helsearbeidsgiver"
version = "0.0.1"

application {
    mainClass.set("no.nav.helsearbeidsgiver.ApplicationKt")

    //val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp")
}

repositories {
    val githubPassword: String by project
    mavenCentral()
    maven {
        setUrl("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-swagger")
    implementation("org.apache.kafka:kafka-clients:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("no.nav.helsearbeidsgiver:domene-inntektsmelding:$hagDomeneInntektsmeldingVersion")
    implementation("no.nav.helsearbeidsgiver:utils:$utilsVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayCoreVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")

    //implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("no.nav.helsearbeidsgiver:utils:$utilsVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.h2database:h2:$h2_version")
}
tasks {
    withType<Test> {
        useJUnitPlatform()
        //testLogging {
        //    events("skipped", "failed")
        //}
    }
    test {
        environment("MASKINPORTEN_SCOPES", "nav:inntektsmelding/lps.write")
        environment("MASKINPORTEN_WELL_KNOWN_URL", "http://localhost:33445/maskinporten/.well-known/openid-configuration")
    }
}
