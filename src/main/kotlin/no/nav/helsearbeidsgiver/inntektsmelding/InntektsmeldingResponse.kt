@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Kanal
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Naturalytelse
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr.Companion.erGyldig
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val gyldigeFritekstTegn = "^[.A-Za-zæøøåÆØÅ0-9 _-]{2,64}\$"

@Serializable
data class InntektsmeldingResponse(
    val loepenr: Long,
    val id: UUID,
    val navReferanseId: UUID,
    val agp: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
    val naturalytelser: List<Naturalytelse>,
    val sykmeldtFnr: String,
    val aarsakInnsending: AarsakInnsending,
    val typeInnsending: InnsendingType,
    val innsendtTid: LocalDateTime,
    val versjon: Int,
    val arbeidsgiver: InntektsmeldingArbeidsgiver,
    val avsender: Avsender,
    val status: InnsendingStatus,
    val valideringsfeil: Valideringsfeil? = null,
)

// Innsending slik APIet sender inn
@Serializable
data class InntektsmeldingRequest(
    val navReferanseId: UUID,
    val agp: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
    val naturalytelser: List<Naturalytelse>,
    val sykmeldtFnr: String,
    val arbeidsgiverTlf: String,
    val aarsakInnsending: AarsakInnsending,
    val avsender: Avsender, // avsendersystem
    val kontaktinformasjon: String = "Kontaktinformasjon mangler", // nytt felt
) {
    fun valider(): Set<String> =
        SkjemaInntektsmelding(navReferanseId, arbeidsgiverTlf, agp, inntekt, naturalytelser, refusjon)
            .valider() + validerAvsender() + validerKontaktInformasjon()

    private fun validerAvsender(): Set<String> =
        runCatching { avsender.valider() }
            .exceptionOrNull()
            ?.let { setOf(it.message ?: "Ugyldig avsender") }
            ?: emptySet()

    private fun validerKontaktInformasjon(): Set<String> =
        if (!Regex(gyldigeFritekstTegn).matches(kontaktinformasjon.trim())) {
            setOf("Ugyldig kontaktinformasjon - tillatte tegn er $gyldigeFritekstTegn")
        } else {
            emptySet()
        }
}

enum class InnsendingType {
    FORESPURT,
    ARBEIDSGIVER_INITIERT,
    FORESPURT_EKSTERN,
    ;

    companion object {
        fun from(type: Inntektsmelding.Type): InnsendingType =
            when (type) {
                is Inntektsmelding.Type.Forespurt -> FORESPURT
                is Inntektsmelding.Type.Selvbestemt,
                is Inntektsmelding.Type.Fisker,
                is Inntektsmelding.Type.UtenArbeidsforhold,
                is Inntektsmelding.Type.Behandlingsdager,
                -> ARBEIDSGIVER_INITIERT

                is Inntektsmelding.Type.ForespurtEkstern -> FORESPURT_EKSTERN
            }
    }

    fun toKanal(): Kanal =
        when (this) {
            FORESPURT -> Kanal.NAV_NO
            ARBEIDSGIVER_INITIERT -> Kanal.NAV_NO
            FORESPURT_EKSTERN -> Kanal.HR_SYSTEM_API
        }
}

@Serializable
data class InntektsmeldingArbeidsgiver(
    val orgnr: String, // Arbeidsgivers orgnr
    val tlf: String, // Arbeidsgiver
    val kontaktinformasjon: String = "Kontaktinformasjon mangler", // må ha default-verdi
)

@Serializable
data class Avsender(
    val systemNavn: String,
    val systemVersjon: String,
)

@Serializable
data class InntektsmeldingFilter(
    val orgnr: String,
    val innsendingId: UUID? = null,
    val fnr: String? = null,
    val navReferanseId: UUID? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val status: InnsendingStatus? = null,
    val fraLoepenr: Long? = null,
) {
    init {
        require(erGyldig(orgnr))
        fom?.year?.let { require(it >= 0) }
        tom?.year?.let { require(it <= 9999) } // Om man tillater alt opp til LocalDate.MAX
        // vil det bli long-overflow ved konvertering til exposed sql-javadate i db-spørring
        fraLoepenr?.let { require(it >= 0) }
    }
}

fun Avsender.valider() {
    require(this.systemNavn.trim().matches(Regex(gyldigeFritekstTegn))) { "Ugyldig systemNavn, tillatte tegn er $gyldigeFritekstTegn" }
    require(
        this.systemVersjon.trim().matches(Regex(gyldigeFritekstTegn)),
    ) { "Ugyldig systemVersjon, tillatte tegn er $gyldigeFritekstTegn" }
}
