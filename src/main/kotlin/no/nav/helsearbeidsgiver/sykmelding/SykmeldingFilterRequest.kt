@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class SykmeldingFilterRequest(
    val fnr: String? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
) {
    init {
        fom?.year?.let { require(it >= 0) }
        tom?.year?.let { require(it <= 9999) } // Om man tillater alt opp til LocalDate.MAX
        // vil det bli long-overflow ved konvertering til exposed sql-javadate i db-spørring
    }
}
