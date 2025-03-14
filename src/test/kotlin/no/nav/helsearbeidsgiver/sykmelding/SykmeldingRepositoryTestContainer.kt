package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.helsearbeidsgiver.utils.FunSpecWithDb
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SykmeldingRepositoryTestContainer :
    FunSpecWithDb(listOf(SykmeldingEntitet), { db ->

        val sykmeldingRepository = SykmeldingRepository(db)

        test("skal lagre sykmelding") {
            transaction {
                SykmeldingEntitet.selectAll().toList()
            }.shouldBeEmpty()

            sykmeldingRepository.opprettSykmelding(sykmeldingMock())

            val lagretSykmelding =
                transaction(db) {
                    SykmeldingEntitet
                        .selectAll()
                        .where { SykmeldingEntitet.sykmeldingId eq UUID.fromString(sykmeldingMock().sykmelding.id) }
                        .firstOrNull()
                        ?.getOrNull(SykmeldingEntitet.arbeidsgiverSykmelding)
                }
            lagretSykmelding.shouldNotBeNull()
        }
    })
