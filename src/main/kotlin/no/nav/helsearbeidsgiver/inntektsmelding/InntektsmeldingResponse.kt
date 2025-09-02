@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
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

@Serializable
data class InntektsmeldingResponse(
    val id: UUID,
    val navReferanseId: UUID,
    val agp: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
    val sykmeldtFnr: String,
    val aarsakInnsending: AarsakInnsending,
    val typeInnsending: InnsendingType,
    val innsendtTid: LocalDateTime,
    val versjon: Int,
    val arbeidsgiver: Arbeidsgiver,
    val avsender: Avsender,
    val status: InnsendingStatus,
    val valideringsfeil: Valideringsfeil? = null,
)

@Serializable
data class InntektsmeldingRequest(
    // Innsending slik APIet sender inn
    val navReferanseId: UUID,
    val agp: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
    val sykmeldtFnr: String,
    val arbeidsgiverTlf: String,
    val aarsakInnsending: AarsakInnsending,
    val avsender: Avsender, // avsendersystem
) {
    fun valider(): Set<String> = SkjemaInntektsmelding(navReferanseId, arbeidsgiverTlf, agp, inntekt, refusjon).valider()
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
}

@Serializable
data class Arbeidsgiver(
    val orgnr: String, // Arbeidsgivers orgnr
    val tlf: String, // Arbeidsgiver
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
) {
    init {
        require(erGyldig(orgnr))
        fom?.year?.let { require(it >= 0) }
        tom?.year?.let { require(it <= 9999) } // Om man tillater alt opp til LocalDate.MAX
        // vil det bli long-overflow ved konvertering til exposed sql-javadate i db-spørring
    }
}
