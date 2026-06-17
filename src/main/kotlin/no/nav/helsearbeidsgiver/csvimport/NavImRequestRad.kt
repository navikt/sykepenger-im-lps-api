package no.nav.helsearbeidsgiver.csvimport

import java.util.UUID

data class NavImRequestRad(
    val company: String,
    val navReferanseId: UUID,
    val ssn: String,
    val unit: String,
    val incomeDate: String,
    val requestStatus: String,
    val externalCase: String,
    val firstDay: String,
    val lastDay: String,
)
