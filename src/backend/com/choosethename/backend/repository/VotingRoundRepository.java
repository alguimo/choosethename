package com.choosethename.backend.repository;

import com.choosethename.backend.model.VotingRoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VotingRoundRepository extends JpaRepository<VotingRoundEntity, Long> {
    Optional<VotingRoundEntity> findByListIdAndRoundNumber(Long listId, Integer roundNumber);

    List<VotingRoundEntity> findByListId(Long listId);

    Optional<VotingRoundEntity> findFirstByListIdOrderByRoundNumberDesc(Long listId);
}