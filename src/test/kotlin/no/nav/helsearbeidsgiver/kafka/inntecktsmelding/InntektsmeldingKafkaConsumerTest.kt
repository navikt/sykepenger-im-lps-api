package no.nav.helsearbeidsgiver.kafka.inntecktsmelding

import no.nav.helsearbeidsgiver.db.Database
import org.junit.Ignore

class InntektsmeldingKafkaConsumerTest {
    @Ignore
    fun handleRecord() {
        Database.init()
        val payload = """
            {"@event_name":"INNTEKTSMELDING_MOTTATT","uuid":"bf127701-ac11-4bf5-b2aa-e4af7da9bf00","forespoerselId":"a6229c94-6e4a-4030-bf52-8b60eeea8ff2","inntektsmelding":{"id":"2220ba8c-0f1c-44c0-8611-f48c8bc1f91b","type":{"type":"Forespurt","id":"a6229c94-6e4a-4030-bf52-8b60eeea8ff2"},"sykmeldt":{"fnr":"22518249472","navn":"OVERFØLSOM GRAVEMASKIN"},"avsender":{"orgnr":"810007842","orgNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","navn":"BERØMT FLYTTELASS","tlf":"33333333"},"sykmeldingsperioder":[{"fom":"2024-07-01","tom":"2024-07-31"}],"agp":{"perioder":[{"fom":"2024-07-01","tom":"2024-07-16"}],"egenmeldinger":[],"redusertLoennIAgp":null},"inntekt":{"beloep":40000.0,"inntektsdato":"2024-07-01","naturalytelser":[],"endringAarsak":null},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-10-16T07:59:03.129692683+02:00","vedtaksperiodeId":"97ebf079-5a23-4389-bc6d-188a575d5cb0"},"bestemmende_fravaersdag":"2024-07-01","innsending_id":1536,"@id":"1833189e-6f48-43fd-802a-30807322cf38","@opprettet":"2024-10-16T07:59:03.159458805","system_read_count":0,"system_participating_services":[{"id":"1833189e-6f48-43fd-802a-30807322cf38","time":"2024-10-16T07:59:03.159458805","service":"im-berik-inntektsmelding-service","instance":"im-berik-inntektsmelding-service-db69cc896-j59l7","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-berik-inntektsmelding-service:b155fcc"}]}
        """
        InntektsmeldingKafkaConsumer().handleRecord(payload)
    }
}
