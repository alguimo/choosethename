package com.choosethename.backend.service;

import com.choosethename.backend.dto.ListMapper;
import com.choosethename.backend.dto.ListResponseDTO;
import com.choosethename.backend.exception.ListAccessDeniedException;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListServiceCloseInvitationsTest {

    private static final long OWNER_ID = 10L;
    private static final long OTHER_USER_ID = 11L;
    private static final long LIST_ID = 1L;

    @Mock private ListRepository listRepository;
    @Mock private ListMembershipRepository membershipRepository;
    @Mock private ListMapper listMapper;
    @Mock private UserRepository userRepository;
    @Mock private VotingRoundRepository votingRoundRepository;
    @InjectMocks private ListService listService;

    private ListEntity ownedList() {
        ListEntity list = new ListEntity();
        list.setId(LIST_ID);
        list.setOwnerId(OWNER_ID);
        list.setInvitationsOpen(true);
        list.setCodeExpiresAt(Instant.now().plusSeconds(3600));
        return list;
    }

    private void stubResponseDto() {
        when(membershipRepository.findByListId(LIST_ID)).thenReturn(List.of());
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(new User()));
        when(listMapper.toResponseDTO(any(ListEntity.class), anyList(), any(), anyList()))
                .thenReturn(new ListResponseDTO());
    }

    @Test
    @DisplayName("FR-12: Owner closes invitations (200 equivalent)")
    void shouldCloseInvitationsAsOwner() {
        ListEntity list = ownedList();
        when(listRepository.findById(LIST_ID)).thenReturn(Optional.of(list));
        when(listRepository.save(any(ListEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        stubResponseDto();

        listService.closeInvitations(LIST_ID, OWNER_ID);

        assertThat(list.isInvitationsOpen()).isFalse();
        verify(listRepository).save(list);
    }

    @Test
    @DisplayName("FR-13: Non-owner member cannot close invitations (403)")
    void shouldRejectCloseInvitationsByNonOwner() {
        when(listRepository.findById(LIST_ID)).thenReturn(Optional.of(ownedList()));

        assertThatThrownBy(() -> listService.closeInvitations(LIST_ID, OTHER_USER_ID))
                .isInstanceOf(ListAccessDeniedException.class);
    }

    @Test
    @DisplayName("FR-12: Close invitations on non-existent list (404)")
    void shouldThrowNotFoundWhenListDoesNotExist() {
        when(listRepository.findById(LIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.closeInvitations(LIST_ID, OWNER_ID))
                .isInstanceOf(ListNotFoundException.class);
    }
}