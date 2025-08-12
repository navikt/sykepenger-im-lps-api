package no.nav.helsearbeidsgiver.authorization

import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.utils.gyldigSystembrukerAuthToken
import no.nav.helsearbeidsgiver.utils.ugyldigTokenManglerSystembruker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID

/**
 * Generisk testklasse som verifiserer autorisasjonslogikken for API-endepunkter som henter sykmeldinger, sykepengesøknader, forespørsler og inntektsmeldinger.
 *
 * @param <Entitet> Entitetstypen som returneres i API-responsen, f.eks. Sykmelding.
 * @param <FilterRequest> Requesten som brukes for å hente og filtrere flere entiteter, f.eks. SykmeldingFilterRequest.
 * @param <EntitetDTO> DTO-typen for entiteten som brukes internt i testen, f.eks. SykmeldingDTO.
 */
abstract class HentEntitetApiAuthTest<Entitet, FilterRequest, EntitetDTO> : ApiTest() {
    @BeforeEach
    fun setup() {
        mockPdpTilganger()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    abstract val enkeltEntitetEndepunkt: String
    abstract val filtreringEndepunkt: String
    abstract val utfasetEndepunkt: String

    abstract fun lagFilterRequest(orgnr: String?): FilterRequest

    abstract fun serialiserFilterRequest(filter: FilterRequest): JsonElement

    abstract fun mockEntitet(
        id: UUID,
        orgnr: String,
    ): EntitetDTO

    abstract fun mockHentingAvEnkeltEntitet(
        id: UUID,
        resultat: EntitetDTO,
    )

    abstract fun mockHentingAvEntiteter(
        orgnr: String,
        filter: FilterRequest?,
        resultat: List<EntitetDTO>,
    )

    abstract fun lesEnkeltEntitetFraRespons(respons: io.ktor.client.statement.HttpResponse): Entitet

    abstract fun lesEntiteterFraRespons(respons: io.ktor.client.statement.HttpResponse): List<Entitet>

    abstract fun hentOrgnrFraEntitet(entitet: Entitet): String

    @Test
    fun `gir 200 OK ved henting av entiteter fra utfaset endepunkt`() {
        val mockEntitet = mockEntitet(id = UUID.randomUUID(), orgnr = underenhetOrgnrMedPdpTilgang)

        mockHentingAvEntiteter(
            orgnr = underenhetOrgnrMedPdpTilgang,
            filter = null,
            resultat = listOf(mockEntitet),
        )

        runBlocking {
            val respons =
                client.get(utfasetEndepunkt) {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK

            val entitetSvar = lesEntiteterFraRespons(respons)
            entitetSvar.size shouldBe 1

            val foersteSvarElement = entitetSvar[0]
            assertNotNull(foersteSvarElement)
            hentOrgnrFraEntitet(foersteSvarElement) shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av en spesifikk entitet`() {
        val entitetId = UUID.randomUUID()
        val mockEntitet = mockEntitet(id = entitetId, orgnr = underenhetOrgnrMedPdpTilgang)

        mockHentingAvEnkeltEntitet(entitetId, mockEntitet)

        runBlocking {
            val respons =
                client.get(urlString = "$enkeltEntitetEndepunkt/$entitetId") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK
            val entitet = lesEnkeltEntitetFraRespons(respons)
            hentOrgnrFraEntitet(entitet) shouldBe underenhetOrgnrMedPdpTilgang
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere entiteter på underenhetorgnr hentet fra request`() {
        val antallForventedeEntiteter = 3
        val filter = lagFilterRequest(underenhetOrgnrMedPdpTilgang)

        mockHentingAvEntiteter(
            orgnr = underenhetOrgnrMedPdpTilgang,
            filter = filter,
            resultat =
                List(antallForventedeEntiteter) {
                    mockEntitet(UUID.randomUUID(), underenhetOrgnrMedPdpTilgang)
                },
        )

        runBlocking {
            val respons =
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(filter))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK

            val entiteter = lesEntiteterFraRespons(respons)
            entiteter.size shouldBe antallForventedeEntiteter

            entiteter.forEach {
                assertNotNull(it)
                hentOrgnrFraEntitet(it) shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 200 OK ved henting av flere entiteter på underenhetorgnr hentet fra systembruker`() {
        val antallForventedeEntiteter = 3
        val requestUtenOrgnr = lagFilterRequest(null)

        mockHentingAvEntiteter(
            orgnr = underenhetOrgnrMedPdpTilgang,
            filter = requestUtenOrgnr,
            resultat =
                List(antallForventedeEntiteter) {
                    mockEntitet(UUID.randomUUID(), underenhetOrgnrMedPdpTilgang)
                },
        )

        runBlocking {
            val respons =
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(requestUtenOrgnr))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(underenhetOrgnrMedPdpTilgang))
                }

            respons.status shouldBe HttpStatusCode.OK

            val entiteter = lesEntiteterFraRespons(respons)
            entiteter.size shouldBe antallForventedeEntiteter

            entiteter.forEach {
                assertNotNull(it)
                hentOrgnrFraEntitet(it) shouldBe underenhetOrgnrMedPdpTilgang
            }
        }
    }

    @Test
    fun `gir 401 Unauthorized når token mangler ved henting av entiteter`() {
        val respons1 = runBlocking { client.get(filtreringEndepunkt) }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        val respons2 = runBlocking { client.get("$enkeltEntitetEndepunkt/${UUID.randomUUID()}") }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        val requestBody = lagFilterRequest(underenhetOrgnrMedPdpTilgang)
        val respons3 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(requestBody))
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når systembruker mangler i token ved henting av entiteter`() {
        val respons1 =
            runBlocking {
                client.get(filtreringEndepunkt) {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        val respons2 =
            runBlocking {
                client.get("$enkeltEntitetEndepunkt/${UUID.randomUUID()}") {
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        val respons3 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(lagFilterRequest(underenhetOrgnrMedPdpTilgang)))
                    bearerAuth(mockOAuth2Server.ugyldigTokenManglerSystembruker())
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren fra utfaset endepunkt`() {
        val mockEntitet = mockEntitet(id = UUID.randomUUID(), orgnr = underenhetOrgnrMedPdpTilgang)
        mockHentingAvEntiteter(underenhetOrgnrMedPdpTilgang, null, listOf(mockEntitet))

        val respons =
            runBlocking {
                client.get(utfasetEndepunkt) {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av en spesifikk entitet`() {
        val entitetIdTilgang = UUID.randomUUID()
        val entitetIdIkkeTilgang = UUID.randomUUID()

        mockHentingAvEnkeltEntitet(
            id = entitetIdTilgang,
            resultat = mockEntitet(entitetIdTilgang, underenhetOrgnrMedPdpTilgang),
        )
        mockHentingAvEnkeltEntitet(
            id = entitetIdIkkeTilgang,
            resultat = mockEntitet(entitetIdTilgang, orgnrUtenPdpTilgang),
        )

        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra entitet).
        // Det vil si at man forsøker å hente en entitet som systembrukeren ikke skal ha tilgang til.
        val respons1 =
            runBlocking {
                client.get("$enkeltEntitetEndepunkt/$entitetIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetorgnr (fra entitet).
        val respons2 =
            runBlocking {
                client.get("$enkeltEntitetEndepunkt/$entitetIdTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra entitet.
        val respons3 =
            runBlocking {
                client.get("$enkeltEntitetEndepunkt/$entitetIdIkkeTilgang") {
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `gir 401 Unauthorized når pdp nekter tilgang for systembrukeren for henting av flere entiteter`() {
        // Systembruker _har_ tilgang til hovedenhetorgnr (fra token), men har _ikke_ tilgang til underenhetorgnr (fra requesten).
        // Det vil si at man forsøker å hente entiteter for en organisasjon som systembrukeren ikke skal ha tilgang til.
        val respons1 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(lagFilterRequest(orgnr = orgnrUtenPdpTilgang)))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(hovedenhetOrgnrMedPdpTilgang))
                }
            }
        respons1.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, men _har_ tilgang til underenhetsorgnr i requesten.
        // Det vil si at man forsøker å hente entiteter for et orgnummer (fra requesten), men blir nektet tilgang fra pdp pga. orgnummeret i tokenet.
        val respons2 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(lagFilterRequest(orgnr = underenhetOrgnrMedPdpTilgang)))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons2.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har _ikke_ tilgang til orgnr i token, og har heller ikke angitt orgnr i requesten .
        // Det vil si at man forsøker å hente entiteter for et orgnummer (fra tokenet) som pdp nekter systembrukeren tilgang til.
        val respons3 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(lagFilterRequest(orgnr = null)))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons3.status shouldBe HttpStatusCode.Unauthorized

        // Systembruker har hverken tilgang til orgnr i token eller orgnr fra requesten.
        val respons4 =
            runBlocking {
                client.post(filtreringEndepunkt) {
                    contentType(ContentType.Application.Json)
                    setBody(serialiserFilterRequest(lagFilterRequest(orgnr = orgnrUtenPdpTilgang)))
                    bearerAuth(mockOAuth2Server.gyldigSystembrukerAuthToken(orgnrUtenPdpTilgang))
                }
            }
        respons4.status shouldBe HttpStatusCode.Unauthorized
    }
}
