package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class InntektsmeldingAuthTest : HentApiAuthTest<InntektsmeldingResponse, InntektsmeldingFilter, InntektsmeldingResponse>() {
    override val filtreringEndepunkt = "/v1/inntektsmeldinger"
    override val enkeltDokumentEndepunkt = "/v1/inntektsmelding"
    override val utfasetEndepunkt = "/v1/inntektsmeldinger"

    override val dokumentSerializer: KSerializer<InntektsmeldingResponse> = InntektsmeldingResponse.serializer()
    override val filterSerializer: KSerializer<InntektsmeldingFilter> = InntektsmeldingFilter.serializer()

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): InntektsmeldingResponse {
        val inntektsmelding =
            buildInntektsmelding(
                inntektsmeldingId = id,
                orgnr = Orgnr(orgnr),
            )
        return mockInntektsmeldingResponse(inntektsmelding)
    }

    override fun lagFilter(orgnr: String): InntektsmeldingFilter = InntektsmeldingFilter(orgnr = orgnr)

    override fun mockHentingAvDokumenter(
        filter: InntektsmeldingFilter,
        resultat: List<InntektsmeldingResponse>,
    ) {
        every { repositories.inntektsmeldingRepository.hent(filter = filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: InntektsmeldingResponse,
    ) {
        every { repositories.inntektsmeldingRepository.hentMedInnsendingId(id) } returns resultat
    }

    override fun hentOrgnrFraDokument(dokument: InntektsmeldingResponse): String = dokument.arbeidsgiver.orgnr
}
