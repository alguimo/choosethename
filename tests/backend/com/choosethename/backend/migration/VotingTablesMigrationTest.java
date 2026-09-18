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
class VotingTablesMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM votes");
        jdbcTemplate.update("DELETE FROM voting_rounds");
        jdbcTemplate.update("DELETE FROM shared_name_pool");
        jdbcTemplate.update("DELETE FROM names");
        jdbcTemplate.update("DELETE FROM list_memberships");
        jdbcTemplate.update("DELETE FROM lists");
        jdbcTemplate.update("DELETE FROM users");
    }

    private Long createUser(String username) {
        jdbcTemplate.update(
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
            username, "hash123", "PARTICIPANT"
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private Long createList(Long ownerId) {
        jdbcTemplate.update(
            "INSERT INTO lists (name, invitation_code, code_expires_at, phase, invitations_open, owner_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            "Test List", "VOTE01", Instant.now().plusSeconds(172800), "VOTING", true, ownerId, Instant.now()
        );
        return jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "VOTE01");
    }

    @Test
    @DisplayName("Should add current_round column to lists with default value 1")
    void shouldAddCurrentRoundColumnToLists() {
        Long userId = createUser("crUser1");
        Long listId = createList(userId);

        Integer currentRound = jdbcTemplate.queryForObject(
            "SELECT current_round FROM lists WHERE id = ?", Integer.class, listId
        );
        assertThat(currentRound).isNotNull().isEqualTo(1);
    }

    @Test
    @DisplayName("Should add total_rounds column to lists with default value 1")
    void shouldAddTotalRoundsColumnToLists() {
        Long userId = createUser("trUser1");
        Long listId = createList(userId);

        Integer totalRounds = jdbcTemplate.queryForObject(
            "SELECT total_rounds FROM lists WHERE id = ?", Integer.class, listId
        );
        assertThat(totalRounds).isNotNull().isEqualTo(1);
    }

    @Test
    @DisplayName("Should allow updating current_round and total_rounds on lists")
    void shouldAllowUpdatingRoundColumnsOnLists() {
        Long userId = createUser("roundCols");
        Long listId = createList(userId);

        jdbcTemplate.update("UPDATE lists SET current_round = 2, total_rounds = 3 WHERE id = ?", listId);

        Integer currentRound = jdbcTemplate.queryForObject(
            "SELECT current_round FROM lists WHERE id = ?", Integer.class, listId
        );
        Integer totalRounds = jdbcTemplate.queryForObject(
            "SELECT total_rounds FROM lists WHERE id = ?", Integer.class, listId
        );
        assertThat(currentRound).isEqualTo(2);
        assertThat(totalRounds).isEqualTo(3);
    }

    @Test
    @DisplayName("Should create voting_rounds table and allow valid insertion")
    void shouldCreateVotingRoundsTableAndAllowValidInsertion() {
        Long userId = createUser("vrUser1");
        Long listId = createList(userId);

        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM voting_rounds WHERE list_id = ?", Integer.class, listId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on (list_id, round_number) in voting_rounds")
    void shouldEnforceUniqueConstraintOnVotingRounds() {
        Long userId = createUser("vrUnique");
        Long listId = createList(userId);

        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
                listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should create votes table and allow valid insertion")
    void shouldCreateVotesTableAndAllowValidInsertion() {
        Long userId = createUser("voteUser1");
        Long listId = createList(userId);

        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
        );
        Long roundId = jdbcTemplate.queryForObject(
            "SELECT id FROM voting_rounds WHERE list_id = ? AND round_number = ?", Long.class, listId, 1
        );

        jdbcTemplate.update(
            "INSERT INTO votes (list_id, user_id, round_id, rankings, submitted_at) VALUES (?, ?, ?, ?, ?)",
            listId, userId, roundId, "[\"ana\",\"pablo\"]", Instant.now()
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM votes WHERE list_id = ? AND user_id = ?", Integer.class, listId, userId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on (list_id, user_id, round_id) in votes")
    void shouldEnforceUniqueConstraintOnVotes() {
        Long userId = createUser("voteUnique");
        Long listId = createList(userId);

        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
        );
        Long roundId = jdbcTemplate.queryForObject(
            "SELECT id FROM voting_rounds WHERE list_id = ? AND round_number = ?", Long.class, listId, 1
        );

        jdbcTemplate.update(
            "INSERT INTO votes (list_id, user_id, round_id, rankings, submitted_at) VALUES (?, ?, ?, ?, ?)",
            listId, userId, roundId, "[\"ana\",\"pablo\"]", Instant.now()
        );

        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO votes (list_id, user_id, round_id, rankings, submitted_at) VALUES (?, ?, ?, ?, ?)",
                listId, userId, roundId, "[\"pablo\",\"ana\"]", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce foreign key on voting_rounds.list_id")
    void shouldEnforceForeignKeyOnVotingRounds() {
        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO voting_rounds (list_id, round_number, created_at) VALUES (?, ?, ?)",
                99999L, 1, Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce foreign keys on votes.list_id, votes.user_id, votes.round_id")
    void shouldEnforceForeignKeysOnVotes() {
        Long userId = createUser("fkVoteUser");
        Long listId = createList(userId);

        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
        );
        Long roundId = jdbcTemplate.queryForObject(
            "SELECT id FROM voting_rounds WHERE list_id = ? AND round_number = ?", Long.class, listId, 1
        );

        // Invalid list_id
        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO votes (list_id, user_id, round_id, rankings, submitted_at) VALUES (?, ?, ?, ?, ?)",
                99999L, userId, roundId, "[\"ana\"]", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Invalid user_id
        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO votes (list_id, user_id, round_id, rankings, submitted_at) VALUES (?, ?, ?, ?, ?)",
                listId, 99999L, roundId, "[\"ana\"]", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Invalid round_id
        assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO votes (list_id, user_id, round_id, rankings, submitted_at) VALUES (?, ?, ?, ?, ?)",
                listId, userId, 99999L, "[\"ana\"]", Instant.now()
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should allow multiple rounds per list with different round_number")
    void shouldAllowMultipleRoundsPerList() {
        Long userId = createUser("multiRound");
        Long listId = createList(userId);

        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 1, "[\"ana\",\"pablo\"]", Instant.now()
        );
        jdbcTemplate.update(
            "INSERT INTO voting_rounds (list_id, round_number, pool_rankings, created_at) VALUES (?, ?, ?, ?)",
            listId, 2, "[\"pablo\"]", Instant.now()
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM voting_rounds WHERE list_id = ?", Integer.class, listId
        );
        assertThat(count).isEqualTo(2);
    }
}
