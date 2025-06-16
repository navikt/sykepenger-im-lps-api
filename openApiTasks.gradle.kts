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
                    """components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
""",
                )
            println("lagt til securitySchemes.")
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

        if (modified) {
            openApiFile.writeText(content)
            println("OpenApi fil oppdatert.")
        } else {
            println("OpenApi fil er allerede oppdatert. Ingen endringer.")
        }
    }
}
