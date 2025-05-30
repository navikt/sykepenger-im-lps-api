@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class DialogMelding

@Serializable
@SerialName("Sykmelding")
data class DialogSykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Periode>,
) : DialogMelding()

@Serializable
@SerialName("Sykepengesoknad")
data class DialogSykepengesoknad(
    val soknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : DialogMelding()
