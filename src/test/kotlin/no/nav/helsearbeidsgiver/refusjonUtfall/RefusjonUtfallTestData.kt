package no.nav.helsearbeidsgiver.refusjonUtfall

import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun refusjonUtfallMock(): RefusjonUtfall =
    RefusjonUtfall(
        refusjonUtfallId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        fnr = Fnr("10107400090"),
        orgnr = Orgnr("896929119"),
        fom = LocalDate.of(2026, 7, 28),
        tom = LocalDate.of(2026, 8, 3),
        sykepengegrunnlag = 154999.92,
        utfallTilArbeidsgiver = Utfall.INNVILGELSE,
        fattetTidspunkt = LocalDateTime.of(2026, 8, 5, 13, 3, 25),
    )
