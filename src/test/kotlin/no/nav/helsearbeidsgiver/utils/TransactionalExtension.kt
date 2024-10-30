package no.nav.helsearbeidsgiver.utils

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method

class TransactionalExtension : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>?,
        invocationContext: ReflectiveInvocationContext<Method>?,
        extensionContext: ExtensionContext?,
    ) {
        transaction {
            invocation?.proceed()
            rollback()
        }
    }
}
