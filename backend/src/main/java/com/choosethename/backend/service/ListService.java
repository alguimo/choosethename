package com.choosethename.backend.service;

import com.choosethename.backend.dto.ListMapper;
import com.choosethename.backend.dto.ListResponseDTO;
import com.choosethename.backend.dto.VoteJsonCodec;
import com.choosethename.backend.exception.ListAccessDeniedException;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.repository.VotingRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ListService {

    private static final int CODE_VALIDITY_SECONDS = 48 * 3600;
    private static final int MAX_MEMBERS = 5;
    private static final String CODE_PATTERN = "^[A-Za-z0-9]{6}$";
    private static final List<ListPhase> ACTIVE_PHASES =
            List.of(ListPhase.ADDITION, ListPhase.SELECTION, ListPhase.VOTING);

    private final ListRepository listRepository;
    private final ListMembershipRepository membershipRepository;
    private final InvitationCodeGenerator codeGenerator;
    private final ListMapper listMapper;
    private final UserRepository userRepository;
    private final VotingRoundRepository votingRoundRepository;

    @Transactional
    public ListResponseDTO createList(String name, Long ownerId) {
        if (name == null || name.isBlank()) {
            throw new ListOperationException("List name cannot be blank");
        }
        ensureNoActiveList(ownerId);

        ListEntity entity = new ListEntity();
        entity.setName(name.trim());
        entity.setOwnerId(ownerId);
        entity.setInvitationCode(codeGenerator.generate());
        entity.setCodeExpiresAt(Instant.now().plusSeconds(CODE_VALIDITY_SECONDS));
        entity.setPhase(ListPhase.ADDITION);
        entity.setInvitationsOpen(true);
        entity.setCurrentRound(1);
        entity.setTotalRounds(1);
        entity.setCreatedAt(Instant.now());

        ListEntity saved = listRepository.save(entity);

        ListMembershipEntity membership = new ListMembershipEntity();
        membership.setListId(saved.getId());
        membership.setUserId(ownerId);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);

        return toResponseDTO(saved);
    }

    @Transactional
    public ListResponseDTO joinList(Long userId, String code) {
        validateCodeFormat(code);
        ensureNoActiveList(userId);

        ListEntity list = listRepository.findByInvitationCodeForUpdate(code.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ListNotFoundException("List not found for invitation code"));

        if (!list.isInvitationsOpen()) {
            throw new ListOperationException("Invitations are closed for this list");
        }
        if (list.getCodeExpiresAt().isBefore(Instant.now())) {
            throw new ListOperationException("Invitation code has expired");
        }
        if (membershipRepository.findByListIdAndUserId(list.getId(), userId).isPresent()) {
            throw new ListOperationException("User is already a member of this list");
        }

        long memberCount = membershipRepository.countByListId(list.getId());
        if (memberCount >= MAX_MEMBERS) {
            throw new ListOperationException("List already has the maximum of " + MAX_MEMBERS + " members");
        }

        ListMembershipEntity membership = new ListMembershipEntity();
        membership.setListId(list.getId());
        membership.setUserId(userId);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);

        if (memberCount + 1 >= MAX_MEMBERS) {
            list.setInvitationsOpen(false);
            listRepository.save(list);
        }

        return toResponseDTO(list);
    }

    public ListResponseDTO getActiveList(Long userId) {
        ListEntity list = findActiveLists(userId).stream().findFirst()
                .orElseThrow(() -> new ListNotFoundException("User is not a member of any active list"));
        return toResponseDTO(list);
    }

    @Transactional
    public ListResponseDTO closeInvitations(Long listId, Long userId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (!userId.equals(list.getOwnerId())) {
            throw new ListAccessDeniedException("Only the list owner can close invitations");
        }
        list.setInvitationsOpen(false);
        return toResponseDTO(listRepository.save(list));
    }

    private void validateCodeFormat(String code) {
        if (code == null || !code.matches(CODE_PATTERN)) {
            throw new ListOperationException("Invitation code must be exactly 6 alphanumeric characters");
        }
    }

    private void ensureNoActiveList(Long userId) {
        if (!findActiveLists(userId).isEmpty()) {
            throw new ListOperationException("User already belongs to an active list");
        }
    }

    private List<ListEntity> findActiveLists(Long userId) {
        return listRepository.findActiveListsForUser(userId, ACTIVE_PHASES);
    }

    private ListResponseDTO toResponseDTO(ListEntity list) {
        List<String> memberNames = membershipRepository.findByListId(list.getId()).stream()
                .map(member -> userRepository.findById(member.getUserId()).map(User::getUsername).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        String ownerUsername = userRepository.findById(list.getOwnerId()).map(User::getUsername).orElse(null);
        List<String> currentPool = votingRoundRepository
                .findByListIdAndRoundNumber(list.getId(), list.getCurrentRound())
                .map(round -> VoteJsonCodec.decode(round.getPoolRankings()))
                .orElse(List.of());
        return listMapper.toResponseDTO(list, memberNames, ownerUsername, currentPool);
    }
}