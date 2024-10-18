package no.nav.helsearbeidsgiver.kafka.inntecktsmelding

import no.nav.helsearbeidsgiver.db.Database
import org.junit.Ignore

class InntektsmeldingKafkaConsumerTest {
    @Ignore
    fun handleRecord() {
        Database.init()
        val payload = """
            {"@event_name":"INNTEKTSMELDING_DISTRIBUERT","uuid":"e842491c-d717-4ca3-b3f1-b81ef75c4739","journalpostId":"671159009","inntektsmelding":{"id":"69297657-fdbe-451b-b77e-43ae3aa90e29","type":{"type":"Forespurt","id":"724fb233-2175-4b58-8f47-181f24547f30"},"sykmeldt":{"fnr":"22518249472","navn":"OVERFØLSOM GRAVEMASKIN"},"avsender":{"orgnr":"810007842","orgNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","navn":"BERØMT FLYTTELASS","tlf":"12345678"},"sykmeldingsperioder":[{"fom":"2024-06-01","tom":"2024-06-30"}],"agp":{"perioder":[{"fom":"2024-06-01","tom":"2024-06-16"}],"egenmeldinger":[],"redusertLoennIAgp":{"beloep":1000.0,"begrunnelse":"BetvilerArbeidsufoerhet"}},"inntekt":{"beloep":60000.0,"inntektsdato":"2024-06-01","naturalytelser":[],"endringAarsak":null},"refusjon":null,"aarsakInnsending":"Endring","mottatt":"2024-10-18T13:18:28.592508012+02:00","vedtaksperiodeId":"7e7c6173-ea6a-4228-8b18-d1c3e3e0af7c"},"bestemmende_fravaersdag":"2024-06-01","@id":"88a3b4e0-bac8-45e4-a128-7cd89cdb515d","@opprettet":"2024-10-18T13:18:29.434466991","system_read_count":0,"system_participating_services":[{"id":"88a3b4e0-bac8-45e4-a128-7cd89cdb515d","time":"2024-10-18T13:18:29.434466991","service":"im-distribusjon","instance":"im-distribusjon-56d6fc7859-rh4g5","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-distribusjon:a180e3d"}],"@forårsaket_av":{"id":"ccbb8e0e-9560-4a52-bd84-056f7e7b348f","opprettet":"2024-10-18T13:18:29.371630591","event_name":"INNTEKTSMELDING_JOURNALFOERT"}}
        """
        InntektsmeldingKafkaConsumer().handleRecord(payload)
    }
}
