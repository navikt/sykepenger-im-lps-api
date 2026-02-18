package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
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

fun Sykmelding.kapitaliserSykmeldtNavn(): Sykmelding = copy(sykmeldt = sykmeldt.copy(navn = sykmeldt.navn.kapitaliserNavn()))

fun String.kapitaliserNavn(): String =
    lowercase()
        .split(" ")
        .joinToString(" ") { ord ->
            ord
                .split("-")
                .joinToString("-") { it.replaceFirstChar(Char::titlecase) }
        }
