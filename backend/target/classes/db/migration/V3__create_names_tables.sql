ALTER TABLE lists ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE names (
    id BIGSERIAL PRIMARY KEY,
    list_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_names_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_names_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_names_list_user_normalized UNIQUE (list_id, user_id, normalized_name)
);

CREATE TABLE shared_name_pool (
    id BIGSERIAL PRIMARY KEY,
    list_id BIGINT NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    adopted_by BIGINT NOT NULL,
    adopted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shared_pool_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_pool_user FOREIGN KEY (adopted_by) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_shared_pool_list_normalized UNIQUE (list_id, normalized_name)
);

CREATE INDEX idx_names_list_id ON names (list_id);
CREATE INDEX idx_names_user_id ON names (user_id);
CREATE INDEX idx_shared_pool_list_id ON shared_name_pool (list_id);
