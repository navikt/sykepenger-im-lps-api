package no.nav.helsearbeidsgiver.soeknad

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.authorization.HentEntitetApiAuthTest
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

class SoeknadAuthTest2 : HentEntitetApiAuthTest<Sykepengesoeknad, SykepengesoeknadFilter, SykepengesoknadDTO>() {
    override val filtreringEndepunkt = "/v1/sykepengesoeknader"
    override val enkeltEntitetEndepunkt = "/v1/sykepengesoeknad"
    override val utfasetEndepunkt = "/v1/sykepengesoeknader"

    override fun mockEntitet(
        id: UUID,
        orgnr: String,
    ): SykepengesoknadDTO =
        soeknadMock()
            .medId(id)
            .medOrgnr(orgnr)

    override fun lagFilterRequest(orgnr: String?): SykepengesoeknadFilter = SykepengesoeknadFilter(orgnr = orgnr)

    override fun serialiserFilterRequest(filter: SykepengesoeknadFilter): JsonElement =
        filter.toJson(serializer = SykepengesoeknadFilter.serializer())

    override fun mockHentingAvEntiteter(
        orgnr: String,
        filter: SykepengesoeknadFilter?,
        resultat: List<SykepengesoknadDTO>,
    ) {
        every { repositories.soeknadRepository.hentSoeknader(orgnr, filter) } returns resultat
    }

    override fun mockHentingAvEnkeltEntitet(
        id: UUID,
        resultat: SykepengesoknadDTO,
    ) {
        every { repositories.soeknadRepository.hentSoeknad(id) } returns resultat
    }

    override fun lesEntiteterFraRespons(respons: HttpResponse): List<Sykepengesoeknad> =
        runBlocking { respons.body<List<Sykepengesoeknad>>() }

    override fun lesEnkeltEntitetFraRespons(respons: HttpResponse): Sykepengesoeknad = runBlocking { respons.body<Sykepengesoeknad>() }

    override fun hentOrgnrFraEntitet(entitet: Sykepengesoeknad): String = entitet.arbeidsgiver.orgnr
}
