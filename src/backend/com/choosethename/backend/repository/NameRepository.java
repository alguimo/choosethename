package com.choosethename.backend.repository;

import com.choosethename.backend.model.NameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NameRepository extends JpaRepository<NameEntity, Long> {
    List<NameEntity> findByListIdAndUserId(Long listId, Long userId);

    List<NameEntity> findByListId(Long listId);

    boolean existsByListIdAndUserIdAndNormalizedName(Long listId, Long userId, String normalizedName);

    long countByListId(Long listId);

    long countByListIdAndUserId(Long listId, Long userId);
}