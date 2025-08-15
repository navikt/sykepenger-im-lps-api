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

        // Legg til tags seksjon for å gruppere endepunkter
        if (!content.contains("tags:")) {
            content =
                content.replace(
                    "openapi: \"3.1.0\"",
                    """openapi: "3.1.0"
tags:
  - name: "Sykmelding"
  - name: "Sykepengesøknad"
  - name: "Forespørsel"
  - name: "Inntektsmelding"
""",
                )
            println("Lagt til tags seksjon.")
            modified = true
        }

        val pathPatterns =
            mapOf(
                // Match any HTTP method for each endpoint type with cleaner formatting
                // Forespørsel endpoints
                Regex("""(  /v1/forespoersel[^:]*:)(\s+)(get|post|put|delete|patch):(?!\s+tags:)""") to
                    """$1$2$3:
$2  tags:
$2    - "Forespørsel"""",
                Regex("""(  /v1/forespoersler:)(\s+)(get:)(?!\s+tags:)""") to
                        """$1$2$3
$2  tags:
$2    - "Forespørsel"""",
                Regex("""(  /v1/forespoersler:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                        """$1$2$3
$2  tags:
$2    - "Forespørsel"""",
                // Sykmelding endpoints
                Regex("""(  /v1/sykmelding[^:]*:)(\s+)(get|post|put|delete|patch):(?!\s+tags:)""") to
                    """$1$2$3:
$2  tags:
$2    - "Sykmelding"""",
                Regex("""(  /v1/sykmeldinger:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                        """$1$2$3
$2  tags:
$2    - "Sykmelding"""",
                // Sykepengesøknad endpoints
                Regex("""(  /v1/sykepengesoeknad[^:]*:)(\s+)(get|post|put|delete|patch):(?!\s+tags:)""") to
                    """$1$2$3:
$2  tags:
$2    - "Sykepengesøknad"""",
                Regex("""(  /v1/sykepengesoeknader:(?:[\s\S]*?))(\s+)(post:)(?!\s+tags:)""") to
                        """$1$2$3
$2  tags:
$2    - "Sykepengesøknad"""",
                // Inntektsmelding endpoints
                Regex("""(  /v1/inntektsmelding[^:]*:)(\s+)(get|post|put|delete|patch):(?!\s+tags:)""") to
                        """$1$2$3:
$2  tags:
$2    - "Inntektsmelding"""",

                Regex("""(  /v1/inntektsmeldinger:)(\s+)(get):(?!\s+tags:)""") to
                        """$1$2$3:
$2  tags:
$2    - "Inntektsmelding"""",
                Regex("""(  /v1/inntektsmeldinger:(?:[\s\S]*?)(\s+)post:)(?!\s+tags:)""") to
                        """$1
$2  tags:
$2    - "Inntektsmelding"""",
            )

        var newContent = content
        for ((pattern, replacement) in pathPatterns) {
            newContent = pattern.replace(newContent, replacement)
        }

        if (newContent != content) {
            println("Lagt til tags til endepunkter.")
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
