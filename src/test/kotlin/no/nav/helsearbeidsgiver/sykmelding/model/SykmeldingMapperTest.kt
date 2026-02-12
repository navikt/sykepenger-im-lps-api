package no.nav.helsearbeidsgiver.sykmelding.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO.ArbeidsrelatertArsakDTO
import no.nav.helsearbeidsgiver.sykmelding.ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.AktivitetIkkeMuligAGDTO.ArbeidsrelatertArsakDTO.ArbeidsrelatertArsakTypeDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO
import no.nav.helsearbeidsgiver.sykmelding.tilSykmeldingDTO
import no.nav.helsearbeidsgiver.utils.TestData.aktivitetMock
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingModelMock
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingsperiodeMock
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykmeldingMapperTest {
    @Test
    fun `tilSykmelding mapper fra sykmeldingDTO til Sykmelding model med riktig data`() {
        val sykmeldingKafkaMessage = sykmeldingMock()
        val sykmeldingDTO = sykmeldingKafkaMessage.tilSykmeldingDTO(1L)

        val sykmeldingApiRespons = sykmeldingModelMock()

        sykmeldingDTO.tilSykmelding() shouldBe sykmeldingApiRespons
    }

    @Test
    fun `tilEgenmeldingsdager leser SporsmalOgSvarDTO riktig`() {
        val sykmeldingMock = sykmeldingMock()

        val egenmeldingSvar =
            SykmeldingStatusKafkaEventDTO.SporsmalOgSvarDTO(
                tekst = "abc",
                shortName = SykmeldingStatusKafkaEventDTO.ShortNameDTO.EGENMELDINGSDAGER,
                svartype = SykmeldingStatusKafkaEventDTO.SvartypeDTO.DAGER,
                svar = "[\"2025-03-27\",\"2025-03-29\",\"2025-03-26\"]",
            )
        val sykmelding =
            sykmeldingMock
                .copy(
                    event = sykmeldingMock.event.copy(sporsmals = listOf(egenmeldingSvar)),
                ).tilSykmeldingDTO()
                .tilSykmelding()

        sykmelding.egenmeldingsdager shouldBe
            setOf(
                Periode(
                    fom = LocalDate.of(2025, 3, 26),
                    tom = LocalDate.of(2025, 3, 27),
                ),
                Periode(
                    fom = LocalDate.of(2025, 3, 29),
                    tom = LocalDate.of(2025, 3, 29),
                ),
            )
    }

    @Test
    fun `når aktivitetIkkeMulig i kafka er null er aktivitetIkkeMulig i api model null`() {
        val bareGradert =
            sykmeldingsperiodeMock.copy(
                aktivitetIkkeMulig = null,
                gradert = ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.GradertDTO(50, false),
                type = ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.PeriodetypeDTO.GRADERT,
            )

        bareGradert.tilAktivitet() shouldBe aktivitetMock.copy(aktivitetIkkeMulig = null, gradertSykmelding = GradertSykmelding(50, false))
    }

    @Test
    fun `når arbeidsrelatertArsak i kafka er null blir manglendeTilrettelegging satt til false og beskrivelse til null`() {
        val arbeidsrelatertArsakErNull =
            sykmeldingsperiodeMock.copy(
                aktivitetIkkeMulig = AktivitetIkkeMuligAGDTO(arbeidsrelatertArsak = null),
                type = ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
            )

        arbeidsrelatertArsakErNull.tilAktivitet() shouldBe
            aktivitetMock.copy(
                aktivitetIkkeMulig =
                    AktivitetIkkeMulig(
                        manglendeTilretteleggingPaaArbeidsplassen = false,
                        beskrivelse = null,
                    ),
            )
    }

    @Test
    fun `når arbeidsrelatertArsak i kafka er definert blir manglendeTilrettelegging og beskrivelse satt`() {
        val beskrivelse = "Veldig god beskrivelse her"
        val arbeidsrelatertArsakErDefinert =
            sykmeldingsperiodeMock.copy(
                aktivitetIkkeMulig =
                    AktivitetIkkeMuligAGDTO(
                        ArbeidsrelatertArsakDTO(
                            arsak = listOf(ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING),
                            beskrivelse = beskrivelse,
                        ),
                    ),
                type = ArbeidsgiverSykmeldingKafka.SykmeldingsperiodeAGDTO.PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
            )

        arbeidsrelatertArsakErDefinert.tilAktivitet() shouldBe
            aktivitetMock.copy(
                aktivitetIkkeMulig =
                    AktivitetIkkeMulig(
                        manglendeTilretteleggingPaaArbeidsplassen = true,
                        beskrivelse = beskrivelse,
                    ),
            )
    }
}
