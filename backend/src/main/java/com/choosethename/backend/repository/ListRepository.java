package com.choosethename.backend.repository;

import com.choosethename.backend.model.ListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListRepository extends JpaRepository<ListEntity, Long> {
    Optional<ListEntity> findByInvitationCodeIgnoreCase(String code);

    @Query("SELECT l FROM ListEntity l WHERE l.id IN " +
           "(SELECT m.listId FROM ListMembershipEntity m WHERE m.userId = :userId) " +
           "AND l.phase IN :phases")
    List<ListEntity> findActiveListsForUser(@Param("userId") Long userId, @Param("phases") List<String> phases);
}