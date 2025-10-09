DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'sykepenger-im-lps-api')
        THEN
            ALTER USER "sykepenger-im-lps-api" WITH REPLICATION;
        END IF;
    END
$$;
DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'lps_api_datastream_bruker')
        THEN
            ALTER USER "lps_api_datastream_bruker" WITH REPLICATION;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "lps_api_datastream_bruker";
            GRANT USAGE ON SCHEMA public TO "lps_api_datastream_bruker";
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "lps_api_datastream_bruker";
        END IF;
    END
$$;
