-- Kopier naturalytelser fra inntekt til rot-nivå, eller opprett tom liste dersom inntekt mangler
UPDATE inntektsmelding
    SET skjema = jsonb_set(
            skjema,
            '{naturalytelser}',
            COALESCE(
                    skjema->'inntekt'->'naturalytelser',
                    '[]'::jsonb
            )
     )
WHERE skjema IS NOT NULL;
