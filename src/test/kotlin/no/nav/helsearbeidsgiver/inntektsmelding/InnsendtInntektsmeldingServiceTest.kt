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

class InnsendtInntektsmeldingServiceTest {
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
            inntektsmeldingRepository.opprettInntektsmeldingFraSimba(
                im = inntektsmelding,
                org = inntektsmelding.avsender.orgnr.verdi,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                innsendtDato = inntektsmelding.mottatt.toLocalDateTime(),
                forespoerselID = inntektsmelding.type.id.toString(),
            )
        } returns 1

        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify {
            inntektsmeldingRepository.opprettInntektsmeldingFraSimba(
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
        val foresporselid = UUID.randomUUID().toString()
        val skjema = buildInntektsmelding(forespoerselId = foresporselid).tilSkjema()
        every { inntektsmeldingRepository.hent(orgnr) } returns
            listOf(
                InnsendtInntektsmelding(
                    skjema = skjema,
                    orgnr = orgnr,
                    fnr = fnr,
                    innsendtTid = innsendt,
                    aarsakInnsending = AarsakInnsending.Ny,
                    typeInnsending = InnsendingType.FORESPURT,
                    versjon = 1,
                    status = InnsendingStatus.MOTTATT,
                    statusMelding = null,
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
        assertEquals(foresporselid, inntektsmelding.skjema?.forespoerselId.toString())
        assertEquals(innsendt, inntektsmelding.innsendtTid)
        assertEquals(skjema, inntektsmelding.skjema)
    }

    @Test
    fun `hentInntektsMeldingByRequest må kalle inntektsmeldingRepository`() {
        val foresporselid = UUID.randomUUID().toString()
        val orgnr = "987654322"
        val fnr = "12345678901"
        val datoFra = LocalDateTime.now()
        val datoTil = datoFra.plusDays(1)
        val request =
            InntektsmeldingRequest(
                fnr = fnr,
                foresporselId = foresporselid,
                fraTid = datoFra,
                tilTid = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } returns
            listOf(
                InnsendtInntektsmelding(
                    skjema = buildInntektsmelding(forespoerselId = foresporselid).tilSkjema(),
                    orgnr = orgnr,
                    fnr = fnr,
                    innsendtTid = LocalDateTime.now(),
                    aarsakInnsending = AarsakInnsending.Ny,
                    typeInnsending = InnsendingType.FORESPURT,
                    versjon = 1,
                    status = InnsendingStatus.MOTTATT,
                    statusMelding = null,
                ),
            )
        val hentInntektsMeldingByRequest = inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request)

        verify {
            inntektsmeldingRepository.hent(orgNr = orgnr, request = request)
        }
        assertEquals(1, hentInntektsMeldingByRequest.antall)
        val inntektsmelding = hentInntektsMeldingByRequest.inntektsmeldinger[0]
        assertEquals(foresporselid, inntektsmelding.skjema?.forespoerselId.toString())
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
                foresporselId = foresporselid,
                fraTid = datoFra,
                tilTid = datoTil,
            )
        every { inntektsmeldingRepository.hent(orgNr = orgnr, request = request) } throws Exception()
        assertThrows<Exception> { inntektsmeldingService.hentInntektsMeldingByRequest(orgnr, request) }
    }
}
