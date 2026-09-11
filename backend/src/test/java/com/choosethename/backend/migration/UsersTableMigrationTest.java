package com.choosethename.backend.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UsersTableMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should create users table with required columns")
    void shouldCreateUsersTableWithRequiredColumns() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        assertThat(count).isNotNull().isZero();
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on username in users table")
    void shouldEnforceUniqueConstraintOnUsername() {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            "admin", "hashed_pass_123", "ADMIN"
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                "admin", "another_hash_456", "PARTICIPANT"
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
