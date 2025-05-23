package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO
import no.nav.helsearbeidsgiver.soknad.Sykepengesoknad

fun SykepengesoknadDTO.konverter(): Sykepengesoknad =
    Sykepengesoknad(
        id = this.id,
        type = Sykepengesoknad.Soknadstype.valueOf(this.type.name),
        // status = Sykepengesoknad.Soknadsstatus.valueOf(this.status.name),
        fnr = this.fnr,
        sykmeldingId = this.sykmeldingId,
        arbeidsgiver = konverter(this.arbeidsgiver),
        // arbeidssituasjon = enumValueOrNull(this.arbeidssituasjon!!.name),
        // korrigertAv = this.korrigertAv,
        korrigerer = this.korrigerer,
        soktUtenlandsopphold = this.soktUtenlandsopphold,
        arbeidsgiverForskutterer = this.arbeidsgiverForskutterer.tilArbeidsgiverForskutterer(),
        fom = this.fom,
        tom = this.tom,
        startSykeforlop = this.startSyketilfelle,
        arbeidGjenopptatt = this.arbeidGjenopptatt,
        sykmeldingSkrevet = this.sykmeldingSkrevet,
        // opprettet = this.opprettet,
        sendtNav = this.sendtNav,
        sendtArbeidsgiver = this.sendtArbeidsgiver,
        // behandlingsdager = this.behandlingsdager ?: emptyList(), TODO: skal vi ta med denne videre til ag?
        egenmeldinger =
            this.egenmeldinger
                ?.map { konverter(it) }
                .orEmpty(), // TODO: Skal vi ta med egenmeldinger fra sykmeldingen
        fravarForSykmeldingen =
            this.fravarForSykmeldingen
                ?.map { konverter(it) }
                .orEmpty(),
        /*papirsykmeldinger =
            this.papirsykmeldinger
                ?.map { konverter(it) }
                .orEmpty(),*/
        fravar =
            this.fravar
                ?.map { konverter(it) }
                .orEmpty(),
        /*andreInntektskilder =
            this.andreInntektskilder
                ?.map { konverter(it) }
                .orEmpty(),*/
        soknadsperioder =
            this.soknadsperioder
                ?.map { konverter(it) }
                .orEmpty(),
        sporsmal =
            this.sporsmal
                ?.map { konverter(it) }
                .orEmpty(),
        // ettersending = this.ettersending,
    )

private fun konverter(svarDTO: SykepengesoknadDTO.SvarDTO): Sykepengesoknad.Svar =
    Sykepengesoknad.Svar(
        verdi = svarDTO.verdi,
    )

private fun konverter(sporsmalDTO: SykepengesoknadDTO.SporsmalDTO): Sykepengesoknad.Sporsmal {
    requireNotNull(sporsmalDTO.svartype)
    return Sykepengesoknad.Sporsmal(
        id = sporsmalDTO.id,
        tag = sporsmalDTO.tag,
        sporsmalstekst = sporsmalDTO.sporsmalstekst,
        undertekst = sporsmalDTO.undertekst,
        svartype = Sykepengesoknad.Svartype.valueOf(sporsmalDTO.svartype.name),
        min = sporsmalDTO.min,
        max = sporsmalDTO.max,
        kriterieForVisningAvUndersporsmal = enumValueOrNull(sporsmalDTO.kriterieForVisningAvUndersporsmal?.name),
        svar =
            sporsmalDTO.svar
                ?.map { konverter(it) }
                .orEmpty(),
        undersporsmal =
            sporsmalDTO.undersporsmal
                ?.map { konverter(it) }
                .orEmpty(),
    )
}

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
        orgnummer = arbeidsgiverDTO.orgnummer,
    )
}

private fun konverter(periodeDTO: SykepengesoknadDTO.PeriodeDTO): Periode {
    requireNotNull(periodeDTO.fom)
    requireNotNull(periodeDTO.tom)
    return Periode(
        fom = periodeDTO.fom,
        tom = periodeDTO.tom,
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

/*private fun konverter(inntektskildeDTO: SykepengesoknadDTO.InntektskildeDTO): Sykepengesoknad.Inntektskilde =
    Sykepengesoknad.Inntektskilde(
        type = Sykepengesoknad.Inntektskildetype.valueOf(inntektskildeDTO.type!!.name),
        sykmeldt = inntektskildeDTO.sykmeldt,
    )*/

private inline fun <reified T : Enum<*>> enumValueOrNull(name: String?): T? = T::class.java.enumConstants.firstOrNull { it.name == name }

private fun SykepengesoknadDTO.ArbeidsgiverForskuttererDTO?.tilArbeidsgiverForskutterer(): Sykepengesoknad.ArbeidsverForskutterer =
    when (this) {
        SykepengesoknadDTO.ArbeidsgiverForskuttererDTO.JA -> Sykepengesoknad.ArbeidsverForskutterer.JA
        SykepengesoknadDTO.ArbeidsgiverForskuttererDTO.NEI -> Sykepengesoknad.ArbeidsverForskutterer.NEI
        SykepengesoknadDTO.ArbeidsgiverForskuttererDTO.VET_IKKE -> Sykepengesoknad.ArbeidsverForskutterer.VET_IKKE
        else -> Sykepengesoknad.ArbeidsverForskutterer.IKKE_SPURT
    }

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
