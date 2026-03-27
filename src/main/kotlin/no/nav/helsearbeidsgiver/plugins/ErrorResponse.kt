package no.nav.helsearbeidsgiver.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val feil: Feil,
    val feilmelding: String,
    @SerialName("referanse_id")
    val referanseId: String? = null,
)

fun ErrorResponse(
    feil: Feil,
    referanseId: String? = null,
) = ErrorResponse(
    feil = feil,
    feilmelding = feil.feilmelding,
    referanseId = referanseId,
)

@Serializable
enum class Feil(
    val feilmelding: String,
) {
    UGYLDIG_FILTERPARAMETER("Ugyldig filterparameter"),
    UGYLDIG_IDENTIFIKATOR("Ugyldig identifikator"),
    UGYLDIG_REQUEST_BODY("Ugyldig request"),
    IKKE_TILGANG_TIL_RESSURS("Ikke tilgang til ressurs"),
    EN_FEIL_OPPSTOD("En feil oppstod"),
    UAUTORISERT("Uautorisert tilgang"),
    MANGLER_BRUKERIDENTIFIKASJON("Mangler brukeridentifikasjon i token"),

    FEIL_VED_PDF_GENERERING("Feil ved generering av pdf"),

    UGYLDIG_SYKMELDING_ID("Ugyldig sykmeldingId"),
    FEIL_VED_HENTING_SYKMELDING("Feil ved henting av sykmelding"),
    FEIL_VED_HENTING_SYKMELDINGER("Feil ved henting av sykmeldinger"),
    SYKMELDING_IKKE_FUNNET("Sykmelding med oppgitt sykmeldingId ikke funnet, se vedlagt referanse_id"),

    UGYLDIG_NAV_REFERANSE_ID("Ugyldig navReferanseId"),
    FEIL_VED_HENTING_FORESPOERSEL("Feil ved henting av forespørsel"),
    FEIL_VED_HENTING_FORESPOERSLER("Feil ved henting av forespørsler"),
    FORESPOERSEL_IKKE_FUNNET("Forespørsel med oppgitt navReferanseId ikke funnet, se vedlagt referanse_id"),
    INNSENDING_PAA_GAMMEL_FORESPOERSEL("Innsending mottatt på utgått forespørsel, nyeste navReferanseId er vedlagt i referanse_id"),

    UGYLDIG_INNSENDING("Ugyldig innsending"),
    UGYLDIG_INNSENDING_ID("Ugyldig innsendingId"),
    FEIL_INNSENDING_STATUS("Kan ikke motta ny inntektsmelding, forrige innsending er ikke ferdig behandlet"),
    FEIL_VED_HENTING_INNTEKTSMELDING("Feil ved henting av inntektsmelding"),
    FEIL_VED_HENTING_INNTEKTSMELDINGER("Feil ved henting av inntektsmeldinger"),
    INNTEKTSMELDING_IKKE_FUNNET("Inntektsmelding med oppgitt innsendingId ikke funnet, se vedlagt referanse_id"),
    DUPLIKAT_INNSENDING("Duplikat innsending, eksisterende innsendingId er vedlagt i referanse_id"),

    UGYLDIG_SOEKNAD_ID("Ugyldig soeknadId"),
    FEIL_VED_HENTING_SYKEPENGESOEKNAD("Feil ved henting av sykepengesøknad"),
    FEIL_VED_HENTING_SYKEPENGESOEKNADER("Feil ved henting av sykepengesøknader"),
    SOEKNAD_IKKE_FUNNET("Søknad med oppgitt soeknadId ikke funnet, se vedlagt referanse_id"),
}
