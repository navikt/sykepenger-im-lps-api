package no.nav.helsearbeidsgiver.sykmelding

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun Sykmelding.toPdf(): ByteArray {
    val loader = ClassPathTemplateLoader()
    loader.prefix = "/"
    loader.suffix = ".hbs"

    val handlebars = Handlebars(loader)
    val template = handlebars.compile("template")
    val htmlContent = template.apply(this)

    val fontSupplier: FSSupplier<InputStream> =
        FSSupplier {
            object {}
                .javaClass
                .getResourceAsStream("/opensans-font.ttf")
                ?: throw IllegalStateException("Could not find variable font file in resources.")
        }
    ByteArrayOutputStream().use { os ->
        PdfRendererBuilder()
            .useFastMode()
            .withHtmlContent(htmlContent, null)
            .toStream(os)
            .useFont(fontSupplier, "Open Sans")
            .useFont(fontSupplier, "sans-serif")
            .usePdfUaAccessbility(true)
            .run()
        return os.toByteArray()
    }
}
