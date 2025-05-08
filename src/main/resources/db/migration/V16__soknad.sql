CREATE TABLE soknad
(
    id                       BIGSERIAL PRIMARY KEY,
    soknad_id                UUID UNIQUE NOT NULL,
    sykmelding_id            UUID NOT NULL,
    fnr                      VARCHAR(11) NOT NULL,
    orgnr                    VARCHAR(9) NOT NULL,
    soknad                   JSONB       NOT NULL,
    opprettet                TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX soknad_id_index ON soknad (soknad_id);
