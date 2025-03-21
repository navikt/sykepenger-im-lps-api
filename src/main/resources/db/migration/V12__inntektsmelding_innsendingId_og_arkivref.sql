ALTER TABLE inntektsmelding ADD COLUMN innsending_id UUID UNIQUE;
ALTER TABLE inntektsmelding ADD COLUMN arkivreferanse VARCHAR(15);
