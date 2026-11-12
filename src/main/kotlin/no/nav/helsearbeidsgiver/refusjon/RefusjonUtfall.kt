@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.refusjon

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Speiler RefusjonUtfall i hag-refusjon (melding på helsearbeidsgiver.refusjon). Feltnavn må holdes like.
// TODO: Få med SøknadID? Skal vi også lese daggrupper og refusjonskrav? (Venter litt med dette)
// TODO: fjern arbeidstakerVedtakMelding
@Serializable
data class RefusjonUtfall(
    val refusjonUtfallId: UUID,
    val vedtaksperiodeId: UUID,
    val sykmeldingId: UUID,
    val fnr: Fnr,
    val orgnr: Orgnr,
    val fom: LocalDate,
    val tom: LocalDate,
    val arbeidstakervedtakFattetTidspunkt: LocalDateTime,
    val arbeidstakerVedtakMelding: ArbeidstakerVedtakMelding,
    val beloepUtbetaltTilArbeidsgiver: Int,
)
