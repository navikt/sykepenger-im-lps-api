package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
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

    override fun lagFilter(orgnr: String?): InntektsmeldingFilterRequest = InntektsmeldingFilterRequest(orgnr = orgnr)

    override val filterSerializer: KSerializer<InntektsmeldingFilterRequest> = InntektsmeldingFilterRequest.serializer()

    override fun mockHentingAvDokumenter(
        orgnr: String,
        resultat: List<InntektsmeldingResponse>,
    ) {
        every { repositories.inntektsmeldingRepository.hent(orgnr) } returns resultat
    }

    override fun mockHentingAvDokumenter(
        orgnr: String,
        filter: InntektsmeldingFilterRequest,
        resultat: List<InntektsmeldingResponse>,
    ) {
        every { repositories.inntektsmeldingRepository.hent(orgnr, filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: InntektsmeldingResponse,
    ) {
        every { repositories.inntektsmeldingRepository.hentMedInnsendingId(id) } returns resultat
    }

    override fun lesDokumenterFraRespons(respons: HttpResponse): List<InntektsmeldingResponse> =
        runBlocking { respons.body<List<InntektsmeldingResponse>>() }

    override fun lesEnkeltDokumentFraRespons(respons: HttpResponse): InntektsmeldingResponse =
        runBlocking { respons.body<InntektsmeldingResponse>() }

    override fun hentOrgnrFraDokument(dokument: InntektsmeldingResponse): String = dokument.arbeidsgiver.orgnr
}
