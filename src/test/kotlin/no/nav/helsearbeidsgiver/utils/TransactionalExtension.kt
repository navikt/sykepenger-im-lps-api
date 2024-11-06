package no.nav.helsearbeidsgiver.utils

import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TransactionalExtension :
    BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        transaction {
            context.getStore(ExtensionContext.Namespace.GLOBAL).put("transaction", this)
        }
    }

    override fun afterEach(context: ExtensionContext) {
        val transaction = context.getStore(ExtensionContext.Namespace.GLOBAL).get("transaction") as Transaction
        transaction.rollback()
    }
}
