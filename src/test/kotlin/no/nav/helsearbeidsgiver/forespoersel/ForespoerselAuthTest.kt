package no.nav.helsearbeidsgiver.forespoersel

import io.mockk.every
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.utils.mockForespoersel
import java.util.UUID

class ForespoerselAuthTest : HentApiAuthTest<Forespoersel, ForespoerselRequest, Forespoersel>() {
    override val filtreringEndepunkt = "/v1/forespoersler"
    override val enkeltDokumentEndepunkt = "/v1/forespoersel"
    override val utfasetEndepunkt = "/v1/forespoersler"

    override val dokumentSerializer: KSerializer<Forespoersel> = Forespoersel.serializer()
    override val filterSerializer: KSerializer<ForespoerselRequest> = ForespoerselRequest.serializer()

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): Forespoersel =
        mockForespoersel()
            .copy(
                orgnr = orgnr,
                navReferanseId = id,
            )

    override fun lagFilter(orgnr: String): ForespoerselRequest = ForespoerselRequest(orgnr = orgnr)

    override fun mockHentingAvDokumenter(
        orgnr: String,
        resultat: List<Forespoersel>,
    ) {
        every { repositories.forespoerselRepository.hentForespoersler(orgnr = orgnr) } returns resultat
    }

    override fun mockHentingAvDokumenter(
        filter: ForespoerselRequest,
        resultat: List<Forespoersel>,
    ) {
        every { repositories.forespoerselRepository.hentForespoersler(request = filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: Forespoersel,
    ) {
        every { repositories.forespoerselRepository.hentForespoersel(id) } returns resultat
    }

    override fun hentOrgnrFraDokument(dokument: Forespoersel): String = dokument.orgnr
}
