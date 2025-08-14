package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import java.util.UUID

class SykmeldingAuthTest : HentApiAuthTest<Sykmelding, SykmeldingFilterRequest, SykmeldingDTO>() {
    override val filtreringEndepunkt = "/v1/sykmeldinger"
    override val enkeltDokumentEndepunkt = "/v1/sykmelding"
    override val utfasetEndepunkt = "/v1/sykmeldinger"

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): SykmeldingDTO =
        sykmeldingMock()
            .medId(id.toString())
            .medOrgnr(orgnr)
            .tilSykmeldingDTO()

    override fun lagFilter(orgnr: String?): SykmeldingFilterRequest = SykmeldingFilterRequest(orgnr = orgnr)

    override val filterSerializer: KSerializer<SykmeldingFilterRequest> = SykmeldingFilterRequest.serializer()

    override fun mockHentingAvDokumenter(
        orgnr: String,
        resultat: List<SykmeldingDTO>,
    ) {
        every { repositories.sykmeldingRepository.hentSykmeldinger(orgnr) } returns resultat
    }

    override fun mockHentingAvDokumenter(
        orgnr: String,
        filter: SykmeldingFilterRequest,
        resultat: List<SykmeldingDTO>,
    ) {
        every { repositories.sykmeldingRepository.hentSykmeldinger(orgnr, filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: SykmeldingDTO,
    ) {
        every { repositories.sykmeldingRepository.hentSykmelding(id) } returns resultat
    }

    override fun lesDokumenterFraRespons(respons: HttpResponse): List<Sykmelding> = runBlocking { respons.body<List<Sykmelding>>() }

    override fun lesEnkeltDokumentFraRespons(respons: HttpResponse): Sykmelding = runBlocking { respons.body<Sykmelding>() }

    override fun hentOrgnrFraDokument(dokument: Sykmelding): String = dokument.arbeidsgiver.orgnr.toString()
}
