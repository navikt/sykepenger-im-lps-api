ALTER TABLE inntektsmelding ADD COLUMN skjema JSONB NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN aarsak_innsending VARCHAR(7) NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN type_innsending VARCHAR(21) NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN avsender_system_navn VARCHAR(32) NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN avsender_system_versjon VARCHAR(10) NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN nav_referanse_id UUID NOT NULL; --- Skal erstatte forespørsel_id (Type.ID)
ALTER TABLE inntektsmelding ADD COLUMN versjon INT NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN status VARCHAR(15) NOT NULL;
ALTER TABLE inntektsmelding ADD COLUMN status_melding VARCHAR(255);
