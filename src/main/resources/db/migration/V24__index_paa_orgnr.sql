CREATE INDEX IF NOT EXISTS ix_fsp_orgnr ON forespoersel (orgnr);
CREATE INDEX IF NOT EXISTS ix_fsp_nav_referanse_id ON forespoersel (nav_referanse_id);

CREATE INDEX IF NOT EXISTS ix_im_orgnr ON inntektsmelding (orgnr);
CREATE INDEX IF NOT EXISTS ix_im_nav_referanse_id ON inntektsmelding (nav_referanse_id);

CREATE INDEX IF NOT EXISTS ix_soknad_orgnr ON soknad (orgnr);

CREATE INDEX IF NOT EXISTS ix_sm_orgnr ON sykmelding (orgnr);
