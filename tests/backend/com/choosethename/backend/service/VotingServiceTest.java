package com.choosethename.backend.service;

import com.choosethename.backend.dto.VoteJsonCodec;
import com.choosethename.backend.dto.VoteRequestDTO;
import com.choosethename.backend.exception.DuplicateNameException;
import com.choosethename.backend.exception.ListAccessDeniedException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.exception.StaleVoteException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.SharedNamePoolEntity;
import com.choosethename.backend.model.User;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.VoteEntity;
import com.choosethename.backend.model.VotingRoundEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VoteRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import com.choosethename.backend.dto.ResultsResponseDTO;
import com.choosethename.backend.exception.ResultsNotReadyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class VotingServiceTest {

    @Autowired
    private VotingService votingService;
    @Autowired
    private ListRepository listRepository;
    @Autowired
    private ListMembershipRepository membershipRepository;
    @Autowired
    private SharedNamePoolRepository sharedNamePoolRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VoteRepository voteRepository;
    @Autowired
    private VotingRoundRepository votingRoundRepository;
    @Autowired
    private RankingService rankingService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User userA;
    private User userB;
    private ListEntity list;
    private List<String> pool;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM votes");
        jdbcTemplate.execute("DELETE FROM voting_rounds");
        jdbcTemplate.execute("DELETE FROM shared_name_pool");
        jdbcTemplate.execute("DELETE FROM names");
        jdbcTemplate.execute("DELETE FROM list_memberships");
        jdbcTemplate.execute("DELETE FROM lists");
        jdbcTemplate.execute("DELETE FROM users");

        userA = createUser("voterA");
        userB = createUser("voterB");
    }

    private void createVotingList(List<String> poolNames) {
        createVotingList(poolNames, 1, 2);
    }

    private void createVotingList(List<String> poolNames, int currentRound, int totalRounds) {
        pool = List.copyOf(poolNames);
        list = new ListEntity();
        list.setName("Voting List");
        list.setOwnerId(userA.getId());
        list.setInvitationCode("VO" + System.nanoTime() % 100000);
        list.setPhase(ListPhase.VOTING);
        list.setInvitationsOpen(false);
        list.setCodeExpiresAt(Instant.now().plusSeconds(48 * 3600));
        list.setCurrentRound(currentRound);
        list.setTotalRounds(totalRounds);
        list.setCreatedAt(Instant.now());
        list = listRepository.save(list);

        for (String name : pool) {
            SharedNamePoolEntity entry = new SharedNamePoolEntity();
            entry.setListId(list.getId());
            entry.setNormalizedName(name);
            entry.setAdoptedBy(userA.getId());
            entry.setAdoptedAt(Instant.now());
            sharedNamePoolRepository.save(entry);
        }
        addMembership(list.getId(), userA.getId());
        addMembership(list.getId(), userB.getId());

        VotingRoundEntity round = new VotingRoundEntity();
        round.setListId(list.getId());
        round.setRoundNumber(currentRound);
        round.setPoolRankings(VoteJsonCodec.encode(currentRound == 1 ? pool : pool.subList(0, Math.min(3, pool.size()))));
        round.setCreatedAt(Instant.now());
        votingRoundRepository.save(round);
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + System.nanoTime());
        user.setPasswordHash("password");
        user.setRole(Role.PARTICIPANT);
        return userRepository.save(user);
    }

    private void addMembership(Long listId, Long userId) {
        com.choosethename.backend.model.ListMembershipEntity membership =
                new com.choosethename.backend.model.ListMembershipEntity();
        membership.setListId(listId);
        membership.setUserId(userId);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);
    }

    private VoteRequestDTO request(int round, String... ranking) {
        VoteRequestDTO dto = new VoteRequestDTO();
        dto.setRoundNumber(round);
        dto.setRankings(List.of(ranking));
        return dto;
    }

    private Long roundId(int round) {
        return votingRoundRepository.findByListIdAndRoundNumber(list.getId(), round)
                .orElseThrow().getId();
    }

    private ListEntity reload() {
        return listRepository.findById(list.getId()).orElseThrow();
    }

    @Test
    @DisplayName("P1: a partial set of votes does not advance the round")
    void partialVotesDoNotAdvanceRound() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo", "maria"));

        ListEntity updated = reload();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.VOTING);
        assertThat(updated.getCurrentRound()).isEqualTo(1);
        assertThat(voteRepository.countByListIdAndRoundId(list.getId(), roundId(1))).isEqualTo(1);
    }

    @Test
    @DisplayName("P2: when all members vote the list advances to the next round")
    void allVotesAdvanceToNextRound() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo", "maria"));
        votingService.submitVote(list.getId(), userB.getId(), request(1, "maria", "ana", "luis", "pablo"));

        ListEntity updated = reload();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.VOTING);
        assertThat(updated.getCurrentRound()).isEqualTo(2);

        VotingRoundEntity roundTwo = votingRoundRepository.findByListIdAndRoundNumber(list.getId(), 2).orElseThrow();
        assertThat(VoteJsonCodec.decode(roundTwo.getPoolRankings())).hasSize(4);
    }

    @Test
    @DisplayName("P3: when all members vote on the last round the list is completed")
    void allVotesOnLastRoundCompletes() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"), 2, 2);

        votingService.submitVote(list.getId(), userA.getId(), request(2, "ana", "luis", "pablo"));
        votingService.submitVote(list.getId(), userB.getId(), request(2, "pablo", "ana", "luis"));

        ListEntity updated = reload();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.COMPLETED);
        assertThat(updated.getCurrentRound()).isEqualTo(2);
    }

    @Test
    @DisplayName("P4: a re-vote in the same round overwrites the previous ranking")
    void revoteOverwritesPreviousRanking() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo", "maria"));
        votingService.submitVote(list.getId(), userA.getId(), request(1, "maria", "pablo", "luis", "ana"));

        assertThat(voteRepository.countByListIdAndRoundId(list.getId(), roundId(1))).isEqualTo(1);
        VoteEntity vote = voteRepository.findByListIdAndUserIdAndRoundId(list.getId(), userA.getId(), roundId(1)).orElseThrow();
        assertThat(VoteJsonCodec.decode(vote.getRankings())).containsExactly("maria", "pablo", "luis", "ana");
    }

    @Test
    @DisplayName("P5: a vote for an already-advanced round is rejected as stale")
    void staleVoteRejected() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo", "maria"));
        votingService.submitVote(list.getId(), userB.getId(), request(1, "maria", "ana", "luis", "pablo"));
        assertThat(reload().getCurrentRound()).isEqualTo(2);

        assertThatThrownBy(() -> votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo", "maria")))
                .isInstanceOf(StaleVoteException.class);
    }

    @Test
    @DisplayName("P6: future round and missing round number are rejected")
    void invalidRoundRejected() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        assertThatThrownBy(() -> votingService.submitVote(list.getId(), userA.getId(), request(3, "ana", "luis", "pablo", "maria")))
                .isInstanceOf(ListOperationException.class);

        VoteRequestDTO noRound = new VoteRequestDTO();
        noRound.setRankings(List.of("ana", "luis", "pablo", "maria"));
        assertThatThrownBy(() -> votingService.submitVote(list.getId(), userA.getId(), noRound))
                .isInstanceOf(ListOperationException.class);
    }

    @Test
    @DisplayName("P7: an incomplete ranking and a ranking with unknown names are rejected")
    void incompleteRankingRejected() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        assertThatThrownBy(() -> votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo")))
                .isInstanceOf(ListOperationException.class);
        assertThatThrownBy(() -> votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "luis", "pablo", "xena")))
                .isInstanceOf(ListOperationException.class);
    }

    @Test
    @DisplayName("P8: a ranking with duplicate names is rejected")
    void duplicateRankingRejected() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));

        assertThatThrownBy(() -> votingService.submitVote(list.getId(), userA.getId(), request(1, "ana", "ana", "pablo", "maria")))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    @DisplayName("P9: a non-member cannot vote")
    void nonMemberCannotVote() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"));
        User outsider = createUser("outsider");

        assertThatThrownBy(() -> votingService.submitVote(list.getId(), outsider.getId(), request(1, "ana", "luis", "pablo", "maria")))
                .isInstanceOf(ListAccessDeniedException.class);
    }

    @Test
    @DisplayName("P10: concurrent final triggers yield one transition and one optimistic lock conflict")
    void concurrentFinalTriggerYieldsOneSuccessAndOneConflict() throws Exception {
        createVotingList(List.of("ana", "luis", "pablo", "maria"), 2, 2);

        Long roundId = votingRoundRepository.findByListIdAndRoundNumber(list.getId(), 2).orElseThrow().getId();
        saveVote(userA.getId(), roundId, "ana", "luis", "pablo");
        saveVote(userB.getId(), roundId, "luis", "pablo", "ana");

        CountDownLatch loserLoaded = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> loser = executor.submit(() ->
            transactionTemplate.executeWithoutResult(status -> {
                ListEntity stale = listRepository.findById(list.getId()).orElseThrow();
                loserLoaded.countDown();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (stale.getPhase() != ListPhase.COMPLETED) {
                    stale.setPhase(ListPhase.COMPLETED);
                }
            })
        );

        try {
            assertThat(loserLoaded.await(5, TimeUnit.SECONDS)).isTrue();
            votingService.submitVote(list.getId(), userA.getId(), request(2, "ana", "luis", "pablo"));
        } finally {
            executor.shutdownNow();
        }

        assertThat(reload().getPhase()).isEqualTo(ListPhase.COMPLETED);
        assertThatThrownBy(loser::get).hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("P11: total rounds and caps are derived from the pool size for a small pool")
    void smallPoolRoundConfiguration() {
        assertThat(rankingService.totalRoundsFor(4)).isEqualTo(2);
        assertThat(rankingService.capForRound(1, 2)).isEqualTo(5);
        assertThat(rankingService.capForRound(2, 2)).isEqualTo(3);
    }

    @Test
    @DisplayName("P12: results expose the ordered top-3 with consolidated scores")
    void resultsExposeOrderedTopThree() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"), 2, 2);

        votingService.submitVote(list.getId(), userA.getId(), request(2, "ana", "luis", "pablo"));
        votingService.submitVote(list.getId(), userB.getId(), request(2, "luis", "pablo", "ana"));
        assertThat(reload().getPhase()).isEqualTo(ListPhase.COMPLETED);

        List<ResultsResponseDTO.ResultEntry> results = votingService.getResults(list.getId(), userA.getId()).getResults();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getName()).isEqualTo("luis");
        assertThat(results.get(0).getScore()).isEqualTo(3);
        assertThat(results.get(1).getName()).isEqualTo("ana");
        assertThat(results.get(1).getScore()).isEqualTo(2);
        assertThat(results.get(2).getName()).isEqualTo("pablo");
        assertThat(results.get(0).getRank()).isEqualTo(1);
        assertThat(results.get(2).getRank()).isEqualTo(3);
    }

    @Test
    @DisplayName("P13: results degrade when fewer than 3 names remain")
    void resultsExposeFewerThanThree() {
        createVotingList(List.of("ana", "luis"), 2, 2);

        votingService.submitVote(list.getId(), userA.getId(), request(2, "ana", "luis"));
        votingService.submitVote(list.getId(), userB.getId(), request(2, "luis", "ana"));

        assertThat(votingService.getResults(list.getId(), userA.getId()).getResults()).hasSize(2);
    }

    @Test
    @DisplayName("P14: non-members cannot read results")
    void nonMemberCannotReadResults() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"), 2, 2);
        User outsider = createUser("resultsOutsider");

        assertThatThrownBy(() -> votingService.getResults(list.getId(), outsider.getId()))
                .isInstanceOf(ListAccessDeniedException.class);
    }

    @Test
    @DisplayName("P15: results are not available while the list is not completed")
    void resultsUnavailableWhileVoting() {
        createVotingList(List.of("ana", "luis", "pablo", "maria"), 2, 2);

        assertThatThrownBy(() -> votingService.getResults(list.getId(), userA.getId()))
                .isInstanceOf(ResultsNotReadyException.class);
    }

    private void saveVote(Long userId, Long roundId, String... ranking) {
        VoteEntity vote = new VoteEntity();
        vote.setListId(list.getId());
        vote.setUserId(userId);
        vote.setRoundId(roundId);
        vote.setRankings(VoteJsonCodec.encode(List.of(ranking)));
        vote.setSubmittedAt(Instant.now());
        voteRepository.save(vote);
    }
}