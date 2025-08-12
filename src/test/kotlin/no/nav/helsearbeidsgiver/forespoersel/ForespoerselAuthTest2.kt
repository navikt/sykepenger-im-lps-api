package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.authorization.AuthTest
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import java.util.UUID

class ForespoerselAuthTest2 : AuthTest<Forespoersel, ForespoerselRequest, Forespoersel>() {
    override val repository = repositories.forespoerselRepository
    override val filtreringEndepunkt = "/v1/forespoersler"
    override val enkeltEntitetEndepunkt = "/v1/forespoersel"
    override val utfasetEndepunkt = "/v1/forespoersler"

    override fun mockEntitet(
        id: UUID,
        orgnr: String,
    ): Forespoersel =
        mockForespoersel()
            .copy(
                orgnr = orgnr,
                navReferanseId = id,
            )

    override fun lagFilterRequest(orgnr: String?): ForespoerselRequest = ForespoerselRequest(orgnr = orgnr)

    override fun serialiserFilterRequest(filter: ForespoerselRequest): JsonElement =
        filter.toJson(serializer = ForespoerselRequest.serializer())

    override fun mockHentingAvEntiteter(
        orgnr: String,
        filter: ForespoerselRequest?,
        resultat: List<Forespoersel>,
    ) {
        every {
            repositories.forespoerselRepository.hentForespoersler(
                orgnr,
                filter,
            )
        } returns resultat // TODO: kan muligens erstattes av repository?
    }

    override fun mockHentingAvEnkeltEntitet(
        id: UUID,
        result: Forespoersel,
    ) {
        every { repositories.forespoerselRepository.hentForespoersel(id) } returns result
    }

    override fun lesEntiteterFraRespons(respons: HttpResponse): List<Forespoersel> = runBlocking { respons.body<List<Forespoersel>>() }

    override fun lesEnkeltEntitetFraRespons(respons: HttpResponse): Forespoersel = runBlocking { respons.body<Forespoersel>() }

    override fun hentOrgnrFraEntitet(entitet: Forespoersel): String = entitet.orgnr
}
