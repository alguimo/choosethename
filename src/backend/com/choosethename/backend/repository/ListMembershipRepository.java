package com.choosethename.backend.repository;

import com.choosethename.backend.model.ListMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListMembershipRepository extends JpaRepository<ListMembershipEntity, Long> {
    List<ListMembershipEntity> findByListId(Long listId);

    Optional<ListMembershipEntity> findByListIdAndUserId(Long listId, Long userId);

    long countByListId(Long listId);
}