package com.choosethename.backend.service;

import com.choosethename.backend.dto.AddNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.exception.BlankNameException;
import com.choosethename.backend.exception.DuplicateNameException;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListMembershipEntity;
import com.choosethename.backend.model.ListPhase;
import com.choosethename.backend.model.NameEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.NameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NameService {

    private final NameRepository nameRepository;
    private final ListRepository listRepository;
    private final ListMembershipRepository membershipRepository;
    private final NameNormalizer nameNormalizer;
    private final ListPhaseTransitionService phaseTransitionService;

    @Transactional
    public NameResponseDTO addNames(Long listId, Long userId, AddNameRequestDTO request) {
        if (request == null) {
            throw new ListOperationException("Request body is required");
        }

        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));

        if (list.getPhase() != ListPhase.ADDITION) {
            throw new ListOperationException("Names can only be added during the ADDITION phase");
        }

        ensureUserIsMember(listId, userId);

        if (request.getNames() == null) {
            throw new ListOperationException("Names list is required");
        }

        List<NameResponseDTO.NameEntry> addedNames = new ArrayList<>();
        for (String rawName : request.getNames()) {
            String normalized = nameNormalizer.normalize(rawName);
            if (normalized.isEmpty()) {
                throw new BlankNameException("Name cannot be blank");
            }
            if (nameRepository.existsByListIdAndUserIdAndNormalizedName(listId, userId, normalized)) {
                throw new DuplicateNameException("Name already exists for this user in this list");
            }

            NameEntity entity = new NameEntity();
            entity.setListId(listId);
            entity.setUserId(userId);
            entity.setName(rawName);
            entity.setNormalizedName(normalized);
            entity.setCreatedAt(Instant.now());
            nameRepository.save(entity);

            addedNames.add(new NameResponseDTO.NameEntry(rawName, normalized));
        }

        NameResponseDTO response = new NameResponseDTO();
        response.setNames(addedNames);
        return response;
    }

    @Transactional(readOnly = true)
    public NameResponseDTO getNames(Long listId, Long userId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));

        if (list.getPhase() != ListPhase.ADDITION) {
            throw new ListOperationException("Names can only be viewed during the ADDITION phase");
        }

        ensureUserIsMember(listId, userId);

        List<NameResponseDTO.NameEntry> myNames = nameRepository.findByListIdAndUserId(listId, userId).stream()
                .sorted(Comparator.comparing(NameEntity::getId))
                .map(entity -> new NameResponseDTO.NameEntry(entity.getName(), entity.getNormalizedName()))
                .toList();

        NameResponseDTO response = new NameResponseDTO();
        response.setNames(myNames);
        return response;
    }

    @Transactional
    public void finishAddition(Long listId, Long userId) {
        listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));

        ensureUserIsMember(listId, userId);

        if (nameRepository.countByListIdAndUserId(listId, userId) == 0) {
            throw new ListOperationException(ListPhaseTransitionService.EMPTY_NAME_MESSAGE);
        }

        ListMembershipEntity membership = membershipRepository.findByListIdAndUserId(listId, userId)
                .orElseThrow(() -> new ListOperationException("User is not a member of this list"));
        membership.setFinishedAt(Instant.now());
        membershipRepository.save(membership);

        phaseTransitionService.checkAndTransitionFromAddition(listId);
    }

    private void ensureUserIsMember(Long listId, Long userId) {
        if (membershipRepository.findByListIdAndUserId(listId, userId).isEmpty()) {
            throw new ListOperationException("User is not a member of this list");
        }
    }
}