package no.nav.helsearbeidsgiver.inntekt

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.GenericElement
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import no.nav.helsearbeidsgiver.plugins.ErrorResponse

fun Route.describeHentInntekt() =
    describe {
        tag("Inntekt")
        summary = "Hent inntekt"
        description = "Henter inntekter for de siste tre månedene og beregnet gjennomsnittsinntekt."

        parameters {
            query("navReferanseId") {
                description = "NAV referanse-ID (UUID)."
                required = true
            }
            query("inntektsdato") {
                description = "Inntektsdato på format yyyy-MM-dd."
                required = true
                example = GenericElement("2024-04-15")
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Inntekt med gjennomsnitt."
                content {
                    schema = jsonSchema<InntektMedGjennomsnittResponse>()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig navReferanseId eller inntektsdato."
                content {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.Unauthorized {
                description = "Mangler tilgang til ressurs."
                content {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.NotFound {
                description = "Forespørsel ikke funnet."
                content {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.InternalServerError {
                description = "Uventet feil."
                content {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
        }
    }
