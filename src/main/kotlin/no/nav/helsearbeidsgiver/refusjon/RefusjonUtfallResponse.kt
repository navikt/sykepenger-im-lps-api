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

@Serializable
data class RefusjonUtfallResponse(
    val loepenr: Long,
    val refusjonUtfallId: UUID,
    val vedtaksperiodeId: UUID,
    val fnr: Fnr,
    val orgnr: Orgnr,
    val fom: LocalDate,
    val tom: LocalDate,
    val sykepengegrunnlag: Double,
    val utfallTilArbeidsgiver: Utfall,
    val fattetTidspunkt: LocalDateTime,
)

fun RefusjonUtfall.tilRefusjonUtfallResponse(loepenr: Long): RefusjonUtfallResponse =
    RefusjonUtfallResponse(
        loepenr = loepenr,
        refusjonUtfallId = refusjonUtfallId,
        vedtaksperiodeId = vedtaksperiodeId,
        fnr = fnr,
        orgnr = orgnr,
        fom = fom,
        tom = tom,
        sykepengegrunnlag = sykepengegrunnlag,
        utfallTilArbeidsgiver = utfallTilArbeidsgiver,
        fattetTidspunkt = fattetTidspunkt,
    )
