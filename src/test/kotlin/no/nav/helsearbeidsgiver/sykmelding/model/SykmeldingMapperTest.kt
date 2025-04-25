package no.nav.helsearbeidsgiver.sykmelding.model

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO
import no.nav.helsearbeidsgiver.sykmelding.tilMockSykmeldingModel
import no.nav.helsearbeidsgiver.sykmelding.tilSykmeldingDTO
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingModelMock
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykmeldingMapperTest {
    @Test
    fun `SykmeldingMapper mapper fra kafkaMelding til Sykmelding model med riktig data`() {
        val sykmeldingKafkaMessage = sykmeldingMock()

        sykmeldingKafkaMessage.tilSykmeldingDTO().tilMockSykmeldingModel() shouldBe sykmeldingModelMock()
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

        val sykmeldingArbeidsgiver =
            SykmeldingDTO(
                id = "",
                fnr = "05117920005",
                orgnr = "220460274",
                sendSykmeldingAivenKafkaMessage =
                    sykmeldingMock.copy(
                        event = sykmeldingMock.event.copy(sporsmals = listOf(egenmeldingSvar)),
                    ),
                sykmeldtNavn = "",
            ).tilMockSykmeldingModel()

        sykmeldingArbeidsgiver.egenmeldingsdager shouldBe
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
}
