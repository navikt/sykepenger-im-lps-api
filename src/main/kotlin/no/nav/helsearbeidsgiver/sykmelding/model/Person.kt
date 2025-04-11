package no.nav.helsearbeidsgiver.sykmelding.model

data class Person(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktorId: String,
    val fnr: String,
)
