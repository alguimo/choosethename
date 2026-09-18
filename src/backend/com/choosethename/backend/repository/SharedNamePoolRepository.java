package com.choosethename.backend.repository;

import com.choosethename.backend.model.SharedNamePoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedNamePoolRepository extends JpaRepository<SharedNamePoolEntity, Long> {
    Optional<SharedNamePoolEntity> findByListIdAndNormalizedName(Long listId, String normalizedName);

    long countByListId(Long listId);

    List<SharedNamePoolEntity> findByListIdOrderByIdAsc(Long listId);
}