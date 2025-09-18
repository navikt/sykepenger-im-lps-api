@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.soeknad

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr.Companion.erGyldig
import java.time.LocalDate

@Serializable
data class SykepengesoeknadFilter( // TODO: Kanskje denne kan de-dupliseres / generaliseres til å gjelde flere typer?
    val orgnr: String,
    val fnr: String? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val fraLoepenr: ULong? = null,
) {
    init {
        require(erGyldig(orgnr))
        fom?.year?.let { require(it >= 0) }
        tom?.year?.let { require(it <= 9999) } // Om man tillater alt opp til LocalDate.MAX
        // vil det bli long-overflow ved konvertering til exposed sql-javadate i db-spørring
    }
}
