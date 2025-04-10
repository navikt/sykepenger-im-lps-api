package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.tilSkjema
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
            inntektsmeldingRepository.opprettInntektsmelding(
                im = inntektsmelding,
            )
        } returns inntektsmelding.id

        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify {
            inntektsmeldingRepository.opprettInntektsmelding(
                im = inntektsmelding,
                innsendingStatus = InnsendingStatus.GODKJENT,
            )
        }
    }

    @Test
    fun `hentInntektsmeldingerByOrgNr should call inntektsmeldingRepository`() {
        val orgnr = "123456789"
        val fnr = "12345678901"
        val innsendt = LocalDateTime.now()
        val foresporselid = UUID.randomUUID()
        val innsendingId = UUID.randomUUID()
        val skjema = buildInntektsmelding(forespoerselId = foresporselid).tilSkjema()
        every { inntektsmeldingRepository.hent(orgnr) } returns
            listOf(
                InntektsmeldingResponse(
                    navReferanseId = foresporselid,
                    agp = skjema.agp,
                    inntekt = skjema.inntekt,
                    refusjon = skjema.refusjon,
                    sykmeldtFnr = fnr,
                    aarsakInnsending = AarsakInnsending.Ny,
                    typeInnsending = InnsendingType.FORESPURT,
                    innsendtTid = innsendt,
                    versjon = 1,
                    arbeidsgiver = Arbeidsgiver(orgnr, skjema.avsenderTlf),
                    avsender = Avsender("", ""),
                    status = InnsendingStatus.MOTTATT,
                    statusMelding = null,
                    id = innsendingId,
                ),
            )
        val hentInntektsmeldingerByOrgNr = inntektsmeldingService.hentInntektsmeldingerByOrgNr(orgnr)

        verify {
            inntektsmeldingRepository.hent(orgnr)
        }
        assertEquals(1, hentInntektsmeldingerByOrgNr.antall)
        val inntektsmelding = hentInntektsmeldingerByOrgNr.inntektsmeldinger[0]
        assertEquals(orgnr, inntektsmelding.arbeidsgiver.orgnr)
        assertEquals(fnr, inntektsmelding.sykmeldtFnr)
        assertEquals(foresporselid, inntektsmelding.navReferanseId)
        assertEquals(innsendt, inntektsmelding.innsendtTid)
    }

    @Test
    fun `hentInntektsMeldingByRequest må kalle inntektsmeldingRepository`() {
        val foresporselid = UUID.randomUUID()
        val innsendingId = UUID.randomUUID()
        val orgnr = "987654322"
        val fnr = "12345678901"
        val datoFra = LocalDateTime.now()
        val datoTil = datoFra.plusDays(1)
        val innsendt = LocalDateTime.now()
        val skjema = buildInntektsmelding(forespoerselId = foresporselid).tilSkjema()
        val request =
            InntektsmeldingFilterRequest(
                fnr = fnr,
                navReferanseId = foresporselid,
                fraTid = datoFra,
                tilTid = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } returns
            listOf(
                InntektsmeldingResponse(
                    navReferanseId = foresporselid,
                    agp = skjema.agp,
                    inntekt = skjema.inntekt,
                    refusjon = skjema.refusjon,
                    sykmeldtFnr = fnr,
                    aarsakInnsending = AarsakInnsending.Ny,
                    typeInnsending = InnsendingType.FORESPURT,
                    innsendtTid = innsendt,
                    versjon = 1,
                    arbeidsgiver = Arbeidsgiver(orgnr, skjema.avsenderTlf),
                    avsender = Avsender("", ""),
                    status = InnsendingStatus.MOTTATT,
                    statusMelding = null,
                    id = innsendingId,
                ),
            )
        val hentInntektsMeldingByRequest = inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request)

        verify {
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }
        assertEquals(1, hentInntektsMeldingByRequest.antall)
        val inntektsmelding = hentInntektsMeldingByRequest.inntektsmeldinger[0]
        assertEquals(foresporselid, inntektsmelding.navReferanseId)
        assertEquals(orgnr, inntektsmelding.arbeidsgiver.orgnr)
        assertEquals(fnr, inntektsmelding.sykmeldtFnr)
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
        val foresporselid = UUID.randomUUID()
        val datoFra = LocalDateTime.now()
        val datoTil = datoFra.plusDays(1)
        val request =
            InntektsmeldingFilterRequest(
                fnr = fnr,
                navReferanseId = foresporselid,
                fraTid = datoFra,
                tilTid = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } throws Exception()
        assertThrows<Exception> { inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request) }
    }
}
