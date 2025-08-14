package no.nav.helsearbeidsgiver.inntektsmelding

import io.mockk.every
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class InntektsmeldingAuthTest : HentApiAuthTest<InntektsmeldingResponse, InntektsmeldingFilterRequest, InntektsmeldingResponse>() {
    override val filtreringEndepunkt = "/v1/inntektsmeldinger"
    override val enkeltDokumentEndepunkt = "/v1/inntektsmelding"
    override val utfasetEndepunkt = "/v1/inntektsmeldinger"

    override val dokumentSerializer: KSerializer<InntektsmeldingResponse> = InntektsmeldingResponse.serializer()
    override val filterSerializer: KSerializer<InntektsmeldingFilterRequest> = InntektsmeldingFilterRequest.serializer()

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

    override fun lagFilter(orgnr: String): InntektsmeldingFilterRequest = InntektsmeldingFilterRequest(orgnr = orgnr)

    override fun mockHentingAvDokumenter(
        orgnr: String,
        resultat: List<InntektsmeldingResponse>,
    ) {
        every { repositories.inntektsmeldingRepository.hent(orgnr = orgnr) } returns resultat
    }

    override fun mockHentingAvDokumenter(
        filter: InntektsmeldingFilterRequest,
        resultat: List<InntektsmeldingResponse>,
    ) {
        every { repositories.inntektsmeldingRepository.hent(request = filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: InntektsmeldingResponse,
    ) {
        every { repositories.inntektsmeldingRepository.hentMedInnsendingId(id) } returns resultat
    }

    override fun hentOrgnrFraDokument(dokument: InntektsmeldingResponse): String = dokument.arbeidsgiver.orgnr
}
