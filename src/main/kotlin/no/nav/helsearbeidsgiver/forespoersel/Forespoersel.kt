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
    val forespoersel_id: String,
    val orgnr: String,
    val fnr: String,
    val status: Status,
    val dokument: ForespoerselDokument,
)

@Serializable
enum class Status {
    AKTIV,
    MOTTATT,
    FORKASTET,
    }

/*
TODO: skal ikke eksponeres mot LPS, men brukes for å tolke innkommende forespørsler fra BRO.
Data fra denne skal legges over i egne felter på eksponert Forespørsel
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
)

enum class Type {
    KOMPLETT,
    BEGRENSET,
}

@Serializable
data class ForespoerselRequest(
    val fnr: String? = null,
    val forespoersel_id: String? = null,
    val status: Status? = null,
)

@Serializable
data class ForespoerselResponse(
    val antall: Int,
    val forespoersler: List<Forespoersel>,
)
