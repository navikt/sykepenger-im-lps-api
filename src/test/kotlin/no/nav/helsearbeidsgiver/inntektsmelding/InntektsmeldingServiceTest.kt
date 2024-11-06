package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class InntektsmeldingServiceTest {
    val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)

    @Test
    fun `opprettInntektsmelding should call inntektsmeldingRepository`() {
        val inntektsmeldingJson = buildInntektsmeldingJson()
        val inntektsmelding =
            jsonMapper.decodeFromString(
                Inntektsmelding.serializer(),
                inntektsmeldingJson,
            )
        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify {
            inntektsmeldingRepository.opprett(
                im = any(),
                org = inntektsmelding.avsender.orgnr.verdi,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                innsendtDato = any(),
                forespoerselID = inntektsmelding.type.id.toString(),
            )
        }
    }

    @Test
    fun `hentInntektsmeldingerByOrgNr should call inntektsmeldingRepository`() {
        val orgnr = "123456789"
        inntektsmeldingService.hentInntektsmeldingerByOrgNr(orgnr)

        verify {
            inntektsmeldingRepository.hent(orgnr)
        }
    }

    @Test
    fun `hentInntektsMeldingByRequest should call inntektsmeldingRepository`() {
        val foresporselid = "123456789"
        val orgnr = "987654322"
        val fnr = "12345678901"
        val datoFra = LocalDateTime.now()
        val datoTil = datoFra.plusDays(1)
        val request =
            InntektsmeldingRequest(
                fnr = fnr,
                foresporselid = foresporselid,
                datoFra = datoFra,
                datoTil = datoTil,
            )
        inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request)

        verify {
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }
    }

    @Test
    fun `hentInntektsmeldingerByOrgNr should return empty list on failure`() {
        val orgnr = "123456789"
        every { inntektsmeldingRepository.hent(orgnr) } throws Exception()
        val hentInntektsmeldingerByOrgNr = inntektsmeldingService.hentInntektsmeldingerByOrgNr(orgnr)
        assertEquals(emptyList(), hentInntektsmeldingerByOrgNr)
    }

    @Test
    fun `hentInntektsMeldingByRequest should return empty list on failure`() {
        val orgnr = "987654322"
        val fnr = "12345678901"
        val foresporselid = "123456789"
        val datoFra = LocalDateTime.now()
        val datoTil = datoFra.plusDays(1)
        val request =
            InntektsmeldingRequest(
                fnr = fnr,
                foresporselid = foresporselid,
                datoFra = datoFra,
                datoTil = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } throws Exception()
        val hentInntektsMeldingByRequest = inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request)
        assertEquals(InntektsmeldingResponse(0, emptyList()), hentInntektsMeldingByRequest)
    }
}
