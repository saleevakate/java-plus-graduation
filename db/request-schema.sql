CREATE TABLE IF NOT EXISTS requests (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT,
    requester_id BIGINT,
    created TIMESTAMP,
    status VARCHAR(50)
);