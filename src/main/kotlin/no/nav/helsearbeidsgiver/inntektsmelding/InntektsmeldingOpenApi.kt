@file:OptIn(ExperimentalKtorApi::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.GenericElement
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonType
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.plugins.ErrorResponse

// redusertLoennIAgp er obligatorisk, men tillater null. Ktor utelater nullable felter fra required.
// Siden modellen ligger i hag-domene, korrigerer vi OpenAPI-skjemaet her uten å endre domenemodellen.
private fun JsonSchema.medPaakrevdRedusertLoennIAgp(): JsonSchema =
    copy(
        properties =
            properties?.mapValues { (navn, skjema) ->
                if (navn == InntektsmeldingRequest::agp.name) {
                    skjema.mapValue {
                        it.copy(
                            required = (it.required.orEmpty() + Arbeidsgiverperiode::redusertLoennIAgp.name).distinct(),
                        )
                    }
                } else {
                    skjema
                }
            },
        items = items?.mapValue { it.medPaakrevdRedusertLoennIAgp() },
    )

internal fun Route.describeSendInntektsmelding() =
    describe {
        tag("Inntektsmelding")
        summary = "Send inn inntektsmelding"
        description = "Send inn inntektsmelding."

        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<InntektsmeldingRequest>().medPaakrevdRedusertLoennIAgp()
            }
        }

        responses {
            HttpStatusCode.Created {
                description = "Inntektsmelding mottatt."
                ContentType.Application.Json {
                    schema = jsonSchema<InnsendingResponse>()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig inntektsmelding."
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
                description = "Inntektsmeldinger er ikke eksponert for virksomheten."
            }
            HttpStatusCode.Conflict {
                description = "Duplikat innsending."
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

internal fun Route.describeFiltrerInntektsmeldinger() =
    describe {
        tag("Inntektsmelding")
        summary = "Hent inntektsmeldinger"
        description =
            "Filtrer inntektsmeldinger på orgnr (underenhet), fnr, innsendingId, navReferanseId, status og/eller dato inntektsmeldingen ble mottatt av NAV."

        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<InntektsmeldingFilter>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Liste med inntektsmeldinger."
                headers {
                    header("X-Warning-limit-reached") {
                        description = "Settes dersom resultatet av en spørring overskrider max antall entiteter (1000)"
                        schema = JsonSchema(type = JsonType.INTEGER)
                        example = GenericElement(1000)
                    }
                }
                ContentType.Application.Json {
                    schema = jsonSchema<List<InntektsmeldingResponse>>().medPaakrevdRedusertLoennIAgp()
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
                description = "Inntektsmeldinger er ikke eksponert for virksomheten."
            }
            HttpStatusCode.InternalServerError {
                description = "Uventet feil."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
        }
    }

internal fun Route.describeHentInntektsmelding() =
    describe {
        tag("Inntektsmelding")
        summary = "Hent inntektsmelding"
        description = "Hent inntektsmelding med id."

        parameters {
            path("innsendingId") {
                description = "Innsending-ID (UUID)."
                required = true
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Inntektsmelding funnet."
                ContentType.Application.Json {
                    schema = jsonSchema<InntektsmeldingResponse>().medPaakrevdRedusertLoennIAgp()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig innsendingId."
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
                description = "Inntektsmeldinger er ikke eksponert for virksomheten."
            }
            HttpStatusCode.NotFound {
                description = "Inntektsmelding ikke funnet."
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
