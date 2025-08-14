package no.nav.helsearbeidsgiver.sykmelding

import io.mockk.every
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import java.util.UUID

class SykmeldingAuthTest : HentApiAuthTest<Sykmelding, SykmeldingFilterRequest, SykmeldingDTO>() {
    override val filtreringEndepunkt = "/v1/sykmeldinger"
    override val enkeltDokumentEndepunkt = "/v1/sykmelding"
    override val utfasetEndepunkt = "/v1/sykmeldinger"

    override val dokumentSerializer: KSerializer<Sykmelding> = Sykmelding.serializer()
    override val filterSerializer: KSerializer<SykmeldingFilterRequest> = SykmeldingFilterRequest.serializer()

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): SykmeldingDTO =
        sykmeldingMock()
            .medId(id.toString())
            .medOrgnr(orgnr)
            .tilSykmeldingDTO()

    override fun lagFilter(orgnr: String?): SykmeldingFilterRequest = SykmeldingFilterRequest(orgnr = orgnr)

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

    override fun hentOrgnrFraDokument(dokument: Sykmelding): String = dokument.arbeidsgiver.orgnr.toString()
}
