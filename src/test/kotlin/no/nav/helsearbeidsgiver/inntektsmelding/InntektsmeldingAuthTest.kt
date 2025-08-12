package no.nav.helsearbeidsgiver.inntektsmelding

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.authorization.HentEntitetApiAuthTest
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class InntektsmeldingAuthTest : HentEntitetApiAuthTest<InntektsmeldingResponse, InntektsmeldingFilterRequest, InntektsmeldingResponse>() {
    override val filtreringEndepunkt = "/v1/inntektsmeldinger"
    override val enkeltEntitetEndepunkt = "/v1/inntektsmelding"
    override val utfasetEndepunkt = "/v1/inntektsmeldinger"

    override fun mockEntitet(
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

    override fun lagFilterRequest(orgnr: String?): InntektsmeldingFilterRequest = InntektsmeldingFilterRequest(orgnr = orgnr)

    override fun serialiserFilterRequest(filter: InntektsmeldingFilterRequest): JsonElement =
        filter.toJson(serializer = InntektsmeldingFilterRequest.serializer())

    override fun mockHentingAvEntiteter(
        orgnr: String,
        filter: InntektsmeldingFilterRequest?,
        resultat: List<InntektsmeldingResponse>,
    ) {
        every {
            repositories.inntektsmeldingRepository.hent(
                orgnr,
                filter,
            )
        } returns resultat
    }

    override fun mockHentingAvEnkeltEntitet(
        id: UUID,
        resultat: InntektsmeldingResponse,
    ) {
        every { repositories.inntektsmeldingRepository.hentMedInnsendingId(id) } returns resultat
    }

    override fun lesEntiteterFraRespons(respons: HttpResponse): List<InntektsmeldingResponse> =
        runBlocking { respons.body<List<InntektsmeldingResponse>>() }

    override fun lesEnkeltEntitetFraRespons(respons: HttpResponse): InntektsmeldingResponse =
        runBlocking { respons.body<InntektsmeldingResponse>() }

    override fun hentOrgnrFraEntitet(entitet: InntektsmeldingResponse): String = entitet.arbeidsgiver.orgnr
}
