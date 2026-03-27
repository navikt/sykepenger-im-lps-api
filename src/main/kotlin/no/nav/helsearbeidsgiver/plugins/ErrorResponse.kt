package no.nav.helsearbeidsgiver.plugins

import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.util.UUID

@Serializable
data class ErrorResponse(
    val feilkode: String,
    val feilmelding: String,
    @Serializable(UuidSerializer::class)
    val referanseId: UUID? = null,
)

fun ErrorResponse(feil: Feil) =
    ErrorResponse(
        feilkode = feil.name,
        feilmelding = feil.feilmelding,
    )

fun ErrorResponse(
    feil: FeilMedReferanse,
    referanseId: UUID,
) = ErrorResponse(
    feilkode = feil.name,
    feilmelding = feil.feilmelding,
    referanseId = referanseId,
)

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

    UGYLDIG_NAV_REFERANSE_ID("Ugyldig navReferanseId"),
    FEIL_VED_HENTING_FORESPOERSEL("Feil ved henting av forespørsel"),
    FEIL_VED_HENTING_FORESPOERSLER("Feil ved henting av forespørsler"),

    UGYLDIG_INNSENDING("Ugyldig innsending"),
    UGYLDIG_INNSENDING_ID("Ugyldig innsendingId"),
    FEIL_INNSENDING_STATUS("Kan ikke motta ny inntektsmelding, forrige innsending er ikke ferdig behandlet"),
    FEIL_VED_HENTING_INNTEKTSMELDING("Feil ved henting av inntektsmelding"),
    FEIL_VED_HENTING_INNTEKTSMELDINGER("Feil ved henting av inntektsmeldinger"),

    UGYLDIG_SOEKNAD_ID("Ugyldig soeknadId"),
    FEIL_VED_HENTING_SYKEPENGESOEKNAD("Feil ved henting av sykepengesøknad"),
    FEIL_VED_HENTING_SYKEPENGESOEKNADER("Feil ved henting av sykepengesøknader"),
}

enum class FeilMedReferanse(
    val feilmelding: String,
) {
    SYKMELDING_IKKE_FUNNET("Sykmelding med oppgitt sykmeldingId ikke funnet, se vedlagt referanseId"),
    FORESPOERSEL_IKKE_FUNNET("Forespørsel med oppgitt navReferanseId ikke funnet, se vedlagt referanseId"),
    INNSENDING_PAA_GAMMEL_FORESPOERSEL("Innsending mottatt på utgått forespørsel, nyeste navReferanseId er vedlagt i referanseId"),
    INNTEKTSMELDING_IKKE_FUNNET("Inntektsmelding med oppgitt innsendingId ikke funnet, se vedlagt referanseId"),
    DUPLIKAT_INNSENDING("Duplikat innsending, eksisterende innsendingId er vedlagt i referanseId"),
    SOEKNAD_IKKE_FUNNET("Søknad med oppgitt soeknadId ikke funnet, se vedlagt referanseId"),
}
