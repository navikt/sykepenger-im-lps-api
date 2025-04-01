package no.nav.helsearbeidsgiver.sykmelding.altinnFormat

import no.nav.helse.xml.sykmelding.arbeidsgiver.ObjectFactory
import no.nav.helse.xml.sykmelding.arbeidsgiver.XMLSykmeldingArbeidsgiver
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamResult

class JAXB private constructor() {
    companion object {
        val SYKMELDING_ARBEIDSGIVER_CONTEXT: JAXBContext =
            JAXBContext.newInstance(ObjectFactory::class.java)

        fun marshallSykmeldingArbeidsgiver(sykmeldingArbeidsgiver: XMLSykmeldingArbeidsgiver): String {
            try {
                val element = ObjectFactory().createSykmeldingArbeidsgiver(sykmeldingArbeidsgiver)
                val writer = StringWriter()
                val marshaller = SYKMELDING_ARBEIDSGIVER_CONTEXT.createMarshaller()
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
                marshaller.marshal(element, StreamResult(writer))
                return writer.toString()
            } catch (ex: JAXBException) {
                throw RuntimeException("Error marshalling sykmelding", ex)
            }
        }

        fun parseXml(xml: String): Document {
            try {
                val builderFactory = DocumentBuilderFactory.newInstance()
                builderFactory.isNamespaceAware = true
                val documentBuilder = builderFactory.newDocumentBuilder()
                return documentBuilder.parse(InputSource(StringReader(xml)))
            } catch (ex: Exception) {
                throw RuntimeException("Error i parsing av XML", ex)
            }
        }
    }
}
