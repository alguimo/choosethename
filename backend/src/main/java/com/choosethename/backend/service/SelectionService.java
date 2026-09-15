package com.choosethename.backend.service;

import com.choosethename.backend.dto.AdoptNameRequestDTO;
import com.choosethename.backend.dto.NameResponseDTO;
import com.choosethename.backend.dto.SelectionResponseDTO;
import com.choosethename.backend.exception.ListNotFoundException;
import com.choosethename.backend.exception.ListOperationException;
import com.choosethename.backend.model.ListEntity;
import com.choosethename.backend.model.ListPhase;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SelectionService {

    private final ListRepository listRepository;
    private final ListMembershipRepository membershipRepository;
    private final NameRepository nameRepository;
    private final SharedNamePoolRepository sharedNamePoolRepository;
    private final NameNormalizer nameNormalizer;

    @Transactional(readOnly = true)
    public SelectionResponseDTO getSelection(Long listId, Long userId) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (list.getPhase() != ListPhase.SELECTION) {
            throw new ListOperationException("Selection is only available during the SELECTION phase");
        }
        ensureUserIsMember(listId, userId);

        List<NameEntity> allNames = nameRepository.findByListId(listId).stream()
                .sorted(Comparator.comparing(NameEntity::getId))
                .toList();
        List<NameEntity> myEntities = allNames.stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();
        Set<String> myNormalized = myEntities.stream()
                .map(NameEntity::getNormalizedName)
                .collect(java.util.stream.Collectors.toSet());

        long totalMembers = membershipRepository.countByListId(listId);
        long majorityThreshold = totalMembers / 2;
        Map<String, Integer> presence = presencePerName(allNames);

        List<NameResponseDTO.NameEntry> commonNames = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : presence.entrySet()) {
            if (entry.getValue() > majorityThreshold) {
                String normalized = entry.getKey();
                NameEntity sample = allNames.stream()
                        .filter(n -> n.getNormalizedName().equals(normalized))
                        .findFirst()
                        .orElseThrow();
                commonNames.add(new NameResponseDTO.NameEntry(sample.getName(), sample.getNormalizedName()));
            }
        }

        List<NameResponseDTO.NameEntry> fadedSuggestions = new ArrayList<>();
        for (NameEntity other : allNames) {
            if (other.getUserId().equals(userId)) {
                continue;
            }
            String normalized = other.getNormalizedName();
            boolean common = presence.getOrDefault(normalized, 0) > majorityThreshold;
            if (!myNormalized.contains(normalized) && !common) {
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
        if (request == null) {
            throw new ListOperationException("Request body is required");
        }

        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found"));
        if (list.getPhase() != ListPhase.SELECTION) {
            throw new ListOperationException("Names can only be adopted during the SELECTION phase");
        }
        ensureUserIsMember(listId, userId);

        String normalized = nameNormalizer.normalize(request.getName());
        if (normalized.isEmpty()) {
            throw new ListOperationException("Name cannot be blank");
        }

        List<NameEntity> allNames = nameRepository.findByListId(listId);
        Set<String> myNormalized = allNames.stream()
                .filter(n -> n.getUserId().equals(userId))
                .map(NameEntity::getNormalizedName)
                .collect(java.util.stream.Collectors.toSet());
        boolean inOtherPool = allNames.stream()
                .filter(n -> !n.getUserId().equals(userId))
                .anyMatch(n -> n.getNormalizedName().equals(normalized));

        long totalMembers = membershipRepository.countByListId(listId);
        long majorityThreshold = totalMembers / 2;
        int presence = presencePerName(allNames).getOrDefault(normalized, 0);
        boolean common = presence > majorityThreshold;

        if (!inOtherPool || myNormalized.contains(normalized) || common) {
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

    private Map<String, Integer> presencePerName(List<NameEntity> entities) {
        Map<Long, Set<String>> memberPools = new LinkedHashMap<>();
        for (NameEntity entity : entities) {
            memberPools.computeIfAbsent(entity.getUserId(), k -> new LinkedHashSet<>())
                    .add(entity.getNormalizedName());
        }
        Map<String, Integer> presence = new HashMap<>();
        for (Set<String> pool : memberPools.values()) {
            for (String normalized : pool) {
                presence.merge(normalized, 1, Integer::sum);
            }
        }
        return presence;
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