package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.ikkeAktuellPayload
import no.nav.helsearbeidsgiver.utils.TestData.payload
import no.nav.helsearbeidsgiver.utils.TestData.payload2
import no.nav.helsearbeidsgiver.utils.TestData.payload3
import no.nav.helsearbeidsgiver.utils.TestData.payload4
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TransactionalExtension::class)
class DatabaseSimbaKafkaConsumerTest {
    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val mottakRepository = MottakRepository(db)
    val simbaKafkaConsumer = SimbaKafkaConsumer(inntektsmeldingRepository, forespoerselRepository, mottakRepository)

    @Test
    fun kunLagreEventerSomMatcher() {
        // Test at kjente payloads ikke kræsjer:
        simbaKafkaConsumer.handleRecord(payload)
        simbaKafkaConsumer.handleRecord(payload2)
        simbaKafkaConsumer.handleRecord(payload3)
        simbaKafkaConsumer.handleRecord(payload4)

        // Skal ikke lagre:
        simbaKafkaConsumer.handleRecord(ikkeAktuellPayload)
    }

    @Test
    fun duplikat() {
        simbaKafkaConsumer.handleRecord(payload)
        simbaKafkaConsumer.handleRecord(payload)
    }

    @Test
    fun feilsituasjon() {
        /* 1. Kan ikke parse record til objekt
         *  Mulige årsaker:
         *  - Nytt format i simba (feil i lokal parser)
         *  - Feil format sendt fra simba (feil i Simba)
         *
         *  Løsning:
         *  - Hvis det er en melding vi tror vi skal ha (event matcher) - logg feil, stopp kafkaconsumer, ikke gå videre
         *  - Hvis bare jibberish: logg feil, lagre record i mottak (på DLQ), men fortsett å parse neste melding. Vurdere en threshold-nivå før man evt stopper?
         *
         *
         * 2. Feil ved lagring til db
         *    - feiler på mottak: gå i feil-mode
         *    - feiler på lagring objekt - rulle tilbake..
         *
         *
         * */
    }
}
