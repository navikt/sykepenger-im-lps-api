package helsearbeidsgiver.nav.no.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Avsender
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Naturalytelse
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RedusertLoennIAgp
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class ImService {
    fun hentInntektsmeldinger() : List<Inntektsmelding> {
        return listOf(
            Inntektsmelding(
                id = UUID.randomUUID(),
                type =
                Inntektsmelding.Type.Forespurt(
                    id = UUID.randomUUID(),
                    vedtaksperiodeId = UUID.randomUUID(),
                ),
                sykmeldt =
                Sykmeldt(
                    fnr = Fnr("10107400090"),
                    navn = "Skummel Bolle",
                ),
                avsender =
                Avsender(
                    orgnr = Orgnr("810007842"),
                    orgNavn = "Skumle bakverk A/S",
                    navn = "Nifs Krumkake",
                    tlf = "44553399",
                ),
                sykmeldingsperioder =
                listOf(
                    Periode(
                        fom = LocalDate.of(2024, 10, 5),
                        tom = LocalDate.of(2024, 10, 15),
                    ),
                    Periode(
                        fom = LocalDate.of(2024, 10, 20),
                        tom = LocalDate.of(2024, 11, 3),
                    ),
                ),
                agp =
                Arbeidsgiverperiode(
                    listOf(
                        Periode(
                            fom = LocalDate.of(2024, 10, 5),
                            tom = LocalDate.of(2024, 10, 15),
                        ),
                        Periode(
                            fom = LocalDate.of(2024, 10, 20),
                            tom = LocalDate.of(2024, 10, 22),
                        ),
                    ),
                    listOf(
                        Periode(
                            fom = LocalDate.of(2024, 9, 20),
                            tom = LocalDate.of(2024, 9, 28),
                        ),
                        Periode(
                            fom = LocalDate.of(2024, 9, 30),
                            tom = LocalDate.of(2024, 9, 30),
                        ),
                    ),
                    RedusertLoennIAgp(
                        beloep = 300.3,
                        begrunnelse = RedusertLoennIAgp.Begrunnelse.FerieEllerAvspasering,
                    ),
                ),
                inntekt =
                Inntekt(
                    beloep = 544.6,
                    inntektsdato = LocalDate.of(2024, 9, 28),
                    naturalytelser =
                    listOf(
                        Naturalytelse(
                            naturalytelse = Naturalytelse.Kode.BEDRIFTSBARNEHAGEPLASS,
                            verdiBeloep = 52.5,
                            sluttdato = LocalDate.of(2024, 10, 10),
                        ),
                        Naturalytelse(
                            naturalytelse = Naturalytelse.Kode.BIL,
                            verdiBeloep = 434.0,
                            sluttdato = LocalDate.of(2024, 10, 12),
                        ),
                    ),
                    endringAarsak =
                    NyStillingsprosent(
                        gjelderFra = LocalDate.of(2024, 10, 16),
                    ),
                ),
                refusjon =
                Refusjon(
                    beloepPerMaaned = 150.2,
                    endringer = listOf(),
                    sluttdato = LocalDate.of(2024, 10, 31),
                ),
                aarsakInnsending = AarsakInnsending.Endring,
                mottatt = LocalDateTime.of(2024, 3, 14, 14,41,42,0).atOffset(ZoneOffset.ofHours(1)),
            )
        )
    }
}
