package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.forespoersel.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.forespoersel.ForespurtData
import no.nav.helsearbeidsgiver.forespoersel.Inntekt
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.utils.json.fromJson
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
            "loepenr": 1,
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

    const val UGYLDIG_FORESPOERSEL_BESVART_MANGLER_FORESPORSEL_ID =
        """
        {
            "notis": "FORESPOERSEL_BESVART",
            "spinnInntektsmeldingId": "329da7e0-ae51-4d26-a5e6-d70adb83aa16"
        }
        """

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

    const val SYKEPENGESOEKNAD = """
        {
            "id": "9e088b5a-16c8-3dcc-91fb-acdd544b8607",
            "type": "ARBEIDSTAKERE",
            "status": "SENDT",
            "fnr": "05449412615",
            "sykmeldingId": "5be7a6f1-90e2-4e4c-9718-1e4491bf8111",
            "arbeidsgiver": {
                "navn": "Jordisk Kunst Katt Kryddermål",
                "orgnummer": "315587336"
            },
            "arbeidssituasjon": "ARBEIDSTAKER",
            "korrigerer": null,
            "korrigertAv": null,
            "soktUtenlandsopphold": false,
            "arbeidsgiverForskutterer": null,
            "fom": "2024-11-04",
            "tom": "2024-11-22",
            "dodsdato": null,
            "startSyketilfelle": "2024-09-02",
            "arbeidGjenopptatt": null,
            "friskmeldt": null,
            "sykmeldingSkrevet": "2025-03-28T01:00:00",
            "opprettet": "2025-04-04T14:57:15.464407",
            "opprinneligSendt": null,
            "sendtNav": "2025-05-07T09:24:53.038533194",
            "sendtArbeidsgiver": "2025-05-07T09:24:53.038533194",
            "egenmeldinger": null,
            "fravarForSykmeldingen": [],
            "papirsykmeldinger": [],
            "fravar": [],
            "andreInntektskilder": [],
            "soknadsperioder": [
                {
                    "fom": "2024-11-04",
                    "tom": "2024-11-22",
                    "sykmeldingsgrad": 100,
                    "faktiskGrad": null,
                    "avtaltTimer": null,
                    "faktiskTimer": null,
                    "sykmeldingstype": "AKTIVITET_IKKE_MULIG",
                    "grad": 100
                }
            ],
            "sporsmal": [
                {
                    "id": "f4f79188-dffc-3140-beb2-223289dab1b3",
                    "tag": "ANSVARSERKLARING",
                    "sporsmalstekst": "Jeg bekrefter at jeg vil svare så riktig som jeg kan.",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "CHECKBOX_PANEL",
                    "kriterieForVisningAvUndersporsmal": null,
                    "svar": [
                        {
                            "verdi": "CHECKED"
                        }
                    ],
                    "undersporsmal": [],
                    "metadata": null
                },
                {
                    "id": "30ca0f3d-eb84-3305-a76e-bce489f8bcf6",
                    "tag": "TILBAKE_I_ARBEID",
                    "sporsmalstekst": "Var du tilbake i fullt arbeid hos Jordisk Kunst Katt Kryddermål i løpet av perioden 4. - 22. november 2024?",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "JA_NEI",
                    "kriterieForVisningAvUndersporsmal": "JA",
                    "svar": [
                        {
                            "verdi": "NEI"
                        }
                    ],
                    "undersporsmal": [
                        {
                            "id": "0770160d-c50a-3412-adab-ef3eba630055",
                            "tag": "TILBAKE_NAR",
                            "sporsmalstekst": "Når begynte du å jobbe igjen?",
                            "undertekst": null,
                            "min": "2024-11-04",
                            "max": "2024-11-22",
                            "svartype": "DATO",
                            "kriterieForVisningAvUndersporsmal": null,
                            "svar": [],
                            "undersporsmal": [],
                            "metadata": null
                        }
                    ],
                    "metadata": null
                },
                {
                    "id": "d2677818-067f-371a-baf3-ca0aafa7c4af",
                    "tag": "FERIE_V2",
                    "sporsmalstekst": "Tok du ut feriedager i tidsrommet 4. - 22. november 2024?",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "JA_NEI",
                    "kriterieForVisningAvUndersporsmal": "JA",
                    "svar": [
                        {
                            "verdi": "NEI"
                        }
                    ],
                    "undersporsmal": [
                        {
                            "id": "ac0c815b-3700-3914-b38f-67149ce5e041",
                            "tag": "FERIE_NAR_V2",
                            "sporsmalstekst": "Når tok du ut feriedager?",
                            "undertekst": null,
                            "min": "2024-11-04",
                            "max": "2024-11-22",
                            "svartype": "PERIODER",
                            "kriterieForVisningAvUndersporsmal": null,
                            "svar": [],
                            "undersporsmal": [],
                            "metadata": null
                        }
                    ],
                    "metadata": null
                },
                {
                    "id": "806e9b17-2ab3-327c-bacc-c74f9112c3cd",
                    "tag": "PERMISJON_V2",
                    "sporsmalstekst": "Tok du permisjon mens du var sykmeldt 4. - 22. november 2024?",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "JA_NEI",
                    "kriterieForVisningAvUndersporsmal": "JA",
                    "svar": [
                        {
                            "verdi": "NEI"
                        }
                    ],
                    "undersporsmal": [
                        {
                            "id": "a440c126-faa5-38ad-897d-0285208df2ad",
                            "tag": "PERMISJON_NAR_V2",
                            "sporsmalstekst": "Når tok du permisjon?",
                            "undertekst": null,
                            "min": "2024-11-04",
                            "max": "2024-11-22",
                            "svartype": "PERIODER",
                            "kriterieForVisningAvUndersporsmal": null,
                            "svar": [],
                            "undersporsmal": [],
                            "metadata": null
                        }
                    ],
                    "metadata": null
                },
                {
                    "id": "7a71e347-a2d1-34de-8e8a-c0d34dba08b4",
                    "tag": "ARBEID_UNDERVEIS_100_PROSENT_0",
                    "sporsmalstekst": "I perioden 4. - 22. november 2024 var du 100 % sykmeldt fra Jordisk Kunst Katt Kryddermål. Jobbet du noe hos Jordisk Kunst Katt Kryddermål i denne perioden?",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "JA_NEI",
                    "kriterieForVisningAvUndersporsmal": "JA",
                    "svar": [
                        {
                            "verdi": "NEI"
                        }
                    ],
                    "undersporsmal": [
                        {
                            "id": "6fd462e8-5291-333d-a997-02cb8f165079",
                            "tag": "HVOR_MYE_HAR_DU_JOBBET_0",
                            "sporsmalstekst": "Oppgi arbeidsmengde i timer eller prosent:",
                            "undertekst": null,
                            "min": null,
                            "max": null,
                            "svartype": "RADIO_GRUPPE_TIMER_PROSENT",
                            "kriterieForVisningAvUndersporsmal": null,
                            "svar": [],
                            "undersporsmal": [
                                {
                                    "id": "2d68fa9a-c2a2-377b-b8f7-8868e8a148d9",
                                    "tag": "HVOR_MYE_PROSENT_0",
                                    "sporsmalstekst": "Prosent",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "RADIO",
                                    "kriterieForVisningAvUndersporsmal": "CHECKED",
                                    "svar": [],
                                    "undersporsmal": [
                                        {
                                            "id": "5a15f8bf-1217-378c-97a4-e9ca2f256591",
                                            "tag": "HVOR_MYE_PROSENT_VERDI_0",
                                            "sporsmalstekst": "Oppgi hvor mange prosent av din normale arbeidstid du jobbet hos Jordisk Kunst Katt Kryddermål i perioden 4. - 22. november 2024?",
                                            "undertekst": "Oppgi i prosent. Eksempel: 40",
                                            "min": "1",
                                            "max": "99",
                                            "svartype": "PROSENT",
                                            "kriterieForVisningAvUndersporsmal": null,
                                            "svar": [],
                                            "undersporsmal": [],
                                            "metadata": null
                                        }
                                    ],
                                    "metadata": null
                                },
                                {
                                    "id": "b7b0472c-1941-3134-8311-65f084c2b34a",
                                    "tag": "HVOR_MYE_TIMER_0",
                                    "sporsmalstekst": "Timer",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "RADIO",
                                    "kriterieForVisningAvUndersporsmal": "CHECKED",
                                    "svar": [],
                                    "undersporsmal": [
                                        {
                                            "id": "a8dc2815-6582-3118-a0c6-f9955253a911",
                                            "tag": "HVOR_MYE_TIMER_VERDI_0",
                                            "sporsmalstekst": "Oppgi totalt antall timer du jobbet i perioden 4. - 22. november 2024 hos Jordisk Kunst Katt Kryddermål",
                                            "undertekst": "Eksempel: 8,5",
                                            "min": "1",
                                            "max": "407",
                                            "svartype": "TIMER",
                                            "kriterieForVisningAvUndersporsmal": null,
                                            "svar": [],
                                            "undersporsmal": [],
                                            "metadata": null
                                        }
                                    ],
                                    "metadata": null
                                }
                            ],
                            "metadata": null
                        },
                        {
                            "id": "24338bc2-2afd-3323-91d6-e43e6454d70e",
                            "tag": "JOBBER_DU_NORMAL_ARBEIDSUKE_0",
                            "sporsmalstekst": "Jobber du vanligvis 37,5 timer i uka hos Jordisk Kunst Katt Kryddermål?",
                            "undertekst": null,
                            "min": null,
                            "max": null,
                            "svartype": "JA_NEI",
                            "kriterieForVisningAvUndersporsmal": "NEI",
                            "svar": [],
                            "undersporsmal": [
                                {
                                    "id": "eae8ed98-27f4-390e-990e-3fbc33a615bc",
                                    "tag": "HVOR_MANGE_TIMER_PER_UKE_0",
                                    "sporsmalstekst": "Oppgi timer per uke",
                                    "undertekst": "Eksempel: 8,5",
                                    "min": "1",
                                    "max": "150",
                                    "svartype": "TIMER",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                }
                            ],
                            "metadata": null
                        }
                    ],
                    "metadata": null
                },
                {
                    "id": "aba9c35d-4cb3-30fd-9a8d-204925f33764",
                    "tag": "ANDRE_INNTEKTSKILDER_V2",
                    "sporsmalstekst": "Har du andre inntektskilder enn Jordisk Kunst Katt Kryddermål?",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "JA_NEI",
                    "kriterieForVisningAvUndersporsmal": "JA",
                    "svar": [
                        {
                            "verdi": "NEI"
                        }
                    ],
                    "undersporsmal": [
                        {
                            "id": "ee238a88-da38-30ad-a8b7-6d237a5ff69f",
                            "tag": "HVILKE_ANDRE_INNTEKTSKILDER",
                            "sporsmalstekst": "Velg inntektskildene som passer for deg:",
                            "undertekst": "Finner du ikke noe som passer for deg, svarer du nei på spørsmålet over",
                            "min": null,
                            "max": null,
                            "svartype": "CHECKBOX_GRUPPE",
                            "kriterieForVisningAvUndersporsmal": null,
                            "svar": [],
                            "undersporsmal": [
                                {
                                    "id": "93f60703-9c81-321a-80a5-ef76ec3595ab",
                                    "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD",
                                    "sporsmalstekst": "Ansatt andre steder enn nevnt over",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": "CHECKED",
                                    "svar": [],
                                    "undersporsmal": [
                                        {
                                            "id": "db130c20-971a-372c-bf13-da32e4152ad5",
                                            "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD_JOBBET_I_DET_SISTE",
                                            "sporsmalstekst": "Har du jobbet for eller mottatt inntekt fra én eller flere av disse arbeidsgiverne de siste 14 dagene før du ble sykmeldt?",
                                            "undertekst": null,
                                            "min": null,
                                            "max": null,
                                            "svartype": "JA_NEI",
                                            "kriterieForVisningAvUndersporsmal": null,
                                            "svar": [],
                                            "undersporsmal": [],
                                            "metadata": null
                                        }
                                    ],
                                    "metadata": null
                                },
                                {
                                    "id": "d3fa228f-07b4-3145-b8ae-6fbb2551a6f9",
                                    "tag": "INNTEKTSKILDE_SELVSTENDIG",
                                    "sporsmalstekst": "Selvstendig næringsdrivende",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                },
                                {
                                    "id": "42cc1c93-ab72-3796-8833-57f26a221ae0",
                                    "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA",
                                    "sporsmalstekst": "Dagmamma",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                },
                                {
                                    "id": "6d9eaba8-18f7-3a0c-974d-e7dadba19041",
                                    "tag": "INNTEKTSKILDE_JORDBRUKER",
                                    "sporsmalstekst": "Jordbruk / Fiske / Reindrift",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                },
                                {
                                    "id": "1f88ace0-d0e2-3ac7-aa63-cc5a60e7e1c2",
                                    "tag": "INNTEKTSKILDE_FRILANSER",
                                    "sporsmalstekst": "Frilanser",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                },
                                {
                                    "id": "5857c52d-e7f8-3998-bd3f-5508cd1fa27d",
                                    "tag": "INNTEKTSKILDE_OMSORGSLONN",
                                    "sporsmalstekst": "Kommunal omsorgstønad",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                },
                                {
                                    "id": "b8341e66-f889-3987-a7ea-852e5c343032",
                                    "tag": "INNTEKTSKILDE_FOSTERHJEM",
                                    "sporsmalstekst": "Fosterhjemsgodtgjørelse",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                },
                                {
                                    "id": "3cd34d9e-1b90-3556-9bb9-c16d3a364b3f",
                                    "tag": "INNTEKTSKILDE_STYREVERV",
                                    "sporsmalstekst": "Styreverv",
                                    "undertekst": null,
                                    "min": null,
                                    "max": null,
                                    "svartype": "CHECKBOX",
                                    "kriterieForVisningAvUndersporsmal": null,
                                    "svar": [],
                                    "undersporsmal": [],
                                    "metadata": null
                                }
                            ],
                            "metadata": null
                        }
                    ],
                    "metadata": {
                        "kjenteInntektskilder": [
                            {
                                "navn": "Jordisk Kunst Katt Kryddermål",
                                "kilde": "SYKMELDING",
                                "orgnummer": "315587336"
                            }
                        ]
                    }
                },
                {
                    "id": "532be66a-a26a-31e0-92dd-b9abbd65853b",
                    "tag": "OPPHOLD_UTENFOR_EOS",
                    "sporsmalstekst": "Var du på reise utenfor EU/EØS mens du var sykmeldt 4. - 22. november 2024?",
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "JA_NEI",
                    "kriterieForVisningAvUndersporsmal": "JA",
                    "svar": [
                        {
                            "verdi": "NEI"
                        }
                    ],
                    "undersporsmal": [
                        {
                            "id": "eea0dde6-f99f-31b0-a344-9fad1ee9646b",
                            "tag": "OPPHOLD_UTENFOR_EOS_NAR",
                            "sporsmalstekst": "Når var du utenfor EU/EØS?",
                            "undertekst": null,
                            "min": "2024-11-04",
                            "max": "2024-11-22",
                            "svartype": "PERIODER",
                            "kriterieForVisningAvUndersporsmal": null,
                            "svar": [],
                            "undersporsmal": [],
                            "metadata": null
                        }
                    ],
                    "metadata": null
                },
                {
                    "id": "0ef55925-42f6-313f-bebf-43d6bb83f785",
                    "tag": "TIL_SLUTT",
                    "sporsmalstekst": null,
                    "undertekst": null,
                    "min": null,
                    "max": null,
                    "svartype": "OPPSUMMERING",
                    "kriterieForVisningAvUndersporsmal": null,
                    "svar": [
                        {
                            "verdi": "true"
                        }
                    ],
                    "undersporsmal": [],
                    "metadata": null
                }
            ],
            "avsendertype": "BRUKER",
            "ettersending": false,
            "mottaker": "ARBEIDSGIVER_OG_NAV",
            "egenmeldtSykmelding": false,
            "yrkesskade": null,
            "arbeidUtenforNorge": null,
            "harRedusertVenteperiode": null,
            "behandlingsdager": [],
            "permitteringer": [],
            "merknaderFraSykmelding": null,
            "egenmeldingsdagerFraSykmelding": null,
            "merknader": null,
            "sendTilGosys": null,
            "utenlandskSykmelding": false,
            "medlemskapVurdering": null,
            "forstegangssoknad": false,
            "tidligereArbeidsgiverOrgnummer": null,
            "fiskerBlad": null,
            "inntektFraNyttArbeidsforhold": [],
            "selvstendigNaringsdrivende": null,
            "friskTilArbeidVedtakId": null,
            "friskTilArbeidVedtakPeriode": null,
            "fortsattArbeidssoker": null,
            "inntektUnderveis": null,
            "ignorerArbeidssokerregister": null
        }
    """

    const val STATUS_I_SPLEIS_MELDING = """
        {
            "vedtaksperiodeId": "3e377f98-1801-4fd2-8d14-cf95d2b831fa",
            "behandlingId": "5ea3b4ce-988e-4c01-9a35-3a449f11be62",
            "tidspunkt": "2025-06-05T16:15:46.320069203+02:00",
            "status": "OPPRETTET",
            "eksterneSøknadIder": [
                "9e088b5a-16c8-3dcc-91fb-acdd544b8607"
            ],
            "versjon": "2.0.2"
        }
    """

    const val API_INNSENDING_MELDING = """
         {
            "@event_name": "API_INNSENDING_STARTET",
            "kontekst_id": "d6c7618d-138e-4eb6-bd58-d4983d204f8a",
            "data": {
                "innsending": {
                    "innsendingId": "002cc8ad-982e-4ebe-92d3-ec53ae795d56",
                    "skjema": {
                        "forespoerselId": "a35dfec7-d4af-4d4c-b3f0-79aab9bd1a71",
                        "avsenderTlf": "12345678",
                        "agp": {
                            "perioder": [
                                {
                                    "fom": "2024-04-16",
                                    "tom": "2024-05-01"
                                }
                            ],
                            "egenmeldinger": [
                                {
                                    "fom": "2024-04-15",
                                    "tom": "2024-04-15"
                                }
                            ],
                            "redusertLoennIAgp": null
                        },
                        "inntekt": {
                            "beloep": 30047.0,
                            "inntektsdato": "2024-04-16",
                            "naturalytelser": [],
                            "endringAarsaker": []
                        },
                        "refusjon": null
                    },
                    "aarsakInnsending": "Endring",
                    "type": {
                        "type": "ForespurtEkstern",
                        "id": "a35dfec7-d4af-4d4c-b3f0-79aab9bd1a71",
                        "avsenderSystem": {
                            "orgnr": "315339138",
                            "navn": "Bruno FTW!",
                            "versjon": "1.1"
                        }
                    },
                    "innsendtTid": "2025-08-26T14:21:36.872757832+02:00",
                    "versjon": 1
                },
                "mottatt": "2025-08-26T14:21:58.915229905"
            }
        }
    """

    const val AVVIST_INNTEKTSMELDING_MELDING = """
         {
            "@event_name": "AVVIST_INNTEKTSMELDING",
            "kontekst_id": "d6c7618d-138e-4eb6-bd58-d4983d204f8a",
            "data": {
                "avvist_inntektsmelding": {
                    "inntektsmeldingId": "002cc8ad-982e-4ebe-92d3-ec53ae795d56",
                    "feilkode": "INNTEKT_AVVIKER_FRA_A_ORDNINGEN"
                }
            }
        }
    """

    fun forespoerselDokument(
        orgnr: String,
        fnr: String,
        forespoerselId: UUID = UUID.randomUUID(),
    ) = ForespoerselDokument(
        orgnr = orgnr,
        fnr = fnr,
        forespoerselId = forespoerselId,
        vedtaksperiodeId = UUID.randomUUID(),
        egenmeldingsperioder = emptyList(),
        sykmeldingsperioder = emptyList(),
        bestemmendeFravaersdager = emptyMap(),
        forespurtData =
            ForespurtData(
                Arbeidsgiverperiode(true),
                Inntekt(paakrevd = true),
            ),
    )

    fun sykmeldingMock(sykmeldingMottattMelding: String = SYKMELDING_MOTTATT): SendSykmeldingAivenKafkaMessage =
        jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(sykmeldingMottattMelding)

    fun sykmeldingModelMock(sykmeldingModel: String = SYKMELDING_API_RESPONSE): Sykmelding =
        jsonMapper.decodeFromString<Sykmelding>(sykmeldingModel)

    fun soeknadMock(soeknad: String = SYKEPENGESOEKNAD): SykepengeSoeknadKafkaMelding =
        soeknad.fromJson(SykepengeSoeknadKafkaMelding.serializer())

    fun SykepengeSoeknadKafkaMelding.medId(id: UUID) = this.copy(id = id)

    fun SykepengeSoeknadKafkaMelding.medOrgnr(orgnr: String) =
        this.copy(
            arbeidsgiver =
                SykepengeSoeknadKafkaMelding.ArbeidsgiverDTO(
                    this.arbeidsgiver?.navn,
                    orgnr,
                ),
        )
}
