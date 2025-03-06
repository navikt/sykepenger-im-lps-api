CREATE TABLE innsending
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dokument          JSONB       NOT NULL,
    orgnr             VARCHAR(9)  NOT NULL,
    fnr               VARCHAR(11) NOT NULL,
    lps               VARCHAR(40) NOT NULL,
    foresporsel_id     UUID        NOT NULL,
    innsendt_dato      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    inntektsmelding_id INTEGER REFERENCES inntektsmelding (id),
    status            VARCHAR(15) NOT NULL,
    feil_aarsak        VARCHAR(500)
);