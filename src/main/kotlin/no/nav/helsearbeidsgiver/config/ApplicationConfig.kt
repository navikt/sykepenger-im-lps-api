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
import no.nav.helsearbeidsgiver.Env.getPropertyAsList
import no.nav.helsearbeidsgiver.Env.getPropertyOrNull
import no.nav.helsearbeidsgiver.auth.gyldigScope
import no.nav.helsearbeidsgiver.auth.gyldigSystembrukerOgConsumer
import no.nav.helsearbeidsgiver.bakgrunnsjobb.InnsendingProcessor
import no.nav.helsearbeidsgiver.bakgrunnsjobb.LeaderElectedBakgrunnsjobbService
import no.nav.helsearbeidsgiver.dialogporten.DialogProducer
import no.nav.helsearbeidsgiver.dialogporten.DialogSerializer
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.DefaultAuthClient
import no.nav.helsearbeidsgiver.felles.auth.NoOpAuthClient
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerConfig
import no.nav.helsearbeidsgiver.kafka.createKafkaProducerConfig
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingSerializer
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.kafka.soknad.SoknadTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.kafka.sykmelding.SykmeldingTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.pdl.IPdlService
import no.nav.helsearbeidsgiver.pdl.IngenPdlService
import no.nav.helsearbeidsgiver.pdl.PdlService
import no.nav.helsearbeidsgiver.pdp.IPdpService
import no.nav.helsearbeidsgiver.pdp.IngenTilgangPdpService
import no.nav.helsearbeidsgiver.pdp.LocalhostPdpService
import no.nav.helsearbeidsgiver.pdp.PdpService
import no.nav.helsearbeidsgiver.pdp.lagPdpClient
import no.nav.helsearbeidsgiver.soknad.SoknadRepository
import no.nav.helsearbeidsgiver.soknad.SoknadService
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_CONNECT_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_READ_TIMEOUT
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever.Companion.DEFAULT_HTTP_SIZE_LIMIT
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport
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
    val soknadRepository: SoknadRepository,
)

data class Services(
    val forespoerselService: ForespoerselService,
    val inntektsmeldingService: InntektsmeldingService,
    val innsendingService: InnsendingService,
    val dialogportenService: DialogportenService,
    val sykmeldingService: SykmeldingService,
    val pdlService: IPdlService,
    val soknadService: SoknadService,
    val pdpService: IPdpService,
)

data class Tolkere(
    val inntektsmeldingTolker: InntektsmeldingTolker,
    val forespoerselTolker: ForespoerselTolker,
    val sykmeldingTolker: SykmeldingTolker,
    val soknadTolker: SoknadTolker,
)

fun configureTolkere(
    services: Services,
    repositories: Repositories,
): Tolkere {
    val inntektsmeldingTolker =
        InntektsmeldingTolker(
            inntektsmeldingService = services.inntektsmeldingService,
            mottakRepository = repositories.mottakRepository,
        )
    val forespoerselTolker =
        ForespoerselTolker(
            forespoerselRepository = repositories.forespoerselRepository,
            mottakRepository = repositories.mottakRepository,
        )
    val sykmeldingTolker =
        SykmeldingTolker(
            sykmeldingService = services.sykmeldingService,
            dialogportenService = services.dialogportenService,
            pdlService = services.pdlService,
        )
    val soknadTolker =
        SoknadTolker(services.soknadService)

    return Tolkere(inntektsmeldingTolker, forespoerselTolker, sykmeldingTolker, soknadTolker)
}

fun configureRepositories(db: Database): Repositories =
    Repositories(
        inntektsmeldingRepository = InntektsmeldingRepository(db),
        forespoerselRepository = ForespoerselRepository(db),
        mottakRepository = MottakRepository(db),
        bakgrunnsjobbRepository = ExposedBakgrunnsjobRepository(db),
        sykmeldingRepository = SykmeldingRepository(db),
        soknadRepository = SoknadRepository(db),
    )

