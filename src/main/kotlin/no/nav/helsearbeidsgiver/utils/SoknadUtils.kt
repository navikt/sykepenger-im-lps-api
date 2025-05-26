package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.Sykepengesoknad
import java.time.LocalDateTime

fun SykepengesoknadDTO.konverter(): Sykepengesoknad =
    Sykepengesoknad(
        id = this.id,
        type = Sykepengesoknad.Soknadstype.valueOf(this.type.name),
        fnr = this.fnr,
        sykmeldingId = this.sykmeldingId,
        arbeidsgiver = konverter(this.arbeidsgiver),
        korrigerer = this.korrigerer,
        soktUtenlandsopphold = this.soktUtenlandsopphold,
        fom = this.fom,
        tom = this.tom,
        arbeidGjenopptattDato = this.arbeidGjenopptatt,
        opprettetTid = this.opprettet ?: LocalDateTime.now(),
        sendtNavTid = this.sendtNav,
        // behandlingsdager = this.behandlingsdager ?: emptyList(), TODO: skal vi ta med denne videre til ag?
        fravar =
            this.fravar
                ?.map { konverter(it) }
                .orEmpty(),
        soknadsperioder =
            this.soknadsperioder
                ?.map { konverter(it) }
                .orEmpty(),
    )

private fun konverter(soknadPeriodeDTO: SykepengesoknadDTO.SoknadsperiodeDTO): Sykepengesoknad.Soknadsperiode {
    requireNotNull(soknadPeriodeDTO.fom)
    requireNotNull(soknadPeriodeDTO.tom)
    requireNotNull(soknadPeriodeDTO.sykmeldingsgrad)
    requireNotNull(soknadPeriodeDTO.sykmeldingstype)
    return Sykepengesoknad.Soknadsperiode(
        fom = soknadPeriodeDTO.fom,
        tom = soknadPeriodeDTO.tom,
        sykmeldingsgrad = soknadPeriodeDTO.sykmeldingsgrad,
        faktiskGrad = soknadPeriodeDTO.faktiskGrad,
        avtaltTimer = soknadPeriodeDTO.avtaltTimer,
        faktiskTimer = soknadPeriodeDTO.faktiskTimer,
        sykmeldingstype = enumValueOrNull(soknadPeriodeDTO.sykmeldingstype.name),
    )
}

private fun konverter(arbeidsgiverDTO: SykepengesoknadDTO.ArbeidsgiverDTO?): Sykepengesoknad.Arbeidsgiver {
    requireNotNull(arbeidsgiverDTO)
    requireNotNull(arbeidsgiverDTO.navn)
    requireNotNull(arbeidsgiverDTO.orgnummer)
    return Sykepengesoknad.Arbeidsgiver(
        navn = arbeidsgiverDTO.navn,
        orgnr = arbeidsgiverDTO.orgnummer,
    )
}

private fun konverter(fravarDTO: SykepengesoknadDTO.FravarDTO): Sykepengesoknad.Fravar {
    requireNotNull(fravarDTO.fom)
    requireNotNull(fravarDTO.type)
    return Sykepengesoknad.Fravar(
        fom = fravarDTO.fom,
        tom = fravarDTO.tom,
        type = Sykepengesoknad.Fravarstype.valueOf(fravarDTO.type.name),
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
    if (this.tag == null) {
        return false
    }
    return this.tag.fjernTagIndex() in whitelistetHovedsporsmal
}

fun SykepengesoknadDTO.whitelistetForArbeidsgiver(): SykepengesoknadDTO =
    this.copy(sporsmal = this.sporsmal?.filter { it.erWhitelistetForArbeidsgiver() })
