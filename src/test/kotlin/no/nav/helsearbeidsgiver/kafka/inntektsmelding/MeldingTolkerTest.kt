package no.nav.helsearbeidsgiver.kafka.inntektsmelding

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
import no.nav.helsearbeidsgiver.utils.TestData.ARBEIDSGIVER_INITIERT_IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_BESVART
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.SIMBA_PAYLOAD
import no.nav.helsearbeidsgiver.utils.TestData.SYKMELDING_MOTTATT
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(TransactionalExtension::class)
class MeldingTolkerTest {
    val db = DbConfig.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
    val mottakRepository = MottakRepository(db)
    val sykmeldingRepository = spyk(SykmeldingRepository(db))

    val inntektsmeldingTolker = InntektsmeldingTolker(inntektsmeldingService, mottakRepository)
    val mockDialogportenService = mockk<IDialogportenService>()
    val forespoerselTolker = ForespoerselTolker(forespoerselRepository, mottakRepository, mockDialogportenService)
    val sykmeldingTolker = SykmeldingTolker(sykmeldingRepository)

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
    fun `ta i mot og lagre gyldig sykmelding`() {
        sykmeldingTolker.lesMelding(SYKMELDING_MOTTATT)
        verify { sykmeldingRepository.lagreSykmelding(any()) }
    }

    @Test
    fun duplikat() {
        every { mockDialogportenService.opprettDialog(any(), any()) } returns
            Result.success(
                UUID.randomUUID().toString(),
            )
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
    }
}
