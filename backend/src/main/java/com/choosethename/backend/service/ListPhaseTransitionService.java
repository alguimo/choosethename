package com.choosethename.backend.service;

import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.NameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPhaseTransitionService {

    private static final int ADDITION_TIMEOUT_HOURS = 48;
    public static final String EMPTY_NAME_MESSAGE = "At least one name must be provided to proceed to the selection phase.";

    private final ListRepository listRepository;
    private final ListMembershipRepository membershipRepository;
    private final NameRepository nameRepository;

    @Transactional
    public void checkAndTransitionFromAddition(Long listId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (!"ADDITION".equals(list.getPhase())) {
            return;
        }
        boolean allFinished = membershipRepository.findByListId(listId).stream()
                .allMatch(m -> m.getFinishedAt() != null);
        if (!allFinished) {
            return;
        }
        if (nameRepository.countByListId(listId) == 0) {
            throw new ListOperationException(EMPTY_NAME_MESSAGE);
        }
        list.setPhase("SELECTION");
        list.setInvitationsOpen(false);
        listRepository.save(list);
    }

    @Transactional
    public void completeSelection(Long listId, Long userId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (!"SELECTION".equals(list.getPhase())) {
            throw new ListOperationException("Selection can only be completed during the SELECTION phase");
        }

        ListMembershipEntity membership = membershipRepository.findByListIdAndUserId(listId, userId)
                .orElseThrow(() -> new ListOperationException("User is not a member of this list"));
        membership.setSelectionCompletedAt(Instant.now());
        membershipRepository.save(membership);

        boolean allCompleted = membershipRepository.findByListId(listId).stream()
                .allMatch(m -> m.getSelectionCompletedAt() != null);
        if (allCompleted) {
            list.setPhase("VOTING");
            listRepository.save(list);
        }
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    @Transactional
    public void expireStaleAdditionLists() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(ADDITION_TIMEOUT_HOURS));
        List<ListEntity> staleLists = listRepository.findByPhaseAndCreatedAtBefore("ADDITION", cutoff);
        for (ListEntity list : staleLists) {
            list.setPhase("EXPIRED");
            listRepository.save(list);
        }
    }
}