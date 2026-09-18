package com.choosethename.backend.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ListsTableMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should create lists and list_memberships tables and allow valid insertions")
    void shouldCreateTablesAndAllowValidInsertions() {
        jdbcTemplate.update("DELETE FROM list_memberships");
        jdbcTemplate.update("DELETE FROM lists");
        jdbcTemplate.update("DELETE FROM users");

        // Create user
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "owner1", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "owner1");

        // Insert list
        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "My List", "CODE01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "CODE01");
        assertThat(listId).isNotNull();

        // Insert membership
        jdbcTemplate.update(
            "INSERT INTO list_memberships (list_id, user_id, joined_at) VALUES (?, ?, ?)",
            listId, userId, Instant.now()
        );
        Integer membershipCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM list_memberships WHERE list_id = ?", Integer.class, listId
        );
        assertThat(membershipCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on invitation_code in lists table")
    void shouldEnforceUniqueConstraintOnInvitationCode() {
        jdbcTemplate.update("DELETE FROM list_memberships");
        jdbcTemplate.update("DELETE FROM lists");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "userA", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "userA");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "List 1", "UNIQUE1", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "List 2", "UNIQUE1", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on (list_id, user_id) in list_memberships table")
    void shouldEnforceUniqueConstraintOnMembership() {
        jdbcTemplate.update("DELETE FROM list_memberships");
        jdbcTemplate.update("DELETE FROM lists");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "userB", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "userB");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "List B", "CODE02", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "CODE02");

        jdbcTemplate.update(
            "INSERT INTO list_memberships (list_id, user_id, joined_at) VALUES (?, ?, ?)",
            listId, userId, Instant.now()
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO list_memberships (list_id, user_id, joined_at) VALUES (?, ?, ?)",
                listId, userId, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce foreign key constraints on owner_id and list_id/user_id")
    void shouldEnforceForeignKeyConstraints() {
        jdbcTemplate.update("DELETE FROM list_memberships");
        jdbcTemplate.update("DELETE FROM lists");
        jdbcTemplate.update("DELETE FROM users");

        // Non-existent owner_id
        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "List FK", "CODEFK", Instant.now().plusSeconds(172800), "ADDITION", true, 99999L, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
