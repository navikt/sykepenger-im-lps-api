package no.nav.helsearbeidsgiver.kafka

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KafkaMonitorTest {
    @Test
    fun `ingen konsumenter registerert skal gi ingen feil`() {
        KafkaMonitor.harFeil() shouldBe false
    }

    @Test
    fun `registrering av nye konsumenter skal gi ingen feil`() {
        KafkaMonitor.registrerConsumer("test1")
        KafkaMonitor.registrerConsumer("test2")
        KafkaMonitor.registrerConsumer("test3")
        KafkaMonitor.harFeil() shouldBe false
    }

    @Test
    fun `registrer feil og null ut ved å re-registrere konsument`() {
        KafkaMonitor.registrerConsumer("test1")
        KafkaMonitor.registrerFeil("test1")
        KafkaMonitor.harFeil() shouldBe true
        KafkaMonitor.registrerConsumer("test2")
        KafkaMonitor.harFeil() shouldBe true
        KafkaMonitor.registrerConsumer("test1")
        KafkaMonitor.harFeil() shouldBe false
    }
}
