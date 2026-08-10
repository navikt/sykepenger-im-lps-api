@file:OptIn(ExperimentalKtorApi::class)

package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonType
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.helsearbeidsgiver.plugins.ErrorResponse
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding

internal fun Route.describeHentSykmelding() =
    describe {
        tag("Sykmelding")
        summary = "Hent sykmelding"
        description = "Henter sykmelding med sykmeldingId."

        parameters {
            path("sykmeldingId") {
                description = "Sykmelding-ID (UUID)."
                required = true
                schema = JsonSchema(type = JsonType.STRING, format = "uuid")
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Sykmelding funnet."
                ContentType.Application.Json {
                    schema = jsonSchema<Sykmelding>()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig sykmeldingId."
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
                description = "Sykmeldinger er ikke eksponert for virksomheten."
            }
            HttpStatusCode.NotFound {
                description = "Sykmelding ikke funnet."
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

internal fun Route.describeHentSykmeldingPdf() =
    describe {
        tag("Sykmelding")
        summary = "Hent sykmelding som PDF"
        description = "Henter sykmelding som PDF med sykmeldingId."

        parameters {
            path("sykmeldingId") {
                description = "Sykmelding-ID (UUID)."
                required = true
                schema = JsonSchema(type = JsonType.STRING, format = "uuid")
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "PDF-fil med sykmelding."
                ContentType.Application.Pdf {
                    schema = JsonSchema(type = JsonType.STRING, format = "binary")
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig sykmeldingId."
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
                description = "Sykmeldinger er ikke eksponert for virksomheten."
            }
            HttpStatusCode.NotFound {
                description = "Sykmelding ikke funnet."
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

internal fun Route.describeFiltrerSykmeldinger() =
    describe {
        tag("Sykmelding")
        summary = "Hent sykmeldinger"
        description = "Filtrer sykmeldinger på orgnr (underenhet), fnr og/eller dato sykmeldingen ble mottatt av NAV."

        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<SykmeldingFilter>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Liste med sykmeldinger."
                ContentType.Application.Json {
                    schema = jsonSchema<List<Sykmelding>>()
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
                description = "Sykmeldinger er ikke eksponert for virksomheten."
            }
            HttpStatusCode.InternalServerError {
                description = "Uventet feil."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
        }
    }
