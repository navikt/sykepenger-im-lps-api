drop table if exists inntektsmelding;

CREATE TABLE IF NOT EXISTS inntektsmelding
(
    id            SERIAL PRIMARY KEY,
    orgnr         VARCHAR(9)  NOT NULL,
    fnr           VARCHAR(11) NOT NULL,
    dokument      TEXT       NOT NULL,
    foresporselId VARCHAR(40),
    innsendt      TIMESTAMP   NOT NULL DEFAULT now(),
    mottatt_event TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS mottak
(
    id      SERIAL PRIMARY KEY,
    melding TEXT NOT NULL
);