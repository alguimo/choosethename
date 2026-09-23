package com.choosethename.backend.service;

import com.choosethename.backend.model.NameEntity;
import com.choosethename.backend.model.SharedNamePoolEntity;
import com.choosethename.backend.repository.ListMembershipRepository;
import com.choosethename.backend.repository.NameRepository;
import com.choosethename.backend.repository.SharedNamePoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the initial voting pool for a list.
 *
 * <p>The pool is the union of every adopted faded name (stored in the shared pool) and
 * every common name (proposed by more than half of the members). Common names are never
 * persisted in the shared pool (their {@code adopted_by} FK is NOT NULL), so they are
 * computed and merged at transition/voting time (Spec 003 FR-11, Spec 004 FR-6).</p>
 */
@Service
@RequiredArgsConstructor
public class NamePoolService {

    private final NameRepository nameRepository;
    private final SharedNamePoolRepository sharedNamePoolRepository;
    private final ListMembershipRepository membershipRepository;

    public List<String> buildSharedPool(Long listId) {
        List<NameEntity> allNames = nameRepository.findByListId(listId);
        Map<String, Integer> presence = presencePerName(allNames);
        long totalMembers = membershipRepository.countByListId(listId);
        long majorityThreshold = totalMembers / 2;

        LinkedHashSet<String> pool = new LinkedHashSet<>();
        sharedNamePoolRepository.findByListIdOrderByIdAsc(listId).stream()
                .map(SharedNamePoolEntity::getNormalizedName)
                .forEach(pool::add);

        presence.entrySet().stream()
                .filter(entry -> entry.getValue() > majorityThreshold)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(pool::add);

        return List.copyOf(pool);
    }

    private Map<String, Integer> presencePerName(List<NameEntity> entities) {
        Map<Long, Set<String>> memberPools = new LinkedHashMap<>();
        for (NameEntity entity : entities) {
            memberPools.computeIfAbsent(entity.getUserId(), k -> new LinkedHashSet<>())
                    .add(entity.getNormalizedName());
        }
        Map<String, Integer> presence = new HashMap<>();
        for (Set<String> memberNames : memberPools.values()) {
            for (String normalized : memberNames) {
                presence.merge(normalized, 1, Integer::sum);
            }
        }
        return presence;
    }
}