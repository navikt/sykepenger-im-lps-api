CREATE TABLE im
(
    id        SERIAL   PRIMARY KEY,
    orgnr     VARCHAR(9) NOT NULL,
    fnr       VARCHAR(11) NOT NULL,
    dokument  JSONB    NOT NULL,
    foresporselId      VARCHAR(40),
    innsendt           TIMESTAMP   NOT NULL DEFAULT now(),
    mottatt_event      TIMESTAMP   NOT NULL DEFAULT now()
);



