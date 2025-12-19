tasks.register("modifyOpenApi") {
    doLast {
        val openApiFile = file("src/main/resources/openapi/documentation.yaml")
        val inntektEndringAarsakFile = file("src/main/resources/openapi/inntektEndringAarsak.yaml")

        if (!openApiFile.exists()) {
            logger.error("OpenApi fil ikke funnet!")
            return@doLast
        }

        var content = openApiFile.readText()

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
          pattern: "^[.A-Za-zæøåÆØÅ0-9 _-]$" 
          minLength: 2
          maxLength: 64""",
                )
        content =
            content
                .replace(
                    "systemNavn:",
                    """systemNavn:
          pattern: "^[.A-Za-zæøåÆØÅ0-9 _-]$" 
          minLength: 2
          maxLength: 64""",
                )
        content =
            content
                .replace(
                    "systemVersjon:",
                    """systemVersjon:
          pattern: "^[.A-Za-zæøåÆØÅ0-9 _-]$" 
          minLength: 2
          maxLength: 64""",
                )
        // Legg til tags for å gruppere endepunkter i denne rekkefølgen
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
                // Legg til header X-warning-limit-reached i response på alle POST-requests som returnerer 200 OK
                // (og slutter på "er:" pga greedy matching) - TODO: bør gjøres litt mer robust..
                Regex("""(er:\s+post:[\s\S]*?"200":\s*description:\s*"OK")\s*(content:)""") to
                    """$1
          headers:
            X-Warning-limit-reached:
              description: "Settes dersom resultatet av en spørring overskrider max antall entiteter (1000)"
              schema:
                type: integer
                example: 1000
          $2""",
                // GET /v1/forespoersel/{navReferanseId}
                Regex("""(  /v1/forespoersel/[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                    """$1$2$3:$2  tags:$2    - "Forespørsel om inntektsmelding"$2  summary: "Hent forespørsel"""",
                // POST /v1/forespoersler
                Regex("""(  /v1/forespoersler:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                    """$1$2$3$2  tags:$2    - "Forespørsel om inntektsmelding"$2  summary: Hent forespørsler""",
                // GET /v1/sykmelding/{sykmeldingId}
                Regex("""(  /v1/sykmelding/[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                    """$1$2$3:$2  tags:$2    - "Sykmelding"$2  summary: "Hent sykmelding"""",
                // POST /v1/sykmeldinger
                Regex("""(  /v1/sykmeldinger:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                    """$1$2$3$2  tags:$2    - "Sykmelding"$2  summary: "Hent sykmeldinger"""",
                // GET /v1/sykepengesoeknad/{soeknadId}
                Regex("""(  /v1/sykepengesoeknad/[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                    """$1$2$3:$2  tags:$2    - "Sykepengesøknad"$2  summary: "Hent sykepengesøknad"""",
                // POST /v1/sykepengesoeknader
                Regex("""(  /v1/sykepengesoeknader:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                    """$1$2$3$2  tags:$2    - "Sykepengesøknad"$2  summary: "Hent sykepengesøknader"""",
                // GET /v1/inntektsmelding/{inntektsmeldingId}
                Regex("""(  /v1/inntektsmelding/\{[^:]*:)(\s+)(get):(?!\s+tags:)""") to
                    """$1$2$3:$2  tags:$2    - "Inntektsmelding"$2  summary: "Hent inntektsmelding"""",
                // POST /v1/inntektsmeldinger
                Regex("""(  /v1/inntektsmeldinger:(?:[\s\S]*?)(\s+)post:)(?!\s+tags:)""") to
                    """$1$2  tags:$2    - "Inntektsmelding"$2  summary: "Hent inntektsmeldinger"""",
                // POST /v1/inntektsmelding
                Regex("""(  /v1/inntektsmelding:(?:[\s\S]*?)(\s+)post:)(?!\s+tags:)""") to
                    """$1$2  tags:$2    - "Inntektsmelding"$2  summary: "Send inn inntektsmelding"""",
                // Fjern helsesjekk-endepunkter
                Regex("""  /health/is-(?:alive|ready):[\s\S]*?(?=  /[^/]|$)""") to "",
                // Fjern metrics-endepunkt
                Regex("""  /metrics:[\s\S]*?(?=  /[^/]|$)""") to "",
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
