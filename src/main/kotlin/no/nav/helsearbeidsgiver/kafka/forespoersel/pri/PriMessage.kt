@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.kafka.forespoersel.pri

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.forespoersel.ForespurtData
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
data class PriMessage(
    val notis: NotisType,
    val forespoersel: ForespoerselDokument? = null,
    val forespoerselId: UUID? = null,
    val eksponertForespoerselId: UUID? = null,
    val status: Status? = null,
)

/*
Kopierte domeneobjekter fra BRO. Skal ikke eksponeres mot LPS, brukes for å tolke innkommende forespørsler fra BRO.
 */
@Serializable
data class ForespoerselDokument(
    val orgnr: String,
    val fnr: String,
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val egenmeldingsperioder: List<Periode>,
    val sykmeldingsperioder: List<Periode>,
    val bestemmendeFravaersdager: Map<Orgnr, LocalDate> = emptyMap(),
    val forespurtData: ForespurtData,
)
