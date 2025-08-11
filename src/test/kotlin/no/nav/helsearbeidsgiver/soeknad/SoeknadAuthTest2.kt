package no.nav.helsearbeidsgiver.soeknad

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.authorization.AuthTest
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

class SoeknadAuthTest2 : AuthTest<Sykepengesoeknad, SykepengesoeknadFilter, SykepengesoknadDTO>() {
    override val repository = repositories.soeknadRepository
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
        result: SykepengesoknadDTO,
    ) {
        every { repositories.soeknadRepository.hentSoeknad(id) } returns result
    }

    override fun deserialiserEntiteterFraRespons(response: HttpResponse): List<*> = runBlocking { response.body<List<Sykepengesoeknad>>() }

    override fun deserialiserEnkeltEntitetFraRespons(response: HttpResponse): Any = runBlocking { response.body<Sykepengesoeknad>() }

    override fun hentOrgnrFraEntitet(entitet: Any): String = (entitet as Sykepengesoeknad).arbeidsgiver.orgnr
}
