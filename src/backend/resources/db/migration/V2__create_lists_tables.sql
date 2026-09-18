CREATE TABLE lists (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    invitation_code VARCHAR(10) NOT NULL,
    code_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    phase VARCHAR(50) NOT NULL,
    invitations_open BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lists_invitation_code UNIQUE (invitation_code),
    CONSTRAINT fk_lists_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE list_memberships (
    id BIGSERIAL PRIMARY KEY,
    list_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_list_memberships_list_user UNIQUE (list_id, user_id),
    CONSTRAINT fk_list_memberships_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_list_memberships_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_lists_invitation_code ON lists (invitation_code);
CREATE INDEX idx_list_memberships_user_id ON list_memberships (user_id);
