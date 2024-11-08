package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class InntektsmeldingServiceTest {
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)

    @Test
    fun `opprettInntektsmelding should call inntektsmeldingRepository`() {
        val inntektsmelding =
            jsonMapper.decodeFromString(
                Inntektsmelding.serializer(),
                buildInntektsmeldingJson(),
            )
        every {
            inntektsmeldingRepository.opprett(
                im =
                    jsonMapper.encodeToString(
                        Inntektsmelding.serializer(),
                        inntektsmelding,
                    ),
                org = inntektsmelding.avsender.orgnr.verdi,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                innsendtDato = inntektsmelding.mottatt.toLocalDateTime(),
                forespoerselID = inntektsmelding.type.id.toString(),
            )
        } returns 1

        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify {
            inntektsmeldingRepository.opprett(
                im =
                    jsonMapper.encodeToString(
                        Inntektsmelding.serializer(),
                        inntektsmelding,
                    ),
                org = inntektsmelding.avsender.orgnr.verdi,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                innsendtDato = inntektsmelding.mottatt.toLocalDateTime(),
                forespoerselID = inntektsmelding.type.id.toString(),
            )
        }
    }

    @Test
    fun `hentInntektsmeldingerByOrgNr should call inntektsmeldingRepository`() {
        val orgnr = "123456789"
        val fnr = "12345678901"
        val innsendt = LocalDateTime.now()
        val mottattEvent = LocalDateTime.now()
        val foresporselid = UUID.randomUUID().toString()
        val dokument = "dokument"
        every { inntektsmeldingRepository.hent(orgnr) } returns
            listOf(
                Inntektsmelding(
                    dokument = dokument,
                    orgnr = orgnr,
                    fnr = fnr,
                    foresporselid = foresporselid,
                    innsendt = innsendt,
                    mottattEvent = mottattEvent,
                ),
            )
        val hentInntektsmeldingerByOrgNr = inntektsmeldingService.hentInntektsmeldingerByOrgNr(orgnr)

        verify {
            inntektsmeldingRepository.hent(orgnr)
        }
        assertEquals(1, hentInntektsmeldingerByOrgNr.antallInntektsmeldinger)
        val inntektsmelding = hentInntektsmeldingerByOrgNr.inntektsmeldinger[0]
        assertEquals(orgnr, inntektsmelding.orgnr)
        assertEquals(fnr, inntektsmelding.fnr)
        assertEquals(foresporselid, inntektsmelding.foresporselid)
        assertEquals(innsendt, inntektsmelding.innsendt)
        assertEquals(mottattEvent, inntektsmelding.mottattEvent)
        assertEquals(dokument, inntektsmelding.dokument)
    }

    @Test
    fun `hentInntektsMeldingByRequest må kalle inntektsmeldingRepository`() {
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
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } returns
            listOf(
                Inntektsmelding(
                    dokument = "dokument",
                    orgnr = orgnr,
                    fnr = fnr,
                    foresporselid = foresporselid,
                    innsendt = LocalDateTime.now(),
                    mottattEvent = LocalDateTime.now(),
                ),
            )
        val hentInntektsMeldingByRequest = inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request)

        verify {
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }
        assertEquals(1, hentInntektsMeldingByRequest.antallInntektsmeldinger)
        val inntektsmelding = hentInntektsMeldingByRequest.inntektsmeldinger[0]
        assertEquals(foresporselid, inntektsmelding.foresporselid)
        assertEquals(orgnr, inntektsmelding.orgnr)
        assertEquals(fnr, inntektsmelding.fnr)
    }

    @Test
    fun `hentInntektsmeldingerByOrgNr kaster exception ved error`() {
        val orgnr = "123456789"
        every { inntektsmeldingRepository.hent(orgnr) } throws Exception()

        assertThrows<Exception> { inntektsmeldingService.hentInntektsmeldingerByOrgNr(orgnr) }
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
        assertThrows<Exception> { inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request) }
    }
}
