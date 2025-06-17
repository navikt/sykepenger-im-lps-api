package no.nav.helsearbeidsgiver.forespoersel

import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.dokument
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.fnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.navReferanseId
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.orgnr
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselEntitet.status
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
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
        navReferanseId: UUID,
        payload: ForespoerselDokument,
    ) {
        val organisasjonsnummer = payload.orgnr
        val foedselsnr = payload.fnr
        val jsonString = jsonMapper.encodeToString(ForespoerselDokument.serializer(), payload)
        transaction(db) {
            ForespoerselEntitet.insert {
                it[this.navReferanseId] = navReferanseId
                it[orgnr] = organisasjonsnummer
                it[fnr] = foedselsnr
                it[opprettet] = LocalDateTime.now()
                it[status] = Status.AKTIV
                it[dokument] = jsonString
            }
        }
        sikkerLogger().info("Forespørsel $navReferanseId lagret")
    }

    fun lagreForespoersel(
        forespoersel: ForespoerselDokument,
        status: Status,
        eksponertForespoerselId: UUID? = null,
    ) {
        transaction(db) {
            ForespoerselEntitet.insert {
                it[navReferanseId] = forespoersel.forespoerselId
                it[orgnr] = forespoersel.orgnr
                it[fnr] = forespoersel.fnr
                it[opprettet] = LocalDateTime.now()
                it[this.status] = status
                it[this.eksponertForespoerselId] = eksponertForespoerselId
                it[dokument] = jsonMapper.encodeToString(ForespoerselDokument.serializer(), forespoersel)
            }
        }
        sikkerLogger().info("Forespørsel ${forespoersel.forespoerselId} lagret")
    }

    fun hentForespoersel(
        navReferanseId: UUID,
        orgnr: String,
    ): Forespoersel? =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where {
                    (ForespoerselEntitet.navReferanseId eq navReferanseId) and
                        (ForespoerselEntitet.orgnr eq orgnr)
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

    fun hentForespoerslerForOrgnr(orgnr: String): List<Forespoersel> =
        transaction(db) {
            ForespoerselEntitet
                .selectAll()
                .where { ForespoerselEntitet.orgnr eq orgnr }
                .map {
                    it.toExposedforespoersel()
                }
        }

    fun filtrerForespoersler(
        consumerOrgnr: String,
        request: ForespoerselRequest,
    ): List<Forespoersel> =
        transaction(db) {
            addLogger(StdOutSqlLogger)
            ForespoerselEntitet
                .selectAll()
                .where {
                    (orgnr eq consumerOrgnr) and
                        (if (!request.fnr.isNullOrBlank()) fnr eq request.fnr else Op.TRUE) and
                        (if (request.navReferanseId != null) navReferanseId eq request.navReferanseId else Op.TRUE) and
                        (if (request.status != null) status eq request.status else Op.TRUE)
                }.map {
                    it.toExposedforespoersel()
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
            arbeidsgiverperiodePaakrevd = dokument.forespurtData.arbeidsgiverperiode.paakrevd,
            inntektPaakrevd = dokument.forespurtData.inntekt.paakrevd,
        )
    }
}
