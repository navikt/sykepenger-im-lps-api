package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
import java.time.LocalDateTime

fun SykepengesoknadDTO.konverter(): Sykepengesoeknad =
    Sykepengesoeknad(
        soeknadId = id,
        type = Sykepengesoeknad.Soeknadstype.valueOf(type.name),
        fnr = fnr,
        sykmeldingId = sykmeldingId,
        arbeidsgiver = arbeidsgiver.konverter(),
        korrigerer = korrigerer,
        soektUtenlandsopphold = soktUtenlandsopphold,
        fom = fom,
        tom = tom,
        arbeidGjenopptattDato = arbeidGjenopptatt,
        mottatTid = utledSendtTid(),
        // behandlingsdager = behandlingsdager ?: emptyList(), TODO: skal vi ta med denne videre til ag?
        fravaer = fravar?.map { it.konverter() }.orEmpty(),
        soeknadsperioder = soknadsperioder?.map { it.konverter() }.orEmpty(),
    )

fun SykepengesoknadDTO.SoknadsperiodeDTO.konverter(): Sykepengesoeknad.Soeknadsperiode {
    requireNotNull(fom)
    requireNotNull(tom)
    requireNotNull(sykmeldingsgrad)
    requireNotNull(sykmeldingstype)
    return Sykepengesoeknad.Soeknadsperiode(
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
    requireNotNull(sendtArbeidsgiver)
    return sendtArbeidsgiver
}

private fun SykepengesoknadDTO.ArbeidsgiverDTO?.konverter(): Sykepengesoeknad.Arbeidsgiver {
    requireNotNull(this)
    requireNotNull(navn)
    requireNotNull(orgnummer)
    return Sykepengesoeknad.Arbeidsgiver(
        navn = navn,
        orgnr = orgnummer,
    )
}

private fun SykepengesoknadDTO.FravarDTO.konverter(): Sykepengesoeknad.Fravaer {
    requireNotNull(fom)
    requireNotNull(type)
    return Sykepengesoeknad.Fravaer(
        fom = fom,
        tom = tom,
        type = Sykepengesoeknad.Fravaerstype.valueOf(type.name),
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
