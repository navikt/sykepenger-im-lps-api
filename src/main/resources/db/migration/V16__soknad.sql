CREATE TABLE soknad
(
    id                       BIGSERIAL PRIMARY KEY,
    soknad_id                UUID UNIQUE NOT NULL,
    soknad                   JSONB       NOT NULL,
    opprettet                TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX soknad_id_index ON soknad (soknad_id);
