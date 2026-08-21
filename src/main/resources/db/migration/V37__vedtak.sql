CREATE TABLE vedtak
(
    id                BIGSERIAL PRIMARY KEY,
    vedtaksperiode_id UUID        NOT NULL,
    fnr               VARCHAR(11) NOT NULL,
    orgnr             VARCHAR(9)  NOT NULL,
    vedtak            JSONB       NOT NULL,
    opprettet         TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX vedtak_vedtaksperiode_id_index ON vedtak (vedtaksperiode_id);
