package no.nav.helsearbeidsgiver.plugins

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val feilmelding: String,
)

object ErrorMessages {
    const val UGYLDIG_FILTERPARAMETER = "Ugyldig filterparameter"
    const val UGYLDIG_IDENTIFIKATOR = "Ugyldig identifikator"
    const val UGYLDIG_REQUEST_BODY = "Ugyldig request"
    const val IKKE_TILGANG_TIL_RESSURS = "Ikke tilgang til ressurs"
    const val EN_FEIL_OPPSTOD = "En feil oppstod"

    const val UGYLDIG_SYKMELDING_ID = "Ugyldig sykmeldingId"
    const val FEIL_VED_HENTING_SYKMELDING = "Feil ved henting av sykmelding"
    const val FEIL_VED_HENTING_SYKMELDINGER = "Feil ved henting av sykmeldinger"

    const val UGYLDIG_NAV_REFERANSE_ID = "Ugyldig navReferanseId"
    const val FEIL_VED_HENTING_FORESPOERSEL = "Feil ved henting av forespørsel"
    const val FEIL_VED_HENTING_FORESPOERSLER = "Feil ved henting av forespørsler"

    const val UGYLDIG_INNSENDING_ID = "Ugyldig innsendingId"
    const val FEIL_INNSENDING_STATUS = "Kan ikke motta ny inntektsmelding, forrige innsending er ikke ferdig behandlet"
    const val FEIL_VED_HENTING_INNTEKTSMELDING = "Feil ved henting av inntektsmelding"
    const val FEIL_VED_HENTING_INNTEKTSMELDINGER = "Feil ved henting av inntektsmeldinger"

    const val UGYLDIG_SOEKNAD_ID = "Ugyldig soeknadId"
    const val FEIL_VED_HENTING_SYKEPENGESOEKNAD = "Feil ved henting av sykepengesøknad"
    const val FEIL_VED_HENTING_SYKEPENGESOEKNADER = "Feil ved henting av sykepengesøknader"
}
