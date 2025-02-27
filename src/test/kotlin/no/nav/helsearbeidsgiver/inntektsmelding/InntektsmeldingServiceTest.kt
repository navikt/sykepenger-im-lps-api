package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
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
                im = inntektsmelding,
                org = inntektsmelding.avsender.orgnr.verdi,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                innsendtDato = inntektsmelding.mottatt.toLocalDateTime(),
                forespoerselID = inntektsmelding.type.id.toString(),
            )
        } returns 1

        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify {
            inntektsmeldingRepository.opprett(
                im = inntektsmelding,
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
        val dokument = buildInntektsmelding()
        every { inntektsmeldingRepository.hent(orgnr) } returns
            listOf(
                Inntektsmelding(
                    dokument = dokument,
                    orgnr = orgnr,
                    fnr = fnr,
                    foresporsel_id = foresporselid,
                    innsendt_tid = innsendt,
                    mottatt_tid = mottattEvent,
                ),
            )
        val hentInntektsmeldingerByOrgNr = inntektsmeldingService.hentInntektsmeldingerByOrgNr(orgnr)

        verify {
            inntektsmeldingRepository.hent(orgnr)
        }
        assertEquals(1, hentInntektsmeldingerByOrgNr.antall)
        val inntektsmelding = hentInntektsmeldingerByOrgNr.inntektsmeldinger[0]
        assertEquals(orgnr, inntektsmelding.orgnr)
        assertEquals(fnr, inntektsmelding.fnr)
        assertEquals(foresporselid, inntektsmelding.foresporsel_id)
        assertEquals(innsendt, inntektsmelding.innsendt_tid)
        assertEquals(mottattEvent, inntektsmelding.mottatt_tid)
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
                foresporsel_id = foresporselid,
                fra_dato = datoFra,
                til_dato = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } returns
            listOf(
                Inntektsmelding(
                    dokument = buildInntektsmelding(),
                    orgnr = orgnr,
                    fnr = fnr,
                    foresporsel_id = foresporselid,
                    innsendt_tid = LocalDateTime.now(),
                    mottatt_tid = LocalDateTime.now(),
                ),
            )
        val hentInntektsMeldingByRequest = inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request)

        verify {
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }
        assertEquals(1, hentInntektsMeldingByRequest.antall)
        val inntektsmelding = hentInntektsMeldingByRequest.inntektsmeldinger[0]
        assertEquals(foresporselid, inntektsmelding.foresporsel_id)
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
                foresporsel_id = foresporselid,
                fra_dato = datoFra,
                til_dato = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } throws Exception()
        assertThrows<Exception> { inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request) }
    }
}
