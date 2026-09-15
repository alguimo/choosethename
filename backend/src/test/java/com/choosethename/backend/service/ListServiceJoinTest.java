package com.choosethename.backend.service;

import com.choosethename.backend.dto.ListMapper;
import com.choosethename.backend.dto.ListResponseDTO;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListServiceJoinTest {

    private static final long USER_ID = 10L;
    private static final long LIST_ID = 1L;
    private static final String CODE = "ABC123";

    @Mock private ListRepository listRepository;
    @Mock private ListMembershipRepository membershipRepository;
    @Mock private ListMapper listMapper;
    @Mock private UserRepository userRepository;
    @Mock private VotingRoundRepository votingRoundRepository;
    @InjectMocks private ListService listService;

    private ListEntity activeList() {
        ListEntity list = new ListEntity();
        list.setId(LIST_ID);
        list.setInvitationsOpen(true);
        list.setCodeExpiresAt(Instant.now().plusSeconds(3600));
        return list;
    }

    private void stubJoinEnvironment() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        when(listRepository.findByInvitationCodeForUpdate(anyString())).thenReturn(Optional.of(activeList()));
        when(membershipRepository.findByListId(LIST_ID)).thenReturn(List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(listMapper.toResponseDTO(any(ListEntity.class), anyList(), any(), anyList()))
                .thenReturn(new ListResponseDTO());
    }

    @Test
    @DisplayName("FR-4/NFR-3: Join with valid code (case-insensitive) succeeds")
    void shouldJoinListSuccessfully() {
        stubJoinEnvironment();

        ListResponseDTO result = listService.joinList(USER_ID, "abc123");

        assertThat(result).isNotNull();
        ArgumentCaptor<ListMembershipEntity> captor = ArgumentCaptor.forClass(ListMembershipEntity.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getListId()).isEqualTo(LIST_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("FR-5: Reject malformed code format (400)")
    void shouldRejectMalformedCode() {
        assertThatThrownBy(() -> listService.joinList(USER_ID, "AB12"))
                .isInstanceOf(ListOperationException.class)
                .hasMessageContaining("6");
        verify(membershipRepository, never()).save(any(ListMembershipEntity.class));
    }

    @Test
    @DisplayName("FR-6: Reject non-existent invitation code (404)")
    void shouldThrowNotFoundForNonExistentCode() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        when(listRepository.findByInvitationCodeForUpdate(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.joinList(USER_ID, CODE))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    @DisplayName("FR-7: Reject expired invitation code (400)")
    void shouldRejectExpiredCode() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        ListEntity expired = activeList();
        expired.setCodeExpiresAt(Instant.now().minusSeconds(60));
        when(listRepository.findByInvitationCodeForUpdate(CODE)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> listService.joinList(USER_ID, CODE))
                .isInstanceOf(ListOperationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("FR-7: Reject closed invitations (400)")
    void shouldRejectClosedInvitations() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        ListEntity closed = activeList();
        closed.setInvitationsOpen(false);
        when(listRepository.findByInvitationCodeForUpdate(CODE)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> listService.joinList(USER_ID, CODE))
                .isInstanceOf(ListOperationException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("FR-8: Reject joining a list the user already belongs to (400)")
    void shouldRejectDuplicateMembership() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        when(listRepository.findByInvitationCodeForUpdate(CODE)).thenReturn(Optional.of(activeList()));
        when(membershipRepository.findByListIdAndUserId(LIST_ID, USER_ID))
                .thenReturn(Optional.of(new ListMembershipEntity()));

        assertThatThrownBy(() -> listService.joinList(USER_ID, CODE))
                .isInstanceOf(ListOperationException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    @DisplayName("FR-9: Reject join when user already belongs to another active list (400)")
    void shouldRejectWhenUserInAnotherActiveList() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList()))
                .thenReturn(List.of(activeList()));

        assertThatThrownBy(() -> listService.joinList(USER_ID, CODE))
                .isInstanceOf(ListOperationException.class)
                .hasMessageContaining("active list");
    }

    @Test
    @DisplayName("FR-10: Reject join when list already has 5 members (400)")
    void shouldRejectWhenListIsFull() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        when(listRepository.findByInvitationCodeForUpdate(CODE)).thenReturn(Optional.of(activeList()));
        when(membershipRepository.countByListId(LIST_ID)).thenReturn(5L);

        assertThatThrownBy(() -> listService.joinList(USER_ID, CODE))
                .isInstanceOf(ListOperationException.class)
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("FR-11: Joining the 5th member auto-closes invitations")
    void shouldAutoCloseInvitationsAtFiveMembers() {
        when(listRepository.findActiveListsForUser(eq(USER_ID), anyList())).thenReturn(List.of());
        when(listRepository.findByInvitationCodeForUpdate(CODE)).thenReturn(Optional.of(activeList()));
        when(membershipRepository.countByListId(LIST_ID)).thenReturn(4L);
        when(membershipRepository.findByListId(LIST_ID)).thenReturn(List.of());
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        when(listMapper.toResponseDTO(any(ListEntity.class), anyList(), any(), anyList()))
                .thenReturn(new ListResponseDTO());

        listService.joinList(USER_ID, CODE);

        ArgumentCaptor<ListEntity> captor = ArgumentCaptor.forClass(ListEntity.class);
        verify(listRepository).save(captor.capture());
        assertThat(captor.getValue().isInvitationsOpen()).isFalse();
    }
}