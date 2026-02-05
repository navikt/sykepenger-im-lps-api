package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
import no.nav.helsearbeidsgiver.dialogporten.DialogUtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.dokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.fnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.navReferanseId
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.opprettet
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.orgnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.status
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.soeknad.SoeknadEntitet
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilTidspunktEndOfDay
import no.nav.helsearbeidsgiver.utils.tilTidspunktStartOfDay
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

class ForespoerselRepository(
    private val db: Database,
) {
    fun lagreForespoersel(
        forespoersel: ForespoerselDokument,
        status: Status = Status.AKTIV,
        eksponertForespoerselId: UUID,
        inntektPaakrevd: Boolean = forespoersel.forespurtData.inntekt.paakrevd,
        arbeidsgiverperiodePaakrevd: Boolean = forespoersel.forespurtData.arbeidsgiverperiode.paakrevd,
    ) {
        transaction(db) {
            ForespoerselEntitet.insert {
                it[navReferanseId] = forespoersel.forespoerselId
                it[orgnr] = forespoersel.orgnr
                it[fnr] = forespoersel.fnr
                it[opprettet] = LocalDateTime.now()
                it[this.status] = status
                it[this.eksponertForespoerselId] = eksponertForespoerselId
                it[vedtaksperiodeId] = forespoersel.vedtaksperiodeId
                it[dokument] = jsonMapper.encodeToString(ForespoerselDokument.serializer(), forespoersel)
                it[this.arbeidsgiverperiodePaakrevd] = arbeidsgiverperiodePaakrevd
                it[this.inntektPaakrevd] = inntektPaakrevd
            }
        }
        logger().info("Forespørsel ${forespoersel.forespoerselId} lagret")
    }

    fun hentForespoersel(navReferanseId: UUID): Forespoersel? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where {
                    (ForespoerselEntitet.navReferanseId eq navReferanseId)
                }.map {
                    it.toExposedforespoersel()
                }.getOrNull(0)
        }

    fun hentVedtaksperiodeId(navReferanseId: UUID): UUID? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { ForespoerselEntitet.navReferanseId eq navReferanseId }
                .map {
                    it[dokument].fromJson(ForespoerselDokument.serializer()).vedtaksperiodeId
                }.firstOrNull()
        }

    fun hentForespoersler(filter: ForespoerselFilter): List<Forespoersel> =
        transaction(db) {
            val query =
                ForespoerselEntitet
                    .selectAll()
                    .where {
                        orgnr eq filter.orgnr
                    }
            filter.fnr?.let { query.andWhere { fnr eq it } }
            filter.navReferanseId?.let { query.andWhere { navReferanseId eq it } }
            filter.status?.let { query.andWhere { status eq it } }
            filter.fom?.let { query.andWhere { opprettet greaterEq it.tilTidspunktStartOfDay() } }
            filter.tom?.let { query.andWhere { opprettet lessEq it.tilTidspunktEndOfDay() } }
            filter.fraLoepenr?.let { query.andWhere { ForespoerselEntitet.id greater it } }
            query.orderBy(ForespoerselEntitet.id, SortOrder.ASC)
            query.limit(MAX_ANTALL_I_RESPONS + 1) // Legg på en, for å kunne sjekke om det faktisk finnes flere enn max antall
            query.map {
                it.toExposedforespoersel()
            }
        }

    fun finnAktivForespoersler(eksponertForespoerselId: UUID): Forespoersel? =
        transaction(db) {
            val result =
                ForespoerselEntitet
                    .selectAll()
                    .where {
                        (ForespoerselEntitet.eksponertForespoerselId eq eksponertForespoerselId) and
                            (status eq Status.AKTIV)
                    }.map { it.toExposedforespoersel() }

            result.firstOrNull().also {
                if (result.size > 1) {
                    logger().warn(
                        "Fant flere aktive forespørsler med eksponertForespoerselId: $eksponertForespoerselId. " +
                            "Dette er ikke forventet og kan indikere en feil.",
                    )
                    throw IllegalStateException(
                        "Forventet en aktiv forespørsel, men fant ${result.size} aktive forespørsler med eksponertForespoerselId $eksponertForespoerselId",
                    )
                }
            }
        }

    fun oppdaterStatus(
        navReferanseId: UUID,
        status: Status,
    ): Int =
        transaction(db) {
            ForespoerselEntitet.update(
                where = {
                    (ForespoerselEntitet.navReferanseId eq navReferanseId)
                },
            ) {
                it[ForespoerselEntitet.status] = status
            }
        }

    private fun ResultRow.toExposedforespoersel(): Forespoersel {
        val dokument = jsonMapper.decodeFromString<ForespoerselDokument>(this[dokument])
        return Forespoersel(
            navReferanseId = this[navReferanseId],
            orgnr = this[orgnr],
            fnr = this[fnr],
            status = this[status],
            sykmeldingsperioder = dokument.sykmeldingsperioder,
            egenmeldingsperioder = dokument.egenmeldingsperioder,
            inntektsdato = dokument.forslagInntektsdato(),
            arbeidsgiverperiodePaakrevd = this[ForespoerselEntitet.arbeidsgiverperiodePaakrevd],
            inntektPaakrevd = this[ForespoerselEntitet.inntektPaakrevd],
            opprettetTid = this[opprettet],
            loepenr = this[ForespoerselEntitet.id],
            vedtaksperiodeId = this[ForespoerselEntitet.vedtaksperiodeId],
            forespoerselDokument = dokument,
        )
    }

    fun hentEksponertForespoerselId(forespoerselId: UUID): UUID? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { navReferanseId eq forespoerselId }
                .map { it[ForespoerselEntitet.eksponertForespoerselId] }
                .firstOrNull()
        }

    fun oppdaterEksponertForespoerselId(
        forespoerselId: UUID,
        eksponertForespoerselId: UUID,
    ) {
        transaction(db) {
            ForespoerselEntitet.update(
                where = {
                    navReferanseId eq forespoerselId
                },
            ) {
                it[ForespoerselEntitet.eksponertForespoerselId] = eksponertForespoerselId
            }
        }
        logger().info("Oppdaterte eksponertForespoerselId for forespørsel med id: $forespoerselId til $eksponertForespoerselId")
    }

    fun hentUtgaattForespoerselDialogMelding(forespoerselId: UUID): DialogUtgaattInntektsmeldingForespoersel? =
        transaction(db) {
            ForespoerselEntitet
                .join(
                    otherTable = SoeknadEntitet,
                    joinType = JoinType.INNER,
                    onColumn = ForespoerselEntitet.vedtaksperiodeId,
                    otherColumn = SoeknadEntitet.vedtaksperiodeId,
                ).select(navReferanseId, orgnr, SoeknadEntitet.sykmeldingId)
                .where({ navReferanseId eq forespoerselId })
                .limit(1)
                .map { row ->
                    DialogUtgaattInntektsmeldingForespoersel(
                        orgnr = row[orgnr],
                        forespoerselId = row[navReferanseId],
                        sykmeldingId = row[SoeknadEntitet.sykmeldingId],
                    )
                }.firstOrNull()
        }

    fun hentIkkeForkastedeForespoerslerPaaVedtaksperiodeId(vedtaksperiodeId: UUID): List<Forespoersel> =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where {
                    (ForespoerselEntitet.vedtaksperiodeId eq vedtaksperiodeId) and
                        (status neq Status.FORKASTET)
                }.orderBy(ForespoerselEntitet.id, SortOrder.DESC)
                .map { it.toExposedforespoersel() }
        }

    fun oppdaterPaakrevdeFelter(
        navReferanseId: UUID,
        inntektPaakrevd: Boolean,
        arbeidsgiverperiodePaakrevd: Boolean,
    ) {
        val transaction =
            transaction(db) {
                ForespoerselEntitet.update(
                    where = {
                        (ForespoerselEntitet.navReferanseId eq navReferanseId)
                    },
                ) {
                    it[ForespoerselEntitet.inntektPaakrevd] = inntektPaakrevd
                    it[ForespoerselEntitet.arbeidsgiverperiodePaakrevd] = arbeidsgiverperiodePaakrevd
                }
            }
    }
}
