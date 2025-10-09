package no.nav.helsearbeidsgiver.utils

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.kafka.erInnenforSiste2Aar
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class KafkaConsumerUtilsTest {
    @Test
    fun `timestamp innenfor siste to år returnerer true`() {
        val innenfor =
            Instant
                .now()
                .minus(300, ChronoUnit.DAYS)
                .toEpochMilli()

        innenfor.erInnenforSiste2Aar() shouldBe true
    }

    @Test
    fun `timestamp innenfor siste to år returnerer false`() {
        val utenfor =
            Instant
                .now()
                .minus(365 * 2 + 10, ChronoUnit.DAYS)
                .toEpochMilli()

        utenfor.erInnenforSiste2Aar() shouldBe false
    }
}
