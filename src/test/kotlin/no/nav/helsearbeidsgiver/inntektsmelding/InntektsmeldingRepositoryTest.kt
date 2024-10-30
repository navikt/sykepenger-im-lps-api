package no.nav.helsearbeidsgiver.inntektsmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.db.Database
import org.junit.jupiter.api.Test

class InntektsmeldingRepositoryTest {
    @Test
    fun opprett() {
        val db = Database.init()
        val repo = InntektsmeldingRepository(db)
        val dok = """{"test":"test"}"""
        val orgnr = "123"
        val fnr = "321"
        val forespoerselID = "1234"
        repo.opprett(dok, orgnr, fnr, forespoerselID)
        val inntektsmelding = repo.hent(orgnr)[0]
        inntektsmelding.foresporselid shouldBe forespoerselID
        inntektsmelding.orgnr shouldBe orgnr
        inntektsmelding.fnr shouldBe fnr
        inntektsmelding.dokument.toString() shouldBe dok
    }
}
