package com.choosethename.backend.service;

import com.choosethename.backend.dto.ListMapper;
import com.choosethename.backend.dto.ListResponseDTO;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListServiceTest {

    private static final long OWNER_ID = 10L;
    private static final String VALID_CODE = "ABC123";

    @Mock private ListRepository listRepository;
    @Mock private ListMembershipRepository membershipRepository;
    @Mock private InvitationCodeGenerator codeGenerator;
    @Mock private ListMapper listMapper;
    @Mock private UserRepository userRepository;
    @Mock private VotingRoundRepository votingRoundRepository;
    @InjectMocks private ListService listService;

    private User owner() {
        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setUsername("alvaro");
        return owner;
    }

    private void stubResponseDto() {
        when(listMapper.toResponseDTO(any(ListEntity.class), anyList(), any(), anyList()))
                .thenReturn(new ListResponseDTO());
    }

    @Test
    @DisplayName("FR-1: Create list successfully with code, owner as first member, invitations open")
    void shouldCreateListSuccessfully() {
        when(codeGenerator.generate()).thenReturn(VALID_CODE);
        when(listRepository.save(any(ListEntity.class))).thenAnswer(inv -> {
            ListEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });
        when(membershipRepository.findByListId(1L)).thenReturn(List.of());
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        stubResponseDto();

        listService.createList("Baby Names 2026", OWNER_ID);

        ArgumentCaptor<ListEntity> listCaptor = ArgumentCaptor.forClass(ListEntity.class);
        verify(listRepository).save(listCaptor.capture());
        ListEntity saved = listCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Baby Names 2026");
        assertThat(saved.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(saved.getInvitationCode()).isEqualTo(VALID_CODE);
        assertThat(saved.getCodeExpiresAt()).isAfter(Instant.now().plusSeconds(47 * 3600));
        assertThat(saved.getPhase()).isEqualTo(ListPhase.ADDITION);
        assertThat(saved.isInvitationsOpen()).isTrue();

        ArgumentCaptor<ListMembershipEntity> membershipCaptor = ArgumentCaptor.forClass(ListMembershipEntity.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getListId()).isEqualTo(1L);
        assertThat(membershipCaptor.getValue().getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("FR-1: Allow creating another list while already belonging to other lists")
    void shouldAllowCreatingAdditionalList() {
        when(codeGenerator.generate()).thenReturn(VALID_CODE);
        when(listRepository.save(any(ListEntity.class))).thenAnswer(inv -> {
            ListEntity entity = inv.getArgument(0);
            entity.setId(2L);
            return entity;
        });
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        stubResponseDto();

        ListResponseDTO result = listService.createList("Second List", OWNER_ID);

        assertThat(result).isNotNull();
        verify(listRepository).save(any(ListEntity.class));
    }

    @Test
    @DisplayName("FR-3: Reject creation with null name (400)")
    void shouldRejectCreationWithNullName() {
        assertThatThrownBy(() -> listService.createList(null, OWNER_ID))
                .isInstanceOf(ListOperationException.class);
    }

    @Test
    @DisplayName("FR-3: Reject creation with blank name (400)")
    void shouldRejectCreationWithBlankName() {
        assertThatThrownBy(() -> listService.createList("   ", OWNER_ID))
                .isInstanceOf(ListOperationException.class);
    }

    @Test
    @DisplayName("FR-13: Return all visible lists for the user")
    void shouldReturnListsForUser() {
        ListEntity first = new ListEntity();
        first.setId(1L);
        first.setOwnerId(OWNER_ID);
        ListEntity second = new ListEntity();
        second.setId(2L);
        second.setOwnerId(OWNER_ID);
        when(listRepository.findListsForUser(eq(OWNER_ID), eq(ListPhase.EXPIRED)))
                .thenReturn(List.of(first, second));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        stubResponseDto();

        List<ListResponseDTO> result = listService.getListsForUser(OWNER_ID);

        assertThat(result).hasSize(2);
        verify(listMapper, org.mockito.Mockito.times(2))
                .toResponseDTO(any(ListEntity.class), anyList(), any(), anyList());
    }

    @Test
    @DisplayName("FR-13: Return an empty list when the user belongs to no lists")
    void shouldReturnEmptyWhenUserHasNoLists() {
        when(listRepository.findListsForUser(eq(OWNER_ID), eq(ListPhase.EXPIRED))).thenReturn(List.of());

        assertThat(listService.getListsForUser(OWNER_ID)).isEmpty();
    }

    @Test
    @DisplayName("FR-14: Return list details for the owner")
    void shouldReturnListByIdForOwner() {
        ListEntity list = new ListEntity();
        list.setId(1L);
        list.setOwnerId(OWNER_ID);
        list.setPhase(ListPhase.ADDITION);
        when(listRepository.findById(1L)).thenReturn(Optional.of(list));
        when(membershipRepository.findByListId(1L)).thenReturn(List.of());
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        stubResponseDto();

        ListResponseDTO result = listService.getListById(1L, OWNER_ID);

        assertThat(result).isNotNull();
        verify(listMapper).toResponseDTO(eq(list), anyList(), eq("alvaro"), anyList());
    }

    @Test
    @DisplayName("FR-14: Return list details for a non-owner member")
    void shouldReturnListByIdForMember() {
        ListEntity list = new ListEntity();
        list.setId(1L);
        list.setOwnerId(99L);
        list.setPhase(ListPhase.SELECTION);
        when(listRepository.findById(1L)).thenReturn(Optional.of(list));
        when(membershipRepository.findByListIdAndUserId(1L, OWNER_ID)).thenReturn(Optional.of(new ListMembershipEntity()));
        when(membershipRepository.findByListId(1L)).thenReturn(List.of());
        when(userRepository.findById(99L)).thenReturn(Optional.of(owner()));
        stubResponseDto();

        assertThat(listService.getListById(1L, OWNER_ID)).isNotNull();
    }

    @Test
    @DisplayName("FR-15: Throw 404 when the list does not exist")
    void shouldThrowNotFoundWhenListMissing() {
        when(listRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.getListById(1L, OWNER_ID))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    @DisplayName("FR-15: Throw 404 when the user is not an owner or member")
    void shouldThrowNotFoundWhenNotAMember() {
        ListEntity list = new ListEntity();
        list.setId(1L);
        list.setOwnerId(99L);
        list.setPhase(ListPhase.ADDITION);
        when(listRepository.findById(1L)).thenReturn(Optional.of(list));
        when(membershipRepository.findByListIdAndUserId(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.getListById(1L, OWNER_ID))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    @DisplayName("FR-17: Throw 404 when the list is EXPIRED")
    void shouldThrowNotFoundWhenListExpired() {
        ListEntity list = new ListEntity();
        list.setId(1L);
        list.setOwnerId(OWNER_ID);
        list.setPhase(ListPhase.EXPIRED);
        when(listRepository.findById(1L)).thenReturn(Optional.of(list));

        assertThatThrownBy(() -> listService.getListById(1L, OWNER_ID))
                .isInstanceOf(ListNotFoundException.class);
    }
}