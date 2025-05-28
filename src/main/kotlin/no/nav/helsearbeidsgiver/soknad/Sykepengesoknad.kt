@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.soknad

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
data class Sykepengesoknad(
    val id: UUID,
    val fnr: String,
    val sykmeldingId: UUID?,
    val type: Soknadstype,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val arbeidGjenopptattDato: LocalDate?,
    val mottatTid: LocalDateTime,
    val arbeidsgiver: Arbeidsgiver,
    val soktUtenlandsopphold: Boolean?,
    val korrigerer: UUID?,
    val soknadsperioder: List<Soknadsperiode>,
    // val behandlingsdager: List<LocalDate>,
    val fravar: List<Fravar>,
) {
    @Serializable
    enum class Soknadstype {
        SELVSTENDIGE_OG_FRILANSERE,
        OPPHOLD_UTLAND,
        ARBEIDSTAKERE,
        BEHANDLINGSDAGER,
        GRADERT_REISETILSKUDD,
    }

    @Serializable
    data class Arbeidsgiver(
        val navn: String,
        val orgnr: String,
    )

    @Serializable
    data class Soknadsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val sykmeldingsgrad: Int,
        val faktiskGrad: Int? = null,
        val avtaltTimer: Double? = null,
        val faktiskTimer: Double? = null,
        val sykmeldingstype: Sykmeldingstype? = null,
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
    data class Fravar(
        val fom: LocalDate,
        val tom: LocalDate?,
        val type: Fravarstype,
    )

    @Serializable
    enum class Fravarstype {
        FERIE,
        PERMISJON,
        UTLANDSOPPHOLD, // Skal vi fjerne denne?
        UTDANNING_FULLTID,
        UTDANNING_DELTID,
        UKJENT, // Skal vi fjerne denne?
    }
}
