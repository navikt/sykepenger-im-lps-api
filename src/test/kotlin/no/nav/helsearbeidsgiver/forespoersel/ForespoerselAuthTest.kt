package no.nav.helsearbeidsgiver.forespoersel

import io.mockk.every
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import java.util.UUID

class ForespoerselAuthTest : HentApiAuthTest<ForespoerselResponse, ForespoerselFilter, Forespoersel>() {
    override val filtreringEndepunkt = "/v1/forespoersler"
    override val enkeltDokumentEndepunkt = "/v1/forespoersel"
    override val utfasetEndepunkt = "/v1/forespoersler"

    override val dokumentSerializer: KSerializer<ForespoerselResponse> = ForespoerselResponse.serializer()
    override val filterSerializer: KSerializer<ForespoerselFilter> = ForespoerselFilter.serializer()

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): Forespoersel =
        mockForespoersel()
            .copy(
                orgnr = orgnr,
                navReferanseId = id,
            )

    override fun lagFilter(orgnr: String): ForespoerselFilter = ForespoerselFilter(orgnr = orgnr)

    override fun mockHentingAvDokumenter(
        filter: ForespoerselFilter,
        resultat: List<Forespoersel>,
    ) {
        every { repositories.forespoerselRepository.hentForespoersler(filter = filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: Forespoersel,
    ) {
        every { repositories.forespoerselRepository.hentForespoersel(id) } returns resultat
    }

    override fun hentOrgnrFraDokument(dokument: ForespoerselResponse): String = dokument.orgnr
}
