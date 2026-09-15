package com.choosethename.backend.service;

import com.choosethename.backend.dto.AddNameRequestDTO;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ListPhaseTransitionServiceTest {

    @Autowired private ListPhaseTransitionService transitionService;
    @Autowired private NameService nameService;
    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;
    @Autowired private VotingRoundRepository votingRoundRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private User userA;
    private User userB;
    private ListEntity list;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM votes");
        jdbcTemplate.execute("DELETE FROM voting_rounds");
        jdbcTemplate.execute("DELETE FROM shared_name_pool");
        jdbcTemplate.execute("DELETE FROM names");

        userA = createUser("alice");
        userB = createUser("bob");

        list = new ListEntity();
        list.setName("Test List");
        list.setOwnerId(userA.getId());
        list.setInvitationCode("CODE" + System.nanoTime() % 100000);
        list.setPhase(ListPhase.ADDITION);
        list.setInvitationsOpen(true);
        list.setCurrentRound(1);
        list.setTotalRounds(1);
        list.setCodeExpiresAt(Instant.now().plusSeconds(48 * 3600));
        list.setCreatedAt(Instant.now());
        listRepository.save(list);

        addMembership(list.getId(), userA.getId());
        addMembership(list.getId(), userB.getId());
    }

    @Test
    @DisplayName("Should transition ADDITION to SELECTION when both participants finish")
    void shouldTransitionToSelectionWhenBothFinished() {
        addName(userA.getId(), "Pablo");
        addName(userB.getId(), "Lucia");

        nameService.finishAddition(list.getId(), userA.getId());
        nameService.finishAddition(list.getId(), userB.getId());

        ListEntity updated = reloadList();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.SELECTION);
        assertThat(updated.getVersion()).isGreaterThan(0);
        assertThat(updated.isInvitationsOpen()).isFalse();
    }

    @Test
    @DisplayName("Should not transition when only one participant finished and keep invitations open")
    void shouldNotTransitionWhenOnlyOneFinished() {
        addName(userA.getId(), "Pablo");

        nameService.finishAddition(list.getId(), userA.getId());

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.ADDITION);
        assertThat(updated.isInvitationsOpen()).isTrue();
    }

    @Test
    @DisplayName("FR-14: Should automatically close invitations when ADDITION completes")
    void shouldCloseInvitationsWhenAllMembersFinished() {
        addName(userA.getId(), "Pablo");
        addName(userB.getId(), "Lucia");

        nameService.finishAddition(list.getId(), userA.getId());
        ListEntity before = listRepository.findById(list.getId()).orElseThrow();
        assertThat(before.isInvitationsOpen()).isTrue();

        nameService.finishAddition(list.getId(), userB.getId());

        ListEntity after = reloadList();
        assertThat(after.getPhase()).isEqualTo(ListPhase.SELECTION);
        assertThat(after.isInvitationsOpen()).isFalse();
    }

    @Test
    @DisplayName("Should reject transition when no names provided by any participant")
    void shouldRejectTransitionWhenNoNames() {
        assertThatThrownBy(() -> nameService.finishAddition(list.getId(), userA.getId()))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("At least one name must be provided to proceed to the selection phase.");

        assertThatThrownBy(() -> nameService.finishAddition(list.getId(), userB.getId()))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("At least one name must be provided to proceed to the selection phase.");

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.ADDITION);
    }

    @Test
    @DisplayName("Should transition SELECTION to VOTING when both participants complete selection")
    void shouldTransitionToVotingWhenBothCompleted() {
        list.setPhase(ListPhase.SELECTION);
        listRepository.save(list);

        transitionService.completeSelection(list.getId(), userA.getId());
        transitionService.completeSelection(list.getId(), userB.getId());

        ListEntity updated = reloadList();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.VOTING);
        assertThat(updated.getCurrentRound()).isEqualTo(1);
        assertThat(updated.getTotalRounds()).isEqualTo(2);
        assertThat(updated.getVersion()).isGreaterThan(0);
        assertThat(votingRoundRepository.findByListIdAndRoundNumber(list.getId(), 1)).isPresent();
    }

    @Test
    @DisplayName("Should not transition SELECTION to VOTING when only one completed")
    void shouldNotTransitionToVotingWhenOnlyOneCompleted() {
        list.setPhase(ListPhase.SELECTION);
        listRepository.save(list);

        transitionService.completeSelection(list.getId(), userA.getId());

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.SELECTION);
    }

    @Test
    @DisplayName("Should expire ADDITION list older than 48 hours")
    void shouldExpireStaleAdditionList() {
        jdbcTemplate.update(
                "UPDATE lists SET created_at = ? WHERE id = ?",
                Instant.now().minus(49, ChronoUnit.HOURS),
                list.getId());

        transitionService.expireStaleAdditionLists();

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.EXPIRED);
    }

    @Test
    @DisplayName("Should not expire ADDITION list younger than 48 hours")
    void shouldNotExpireYoungAdditionList() {
        transitionService.expireStaleAdditionLists();

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.ADDITION);
    }

    @Test
    @DisplayName("Should expire VOTING list older than 48 hours")
    void shouldExpireStaleVotingList() {
        list.setPhase(ListPhase.VOTING);
        list.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
        listRepository.save(list);

        transitionService.expireStaleVotingLists();

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.EXPIRED);
    }

    @Test
    @DisplayName("Should not expire VOTING list younger than 48 hours")
    void shouldNotExpireYoungVotingList() {
        list.setPhase(ListPhase.VOTING);
        listRepository.save(list);

        transitionService.expireStaleVotingLists();

        ListEntity updated = listRepository.findById(list.getId()).orElseThrow();
        assertThat(updated.getPhase()).isEqualTo(ListPhase.VOTING);
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + System.nanoTime());
        user.setPasswordHash("password");
        user.setRole(Role.PARTICIPANT);
        return userRepository.save(user);
    }

    private void addMembership(Long listId, Long userId) {
        ListMembershipEntity membership = new ListMembershipEntity();
        membership.setListId(listId);
        membership.setUserId(userId);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);
    }

    private void addName(Long userId, String name) {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of(name));
        nameService.addNames(list.getId(), userId, request);
    }

    private ListEntity reloadList() {
        entityManager.flush();
        entityManager.clear();
        return listRepository.findById(list.getId()).orElseThrow();
    }
}