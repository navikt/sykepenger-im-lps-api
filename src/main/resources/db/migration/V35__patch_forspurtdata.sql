UPDATE forespoersel
SET arbeidsgiverperiode_paakrevd = (dokument::jsonb -> 'forespurtData' -> 'arbeidsgiverperiode' ->> 'paakrevd')::boolean,
    inntekt_paakrevd = (dokument::jsonb -> 'forespurtData' -> 'inntekt' ->> 'paakrevd')::boolean
where arbeidsgiverperiode_paakrevd is null
  and inntekt_paakrevd is null
  and dokument::jsonb ? 'forespurtData';
