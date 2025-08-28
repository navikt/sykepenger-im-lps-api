CREATE INDEX ix_fsp_orgnr ON forespoersel (orgnr);
CREATE INDEX ix_fsp_nav_referanse_id ON forespoersel (nav_referanse_id);

CREATE INDEX ix_im_orgnr ON inntektsmelding (orgnr);
CREATE INDEX ix_im_nav_referanse_id ON inntektsmelding (nav_referanse_id);
CREATE INDEX ix_im_innsending_id_orgnr ON inntektsmelding (innsending_id, orgnr);

CREATE INDEX ix_soknad_orgnr ON soknad (orgnr);
CREATE INDEX ix_soknad_id ON soknad (soknad_id);

CREATE INDEX ix_sm_orgnr ON sykmelding (orgnr);
CREATE INDEX ix_sm_id ON sykmelding (sykmelding_id);
