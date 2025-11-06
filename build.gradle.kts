val bakgrunnsjobbVersion: String by project
val coroutineVersion: String by project
val exposedVersion: String by project
val flywayCoreVersion: String by project
val hagDomeneInntektsmeldingVersion: String by project
val hikariVersion: String by project
val kafkaVersion: String by project
val kotestVersion: String by project
val kotlinVersion: String by project
val kotlinxSerializationVersion: String by project
val ktorVersion: String by project
val logbackEncoderVersion: String by project
val logbackVersion: String by project
val mockOauth2ServerVersion: String by project
val mockkVersion: String by project
val pdpClientVersion: String by project
val postgresqlVersion: String by project
val swaggerVersion: String by project
val testContainerVersion: String by project
val tokenSupportVersion: String by project
val unleashVersion: String by project
val utilsVersion: String by project
val pdlKlientVersion: String by project
val microMeterVersion: String by project

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization")
    id("io.ktor.plugin") version "3.1.2"
    id("org.jmailen.kotlinter")
}

group = "no.nav.helsearbeidsgiver"
version = "0.0.1"

application {
    mainClass.set("no.nav.helsearbeidsgiver.ApplicationKt")
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
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.getunleash:unleash-client-java:$unleashVersion")
    implementation("io.ktor:ktor-client-apache5")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.swagger.core.v3:swagger-annotations:$swaggerVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")
    implementation("no.nav.helsearbeidsgiver:altinn-pdp-client:$pdpClientVersion")
    implementation("no.nav.helsearbeidsgiver:domene-inntektsmelding:$hagDomeneInntektsmeldingVersion")
    implementation("no.nav.helsearbeidsgiver:hag-bakgrunnsjobb:$bakgrunnsjobbVersion")
    implementation("no.nav.helsearbeidsgiver:pdl-client:$pdlKlientVersion")
    implementation("no.nav.helsearbeidsgiver:utils:$utilsVersion")
    implementation("no.nav.security:token-validation-ktor-v3:$tokenSupportVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayCoreVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$microMeterVersion")
    api("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    testImplementation(testFixtures("no.nav.helsearbeidsgiver:utils:$utilsVersion"))
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("no.nav.helsearbeidsgiver:utils:$utilsVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutineVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainerVersion")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainerVersion")
}
apply(from = "openApiTasks.gradle.kts")
tasks {
    withType<Test> {
        useJUnitPlatform()
    }
    test {
        testLogging {
            events("failed")
        }
        environment("database.embedded", "true")
        environment("EKSPONERT_MASKINPORTEN_SCOPES", "nav:helseytelser/sykepenger")
        environment(
            "MASKINPORTEN_WELL_KNOWN_URL",
            "http://localhost:33445/maskinporten/.well-known/openid-configuration",
        )
        environment("NAV_ARBEIDSGIVER_PORTAL_BASEURL", "https://arbeidsgiver.intern.dev.nav.no")
        environment("NAV_ARBEIDSGIVER_API_BASEURL", "https://sykepenger-im-lps-api.ekstern.dev.nav.no")
        environment("ALTINN_IM_RESSURS", "nav_sykepenger_inntektsmelding")
        environment("ALTINN_SM_RESSURS", "nav_sykepenger_sykmelding")
        environment("ALTINN_SOEKNAD_RESSURS", "nav_sykepenger_soeknad")
    }
}
