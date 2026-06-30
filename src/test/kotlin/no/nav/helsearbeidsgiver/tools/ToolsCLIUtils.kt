package no.nav.helsearbeidsgiver.tools

import kotlinx.serialization.json.Json

val json =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

fun konverteringLesFraStdin(): String {
    System.err.println(
        "\n\n\n\n\n\n////////////// Database kafka -> API json konvertering  /////////////////\nLim inn JSON og trykk [ENTER]:\n",
    )
    return generateSequence(::readLine)
        .runningReduce { acc, line -> "$acc\n$line" }
        .first { it.count { c -> c == '{' } == it.count { c -> c == '}' } && it.isNotBlank() }
        .trim()
}

fun printOgKopierResultat(result: String) {
    println("///////////////////////////////")
    println(result)
    val os = System.getProperty("os.name").lowercase()
    val cmd =
        when {
            os.contains("mac") -> "pbcopy"
            os.contains("win") -> "clip"
            else -> "xclip -selection clipboard"
        }
    runCatching {
        ProcessBuilder(*cmd.split(" ").toTypedArray())
            .start()
            .outputStream
            .writer()
            .use { it.write(result) }
        System.err.println("✓ Kopiert til utklippstavlen")
    }
}
