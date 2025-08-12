package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.authorization.HentEntitetApiAuthTest
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

class SykmeldingAuthTest : HentEntitetApiAuthTest<Sykmelding, SykmeldingFilterRequest, SykmeldingDTO>() {
    override val filtreringEndepunkt = "/v1/sykmeldinger"
    override val enkeltEntitetEndepunkt = "/v1/sykmelding"
    override val utfasetEndepunkt = "/v1/sykmeldinger"

    override fun mockEntitet(
        id: UUID,
        orgnr: String,
    ): SykmeldingDTO =
        sykmeldingMock()
            .medId(id.toString())
            .medOrgnr(orgnr)
            .tilSykmeldingDTO()

    override fun lagFilterRequest(orgnr: String?): SykmeldingFilterRequest = SykmeldingFilterRequest(orgnr = orgnr)

    override fun serialiserFilterRequest(filter: SykmeldingFilterRequest): JsonElement =
        filter.toJson(serializer = SykmeldingFilterRequest.serializer())

    override fun mockHentingAvEntiteter(
        orgnr: String,
        filter: SykmeldingFilterRequest?,
        resultat: List<SykmeldingDTO>,
    ) {
        every { repositories.sykmeldingRepository.hentSykmeldinger(orgnr, filter) } returns resultat
    }

    override fun mockHentingAvEnkeltEntitet(
        id: UUID,
        resultat: SykmeldingDTO,
    ) {
        every { repositories.sykmeldingRepository.hentSykmelding(id) } returns resultat
    }

    override fun lesEntiteterFraRespons(respons: HttpResponse): List<Sykmelding> = runBlocking { respons.body<List<Sykmelding>>() }

    override fun lesEnkeltEntitetFraRespons(respons: HttpResponse): Sykmelding = runBlocking { respons.body<Sykmelding>() }

    override fun hentOrgnrFraEntitet(entitet: Sykmelding): String = entitet.arbeidsgiver.orgnr.toString()
}
