package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.forespoersel.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.forespoersel.ForespurtData
import no.nav.helsearbeidsgiver.forespoersel.Inntekt
import no.nav.helsearbeidsgiver.forespoersel.Type
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
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
                    "juridiskOrgnummer": "744372453",
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
                    },
                    {
                        "svar": "[\"2025-03-29\",\"2025-03-30\",\"2025-03-31\"]",
                        "tekst": "Velg dagene du brukte egenmelding",
                        "svartype": "DAGER",
                        "shortName": "EGENMELDINGSDAGER"
                    }
                ]
            }
        }
        """

    const val SYKMELDING_API_RESPONSE =
        """
        {
            "sykmeldingId": "b5f66f7a-d1a9-483c-a9d1-e4d45a7bba4d",
            "mottattAvNav": "2020-03-14T23:00",
            "sykmeldt": {
                "fnr": "01447842099",
                "navn": "Ola Nordmann"
            },
            "egenmeldingsdager": [
                {
                    "fom": "2025-03-29",
                    "tom": "2025-03-31"
                }
            ],
            "sykefravaerFom": "2020-03-15",
            "sykmeldingPerioder": [
                {
                    "fom": "2020-03-15",
                    "tom": "2020-04-15",
                    "aktivitet": {
                        "avventendeSykmelding": null,
                        "gradertSykmelding": null,
                        "aktivitetIkkeMulig": {
                            "manglendeTilretteleggingPaaArbeidsplassen": false,
                            "beskrivelse": "andre årsaker til sykefravær"
                        },
                        "antallBehandlingsdagerUke": null,
                        "harReisetilskudd": false
                    }
                }
            ],
            "oppfoelging": {
                "prognose": {
                    "erArbeidsfoerEtterEndtPeriode": true,
                    "beskrivHensynArbeidsplassen": "Må ta det pent"
                },
                "tiltakArbeidsplassen": "Fortsett som sist."
            },
            "kontaktMedPasient": "2020-03-14T23:00",
            "behandler": {
                "navn": "Frida Perma Frost",
                "tlf": "1234678"
            },
            "arbeidsgiver": {
                "navn": "LOMMEN BARNEHAVE",
                "orgnr": "315587336"
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

    const val SYKEPENGE_SOKNAD = """
        {
  "id": "88f66acd-3bc2-4554-b69a-35a723a6af09",
  "aktorId": "aktorId-745463060",
  "sykmeldingId": "45462a55-f2a4-4173-87bb-1ee10a3b7a6c",
  "soknadstype": "ARBEIDSTAKERE",
  "status": "NY",
  "fom": "2019-03-24T00:00:00.000Z",
  "tom": "2019-04-01T00:00:00.000Z",
  "opprettetDato": "2019-04-02T00:00:00.000Z",
  "sendtTilNAVDato": null,
  "sendtTilArbeidsgiverDato": null,
  "avbruttDato": null,
  "startSykeforlop": "2019-03-23",
  "sykmeldingUtskrevet": "2019-03-24",
  "arbeidsgiver": {
    "navn": "ÅSEN BOFELLESSKAP",
    "orgnummer": "995816598"
  },
  "korrigerer": null,
  "korrigertAv": null,
  "arbeidssituasjon": "ARBEIDSTAKER",
  "soknadPerioder": [
    {
      "fom": "2019-03-24",
      "tom": "2019-04-01",
      "grad": 100
    }
  ],
  "sporsmal": [
    {
      "id": "187771",
      "tag": "ANSVARSERKLARING",
      "sporsmalstekst": "Jeg vet at dersom jeg gir uriktige opplysninger, eller holder tilbake opplysninger som har betydning for min rett til sykepenger, kan pengene holdes tilbake eller kreves tilbake, og/eller det kan medføre straffeansvar. Jeg er også klar over at jeg må melde fra til NAV dersom jeg i sykmeldingsperioden satt i varetekt, sonet straff eller var under forvaring.",
      "undertekst": null,
      "svartype": "CHECKBOX_PANEL",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": null,
      "svar": [
        {
          "verdi": "CHECKED"
        }
      ],
      "undersporsmal": []
    },
    {
      "id": "187772",
      "tag": "EGENMELDINGER",
      "sporsmalstekst": "Vi har registrert at du ble sykmeldt lørdag 23. mars 2019. Brukte du egenmeldinger og/eller var du sykmeldt i perioden 7. - 22. mars 2019?",
      "undertekst": null,
      "svartype": "JA_NEI",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": "JA",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "undersporsmal": [
        {
          "id": "187773",
          "tag": "EGENMELDINGER_NAR",
          "sporsmalstekst": "Hvilke dager før 23. mars 2019 var du borte fra jobb?",
          "undertekst": null,
          "svartype": "PERIODER",
          "min": "2018-09-23",
          "max": "2019-03-22",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": []
        }
      ]
    },
    {
      "id": "187774",
      "tag": "TILBAKE_I_ARBEID",
      "sporsmalstekst": "Var du tilbake i fullt arbeid hos ÅSEN BOFELLESSKAP før 2. april 2019?",
      "undertekst": null,
      "svartype": "JA_NEI",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": "JA",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "undersporsmal": [
        {
          "id": "187775",
          "tag": "TILBAKE_NAR",
          "sporsmalstekst": "Fra hvilken dato ble arbeidet gjenopptatt?",
          "undertekst": null,
          "svartype": "DATO",
          "min": "2019-03-24",
          "max": "2019-04-01",
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": []
        }
      ]
    },
    {
      "id": "187792",
      "tag": "ANDRE_INNTEKTSKILDER",
      "sporsmalstekst": "Har du andre inntektskilder, eller jobber du for andre enn ÅSEN BOFELLESSKAP?",
      "undertekst": null,
      "svartype": "JA_NEI",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": "JA",
      "svar": [],
      "undersporsmal": [
        {
          "id": "187793",
          "tag": "HVILKE_ANDRE_INNTEKTSKILDER",
          "sporsmalstekst": "Hvilke andre inntektskilder har du?",
          "undertekst": "Du trenger ikke oppgi andre ytelser fra NAV",
          "svartype": "CHECKBOX_GRUPPE",
          "min": null,
          "max": null,
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": [
            {
              "id": "187794",
              "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD",
              "sporsmalstekst": "Andre arbeidsforhold",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "187795",
                  "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD_ER_DU_SYKMELDT",
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "undertekst": null,
                  "svartype": "JA_NEI",
                  "min": null,
                  "max": null,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "187796",
              "tag": "INNTEKTSKILDE_SELVSTENDIG",
              "sporsmalstekst": "Selvstendig næringsdrivende",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "187797",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG_ER_DU_SYKMELDT",
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "undertekst": null,
                  "svartype": "JA_NEI",
                  "min": null,
                  "max": null,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "187798",
              "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA",
              "sporsmalstekst": "Selvstendig næringsdrivende dagmamma",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "187799",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA_ER_DU_SYKMELDT",
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "undertekst": null,
                  "svartype": "JA_NEI",
                  "min": null,
                  "max": null,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "187800",
              "tag": "INNTEKTSKILDE_JORDBRUKER",
              "sporsmalstekst": "Jordbruker / Fisker / Reindriftsutøver",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "187801",
                  "tag": "INNTEKTSKILDE_JORDBRUKER_ER_DU_SYKMELDT",
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "undertekst": null,
                  "svartype": "JA_NEI",
                  "min": null,
                  "max": null,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "187802",
              "tag": "INNTEKTSKILDE_FRILANSER",
              "sporsmalstekst": "Frilanser",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "187803",
                  "tag": "INNTEKTSKILDE_FRILANSER_ER_DU_SYKMELDT",
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "undertekst": null,
                  "svartype": "JA_NEI",
                  "min": null,
                  "max": null,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "187804",
              "tag": "INNTEKTSKILDE_ANNET",
              "sporsmalstekst": "Annet",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": null,
              "svar": [],
              "undersporsmal": []
            }
          ]
        }
      ]
    },
    {
      "id": "187808",
      "tag": "VAER_KLAR_OVER_AT",
      "sporsmalstekst": "Vær klar over at:",
      "undertekst": "<ul><li>rett til sykepenger forutsetter at du er borte fra arbeid på grunn av egen sykdom. Sosiale eller økonomiske problemer gir ikke rett til sykepenger</li><li>du kan miste retten til sykepenger hvis du uten rimelig grunn nekter å opplyse om egen funksjonsevne eller nekter å ta imot tilbud om behandling og/eller tilrettelegging</li><li>sykepenger utbetales i maksimum 52 uker, også for gradert (delvis) sykmelding</li><li>fristen for å søke sykepenger er som hovedregel 3 måneder</li></ul>",
      "svartype": "IKKE_RELEVANT",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": null,
      "svar": [],
      "undersporsmal": []
    },
    {
      "id": "187809",
      "tag": "BEKREFT_OPPLYSNINGER",
      "sporsmalstekst": "Jeg har lest all informasjonen jeg har fått i søknaden og bekrefter at opplysningene jeg har gitt er korrekte.",
      "undertekst": null,
      "svartype": "CHECKBOX_PANEL",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": null,
      "svar": [],
      "undersporsmal": []
    },
    {
      "id": "188629",
      "tag": "ARBEID_UNDERVEIS_100_PROSENT_0",
      "sporsmalstekst": "I perioden 24. mars - 1. april 2019 2019 var du 100 % sykmeldt fra ÅSEN BOFELLESSKAP. Jobbet du noe i denne perioden?",
      "undertekst": null,
      "svartype": "JA_NEI",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": "JA",
      "svar": [],
      "undersporsmal": [
        {
          "id": "188630",
          "tag": "HVOR_MANGE_TIMER_PER_UKE_0",
          "sporsmalstekst": "Hvor mange timer jobbet du per uke før du ble sykmeldt?",
          "svartype": "TALL",
          "min": 1,
          "max": 150,
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": []
        },
        {
          "id": "188631",
          "tag": "HVOR_MYE_HAR_DU_JOBBET_0",
          "sporsmalstekst": "Hvor mye jobbet du totalt 24. mars - 1. april 2019 hos ÅSEN BOFELLESSKAP?",
          "undertekst": null,
          "svartype": "RADIO_GRUPPE_TIMER_PROSENT",
          "min": null,
          "max": null,
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": [
            {
              "id": "188632",
              "tag": "HVOR_MYE_PROSENT_0",
              "sporsmalstekst": "prosent",
              "undertekst": null,
              "svartype": "RADIO",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [
                {
                  "verdi": "CHECKED"
                }
              ],
              "undersporsmal": [
                {
                  "id": "188633",
                  "tag": "HVOR_MYE_PROSENT_VERDI_0",
                  "sporsmalstekst": null,
                  "undertekst": "prosent",
                  "svartype": "TALL",
                  "min": 1,
                  "max": 99,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "188634",
              "tag": "HVOR_MYE_TIMER_0",
              "sporsmalstekst": "timer",
              "undertekst": null,
              "svartype": "RADIO",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "188635",
                  "tag": "HVOR_MYE_TIMER_VERDI_0",
                  "sporsmalstekst": null,
                  "svartype": "TALL",
                  "min": 1,
                  "max": 64,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "id": "188636",
      "tag": "FERIE_PERMISJON_UTLAND",
      "sporsmalstekst": "Har du hatt ferie, permisjon eller oppholdt deg utenfor Norge i perioden 24. mars - 1. april 2019?",
      "undertekst": null,
      "svartype": "JA_NEI",
      "min": null,
      "max": null,
      "kriterieForVisningAvUndersporsmal": "JA",
      "svar": [
        {
          "verdi": "JA"
        }
      ],
      "undersporsmal": [
        {
          "id": "188637",
          "tag": "FERIE_PERMISJON_UTLAND_HVA",
          "sporsmalstekst": "Kryss av alt som gjelder deg:",
          "undertekst": null,
          "svartype": "CHECKBOX_GRUPPE",
          "min": null,
          "max": null,
          "kriterieForVisningAvUndersporsmal": null,
          "svar": [],
          "undersporsmal": [
            {
              "id": "188638",
              "tag": "FERIE",
              "sporsmalstekst": "Jeg tok ut ferie",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "188639",
                  "tag": "FERIE_NAR",
                  "sporsmalstekst": null,
                  "undertekst": null,
                  "svartype": "PERIODER",
                  "min": "2019-03-24",
                  "max": "2019-03-26",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "188640",
              "tag": "PERMISJON",
              "sporsmalstekst": "Jeg hadde permisjon",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [],
              "undersporsmal": [
                {
                  "id": "188641",
                  "tag": "PERMISJON_NAR",
                  "sporsmalstekst": null,
                  "undertekst": null,
                  "svartype": "PERIODER",
                  "min": "2019-03-24",
                  "max": "2019-03-26",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [],
                  "undersporsmal": []
                }
              ]
            },
            {
              "id": "188642",
              "tag": "UTLAND",
              "sporsmalstekst": "Jeg var utenfor Norge",
              "undertekst": null,
              "svartype": "CHECKBOX",
              "min": null,
              "max": null,
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "svar": [
                {
                  "verdi": "CHECKED"
                }
              ],
              "undersporsmal": [
                {
                  "id": "188643",
                  "tag": "UTLAND_NAR",
                  "sporsmalstekst": null,
                  "undertekst": null,
                  "svartype": "PERIODER",
                  "min": "2019-03-24",
                  "max": "2019-03-26",
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [
                    {
                      "verdi": "{\"fom\":\"25.03.2019\",\"tom\":\"26.03.2019\"}"
                    }
                  ],
                  "undersporsmal": []
                },
                {
                  "id": "188644",
                  "tag": "UTLANDSOPPHOLD_SOKT_SYKEPENGER",
                  "sporsmalstekst": null,
                  "undertekst": null,
                  "svartype": "JA_NEI",
                  "min": null,
                  "max": null,
                  "kriterieForVisningAvUndersporsmal": null,
                  "svar": [
                    {
                      "verdi": "JA"
                    }
                  ],
                  "undersporsmal": []
                }
              ]
            }
          ]
        }
      ]
    }
  ]
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

    fun sykmeldingModelMock(sykmeldingModel: String = SYKMELDING_API_RESPONSE): Sykmelding =
        jsonMapper.decodeFromString<Sykmelding>(sykmeldingModel)
}
