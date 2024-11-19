@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.forespoersel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.util.UUID

@Serializable
data class Forespoersel(
    val forespoerselId: String,
    val orgnr: String,
    val fnr: String,
    val status: Status,
    val dokument: ForespoerselDokument,
)

enum class Status {
    AKTIV,
    MOTTATT,
    FORKASTET,
}

@Serializable
data class ForespoerselDokument(
    val type: Type,
    val orgnr: String,
    val fnr: String,
    val vedtaksperiodeId: UUID,
    val forespoerselId: UUID,
    val sykmeldingsperioder: List<Periode>,
    val egenmeldingsperioder: List<Periode>,
)

enum class Type {
    KOMPLETT,
    BEGRENSET,
}
