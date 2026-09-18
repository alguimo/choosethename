package com.choosethename.backend.migration;

import org.junit.jupiter.api.BeforeEach;
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
class NamesTableMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM shared_name_pool");
        jdbcTemplate.update("DELETE FROM names");
        jdbcTemplate.update("DELETE FROM list_memberships");
        jdbcTemplate.update("DELETE FROM lists");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("Should create names table and allow valid insertion")
    void shouldCreateNamesTableAndAllowValidInsertion() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "nameUser1", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "nameUser1");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "NAM01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "NAM01");

        jdbcTemplate.update(
            "INSERT INTO names (list_id, user_id, name, normalized_name, created_at) VALUES (?, ?, ?, ?, ?)",
            listId, userId, "Maria", "maria", Instant.now()
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM names WHERE list_id = ? AND user_id = ?", Integer.class, listId, userId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create shared_name_pool table and allow valid insertion")
    void shouldCreateSharedNamePoolTableAndAllowValidInsertion() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "poolUser1", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "poolUser1");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "SHP01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "SHP01");

        jdbcTemplate.update(
            "INSERT INTO shared_name_pool (list_id, normalized_name, adopted_by, adopted_at) VALUES (?, ?, ?, ?)",
            listId, "maria", userId, Instant.now()
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM shared_name_pool WHERE list_id = ?", Integer.class, listId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should add version column to lists with default value 0")
    void shouldAddVersionColumnToListsWithDefaultValue() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "versionUser1", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "versionUser1");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "VER01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "VER01");

        Integer version = jdbcTemplate.queryForObject(
            "SELECT version FROM lists WHERE id = ?", Integer.class, listId
        );
        assertThat(version).isNotNull().isEqualTo(0);
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on (list_id, user_id, normalized_name) in names")
    void shouldEnforceUniqueConstraintOnNames() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "uniqueNameUser", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "uniqueNameUser");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "UQN01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "UQN01");

        jdbcTemplate.update(
            "INSERT INTO names (list_id, user_id, name, normalized_name, created_at) VALUES (?, ?, ?, ?, ?)",
            listId, userId, "Pedro", "pedro", Instant.now()
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO names (list_id, user_id, name, normalized_name, created_at) VALUES (?, ?, ?, ?, ?)",
                listId, userId, "Pedro", "pedro", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on (list_id, normalized_name) in shared_name_pool")
    void shouldEnforceUniqueConstraintOnSharedNamePool() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "uniquePoolUser", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "uniquePoolUser");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "UQP01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "UQP01");

        jdbcTemplate.update(
            "INSERT INTO shared_name_pool (list_id, normalized_name, adopted_by, adopted_at) VALUES (?, ?, ?, ?)",
            listId, "ana", userId, Instant.now()
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO shared_name_pool (list_id, normalized_name, adopted_by, adopted_at) VALUES (?, ?, ?, ?)",
                listId, "ana", userId, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce foreign keys on names table")
    void shouldEnforceForeignKeysOnNames() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "fkNamesUser", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "fkNamesUser");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "FKN01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "FKN01");

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO names (list_id, user_id, name, normalized_name, created_at) VALUES (?, ?, ?, ?, ?)",
                99999L, userId, "Luis", "luis", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO names (list_id, user_id, name, normalized_name, created_at) VALUES (?, ?, ?, ?, ?)",
                listId, 99999L, "Luis", "luis", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce foreign keys on shared_name_pool table")
    void shouldEnforceForeignKeysOnSharedNamePool() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "fkPoolUser", "hash123", "PARTICIPANT"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "fkPoolUser");

        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "FKP01", Instant.now().plusSeconds(172800), "ADDITION", true, userId, Instant.now()
        );
        Long listId = jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "FKP01");

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO shared_name_pool (list_id, normalized_name, adopted_by, adopted_at) VALUES (?, ?, ?, ?)",
                99999L, "luis", userId, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO shared_name_pool (list_id, normalized_name, adopted_by, adopted_at) VALUES (?, ?, ?, ?)",
                listId, "luis", 99999L, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
