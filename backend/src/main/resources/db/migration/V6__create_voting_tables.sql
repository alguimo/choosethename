ALTER TABLE lists ADD COLUMN current_round INTEGER NOT NULL DEFAULT 1;
ALTER TABLE lists ADD COLUMN total_rounds INTEGER NOT NULL DEFAULT 1;

CREATE TABLE voting_rounds (
    id BIGSERIAL PRIMARY KEY,
    list_id BIGINT NOT NULL,
    round_number INTEGER NOT NULL,
    pool_rankings TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_voting_rounds_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT uk_voting_rounds_list_round UNIQUE (list_id, round_number)
);

CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    list_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    rankings TEXT NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_votes_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_round FOREIGN KEY (round_id) REFERENCES voting_rounds (id) ON DELETE CASCADE,
    CONSTRAINT uk_votes_list_user_round UNIQUE (list_id, user_id, round_id)
);

CREATE INDEX idx_voting_rounds_list_id ON voting_rounds (list_id);
CREATE INDEX idx_votes_list_id ON votes (list_id);
CREATE INDEX idx_votes_user_id ON votes (user_id);
CREATE INDEX idx_votes_round_id ON votes (round_id);