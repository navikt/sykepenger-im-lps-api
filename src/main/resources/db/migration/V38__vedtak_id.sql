ALTER TABLE vedtak
    ADD COLUMN vedtak_id UUID NOT NULL DEFAULT gen_random_uuid();
