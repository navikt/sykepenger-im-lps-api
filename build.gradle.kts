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
val inntektKlientVersion = project.property("inntektKlientVersion")
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    id("org.jmailen.kotlinter")
}

group = "no.nav.helsearbeidsgiver"
version = "0.0.1"

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
    register("modifyOpenApi") {
        doLast {
            val openApiFile = file("src/main/resources/openapi/documentation.yaml")
            val inntektEndringAarsakFile = file("src/main/resources/openapi/inntektEndringAarsak.yaml")

            if (!openApiFile.exists()) {
                logger.error("OpenApi fil ikke funnet!")
                return@doLast
            }

            var content = openApiFile.readText()

            fun removeGroup(path: String): Pair<Regex, String> {
                val escapedPath = Regex.escape(path)
                return Regex("""  $escapedPath[\s\S]*?(?=^  \S|\z)""", RegexOption.MULTILINE) to ""
            }

            var modified = false
            val targetRegex =
                Regex(
                    """\s*InntektEndringAarsak:\s*type:\s*"object"\s*properties:\s*\{\s*\}""",
                )

            if (targetRegex.containsMatchIn(content)) {
                val inntektEndring = inntektEndringAarsakFile.readText()
                content =
                    content
                        .replace(
                            Regex("""\s*InntektEndringAarsak:\s*type:\s*"object"\s*properties:\s*\{\s*\}"""),
                            Regex.escapeReplacement(inntektEndring),
                        )
                println("lagt til InntektEndringAarsak.")
                modified = true
            }

            if (!content.contains("securitySchemes:")) {
                content =
                    content.replace(
                        Regex("components:"),
                        """servers:
  - url: https://sykepenger-api.ekstern.dev.nav.no
  - url: https://sykepenger-api.nav.no
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
""",
                    )
                println("lagt til securitySchemes og server url.")
                modified = true
            }

            val newInfoBlock = """info:
  title: "Sykepenger API"
  description: "API for sykmelding, sykepengesøknad og inntektsmelding for sykepenger"
  version: "1.0.0""""

            content =
                content
                    .replace(
                        Regex(
                            """info:\s*title:\s*"sykepenger_im_lps_api API"\s*description:\s*"sykepenger_im_lps_api API"\s*version:\s*"1.0.0"""",
                        ),
                        newInfoBlock,
                    ).also {
                        if (it != content) {
                            println("oppdatert info block.")
                            modified = true
                        }
                    }

            content =
                content
                    .replace(
                        Regex("""servers:\s*- url:\s*"https://sykepenger_im_lps_api""""),
                        """
security:
  - bearerAuth: [ ]""",
                    ).also {
                        if (it != content) {
                            println("oppdatert security block.")
                            modified = true
                        }
                    }

            content =
                content
                    .replace(
                        "kontaktinformasjon:",
                        """kontaktinformasjon:
          pattern: "^[.A-Za-zæøåÆØÅ0-9, _-]$" 
          minLength: 2
          maxLength: 64""",
                    )
            content =
                content
                    .replace(
                        "systemNavn:",
                        """systemNavn:
          pattern: "^[.A-Za-zæøåÆØÅ0-9, _-]$" 
          minLength: 2
          maxLength: 64""",
                    )
            content =
                content
                    .replace(
                        "systemVersjon:",
                        """systemVersjon:
          pattern: "^[.A-Za-zæøåÆØÅ0-9, _-]$" 
          minLength: 2
          maxLength: 64""",
                    )
            if (!content.contains("tags:")) {
                content =
                    content.replace(
                        "openapi: \"3.1.0\"",
                        """openapi: "3.1.0"
tags:
  - name: "Sykmelding"
  - name: "Sykepengesøknad"
  - name: "Forespørsel om inntektsmelding"
  - name: "Inntektsmelding"
""",
                    )
                modified = true
            }

            val pathPatterns =
                mapOf(
                    Regex("""(er:\s+post:[\s\S]*?"200":\s*description:\s*"OK")\s*(content:)""") to
                        """$1
          headers:
            X-Warning-limit-reached:
              description: "Settes dersom resultatet av en spørring overskrider max antall entiteter (1000)"
              schema:
                type: integer
                example: 1000
          $2""",
                    Regex("""(  /v1/forespoersel/[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                        """$1$2$3:$2  tags:$2    - "Forespørsel om inntektsmelding"$2  summary: "Hent forespørsel"""",
                    Regex("""(  /v1/forespoersler:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                        """$1$2$3$2  tags:$2    - "Forespørsel om inntektsmelding"$2  summary: Hent forespørsler""",
                    Regex("""(  /v1/sykmelding/[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                        """$1$2$3:$2  tags:$2    - "Sykmelding"$2  summary: "Hent sykmelding"""",
                    Regex("""(  /v1/sykmeldinger:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                        """$1$2$3$2  tags:$2    - "Sykmelding"$2  summary: "Hent sykmeldinger"""",
                    Regex("""(  /v1/sykepengesoeknad/[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                        """$1$2$3:$2  tags:$2    - "Sykepengesøknad"$2  summary: "Hent sykepengesøknad"""",
                    Regex("""(  /v1/sykepengesoeknader:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                        """$1$2$3$2  tags:$2    - "Sykepengesøknad"$2  summary: "Hent sykepengesøknader"""",
                    Regex("""(  /v1/inntektsmelding/\{[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                        """$1$2$3:$2  tags:$2    - "Inntektsmelding"$2  summary: "Hent inntektsmelding"""",
                    Regex("""(  /v1/inntektsmeldinger:(?:[\s\S]*?)(\s+)post:)(?!\s+tags:)""") to
                        """$1$2  tags:$2    - "Inntektsmelding"$2  summary: "Hent inntektsmeldinger"""",
                    Regex("""(  /v1/inntektsmelding:(?:[\s\S]*?)(\s+)post:)(?!\s+tags:)""") to
                        """$1$2  tags:$2    - "Inntektsmelding"$2  summary: "Send inn inntektsmelding"""",
                    Regex("""  /health/is-(?:alive|ready):[\s\S]*?(?=  /[^/]|$)""") to "",
                    Regex("""  /metrics:[\s\S]*?(?=  /[^/]|$)""") to "",
                    removeGroup("/intern/personbruker/sykmelding/{sykmeldingId}/pdf:"),
                    removeGroup("/intern/personbruker/sykepengesoeknad/{soknadId}/pdf:"),
                    removeGroup("/v1/sykmelding/{sykmeldingId}.pdf:"),
                    Regex("""(application/pdf:\s*schema:\s*type:\s*"string"\s*format:\s*)"byte"""") to """$1"binary"""",
                )

            var newContent = content
            for ((pattern, replacement) in pathPatterns) {
                newContent = pattern.replace(newContent, replacement)
            }

            if (newContent != content) {
                println("Lagt til tags og sammendrag til endepunkter.")
                content = newContent
                modified = true
            }

            if (modified) {
                openApiFile.writeText(content)
                println("OpenApi fil oppdatert.")
            } else {
                println("OpenApi fil er allerede oppdatert. Ingen endringer.")
            }
        }
    }
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
