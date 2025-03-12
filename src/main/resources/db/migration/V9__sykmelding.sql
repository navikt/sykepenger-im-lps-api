CREATE TABLE sykmelding
(
    id            BIGSERIAL PRIMARY KEY,
    sykmelding_id UUID UNIQUE NOT NULL,
    fnr           VARCHAR(11) NOT NULL,
    sykmelding    JSONB       NOT NULL,
    opprettet     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX sykmelding_id_index ON sykmelding (sykmelding_id);