fun configureServices(
    repositories: Repositories,
    authClient: AuthClient,
    unleashFeatureToggles: UnleashFeatureToggles,
): Services {
    val forespoerselService = ForespoerselService(repositories.forespoerselRepository)
    val inntektsmeldingService = InntektsmeldingService(repositories.inntektsmeldingRepository)
    val sykmeldingService = SykmeldingService(repositories.sykmeldingRepository)

    val innsendingProducer =
        InnsendingProducer(
            KafkaProducer(
                createKafkaProducerConfig(producerName = "api-innsending-producer"),
                StringSerializer(),
                InnsendingSerializer(),
            ),
        )

    val bakgrunnsjobbService =
        LeaderElectedBakgrunnsjobbService(
            bakgrunnsjobbRepository = repositories.bakgrunnsjobbRepository,
            createHttpClient(),
        )

    val innsendingService =
        InnsendingService(
            innsendingProducer = innsendingProducer,
            bakgrunnsjobbService = bakgrunnsjobbService,
            unleashFeatureToggles = unleashFeatureToggles,
        )

    bakgrunnsjobbService
        .apply {
            registrer(InnsendingProcessor(innsendingService))
            startAsync(true)
        }

    val dialogProducer =
        DialogProducer(
            KafkaProducer(
                createKafkaProducerConfig(producerName = "dialog-producer"),
                StringSerializer(),
                DialogSerializer(),
            ),
        )
    val dialogportenService =
        DialogportenService(
            dialogProducer = dialogProducer,
            unleashFeatureToggles = unleashFeatureToggles,
        )

    val pdlService = if (isDev()) PdlService(authClient) else IngenPdlService()
    val soknadService = SoknadService(repositories.soknadRepository)
    return Services(
        forespoerselService,
        inntektsmeldingService,
        innsendingService,
        dialogportenService,
        sykmeldingService,
        pdlService,
        soknadService,
        configurePdpService(authClient),
    )
}

fun configureUnleashFeatureToggles(): UnleashFeatureToggles = UnleashFeatureToggles(isLocal())

fun Application.configureKafkaConsumers(
    tolkere: Tolkere,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    val inntektsmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("im"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.inntektsmelding.topic"),
            inntektsmeldingKafkaConsumer,
            tolkere.inntektsmeldingTolker,
        )
    }

    val forespoerselKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("fsp"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.forespoersel.topic"),
            forespoerselKafkaConsumer,
            tolkere.forespoerselTolker,
        )
    }

    if (unleashFeatureToggles.skalKonsumereSykmeldinger()) {
        val sykmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("sm"))
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                topic = getProperty("kafkaConsumer.sykmelding.topic"),
                consumer = sykmeldingKafkaConsumer,
                meldingTolker = tolkere.sykmeldingTolker,
            )
        }
    }

    if (unleashFeatureToggles.skalKonsumereSykepengesoknader()) {
        val soknadKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("so"))
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                topic = getProperty("kafkaConsumer.soknad.topic"),
                consumer = soknadKafkaConsumer,
                meldingTolker = tolkere.soknadTolker,
            )
        }
    }
}

fun Application.configureAuth(authClient: AuthClient) {
    install(Authentication) {
        tokenValidationSupport(
            name = "systembruker-config",
            config =
                TokenSupportConfig(
                    IssuerConfig(
                        name = "maskinporten",
                        discoveryUrl = getProperty("maskinporten.wellknownUrl"),
                        acceptedAudience = getPropertyAsList("maskinporten.eksponert_scopes"),
                        optionalClaims = listOf("aud", "sub"),
                    ),
                ),
            requiredClaims =
                RequiredClaims(
                    issuer = "maskinporten",
                    claimMap = arrayOf("authorization_details", "consumer", "scope"),
                ),
            additionalValidation = {
                it.gyldigScope() && it.gyldigSystembrukerOgConsumer()
            },
            resourceRetriever =
                DefaultResourceRetriever(
                    DEFAULT_HTTP_CONNECT_TIMEOUT,
                    DEFAULT_HTTP_READ_TIMEOUT,
                    DEFAULT_HTTP_SIZE_LIMIT,
                ),
        )

        // TODO: Kan registrere en PDP-authenticator her?
    }
}

fun configurePdpService(authClient: AuthClient): IPdpService =
    when {
        isDev() -> PdpService(lagPdpClient(authClient))
        isLocal() -> LocalhostPdpService()
        else -> IngenTilgangPdpService()
    }

fun configureAuthClient() = if (isLocal()) NoOpAuthClient() else DefaultAuthClient()

private fun isDev(): Boolean = "dev-gcp".equals(getPropertyOrNull("application.env"), true)

private fun isLocal(): Boolean = "local".equals(getPropertyOrNull("application.env"), true)
