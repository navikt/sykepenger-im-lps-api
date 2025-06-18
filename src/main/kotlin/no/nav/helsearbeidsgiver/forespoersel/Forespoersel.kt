@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class, YearMonthSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.forespoersel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Forespoersel(
    val navReferanseId: UUID,
    val orgnr: String,
    val fnr: String,
    val status: Status,
    val sykmeldingsperioder: List<Periode>,
    val egenmeldingsperioder: List<Periode>,
    val arbeidsgiverperiodePaakrevd: Boolean,
    val inntektPaakrevd: Boolean,
    val opprettetTid: LocalDateTime,
)

@Serializable
enum class Status {
    AKTIV,
    BESVART,
    FORKASTET,
}

enum class Type {
    KOMPLETT,
    BEGRENSET,
}

@Serializable
data class ForespoerselRequest(
    val fnr: String? = null,
    val navReferanseId: UUID? = null,
    val status: Status? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
) {
    init {
        fom?.year?.let { require(it >= 0) }
        tom?.year?.let { require(it <= 9999) } // Om man tillater alt opp til LocalDate.MAX
        // vil det bli long-overflow ved konvertering til exposed sql-javadate i db-spørring
    }
}

@Serializable
data class ForespoerselResponse(
    val antall: Int,
    val forespoersler: List<Forespoersel>,
)

/*
Kopierte domeneobjekter fra BRO. Skal ikke eksponeres mot LPS, brukes for å tolke innkommende forespørsler fra BRO.
 */
@Serializable
data class ForespoerselDokument(
    val type: Type,
    val orgnr: String,
    val fnr: String,
    val vedtaksperiodeId: UUID,
    val forespoerselId: UUID,
    val sykmeldingsperioder: List<Periode>,
    val egenmeldingsperioder: List<Periode>,
    val forespurtData: ForespurtData,
)

@Serializable
data class ForespurtData(
    val arbeidsgiverperiode: Arbeidsgiverperiode,
    val inntekt: Inntekt,
)

@Serializable
data class Arbeidsgiverperiode(
    val paakrevd: Boolean,
)

@Serializable
data class Inntekt(
    val paakrevd: Boolean,
)
