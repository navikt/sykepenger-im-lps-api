@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.dokumentKobling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class DokumentKobling

@Serializable
@SerialName("Sykmelding")
data class Sykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Periode>,
) : DokumentKobling()

@Serializable
@SerialName("Sykepengesoeknad")
data class Sykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : DokumentKobling()

@Serializable
@SerialName("VedtaksperiodeKobling")
data class VedtaksperiodeKobling(
    val vedtaksperiodeId: UUID,
    val soeknadId: UUID,
) : DokumentKobling()

// Skal vi bruke disse to:
@Serializable
@SerialName("ForespoerselSendt")
data class ForespoerselSendt(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr,
) : DokumentKobling()

@Serializable
@SerialName("ForespoerselUtgaatt")
data class ForespoerselUtgaatt(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: String,
) : DokumentKobling()

// ...eller denne?
@Serializable
@SerialName("Forespoersel")
data class Forespoersel(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: String,
    val status: ForespoerselStatus,
) : DokumentKobling()

enum class ForespoerselStatus {
    UTGAATT,
    AKTIV,
}

// Og skal vi bruke denne?
@Serializable
@SerialName("Inntektsmelding")
data class Inntektsmelding(
    val forespoerselId: UUID,
    val innsendingId: UUID,
    val orgnr: String,
    val status: InnsendingStatus,
) : DokumentKobling()

// Eller flate det ut til disse tre?
@Serializable
@SerialName("InntektsmeldingMottatt")
data class InntektsmeldingMottatt(
    val forespoerselId: UUID,
    val innsendingId: UUID,
    val orgnr: String,
) : DokumentKobling()

@Serializable
@SerialName("InntektsmeldingAvvist")
data class InntektsmeldingAvvist(
    val forespoerselId: UUID,
    val innsendingId: UUID,
    val orgnr: String,
) : DokumentKobling()

@Serializable
@SerialName("InntektsmeldingGodkjent")
data class InntektsmeldingGodkjent(
    val forespoerselId: UUID,
    val innsendingId: UUID,
    val orgnr: String,
) : DokumentKobling()
