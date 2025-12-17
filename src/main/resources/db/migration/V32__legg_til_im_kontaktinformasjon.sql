ALTER TABLE inntektsmelding ADD COLUMN kontaktinformasjon VARCHAR(64);
ALTER TABLE inntektsmelding ALTER avsender_system_navn TYPE VARCHAR(64);
ALTER TABLE inntektsmelding ALTER avsender_system_versjon TYPE VARCHAR(64);
