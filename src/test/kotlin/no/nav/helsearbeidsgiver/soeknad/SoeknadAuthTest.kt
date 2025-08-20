package no.nav.helsearbeidsgiver.soeknad

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import no.nav.helsearbeidsgiver.authorization.HentApiAuthTest
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.utils.TestData.medId
import no.nav.helsearbeidsgiver.utils.TestData.medOrgnr
import no.nav.helsearbeidsgiver.utils.TestData.soeknadMock
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.toJson
import org.junit.jupiter.api.Test
import java.util.UUID

class SoeknadAuthTest : HentApiAuthTest<Sykepengesoeknad, SykepengesoeknadFilter, SykepengesoknadDTO>() {
    override val filtreringEndepunkt = "/v1/sykepengesoeknader"
    override val enkeltDokumentEndepunkt = "/v1/sykepengesoeknad"
    override val utfasetEndepunkt = "/v1/sykepengesoeknader"

    override val dokumentSerializer: KSerializer<Sykepengesoeknad> = Sykepengesoeknad.serializer()
    override val filterSerializer: KSerializer<SykepengesoeknadFilter> = SykepengesoeknadFilter.serializer()

    override fun mockDokument(
        id: UUID,
        orgnr: String,
    ): SykepengesoknadDTO =
        soeknadMock()
            .medId(id)
            .medOrgnr(orgnr)

    override fun lagFilter(orgnr: String): SykepengesoeknadFilter = SykepengesoeknadFilter(orgnr = orgnr)

    override fun mockHentingAvDokumenter(
        filter: SykepengesoeknadFilter,
        resultat: List<SykepengesoknadDTO>,
    ) {
        every { repositories.soeknadRepository.hentSoeknader(filter) } returns resultat
    }

    override fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: SykepengesoknadDTO,
    ) {
        every { repositories.soeknadRepository.hentSoeknad(id) } returns resultat
    }

    override fun hentOrgnrFraDokument(dokument: Sykepengesoeknad): String = dokument.arbeidsgiver.orgnr

    // Tester som er spesifikke for SoeknadAuthTest
    @Test
    fun `gir 404 Not Found ved henting av en spesifikk søknad som ikke skal vises til arbeidsgiver`() {
        val soeknadId = UUID.randomUUID()
        val mockSoeknad =
            soeknadMock()
                .copy(sendtArbeidsgiver = null)
                .medId(soeknadId)
                .medOrgnr(underenhetOrgnrMedPdpTilgang)

        mockHentingAvEnkeltDokument(soeknadId, mockSoeknad)

        runBlocking {
            val respons =
                client.get("$enkeltDokumentEndepunkt/$soeknadId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            respons.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere søknader og returnerer kun de søknadene som skal vises til arbeidsgiver`() {
        val antallForventedeSoeknader = 3

        val soeknaderSomSkalVisesTilArbeidsgiver =
            List(
                antallForventedeSoeknader,
            ) {
                soeknadMock()
                    .medId(UUID.randomUUID())
                    .medOrgnr(underenhetOrgnrMedPdpTilgang)
            }

        val soeknadSomIkkeSkalVisesTilArbeidsgiver =
            soeknadMock()
                .copy(sendtArbeidsgiver = null)
                .medId(UUID.randomUUID())
                .medOrgnr(underenhetOrgnrMedPdpTilgang)

        val filter =
            SykepengesoeknadFilter(
                orgnr = underenhetOrgnrMedPdpTilgang,
            )

        mockHentingAvDokumenter(
            filter = filter,
            resultat = soeknaderSomSkalVisesTilArbeidsgiver + soeknadSomIkkeSkalVisesTilArbeidsgiver,
        )

        runBlocking {
            val respons =
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(filterSerializer))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            respons.status shouldBe HttpStatusCode.OK
            val soeknadSvar = respons.body<List<Sykepengesoeknad>>()
            soeknadSvar.size shouldBe antallForventedeSoeknader
            soeknadSvar.forEach {
                it.arbeidsgiver.orgnr shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }
}
