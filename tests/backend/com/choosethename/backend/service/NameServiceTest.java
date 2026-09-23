package com.choosethename.backend.service;

import com.choosethename.backend.dto.AddNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.exception.BlankNameException;
import com.choosethename.backend.exception.DuplicateNameException;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NameServiceTest {

    @Autowired private NameService nameService;
    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private ListMembershipRepository membershipRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User userA;
    private User userB;
    private ListEntity list;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM shared_name_pool");
        jdbcTemplate.execute("DELETE FROM names");

        userA = new User();
        userA.setUsername("alice_" + System.nanoTime());
        userA.setPasswordHash("password");
        userA.setRole(Role.PARTICIPANT);
        userRepository.save(userA);

        userB = new User();
        userB.setUsername("bob_" + System.nanoTime());
        userB.setPasswordHash("password");
        userB.setRole(Role.PARTICIPANT);
        userRepository.save(userB);

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

        ListMembershipEntity membershipA = new ListMembershipEntity();
        membershipA.setListId(list.getId());
        membershipA.setUserId(userA.getId());
        membershipA.setJoinedAt(Instant.now());
        membershipRepository.save(membershipA);

        ListMembershipEntity membershipB = new ListMembershipEntity();
        membershipB.setListId(list.getId());
        membershipB.setUserId(userB.getId());
        membershipB.setJoinedAt(Instant.now());
        membershipRepository.save(membershipB);
    }

    @Test
    @DisplayName("Should add names and return normalized form")
    void shouldAddNamesAndReturnNormalized() {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("  Pablo  ", "Lucia"));

        NameResponseDTO response = nameService.addNames(list.getId(), userA.getId(), request);

        assertThat(response.getNames()).hasSize(2);
        assertThat(response.getNames().get(0).getName()).isEqualTo("  Pablo  ");
        assertThat(response.getNames().get(0).getNormalizedName()).isEqualTo("pablo");
        assertThat(response.getNames().get(1).getName()).isEqualTo("Lucia");
        assertThat(response.getNames().get(1).getNormalizedName()).isEqualTo("lucia");
    }

    @Test
    @DisplayName("Should return only the current user's own names during ADDITION (privacy NFR-2)")
    void shouldReturnOnlyOwnNamesDuringAddition() {
        addNames(userA.getId(), List.of("Pablo", "Maria"));
        addNames(userB.getId(), List.of("Pablo", "Lucia"));

        NameResponseDTO response = nameService.getNames(list.getId(), userA.getId());

        assertThat(response.getNames()).hasSize(2);
        assertThat(response.getNames()).extracting(NameResponseDTO.NameEntry::getName)
                .containsExactly("Pablo", "Maria");
        assertThat(response.getNames()).extracting(NameResponseDTO.NameEntry::getNormalizedName)
                .containsExactly("pablo", "maria");
    }

    @Test
    @DisplayName("Should reject viewing names outside the ADDITION phase")
    void shouldRejectViewingNamesOutsideAddition() {
        addNames(userA.getId(), List.of("Pablo"));
        list.setPhase(ListPhase.SELECTION);
        listRepository.save(list);

        assertThatThrownBy(() -> nameService.getNames(list.getId(), userA.getId()))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Names can only be viewed during the ADDITION phase");
    }

    @Test
    @DisplayName("Should reject viewing names when the user is not a member")
    void shouldRejectViewingNamesWhenNotMember() {
        addNames(userA.getId(), List.of("Pablo"));

        assertThatThrownBy(() -> nameService.getNames(list.getId(), 999999L))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("User is not a member of this list");
    }

    private void addNames(Long userId, List<String> names) {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(names);
        nameService.addNames(list.getId(), userId, request);
    }

    @Test
    @DisplayName("Should reject duplicate name with 422")
    void shouldRejectDuplicateNameWith422() {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("Pablo"));
        nameService.addNames(list.getId(), userA.getId(), request);

        AddNameRequestDTO duplicateRequest = new AddNameRequestDTO();
        duplicateRequest.setNames(List.of("pablo"));

        assertThatThrownBy(() -> nameService.addNames(list.getId(), userA.getId(), duplicateRequest))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessage("Name already exists for this user in this list");
    }

    @Test
    @DisplayName("Should reject finish addition when participant has no names in their private pool")
    void shouldRejectFinishAdditionWhenNoNames() {
        assertThatThrownBy(() -> nameService.finishAddition(list.getId(), userA.getId()))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("At least one name must be provided to proceed to the selection phase.");

        assertThatThrownBy(() -> nameService.finishAddition(list.getId(), userB.getId()))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("At least one name must be provided to proceed to the selection phase.");
    }

    @Test
    @DisplayName("Should reject blank name after normalization with 422 exception")
    void shouldRejectBlankNameAfterNormalization() {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("   "));

        assertThatThrownBy(() -> nameService.addNames(list.getId(), userA.getId(), request))
                .isInstanceOf(BlankNameException.class)
                .hasMessage("Name cannot be blank");
    }

    @Test
    @DisplayName("Should finish addition successfully when names exist")
    void shouldFinishAdditionSuccessfully() {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("Pablo"));
        nameService.addNames(list.getId(), userA.getId(), request);

        nameService.finishAddition(list.getId(), userA.getId());

        var membership = membershipRepository.findByListIdAndUserId(list.getId(), userA.getId());
        assertThat(membership).isPresent();
        assertThat(membership.get().getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject addition when list is not in ADDITION phase")
    void shouldRejectAdditionWhenNotInAdditionPhase() {
        list.setPhase(ListPhase.SELECTION);
        listRepository.save(list);

        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("Pablo"));

        assertThatThrownBy(() -> nameService.addNames(list.getId(), userA.getId(), request))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Names can only be added during the ADDITION phase");
    }

    @Test
    @DisplayName("Should allow multiple different names from same user")
    void shouldAllowMultipleDifferentNames() {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("Pablo", "Lucia", "Maria"));

        NameResponseDTO response = nameService.addNames(list.getId(), userA.getId(), request);

        assertThat(response.getNames()).hasSize(3);
    }

    @Test
    @DisplayName("Should allow same name from different users")
    void shouldAllowSameNameFromDifferentUsers() {
        AddNameRequestDTO requestA = new AddNameRequestDTO();
        requestA.setNames(List.of("Pablo"));
        nameService.addNames(list.getId(), userA.getId(), requestA);

        AddNameRequestDTO requestB = new AddNameRequestDTO();
        requestB.setNames(List.of("pablo"));

        NameResponseDTO response = nameService.addNames(list.getId(), userB.getId(), requestB);

        assertThat(response.getNames()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject names when list not found")
    void shouldRejectWhenListNotFound() {
        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("Pablo"));

        assertThatThrownBy(() -> nameService.addNames(999L, userA.getId(), request))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject names when user is not a member")
    void shouldRejectWhenUserNotMember() {
        User outsider = new User();
        outsider.setUsername("outsider_" + System.nanoTime());
        outsider.setPasswordHash("password");
        outsider.setRole(Role.PARTICIPANT);
        userRepository.save(outsider);

        AddNameRequestDTO request = new AddNameRequestDTO();
        request.setNames(List.of("Pablo"));

        assertThatThrownBy(() -> nameService.addNames(list.getId(), outsider.getId(), request))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("User is not a member of this list");
    }

    @Test
    @DisplayName("Should reject a null request body with 400")
    void shouldRejectNullRequestBody() {
        assertThatThrownBy(() -> nameService.addNames(list.getId(), userA.getId(), null))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Request body is required");
    }

    @Test
    @DisplayName("Should reject a null names list with 400")
    void shouldRejectNullNamesList() {
        AddNameRequestDTO request = new AddNameRequestDTO();

        assertThatThrownBy(() -> nameService.addNames(list.getId(), userA.getId(), request))
                .isInstanceOf(ListOperationException.class)
                .hasMessage("Names list is required");
    }
}
