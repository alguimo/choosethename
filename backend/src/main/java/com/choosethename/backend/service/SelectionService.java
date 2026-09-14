package com.choosethename.backend.service;

import com.choosethename.backend.dto.AdoptNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.dto.SelectionResponseDTO;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.NameEntity;
import com.choosethename.backend.model.SharedNamePoolEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.ListRepository;
import com.choosethename.backend.repository.NameRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SelectionService {

    private static final String RULE_SELECTION_PHASE = "SELECTION";

    private final ListRepository listRepository;
    private final ListMembershipRepository membershipRepository;
    private final NameRepository nameRepository;
    private final SharedNamePoolRepository sharedNamePoolRepository;
    private final NameNormalizer nameNormalizer;

    @Transactional(readOnly = true)
    public SelectionResponseDTO getSelection(Long listId, Long userId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (!RULE_SELECTION_PHASE.equals(list.getPhase())) {
            throw new ListOperationException("Selection is only available during the SELECTION phase");
        }
        ensureUserIsMember(listId, userId);

        List<NameEntity> myEntities = nameRepository.findByListIdAndUserId(listId, userId);
        List<NameEntity> otherEntities = nameRepository.findByListId(listId).stream()
                .filter(n -> !n.getUserId().equals(userId))
                .toList();

        Map<String, NameEntity> myByNormalized = indexByNormalized(myEntities);
        Map<String, NameEntity> otherByNormalized = indexByNormalized(otherEntities);

        List<NameResponseDTO.NameEntry> commonNames = new ArrayList<>();
        for (NameEntity mine : myEntities) {
            if (otherByNormalized.containsKey(mine.getNormalizedName())) {
                commonNames.add(entry(mine));
            }
        }

        List<NameResponseDTO.NameEntry> fadedSuggestions = new ArrayList<>();
        for (NameEntity other : otherEntities) {
            if (!myByNormalized.containsKey(other.getNormalizedName())) {
                fadedSuggestions.add(entry(other));
            }
        }

        SelectionResponseDTO response = new SelectionResponseDTO();
        response.setCommonNames(commonNames);
        response.setFadedSuggestions(fadedSuggestions);
        response.setMyNames(myEntities.stream().map(this::entry).toList());
        return response;
    }

    @Transactional
    public void adoptFadedName(Long listId, Long userId, AdoptNameRequestDTO request) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (!RULE_SELECTION_PHASE.equals(list.getPhase())) {
            throw new ListOperationException("Names can only be adopted during the SELECTION phase");
        }
        ensureUserIsMember(listId, userId);

        String normalized = nameNormalizer.normalize(request.getName());
        if (normalized.isEmpty()) {
            throw new ListOperationException("Name cannot be blank");
        }

        List<NameEntity> allNames = nameRepository.findByListId(listId);
        List<NameEntity> otherNames = allNames.stream()
                .filter(n -> !n.getUserId().equals(userId))
                .toList();
        Set<String> myNormalized = allNames.stream()
                .filter(n -> n.getUserId().equals(userId))
                .map(NameEntity::getNormalizedName)
                .collect(java.util.stream.Collectors.toSet());

        boolean inOtherPool = otherNames.stream()
                .anyMatch(n -> n.getNormalizedName().equals(normalized));
        if (!inOtherPool || myNormalized.contains(normalized)) {
            throw new ListOperationException("Name is not a faded suggestion for this user");
        }

        if (sharedNamePoolRepository.findByListIdAndNormalizedName(listId, normalized).isPresent()) {
            throw new ListOperationException("Name has already been adopted");
        }

        SharedNamePoolEntity entry = new SharedNamePoolEntity();
        entry.setListId(listId);
        entry.setNormalizedName(normalized);
        entry.setAdoptedBy(userId);
        entry.setAdoptedAt(Instant.now());
        sharedNamePoolRepository.save(entry);
    }

    private Map<String, NameEntity> indexByNormalized(List<NameEntity> entities) {
        Map<String, NameEntity> map = new LinkedHashMap<>();
        for (NameEntity entity : entities) {
            map.put(entity.getNormalizedName(), entity);
        }
        return map;
    }

    private NameResponseDTO.NameEntry entry(NameEntity entity) {
        return new NameResponseDTO.NameEntry(entity.getName(), entity.getNormalizedName());
    }

    private void ensureUserIsMember(Long listId, Long userId) {
        if (membershipRepository.findByListIdAndUserId(listId, userId).isEmpty()) {
            throw new ListOperationException("User is not a member of this list");
        }
    }
}