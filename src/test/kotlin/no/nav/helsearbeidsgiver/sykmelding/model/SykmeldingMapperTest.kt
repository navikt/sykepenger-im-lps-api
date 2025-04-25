package no.nav.helsearbeidsgiver.sykmelding.model

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO
import no.nav.helsearbeidsgiver.sykmelding.tilSykmeldingDTO
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingModelMock
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykmeldingMapperTest {
    @Test
    fun `tilSykmelding mapper fra sykmeldingDTO til Sykmelding model med riktig data`() {
        val sykmeldingKafkaMessage = sykmeldingMock()
        val sykmeldingDTO = sykmeldingKafkaMessage.tilSykmeldingDTO()

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
}
