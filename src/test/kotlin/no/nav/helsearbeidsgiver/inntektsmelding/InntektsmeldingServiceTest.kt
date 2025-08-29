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
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.tilSkjema
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class InntektsmeldingServiceTest {
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)

    @Test
    fun `opprettInntektsmelding skal kalle inntektsmeldingRepository`() {
        val inntektsmelding =
            jsonMapper.decodeFromString(
                Inntektsmelding.serializer(),
                buildInntektsmeldingJson(),
            )
        every {
            inntektsmeldingRepository.hentMedInnsendingId(any(), inntektsmelding.id)
        } returns null
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
    fun `opprettInntektsmelding skal ikke kalle inntektsmeldingRepository ved eksisterende inntektsmelding`() {
        val inntektsmelding =
            jsonMapper.decodeFromString(
                Inntektsmelding.serializer(),
                buildInntektsmeldingJson(),
            )
        every {
            inntektsmeldingRepository.hentMedInnsendingId(any(), inntektsmelding.id)
        } returns
            InntektsmeldingResponse(
                navReferanseId = inntektsmelding.type.id,
                inntekt = inntektsmelding.inntekt,
                refusjon = inntektsmelding.refusjon,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                aarsakInnsending = AarsakInnsending.Ny,
                typeInnsending = InnsendingType.FORESPURT,
                innsendtTid = LocalDateTime.now(),
                versjon = 1,
                arbeidsgiver = Arbeidsgiver(inntektsmelding.avsender.orgnr.verdi, inntektsmelding.avsender.tlf),
                avsender = Avsender("", ""),
                status = InnsendingStatus.MOTTATT,
                feilinfo = null,
                agp = inntektsmelding.agp,
                id = inntektsmelding.id,
            )

        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify(exactly = 0) { inntektsmeldingRepository.opprettInntektsmelding(any()) }
    }

    @Test
    fun `hentInntektsMelding må kalle inntektsmeldingRepository`() {
        val navReferanseId = UUID.randomUUID()
        val innsendingId = UUID.randomUUID()
        val orgnr = Orgnr.genererGyldig().verdi
        val fnr = Fnr.genererGyldig().verdi
        val datoFra = LocalDate.now()
        val datoTil = datoFra.plusDays(1)
        val innsendt = LocalDateTime.now()
        val skjema = buildInntektsmelding(forespoerselId = navReferanseId).tilSkjema()
        val filter =
            InntektsmeldingFilter(
                orgnr = orgnr,
                fnr = fnr,
                navReferanseId = navReferanseId,
                fom = datoFra,
                tom = datoTil,
            )
        every { inntektsmeldingRepository.hent(filter = filter) } returns
            listOf(
                InntektsmeldingResponse(
                    navReferanseId = navReferanseId,
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
                    feilinfo = null,
                    id = innsendingId,
                ),
            )
        val hentInntektsMelding = inntektsmeldingService.hentInntektsMelding(filter)

        verify {
            inntektsmeldingRepository.hent(filter = filter)
        }
        assertEquals(1, hentInntektsMelding.size)
        val inntektsmelding = hentInntektsMelding[0]
        assertEquals(navReferanseId, inntektsmelding.navReferanseId)
        assertEquals(orgnr, inntektsmelding.arbeidsgiver.orgnr)
        assertEquals(fnr, inntektsmelding.sykmeldtFnr)
    }

    @Test
    fun `henting av inntektsmelding kaster exception ved feil`() {
        val orgnr = Orgnr.genererGyldig().verdi
        every { inntektsmeldingRepository.hent(filter = InntektsmeldingFilter(orgnr = orgnr)) } throws Exception()

        assertThrows<Exception> { inntektsmeldingService.hentInntektsMelding(filter = InntektsmeldingFilter(orgnr = orgnr)) }
    }

    @Test
    fun `hentInntektsMeldingByRequest should return empty list on failure`() {
        val orgnr = Orgnr.genererGyldig().toString()
        val fnr = Fnr.genererGyldig().toString()
        val navReferanseId = UUID.randomUUID()
        val datoFra = LocalDate.now()
        val datoTil = datoFra.plusDays(1)
        val request =
            InntektsmeldingFilter(
                orgnr = orgnr,
                fnr = fnr,
                navReferanseId = navReferanseId,
                fom = datoFra,
                tom = datoTil,
            )
        every { inntektsmeldingRepository.hent(filter = request) } throws Exception()
        assertThrows<Exception> { inntektsmeldingService.hentInntektsMelding(request) }
    }

    @Test
    fun `hentNyesteInntektsmeldingByNavRefernaseId returnerer nyeste inntektsmelding`() {
        val orgnr = "123456789"
        val fnr = "12345678901"
        val innsendt1 = LocalDateTime.now().minusWeeks(1)
        val innsendt2 = LocalDateTime.now()
        val navReferanseId = UUID.randomUUID()
        val innsendingId1 = UUID.randomUUID()
        val innsendingId2 = UUID.randomUUID()
        val skjema = buildInntektsmelding(forespoerselId = navReferanseId).tilSkjema()
        every { inntektsmeldingRepository.hent(navReferanseId) } returns
            listOf(
                InntektsmeldingResponse(
                    navReferanseId = navReferanseId,
                    agp = skjema.agp,
                    inntekt = skjema.inntekt,
                    refusjon = skjema.refusjon,
                    sykmeldtFnr = fnr,
                    aarsakInnsending = AarsakInnsending.Ny,
                    typeInnsending = InnsendingType.FORESPURT,
                    innsendtTid = innsendt1,
                    versjon = 1,
                    arbeidsgiver = Arbeidsgiver(orgnr, skjema.avsenderTlf),
                    avsender = Avsender("", ""),
                    status = InnsendingStatus.MOTTATT,
                    feilinfo = null,
                    id = innsendingId1,
                ),
                InntektsmeldingResponse(
                    navReferanseId = navReferanseId,
                    agp = skjema.agp,
                    inntekt = skjema.inntekt,
                    refusjon = skjema.refusjon,
                    sykmeldtFnr = fnr,
                    aarsakInnsending = AarsakInnsending.Endring,
                    typeInnsending = InnsendingType.FORESPURT,
                    innsendtTid = innsendt2,
                    versjon = 1,
                    arbeidsgiver = Arbeidsgiver(orgnr, skjema.avsenderTlf),
                    avsender = Avsender("", ""),
                    status = InnsendingStatus.MOTTATT,
                    feilinfo = null,
                    id = innsendingId2,
                ),
            )
        val hentInntektsmelding = inntektsmeldingService.hentNyesteInntektsmeldingByNavReferanseId(navReferanseId)

        verify {
            inntektsmeldingRepository.hent(navReferanseId)
        }
        assertEquals(innsendingId2, hentInntektsmelding?.id)
        assertEquals(orgnr, hentInntektsmelding?.arbeidsgiver?.orgnr)
        assertEquals(fnr, hentInntektsmelding?.sykmeldtFnr)
        assertEquals(navReferanseId, hentInntektsmelding?.navReferanseId)
        assertEquals(innsendt2, hentInntektsmelding?.innsendtTid)
    }
}
