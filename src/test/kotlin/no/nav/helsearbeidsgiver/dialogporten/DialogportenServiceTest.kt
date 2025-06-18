package no.nav.helsearbeidsgiver.dialogporten

import io.kotest.assertions.throwables.shouldThrowExactly
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.SerializationException
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DialogportenServiceTest {
    val mockDialogProducer = mockk<DialogProducer>()
    val mockSoeknadRepository = mockk<SoeknadRepository>()
    val mockUnleashFeatureToggles = mockk<UnleashFeatureToggles>()
    val dialogportenService = DialogportenService(mockDialogProducer, mockSoeknadRepository, mockUnleashFeatureToggles)

    @BeforeEach
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `dialogportenservice kaller dialogProducer ved mottatt sykmelding`() {
        val dialogMelding = genererDialogSykmelding()

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialogMelding.orgnr) } returns true

        dialogportenService.opprettNyDialogMedSykmelding(dialogMelding)

        verify(exactly = 1) {
            mockDialogProducer.send(dialogMelding)
        }
    }

    @Test
    fun `dialogportenservice kaster feil dersom opprettelse av dialog går galt`() {
        val dialogMelding = genererDialogSykmelding()
        coEvery {
            mockDialogProducer.send(
                any(),
            )
        } throws SerializationException("Noe gikk galt")
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialogMelding.orgnr) } returns true

        shouldThrowExactly<SerializationException> {
            dialogportenService.opprettNyDialogMedSykmelding(
                dialogMelding,
            )
        }
    }

    @Test
    fun `dialogportenservice kaller _ikke_ dialogProducer dersom feature toggle for dialogutsending er skrudd av`() {
        val dialogMelding = genererDialogSykmelding()
        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(dialogMelding.orgnr) } returns false

        dialogportenService.opprettNyDialogMedSykmelding(dialogMelding)

        verify(exactly = 0) {
            mockDialogProducer.send(any())
        }
    }

    @Test
    fun `dialogportenservice kaller dialogProducer ved mottatt sykepengesøknad`() {
        val dialogMelding = genererDialogSykepengesoeknad()

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(dialogMelding.orgnr) } returns true

        dialogportenService.oppdaterDialogMedSykepengesoeknad(dialogMelding)

        verify(exactly = 1) {
            mockDialogProducer.send(dialogMelding)
        }
    }

    @Test
    fun `dialogportenservice kaster feil dersom opprettelse av dialog går galt ved mottatt søknad`() {
        val dialogMelding = genererDialogSykepengesoeknad()
        coEvery {
            mockDialogProducer.send(
                any(),
            )
        } throws SerializationException("Noe gikk galt")
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(dialogMelding.orgnr) } returns true

        shouldThrowExactly<SerializationException> {
            dialogportenService.oppdaterDialogMedSykepengesoeknad(
                dialogMelding,
            )
        }
    }

    @Test
    fun `dialogportenservice kaller _ikke_ dialogProducer dersom feature toggle for dialogutsending er skrudd av ved mottatt søknad`() {
        val dialogMelding = genererDialogSykepengesoeknad()
        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(dialogMelding.orgnr) } returns false

        dialogportenService.oppdaterDialogMedSykepengesoeknad(dialogMelding)

        verify(exactly = 0) {
            mockDialogProducer.send(any())
        }
    }

    @Test
    fun `dialogportenservice kaller dialogProducer ved mottatt inntektsmeldingsforespørsel`() {
        val orgnr = Orgnr.genererGyldig()
        val forespoerselDokument = forespoerselDokument(orgnr.toString(), Fnr.genererGyldig().toString())
        val soeknad = soeknadMock()

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true
        every { mockSoeknadRepository.hentSoeknaderMedVedtaksperiodeId(any()) } returns listOf(soeknad)

        dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoerselDokument)

        val forventetDialogMelding =
            DialogInntektsmeldingsforespoersel(
                forespoerselId = forespoerselDokument.forespoerselId,
                sykmeldingId = requireNotNull(soeknad.sykmeldingId),
                orgnr = orgnr,
            )

        verify(exactly = 1) {
            mockDialogProducer.send(forventetDialogMelding)
        }
    }

    @Test
    fun `dialogportenservice kaster feil dersom opprettelse av dialog går galt ved mottatt inntektsmeldingsforespørsel`() {
        val orgnr = Orgnr.genererGyldig()
        val forespoerselDokument = forespoerselDokument(orgnr.toString(), Fnr.genererGyldig().toString())
        val soeknad = soeknadMock()

        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true
        every { mockSoeknadRepository.hentSoeknaderMedVedtaksperiodeId(any()) } returns listOf(soeknad)
        coEvery {
            mockDialogProducer.send(
                any(),
            )
        } throws SerializationException("Noe gikk galt")

        shouldThrowExactly<SerializationException> {
            dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(
                forespoerselDokument,
            )
        }
    }

    @Test
    fun `kaller _ikke_ dialogProducer ved mottatt inntektsmeldingsforespørsel dersom feature toggle for dialogutsending er skrudd av`() {
        val orgnr = Orgnr.genererGyldig()
        val forespoerselDokument = forespoerselDokument(orgnr.toString(), Fnr.genererGyldig().toString())

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns false

        dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoerselDokument)

        verify(exactly = 0) {
            mockDialogProducer.send(any())
        }
    }

    @Test
    fun `dialogportenservice kaller _ikke_ dialogProducer dersom vi ikke finner noen søknader for vedtaksperioden i forespørselen`() {
        val orgnr = Orgnr.genererGyldig()
        val forespoerselDokument = forespoerselDokument(orgnr.toString(), Fnr.genererGyldig().toString())

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true
        every { mockSoeknadRepository.hentSoeknaderMedVedtaksperiodeId(any()) } returns emptyList()

        dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoerselDokument)

        verify(exactly = 0) {
            mockDialogProducer.send(any())
        }
    }

    @Test
    fun `dialogportenservice kaller dialogProducer med sykmeldingIden basert på nyeste søknad ved mottatt inntektsmeldingsforespørsel`() {
        val orgnr = Orgnr.genererGyldig()
        val forespoerselDokument = forespoerselDokument(orgnr.toString(), Fnr.genererGyldig().toString())

        val soeknadEldre = soeknadMock()
        val soeknadNyere =
            soeknadMock().copy(
                sykmeldingId = UUID.randomUUID(),
                sendtArbeidsgiver = requireNotNull(soeknadEldre.sendtArbeidsgiver).plusDays(1),
                sendtNav = requireNotNull(soeknadEldre.sendtNav).plusDays(1),
            )

        coEvery { mockDialogProducer.send(any()) } just Runs
        every { mockUnleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr) } returns true
        every { mockSoeknadRepository.hentSoeknaderMedVedtaksperiodeId(any()) } returns
            listOf(
                soeknadEldre,
                soeknadNyere,
            )

        dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(forespoerselDokument)

        val forventetDialogMelding =
            DialogInntektsmeldingsforespoersel(
                forespoerselId = forespoerselDokument.forespoerselId,
                sykmeldingId = requireNotNull(soeknadNyere.sykmeldingId),
                orgnr = orgnr,
            )

        verify(exactly = 1) {
            mockDialogProducer.send(forventetDialogMelding)
        }
    }

    private fun genererDialogSykmelding(): DialogSykmelding =
        DialogSykmelding(
            sykmeldingId = UUID.randomUUID(),
            orgnr = Orgnr.genererGyldig(),
            foedselsdato = LocalDate.now(),
            fulltNavn = "Dialogus Sykmeldingus",
            sykmeldingsperioder = listOf(Periode(fom = 1.januar, tom = 12.januar)),
        )

    private fun genererDialogSykepengesoeknad(): DialogSykepengesoeknad =
        DialogSykepengesoeknad(
            soeknadId = UUID.randomUUID(),
            sykmeldingId = UUID.randomUUID(),
            orgnr = Orgnr.genererGyldig(),
        )
}
