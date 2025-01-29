tasks.register("modifyOpenApi") {
    doLast {
        val openApiFile = file("src/main/resources/openapi/documentation.yaml")
        if (!openApiFile.exists()) {
            logger.error("OpenApi fil ikke funnet!")
            return@doLast
        }

        var content = openApiFile.readText()
        var modified = false

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
  title: "Sykepenger inntektsmelding lps API"
  description: "API for å hente forespørsler og inntektsmeldinger for sykepenger"
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

        val newServersBlock = """servers:
  - url: http://localhost:8080
    description: Localhost
  - url: https://sykepenger-im-lps-api.ekstern.dev.nav.no
    description: Dev"""

        content =
            content
                .replace(
                    Regex("""servers:\s*- url:\s*"https://sykepenger_im_lps_api""""),
                    newServersBlock,
                ).also {
                    if (it != content) {
                        println("oppdatert servers block.")
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
