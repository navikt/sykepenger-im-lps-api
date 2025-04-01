package no.nav.helsearbeidsgiver.sykmelding.model

import java.util.Locale

data class Person(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktorId: String,
    val fnr: String,
)

fun Person.fulltNavn(): String =
    if (mellomnavn.isNullOrEmpty()) {
        capitalizeFirstLetter("$fornavn $etternavn")
    } else {
        capitalizeFirstLetter("$fornavn $mellomnavn $etternavn")
    }

fun capitalizeFirstLetter(string: String): String =
    string
        .lowercase(Locale.getDefault())
        .split(" ")
        .joinToString(" ") { it ->
            it.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }.split("-")
        .joinToString("-") { it ->
            it.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }.trimEnd()
