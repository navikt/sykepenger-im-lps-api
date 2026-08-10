@file:OptIn(ExperimentalKtorApi::class)

package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.GenericElement
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonType
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.helsearbeidsgiver.plugins.ErrorResponse

internal fun Route.describeHentForespoersel() =
    describe {
        tag("Forespørsel om inntektsmelding")
        summary = "Hent forespørsel"
        description = "Hent forespørsel med navReferanseId."

        parameters {
            path("navReferanseId") {
                description = "NAV referanse-ID (UUID)."
                required = true
                schema = JsonSchema(type = JsonType.STRING, format = "uuid")
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Forespørsel funnet."
                ContentType.Application.Json {
                    schema = jsonSchema<ForespoerselResponse>()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig navReferanseId."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.Unauthorized {
                description = "Mangler tilgang til ressurs."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.Forbidden {
                description = "Forespørsler er ikke eksponert for virksomheten."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.NotFound {
                description = "Forespørsel ikke funnet."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.InternalServerError {
                description = "Uventet feil."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
        }
    }

internal fun Route.describeFiltrerForespoersler() =
    describe {
        tag("Forespørsel om inntektsmelding")
        summary = "Hent forespørsler"
        description =
            "Filtrer forespørsler om inntektsmelding på orgnr (underenhet), fnr, navReferanseId, status og/eller dato forespørselen ble opprettet av NAV."

        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<ForespoerselFilter>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Liste med forespørsler."
                headers {
                    header("X-Warning-limit-reached") {
                        description = "Settes dersom resultatet av en spørring overskrider max antall entiteter (1000)"
                        schema = JsonSchema(type = JsonType.INTEGER)
                        example = GenericElement(1000)
                    }
                }
                ContentType.Application.Json {
                    schema = jsonSchema<List<ForespoerselResponse>>()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig forespørsel."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.Unauthorized {
                description = "Mangler tilgang til ressurs."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.Forbidden {
                description = "Forespørsler er ikke eksponert for virksomheten."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.InternalServerError {
                description = "Uventet feil."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
        }
    }
