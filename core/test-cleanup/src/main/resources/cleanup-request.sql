SET session_replication_role = 'replica';
TRUNCATE TABLE requests CASCADE;
SET session_replication_role = 'origin';