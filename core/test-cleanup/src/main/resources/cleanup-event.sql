SET session_replication_role = 'replica';
TRUNCATE TABLE compilation_events CASCADE;
TRUNCATE TABLE compilations CASCADE;
TRUNCATE TABLE events CASCADE;
TRUNCATE TABLE categories CASCADE;
SET session_replication_role = 'origin';