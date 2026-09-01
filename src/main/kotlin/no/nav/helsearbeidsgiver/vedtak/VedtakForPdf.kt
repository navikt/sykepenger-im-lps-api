@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.vedtak

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.kafka.sis.VedtaksUtfall
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Forenklet utvalg av felter fra VedtakArbeidsgiverMelding, for bruk i PDF-generering i første iterasjon.
@Serializable
data class VedtakForPdf(
    val vedtakId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val sykepengegrunnlag: Double,
    val vedtaksUtfallTilArbeidsgiver: VedtaksUtfall,
    val vedtakFattetTidspunkt: LocalDateTime,
)
