package no.nav.helsearbeidsgiver.utils

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method

// Tror ikke denne fungerer sammen med insert som gir exception,
// transaksjon rulles tilbake slik at ConstraintViolation ruller tilbake alle inserts i hele testen..
// (test som feilet: InntektsmeldingRepository: opprett skal ikke kunne lagre samme inntektsmelding (id) to ganger)
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
