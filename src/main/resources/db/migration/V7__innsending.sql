CREATE TABLE innsending
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dokument          JSONB       NOT NULL,
    orgnr             VARCHAR(9)  NOT NULL,
    fnr               VARCHAR(11) NOT NULL,
    lps               VARCHAR(40) NOT NULL,
    foresporselid     UUID        NOT NULL,
    mottattDato       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    inntektsmeldingId INTEGER REFERENCES inntektsmelding (id),
    status            VARCHAR(15) NOT NULL,
    feilAarsak        VARCHAR(500)
);