SET session_replication_role = 'replica';
TRUNCATE TABLE users CASCADE;
SET session_replication_role = 'origin';