package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.security.mock.oauth2.MockOAuth2Server

val TIGERSYS_ORGNR = Orgnr("315339138")

fun MockOAuth2Server.hentToken(claims: Map<String, Any>): String =
    this
        .issueToken(
            issuerId = "maskinporten",
            audience = "nav:helseytelser/sykepenger",
            claims = claims,
        ).serialize()

fun MockOAuth2Server.ugyldigTokenManglerSystembruker() =
    hentToken(
        claims =
            mapOf(
                "scope" to "nav:helseytelser/sykepenger",
                "consumer" to
                    mapOf(
                        "authority" to "iso6523-actorid-upis",
                        "ID" to "0192:810007842",
                    ),
            ),
    )

fun MockOAuth2Server.gyldigSystembrukerAuthToken(orgnr: String): String =
    hentToken(
        claims =
            mapOf(
                "authorization_details" to
                    listOf(
                        mapOf(
                            "type" to "urn:altinn:systemuser",
                            "systemuser_id" to listOf("a_unique_identifier_for_the_systemuser"),
                            "systemuser_org" to
                                mapOf(
                                    "authority" to "iso6523-actorid-upis",
                                    "ID" to "0192:$orgnr",
                                ),
                            "system_id" to "315339138_tigersys",
                        ),
                    ),
                "scope" to "nav:helseytelser/sykepenger", // TODO sjekk om scope faktisk blir validert av tokensupport
                "consumer" to
                    mapOf(
                        "authority" to "iso6523-actorid-upis",
                        "ID" to "0192:$TIGERSYS_ORGNR",
                    ),
            ),
    )
