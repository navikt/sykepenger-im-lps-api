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
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingProducer
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.felles.auth.DefaultAuthClient
import no.nav.helsearbeidsgiver.felles.auth.NoOpAuthClient
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.helsesjekker.HelseSjekkService
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.inntektsmelding.AvvistInntektsmeldingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerMultiPollerConfig
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerSinglePollerConfig
import no.nav.helsearbeidsgiver.kafka.createKafkaProducerConfig
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingProducer
import no.nav.helsearbeidsgiver.kafka.innsending.InnsendingSerializer
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.AvvistInntektsmeldingTolker
import no.nav.helsearbeidsgiver.kafka.inntektsmelding.InntektsmeldingTolker
import no.nav.helsearbeidsgiver.kafka.sis.StatusISpeilTolker
import no.nav.helsearbeidsgiver.kafka.soeknad.SoeknadTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.kafka.sykmelding.SykmeldingTolker
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.pdl.PdlService
import no.nav.helsearbeidsgiver.pdp.IPdpService
import no.nav.helsearbeidsgiver.pdp.LocalhostPdpService
import no.nav.helsearbeidsgiver.pdp.PdpService
import no.nav.helsearbeidsgiver.sis.StatusISpeilRepository
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.soeknad.SoeknadService
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.utils.LeaderConfig
import no.nav.helsearbeidsgiver.utils.NaisLeaderConfig
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
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

val MAX_ANTALL_I_RESPONS = 1000 // Max antall entiteter som kan returneres i API-kall

data class Repositories(
    val inntektsmeldingRepository: InntektsmeldingRepository,
    val forespoerselRepository: ForespoerselRepository,
    val mottakRepository: MottakRepository,
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
    val sykmeldingRepository: SykmeldingRepository,
    val soeknadRepository: SoeknadRepository,
    val statusISpeilRepository: StatusISpeilRepository,
)

data class Services(
    val forespoerselService: ForespoerselService,
    val inntektsmeldingService: InntektsmeldingService,
    val innsendingService: InnsendingService,
    val dialogportenService: DialogportenService,
    val dokumentkoblingService: DokumentkoblingService,
    val sykmeldingService: SykmeldingService,
    val pdlService: PdlService,
    val soeknadService: SoeknadService,
    val helseSjekkService: HelseSjekkService,
    val avvistInntektsmeldingService: AvvistInntektsmeldingService,
)

data class Tolkere(
    val inntektsmeldingTolker: InntektsmeldingTolker,
    val forespoerselTolker: ForespoerselTolker,
    val sykmeldingTolker: SykmeldingTolker,
    val soeknadTolker: SoeknadTolker,
    val statusISpeilTolker: StatusISpeilTolker,
    val avvistInntektsmeldingTolker: AvvistInntektsmeldingTolker,
)

fun configureTolkere(
    services: Services,
    repositories: Repositories,
): Tolkere {
    val inntektsmeldingTolker =
        InntektsmeldingTolker(
            inntektsmeldingService = services.inntektsmeldingService,
            mottakRepository = repositories.mottakRepository,
            dialogportenService = services.dialogportenService,
        )
    val forespoerselTolker =
        ForespoerselTolker(
            mottakRepository = repositories.mottakRepository,
            dialogportenService = services.dialogportenService,
            forespoerselService = services.forespoerselService,
            dokumentkoblingService = services.dokumentkoblingService,
        )
    val sykmeldingTolker =
        SykmeldingTolker(
            sykmeldingService = services.sykmeldingService,
            dialogportenService = services.dialogportenService,
            dokumentkoblingService = services.dokumentkoblingService,
            pdlService = services.pdlService,
        )
    val soeknadTolker = SoeknadTolker(services.soeknadService)

    val statusISpeilTolker =
        StatusISpeilTolker(
            repositories.soeknadRepository,
            repositories.statusISpeilRepository,
            services.dokumentkoblingService,
        )

    val avvistInntektsmeldingTolker =
        AvvistInntektsmeldingTolker(
            avvistInntektsmeldingService = services.avvistInntektsmeldingService,
        )

    return Tolkere(
        inntektsmeldingTolker,
        forespoerselTolker,
        sykmeldingTolker,
        soeknadTolker,
        statusISpeilTolker,
        avvistInntektsmeldingTolker,
    )
}

fun configureRepositories(db: Database): Repositories =
    Repositories(
        inntektsmeldingRepository = InntektsmeldingRepository(db),
        forespoerselRepository = ForespoerselRepository(db),
        mottakRepository = MottakRepository(db),
        bakgrunnsjobbRepository = ExposedBakgrunnsjobRepository(db),
        sykmeldingRepository = SykmeldingRepository(db),
        soeknadRepository = SoeknadRepository(db),
        statusISpeilRepository = StatusISpeilRepository(db),
    )

