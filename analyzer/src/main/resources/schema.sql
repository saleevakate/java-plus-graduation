CREATE TABLE IF NOT EXISTS user_action_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    last_action_time TIMESTAMP NOT NULL,
    UNIQUE(user_id, event_id)
);

CREATE TABLE IF NOT EXISTS event_similarities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(event_a, event_b)
);

CREATE INDEX IF NOT EXISTS idx_user_action_history_user_id ON user_action_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_action_history_event_id ON user_action_history(event_id);
CREATE INDEX IF NOT EXISTS idx_event_similarities_event_a ON event_similarities(event_a);
CREATE INDEX IF NOT EXISTS idx_event_similarities_event_b ON event_similarities(event_b);