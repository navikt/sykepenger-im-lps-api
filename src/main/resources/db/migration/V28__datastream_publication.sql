DO
$$
    BEGIN
        if not exists
            (select 1 from pg_publication where pubname = 'lps_api_publication')
        then
            CREATE PUBLICATION lps_api_publication for ALL TABLES;
        end if;
    end;
$$;
