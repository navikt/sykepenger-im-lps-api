package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.forespoersel.Type
import java.util.UUID

object TestData {
    val payload = """
            {"@event_name":"FORESPOERSEL_MOTTATT","uuid":"9c87f626-fe80-4618-ad37-f7e6a7026bc7","data":{"forespoerselId":"556d6430-0c43-4dbc-8040-36ba37bfa191","orgnrUnderenhet":"810007842","fnr":"22518249472","skal_ha_paaminnelse":true, "forespoersel":{"orgnr":"810007982","fnr":"01447842099","vedtaksperiodeId":"d707c782-ec1a-46a0-9a64-e5405810ad19","sykmeldingsperioder":[{"fom":"2024-09-18","tom":"2024-09-30"},{"fom":"2024-10-03","tom":"2024-10-10"}],"egenmeldingsperioder":[],"bestemmendeFravaersdager":{"810007982":"2024-10-03"},"forespurtData":{"arbeidsgiverperiode":{"paakrevd":true},"inntekt":{"paakrevd":true,"forslag":{"type":"ForslagInntektGrunnlag","forrigeInntekt":null}},"refusjon":{"paakrevd":true,"forslag":{"perioder":[],"opphoersdato":null}}},"erBesvart":false}},"@id":"44df174f-3496-485d-ae3a-517b532aee70","@opprettet":"2024-10-23T12:52:56.081140841","system_read_count":0,"system_participating_services":[{"id":"44df174f-3496-485d-ae3a-517b532aee70","time":"2024-10-23T12:52:56.081140841","service":"im-forespoersel-mottatt","instance":"im-forespoersel-mottatt-58f979bbff-rbdst","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-forespoersel-mottatt:d79b643"}],"@forårsaket_av":{"id":"67b446f5-167f-4167-a590-91df7fccd66b","opprettet":"2024-10-23T12:52:56.077268111"}}
        """

    val payload2 =
        """
        {"@event_name":"FORESPOERSEL_BESVART","uuid":"b52d4703-48c9-4ada-bcba-a088f1acab96","forespoerselId":"556d6430-0c43-4dbc-8040-36ba37bfa191","@id":"ce1289a0-1554-4b41-8307-ed2396b59846","@opprettet":"2024-10-23T12:54:03.432888987","system_read_count":0,"system_participating_services":[{"id":"ce1289a0-1554-4b41-8307-ed2396b59846","time":"2024-10-23T12:54:03.432888987","service":"im-forespoersel-besvart","instance":"im-forespoersel-besvart-788d6bdbd-qqrw9","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-forespoersel-besvart:d79b643"}],"@forårsaket_av":{"id":"b584f32a-ca76-481f-8cf1-37c31d51b6f7","opprettet":"2024-10-23T12:54:03.413234137","event_name":"INNTEKTSMELDING_MOTTATT"}}
        """.trimIndent()

    val payload3 =
        """
        {"journalpostId":"671166144","inntektsmeldingV1":{"id":"01f37507-24fa-4266-9f70-42b97f9eb527","type":{"type":"Forespurt","id":"034f161b-d43b-4e06-9563-3fe5fbd15442"},"sykmeldt":{"fnr":"01447842099","navn":"KONKURRANSEDYKTIG HANDLINGSROM"},"avsender":{"orgnr":"810007982","orgNavn":"ANSTENDIG PIGGSVIN SYKEHJEM","navn":"BERØMT FLYTTELASS","tlf":"11223344"},"sykmeldingsperioder":[{"fom":"2024-08-01","tom":"2024-08-31"}],"agp":{"perioder":[{"fom":"2024-08-01","tom":"2024-08-16"}],"egenmeldinger":[],"redusertLoennIAgp":null},"inntekt":{"beloep":24667.33,"inntektsdato":"2024-08-01","naturalytelser":[],"endringAarsak":null},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-11-07T15:12:52.207740699+01:00","vedtaksperiodeId":"d90503a1-c15a-41ca-9cb8-cbc729d63893"},"bestemmendeFravaersdag":"2024-08-01","inntektsmelding":{"orgnrUnderenhet":"810007982","identitetsnummer":"01447842099","fulltNavn":"KONKURRANSEDYKTIG HANDLINGSROM","virksomhetNavn":"ANSTENDIG PIGGSVIN SYKEHJEM","behandlingsdager":[],"egenmeldingsperioder":[],"fraværsperioder":[{"fom":"2024-08-01","tom":"2024-08-31"}],"arbeidsgiverperioder":[{"fom":"2024-08-01","tom":"2024-08-16"}],"beregnetInntekt":24667.33,"inntektsdato":"2024-08-01","inntekt":{"bekreftet":true,"beregnetInntekt":24667.33,"manueltKorrigert":false},"fullLønnIArbeidsgiverPerioden":{"utbetalerFullLønn":true},"refusjon":{"utbetalerHeleEllerDeler":false},"naturalytelser":[],"tidspunkt":"2024-11-07T15:12:52.207740699+01:00","årsakInnsending":"NY","innsenderNavn":"BERØMT FLYTTELASS","telefonnummer":"11223344","forespurtData":["arbeidsgiverperiode","inntekt"],"bestemmendeFraværsdag":"2024-08-01","vedtaksperiodeId":"d90503a1-c15a-41ca-9cb8-cbc729d63893"},"selvbestemt":false}
        """.trimIndent()

