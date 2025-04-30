package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import java.time.LocalDate

fun Set<LocalDate>.tilPerioder(): List<Periode> {
    if (isEmpty()) return emptyList()
    val sorterteDatoer = sorted().toSet()

    return buildList {
        var periodeStart = sorterteDatoer.first()

        sorterteDatoer.zipWithNext().forEach { (dato, nesteDato) ->
            if (nesteDato != dato.plusDays(1)) {
                add(Periode(fom = periodeStart, tom = dato))
                periodeStart = nesteDato
            }
        }

        add(Periode(periodeStart, sorterteDatoer.last()))
    }
}
