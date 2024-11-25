package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.ARBEIDSGIVER_INITIERT_IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_BESVART
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.SIMBA_PAYLOAD
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TransactionalExtension::class)
class MeldingTolkerTest {
    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
    val mottakRepository = MottakRepository(db)
    val inntektsmeldingTolker = InntektsmeldingTolker(inntektsmeldingService, mottakRepository)
    val forespoerselTolker = ForespoerselTolker(forespoerselRepository, mottakRepository)

    @Test
    fun kunLagreEventerSomMatcher() {
        // Test at kjente payloads ikke kræsjer:
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
        forespoerselTolker.lesMelding(FORESPOERSEL_BESVART)
        inntektsmeldingTolker.lesMelding(IM_MOTTATT)
        inntektsmeldingTolker.lesMelding(ARBEIDSGIVER_INITIERT_IM_MOTTATT)

        // Skal ikke lagre:
        inntektsmeldingTolker.lesMelding(SIMBA_PAYLOAD)
    }

    @Test
    fun duplikat() {
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
        forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
    }
}
