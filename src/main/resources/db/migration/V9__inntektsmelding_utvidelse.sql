ALTER TABLE inntektsmelding ADD COLUMN skjema JSONB;
ALTER TABLE inntektsmelding ADD COLUMN aarsak_innsending VARCHAR(7) NOT NULL DEFAULT 'Ny';
ALTER TABLE inntektsmelding ADD COLUMN type_innsending VARCHAR(21) NOT NULL DEFAULT 'FORESPURT';
ALTER TABLE inntektsmelding ADD COLUMN avsender_system_navn VARCHAR(32) NOT NULL DEFAULT 'SIMBA';
ALTER TABLE inntektsmelding ADD COLUMN avsender_system_versjon VARCHAR(10) NOT NULL DEFAULT '1.0';
ALTER TABLE inntektsmelding ADD COLUMN nav_referanse_id UUID;
ALTER TABLE inntektsmelding ADD COLUMN versjon INT NOT NULL DEFAULT 1;
ALTER TABLE inntektsmelding ADD COLUMN status VARCHAR(15) NOT NULL DEFAULT 'MOTTATT';
ALTER TABLE inntektsmelding ADD COLUMN status_melding VARCHAR(255);
