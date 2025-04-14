package no.nav.helsearbeidsgiver.sykmelding.model

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingDTO
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO
import no.nav.helsearbeidsgiver.sykmelding.mockHentPersonFraPDL
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykmeldingArbeidsgiverMapperTest {
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
                fnr = "",
                orgnr = "",
                sendSykmeldingAivenKafkaMessage =
                    sykmeldingMock.copy(
                        event = sykmeldingMock.event.copy(sporsmals = listOf(egenmeldingSvar)),
                    ),
            ).tilSykmeldingArbeidsgiver(mockHentPersonFraPDL(""))

        sykmeldingArbeidsgiver.egenmeldingsdager shouldBe
            setOf(
                LocalDate.of(2025, 3, 27),
                LocalDate.of(2025, 3, 29),
                LocalDate.of(2025, 3, 26),
            )
    }
}
