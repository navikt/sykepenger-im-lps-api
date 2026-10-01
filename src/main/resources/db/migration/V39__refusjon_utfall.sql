CREATE TABLE refusjon_utfall
(
    id                 BIGSERIAL PRIMARY KEY,
    refusjon_utfall_id UUID       NOT NULL UNIQUE,
    vedtaksperiode_id  UUID       NOT NULL,
    orgnr              VARCHAR(9) NOT NULL,
    refusjon_utfall    JSONB      NOT NULL,
    opprettet          TIMESTAMP  NOT NULL DEFAULT now()
);

CREATE INDEX refusjon_utfall_vedtaksperiode_id_index ON refusjon_utfall (vedtaksperiode_id);
CREATE INDEX refusjon_utfall_orgnr_index ON refusjon_utfall (orgnr);
