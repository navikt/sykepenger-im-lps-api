package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.Sykepengesoknad
import java.time.LocalDateTime

fun SykepengesoknadDTO.konverter(): Sykepengesoknad =
    Sykepengesoknad(
        id = id,
        type = Sykepengesoknad.Soknadstype.valueOf(type.name),
        fnr = fnr,
        sykmeldingId = sykmeldingId,
        arbeidsgiver = arbeidsgiver.konverter(),
        korrigerer = korrigerer,
        soktUtenlandsopphold = soktUtenlandsopphold,
        fom = fom,
        tom = tom,
        arbeidGjenopptattDato = arbeidGjenopptatt,
        mottatTid = utledSendtTid(),
        // behandlingsdager = behandlingsdager ?: emptyList(), TODO: skal vi ta med denne videre til ag?
        fravar = fravar?.map { it.konverter() }.orEmpty(),
        soknadsperioder = soknadsperioder?.map { it.konverter() }.orEmpty(),
    )

fun SykepengesoknadDTO.SoknadsperiodeDTO.konverter(): Sykepengesoknad.Soknadsperiode {
    requireNotNull(fom)
    requireNotNull(tom)
    requireNotNull(sykmeldingsgrad)
    requireNotNull(sykmeldingstype)
    return Sykepengesoknad.Soknadsperiode(
        fom = fom,
        tom = tom,
        sykmeldingsgrad = sykmeldingsgrad,
        faktiskGrad = faktiskGrad,
        avtaltTimer = avtaltTimer,
        faktiskTimer = faktiskTimer,
        sykmeldingstype = enumValueOrNull(sykmeldingstype.name),
    )
}

private fun SykepengesoknadDTO.utledSendtTid(): LocalDateTime {
    // sendtArbeidsgiver og sendtNav blir populert av samme verdi så en av dem vil alltid være satt.
    val sendt = sendtArbeidsgiver ?: sendtNav
    requireNotNull(sendt)
    return sendt
}

private fun SykepengesoknadDTO.ArbeidsgiverDTO?.konverter(): Sykepengesoknad.Arbeidsgiver {
    requireNotNull(this)
    requireNotNull(navn)
    requireNotNull(orgnummer)
    return Sykepengesoknad.Arbeidsgiver(
        navn = navn,
        orgnr = orgnummer,
    )
}

private fun SykepengesoknadDTO.FravarDTO.konverter(): Sykepengesoknad.Fravar {
    requireNotNull(fom)
    requireNotNull(type)
    return Sykepengesoknad.Fravar(
        fom = fom,
        tom = tom,
        type = Sykepengesoknad.Fravarstype.valueOf(type.name),
    )
}

private inline fun <reified T : Enum<*>> enumValueOrNull(name: String?): T? = T::class.java.enumConstants.firstOrNull { it.name == name }

private val whitelistetHovedsporsmal =
    listOf(
        // Vanlige spørsmål
        "ANSVARSERKLARING",
        "TILBAKE_I_ARBEID",
        "FERIE_V2",
        "PERMISJON_V2",
        "JOBBET_DU_100_PROSENT",
        "JOBBET_DU_GRADERT",
        "ARBEID_UNDERVEIS_100_PROSENT",
        "OPPHOLD_UTENFOR_EOS",
        // behandlingsdager
        "FRAVER_FOR_BEHANDLING",
        "ENKELTSTAENDE_BEHANDLINGSDAGER",
        // gradert reisetilskudd
        "BRUKTE_REISETILSKUDDET",
        "TRANSPORT_TIL_DAGLIG",
        "REISE_MED_BIL",
        "KVITTERINGER",
        "UTBETALING",
        // deprecated spørsmål vi likevel støtter
        "UTDANNING",
        "FERIE_PERMISJON_UTLAND",
        "EGENMELDINGER",
        "PERMITTERT_NAA",
        "PERMITTERT_PERIODE",
        "FRAVAR_FOR_SYKMELDINGEN",
        "UTLAND",
        "UTLAND_V2",
    )

private fun String.fjernTagIndex(): String {
    val regex = "_\\d+$".toRegex()
    return regex.replace(this, "")
}

fun SykepengesoknadDTO.SporsmalDTO.erWhitelistetForArbeidsgiver(): Boolean {
    if (tag == null) {
        return false
    }
    return tag.fjernTagIndex() in whitelistetHovedsporsmal
}

fun SykepengesoknadDTO.whitelistetForArbeidsgiver(): SykepengesoknadDTO =
    this.copy(sporsmal = sporsmal?.filter { it.erWhitelistetForArbeidsgiver() })
