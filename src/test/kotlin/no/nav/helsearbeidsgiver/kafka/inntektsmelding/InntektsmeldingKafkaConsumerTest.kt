package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.inntektsmelding.ExposedMottak
import org.junit.jupiter.api.Test
import readJsonFromResources
import java.util.UUID
import kotlin.test.assertEquals

class InntektsmeldingKafkaConsumerTest {
    @Test
    fun handleRecord() {
        Database.init()
        val orgnr = "999999999"
        val fnr = "99999999999"
        // Simuler flere samtidige http-klientkall (hent/les) og diverse innkommende kafka-meldinger (opprett/skriv)
        runBlocking {
            val im = readJsonFromResources("im.json")
            val event =
                readJsonFromResources(
                    "inntektsmelding_distribuert.json",
                ).replace("%%%FORESPORSELID%%%", UUID.randomUUID().toString())
            for (i in 1..100) {
                launch {
                    Database.getInntektsmeldingRepo().hent(orgnr)
                    Database.getMottakRepository().opprett(ExposedMottak(event))
                    Database.getForespoerselRepo().hentForespoerslerForOrgnr(orgnr)
                }
                launch {
                    val forespoerselID = lagreInntektsmelding(im, orgnr, fnr)
                    Database.getForespoerselRepo().hentForespoerslerForOrgnr(orgnr)
                    Database.getForespoerselRepo().lagreForespoersel(forespoerselID, orgnr, fnr)
                    Database.getForespoerselRepo().hentForespoerslerForOrgnr(orgnr)
                    Database.getForespoerselRepo().settBesvart(forespoerselID)
                    Database.getInntektsmeldingRepo().hent(orgnr)
                }
                launch {
                    Database.getForespoerselRepo().hentForespoerslerForOrgnr(orgnr)
                    Database.getInntektsmeldingRepo().hent(orgnr)
                }
            }
        }
        assertEquals(100, Database.getForespoerselRepo().hentForespoerslerForOrgnr(orgnr).count())
        assertEquals(100, Database.getInntektsmeldingRepo().hent(orgnr).count())

        // Test at kjente payloads ikke kræsjer:
        val payload = """
            {"@event_name":"FORESPOERSEL_MOTTATT","uuid":"9c87f626-fe80-4618-ad37-f7e6a7026bc7","data":{"forespoerselId":"556d6430-0c43-4dbc-8040-36ba37bfa191","orgnrUnderenhet":"810007842","fnr":"22518249472","skal_ha_paaminnelse":true},"@id":"44df174f-3496-485d-ae3a-517b532aee70","@opprettet":"2024-10-23T12:52:56.081140841","system_read_count":0,"system_participating_services":[{"id":"44df174f-3496-485d-ae3a-517b532aee70","time":"2024-10-23T12:52:56.081140841","service":"im-forespoersel-mottatt","instance":"im-forespoersel-mottatt-58f979bbff-rbdst","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-forespoersel-mottatt:d79b643"}],"@forårsaket_av":{"id":"67b446f5-167f-4167-a590-91df7fccd66b","opprettet":"2024-10-23T12:52:56.077268111"}}
        """

        val payload2 =
            """
            {"@event_name":"FORESPOERSEL_BESVART","uuid":"b52d4703-48c9-4ada-bcba-a088f1acab96","forespoerselId":"556d6430-0c43-4dbc-8040-36ba37bfa191","@id":"ce1289a0-1554-4b41-8307-ed2396b59846","@opprettet":"2024-10-23T12:54:03.432888987","system_read_count":0,"system_participating_services":[{"id":"ce1289a0-1554-4b41-8307-ed2396b59846","time":"2024-10-23T12:54:03.432888987","service":"im-forespoersel-besvart","instance":"im-forespoersel-besvart-788d6bdbd-qqrw9","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-forespoersel-besvart:d79b643"}],"@forårsaket_av":{"id":"b584f32a-ca76-481f-8cf1-37c31d51b6f7","opprettet":"2024-10-23T12:54:03.413234137","event_name":"INNTEKTSMELDING_MOTTATT"}}
            """.trimIndent()

        val payload3 =
            """
            {"@event_name":"INNTEKTSMELDING_DISTRIBUERT","uuid":"c13943eb-e4be-47e9-9ae4-8fd9f09abf2e","journalpostId":"671159571","inntektsmelding":{"id":"d09b2674-9f2c-439f-9a60-9fe84ca117b5","type":{"type":"Forespurt","id":"8e43298c-8083-4daf-8092-8500e6524bbd"},"sykmeldt":{"fnr":"05499120674","navn":"FANTASTISK FERSKVANN"},"avsender":{"orgnr":"810007842","orgNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","navn":"BERØMT FLYTTELASS","tlf":"99999999"},"sykmeldingsperioder":[{"fom":"2024-07-01","tom":"2024-07-31"}],"agp":{"perioder":[{"fom":"2024-07-01","tom":"2024-07-16"}],"egenmeldinger":[],"redusertLoennIAgp":null},"inntekt":{"beloep":20000.0,"inntektsdato":"2024-07-01","naturalytelser":[],"endringAarsak":null},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-10-23T14:32:16.708400904+02:00","vedtaksperiodeId":"c6edb839-2fb5-4602-8f88-c70693f33015"},"bestemmende_fravaersdag":"2024-07-01","@id":"6d189fc1-79f7-4ecd-9d28-671a264df06f","@opprettet":"2024-10-23T14:32:17.596232993","system_read_count":0,"system_participating_services":[{"id":"6d189fc1-79f7-4ecd-9d28-671a264df06f","time":"2024-10-23T14:32:17.596232993","service":"im-distribusjon","instance":"im-distribusjon-798b4856fc-82qt6","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-distribusjon:3285d5d"}],"@forårsaket_av":{"id":"48715ec3-e08c-417e-bbce-78327e136482","opprettet":"2024-10-23T14:32:17.368784914","event_name":"INNTEKTSMELDING_JOURNALFOERT"}}
            """.trimIndent()

        val payload4 =
            """
            {"@event_name":"INNTEKTSMELDING_DISTRIBUERT","uuid":"c13943eb-e4be-47e9-9ae4-8fd9f09abf2f","journalpostId":"671159572","inntektsmelding":{"id":"6e9593cc-2a34-48fd-9eeb-ac90a68a2c1a","type":{"type":"Selvbestemt","id":"64fe55fa-6332-494c-8d82-50c09b4ea546"},"sykmeldt":{"fnr":"10107400090","navn":"BERØMT FLYTTELASS"},"avsender":{"orgnr":"810007842","orgNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","navn":"BERØMT FLYTTELASS","tlf":"12345678"},"sykmeldingsperioder":[{"fom":"2024-08-16","tom":"2024-08-28"}],"agp":{"perioder":[{"fom":"2024-08-16","tom":"2024-08-28"}],"egenmeldinger":[],"redusertLoennIAgp":{"beloep":12354.0,"begrunnelse":"ManglerOpptjening"}},"inntekt":{"beloep":54000.0,"inntektsdato":"2024-08-16","naturalytelser":[],"endringAarsak":{"aarsak":"Ferie","ferier":[{"fom":"2024-06-18","tom":"2024-06-23"}]}},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-10-23T08:47:22.357991117+02:00","vedtaksperiodeId":"eb7a3cc1-792c-4a26-b3af-324e7545c6dd"},"bestemmende_fravaersdag":"2024-07-01","@id":"6d189fc1-79f7-4ecd-9d28-671a264df06f","@opprettet":"2024-10-23T14:32:17.596232993","system_read_count":0,"system_participating_services":[{"id":"6d189fc1-79f7-4ecd-9d28-671a264df06f","time":"2024-10-23T14:32:17.596232993","service":"im-distribusjon","instance":"im-distribusjon-798b4856fc-82qt6","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-distribusjon:3285d5d"}],"@forårsaket_av":{"id":"48715ec3-e08c-417e-bbce-78327e136482","opprettet":"2024-10-23T14:32:17.368784914","event_name":"INNTEKTSMELDING_JOURNALFOERT"}}
            """.trimIndent()
        InntektsmeldingKafkaConsumer().handleRecord(payload)
        InntektsmeldingKafkaConsumer().handleRecord(payload2)
        InntektsmeldingKafkaConsumer().handleRecord(payload3)
        InntektsmeldingKafkaConsumer().handleRecord(payload4)
    }

    fun lagreInntektsmelding(
        im: String,
        orgnr: String,
        fnr: String,
    ): String {
        val forespoerselID = UUID.randomUUID().toString()
        val generert =
            im
                .replace("%%%FORESPORSELID%%%", forespoerselID)
                .replace("%%%ORGNR%%%", orgnr)
                .replace("%%%SYKMELDT%%%", fnr)
        Database.getInntektsmeldingRepo().opprett(generert, orgnr, fnr)
        return forespoerselID
    }
}
