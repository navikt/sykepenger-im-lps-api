@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.soeknad

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Datamodell for Sykepengesøknad.
 * Inneholder bare felter som skal vises til arbeidsgiver.
 * Er basert på modellen i flex sin applikasjon for å sende søknader til arbeidsgiver via altinn
 * https://github.com/navikt/sykepengesoknad-altinn/tree/main/src/main/kotlin/no/nav/syfo/domain/soknad
 */

@Serializable
data class Sykepengesoeknad(
    val loepenr: Long,
    val soeknadId: UUID,
    val fnr: String,
    val sykmeldingId: UUID?,
    val type: Soeknadstype,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val arbeidGjenopptattDato: LocalDate?,
    val mottatTid: LocalDateTime,
    val arbeidsgiver: SykepengesoeknadArbeidsgiver,
    val soektUtenlandsopphold: Boolean?,
    val korrigerer: UUID?,
    val soeknadsperioder: List<Soeknadsperiode>,
    // val behandlingsdager: List<LocalDate>,
    val fravaer: List<Fravaer>,
) {
    @Serializable
    enum class Soeknadstype {
        // Selvstending og utland skal ikke til arbeidsgiver
        // SELVSTENDIGE_OG_FRILANSERE
        // OPPHOLD_UTLAND,
        ARBEIDSTAKERE,
        BEHANDLINGSDAGER,
        GRADERT_REISETILSKUDD,
    }

    @Serializable
    data class SykepengesoeknadArbeidsgiver(
        val navn: String,
        val orgnr: String,
    )

    @Serializable
    data class Soeknadsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val sykmeldingsgrad: Int,
        val faktiskGrad: Int? = null,
        val avtaltTimer: Double? = null,
        val faktiskTimer: Double? = null,
        val sykmeldingstype: Sykmeldingstype? = null, // TODO: Vi kan antageligvis fjerne denne
    )

    @Serializable
    enum class Sykmeldingstype {
        AKTIVITET_IKKE_MULIG,
        GRADERT,
        BEHANDLINGSDAGER,
        AVVENTENDE,
        REISETILSKUDD,
    }

    @Serializable
    data class Fravaer(
        val fom: LocalDate,
        val tom: LocalDate?,
        val type: Fravaerstype,
    )

    @Serializable
    enum class Fravaerstype {
        FERIE,
        PERMISJON,
        UTLANDSOPPHOLD, // Skal vi fjerne denne?
    }
}
