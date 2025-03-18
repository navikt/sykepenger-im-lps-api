package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Naturalytelse
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RedusertLoennIAgp
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RefusjonEndring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.Arbeidsgiver
import no.nav.helsearbeidsgiver.inntektsmelding.Avsender
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingSkjema
import no.nav.helsearbeidsgiver.utils.test.date.oktober
import no.nav.helsearbeidsgiver.utils.test.date.september
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.KafkaProducer
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

private const val FORESPOERSELID = "%%%FORESPOERSELID%%%"
private const val SYKMELDT_FNR = "%%%SYKMELDT%%%"
private const val ORGNUMMER = "%%%ORGNR%%%"

const val DEFAULT_FNR = "16076006028"
const val DEFAULT_ORG = "732812083"

fun Inntektsmelding.tilSkjema(): SkjemaInntektsmelding =
    SkjemaInntektsmelding(this.type.id, this.avsender.tlf, this.agp, this.inntekt, this.refusjon)

fun buildInntektsmelding(
    forespoerselId: String = UUID.randomUUID().toString(),
    sykemeldtFnr: Fnr = Fnr(DEFAULT_FNR),
    orgNr: Orgnr = Orgnr(DEFAULT_ORG),
): Inntektsmelding = jsonMapper.decodeFromString<Inntektsmelding>(buildInntektsmeldingJson(forespoerselId, sykemeldtFnr, orgNr))

fun buildInntektsmeldingJson(
    forespoerselId: String = UUID.randomUUID().toString(),
    sykemeldtFnr: Fnr = Fnr(DEFAULT_FNR),
    orgNr: Orgnr = Orgnr(DEFAULT_ORG),
): String {
    val filePath = "im.json"
    return readJsonFromResources(filePath)
        .replace(FORESPOERSELID, forespoerselId)
        .replace(SYKMELDT_FNR, sykemeldtFnr.verdi)
        .replace(ORGNUMMER, orgNr.verdi)
}

fun buildForespoerselMottattJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "forespoersel.json"
    return readJsonFromResources(filePath).replace(
        FORESPOERSELID,
        forespoerselId,
    )
}

fun buildInntektsmeldingDistribuertJson(forespoerselId: String = UUID.randomUUID().toString()): String {
    val filePath = "inntektsmelding_distribuert.json"
    return readJsonFromResources(filePath).replace(
        FORESPOERSELID,
        forespoerselId,
    )
}

fun readJsonFromResources(fileName: String): String {
    val resource = KafkaProducer::class.java.getResource("/$fileName")
    return File(resource!!.toURI()).readText(Charsets.UTF_8)
}

fun mockSkjemaInntektsmelding(): SkjemaInntektsmelding =
    SkjemaInntektsmelding(
        forespoerselId = UUID.randomUUID(),
        avsenderTlf = randomDigitString(8),
        agp = mockArbeidsgiverperiode(),
        inntekt = mockInntekt(),
        refusjon = mockRefusjon(),
    )

fun mockInntektsmeldingSkjema(): InntektsmeldingSkjema =
    InntektsmeldingSkjema(
        navReferanseId = UUID.randomUUID(),
        agp = mockArbeidsgiverperiode(),
        inntekt = mockInntekt(),
        refusjon = mockRefusjon(),
        sykmeldtFnr = Fnr.genererGyldig().toString(),
        arbeidsgiver = Arbeidsgiver(DEFAULT_ORG, "22222222"),
        avsender = Avsender("Tigersys", "3.0"),
    )

private fun randomDigitString(length: Int): String =
    List(length) { Random.nextInt(10) }
        .joinToString(separator = "")

private fun mockArbeidsgiverperiode(): Arbeidsgiverperiode =
    Arbeidsgiverperiode(
        perioder =
            listOf(
                5.oktober til 15.oktober,
                20.oktober til 22.oktober,
            ),
        egenmeldinger =
            listOf(
                28.september til 28.september,
                30.september til 30.september,
            ),
        redusertLoennIAgp =
            RedusertLoennIAgp(
                beloep = 300.3,
                begrunnelse = RedusertLoennIAgp.Begrunnelse.FerieEllerAvspasering,
            ),
    )

private infix fun LocalDate.til(tom: LocalDate): Periode =
    Periode(
        fom = this,
        tom = tom,
    )

private fun mockInntekt(): Inntekt =
    Inntekt(
        beloep = 544.6,
        inntektsdato = 28.september,
        naturalytelser =
            listOf(
                Naturalytelse(
                    naturalytelse = Naturalytelse.Kode.BEDRIFTSBARNEHAGEPLASS,
                    verdiBeloep = 52.5,
                    sluttdato = 10.oktober,
                ),
                Naturalytelse(
                    naturalytelse = Naturalytelse.Kode.BIL,
                    verdiBeloep = 434.0,
                    sluttdato = 12.oktober,
                ),
            ),
        endringAarsak =
            NyStillingsprosent(
                gjelderFra = 16.oktober,
            ),
        endringAarsaker =
            listOf(
                NyStillingsprosent(
                    gjelderFra = 16.oktober,
                ),
            ),
    )

private fun mockRefusjon(): Refusjon =
    Refusjon(
        beloepPerMaaned = 150.2,
        endringer =
            listOf(
                RefusjonEndring(
                    beloep = 140.9,
                    startdato = 23.oktober,
                ),
                RefusjonEndring(
                    beloep = 130.8,
                    startdato = 25.oktober,
                ),
                RefusjonEndring(
                    beloep = 120.7,
                    startdato = 27.oktober,
                ),
            ),
        sluttdato = 31.oktober,
    )
