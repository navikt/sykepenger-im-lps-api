CREATE TABLE forespoersel
(
    forespoersel_id      VARCHAR(40) PRIMARY KEY,
    opprettet TIMESTAMP   NOT NULL DEFAULT now(),
    orgnr VARCHAR(12) NOT NULL,
    fnr VARCHAR(11) NOT NULL,
    status VARCHAR(20)
);
