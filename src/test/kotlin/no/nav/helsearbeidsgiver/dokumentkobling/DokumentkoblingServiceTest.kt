package no.nav.helsearbeidsgiver.dokumentkobling

import io.kotest.assertions.throwables.shouldThrowExactly
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.pdl.domene.PersonNavn
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.ArbeidsgiverStatusDTO
import no.nav.helsearbeidsgiver.utils.TestData
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DokumentkoblingServiceTest {
    val mockDokumentkoblingProducer = mockk<DokumentkoblingProducer>()
    val mockUnleashFeatureToggles = mockk<UnleashFeatureToggles>()
    val dokumentkoblingService =
        DokumentkoblingService(
            dokumentkoblingProducer = mockDokumentkoblingProducer,
            unleashFeatureToggles = mockUnleashFeatureToggles,
            repositories = mockk(),
        )
    private val sykmeldingId = UUID.randomUUID()
    private val soeknadId = UUID.randomUUID()
    private val orgnr = Orgnr.genererGyldig()

    @BeforeEach
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `dokumentkoblingService kaller dokumentkoblingProducer ved mottatt sykmelding`() {
        val sykmeldingMessage =
            sykmeldingMock()
                .medId(sykmeldingId.toString())
                .medOrgnr(orgnr.toString())

        val fullPerson = genererFullPerson()

        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(orgnr) } returns true

        dokumentkoblingService.produserSykmeldingKobling(sykmeldingId, sykmeldingMessage, fullPerson)

        verifySequence {
            mockDokumentkoblingProducer.send(any<Sykmelding>())
        }
    }

    @Test
    fun `dokumentkoblingService kaster feil dersom opprettelse av kobling går galt`() {
        val sykmeldingMessage =
            sykmeldingMock()
                .medId(sykmeldingId.toString())
                .medOrgnr(orgnr.toString())

        val fullPerson = genererFullPerson()

        coEvery {
            mockDokumentkoblingProducer.send(any())
        } throws SerializationException("Noe gikk galt")
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(orgnr) } returns true

        shouldThrowExactly<SerializationException> {
            dokumentkoblingService.produserSykmeldingKobling(sykmeldingId, sykmeldingMessage, fullPerson)
        }
    }

    @Test
    fun `dokumentkoblingService kaller _ikke_ dokumentkoblingProducer dersom feature toggle er skrudd av`() {
        val sykmeldingMessage =
            sykmeldingMock()
                .medId(sykmeldingId.toString())
                .medOrgnr(orgnr.toString())

        val fullPerson = genererFullPerson()

        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(orgnr) } returns false

        dokumentkoblingService.produserSykmeldingKobling(sykmeldingId, sykmeldingMessage, fullPerson)

        verify(exactly = 0) {
            mockDokumentkoblingProducer.send(any())
        }
    }

    @Test
    fun `dokumentkoblingService kaller dokumentkoblingProducer ved mottatt sykepengesøknad`() {
        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(orgnr) } returns true

        dokumentkoblingService.produserSykepengesoeknadKobling(soeknadId, sykmeldingId, orgnr)

        verifySequence {
            mockDokumentkoblingProducer.send(any<Sykepengesoeknad>())
        }
    }

    @Test
    fun `dokumentkoblingService kaster feil dersom opprettelse av søknadkobling går galt`() {
        coEvery {
            mockDokumentkoblingProducer.send(any())
        } throws SerializationException("Noe gikk galt")
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(orgnr) } returns true

        shouldThrowExactly<SerializationException> {
            dokumentkoblingService.produserSykepengesoeknadKobling(soeknadId, sykmeldingId, orgnr)
        }
    }

    @Test
    fun `dokumentkoblingService kaller _ikke_ dokumentkoblingProducer for søknad dersom feature toggle er skrudd av`() {
        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(orgnr) } returns false

        dokumentkoblingService.produserSykepengesoeknadKobling(soeknadId, sykmeldingId, orgnr)

        verify(exactly = 0) {
            mockDokumentkoblingProducer.send(any())
        }
    }

    @Test
    fun `dokumentkoblingService kaller _ikke_ dokumentkoblingProducer for forespørsler dersom feature toggle er skrudd av`() {
        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns false

        dokumentkoblingService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoersel = TestData.forespoerselDokument(fnr = Fnr.genererGyldig().verdi, orgnr = orgnr.verdi))
        dokumentkoblingService.oppdaterDialogMedUtgaattForespoersel(forespoersel = mockForespoersel().copy(orgnr = orgnr.verdi))


        verify(exactly = 0) {
            mockDokumentkoblingProducer.send(any())
        }
    }

    @Test
    fun `dokumentkoblingService kaller dokumentkoblingProducer ved mottatt forespoersel`() {
        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true

        dokumentkoblingService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoersel = TestData.forespoerselDokument(fnr = Fnr.genererGyldig().verdi, orgnr = orgnr.verdi))

        verifySequence {
            mockDokumentkoblingProducer.send(any<ForespoerselSendt>())
        }
    }

    @Test
    fun `dokumentkoblingService kaller dokumentkoblingProducer ved utgaat forespoersel`() {
        coEvery { mockDokumentkoblingProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true
        every { dokumentkoblingService.repositories.forespoerselRepository.hentVedtaksperiodeId(any()) } returns UUID.randomUUID()

        dokumentkoblingService.oppdaterDialogMedUtgaattForespoersel(forespoersel = mockForespoersel().copy(orgnr = orgnr.verdi))

        verifySequence {
            mockDokumentkoblingProducer.send(any<ForespoerselUtgaatt>())
        }
    }

    private fun SendSykmeldingAivenKafkaMessage.medId(id: String) = copy(sykmelding = sykmelding.copy(id = id))

    private fun SendSykmeldingAivenKafkaMessage.medOrgnr(orgnr: String) =
        copy(event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnr, "", "")))

    private fun genererFullPerson(): FullPerson =
        FullPerson(
            foedselsdato = LocalDate.now().minusYears(50),
            navn =
                PersonNavn(
                    fornavn = "Jeppe",
                    mellomnavn = "På",
                    etternavn = "Bjerget",
                ),
            ident = null,
        )
}
