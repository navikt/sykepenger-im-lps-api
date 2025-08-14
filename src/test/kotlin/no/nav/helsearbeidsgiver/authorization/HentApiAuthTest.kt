package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID

/**
 * Generisk testklasse som verifiserer autorisasjonslogikken for API-endepunkter som henter sykmeldinger, sykepengesøknader, forespørsler og inntektsmeldinger.
 *
 * @param <Dokument> Dokumenttypen som returneres i API-responsen, f.eks. Sykmelding.
 * @param <Filter> Filteret som brukes for å hente og filtrere dokumenter, f.eks. SykepengesoeknadFilter.
 * @param <DokumentDTO> DTO-typen for dokumentet som brukes internt i testen, f.eks. SykmeldingDTO.
 */
abstract class HentApiAuthTest<Dokument, Filter, DokumentDTO> : ApiTest() {
    @BeforeEach
    fun setup() {
        mockPdpTilganger()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    abstract val enkeltDokumentEndepunkt: String
    abstract val filtreringEndepunkt: String
    abstract val utfasetEndepunkt: String

    abstract val dokumentSerializer: KSerializer<Dokument>
    abstract val filterSerializer: KSerializer<Filter>

    abstract fun lagFilter(orgnr: String): Filter

    abstract fun mockDokument(
        id: UUID,
        orgnr: String,
    ): DokumentDTO

    abstract fun mockHentingAvEnkeltDokument(
        id: UUID,
        resultat: DokumentDTO,
    )

    @Deprecated(
        message =
            "Kan slettes når vi fjerner de utfasede endepunktene " +
                "GET v1/sykmeldinger, GET v1/forespoersler, GET v1/sykepengesoeknader og GET v1/inntektsmeldinger ." +
                "Bruk mockHentingAvDokumenter(orgnr: String, filter: Filter, resultat: List<DokumentDTO>) istedenfor.",
    )
    abstract fun mockHentingAvDokumenter(
        orgnr: String,
        resultat: List<DokumentDTO>,
    )

    abstract fun mockHentingAvDokumenter(
        filter: Filter,
        resultat: List<DokumentDTO>,
    )

    abstract fun hentOrgnrFraDokument(dokument: Dokument): String

    @Test
    fun `gir 200 OK ved henting av dokumenter fra utfaset endepunkt`() {
        val mockDokument = mockDokument(id = UUID.randomUUID(), orgnr = underenhetOrgnrMedPdpTilgang)

        mockHentingAvDokumenter(
            orgnr = underenhetOrgnrMedPdpTilgang,
            resultat = listOf(mockDokument),
        )

        runBlocking {
            val respons =
                client.get(utfasetEndepunkt) {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK

            val dokumentSvar = respons.bodyAsText().fromJson(ListSerializer(dokumentSerializer))
            dokumentSvar.size shouldBe 1

            val foersteSvarElement = dokumentSvar[0]
            assertNotNull(foersteSvarElement)
            hentOrgnrFraDokument(foersteSvarElement) shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk dokument`() {
        val dokumentId = UUID.randomUUID()
        val mockDokument = mockDokument(id = dokumentId, orgnr = underenhetOrgnrMedPdpTilgang)

        mockHentingAvEnkeltDokument(dokumentId, mockDokument)

        runBlocking {
            val respons =
                client.get(urlString = "$enkeltDokumentEndepunkt/$dokumentId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK
            val dokument = respons.bodyAsText().fromJson(dokumentSerializer)
            hentOrgnrFraDokument(dokument) shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere dokumenter på underenhetorgnr hentet fra request`() {
        val antallForventedeDokumenter = 3
        val filter = lagFilter(underenhetOrgnrMedPdpTilgang)

        mockHentingAvDokumenter(
            filter = filter,
            resultat =
                List(antallForventedeDokumenter) {
                    mockDokument(UUID.randomUUID(), underenhetOrgnrMedPdpTilgang)
                },
        )

        runBlocking {
            val respons =
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(filter.toJson(filterSerializer))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK

            val dokumenter = respons.bodyAsText().fromJson(ListSerializer(dokumentSerializer))
            dokumenter.size shouldBe antallForventedeDokumenter

            dokumenter.forEach {
                assertNotNull(it)
                hentOrgnrFraDokument(it) shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av dokumenter`() {
        val respons1 = runBlocking { client.get(filtreringEndepunkt) }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        val respons2 = runBlocking { client.get("$enkeltDokumentEndepunkt/${UUID.randomUUID()}") }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = lagFilter(underenhetOrgnrMedPdpTilgang)
        val respons3 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toJson(filterSerializer))
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved henting av dokumenter`() {
        val respons1 =
            runBlocking {
                client.get(filtreringEndepunkt) {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        val respons2 =
            runBlocking {
                client.get("$enkeltDokumentEndepunkt/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        val respons3 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(lagFilter(underenhetOrgnrMedPdpTilgang).toJson(filterSerializer))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren fra utfaset endepunkt`() {
        val mockDokument = mockDokument(id = UUID.randomUUID(), orgnr = underenhetOrgnrMedPdpTilgang)
        mockHentingAvDokumenter(underenhetOrgnrMedPdpTilgang, listOf(mockDokument))

        val respons =
            runBlocking {
                client.get(utfasetEndepunkt) {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av en spesifikk dokument`() {
        val dokumentIdTilgang = UUID.randomUUID()
        val dokumentIdIkkeTilgang = UUID.randomUUID()

        mockHentingAvEnkeltDokument(
            id = dokumentIdTilgang,
            resultat = mockDokument(dokumentIdTilgang, underenhetOrgnrMedPdpTilgang),
        )
        mockHentingAvEnkeltDokument(
            id = dokumentIdIkkeTilgang,
            resultat = mockDokument(dokumentIdTilgang, orgnrUtenPdpTilgang),
        )

        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra dokument).
        // Det vil si at man forsøker å hente en dokument som systembrukeren ikke skal ha tilgang til.
        val respons1 =
            runBlocking {
                client.get("$enkeltDokumentEndepunkt/$dokumentIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetorgnr (fra dokument).
        val respons2 =
            runBlocking {
                client.get("$enkeltDokumentEndepunkt/$dokumentIdTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra dokument.
        val respons3 =
            runBlocking {
                client.get("$enkeltDokumentEndepunkt/$dokumentIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av flere dokumenter`() {
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra requesten).
        // Det vil si at man forsøker å hente dokumenter for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val respons1 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(lagFilter(orgnr = orgnrUtenPdpTilgang).toJson(filterSerializer))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i requesten.
        // Det vil si at man forsøker å hente dokumenter for et orgnummer (fra requesten), men blir nektet tilgang fra pdp pga. orgnummeret i tokenet.
        val respons2 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(lagFilter(orgnr = underenhetOrgnrMedPdpTilgang).toJson(filterSerializer))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra requesten.
        val respons4 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(lagFilter(orgnr = orgnrUtenPdpTilgang).toJson(filterSerializer))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons4.status shouldBe HttpStatusCode.Unauthorized
    }
}
