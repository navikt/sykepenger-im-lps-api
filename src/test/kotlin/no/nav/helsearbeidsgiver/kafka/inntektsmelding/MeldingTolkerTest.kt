package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.sykmelding.SykmeldingTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.TestData.ARBEIDSGIVER_INITIERT_IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_BESVART
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.SIMBA_PAYLOAD
import no.nav.helsearbeidsgiver.utils.TestData.SYKMELDING_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.TRENGER_FORESPOERSEL
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(TransactionalExtension::class)
class MeldingTolkerTest {
    val db = DbConfig.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
    val mottakRepository = spyk(MottakRepository(db))
    val sykmeldingRepository = spyk(SykmeldingRepository(db))
    val sykmeldingService = spyk(SykmeldingService(sykmeldingRepository))
    val inntektsmeldingTolker = InntektsmeldingTolker(inntektsmeldingService, mottakRepository)
    val mockDialogportenService = mockk<IDialogportenService>()
    val mockUnleashFeatureToggles = mockk<UnleashFeatureToggles>(relaxed = true)
    val forespoerselTolker = ForespoerselTolker(forespoerselRepository, mottakRepository, mockDialogportenService)
    val sykmeldingTolker = SykmeldingTolker(sykmeldingService, mockUnleashFeatureToggles)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun kunLagreEventerSomMatcher() {
        every { mockDialogportenService.opprettDialog(any(), any()) } returns
            Result.success(
                UUID.randomUUID().toString(),
            )
        // Test at kjente payloads ikke kræsjer:
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
        verify { mockDialogportenService.opprettDialog(any(), any()) }

        forespoerselTolker.lesMelding(FORESPOERSEL_BESVART)
        inntektsmeldingTolker.lesMelding(IM_MOTTATT)
        inntektsmeldingTolker.lesMelding(ARBEIDSGIVER_INITIERT_IM_MOTTATT)

        // Skal ikke lagre:
        inntektsmeldingTolker.lesMelding(SIMBA_PAYLOAD)
    }

    @Test
    fun `sykmeldingTolker deserialiserer og lagrer gyldig sykmelding`() {
        sykmeldingTolker.lesMelding(SYKMELDING_MOTTATT)
        verify { sykmeldingService.lagreSykmelding(any()) }
    }

    @Test
    fun `forespoerselTolker håndterer duplikater`() {
        every { mockDialogportenService.opprettDialog(any(), any()) } returns
            Result.success(
                UUID.randomUUID().toString(),
            )
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
    }

    @Test
    fun `trengerForespoersel-meldinger ignoreres uten å lagre til mottak`() {
        forespoerselTolker.lesMelding(TRENGER_FORESPOERSEL)
        verify(exactly = 0) { mottakRepository.opprett(any()) }
    }
}
