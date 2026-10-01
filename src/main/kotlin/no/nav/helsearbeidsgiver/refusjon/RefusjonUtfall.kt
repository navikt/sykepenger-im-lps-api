@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.refusjon

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Speiler RefusjonUtfall i hag-refusjon (melding på helsearbeidsgiver.refusjon). Feltnavn må holdes like.
@Serializable
data class RefusjonUtfall(
    val refusjonUtfallId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr,
    val fom: LocalDate,
    val tom: LocalDate,
    val sykepengegrunnlag: Double,
    val utfallTilArbeidsgiver: Utfall,
    val fattetTidspunkt: LocalDateTime,
    val sykmeldtNavn: String? = null,
    val arbeidsgiverNavn: String? = null,
)

@Serializable
enum class Utfall {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
}
