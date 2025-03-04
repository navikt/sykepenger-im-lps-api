create table bakgrunnsjobb
(
    jobb_id      uuid unique  not null,
    type         VARCHAR(100) not null,
    behandlet    timestamp,
    opprettet    timestamp    not null,

    status       VARCHAR(50)  not null,
    kjoeretid    timestamp    not null,

    forsoek      int          not null default 0,
    maks_forsoek int          not null,
    data         jsonb
);
CREATE INDEX idx_bgjobb_kjoeretid_status ON BAKGRUNNSJOBB(KJOERETID, STATUS);