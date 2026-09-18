package com.choosethename.backend.repository;

import com.choosethename.backend.model.SharedNamePoolEntity;
import com.choosethename.backend.model.VoteEntity;
import com.choosethename.backend.model.VotingRoundEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VotingRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VotingRoundRepository votingRoundRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private SharedNamePoolRepository sharedNamePoolRepository;

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
            "Test List", "VOTREPO", Instant.now().plusSeconds(172800), "VOTING", true, ownerId, Instant.now()
        );
        return jdbcTemplate.queryForObject("SELECT id FROM lists WHERE invitation_code = ?", Long.class, "VOTREPO");
    }

    private Long createRound(Long listId, int roundNumber, String poolRankings) {
        VotingRoundEntity round = new VotingRoundEntity();
        round.setListId(listId);
        round.setRoundNumber(roundNumber);
        round.setPoolRankings(poolRankings);
        round.setCreatedAt(Instant.now());
        return votingRoundRepository.save(round).getId();
    }

    @Test
    @DisplayName("should find voting rounds by list and round number")
    void shouldFindRoundByListAndNumber() {
        Long userId = createUser("repoUser");
        Long listId = createList(userId);
        createRound(listId, 1, "[\"ana\",\"luis\"]");
        createRound(listId, 2, "[\"ana\"]");

        VotingRoundEntity roundTwo = votingRoundRepository.findByListIdAndRoundNumber(listId, 2)
                .orElseThrow();
        assertThat(roundTwo.getListId()).isEqualTo(listId);
        assertThat(roundTwo.getRoundNumber()).isEqualTo(2);
        assertThat(roundTwo.getPoolRankings()).isEqualTo("[\"ana\"]");
    }

    @Test
    @DisplayName("should find the latest round for a list")
    void shouldFindLatestRound() {
        Long userId = createUser("repoUser2");
        Long listId = createList(userId);
        createRound(listId, 1, "[\"ana\",\"luis\"]");
        createRound(listId, 2, "[\"ana\"]");

        VotingRoundEntity latest = votingRoundRepository.findFirstByListIdOrderByRoundNumberDesc(listId)
                .orElseThrow();
        assertThat(latest.getRoundNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("should find all rounds for a list ordered by round number")
    void shouldFindAllRoundsForList() {
        Long userId = createUser("repoUser3");
        Long listId = createList(userId);
        createRound(listId, 1, "[\"ana\",\"luis\"]");
        createRound(listId, 2, "[\"ana\"]");

        assertThat(votingRoundRepository.findByListId(listId))
                .extracting(VotingRoundEntity::getRoundNumber)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("should find a vote by list, user and round")
    void shouldFindVoteByListUserAndRound() {
        Long userId = createUser("repoVoter");
        Long listId = createList(userId);
        Long roundId = createRound(listId, 1, "[\"ana\",\"luis\"]");

        VoteEntity vote = new VoteEntity();
        vote.setListId(listId);
        vote.setUserId(userId);
        vote.setRoundId(roundId);
        vote.setRankings("[\"ana\",\"luis\"]");
        vote.setSubmittedAt(Instant.now());
        voteRepository.save(vote);

        VoteEntity found = voteRepository.findByListIdAndUserIdAndRoundId(listId, userId, roundId)
                .orElseThrow();
        assertThat(found.getRankings()).isEqualTo("[\"ana\",\"luis\"]");
    }

    @Test
    @DisplayName("should find all votes for a list and round")
    void shouldFindAllVotesForListAndRound() {
        Long userA = createUser("repoA");
        Long userB = createUser("repoB");
        Long listId = createList(userA);
        Long roundId = createRound(listId, 1, "[\"ana\",\"luis\"]");

        saveVote(listId, userA, roundId, "[\"ana\",\"luis\"]");
        saveVote(listId, userB, roundId, "[\"luis\",\"ana\"]");

        assertThat(voteRepository.findByListIdAndRoundId(listId, roundId)).hasSize(2);
        assertThat(voteRepository.countByListIdAndRoundId(listId, roundId)).isEqualTo(2);
    }

    @Test
    @DisplayName("should count pool names for a list")
    void shouldCountPoolNamesForList() {
        Long userId = createUser("repoPool");
        Long listId = createList(userId);

        insertPoolName(listId, "ana", userId);
        insertPoolName(listId, "luis", userId);

        assertThat(sharedNamePoolRepository.countByListId(listId)).isEqualTo(2);
        assertThat(sharedNamePoolRepository.findByListIdOrderByIdAsc(listId))
                .extracting(SharedNamePoolEntity::getNormalizedName)
                .containsExactly("ana", "luis");
    }

    private void saveVote(Long listId, Long userId, Long roundId, String rankings) {
        VoteEntity vote = new VoteEntity();
        vote.setListId(listId);
        vote.setUserId(userId);
        vote.setRoundId(roundId);
        vote.setRankings(rankings);
        vote.setSubmittedAt(Instant.now());
        voteRepository.save(vote);
    }

    private void insertPoolName(Long listId, String normalizedName, Long adoptedBy) {
        jdbcTemplate.update(
            "INSERT INTO shared_name_pool (list_id, normalized_name, adopted_by, adopted_at) VALUES (?, ?, ?, ?)",
            listId, normalizedName, adoptedBy, Instant.now()
        );
    }
}