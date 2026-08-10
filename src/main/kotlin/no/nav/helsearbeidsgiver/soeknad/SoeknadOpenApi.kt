@file:OptIn(ExperimentalKtorApi::class)

package no.nav.helsearbeidsgiver.soeknad

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

internal fun Route.describeHentSoeknad() =
    describe {
        tag("Sykepengesøknad")
        summary = "Hent sykepengesøknad"
        description = "Hent sykepengesøknad med soeknadId."

        parameters {
            path("soeknadId") {
                description = "Søknad-ID (UUID)."
                required = true
                schema = JsonSchema(type = JsonType.STRING, format = "uuid")
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Sykepengesøknad funnet."
                ContentType.Application.Json {
                    schema = jsonSchema<Sykepengesoeknad>()
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig soeknadId."
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
                description = "Søknader er ikke eksponert for virksomheten."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.NotFound {
                description = "Søknad ikke funnet."
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

internal fun Route.describeHentSoeknadPdf() =
    describe {
        tag("Sykepengesøknad")
        summary = "Hent sykepengesøknad som PDF"
        description = "Hent sykepengesøknad som PDF med soeknadId."

        parameters {
            path("soeknadId") {
                description = "Søknad-ID (UUID)."
                required = true
                schema = JsonSchema(type = JsonType.STRING, format = "uuid")
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "PDF-fil med sykepengesøknad."
                ContentType.Application.Pdf {
                    schema = JsonSchema(type = JsonType.STRING, format = "binary")
                }
            }
            HttpStatusCode.BadRequest {
                description = "Ugyldig soeknadId."
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
                description = "Søknader er ikke eksponert for virksomheten."
                ContentType.Application.Json {
                    schema = jsonSchema<ErrorResponse>()
                }
            }
            HttpStatusCode.NotFound {
                description = "Søknad ikke funnet."
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

internal fun Route.describeFiltrerSoeknader() =
    describe {
        tag("Sykepengesøknad")
        summary = "Hent sykepengesøknader"
        description = "Filtrer søknader på orgnr (underenhet), fnr og/eller dato søknaden ble mottatt av NAV."

        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<SykepengesoeknadFilter>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "Liste med sykepengesøknader."
                headers {
                    header("X-Warning-limit-reached") {
                        description = "Settes dersom resultatet av en spørring overskrider max antall entiteter (1000)"
                        schema = JsonSchema(type = JsonType.INTEGER)
                        example = GenericElement(1000)
                    }
                }
                ContentType.Application.Json {
                    schema = jsonSchema<List<Sykepengesoeknad>>()
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
                description = "Søknader er ikke eksponert for virksomheten."
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