fun configureServices(
    repositories: Repositories,
    unleashFeatureToggles: UnleashFeatureToggles,
    database: Database,
    pdlService: PdlService,
    leaderConfig: LeaderConfig = NaisLeaderConfig,
): Services {
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
            leaderConfig,
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
            soeknadRepository = repositories.soeknadRepository,
            unleashFeatureToggles = unleashFeatureToggles,
            inntektsmeldingRepository = repositories.inntektsmeldingRepository,
            forespoerselRepository = repositories.forespoerselRepository,
        )

    val dokumentkoblingProducer =
        DokumentkoblingProducer(
            KafkaProducer(
                createKafkaProducerConfig(producerName = "dokumentkobling-producer"),
                StringSerializer(),
                DialogSerializer(),
            ),
        )

    val dokumentkoblingService =
        DokumentkoblingService(
            dokumentkoblingProducer = dokumentkoblingProducer,
            unleashFeatureToggles = unleashFeatureToggles,
            forespoerselRepository = repositories.forespoerselRepository,
        )

    val soeknadService = SoeknadService(repositories.soeknadRepository, dialogportenService, dokumentkoblingService)
    val helseSjekkService = HelseSjekkService(db = database)
    val avvistInntektsmeldingService =
        AvvistInntektsmeldingService(repositories.inntektsmeldingRepository, dialogportenService)
    val forespoerselService = ForespoerselService(repositories.forespoerselRepository, dialogportenService, dokumentkoblingService)

    return Services(
        forespoerselService,
        inntektsmeldingService,
        innsendingService,
        dialogportenService,
        dokumentkoblingService,
        sykmeldingService,
        pdlService,
        soeknadService,
        helseSjekkService,
        avvistInntektsmeldingService,
    )
}

fun configureUnleashFeatureToggles(): UnleashFeatureToggles = UnleashFeatureToggles(isLocal())

fun Application.configureKafkaConsumers(
    tolkere: Tolkere,
    unleashFeatureToggles: UnleashFeatureToggles,
    leaderConfig: LeaderConfig,
) {
    val inntektsmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerSinglePollerConfig("im"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.inntektsmelding.topic"),
            inntektsmeldingKafkaConsumer,
            tolkere.inntektsmeldingTolker,
            enabled = unleashFeatureToggles::skalKonsumereInntektsmeldinger,
            isLeader = leaderConfig::isElectedLeader,
        )
    }

    val forespoerselKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerSinglePollerConfig("fsp"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            getProperty("kafkaConsumer.forespoersel.topic"),
            forespoerselKafkaConsumer,
            tolkere.forespoerselTolker,
            unleashFeatureToggles::skalKonsumereForespoersler,
            isLeader = leaderConfig::isElectedLeader,
        )
    }

    val sykmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerSinglePollerConfig("sm"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            topic = getProperty("kafkaConsumer.sykmelding.topic"),
            consumer = sykmeldingKafkaConsumer,
            meldingTolker = tolkere.sykmeldingTolker,
            enabled = unleashFeatureToggles::skalKonsumereSykmeldinger,
            isLeader = leaderConfig::isElectedLeader,
        )
    }

    val soeknadKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerSinglePollerConfig("so"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            topic = getProperty("kafkaConsumer.soeknad.topic"),
            consumer = soeknadKafkaConsumer,
            meldingTolker = tolkere.soeknadTolker,
            enabled = unleashFeatureToggles::skalKonsumereSykepengesoeknader,
            isLeader = leaderConfig::isElectedLeader,
        )
    }

    val statusISpeilKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerMultiPollerConfig("sis"))
    launch(Dispatchers.Default) {
        startKafkaConsumer(
            topic = getProperty("kafkaConsumer.sis.topic"),
            consumer = statusISpeilKafkaConsumer,
            meldingTolker = tolkere.statusISpeilTolker,
            enabled = unleashFeatureToggles::skalKonsumereStatusISpeil,
            isLeader = leaderConfig::isElectedLeader,
        )
    }

    if (unleashFeatureToggles.skalKonsumereAvvisteInntektsmeldinger()) {
        val avvistInntektsmeldingKafkaConsumer =
            KafkaConsumer<String, String>(createKafkaConsumerSinglePollerConfig("im-avvist"))
        launch(Dispatchers.Default) {
            startKafkaConsumer(
                topic = getProperty("kafkaConsumer.innsending.topic"),
                consumer = avvistInntektsmeldingKafkaConsumer,
                meldingTolker = tolkere.avvistInntektsmeldingTolker,
                isLeader = leaderConfig::isElectedLeader,
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
    }
}

fun getPdpService(): IPdpService =
    when {
        isLocal() -> LocalhostPdpService
        else -> PdpService
    }

fun configureAuthClient() = if (isLocal()) NoOpAuthClient() else DefaultAuthClient()

private fun isLocal(): Boolean = "local".equals(getPropertyOrNull("application.env"), true)
