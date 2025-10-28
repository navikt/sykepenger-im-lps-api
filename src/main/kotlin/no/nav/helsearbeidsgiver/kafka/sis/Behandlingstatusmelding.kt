@file:UseSerializers(OffsetDateTimeSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka.sis

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.OffsetDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.OffsetDateTime
import java.util.UUID

@Serializable
data class Behandlingstatusmelding(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val tidspunkt: OffsetDateTime,
    val status: Behandlingstatustype,
    val eksterneSøknadIder: Set<UUID>? = null,
) {
    val versjon = "2.0.2" // TODO: Trengs denne?

    @Serializable
    enum class Behandlingstatustype {
        OPPRETTET,
        VENTER_PÅ_ARBEIDSGIVER,
        VENTER_PÅ_ANNEN_PERIODE,
        VENTER_PÅ_SAKSBEHANDLER,
        FERDIG,
        BEHANDLES_UTENFOR_SPEIL,
    }
}