    val payload4 =
        """
        {"journalpostId":"671166582","inntektsmeldingV1":{"id":"2f7685d0-fe65-48c6-96b9-7b5189358ab1","type":{"type":"Selvbestemt","id":"24428a05-6826-4a01-a6be-30fb15816a6e"},"sykmeldt":{"fnr":"10107400090","navn":"BERØMT FLYTTELASS"},"avsender":{"orgnr":"810007842","orgNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","navn":"BERØMT FLYTTELASS","tlf":"12345678"},"sykmeldingsperioder":[{"fom":"2024-08-01","tom":"2024-08-08"}],"agp":{"perioder":[{"fom":"2024-08-01","tom":"2024-08-08"}],"egenmeldinger":[],"redusertLoennIAgp":{"beloep":23456.0,"begrunnelse":"ArbeidOpphoert"}},"inntekt":{"beloep":54000.0,"inntektsdato":"2024-08-01","naturalytelser":[],"endringAarsak":null},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-11-12T14:04:07.557238646+01:00","vedtaksperiodeId":"dc2b6464-d606-4c2d-8bb1-c5ce8d811077"},"bestemmendeFravaersdag":null,"inntektsmelding":{"orgnrUnderenhet":"810007842","identitetsnummer":"10107400090","fulltNavn":"BERØMT FLYTTELASS","virksomhetNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","behandlingsdager":[],"egenmeldingsperioder":[],"fraværsperioder":[{"fom":"2024-08-01","tom":"2024-08-08"}],"arbeidsgiverperioder":[{"fom":"2024-08-01","tom":"2024-08-08"}],"beregnetInntekt":54000.0,"inntektsdato":"2024-08-01","inntekt":{"bekreftet":true,"beregnetInntekt":54000.0,"manueltKorrigert":false},"fullLønnIArbeidsgiverPerioden":{"utbetalerFullLønn":false,"begrunnelse":"ArbeidOpphoert","utbetalt":23456.0},"refusjon":{"utbetalerHeleEllerDeler":false},"naturalytelser":[],"tidspunkt":"2024-11-12T14:04:07.557238646+01:00","årsakInnsending":"NY","innsenderNavn":"BERØMT FLYTTELASS","telefonnummer":"12345678","forespurtData":["arbeidsgiverperiode","inntekt"],"bestemmendeFraværsdag":"2024-08-01","vedtaksperiodeId":"dc2b6464-d606-4c2d-8bb1-c5ce8d811077"},"selvbestemt":true}
        """.trimIndent()

    val ikkeAktuellPayload =
        """
        {"@event_name":"TILGANG_FORESPOERSEL_REQUESTED","uuid":"b52d4703-48c9-4ada-bcba-a088f1acab96","forespoerselId":"556d6430-0c43-4dbc-8040-36ba37bfa191","@id":"ce1289a0-1554-4b41-8307-ed2396b59846","@opprettet":"2024-10-23T12:54:03.432888987","system_read_count":0,"system_participating_services":[{"id":"ce1289a0-1554-4b41-8307-ed2396b59846","time":"2024-10-23T12:54:03.432888987","service":"im-forespoersel-besvart","instance":"im-forespoersel-besvart-788d6bdbd-qqrw9","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-forespoersel-besvart:d79b643"}],"@forårsaket_av":{"id":"b584f32a-ca76-481f-8cf1-37c31d51b6f7","opprettet":"2024-10-23T12:54:03.413234137","event_name":"INNTEKTSMELDING_MOTTATT"}}
        """.trimIndent()

    fun forespoerselDokument(
        orgnr: String,
        fnr: String,
    ) = ForespoerselDokument(Type.KOMPLETT, orgnr, fnr, UUID.randomUUID(), UUID.randomUUID(), emptyList(), emptyList())
}
