package no.nav.helsearbeidsgiver.config

import com.nimbusds.jose.util.DefaultResourceRetriever
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.exposed.ExposedBakgrunnsjobRepository
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.auth.AltinnAuthClient
import no.nav.helsearbeidsgiver.auth.gyldigScope
import no.nav.helsearbeidsgiver.auth.gyldigSystembrukerOgConsumer
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.bakgrunnsjobb.LeaderElectedBakgrunnsjobbService
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.dialogporten.IngenDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerConfig
import no.nav.helsearbeidsgiver.kafka.createKafkaProducerConfig
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.innsending.IngenInnsendingProducer
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingSerializer
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.kafka.sykmelding.SykmeldingTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.pdp.IPdpService
import no.nav.helsearbeidsgiver.pdp.IngenTilgangPdpService
import no.nav.helsearbeidsgiver.pdp.LocalhostPdpService
import no.nav.helsearbeidsgiver.pdp.PdpService
import no.nav.helsearbeidsgiver.pdp.lagPdpClient
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.jetbrains.exposed.sql.Database

data class Repositories(
    val inntektsmeldingRepository: InntektsmeldingRepository,
    val forespoerselRepository: ForespoerselRepository,
    val mottakRepository: MottakRepository,
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
    val sykmeldingRepository: SykmeldingRepository,
)

data class Services(
    val forespoerselService: ForespoerselService,
    val inntektsmeldingService: InntektsmeldingService,
    val innsendingService: InnsendingService,
    val dialogportenService: IDialogportenService,
    val sykmeldingService: SykmeldingService,
)

fun configureRepositories(db: Database): Repositories =
    Repositories(
        inntektsmeldingRepository = InntektsmeldingRepository(db),
        forespoerselRepository = ForespoerselRepository(db),
        mottakRepository = MottakRepository(db),
        bakgrunnsjobbRepository = ExposedBakgrunnsjobRepository(db),
        sykmeldingRepository = SykmeldingRepository(db),
    )

fun configureServices(repositories: Repositories): Services {
    val forespoerselService = ForespoerselService(repositories.forespoerselRepository)
    val inntektsmeldingService = InntektsmeldingService(repositories.inntektsmeldingRepository)
    val sykmeldingService = SykmeldingService(repositories.sykmeldingRepository)

    val innsendingProducer =
        if (isLocal() || isDev()) {
            InnsendingProducer(
                KafkaProducer(
                    createKafkaProducerConfig(producerName = "api-innsending-producer"),
                    StringSerializer(),
                    InnsendingSerializer(),
                ),
            )
        } else {
            IngenInnsendingProducer()
        }

    val bakgrunnsjobbService =
        LeaderElectedBakgrunnsjobbService(
            bakgrunnsjobbRepository = repositories.bakgrunnsjobbRepository,
            createHttpClient(),
        )

    val innsendingService =
        InnsendingService(
            innsendingProducer = innsendingProducer,
            bakgrunnsjobbService = bakgrunnsjobbService,
        )

    bakgrunnsjobbService
        .apply {
            registrer(InnsendingProcessor(innsendingService))
            startAsync(true)
        }

    // val dialogService = if (isDev()) DialogportenService(lagDialogportenClient(authClient)) else IngenDialogportenService()
    val dialogportenService = IngenDialogportenService()

    return Services(forespoerselService, inntektsmeldingService, innsendingService, dialogportenService, sykmeldingService)
}

fun Application.configureKafkaConsumers(
    services: Services,
    repositories: Repositories,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    val inntektsmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("im"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.inntektsmelding.topic"),
            inntektsmeldingKafkaConsumer,
            InntektsmeldingTolker(
                services.inntektsmeldingService,
                repositories.mottakRepository,
            ),
        )
    }

    val forespoerselKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("fsp"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.forespoersel.topic"),
            forespoerselKafkaConsumer,
            ForespoerselTolker(
                repositories.forespoerselRepository,
                repositories.mottakRepository,
                services.dialogportenService,
            ),
        )
    }

    // Ta bare imot sykmeldinger i dev inntil videre
    if (isLocal() || isDev()) {
        val sykmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("sm"))
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                topic = getProperty("kafkaConsumer.sykmelding.topic"),
                consumer = sykmeldingKafkaConsumer,
                meldingTolker = SykmeldingTolker(services.sykmeldingService, unleashFeatureToggles),
            )
        }
    }
}

fun Application.configureAuth() {
    val authClient = AltinnAuthClient()
    val pdpService = configurePdpService(authClient)

    install(Authentication) {
        tokenValidationSupport(
            name = "systembruker-config",
            config =
                TokenSupportConfig(
                    IssuerConfig(
                        name = "maskinporten",
                        discoveryUrl = getProperty("maskinporten.wellknownUrl"),
                        acceptedAudience = listOf(getProperty("maskinporten.eksponert_scopes")),
                        optionalClaims = listOf("aud", "sub"),
                    ),
                ),
            requiredClaims =
                RequiredClaims(
                    issuer = "maskinporten",
                    claimMap = arrayOf("authorization_details", "consumer", "scope"),
                ),
            additionalValidation = {
                it.gyldigScope() && it.gyldigSystembrukerOgConsumer(pdpService)
            },
            resourceRetriever =
                DefaultResourceRetriever(
                    DEFAULT_HTTP_CONNECT_TIMEOUT,
                    DEFAULT_HTTP_READ_TIMEOUT,
                    DEFAULT_HTTP_SIZE_LIMIT,
                ),
        )
    }
}

fun configurePdpService(authClient: AltinnAuthClient): IPdpService =
    when {
        isDev() -> PdpService(lagPdpClient(authClient))
        isLocal() -> LocalhostPdpService()
        else -> IngenTilgangPdpService()
    }

private fun isDev(): Boolean = "dev-gcp".equals(getPropertyOrNull("application.env"), true)

private fun isLocal(): Boolean = "local".equals(getPropertyOrNull("application.env"), true)
