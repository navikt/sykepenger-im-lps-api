val bakgrunnsjobbVersion = project.property("bakgrunnsjobbVersion") as String
val coroutineVersion = project.property("coroutineVersion") as String
val exposedVersion = project.property("exposedVersion") as String
val flywayCoreVersion = project.property("flywayCoreVersion") as String
val hagDomeneInntektsmeldingVersion = project.property("hagDomeneInntektsmeldingVersion") as String
val hikariVersion = project.property("hikariVersion") as String
val junitJupiterVersion = project.property("junitJupiterVersion") as String
val kafkaVersion = project.property("kafkaVersion") as String
val kotestVersion = project.property("kotestVersion") as String
val kotlinVersion = project.property("kotlinVersion") as String
val kotlinxSerializationVersion = project.property("kotlinxSerializationVersion") as String
val ktorVersion = project.property("ktorVersion") as String
val logbackEncoderVersion = project.property("logbackEncoderVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val mockOauth2ServerVersion = project.property("mockOauth2ServerVersion") as String
val mockkVersion = project.property("mockkVersion") as String
val pdpClientVersion = project.property("pdpClientVersion") as String
val postgresqlVersion = project.property("postgresqlVersion") as String
val swaggerVersion = project.property("swaggerVersion") as String
val testContainerVersion = project.property("testContainerVersion") as String
val tokenSupportVersion = project.property("tokenSupportVersion") as String
val unleashVersion = project.property("unleashVersion") as String
val utilsVersion = project.property("utilsVersion") as String
val pdlKlientVersion = project.property("pdlKlientVersion") as String
val microMeterVersion = project.property("microMeterVersion") as String
val inntektKlientVersion = project.property("inntektKlientVersion") as String
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    id("org.jmailen.kotlinter")
}

group = "no.nav.helsearbeidsgiver"
version = "0.0.1"

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("no.nav.helsearbeidsgiver.ApplicationKt")
}

repositories {
    val githubPassword = project.property("githubPassword") as String
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
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-routing-openapi:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
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
    implementation("no.nav.helsearbeidsgiver:inntekt-klient:$inntektKlientVersion")
    api("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    testImplementation(testFixtures("no.nav.helsearbeidsgiver:utils:$utilsVersion"))
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutineVersion")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainerVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}
ktor {
    openApi {
        enabled = true
        codeInferenceEnabled = false
        onlyCommented = true
    }
}
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
        environment(
            "TOKEN_X_WELL_KNOWN_URL",
            "http://localhost:33445/tokenx/.well-known/openid-configuration",
        )
        environment("ALTINN_IM_RESSURS", "nav_sykepenger_inntektsmelding")
        environment("ALTINN_SM_RESSURS", "nav_sykepenger_sykmelding")
        environment("ALTINN_SOEKNAD_RESSURS", "nav_sykepenger_soeknad")
    }
}
