package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.forespoersel.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.forespoersel.ForespurtData
import no.nav.helsearbeidsgiver.forespoersel.Inntekt
import no.nav.helsearbeidsgiver.forespoersel.Type
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import java.util.UUID

object TestData {
    const val FORESPOERSEL_MOTTATT = """
    {
    "notis": "FORESPØRSEL_MOTTATT",
    "forespoerselId": "c8d75a15-dce3-4db2-8b48-fc4d9a1cfd5c",
    "orgnr": "810007982",
    "fnr": "01447842099",
    "skal_ha_paaminnelse": true,
    "forespoersel": {
        "type": "KOMPLETT",
        "orgnr": "810007982",
        "fnr": "01447842099",
        "forespoerselId": "c8d75a15-dce3-4db2-8b48-fc4d9a1cfd5c",
        "vedtaksperiodeId": "70aeba59-ebe1-4e26-bf48-23765e037078",
        "egenmeldingsperioder": [],
        "sykmeldingsperioder": [
            {
                "fom": "2024-08-01",
                "tom": "2024-08-31"
            }
        ],
        "bestemmendeFravaersdager": {
            "810007982": "2024-08-01"
        },
        "forespurtData": {
            "arbeidsgiverperiode": {
                "paakrevd": true
            },
            "inntekt": {
                "paakrevd": true,
                "forslag": {
                    "type": "ForslagInntektGrunnlag",
                    "forrigeInntekt": null
                }
            },
            "refusjon": {
                "paakrevd": true,
                "forslag": {
                    "perioder": [],
                    "opphoersdato": null
                }
            }
        },
        "erBesvart": false
    }
}
        """

    const val FORESPOERSEL_BESVART =
        """
        {
            "notis": "FORESPOERSEL_BESVART",
            "forespoerselId": "c8d75a15-dce3-4db2-8b48-fc4d9a1cfd5c",
            "spinnInntektsmeldingId": "329da7e0-ae51-4d26-a5e6-d70adb83aa16"
        }
        """

    const val IM_MOTTATT =
        """
        {"journalpostId":"671166144","inntektsmelding":{"id":"01f37507-24fa-4266-9f70-42b97f9eb527","type":{"type":"Forespurt","id":"034f161b-d43b-4e06-9563-3fe5fbd15442"},"sykmeldt":{"fnr":"01447842099","navn":"KONKURRANSEDYKTIG HANDLINGSROM"},"avsender":{"orgnr":"810007982","orgNavn":"ANSTENDIG PIGGSVIN SYKEHJEM","navn":"BERØMT FLYTTELASS","tlf":"11223344"},"sykmeldingsperioder":[{"fom":"2024-08-01","tom":"2024-08-31"}],"agp":{"perioder":[{"fom":"2024-08-01","tom":"2024-08-16"}],"egenmeldinger":[],"redusertLoennIAgp":null},"inntekt":{"beloep":24667.33,"inntektsdato":"2024-08-01","naturalytelser":[],"endringAarsaker":[]},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-11-07T15:12:52.207740699+01:00","vedtaksperiodeId":"d90503a1-c15a-41ca-9cb8-cbc729d63893"},"bestemmendeFravaersdag":"2024-08-01","selvbestemt":false}
        """

    const val ARBEIDSGIVER_INITIERT_IM_MOTTATT =
        """
        {"journalpostId":"671166582","inntektsmelding":{"id":"2f7685d0-fe65-48c6-96b9-7b5189358ab1","type":{"type":"Selvbestemt","id":"24428a05-6826-4a01-a6be-30fb15816a6e"},"sykmeldt":{"fnr":"10107400090","navn":"BERØMT FLYTTELASS"},"avsender":{"orgnr":"810007842","orgNavn":"ANSTENDIG PIGGSVIN BARNEHAGE","navn":"BERØMT FLYTTELASS","tlf":"12345678"},"sykmeldingsperioder":[{"fom":"2024-08-01","tom":"2024-08-08"}],"agp":{"perioder":[{"fom":"2024-08-01","tom":"2024-08-08"}],"egenmeldinger":[],"redusertLoennIAgp":{"beloep":23456.0,"begrunnelse":"ArbeidOpphoert"}},"inntekt":{"beloep":54000.0,"inntektsdato":"2024-08-01","naturalytelser":[],"endringAarsaker":[]},"refusjon":null,"aarsakInnsending":"Ny","mottatt":"2024-11-12T14:04:07.557238646+01:00","vedtaksperiodeId":"dc2b6464-d606-4c2d-8bb1-c5ce8d811077"},"bestemmendeFravaersdag":null,"selvbestemt":true}
        """

