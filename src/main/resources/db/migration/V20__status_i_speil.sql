CREATE TABLE status_i_speil
(
    vedtaksperiode_id UUID NOT NULL,
    soeknad_id        UUID NOT NULL,
    behandling_id     UUID NOT NULL,
    opprettet         TIMESTAMP
);
ALTER TABLE status_i_speil
ADD CONSTRAINT unique_vedtaksperiode_soeknad UNIQUE (vedtaksperiode_id, soeknad_id);
