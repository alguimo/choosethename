package com.choosethename.backend.service;

import com.choosethename.backend.dto.AddNameRequestDTO;
import com.choosethename.backend.dto.AdoptNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.dto.SelectionResponseDTO;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import com.choosethename.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SelectionServiceTest {

    @Autowired private SelectionService selectionService;
    @Autowired private NameService nameService;
    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;
    @Autowired private SharedNamePoolRepository sharedNamePoolRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private User userA;
    private User userB;
    private ListEntity list;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM shared_name_pool");
        jdbcTemplate.execute("DELETE FROM names");
        jdbcTemplate.execute("DELETE FROM list_memberships");
        jdbcTemplate.execute("DELETE FROM lists");

        userA = createUser("alice");
        userB = createUser("bob");

        list = new ListEntity();
        list.setName("Test List");
        list.setOwnerId(userA.getId());
        list.setInvitationCode("CODE" + System.nanoTime() % 1000000);
        list.setPhase(ListPhase.ADDITION);
        list.setInvitationsOpen(true);
        list.setCodeExpiresAt(Instant.now().plusSeconds(48 * 3600));
        list.setCurrentRound(1);
        list.setTotalRounds(1);
        list.setCreatedAt(Instant.now());
        listRepository.save(list);

        addMembership(list.getId(), userA.getId());
        addMembership(list.getId(), userB.getId());
    }

    @Test
    @DisplayName("Should return common names, faded suggestions and own names during SELECTION")
    void shouldReturnCommonAndFadedNamesDuringSelection() {
        enterSelectionPhase();

        SelectionResponseDTO response = selectionService.getSelection(list.getId(), userA.getId());

        assertThat(names(response.getCommonNames())).containsExactly("pablo");
        assertThat(names(response.getFadedSuggestions())).containsExactly("lucia");
        assertThat(names(response.getMyNames())).containsExactly("pablo", "maria");
    }

    @Test
    @DisplayName("Should return empty common names when pools do not overlap")
    void shouldReturnEmptyCommonWhenNoOverlap() {
        addName(userA.getId(), "Maria");
        addName(userB.getId(), "Lucia");
        finishAdditionForBoth();

        SelectionResponseDTO response = selectionService.getSelection(list.getId(), userA.getId());

        assertThat(response.getCommonNames()).isEmpty();
        assertThat(names(response.getFadedSuggestions())).containsExactly("lucia");
    }

    @Test
    @DisplayName("Should adopt a faded suggestion into the shared pool")
    void shouldAdoptFadedSuggestion() {
        enterSelectionPhase();

        AdoptNameRequestDTO request = new AdoptNameRequestDTO();
        request.setName("  lucia  ");
        selectionService.adoptFadedName(list.getId(), userA.getId(), request);

        assertThat(sharedNamePoolRepository.countByListId(list.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-10: Faded suggestions report adopted=true after adoption; common names are always adopted")
    void shouldFlagAdoptedFadedSuggestions() {
        enterSelectionPhase();

        SelectionResponseDTO before = selectionService.getSelection(list.getId(), userA.getId());
        assertThat(before.getFadedSuggestions()).hasSize(1);
        assertThat(before.getFadedSuggestions().get(0).getNormalizedName()).isEqualTo("lucia");
        assertThat(before.getFadedSuggestions().get(0).isAdopted()).isFalse();

        AdoptNameRequestDTO request = new AdoptNameRequestDTO();
        request.setName("lucia");
        selectionService.adoptFadedName(list.getId(), userA.getId(), request);

        SelectionResponseDTO after = selectionService.getSelection(list.getId(), userA.getId());
        assertThat(after.getFadedSuggestions()).hasSize(1);
        assertThat(after.getFadedSuggestions().get(0).getNormalizedName()).isEqualTo("lucia");
        assertThat(after.getFadedSuggestions().get(0).isAdopted()).isTrue();
        assertThat(after.getCommonNames()).allMatch(NameResponseDTO.NameEntry::isAdopted);
    }

    @Test
    @DisplayName("Should reject adoption of a common name")
    void shouldRejectAdoptionOfCommonName() {
        enterSelectionPhase();

        AdoptNameRequestDTO request = new AdoptNameRequestDTO();
        request.setName("Pablo");

        assertThatThrownBy(() -> selectionService.adoptFadedName(list.getId(), userA.getId(), request))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Name is not a faded suggestion for this user");
    }

    @Test
    @DisplayName("Should reject adoption of a name the user does not have access to")
    void shouldRejectAdoptionWhenNotFaded() {
        enterSelectionPhase();

        AdoptNameRequestDTO request = new AdoptNameRequestDTO();
        request.setName("Charlotte");

        assertThatThrownBy(() -> selectionService.adoptFadedName(list.getId(), userA.getId(), request))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Name is not a faded suggestion for this user");
    }

    @Test
    @DisplayName("Optimistic lock should reject a stale phase transition")
    void shouldRejectStaleTransitionWithOptimisticLock() {
        ListEntity stale = listRepository.findById(list.getId()).orElseThrow();
        entityManager.detach(stale);

        jdbcTemplate.update("UPDATE lists SET version = version + 1 WHERE id = ?", list.getId());

        assertThatThrownBy(() -> listRepository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Should reject adoption when list is not in SELECTION phase")
    void shouldRejectAdoptionOutsideSelection() {
        AdoptNameRequestDTO request = new AdoptNameRequestDTO();
        request.setName("Lucia");

        assertThatThrownBy(() -> selectionService.adoptFadedName(list.getId(), userA.getId(), request))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Names can only be adopted during the SELECTION phase");
    }

    @Test
    @DisplayName("Should reject adoption with a null request body")
    void shouldRejectNullRequestBody() {
        enterSelectionPhase();

        assertThatThrownBy(() -> selectionService.adoptFadedName(list.getId(), userA.getId(), null))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Request body is required");
    }

    @Test
    @DisplayName("Should identify common and faded names for three members (majority >1)")
    void threeMemberMajorityMatching() {
        User userC = createUser("charlie");
        ListEntity list3 = createList("Three", userA.getId(), userB.getId(), userC.getId());

        addNameToList(list3.getId(), userA.getId(), "Pablo", "Maria");
        addNameToList(list3.getId(), userB.getId(), "Pablo", "Lucia");
        addNameToList(list3.getId(), userC.getId(), "Pablo", "Elena");

        list3.setPhase(ListPhase.SELECTION);
        listRepository.save(list3);

        SelectionResponseDTO response = selectionService.getSelection(list3.getId(), userC.getId());

        assertThat(names(response.getCommonNames())).containsExactly("pablo");
        assertThat(names(response.getFadedSuggestions())).containsExactly("maria", "lucia");
        assertThat(names(response.getMyNames())).containsExactly("pablo", "elena");
    }

    @Test
    @DisplayName("Should identify faded suggestions across three pools for the member that lacks them")
    void threeMemberFadedSuggestionsDistributed() {
        User userC = createUser("charlie");
        ListEntity list3 = createList("ThreePools", userA.getId(), userB.getId(), userC.getId());

        addNameToList(list3.getId(), userA.getId(), "Maria");
        addNameToList(list3.getId(), userB.getId(), "Lucia");
        addNameToList(list3.getId(), userC.getId(), "Elena");

        list3.setPhase(ListPhase.SELECTION);
        listRepository.save(list3);

        SelectionResponseDTO responseForA = selectionService.getSelection(list3.getId(), userA.getId());

        assertThat(responseForA.getCommonNames()).isEmpty();
        assertThat(names(responseForA.getFadedSuggestions())).containsExactly("lucia", "elena");
    }

    @Test
    @DisplayName("Should restrict faded adoption to names in other pools, not common names")
    void threeMemberAdoptionOnlyForFaded() {
        User userC = createUser("charlie");
        ListEntity list3 = createList("ThreeAdopt", userA.getId(), userB.getId(), userC.getId());

        addNameToList(list3.getId(), userA.getId(), "Pablo");
        addNameToList(list3.getId(), userB.getId(), "Pablo", "Lucia");
        addNameToList(list3.getId(), userC.getId(), "Pablo", "Elena");

        list3.setPhase(ListPhase.SELECTION);
        listRepository.save(list3);

        AdoptNameRequestDTO common = new AdoptNameRequestDTO();
        common.setName("Pablo");
        assertThatThrownBy(() -> selectionService.adoptFadedName(list3.getId(), userA.getId(), common))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Name is not a faded suggestion for this user");

        AdoptNameRequestDTO faded = new AdoptNameRequestDTO();
        faded.setName("Lucia");
        selectionService.adoptFadedName(list3.getId(), userA.getId(), faded);

        assertThat(sharedNamePoolRepository.countByListId(list3.getId())).isEqualTo(1);
    }

    private void enterSelectionPhase() {
        addName(userA.getId(), "Pablo");
        addName(userA.getId(), "Maria");
        addName(userB.getId(), "Pablo");
        addName(userB.getId(), "Lucia");
        finishAdditionForBoth();
    }

    private void finishAdditionForBoth() {
        nameService.finishAddition(list.getId(), userA.getId());
        nameService.finishAddition(list.getId(), userB.getId());
    }

    private void addName(Long userId, String name) {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of(name));
        nameService.addNames(list.getId(), userId, request);
    }

    private List<String> names(List<NameResponseDTO.NameEntry> entries) {
        return entries.stream().map(NameResponseDTO.NameEntry::getNormalizedName).toList();
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

    private ListEntity createList(String name, Long... memberIds) {
        ListEntity list = new ListEntity();
        list.setName(name);
        list.setOwnerId(memberIds[0]);
        list.setInvitationCode("SEL" + System.nanoTime() % 1000000);
        list.setPhase(ListPhase.ADDITION);
        list.setInvitationsOpen(true);
        list.setCodeExpiresAt(Instant.now().plusSeconds(48 * 3600));
        list.setCurrentRound(1);
        list.setTotalRounds(1);
        list.setCreatedAt(Instant.now());
        list = listRepository.save(list);
        for (Long userId : memberIds) {
            addMembership(list.getId(), userId);
        }
        return list;
    }

    private void addNameToList(Long listId, Long userId, String... nameValues) {
        for (String name : nameValues) {
            AddNameRequestDTO request = new AddNameRequestDTO();
            request.setNames(List.of(name));
            nameService.addNames(listId, userId, request);
        }
    }
}