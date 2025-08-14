package no.nav.helsearbeidsgiver.soeknad

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import java.util.UUID

class SoeknadAuthTest : HentApiAuthTest<Sykepengesoeknad, SykepengesoeknadFilter, SykepengesoknadDTO>() {
    override val filtreringEndepunkt = "/v1/sykepengesoeknader"
    override val enkeltDokumentEndepunkt = "/v1/sykepengesoeknad"
    override val utfasetEndepunkt = "/v1/sykepengesoeknader"

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): SykepengesoknadDTO =
        soeknadMock()
            .medId(id)
            .medOrgnr(orgnr)

    override fun lagFilter(orgnr: String?): SykepengesoeknadFilter = SykepengesoeknadFilter(orgnr = orgnr)

    override val filterSerializer: KSerializer<SykepengesoeknadFilter> = SykepengesoeknadFilter.serializer()

    override fun mockHentingAvDokumenter(
        orgnr: String,
        resultat: List<SykepengesoknadDTO>,
    ) {
        every { repositories.soeknadRepository.hentSoeknader(orgnr) } returns resultat
    }

    override fun mockHentingAvDokumenter(
        orgnr: String,
        filter: SykepengesoeknadFilter,
        resultat: List<SykepengesoknadDTO>,
    ) {
        every { repositories.soeknadRepository.hentSoeknader(orgnr, filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: SykepengesoknadDTO,
    ) {
        every { repositories.soeknadRepository.hentSoeknad(id) } returns resultat
    }

    override fun lesDokumenterFraRespons(respons: HttpResponse): List<Sykepengesoeknad> =
        runBlocking { respons.body<List<Sykepengesoeknad>>() }

    override fun lesEnkeltDokumentFraRespons(respons: HttpResponse): Sykepengesoeknad = runBlocking { respons.body<Sykepengesoeknad>() }

    override fun hentOrgnrFraDokument(dokument: Sykepengesoeknad): String = dokument.arbeidsgiver.orgnr
}
