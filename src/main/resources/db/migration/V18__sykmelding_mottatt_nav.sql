ALTER TABLE sykmelding ADD COLUMN mottatt_av_nav TIMESTAMP;
UPDATE sykmelding set mottatt_av_nav = opprettet;
ALTER TABLE sykmelding ALTER COLUMN mottatt_av_nav SET NOT NULL;
