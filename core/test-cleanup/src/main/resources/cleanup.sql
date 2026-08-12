SET session_replication_role = 'replica';

TRUNCATE TABLE compilation_events CASCADE;
TRUNCATE TABLE requests CASCADE;
TRUNCATE TABLE events CASCADE;
TRUNCATE TABLE compilations CASCADE;
TRUNCATE TABLE categories CASCADE;
TRUNCATE TABLE users CASCADE;

SET session_replication_role = 'origin';