package no.nav.helsearbeidsgiver.forespoersel

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.authorization.HentEntitetApiAuthTest
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import java.util.UUID

class ForespoerselAuthTest : HentEntitetApiAuthTest<Forespoersel, ForespoerselRequest, Forespoersel>() {
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
        resultat: List<Forespoersel>,
    ) {
        every { repositories.forespoerselRepository.hentForespoersler(orgnr) } returns resultat
    }

    override fun mockHentingAvEntiteter(
        orgnr: String,
        filter: ForespoerselRequest,
        resultat: List<Forespoersel>,
    ) {
        every { repositories.forespoerselRepository.hentForespoersler(orgnr, filter) } returns resultat
    }

    override fun mockHentingAvEnkeltEntitet(
        id: UUID,
        resultat: Forespoersel,
    ) {
        every { repositories.forespoerselRepository.hentForespoersel(id) } returns resultat
    }

    override fun lesEntiteterFraRespons(respons: HttpResponse): List<Forespoersel> = runBlocking { respons.body<List<Forespoersel>>() }

    override fun lesEnkeltEntitetFraRespons(respons: HttpResponse): Forespoersel = runBlocking { respons.body<Forespoersel>() }

    override fun hentOrgnrFraEntitet(entitet: Forespoersel): String = entitet.orgnr
}
