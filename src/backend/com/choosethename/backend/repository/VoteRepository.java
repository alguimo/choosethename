package com.choosethename.backend.repository;

import com.choosethename.backend.model.VoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<VoteEntity, Long> {
    Optional<VoteEntity> findByListIdAndUserIdAndRoundId(Long listId, Long userId, Long roundId);

    List<VoteEntity> findByListIdAndRoundId(Long listId, Long roundId);

    long countByListIdAndRoundId(Long listId, Long roundId);
}