    const val SYKMELDING_MOTTATT =
        """
        {
            "sykmelding": {
                "id": "b5f66f7a-d1a9-483c-a9d1-e4d45a7bba4d",
                "mottattTidspunkt": "2020-03-14T23:00:00Z",
                "syketilfelleStartDato": "2020-03-15",
                "behandletTidspunkt": "2020-03-14T23:00:00Z",
                "arbeidsgiver": {
                    "navn": "LOMMEN BARNEHAVE"
                },
                "sykmeldingsperioder": [
                    {
                        "fom": "2020-03-15",
                        "tom": "2020-04-15",
                        "type": "AKTIVITET_IKKE_MULIG",
                        "aktivitetIkkeMulig": {
                            "arbeidsrelatertArsak": {
                                "beskrivelse": "andre årsaker til sykefravær",
                                "arsak": [
                                    "ANNET"
                                ]
                            }
                        },
                        "reisetilskudd": false
                    }
                ],
                "prognose": {
                    "arbeidsforEtterPeriode": true,
                    "hensynArbeidsplassen": "Må ta det pent"
                },
                "tiltakArbeidsplassen": "Fortsett som sist.",
                "kontaktMedPasient": {},
                "behandler": {
                    "fornavn": "Frida",
                    "mellomnavn": "Perma",
                    "etternavn": "Frost",
                    "adresse": {
                        "gate": "Kirkegårdsveien 3",
                        "postnummer": 1348,
                        "kommune": "Rykkinn",
                        "land": "Country"
                    },
                    "tlf": "tel:1234678"
                },
                "egenmeldt": false,
                "papirsykmelding": false,
                "harRedusertArbeidsgiverperiode": false
            },
            "kafkaMetadata": {
                "sykmeldingId": "b5f66f7a-d1a9-483c-a9d1-e4d45a7bba4d",
                "timestamp": "2020-04-30T13:57:48.444372Z",
                "fnr": "01447842099",
                "source": "macgyver-syfoservice"
            },
            "event": {
                "sykmeldingId": "b5f66f7a-d1a9-483c-a9d1-e4d45a7bba4d",
                "timestamp": "2020-04-30T13:57:48.302706Z",
                "statusEvent": "SENDT",
                "arbeidsgiver": {
                    "orgnummer": "315587336",
                    "juridiskOrgnummer": "928497704",
                    "orgNavn": "Lama utleiren"
                },
                "sporsmals": [
                    {
                        "tekst": "Jeg er sykmeldt fra",
                        "shortName": "ARBEIDSSITUASJON",
                        "svartype": "ARBEIDSSITUASJON",
                        "svar": "ARBEIDSTAKER"
                    },
                    {
                        "tekst": "Skal finne ny nærmeste leder",
                        "shortName": "NY_NARMESTE_LEDER",
                        "svartype": "JA_NEI",
                        "svar": "NEI"
                    }
                ]
            }
        }
        """

    const val SIMBA_PAYLOAD =
        """
        {"@event_name":"TILGANG_FORESPOERSEL_REQUESTED","uuid":"b52d4703-48c9-4ada-bcba-a088f1acab96","forespoerselId":"556d6430-0c43-4dbc-8040-36ba37bfa191","@id":"ce1289a0-1554-4b41-8307-ed2396b59846","@opprettet":"2024-10-23T12:54:03.432888987","system_read_count":0,"system_participating_services":[{"id":"ce1289a0-1554-4b41-8307-ed2396b59846","time":"2024-10-23T12:54:03.432888987","service":"im-forespoersel-besvart","instance":"im-forespoersel-besvart-788d6bdbd-qqrw9","image":"ghcr.io/navikt/helsearbeidsgiver-inntektsmelding/im-forespoersel-besvart:d79b643"}],"@forårsaket_av":{"id":"b584f32a-ca76-481f-8cf1-37c31d51b6f7","opprettet":"2024-10-23T12:54:03.413234137","event_name":"INNTEKTSMELDING_MOTTATT"}}
        """

    const val UGYLDIG_JSON = """{ "hei":"gakk",....   """

    const val UGYLDIG_FORESPOERSEL_MOTTATT =
        """ 
        {
            "notis": "FORESPØRSEL_MOTTATT",
            "forespoerselId": "c8d75a15-dce3-4db2-8b48-fc4d9a1cfd5c",
            "orgnr": "810007982",
            "fnr": "01447842099",
            "skal_ha_paaminnelse": true,
            "forespoersel": {
                "type": "KOMPLETT",
            }
        }
        """

    const val TRENGER_FORESPOERSEL =
        """
        {
            "@behov": "TRENGER_FORESPØRSEL",
            "@løsning": {
                "forespoerselId": "d1ca7cce-d236-47e9-b0eb-37dc95511e4e",
                "resultat": {
                    "type": "KOMPLETT",
                    "orgnr": "810007842",
                    "fnr": "10107400090",
                    "forespoerselId": "d1ca7cce-d236-47e9-b0eb-37dc95511e4e",
                    "vedtaksperiodeId": "d8d2a8d2-8c64-42b6-b23d-0c688ca8cb17",
                    "egenmeldingsperioder": [],
                    "sykmeldingsperioder": [
                        {
                            "fom": "2025-02-26",
                            "tom": "2025-03-07"
                        },
                        {
                            "fom": "2025-03-08",
                            "tom": "2025-03-14"
                        }
                    ],
                    "bestemmendeFravaersdager": {
                        "983491251": "2025-02-26"
                    },
                    "forespurtData": {
                        "arbeidsgiverperiode": {
                            "paakrevd": true
                        },
                        "inntekt": {
                            "paakrevd": true,
                            "forslag": null
                        },
                        "refusjon": {
                            "paakrevd": true,
                            "forslag": {
                                "perioder": [],
                                "opphoersdato": null
                            }
                        }
                    },
                    "erBesvart": false,
                    "opprettetUpresisIkkeBruk": "2025-03-20"
                },
                "boomerang": {
                    "@event_name": "TRENGER_REQUESTED",
                    "kontekst_id": "827dd05d-bfc6-441d-a1ca-2b09346fb2f1",
                    "data": {
                        "forespoersel_id": "d1ca7cce-d236-47e9-b0eb-37dc95511e4e"
                    }
                }
            }
        }
    """

    fun forespoerselDokument(
        orgnr: String,
        fnr: String,
    ) = ForespoerselDokument(
        Type.KOMPLETT,
        orgnr,
        fnr,
        UUID.randomUUID(),
        UUID.randomUUID(),
        emptyList(),
        emptyList(),
        ForespurtData(
            Arbeidsgiverperiode(true),
            Inntekt(paakrevd = true),
        ),
    )

    fun sykmeldingMock(sykmeldingMottattMelding: String = SYKMELDING_MOTTATT): SendSykmeldingAivenKafkaMessage =
        jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(sykmeldingMottattMelding)
}
