package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.buildInntektsmeldingJson
import org.junit.jupiter.api.Test

class InntektsmeldingServiceTest {
    val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)

    @Test
    fun `opprettInntektsmelding should call inntektsmeldingRepository`() {
        val inntektsmeldingJson = buildInntektsmeldingJson()
        val inntektsmelding =
            inntektsmeldingService.jsonMapper.decodeFromString(
                Inntektsmelding.serializer(),
                inntektsmeldingJson,
            )
        inntektsmeldingService.opprettInntektsmelding(inntektsmelding)

        verify {
            inntektsmeldingRepository.opprett(
                im = any(),
                org = inntektsmelding.avsender.orgnr.verdi,
                sykmeldtFnr = inntektsmelding.sykmeldt.fnr.verdi,
                innsendtDato = any(),
                forespoerselID = inntektsmelding.type.id.toString(),
            )
        }
    }
}
