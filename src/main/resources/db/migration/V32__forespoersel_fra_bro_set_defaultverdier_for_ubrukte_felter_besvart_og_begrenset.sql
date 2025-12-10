-- Sett defaultverdier for ubrukte forespoerselFelter som kommer fra Bro (ble ignorert fra parsing tidligere)

UPDATE forespoersel
SET dokument = dokument::jsonb || '{"bestemmendeFravaersdager": {}}'::jsonb
where dokument::jsonb -> 'bestemmendeFravaersdager' is null;

UPDATE forespoersel
SET dokument = dokument::jsonb || '{"erBesvart": false}'::jsonb;

UPDATE forespoersel
SET dokument = dokument::jsonb || '{"erBegrenset": false}'::jsonb;

UPDATE forespoersel
SET dokument = jsonb_set(
        dokument::jsonb,
        '{forespurtData, inntekt, forslag}',
        'null'::jsonb,
        true
               );


UPDATE forespoersel
SET dokument = jsonb_set(
        dokument::jsonb,
        '{forespurtData, refusjon, forslag}',
        '{"perioder": [], "opphoersdato": null}'::jsonb,
        true
               );

